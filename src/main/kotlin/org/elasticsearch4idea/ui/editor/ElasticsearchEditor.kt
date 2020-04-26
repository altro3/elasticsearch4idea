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
package org.elasticsearch4idea.ui.editor

import com.intellij.codeHighlighting.BackgroundEditorHighlighter
import com.intellij.ide.structureView.StructureViewBuilder
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorLocation
import com.intellij.openapi.fileEditor.FileEditorState
import com.intellij.openapi.fileEditor.FileEditorStateLevel
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.UserDataHolderBase
import org.elasticsearch4idea.ui.editor.views.ElasticsearchPanel
import java.beans.PropertyChangeListener
import javax.swing.JComponent
import javax.swing.JPanel

internal class ElasticsearchEditor(
    project: Project,
    elasticsearchFile: ElasticsearchFile
) : UserDataHolderBase(), FileEditor {
    private var panel: ElasticsearchPanel?
    private var isDisposed = false

    init {
        panel = ElasticsearchPanel(project, elasticsearchFile)
        ApplicationManager.getApplication().invokeLater { panel!!.showResults() }
    }

    override fun getComponent(): JComponent {
        return (if (isDisposed) JPanel() else panel)!!
    }

    override fun getPreferredFocusedComponent(): JComponent? {
        return panel?.getResultPanel()
    }

    override fun getName(): String {
        return "Elasticsearch Editor"
    }

    override fun dispose() {
        if (!isDisposed) {
            panel!!.dispose()
            panel = null
            isDisposed = true
        }
    }

    override fun getState(level: FileEditorStateLevel): FileEditorState {
        return FileEditorState.INSTANCE
    }

    override fun setState(state: FileEditorState) {}
    override fun isModified(): Boolean {
        return false
    }

    override fun isValid(): Boolean {
        return true
    }

    override fun selectNotify() {}
    override fun deselectNotify() {}
    override fun addPropertyChangeListener(listener: PropertyChangeListener) {}
    override fun removePropertyChangeListener(listener: PropertyChangeListener) {}
    override fun getBackgroundHighlighter(): BackgroundEditorHighlighter? {
        return null
    }

    override fun getCurrentLocation(): FileEditorLocation? {
        return null
    }

    override fun getStructureViewBuilder(): StructureViewBuilder? {
        return null
    }
}