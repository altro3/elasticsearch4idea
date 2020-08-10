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

package org.elasticsearch4idea.utils

import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.Presentation
import com.intellij.openapi.actionSystem.ex.ActionButtonLook
import com.intellij.openapi.actionSystem.impl.ActionButton
import com.intellij.openapi.actionSystem.impl.ActionToolbarImpl
import java.awt.Dimension

class MyActionToolbarImpl(place: String, actionGroup: ActionGroup, horizontal: Boolean) :
    ActionToolbarImpl(place, actionGroup, horizontal) {

    override fun createToolbarButton(
        action: AnAction,
        look: ActionButtonLook?,
        place: String,
        presentation: Presentation,
        minimumSize: Dimension
    ): ActionButton {
        val toolbarButton = super.createToolbarButton(action, look, place, presentation, minimumSize)
        presentation.putClientProperty(action::class.simpleName!! + "Component", toolbarButton)
        return toolbarButton
    }
}