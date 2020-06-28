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
package org.elasticsearch4idea.ui.explorer.table

import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.editor.colors.EditorFontType
import com.intellij.ui.table.TableView
import com.intellij.util.ui.ColumnInfo
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.ListTableModel
import org.elasticsearch4idea.utils.MyUIUtils
import java.awt.Font
import javax.swing.table.JTableHeader

class ElasticsearchInfosTable internal constructor() : TableView<TableEntry>(
    ListTableModel(
        KeyColumnInfo(),
        ValueColumnInfo()
    )
) {

    init {
        val font: Font = EditorColorsManager.getInstance().globalScheme.getFont(EditorFontType.PLAIN)
        setFont(font)
    }

    override fun getTableHeader(): JTableHeader {
        val header = super.getTableHeader()
        header.background = MyUIUtils.getTableHeaderColor()
        header.border = JBUI.Borders.customLine(JBUI.CurrentTheme.ToolWindow.borderColor(), 1, 0, 0, 0)
        return header
    }

    fun updateInfos(collectionInfoEntries: List<TableEntry>) {
        (model as ListTableModel<*>).items = collectionInfoEntries
    }

    private class KeyColumnInfo : ColumnInfo<TableEntry, String>("Property") {

        override fun valueOf(tableEntry: TableEntry): String {
            return tableEntry.key
        }
    }

    private class ValueColumnInfo : ColumnInfo<TableEntry, String>("Value") {
        override fun valueOf(tableEntry: TableEntry): String {
            return tableEntry.value ?: ""
        }
    }
}