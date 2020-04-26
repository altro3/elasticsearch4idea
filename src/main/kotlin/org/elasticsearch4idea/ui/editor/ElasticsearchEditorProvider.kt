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

import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorPolicy
import com.intellij.openapi.fileEditor.FileEditorProvider
import com.intellij.openapi.fileEditor.FileEditorState
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import org.jdom.Element

class ElasticsearchEditorProvider : FileEditorProvider, DumbAware {

    override fun accept(project: Project, file: VirtualFile): Boolean {
        return file is ElasticsearchFile
    }

    override fun createEditor(project: Project, file: VirtualFile): FileEditor {
        val elasticsearchFile = file as ElasticsearchFile
        return ElasticsearchEditor(project, elasticsearchFile)
    }

    override fun disposeEditor(editor: FileEditor) {
        editor.dispose()
    }

    override fun readState(
        sourceElement: Element,
        project: Project,
        file: VirtualFile
    ): FileEditorState {
        return FileEditorState.INSTANCE
    }

    override fun writeState(
        state: FileEditorState,
        project: Project,
        targetElement: Element
    ) {
    }

    override fun getEditorTypeId(): String {
        return "ElasticsearchEditor"
    }

    override fun getPolicy(): FileEditorPolicy {
        return FileEditorPolicy.HIDE_DEFAULT_EDITOR
    }
}