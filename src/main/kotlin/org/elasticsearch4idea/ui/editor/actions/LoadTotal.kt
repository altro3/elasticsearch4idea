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

package org.elasticsearch4idea.ui.editor.actions

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.ex.CustomComponentAction
import com.intellij.openapi.actionSystem.impl.ActionButtonWithText
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.roots.ui.configuration.actions.IconWithTextAction
import com.intellij.util.ui.UIUtil
import org.elasticsearch4idea.ui.editor.QueryManager
import org.elasticsearch4idea.ui.editor.model.PageModel


class LoadTotal(
    private val queryManager: QueryManager,
    private val pageModel: PageModel
) :
    IconWithTextAction(
        "of ${pageModel.getDisplayedTotal()}",
        "Click to get count for this query by GET /_count API",
        null
    ), DumbAware {

    override fun actionPerformed(e: AnActionEvent) {
        queryManager.executeCountForLastSearchRequest {
            pageModel.updateDisplayedTotal(it)
            val component =
                e.presentation.getClientProperty(CustomComponentAction.COMPONENT_KEY) as ActionButtonWithText
            UIUtil.invokeLaterIfNeeded {
                component.updateUI()
            }
        }
    }

    override fun useSmallerFontForTextInToolbar(): Boolean {
        return true
    }

    override fun update(e: AnActionEvent) {
        e.presentation.isVisible = pageModel.getDisplayedTotal() >= 0
        e.presentation.text = "of ${pageModel.getDisplayedTotal()}"
    }
}
    