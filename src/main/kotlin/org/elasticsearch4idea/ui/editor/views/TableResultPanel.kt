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
import org.apache.http.client.utils.URIBuilder
import org.elasticsearch4idea.ui.editor.QueryManager
import org.elasticsearch4idea.ui.editor.RequestAndResponse
import org.elasticsearch4idea.ui.editor.model.Hit
import org.elasticsearch4idea.ui.editor.model.Mapping
import org.elasticsearch4idea.ui.editor.model.PageModel
import org.elasticsearch4idea.ui.editor.model.ResultModel
import java.awt.BorderLayout
import javax.swing.JPanel

class TableResultPanel(
    private val elasticsearchPanel: ElasticsearchPanel,
    private val queryManager: QueryManager
) : JPanel() {
    private var table: ResultTable? = null
    private val objectMapper = jacksonObjectMapper()
    private val pageModel = PageModel(0, 20, 0, 0)

    init {
        layout = BorderLayout()
        background = EditorColorsManager.getInstance().globalScheme.defaultBackground
    }

    fun updateResultTable(requestAndResponse: RequestAndResponse): Boolean {
        if (!requestAndResponse.isSearchRequest()) {
            return false
        }
        val tableModel = createTableModel(requestAndResponse) ?: return false
        pageModel.update(tableModel, requestAndResponse.isNewRequest)
        if (table == null) {
            table = ResultTable.createResultTable(tableModel)
            initPanel()
        } else {
            table!!.updateTable(tableModel)
        }
        return true
    }

    private fun initPanel() {
        val scrollPane = JBScrollPane(table)
        scrollPane.border = JBUI.Borders.empty()
        add(scrollPane, BorderLayout.CENTER)
        val paginationPanel = PaginationPanel(elasticsearchPanel, queryManager, pageModel)
        add(paginationPanel, BorderLayout.SOUTH)
    }

    private fun createTableModel(requestAndResponse: RequestAndResponse): ResultModel? {
        return try {
            val rootNode: JsonNode = objectMapper.readValue(requestAndResponse.response)
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
            val mappings = Mapping.parseMappings(requestAndResponse.mappingResponse!!)

            val queryParams = URIBuilder(requestAndResponse.request.path)
                .queryParams.associateBy { it.name }
            val jsonNode = try {
                objectMapper.readTree(requestAndResponse.request.body)
            } catch (e: Exception) {
                null
            }
            var from = queryParams.get("from")?.value?.toLongOrNull()
                ?: jsonNode?.get("from")?.asLong() ?: 0
            if (from < 0) {
                from = 0
            }
            var size = queryParams.get("size")?.value?.toLongOrNull()
                ?: jsonNode?.get("size")?.asLong() ?: hits.size.toLong()
            if (size < 0) {
                size = hits.size.toLong()
            }
            
            return ResultModel(hits, mappings, total, from, size)
        } catch (e: Exception) {
            null
        }
    }

}