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
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.editor.colors.EditorFontType
import com.intellij.ui.table.TableView
import com.intellij.util.ui.ColumnInfo
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.ListTableModel
import org.elasticsearch4idea.ui.editor.model.Hit
import org.elasticsearch4idea.ui.editor.model.Mapping
import org.elasticsearch4idea.ui.editor.model.MappingNode
import org.elasticsearch4idea.ui.editor.table.NumberColumnCellRenderer
import org.elasticsearch4idea.ui.editor.table.ResultTableCellRenderer
import org.elasticsearch4idea.ui.editor.table.ResultTableHeaderRenderer
import org.elasticsearch4idea.utils.MyUIUtils
import org.elasticsearch4idea.utils.TableColumnModelListenerAdapter
import java.awt.Component
import java.awt.Font
import javax.swing.JTable
import javax.swing.event.ListSelectionEvent
import javax.swing.table.TableCellRenderer
import javax.swing.table.TableColumn
import kotlin.math.max
import kotlin.math.min


class ResultTable internal constructor(
    columns: Array<ColumnInfo<ResultTableEntry, *>>,
    entries: List<ResultTableEntry>,
    val label: String
) : TableView<ResultTable.ResultTableEntry>(ListTableModel(columns, entries)) {

    init {
        setAutoResizeMode(JTable.AUTO_RESIZE_OFF)
        setCellSelectionEnabled(true)

        val colorsScheme = EditorColorsManager.getInstance().globalScheme
        val font: Font = colorsScheme.getFont(EditorFontType.PLAIN)
        setFont(font)
        getTableHeader().defaultRenderer = ResultTableHeaderRenderer()
        getTableHeader().font = font
        getTableHeader().background = MyUIUtils.getResultTableHeaderColor()
        getTableHeader().foreground = colorsScheme.defaultForeground
        getTableHeader().border = JBUI.Borders.customLine(MyUIUtils.getResultTableGridColor(), 1)
        gridColor = MyUIUtils.getResultTableGridColor()
        selectionForeground = null
        columnModel.addColumnModelListener(object : TableColumnModelListenerAdapter() {

            override fun columnSelectionChanged(e: ListSelectionEvent?) {
                if (e == null) {
                    return
                }
                getTableHeader().repaint(getTableHeader().getHeaderRect(e.firstIndex))
                getTableHeader().repaint(getTableHeader().getHeaderRect(e.lastIndex))
            }
        })
        adjustColumnsBySize()
    }

    private fun adjustColumnsBySize() {
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

        fun createResultTable(result: String, mappingJson: String?): ResultTable? {
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
                    .map { Hit.create(it as ObjectNode) }
                    .mapIndexed { index, hit -> ResultTableEntry(index + 1, hit) }
                    .toList()

                val mappings = Mapping.parseMappings(mappingJson!!)
                val columns = createColumns(mappings, entries)


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

        private fun createColumns(
            mappings: List<Mapping>,
            entries: List<ResultTableEntry>
        ): List<ColumnInfo<ResultTableEntry, *>> {
            val columns = mutableListOf<ColumnInfo<ResultTableEntry, *>>()
            columns.add(NumbersColumnInfo())
            columns.add(ResultTableColumnInfo("_index", null, Hit::index))
            columns.add(ResultTableColumnInfo("_type", null, Hit::type))
            columns.add(ResultTableColumnInfo("_id", null, Hit::id))
            columns.add(ResultTableColumnInfo("_score", null, Hit::score))
            val columnNames = ArrayListMultimap.create<String, Pair<Mapping, MappingNode>>()
            mappings.forEach { mapping ->
                mapping.nodes.forEach {
                    collectColumns(it.key, it.value, mapping, columnNames)
                }
            }
            columnNames.asMap().forEach { (key, collection) ->
                val hasValue = entries.any { it.hit.values.contains(key) }
                if (hasValue) {
                    columns.add(ResultTableColumnInfo(key, collection) {
                        it.values.get(key)
                    })
                }
            }
            return columns
        }

        private fun collectColumns(
            field: String,
            node: MappingNode,
            mapping: Mapping,
            columns: ArrayListMultimap<String, Pair<Mapping, MappingNode>>
        ) {
            if (node.children.isNullOrEmpty()) {
                columns.put(field, Pair(mapping, node))
            } else {
                node.children.forEach {
                    collectColumns("$field.${it.key}", it.value, mapping, columns)
                }
            }
        }
    }

    class ResultTableColumnInfo(
        name: String,
        private val mappingNodes: Collection<Pair<Mapping, MappingNode>>?,
        private val valueExtractor: (Hit) -> Any?
    ) :
        ColumnInfo<ResultTableEntry, Any?>(name) {

        override fun valueOf(item: ResultTableEntry): Any? {
            return valueExtractor.invoke(item.hit)
        }

        override fun getRenderer(item: ResultTableEntry): TableCellRenderer {
            return ResultTableCellRenderer.instance
        }

        override fun getTooltipText(): String? {
            if (mappingNodes.isNullOrEmpty()) {
                return null
            } else {
                val indexesByType = mappingNodes.asSequence()
                    .filterNot { it.second.type == null }
                    .groupBy({ it.second.type!! }, { it.first.index })
                if (mappingNodes.size == 1 || indexesByType.size == 1) {
                    return "<html><b>$name</b>: ${mappingNodes.first().second.type}</html>"
                }
                val stringBuilder = StringBuilder()
                stringBuilder.append("<html>")
                stringBuilder.append("<b>$name</b><br>")
                indexesByType.forEach {
                    val indexes = it.value.asSequence().joinToString()
                    stringBuilder.append("($indexes): ${it.key}<br>")
                }
                stringBuilder.append("</html>")
                return stringBuilder.toString()
            }
        }
    }

    class NumbersColumnInfo : ColumnInfo<ResultTableEntry, Any?>("") {

        override fun valueOf(item: ResultTableEntry): Any? {
            return item.number
        }

        override fun getRenderer(item: ResultTableEntry): TableCellRenderer {
            return NumberColumnCellRenderer.instance
        }

    }

    class ResultTableEntry(
        val number: Int,
        val hit: Hit
    )

}