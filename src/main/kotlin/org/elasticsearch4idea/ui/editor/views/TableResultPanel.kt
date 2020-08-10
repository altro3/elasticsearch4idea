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

import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.ui.JBUI
import org.elasticsearch4idea.ui.editor.model.ResponseContext
import java.awt.BorderLayout
import javax.swing.JPanel

class TableResultPanel : JPanel() {
    private var table: ResultTable? = null

    init {
        layout = BorderLayout()
        background = EditorColorsManager.getInstance().globalScheme.defaultBackground
    }

    fun updateResultTable(responseContext: ResponseContext): Boolean {
        if (!responseContext.isValidSearchRequest()) {
            return false
        }
        if (table == null) {
            table = ResultTable.createResultTable(responseContext)
            initPanel()
        } else {
            table!!.updateTable(responseContext)
        }
        return true
    }

    private fun initPanel() {
        val scrollPane = JBScrollPane(table)
        scrollPane.border = JBUI.Borders.empty()
        add(scrollPane, BorderLayout.CENTER)
    }

}