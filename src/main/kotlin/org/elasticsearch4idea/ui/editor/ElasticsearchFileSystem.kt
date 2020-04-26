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

import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.vfs.*
import com.intellij.openapi.vfs.newvfs.impl.NullVirtualFile
import org.jetbrains.annotations.NonNls

class ElasticsearchFileSystem : VirtualFileSystem(), NonPhysicalFileSystem {

    override fun getProtocol(): String {
        return PROTOCOL
    }

    fun openEditor(elasticsearchFile: ElasticsearchFile) {
        val fileEditorManager = FileEditorManager.getInstance(elasticsearchFile.project)
        fileEditorManager.openFile(elasticsearchFile, true)
        fileEditorManager.setSelectedEditor(elasticsearchFile, "ElasticsearchEditor")
    }

    //    Unused methods
    override fun findFileByPath(@NonNls path: String): VirtualFile? {
        return null
    }

    override fun refresh(asynchronous: Boolean) {}
    override fun refreshAndFindFileByPath(path: String): VirtualFile? {
        return null
    }

    override fun addVirtualFileListener(listener: VirtualFileListener) {}
    override fun removeVirtualFileListener(listener: VirtualFileListener) {}
    override fun deleteFile(requestor: Any, vFile: VirtualFile) {}
    override fun moveFile(requestor: Any, vFile: VirtualFile, newParent: VirtualFile) {}
    override fun renameFile(requestor: Any, vFile: VirtualFile, newName: String) {}
    override fun createChildFile(
        requestor: Any,
        vDir: VirtualFile,
        fileName: String
    ): VirtualFile {
        return NullVirtualFile.INSTANCE
    }

    override fun createChildDirectory(
        requestor: Any,
        vDir: VirtualFile,
        dirName: String
    ): VirtualFile {
        return NullVirtualFile.INSTANCE
    }

    override fun copyFile(
        requestor: Any,
        virtualFile: VirtualFile,
        newParent: VirtualFile,
        copyName: String
    ): VirtualFile {
        return NullVirtualFile.INSTANCE
    }

    override fun isReadOnly(): Boolean {
        return true
    }

    companion object {
        private const val PROTOCOL = "elasticsearch"

        val instance: ElasticsearchFileSystem?
            get() = VirtualFileManager.getInstance().getFileSystem(PROTOCOL) as ElasticsearchFileSystem?
    }
}