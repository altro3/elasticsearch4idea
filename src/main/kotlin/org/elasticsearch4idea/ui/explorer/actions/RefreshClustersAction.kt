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

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.components.service
import com.intellij.openapi.project.DumbAwareAction
import org.elasticsearch4idea.service.ElasticsearchManager
import org.elasticsearch4idea.ui.explorer.ElasticsearchExplorer
import org.elasticsearch4idea.utils.TaskUtils
import java.awt.Toolkit
import java.awt.event.KeyEvent

class RefreshClustersAction(private val elasticsearchExplorer: ElasticsearchExplorer) :
    DumbAwareAction("Refresh cluster", "Refresh Elasticsearch clusters", AllIcons.Actions.Refresh) {

    init {
        registerCustomShortcutSet(
            KeyEvent.VK_R,
            Toolkit.getDefaultToolkit().menuShortcutKeyMask,
            elasticsearchExplorer
        )
    }

    override fun actionPerformed(event: AnActionEvent) {
        val elasticsearchManager = event.project!!.service<ElasticsearchManager>()
        elasticsearchExplorer.getSelectedClusters().asSequence()
            .filter { !it.isLoading() }
            .map { it.label }
            .forEach {
                TaskUtils.runBackgroundTask("Getting Elasticsearch cluster info...") {
                    elasticsearchManager.prepareFetchCluster(it)
                }
            }
    }

    override fun update(event: AnActionEvent) {
        val clusters = elasticsearchExplorer.getSelectedClusters()
        event.presentation.isEnabled = clusters.any { !it.isLoading() }
    }
}