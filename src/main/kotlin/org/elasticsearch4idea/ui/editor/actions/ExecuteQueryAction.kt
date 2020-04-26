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
import org.elasticsearch4idea.ui.editor.views.ElasticsearchPanel
import java.awt.event.InputEvent
import java.awt.event.KeyEvent

class ExecuteQueryAction(private val elasticsearchPanel: ElasticsearchPanel) :
    DumbAwareAction("Execute query", "Execute query", AllIcons.Actions.Execute) {

    override fun actionPerformed(anActionEvent: AnActionEvent) {
        elasticsearchPanel.executeQuery()
    }

    override fun update(event: AnActionEvent) {
    }

    init {
        registerCustomShortcutSet(KeyEvent.VK_ENTER, InputEvent.CTRL_MASK, elasticsearchPanel)
    }
}