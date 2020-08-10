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

import com.intellij.util.ui.JBUI
import org.elasticsearch4idea.utils.MyUIUtils
import sun.swing.table.DefaultTableCellHeaderRenderer
import java.awt.Component
import javax.swing.JLabel
import javax.swing.JTable
import javax.swing.SwingConstants

class NumberColumnCellRenderer : DefaultTableCellHeaderRenderer() {

    override fun getTableCellRendererComponent(
        table: JTable?,
        value: Any?,
        isSelected: Boolean,
        hasFocus: Boolean,
        row: Int,
        column: Int
    ): Component? {
        val cmp: Component = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column)
        if (cmp !is JLabel) return cmp
        cmp.horizontalAlignment = SwingConstants.CENTER
        var border = JBUI.Borders.customLine(MyUIUtils.getEditorBackground(), 1, 0, 0, 0)
        val indent = JBUI.Borders.empty(0, 8)
        border = JBUI.Borders.merge(border, indent, false)
        cmp.border = border
        if (table?.isRowSelected(row) == true) {
            cmp.background = MyUIUtils.getSelectedLineColor()
        }
        return cmp
    }

    companion object {
        val instance = NumberColumnCellRenderer()
    }
}