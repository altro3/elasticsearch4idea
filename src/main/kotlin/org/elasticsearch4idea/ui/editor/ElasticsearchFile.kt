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

import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileSystem
import com.intellij.openapi.vfs.ex.dummy.DummyFileSystem
import com.intellij.util.LocalTimeCounter
import org.elasticsearch4idea.model.ElasticsearchCluster
import org.elasticsearch4idea.model.Request
import java.io.InputStream
import java.io.OutputStream

class ElasticsearchFile(
    val project: Project,
    val cluster: ElasticsearchCluster,
    val request: Request
) : VirtualFile() {
    private val myModStamp: Long = LocalTimeCounter.currentTime()

    override fun getName(): String {
        return cluster.label
    }

    override fun getFileType(): FileType {
        return ElasticsearchFakeFileType
    }

    override fun getFileSystem(): VirtualFileSystem {
        return ElasticsearchFileSystem.instance ?: DummyFileSystem.getInstance()
    }

    override fun getPath(): String {
        return name
    }

    override fun isValid(): Boolean {
        return true
    }

    override fun isWritable(): Boolean {
        return false
    }

    override fun isDirectory(): Boolean {
        return false
    }

    //    Unused methods
    override fun getParent(): VirtualFile? {
        return null
    }

    override fun getChildren(): Array<VirtualFile> {
        return emptyArray()
    }

    override fun getOutputStream(
        requestor: Any,
        newModificationStamp: Long,
        newTimeStamp: Long
    ): OutputStream {
        throw UnsupportedOperationException("ElasticsearchFile is read-only")
    }

    override fun getModificationStamp(): Long {
        return myModStamp
    }

    override fun contentsToByteArray(): ByteArray {
        return ByteArray(0)
    }

    override fun getTimeStamp(): Long {
        return 0
    }

    override fun getLength(): Long {
        return 0
    }

    override fun refresh(asynchronous: Boolean, recursive: Boolean, postRunnable: Runnable?) {}
    override fun getInputStream(): InputStream? {
        return null
    }
}