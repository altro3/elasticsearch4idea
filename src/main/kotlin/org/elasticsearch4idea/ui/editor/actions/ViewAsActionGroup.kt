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
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.actionSystem.ToggleOptionAction
import com.intellij.openapi.components.service
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import org.elasticsearch4idea.model.ViewMode
import org.elasticsearch4idea.service.ElasticsearchConfiguration
import org.elasticsearch4idea.ui.editor.views.ElasticsearchPanel

class ViewAsActionGroup(
    private val elasticsearchPanel: ElasticsearchPanel,
    project: Project
) : DefaultActionGroup("Auto-refresh clusters", true), DumbAware {

    private val elasticsearchConfiguration = project.service<ElasticsearchConfiguration>()

    init {
        templatePresentation.icon = AllIcons.Actions.Show
        templatePresentation.text = "View as"
        ViewMode.values().forEach {
            add(Action(Option(it)))
        }
    }

    class Action(option: Option) : ToggleOptionAction(option), DumbAware

    inner class Option(
        private val option: ViewMode
    ) : ToggleOptionAction.Option {

        override fun getName(): String? {
            return option.text
        }

        override fun setSelected(selected: Boolean) {
            this@ViewAsActionGroup.elasticsearchConfiguration.viewMode = option
            elasticsearchPanel.setViewMode(option)
        }

        override fun isSelected(): Boolean {
            return this@ViewAsActionGroup.elasticsearchConfiguration.viewMode == option
        }

    }
}