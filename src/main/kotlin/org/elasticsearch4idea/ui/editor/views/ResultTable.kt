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

package org.elasticsearch4idea.ui.editor.views

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.google.common.collect.ArrayListMultimap
import com.intellij.ui.table.TableView
import com.intellij.util.ui.ColumnInfo
import com.intellij.util.ui.ListTableModel
import java.awt.Component
import java.util.*
import javax.swing.JTable
import javax.swing.table.TableCellRenderer
import javax.swing.table.TableColumn
import kotlin.math.max
import kotlin.math.min


class ResultTable internal constructor(
    columns: Array<ResultTableColumnInfo>,
    entries: List<ResultTableEntry>,
    val label: String
) : TableView<ResultTable.ResultTableEntry>(ListTableModel(columns, entries)) {

    init {
        setAutoResizeMode(JTable.AUTO_RESIZE_OFF)
        setCellSelectionEnabled(true)

        for (column in 0 until columnCount) {
            val tableColumn: TableColumn = getColumnModel().getColumn(column)
            val renderer: TableCellRenderer = getTableHeader().defaultRenderer
            val header: Component =
                renderer.getTableCellRendererComponent(this, tableColumn.headerValue, false, false, 0, column)
            var preferredWidth = header.preferredSize.width + 10
            val maxWidth = max(300, preferredWidth)
            for (row in 0 until rowCount) {
                val cellRenderer: TableCellRenderer = getCellRenderer(row, column)
                val c: Component = prepareRenderer(cellRenderer, row, column)
                val width: Int = c.preferredSize.width + intercellSpacing.width
                preferredWidth = min(max(preferredWidth, width), maxWidth)

                if (preferredWidth == maxWidth) {
                    break
                }
            }
            tableColumn.preferredWidth = preferredWidth
        }
    }

    companion object {

        private val objectMapper = jacksonObjectMapper()

        fun createResultTable(result: String): ResultTable? {
            return try {
                val rootNode: JsonNode = objectMapper.readValue(result)
                val hits = rootNode.get("hits") ?: return null
                val totalShards = rootNode.get("_shards")?.get("total")?.asInt() ?: 0
                val successfulShards = rootNode.get("_shards")?.get("successful")?.asInt() ?: 0
                val took = (rootNode.get("took")?.asInt() ?: 0) / 1000f
                val tookString = String.format("%.3f", took)
                val totalNode = hits.get("total")
                val total = when {
                    totalNode.isInt -> {
                        totalNode.asLong()
                    }
                    totalNode.isObject -> {
                        totalNode.get("value").asLong()
                    }
                    else -> {
                        0
                    }
                }
                val entries = (hits.get("hits") as ArrayNode).asIterable().asSequence()
                    .map { convertHit(it as ObjectNode) }
                    .toList()

                val columns = entries.asSequence()
                    .flatMap { it.values.keys.asSequence() }
                    .distinct()
                    .sorted()
                    .map {
                        ResultTableColumnInfo(
                            it
                        )
                    }
                    .toList()

                val label = "Searched $successfulShards of $totalShards shards, $total hits, $tookString seconds."

                ResultTable(
                    columns.toTypedArray(),
                    entries,
                    label
                )
            } catch (e: Exception) {
                null
            }
        }

        private fun convertHit(hit: ObjectNode): ResultTableEntry {
            val propertiesMultiMap = ArrayListMultimap.create<String, String>()
            hit.fields()
                .forEach {
                    collectValues(
                        it.key,
                        it.value,
                        propertiesMultiMap
                    )
                }
            val propertiesMap = propertiesMultiMap.asMap().asSequence()
                .map {
                    if (it.key.startsWith("_source")) {
                        it.key.replaceFirst("_source.", "") to it.value
                    } else {
                        it.key to it.value
                    }
                }
                .toMap()
            return ResultTableEntry(propertiesMap)
        }

        private fun collectValues(key: String, jsonNode: JsonNode, values: ArrayListMultimap<String, String>) {
            if (jsonNode.isValueNode) {
                values.put(key, jsonNode.asText())
            } else if (jsonNode.isArray) {
                val items = StringJoiner(", ", "[", "]")
                for (item: JsonNode in (jsonNode as ArrayNode).asIterable()) {
                    if (item.isValueNode) {
                        items.add(item.asText())
                    } else if (item.isObject) {
                        collectValues(
                            key,
                            item,
                            values
                        )
                    }
                }
                if (items.length() > 2) {
                    values.put(key, items.toString())
                }
            } else if (jsonNode.isObject) {
                (jsonNode as ObjectNode).fields().forEach {
                    collectValues(
                        key + "." + it.key,
                        it.value,
                        values
                    )
                }
            }
        }
    }

    class ResultTableColumnInfo(name: String) : ColumnInfo<ResultTableEntry, String>(name) {

        override fun valueOf(item: ResultTableEntry?): String? {
            return item?.values?.get(name)?.joinToString(", ")
        }

    }

    class ResultTableEntry(val values: Map<String, Collection<String>>)

}