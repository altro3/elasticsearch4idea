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

import com.intellij.openapi.editor.colors.EditorColors
import com.intellij.openapi.editor.colors.EditorColorsManager
import java.awt.Color

object MyUIUtils {

    fun getResultTableHeaderColor(): Color {
        val color = EditorColorsManager.getInstance().globalScheme.defaultBackground
        return if (EditorColorsManager.getInstance().isDarkEditor) {
            Color(color.red + 16, color.green + 16, color.blue + 16)
        } else {
            Color(color.red - 16, color.green - 16, color.blue - 16)
        }
    }

    fun getTableGridColor(): Color {
        val color = EditorColorsManager.getInstance().globalScheme.getColor(EditorColors.INDENT_GUIDE_COLOR)
        if (color != null) {
            return color
        }
        return getPropertiesTableHeaderColor();
    }

    fun getPropertiesTableHeaderColor(): Color {
        return if (EditorColorsManager.getInstance().isDarkEditor) {
            EditorColorsManager.getInstance().globalScheme.getColor(EditorColors.GUTTER_BACKGROUND)!!.brighter()
        } else {
            EditorColorsManager.getInstance().globalScheme.getColor(EditorColors.GUTTER_BACKGROUND)!!
        }
    }

    fun getBottomPanelBackgroundColor(): Color {
        return EditorColorsManager.getInstance().globalScheme.getColor(EditorColors.GUTTER_BACKGROUND)!!
    }

    fun getSelectedLineColor(): Color {
        return EditorColorsManager.getInstance().globalScheme.getColor(EditorColors.CARET_ROW_COLOR)!!
    }

    fun getSelectedCellColor(): Color {
        return EditorColorsManager.getInstance().globalScheme.getColor(EditorColors.SELECTION_BACKGROUND_COLOR)!!
    }

    fun getEditorBackground(): Color {
        return EditorColorsManager.getInstance().globalScheme.defaultBackground
    }
}