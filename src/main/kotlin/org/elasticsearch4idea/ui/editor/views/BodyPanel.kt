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
package org.elasticsearch4idea.ui.editor.views

import com.intellij.json.JsonFileType
import com.intellij.lang.Language
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.editor.ex.util.LexerEditorHighlighter
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileTypes.SyntaxHighlighterFactory
import com.intellij.openapi.project.Project
import com.intellij.testFramework.LightVirtualFile
import java.awt.BorderLayout
import javax.swing.JPanel

class BodyPanel(
    private val project: Project
) : JPanel(), Disposable {
    private val editor: Editor
    private val editorDocument: Document

    init {
        val file = LightVirtualFile("body.json", JsonFileType.INSTANCE, "")
        editorDocument = FileDocumentManager.getInstance().getDocument(file)!!
        editor = createEditor()

        layout = BorderLayout()
        add(editor.component, BorderLayout.CENTER)
    }

    private fun createEditor(): Editor {
        val editor = EditorFactory.getInstance().createEditor(editorDocument, project) as EditorEx

        val language = Language.findLanguageByID("JSON")!!
        val highlighter = LexerEditorHighlighter(
            SyntaxHighlighterFactory.getSyntaxHighlighter(language, null, null),
            editor.colorsScheme
        )
        editor.highlighter = highlighter
        return editor
    }

    override fun dispose() {
        EditorFactory.getInstance().releaseEditor(editor)
    }

    fun updateQueryView(text: String) {
        ApplicationManager.getApplication().runWriteAction {
            editorDocument.setText(text)
        }
    }

    fun getBody(): String {
        return editorDocument.text
    }
}