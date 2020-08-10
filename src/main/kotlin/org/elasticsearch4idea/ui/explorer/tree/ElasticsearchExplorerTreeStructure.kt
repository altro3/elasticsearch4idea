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

import com.intellij.ide.util.treeView.AbstractTreeStructure
import com.intellij.ide.util.treeView.NodeDescriptor
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.util.ArrayUtil
import org.elasticsearch4idea.model.ElasticsearchCluster
import org.elasticsearch4idea.model.ElasticsearchIndex
import org.elasticsearch4idea.service.ElasticsearchManager

class ElasticsearchExplorerTreeStructure(project: Project) : AbstractTreeStructure() {

    private val elasticsearchManager = project.service<ElasticsearchManager>()

    override fun isToBuildChildrenInBackground(element: Any) = true

    override fun isAlwaysLeaf(element: Any): Boolean {
        return false
    }

    override fun createDescriptor(element: Any, parentDescriptor: NodeDescriptor<*>?): AbstractNodeDescriptor<*> {
        return when (element) {
            RootNodeDescriptor.ROOT -> {
                RootNodeDescriptor(parentDescriptor)
            }
            is String -> {
                TextNodeDescriptor(element, parentDescriptor)
            }
            is ElasticsearchCluster -> {
                ClusterNodeDescriptor(
                    element,
                    parentDescriptor
                )
            }
            is ElasticsearchIndex -> {
                IndexNodeDescriptor(
                    element,
                    parentDescriptor
                )
            }
            else -> {
                LOG.error("Unknown element for this tree structure $element")
                throw IllegalArgumentException()
            }
        }
    }

    override fun getChildElements(element: Any): Array<Any> {
        if (element === RootNodeDescriptor.ROOT) {
            val clusters = elasticsearchManager.getClusters()
            return clusters.sortedBy { it.label }.toTypedArray()
        }
        if (element is ElasticsearchCluster) {
            val indices: List<ElasticsearchIndex> = element.indices.sortedBy { it.name }
            return indices.toTypedArray()
        }
        return ArrayUtil.EMPTY_OBJECT_ARRAY
    }

    override fun commit() {
    }

    override fun getParentElement(element: Any): Any? {
        return when (element) {
            is ElasticsearchIndex -> {
                element.cluster
            }
            is ElasticsearchCluster -> {
                RootNodeDescriptor.ROOT
            }
            else -> {
                null
            }
        }
    }

    override fun getRootElement() =
        RootNodeDescriptor.ROOT

    override fun hasSomethingToCommit() = false

    companion object {
        private val LOG = Logger.getInstance(
            ElasticsearchExplorerTreeStructure::class.java
        )
    }

}