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
package org.elasticsearch4idea.ui.explorer.actions

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonShortcuts
import com.intellij.openapi.components.service
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.ui.Messages
import org.elasticsearch4idea.service.ElasticsearchManager
import org.elasticsearch4idea.ui.explorer.ElasticsearchExplorer
import org.elasticsearch4idea.utils.TaskUtils

class RemoveAction(
    private val elasticsearchExplorer: ElasticsearchExplorer
) :
    DumbAwareAction("Delete...", "Remove selected item", null) {

    init {
        registerCustomShortcutSet(CommonShortcuts.getDelete(), elasticsearchExplorer)
    }

    override fun actionPerformed(event: AnActionEvent) {
        val selectedCluster = elasticsearchExplorer.getSelectedCluster()
        if (selectedCluster != null) {
            val result: Int = Messages.showYesNoDialog(
                "'${selectedCluster.label}' will be removed.",
                "Confirmation",
                Messages.getQuestionIcon()
            )
            if (result == Messages.YES) {
                elasticsearchExplorer.removeSelectedCluster(selectedCluster)
            }
            return
        }
        val selectedIndex = elasticsearchExplorer.getSelectedIndex()
        if (selectedIndex != null) {
            val result = Messages.showInputDialog(
                "Type 'DELETE' to delete '${selectedIndex.name}' index.",
                "Confirmation",
                Messages.getQuestionIcon()
            )
            if (result?.toUpperCase() == "DELETE") {
                TaskUtils.runBackgroundTask("Deleting index...") {
                    val elasticsearchManager = event.project!!.service<ElasticsearchManager>()
                    elasticsearchManager.prepareDeleteIndex(selectedIndex)
                }
            }
            return
        }
    }

    override fun update(event: AnActionEvent) {
        event.presentation.isVisible = elasticsearchExplorer.getSelected() != null
    }
}