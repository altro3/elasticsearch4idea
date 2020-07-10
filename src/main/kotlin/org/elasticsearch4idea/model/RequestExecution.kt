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

package org.elasticsearch4idea.model

import com.intellij.util.concurrency.AppExecutorUtil
import java.util.concurrent.CompletableFuture
import java.util.function.Supplier

class RequestExecution<T>(
    private val execution: () -> T,
    private val onAbort: () -> Unit,
    private var onSuccessHandlers: MutableList<(T) -> Unit> = mutableListOf(),
    private var onErrorHandlers: MutableList<(Exception) -> Unit> = mutableListOf(),
    private var finallyHandlers: MutableList<(T?, Exception?) -> Unit> = mutableListOf()
) {

    fun abort() {
        try {
            onAbort.invoke()
        } catch (ignore: Exception) {
        }
    }

    fun execute() {
        try {
            executeInternal()
        } catch (ignore: Exception) {
        }
    }

    fun executeOnPooledThread(): CompletableFuture<T> {
        return CompletableFuture.supplyAsync(Supplier {
            executeInternal()
        }, AppExecutorUtil.getAppExecutorService())
    }

    private fun executeInternal(): T {
        return try {
            val result = execution.invoke()
            onSuccessHandlers.forEach { it.invoke(result) }
            finallyHandlers.forEach { it.invoke(result, null) }
            result
        } catch (e: Exception) {
            onErrorHandlers.forEach { it.invoke(e) }
            finallyHandlers.forEach { it.invoke(null, e) }
            throw e
        }
    }

    fun <R> map(mapper: (T) -> R): RequestExecution<R> {
        return RequestExecution(
            { mapper.invoke(execution.invoke()) },
            onAbort
        )
    }

    fun onSuccess(handler: (T) -> Unit): RequestExecution<T> {
        this.onSuccessHandlers.add(handler)
        return this
    }

    fun onError(handler: (Exception) -> Unit): RequestExecution<T> {
        this.onErrorHandlers.add(handler)
        return this
    }

    fun finally(handler: (T?, Exception?) -> Unit): RequestExecution<T> {
        this.finallyHandlers.add(handler)
        return this
    }

    companion object {
        fun empty() = RequestExecution({}, {})
    }
}