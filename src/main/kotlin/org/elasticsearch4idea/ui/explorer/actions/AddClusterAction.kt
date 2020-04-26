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
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.DumbAwareAction
import org.elasticsearch4idea.service.ElasticsearchManager
import org.elasticsearch4idea.ui.explorer.ElasticsearchExplorer
import org.elasticsearch4idea.ui.explorer.dialogs.ClusterConfigurationDialog

class AddClusterAction(private val elasticsearchExplorer: ElasticsearchExplorer) :
    DumbAwareAction("Add Cluster", "Add Elasticsearch cluster", AllIcons.General.Add) {

    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project!!

        val dialog = ClusterConfigurationDialog(
            elasticsearchExplorer,
            project,
            null,
            false
        )
        dialog.title = "Add Elasticsearch cluster"
        dialog.show()
        if (!dialog.isOK) {
            return
        }
        val clusterConfiguration = dialog.getConfiguration()
        val manager = project.service<ElasticsearchManager>()

        ProgressManager.getInstance()
            .run(object : Task.Backgroundable(project, "Getting Elasticsearch cluster info", false) {
                override fun run(indicator: ProgressIndicator) {
                    manager.addCluster(clusterConfiguration)
                }
            })
    }

}