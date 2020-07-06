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
package org.elasticsearch4idea.ui.editor.table

import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.ui.SimpleTextAttributes
import java.awt.Font

object StyleAttributesProvider {

    fun getUnsetAttributes() = getSimpleTextAttributes(DefaultLanguageHighlighterColors.LINE_COMMENT, Font.ITALIC)

    fun getNumberAttributes() = getSimpleTextAttributes(DefaultLanguageHighlighterColors.NUMBER)

    fun getKeywordAttributes() = getSimpleTextAttributes(DefaultLanguageHighlighterColors.KEYWORD)

    fun getStringAttributes() = getSimpleTextAttributes(DefaultLanguageHighlighterColors.STRING)

    fun getIdentifierAttributes() = getSimpleTextAttributes(DefaultLanguageHighlighterColors.IDENTIFIER)

    fun getBracesAttributes() = getSimpleTextAttributes(DefaultLanguageHighlighterColors.BRACES)

    private fun getSimpleTextAttributes(
        textAttributesKey: TextAttributesKey,
        fontStyle: Int = Font.PLAIN
    ): SimpleTextAttributes {
        val textAttributes =
            EditorColorsManager.getInstance().globalScheme.getAttributes(textAttributesKey)
        return SimpleTextAttributes(fontStyle, textAttributes.foregroundColor)
    }
}