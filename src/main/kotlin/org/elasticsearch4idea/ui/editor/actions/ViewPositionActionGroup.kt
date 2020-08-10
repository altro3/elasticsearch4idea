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

import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.actionSystem.ToggleOptionAction
import com.intellij.openapi.project.DumbAware
import icons.Icons
import org.elasticsearch4idea.ui.editor.views.ElasticsearchPanel
import javax.swing.Icon

class ViewPositionActionGroup(private val elasticsearchPanel: ElasticsearchPanel) :
    DefaultActionGroup("View position", true),
    DumbAware {

    init {
        add(Action(Option(true), Icons.VERTICAL_VIEW))
        add(Action(Option(false), Icons.HORIZONTAL_VIEW))
    }

    class Action(option: Option, icon: Icon) : ToggleOptionAction(option, icon), DumbAware

    inner class Option(
        private val isVertical: Boolean
    ) : ToggleOptionAction.Option {

        override fun getName(): String? {
            return if (isVertical) "Vertically" else "Horizontally"
        }

        override fun setSelected(selected: Boolean) {
            elasticsearchPanel.setOrientation(isVertical)
        }

        override fun isSelected(): Boolean {
            return elasticsearchPanel.isVerticalOrientation() == isVertical
        }
    }
}