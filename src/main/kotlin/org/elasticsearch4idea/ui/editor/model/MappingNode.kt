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

package org.elasticsearch4idea.ui.editor.model

class MappingNode(
    val name: String,
    val type: String?,
    val children: Map<String, MappingNode>?
) {

    companion object {

        fun createNode(name: String, properties: Map<String, Any>): MappingNode {
            val type = properties.get("type") as String?
            val children = if (properties.contains("properties")) {
                (properties.get("properties") as Map<String, Map<String, Any>>).map {
                    it.key to createNode(it.key, it.value)
                }.toMap()
            } else {
                null
            }
            return MappingNode(name, type, children)
        }
    }
}