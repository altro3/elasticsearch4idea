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
import com.intellij.util.ObjectUtils
import com.intellij.util.ui.UIUtil
import java.awt.Color
import java.lang.Integer.max
import java.lang.Integer.min

object MyUIUtils {

    fun getResultTableHeaderColor(): Color {
        val color = EditorColorsManager.getInstance().globalScheme.defaultBackground
        return if (EditorColorsManager.getInstance().isDarkEditor) {
            Color(min(color.red + 16, 255), min(color.green + 16, 255), min(color.blue + 16, 255))
        } else {
            Color(max(0, color.red - 16), max(0, color.green - 16), max(0, color.blue - 16))
        }
    }

    fun getTableGridColor(): Color {
        return ObjectUtils.chooseNotNull(
            EditorColorsManager.getInstance().globalScheme.getColor(EditorColors.INDENT_GUIDE_COLOR),
            UIUtil.getTableGridColor()
        )
    }

    fun getPropertiesTableHeaderColor(): Color {
        val color = ObjectUtils.chooseNotNull(
            EditorColorsManager.getInstance().globalScheme.getColor(EditorColors.GUTTER_BACKGROUND),
            getResultTableHeaderColor()
        )
        return if (EditorColorsManager.getInstance().isDarkEditor) {
            color.brighter()
        } else {
            color
        }
    }

    fun getSelectedLineColor(): Color {
        return ObjectUtils.chooseNotNull(
            EditorColorsManager.getInstance().globalScheme.getColor(EditorColors.CARET_ROW_COLOR),
            UIUtil.getDecoratedRowColor()
        )
    }

    fun getSelectedCellColor(): Color {
        return ObjectUtils.chooseNotNull(
            EditorColorsManager.getInstance().globalScheme.getColor(EditorColors.SELECTION_BACKGROUND_COLOR),
            UIUtil.getTableSelectionBackground(true)
        )
    }

    fun getTableBackground(): Color {
        return ObjectUtils.chooseNotNull(
            EditorColorsManager.getInstance().globalScheme.defaultBackground,
            UIUtil.getTableBackground()
        )
    }
}