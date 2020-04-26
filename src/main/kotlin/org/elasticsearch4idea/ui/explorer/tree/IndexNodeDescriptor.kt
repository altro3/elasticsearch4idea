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
import com.intellij.ui.SimpleTextAttributes
import icons.Icons
import org.elasticsearch4idea.model.ElasticsearchIndex
import org.elasticsearch4idea.rest.model.HealthStatus.*
import org.elasticsearch4idea.rest.model.IndexStatus

class IndexNodeDescriptor(
    private val index: ElasticsearchIndex,
    parentDescriptor: NodeDescriptor<*>?
) : AbstractNodeDescriptor<ElasticsearchIndex>(index, parentDescriptor) {

    override fun update(presentation: PresentationData) {
        icon = if (index.status == IndexStatus.CLOSE) {
            Icons.INDEX_CLOSED
        } else when (index.healthStatus) {
            GREEN -> Icons.INDEX_GREEN
            YELLOW -> Icons.INDEX_YELLOW
            RED -> Icons.INDEX_RED
            else -> Icons.INDEX_CLOSED
        }
        presentation.setIcon(icon)
        presentation.presentableText = index.name
        presentation.addText(index.name, SimpleTextAttributes.REGULAR_ATTRIBUTES)
    }
}