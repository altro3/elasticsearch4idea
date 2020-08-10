/*
 * Copyright 2020 Anton Shuvaev
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.elasticsearch4idea.ui.editor.views

import com.intellij.find.editorHeaderActions.Utils
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.actionSystem.impl.ActionToolbarImpl
import com.intellij.openapi.components.service
import com.intellij.openapi.keymap.KeymapUtil
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.Splitter
import com.intellij.ui.EnumComboBoxModel
import com.intellij.ui.JBColor
import com.intellij.ui.SearchTextField
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import com.intellij.util.ui.components.BorderLayoutPanel
import org.elasticsearch4idea.model.Method
import org.elasticsearch4idea.model.Request
import org.elasticsearch4idea.model.ViewMode
import org.elasticsearch4idea.service.GlobalSettings
import org.elasticsearch4idea.ui.editor.ElasticsearchFile
import org.elasticsearch4idea.ui.editor.QueryManager
import org.elasticsearch4idea.ui.editor.actions.ExecuteQueryAction
import org.elasticsearch4idea.ui.editor.actions.SettingsActionGroup
import java.awt.BorderLayout
import java.awt.Dimension
import javax.swing.JPanel

class ElasticsearchPanel(
    project: Project,
    private val elasticsearchFile: ElasticsearchFile
) : JPanel(), Disposable {

    private val bodyPanel: BodyPanel
    private val resultPanel: ResultPanel
    private val queryManager: QueryManager

    private val methodCombo = ComboBox(EnumComboBoxModel(Method::class.java), 85)
        .also { it.preferredSize = Dimension(it.width, 28) }
    private val urlField = UrlField()
    private val splitter: Splitter
    private val globalSettings = service<GlobalSettings>()

    init {
        layout = BorderLayout()
        bodyPanel = BodyPanel(project)
        queryManager = QueryManager(project, elasticsearchFile.cluster, this::getRequest)
        resultPanel = ResultPanel(project, this, queryManager)

        initUrlComponent()

        bodyPanel.updateQueryView(elasticsearchFile.request.body)
        methodCombo.selectedItem = elasticsearchFile.request.method
        urlField.text = elasticsearchFile.request.path

        methodCombo.addItemListener {
            val selectedMethod = it.item as Method
            bodyPanel.isVisible = selectedMethod.hasBody
        }

        splitter = Splitter(globalSettings.settings.isVerticalOrientation, elasticsearchFile.bodyToResponseProportion)
        splitter.divider.background = UIUtil.SIDE_PANEL_BACKGROUND
        splitter.firstComponent = bodyPanel
        splitter.secondComponent = resultPanel
        val toolbar = createToolbarPanel()
        add(toolbar, BorderLayout.NORTH)
        add(splitter, BorderLayout.CENTER)
    }

    private fun initUrlComponent() {
        val textComponent = urlField.textEditor

        UIUtil.addUndoRedoActions(textComponent)

        textComponent.background = UIUtil.getTextFieldBackground()
        textComponent.border = JBUI.Borders.empty()

        object : DumbAwareAction() {
            override fun actionPerformed(e: AnActionEvent) {
            }
        }.registerCustomShortcutSet(KeymapUtil.getActiveKeymapShortcuts(IdeActions.ACTION_EDITOR_ESCAPE), urlField)

    }

    private fun createToolbarPanel(): JPanel {
        val group = DefaultActionGroup()
        group.add(ExecuteQueryAction(queryManager, this))
//        group.add(ViewAsActionGroup(this))
        group.add(SettingsActionGroup(this))
        val actionToolBar = ActionManager.getInstance()
            .createActionToolbar("ElasticsearchQueryToolBar", group, true) as ActionToolbarImpl
        actionToolBar.setTargetComponent(this)
        actionToolBar.layoutPolicy = ActionToolbar.AUTO_LAYOUT_POLICY
        Utils.setSmallerFontForChildren(actionToolBar)

        val panel = BorderLayoutPanel()
        panel.border = JBUI.Borders.customLine(JBColor.border(), 0, 0, 1, 0)
        panel.add(methodCombo, BorderLayout.WEST)
        panel.add(urlField, BorderLayout.CENTER)
        panel.add(actionToolBar.component, BorderLayout.EAST)
        return panel
    }

    fun getUrl(): String {
        return urlField.text
    }

    private fun getMethod(): Method {
        return methodCombo.selectedItem as Method
    }

    fun showResults() {
        queryManager.executeRequest()
    }

    fun getResultPanel(): ResultPanel {
        return resultPanel
    }

    fun getViewMode(): ViewMode {
        return resultPanel.getCurrentViewMode()
    }

    fun setViewMode(viewMode: ViewMode) {
        if (resultPanel.getCurrentViewMode() == viewMode) {
            return
        }
        resultPanel.setCurrentViewMode(viewMode)
        queryManager.executeRequest()
    }

    fun isVerticalOrientation(): Boolean {
        return splitter.orientation
    }

    fun setOrientation(isVertical: Boolean) {
        splitter.orientation = isVertical
        globalSettings.settings.isVerticalOrientation = isVertical
    }

    fun updateFromRequest(request: Request) {
        urlField.text = request.path
        methodCombo.selectedItem = request.method
        bodyPanel.updateQueryView(request.body)
    }

    private fun getRequest(): Request {
        val body = if (getMethod().hasBody) {
            bodyPanel.getBody()
        } else {
            ""
        }
        return Request(
            host = elasticsearchFile.cluster.host,
            path = urlField.text,
            body = body,
            method = getMethod()
        )
    }

    override fun dispose() {
        resultPanel.dispose()
        bodyPanel.dispose()
    }

    internal class UrlField : SearchTextField() {
        init {
            border = JBUI.Borders.empty(2, 0)
        }

        override fun toClearTextOnEscape(): Boolean {
            return false
        }

    }

}