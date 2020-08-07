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

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.ui.JBCardLayout
import org.elasticsearch4idea.model.ViewMode
import org.elasticsearch4idea.service.GlobalSettings
import org.elasticsearch4idea.ui.editor.QueryManager
import org.elasticsearch4idea.ui.editor.RequestAndResponse
import javax.swing.JPanel

class ResultPanel(
    private val project: Project,
    private val elasticsearchPanel: ElasticsearchPanel
) : JPanel(), Disposable {
    private var jsonResultPanel: JsonResultPanel? = null
    private var tableResultPanel: TableResultPanel? = null
    private val cardLayout: JBCardLayout = JBCardLayout()
    private lateinit var queryManager: QueryManager
    private val globalSettings = service<GlobalSettings>()

    init {
        layout = cardLayout
    }

    fun setCurrentViewMode(viewMode: ViewMode) {
        globalSettings.settings.viewMode = viewMode
    }

    fun getCurrentViewMode(): ViewMode {
        return globalSettings.settings.viewMode
    }

    fun updateResult(requestAndResponse: RequestAndResponse) {
        updateView(getCurrentViewMode(), requestAndResponse)
    }

    private fun updateView(viewMode: ViewMode, requestAndResponse: RequestAndResponse) {
        when (viewMode) {
            ViewMode.TEXT -> {
                if (jsonResultPanel == null) {
                    jsonResultPanel = JsonResultPanel(project)
                    add(jsonResultPanel!!, "jsonResultPanel")
                }
                jsonResultPanel?.updateEditorText(requestAndResponse)
                cardLayout.show(this, "jsonResultPanel")
            }
            ViewMode.TABLE -> {
                if (tableResultPanel == null) {
                    tableResultPanel = TableResultPanel(elasticsearchPanel, queryManager)
                    add(tableResultPanel!!, "tableResultPanel")
                }
                if (tableResultPanel?.updateResultTable(requestAndResponse) == false) {
                    updateView(ViewMode.TEXT, requestAndResponse)
                    return
                }
                cardLayout.show(this, "tableResultPanel")
            }
        }
    }

    override fun dispose() {
        jsonResultPanel?.dispose()
    }

    fun setQueryManager(queryManager: QueryManager) {
        this.queryManager = queryManager
    }

}