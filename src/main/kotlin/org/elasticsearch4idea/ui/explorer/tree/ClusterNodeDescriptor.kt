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
package org.elasticsearch4idea.ui.explorer.tree

import com.intellij.ide.projectView.PresentationData
import com.intellij.ide.util.treeView.NodeDescriptor
import com.intellij.ui.AnimatedIcon
import com.intellij.ui.SimpleTextAttributes
import icons.Icons
import org.elasticsearch4idea.model.ElasticsearchCluster
import org.elasticsearch4idea.rest.model.HealthStatus.*

class ClusterNodeDescriptor(
    private val cluster: ElasticsearchCluster,
    parentDescriptor: NodeDescriptor<*>?
) : AbstractNodeDescriptor<ElasticsearchCluster>(cluster, parentDescriptor) {

    override fun update(presentation: PresentationData) {
        val icon = when {
            cluster.isLoaded() -> {
                when (cluster.healthStatus!!) {
                    GREEN -> Icons.CLUSTER_GREEN
                    YELLOW -> Icons.CLUSTER_YELLOW
                    RED -> Icons.CLUSTER_RED
                }
            }
            cluster.isLoading() -> {
                AnimatedIcon.Default.INSTANCE
            }
            else -> {
                Icons.CLUSTER
            }
        }
        presentation.setIcon(icon)
        presentation.presentableText = cluster.label
        presentation.addText(cluster.label, SimpleTextAttributes.REGULAR_ATTRIBUTES)
        presentation.setTooltip(cluster.host)
    }

}