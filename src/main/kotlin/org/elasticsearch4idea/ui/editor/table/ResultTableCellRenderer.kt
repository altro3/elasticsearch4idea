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

package org.elasticsearch4idea.ui.editor.table

import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.ui.ColoredTableCellRenderer
import com.intellij.ui.SimpleTextAttributes
import org.elasticsearch4idea.utils.MyUIUtils
import javax.swing.JTable
import javax.swing.SwingConstants
import javax.swing.border.EmptyBorder

class ResultTableCellRenderer : ColoredTableCellRenderer() {

    override fun customizeCellRenderer(
        table: JTable?,
        value: Any?,
        selected: Boolean,
        hasFocus: Boolean,
        row: Int,
        column: Int
    ) {
        border = EMPTY_BORDER
        background = when {
            table?.isCellSelected(row, column) == true -> MyUIUtils.getSelectedCellColor()
            table?.isRowSelected(row) == true -> MyUIUtils.getSelectedLineColor()
            else -> EditorColorsManager.getInstance().globalScheme.defaultBackground
        }

        if (value == null) {
            appendInternal("<unset>", StyleAttributesProvider.getUnsetAttributes())
            return
        }
        val values = value as Collection<Any?>
        if (values.size > 1) {
            appendInternal("[", StyleAttributesProvider.getBracesAttributes())
            values.asSequence().forEachIndexed { index, it ->
                writeValue(it)
                if (index != values.size - 1) {
                    appendInternal(", ", StyleAttributesProvider.getKeywordAttributes())
                }
            }
            appendInternal("]", StyleAttributesProvider.getBracesAttributes())
        } else {
            writeValue(values.firstOrNull())
        }
    }

    private fun writeValue(value: Any?) {
        when (value) {
            null -> {
                appendInternal("null", StyleAttributesProvider.getKeywordAttributes(), SwingConstants.RIGHT)
            }
            is Number -> {
                appendInternal(value.toString(), StyleAttributesProvider.getNumberAttributes(), SwingConstants.RIGHT)
            }
            is Boolean -> {
                appendInternal(value.toString(), StyleAttributesProvider.getKeywordAttributes(), SwingConstants.RIGHT)
            }
            else -> {
                appendInternal(value.toString(), StyleAttributesProvider.getIdentifierAttributes())
            }
        }
    }

    private fun appendInternal(fragment: String, attributes: SimpleTextAttributes, align: Int = SwingConstants.LEFT) {
        foreground = attributes.fgColor
        setTextAlign(align)
        append(fragment, attributes)
    }

    companion object {
        val instance = ResultTableCellRenderer()
        private val EMPTY_BORDER = EmptyBorder(1, 1, 1, 1)
    }
}