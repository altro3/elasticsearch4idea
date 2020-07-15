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

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue

class Mapping(
    val index: String,
    val type: String?,
    val nodes: Map<String, MappingNode>
) {

    companion object {
        fun parseMappings(mappingsJson: String): List<Mapping> {
            val jsonMap = jacksonObjectMapper().readValue<Map<String, Any>>(mappingsJson)
            val mappings = ArrayList<Mapping>()
            jsonMap.forEach { index, indexProperties ->
                val indexMapping = (indexProperties as Map<String, Map<String, Any>>).get("mappings")!!
                if (indexMapping.contains("properties")) {
                    val nodes = parseMappingNodes(indexMapping)
                    mappings.add(Mapping(index, null, nodes))
                } else if (indexMapping.isNotEmpty()) {
                    indexMapping.forEach { type, typeMapping ->
                        val nodes = parseMappingNodes(typeMapping as Map<String, Any>)
                        mappings.add(Mapping(index, type, nodes))
                    }
                }
            }
            return mappings
        }

        private fun parseMappingNodes(mapping: Map<String, Any>): Map<String, MappingNode> {
            val mappingProperties = (mapping as Map<String, Map<String, Map<String, Any>>>).get("properties")!!
            return mappingProperties.map {
                it.key to MappingNode.createNode(it.key, it.value)
            }.toMap()
        }
    }
}

