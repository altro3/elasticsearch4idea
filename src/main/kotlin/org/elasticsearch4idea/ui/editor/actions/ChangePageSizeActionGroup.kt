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
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.actionSystem.ToggleOptionAction
import com.intellij.openapi.actionSystem.impl.ActionButtonWithText
import com.intellij.openapi.project.DumbAware
import com.intellij.util.ui.UIUtil
import org.elasticsearch4idea.ui.editor.QueryManager
import org.elasticsearch4idea.ui.editor.model.PageModel
import java.lang.Long.min

class ChangePageSizeActionGroup(
    private val queryManager: QueryManager,
    private val pageModel: PageModel
) : DefaultActionGroup("${pageModel.getDisplayedPageStart()}-${pageModel.getDisplayedPageEnd()}", true), DumbAware {

    private var component: ActionButtonWithText? = null

    init {
        templatePresentation.description = "Change page size"
        setActions(pageModel)

        pageModel.addListener(object : PageModel.PageModelListener {
            override fun onModelChanged(pageModel: PageModel) {
                setActions(pageModel)
            }
        })
    }

    override fun update(e: AnActionEvent) {
        e.presentation.text = "${pageModel.getDisplayedPageStart()}-${pageModel.getDisplayedPageEnd()}"
        if (component == null) {
            component = e.presentation.getClientProperty(ChangePageSizeActionGroup::class.simpleName!! + "Component")
                    as? ActionButtonWithText
        }
    }

    override fun displayTextInToolbar(): Boolean {
        return true
    }

    override fun useSmallerFontForTextInToolbar(): Boolean {
        return true
    }

    private fun setActions(pageModel: PageModel) {
        this.removeAll()
        val sizes = DEFAULT_PAGE_SIZES.toMutableSet()
        sizes.add(pageModel.getPageSize() / 2)
        sizes.add(pageModel.getPageSize())
        sizes.add(pageModel.getPageSize() * 2)

        sizes.asSequence()
            .map { min(it, pageModel.getTotal()) }
            .filter { it > 0 }
            .filter { it != pageModel.getTotal() }
            .sorted()
            .forEach {
                add(Action(PageSizeOption(queryManager, pageModel, it)))
            }
        add(Action(AllOption(queryManager, pageModel)))
    }

    class Action(option: Option) : ToggleOptionAction(option), DumbAware

    inner class PageSizeOption(
        private val queryManager: QueryManager,
        private val pageModel: PageModel,
        private val size: Long
    ) : ToggleOptionAction.Option {

        override fun getName(): String {
            return size.toString()
        }

        override fun setSelected(selected: Boolean) {
            pageModel.setPageSize(size)
            queryManager.updateAndExecuteLastSearchRequest(pageModel) {
                UIUtil.invokeLaterIfNeeded {
                    component?.update()
                }
            }
        }

        override fun isSelected(): Boolean {
            return pageModel.getPageSize() == size
        }

    }

    inner class AllOption(
        private val queryManager: QueryManager,
        private val pageModel: PageModel
    ) : ToggleOptionAction.Option {

        override fun getName(): String {
            return "All"
        }

        override fun setSelected(selected: Boolean) {
            pageModel.setPageSize(pageModel.getTotal())
            pageModel.setPageStart(0)
            queryManager.updateAndExecuteLastSearchRequest(pageModel) {
                UIUtil.invokeLaterIfNeeded {
                    component?.update()
                }
            }
        }

        override fun isSelected(): Boolean {
            return pageModel.getPageSize() == pageModel.getTotal()
        }

    }

    companion object {
        private val DEFAULT_PAGE_SIZES: List<Long> = listOf(10, 100, 500, 1000)
    }
}