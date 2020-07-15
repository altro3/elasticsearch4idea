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

import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.convertValue
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.google.common.collect.ArrayListMultimap

class Hit(
    val id: String,
    val index: String,
    val score: Double,
    val type: String,
    val values: Map<String, Any?>
) {

    companion object {

        fun create(objectNode: ObjectNode): Hit {
            val id = objectNode.get("_id").asText()
            val index = objectNode.get("_index").asText()
            val score = objectNode.get("_score").asDouble()
            val type = objectNode.get("_type").asText()
            val source = objectNode.get("_source")
            val values = HashMap<String, Any?>()
            val map = jacksonObjectMapper().convertValue<Map<String, Any?>>(source)
            map.forEach { (key, value) ->
                collectValues(key, value, values)
            }

            return Hit(id, index, score, type, values)
        }

        private fun collectValues(field: String, value: Any?, values: MutableMap<String, Any?>) {
            values.put(field, value)
            if (value is List<*>) {
                val objects = value.asSequence().mapNotNull {
                    if (it is Map<*, *>) {
                        it
                    } else {
                        null
                    }
                }.toList()
                if (objects.isNotEmpty()) {
                    val multiMap = ArrayListMultimap.create<String, Any?>()
                    objects.forEach {
                        it.forEach { entry ->
                            multiMap.put("$field.${entry.key as String}", entry.value)
                        }
                    }
                    multiMap.asMap().forEach { entry ->
                        if (entry.value.size == 1) {
                            collectValues(entry.key as String, entry.value.first(), values)
                        } else if (entry.value.size > 1) {
                            collectValues(entry.key as String, entry.value, values)
                        }
                    }
                }
            } else if (value is Map<*, *>) {
                value.forEach {
                    collectValues("$field.${it.key as String}", it.value, values)
                }
            }
        }
    }
}