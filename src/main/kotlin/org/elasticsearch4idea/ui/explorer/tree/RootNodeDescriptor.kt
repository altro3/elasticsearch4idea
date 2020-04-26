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

class RootNodeDescriptor(parentDescriptor: NodeDescriptor<*>?) :
    AbstractNodeDescriptor<Any>(ROOT, parentDescriptor) {

    override fun update(presentation: PresentationData) {
        presentation.presentableText = ""
        presentation.addText("<root>", SimpleTextAttributes.REGULAR_ATTRIBUTES)
    }

    companion object {
        val ROOT = Any()
    }
}