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

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareAction
import org.elasticsearch4idea.ui.editor.QueryManager
import org.elasticsearch4idea.ui.editor.model.PageModel

object PageActions {

    class NextPage(
        private val queryManager: QueryManager,
        private val pageModel: PageModel
    ) : DumbAwareAction("Next Page", "Next Page", AllIcons.Actions.Play_forward) {

        override fun actionPerformed(e: AnActionEvent) {
            pageModel.nextPage()
            queryManager.updateAndExecuteLastSearchRequest(pageModel) {
                update(e)
            }
        }

        override fun update(event: AnActionEvent) {
            event.presentation.isEnabled = !pageModel.isLastPage()
        }
    }

    class PreviousPage(
        private val queryManager: QueryManager,
        private val pageModel: PageModel
    ) : DumbAwareAction("Previous Page", "Previous Page", AllIcons.Actions.Play_back) {

        override fun actionPerformed(e: AnActionEvent) {
            pageModel.previousPage()
            queryManager.updateAndExecuteLastSearchRequest(pageModel) {
                update(e)
            }
        }

        override fun update(event: AnActionEvent) {
            event.presentation.isEnabled = !pageModel.isFirstPage()
        }
    }

    class LastPage(
        private val queryManager: QueryManager,
        private val pageModel: PageModel
    ) : DumbAwareAction("Last Page", "Last Page", AllIcons.Actions.Play_last) {

        override fun actionPerformed(e: AnActionEvent) {
            pageModel.lastPage()
            queryManager.updateAndExecuteLastSearchRequest(pageModel) {
                update(e)
            }
        }

        override fun update(event: AnActionEvent) {
            event.presentation.isEnabled = !pageModel.isLastPage()
        }
    }

    class FirstPage(
        private val queryManager: QueryManager,
        private val pageModel: PageModel
    ) : DumbAwareAction("First Page", "First Page", AllIcons.Actions.Play_first) {

        override fun actionPerformed(e: AnActionEvent) {
            pageModel.firstPage()
            queryManager.updateAndExecuteLastSearchRequest(pageModel) {
                update(e)
            }
        }

        override fun update(event: AnActionEvent) {
            event.presentation.isEnabled = !pageModel.isFirstPage()
        }
    }
}