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
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.editor.ex.util.LexerEditorHighlighter
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileTypes.SyntaxHighlighterFactory
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFile
import com.intellij.psi.codeStyle.CodeStyleManager
import com.intellij.testFramework.LightVirtualFile
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.ui.JBUI
import org.elasticsearch4idea.model.ViewMode
import java.awt.BorderLayout
import javax.swing.JPanel

class ResultPanel(
    private val project: Project,
    private var currentViewMode: ViewMode
) : JPanel(), Disposable {
    private val editor: Editor
    private val editorDocument: Document
    private val psiFile: PsiFile

    init {
        val file = LightVirtualFile("result.json", JsonFileType.INSTANCE, "")
        editorDocument = FileDocumentManager.getInstance().getDocument(file)!!
        psiFile = PsiDocumentManager.getInstance(project).getPsiFile(editorDocument)!!

        editor = createEditor()
        layout = BorderLayout()
    }

    fun setCurrentViewMode(viewMode: ViewMode) {
        currentViewMode = viewMode
    }

    fun getCurrentViewMode(): ViewMode {
        return currentViewMode
    }

    fun updateResult(result: String) {
        invalidate()
        removeAll()

        when (currentViewMode) {
            ViewMode.TEXT -> {
                add(editor.component, BorderLayout.CENTER)
                updateEditorText(result)
            }
            ViewMode.TABLE -> {
                val table = ResultTable.createResultTable(result)
                if (table == null) {
                    add(editor.component, BorderLayout.CENTER)
                    updateEditorText(result)
                } else {
                    val panel = JPanel(BorderLayout())
                    panel.add(JBLabel("  " + table.label), BorderLayout.NORTH)
                    val scrollPane = JBScrollPane(table)
                    scrollPane.border = JBUI.Borders.empty()
                    panel.add(scrollPane, BorderLayout.CENTER)
                    add(panel)
                }
            }
        }
        validate()
    }

    private fun createEditor(): Editor {
        val editor = EditorFactory.getInstance()
            .createEditor(editorDocument, project, JsonFileType.INSTANCE, true) as EditorEx

        val language = Language.findLanguageByID("JSON")!!
        val highlighter = LexerEditorHighlighter(
            SyntaxHighlighterFactory.getSyntaxHighlighter(language, null, null),
            editor.colorsScheme
        )
        editor.highlighter = highlighter
        return editor
    }

    private fun updateEditorText(text: String) {
        editorDocument.setText(text)
        PsiDocumentManager.getInstance(project).commitDocument(editorDocument)
        CodeStyleManager.getInstance(project).reformatText(psiFile, 0, psiFile.textRange.endOffset)
    }

    override fun dispose() {
        EditorFactory.getInstance().releaseEditor(editor)
    }
}