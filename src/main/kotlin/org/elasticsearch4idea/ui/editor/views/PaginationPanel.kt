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

import com.intellij.find.editorHeaderActions.Utils
import com.intellij.openapi.actionSystem.ActionToolbar
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.ui.JBColor
import com.intellij.util.ui.JBUI
import org.elasticsearch4idea.ui.editor.QueryManager
import org.elasticsearch4idea.ui.editor.actions.ChangePageSizeActionGroup
import org.elasticsearch4idea.ui.editor.actions.LoadTotal
import org.elasticsearch4idea.ui.editor.actions.PageActions
import org.elasticsearch4idea.ui.editor.actions.ReloadPageAction
import org.elasticsearch4idea.ui.editor.model.PageModel
import org.elasticsearch4idea.utils.MyActionToolbarImpl
import java.awt.BorderLayout
import javax.swing.JPanel
import javax.swing.border.EmptyBorder

class PaginationPanel(
    elasticsearchPanel: ElasticsearchPanel,
    queryManager: QueryManager,
    pageModel: PageModel
) : JPanel() {

    init {
        layout = BorderLayout()
        val group = DefaultActionGroup()
        group.add(PageActions.FirstPage(queryManager, pageModel))
        group.add(PageActions.PreviousPage(queryManager, pageModel))
        group.add(ChangePageSizeActionGroup(queryManager, pageModel))
        group.add(LoadTotal(queryManager, pageModel))
        group.add(PageActions.NextPage(queryManager, pageModel))
        group.add(PageActions.LastPage(queryManager, pageModel))
        group.addSeparator()
        group.add(ReloadPageAction(queryManager, pageModel, elasticsearchPanel))
        val actionToolBar = MyActionToolbarImpl("ElasticsearchPaginationToolBar", group, true)
        actionToolBar.setTargetComponent(this)
        actionToolBar.layoutPolicy = ActionToolbar.AUTO_LAYOUT_POLICY
        Utils.setSmallerFontForChildren(actionToolBar)

        actionToolBar.border = EmptyBorder(0, 0, 0, 0)
        border = JBUI.Borders.customLine(JBColor.border(), 1, 0, 0, 0)

        add(actionToolBar.component, BorderLayout.WEST)
    }
}