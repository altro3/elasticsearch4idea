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
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.ui.JBUI
import org.elasticsearch4idea.ui.editor.model.Hit
import org.elasticsearch4idea.ui.editor.model.Mapping
import org.elasticsearch4idea.ui.editor.model.TableModel
import java.awt.BorderLayout
import javax.swing.JPanel

class TableResultPanel : JPanel() {
    private var table: ResultTable? = null
    private val objectMapper = jacksonObjectMapper()

    init {
        layout = BorderLayout()
        background = EditorColorsManager.getInstance().globalScheme.defaultBackground
    }

    fun updateResultTable(result: String, mappingJson: String?): Boolean {
        if (mappingJson == null) {
            return false
        }
        val tableModel = createTableModel(result, mappingJson) ?: return false
        if (table == null) {
            table = ResultTable.createResultTable(tableModel)
            val scrollPane = JBScrollPane(table)
            scrollPane.border = JBUI.Borders.empty()
            add(scrollPane, BorderLayout.CENTER)
        } else {
            table!!.updateTable(tableModel)
        }
        return true
    }


    private fun createTableModel(result: String, mappingJson: String?): TableModel? {
        return try {
            val rootNode: JsonNode = objectMapper.readValue(result)
            val hitsNode = rootNode.get("hits") ?: return null
            val totalShards = rootNode.get("_shards")?.get("total")?.asInt() ?: 0
            val successfulShards = rootNode.get("_shards")?.get("successful")?.asInt() ?: 0
            val took = (rootNode.get("took")?.asInt() ?: 0) / 1000f
            val tookString = String.format("%.3f", took)
            val totalNode = hitsNode.get("total")
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
            val hits = (hitsNode.get("hits") as ArrayNode).asIterable().asSequence()
                .map { Hit.create(it as ObjectNode) }
                .toList()
            val mappings = Mapping.parseMappings(mappingJson!!)
            return TableModel(hits, mappings)
        } catch (e: Exception) {
            null
        }
    }

}