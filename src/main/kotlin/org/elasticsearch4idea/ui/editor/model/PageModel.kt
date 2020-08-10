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

package org.elasticsearch4idea.ui.editor.model

import kotlin.math.max
import kotlin.math.min

class PageModel(
    private var pageSize: Long,
    private var total: Long,
    private var pageStart: Long,
    private var pageEnd: Long,
    private val listeners: MutableList<PageModelListener> = mutableListOf()
) {
    private var displayedTotal: Long? = null

    fun getFromForRequest(): Long {
        return pageStart
    }

    fun getSizeForRequest(): Long {
        if (pageStart + pageSize > total) {
            return total - pageStart
        }
        return pageSize
    }

    fun getDisplayedPageStart(): Long {
        return if (total == 0L || pageSize == 0L) 0 else pageStart + 1
    }

    fun getDisplayedPageEnd(): Long {
        return if (total == 0L || pageSize == 0L) 0 else pageEnd
    }

    fun addListener(pageModelListener: PageModelListener) {
        listeners.add(pageModelListener)
    }

    fun nextPage() {
        if (!isLastPage()) {
            setPageStart(pageStart + pageSize)
        }
    }

    fun previousPage() {
        if (!isFirstPage()) {
            setPageStart(pageStart - pageSize)
        }
    }

    fun isFirstPage(): Boolean {
        return pageStart == 0L
    }

    fun isLastPage(): Boolean {
        return pageEnd == total
    }

    fun isSinglePage(): Boolean {
        return isFirstPage() && isLastPage()
    }

    fun lastPage() {
        setPageStart(total - pageSize)
    }

    fun firstPage() {
        setPageStart(0)
    }

    fun update(responseContext: ResponseContext) {
        var size = pageSize
        if (responseContext.isNewRequest) {
            displayedTotal = null
            size = responseContext.getSize()
        }
        pageSize = size
        total = responseContext.getTotal()
        setPageStart(responseContext.getFrom())

        listeners.forEach { it.onModelChanged(this) }
    }

    fun setPageSize(size: Long) {
        pageSize = size
    }

    fun setPageStart(start: Long) {
        pageStart = if (total == 0L || pageSize == 0L) 0 else max(0, min(total - 1, start))
        pageEnd = if (total == 0L || pageSize == 0L) 0 else min(total, pageStart + pageSize)
    }

    fun updateDisplayedTotal(total: Long) {
        displayedTotal = total
        listeners.forEach { it.onModelChanged(this) }
    }

    fun getDisplayedTotal(): Long {
        return displayedTotal ?: total
    }

    fun getTotal(): Long {
        return total
    }

    fun getPageSize(): Long {
        return pageSize
    }

    interface PageModelListener {

        fun onModelChanged(pageModel: PageModel)
    }
}