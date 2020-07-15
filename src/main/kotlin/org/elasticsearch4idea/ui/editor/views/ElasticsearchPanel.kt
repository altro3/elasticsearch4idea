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
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.components.service
import com.intellij.openapi.keymap.KeymapUtil
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.ui.Splitter
import com.intellij.ui.EnumComboBoxModel
import com.intellij.ui.JBColor
import com.intellij.ui.SearchTextField
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import com.intellij.util.ui.components.BorderLayoutPanel
import org.elasticsearch4idea.model.*
import org.elasticsearch4idea.service.ElasticsearchConfiguration
import org.elasticsearch4idea.service.ElasticsearchManager
import org.elasticsearch4idea.ui.editor.ElasticsearchFile
import org.elasticsearch4idea.ui.editor.actions.ExecuteQueryAction
import org.elasticsearch4idea.ui.editor.actions.ViewAsActionGroup
import org.elasticsearch4idea.utils.TaskUtils
import java.awt.BorderLayout
import java.awt.Dimension
import javax.swing.JPanel

class ElasticsearchPanel(
    private val project: Project,
    private val elasticsearchFile: ElasticsearchFile
) : JPanel(), Disposable {

    private val bodyPanel: BodyPanel
    private val resultPanel: ResultPanel

    private val methodCombo = ComboBox(EnumComboBoxModel(Method::class.java), 85)
        .also { it.preferredSize = Dimension(it.width, 28) }
    private val urlField = UrlField()
    private val elasticsearchConfiguration = project.service<ElasticsearchConfiguration>()

    init {
        layout = BorderLayout()
        bodyPanel = BodyPanel(project)
        resultPanel = ResultPanel(project, elasticsearchConfiguration.viewMode)

        initUrlComponent()
        val toolbar = createToolbarPanel()
        add(toolbar, BorderLayout.NORTH)

        bodyPanel.updateQueryView(elasticsearchFile.request.body)
        methodCombo.selectedItem = elasticsearchFile.request.method
        urlField.text = elasticsearchFile.request.urlPath

        val splitter = Splitter(true, 0.2f)
        splitter.divider.background = UIUtil.SIDE_PANEL_BACKGROUND
        splitter.firstComponent = bodyPanel
        splitter.secondComponent = resultPanel
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
        group.add(ExecuteQueryAction(this))
        group.add(ViewAsActionGroup(this, project))
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

    fun executeQuery() {
        TaskUtils.runBackgroundTask("Executing request...") {
            val elasticsearchManager = project.service<ElasticsearchManager>()
            val url = "${elasticsearchFile.cluster.host}/${urlField.text}"
            val request = Request(url, bodyPanel.getBody(), methodCombo.selectedItem as Method)
            val mappingRequestExecution = if (urlField.text.contains("_search")) {
                val mappingUrl =
                    "${elasticsearchFile.cluster.host}/${urlField.text.substring(0, urlField.text.indexOf("_search"))}_mapping"
                val mappingRequest = Request(urlPath = mappingUrl, method = Method.GET)
                elasticsearchManager.prepareExecuteRequest(mappingRequest, elasticsearchFile.cluster)
            } else {
                null
            }
            val requestExecution = elasticsearchManager.prepareExecuteRequest(request, elasticsearchFile.cluster)
            RequestExecution(
                execution = {
                    val mappingFuture = mappingRequestExecution?.executeOnPooledThread()
                    val response = requestExecution.execute()
                    if (mappingFuture != null) {
                        val mappingResponse = mappingFuture.get()
                        Pair<Response, Response>(response, mappingResponse)
                    } else {
                        Pair(response, null)
                    }
                },
                onAbort = {
                    mappingRequestExecution?.abort()
                    requestExecution.abort()
                }
            )
                .onSuccess {
                    WriteCommandAction.runWriteCommandAction(project) {
                        UIUtil.invokeLaterIfNeeded {
                            resultPanel.updateResult(it.first.content, it.second?.content)
                        }
                    }
                }
                .onError {
                    UIUtil.invokeLaterIfNeeded {
                        Messages.showErrorDialog(it.message, "Error")
                    }
                }
        }
    }

    fun showResults() {
        executeQuery()
    }

    fun getResultPanel(): ResultPanel {
        return resultPanel
    }

    override fun dispose() {
        resultPanel.dispose()
        bodyPanel.dispose()
    }

    fun setViewMode(viewMode: ViewMode) {
        if (resultPanel.getCurrentViewMode() == viewMode) {
            return
        }
        resultPanel.setCurrentViewMode(viewMode)
        executeQuery()
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