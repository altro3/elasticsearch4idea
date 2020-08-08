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

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.util.ui.UIUtil
import org.apache.http.client.utils.URIBuilder
import org.elasticsearch4idea.model.*
import org.elasticsearch4idea.service.ElasticsearchManager
import org.elasticsearch4idea.ui.editor.model.PageModel
import org.elasticsearch4idea.ui.editor.views.ResultPanel
import org.elasticsearch4idea.utils.TaskUtils


class QueryManager(
    private val project: Project,
    private val cluster: ElasticsearchCluster,
    private val requestProvider: () -> Request,
    private val resultPanel: ResultPanel
) {
    private val responseListeners: MutableList<Listener<RequestAndResponse>> = mutableListOf()
    private lateinit var lastSearchRequest: Request

    fun addResponseListener(responseListener: Listener<RequestAndResponse>) {
        responseListeners.add(responseListener)
    }

    fun updateAndExecuteLastSearchRequest(pageModel: PageModel) {
        updateLastSearchRequest(pageModel)
        executeRequest(lastSearchRequest, false, { })
    }

    fun updateAndExecuteLastSearchRequest(pageModel: PageModel, responseListener: Listener<RequestAndResponse>) {
        updateLastSearchRequest(pageModel)
        executeRequest(lastSearchRequest, false, responseListener)
    }

    fun executeRequest() {
        val request = requestProvider.invoke()
        executeRequest(request, true, {})
    }

    private fun executeRequest(
        request: Request,
        isNewRequest: Boolean,
        responseListener: Listener<RequestAndResponse>
    ) {
        TaskUtils.runBackgroundTask("Executing request...") {
            resultPanel.startLoading()
            val elasticsearchManager = project.service<ElasticsearchManager>()
            val mappingRequestExecution = if (request.path.contains("_search")) {
                lastSearchRequest = request
                val mappingUrl = "${request.path.substring(0, request.path.indexOf("_search"))}/_mapping"
                val mappingRequest = Request(
                    host = request.host,
                    path = mappingUrl,
                    method = Method.GET
                )
                elasticsearchManager.prepareExecuteRequest(mappingRequest, cluster)
            } else {
                null
            }
            val requestExecution = elasticsearchManager.prepareExecuteRequest(request, cluster)
            RequestExecution(
                execution = {
                    val mappingFuture = mappingRequestExecution?.executeOnPooledThread()
                    val response = requestExecution.execute()
                    if (mappingFuture != null) {
                        val mappingResponse = mappingFuture.get()
                        Pair<Response, Response>(response, mappingResponse)
                    } else {
                        Pair(response, null)
                    }
                },
                onAbort = {
                    mappingRequestExecution?.abort()
                    requestExecution.abort()
                }
            )
                .onSuccess {
                    val requestAndResponse = RequestAndResponse(
                        isNewRequest,
                        request,
                        it.first.content,
                        it.second?.content
                    )
                    responseListeners.forEach { listener ->
                        listener.invoke(requestAndResponse)
                    }
                    responseListener.invoke(requestAndResponse)
                }
                .onError {
                    UIUtil.invokeLaterIfNeeded {
                        Messages.showErrorDialog(it.message, "Error")
                    }
                }
                .finally { _, _ ->
                    UIUtil.invokeLaterIfNeeded {
                        resultPanel.stopLoading()
                    }
                }
        }
    }

    private fun updateLastSearchRequest(pageModel: PageModel) {
        lastSearchRequest = when (lastSearchRequest.method) {
            Method.GET -> {
                val uriBuilder = URIBuilder(lastSearchRequest.path)
                uriBuilder.setParameter("from", (pageModel.getFromForRequest()).toString())
                uriBuilder.setParameter("size", pageModel.getSizeForRequest().toString())
                lastSearchRequest.copy(path = uriBuilder.toString())
            }
            Method.POST -> {
                val body = if (lastSearchRequest.body.isBlank()) "{}" else lastSearchRequest.body
                val jsonObject: JsonObject = GSON.fromJson(body, JsonElement::class.java).asJsonObject
                if (pageModel.getFromForRequest() >= 0) {
                    jsonObject.addProperty("from", pageModel.getFromForRequest())
                } else {
                    jsonObject.remove("from")
                }
                if (pageModel.getSizeForRequest() >= 0) {
                    jsonObject.addProperty("size", pageModel.getSizeForRequest())
                } else {
                    jsonObject.remove("size")
                }
                val uriBuilder = URIBuilder(lastSearchRequest.path)
                val params = uriBuilder.queryParams.asSequence()
                    .filter { it.name != "from" }
                    .filter { it.name != "size" }
                    .toList()
                uriBuilder.setParameters(params)
                lastSearchRequest.copy(
                    body = GSON_PRETTY.toJson(jsonObject),
                    path = uriBuilder.toString()
                )
            }
            else -> {
                throw IllegalArgumentException()
            }
        }
    }

    fun executeCountForLastSearchRequest(totalListener: Listener<Long>) {
        val request = when (lastSearchRequest.method) {
            Method.GET -> {
                val uriBuilder = URIBuilder(lastSearchRequest.path)
                val params = uriBuilder.queryParams.asSequence()
                    .filter { COUNT_QUERY_PARAMS.contains(it.name) }
                    .toList()
                uriBuilder.setParameters(params)
                lastSearchRequest.copy(
                    path = uriBuilder.toString().replace("_search", "_count")
                )
            }
            Method.POST -> {
                val uriBuilder = URIBuilder(lastSearchRequest.path)
                val params = uriBuilder.queryParams.asSequence()
                    .filter { COUNT_QUERY_PARAMS.contains(it.name) }
                    .toList()
                uriBuilder.setParameters(params)
                val body = if (lastSearchRequest.body.isBlank()) "{}" else lastSearchRequest.body
                val jsonObject: JsonObject = GSON.fromJson(body, JsonElement::class.java).asJsonObject
                val newJsonObject = JsonObject()
                if (jsonObject.has("query")) {
                    newJsonObject.add("query", jsonObject.get("query"))
                }
                lastSearchRequest.copy(
                    body = GSON.toJson(newJsonObject),
                    path = uriBuilder.toString().replace("_search", "_count")
                )
            }
            else -> {
                throw IllegalArgumentException()
            }
        }
        TaskUtils.runBackgroundTask("Executing count request...") {
            val elasticsearchManager = project.service<ElasticsearchManager>()
            elasticsearchManager.prepareExecuteRequest(request, cluster)
                .onSuccess {
                    val count = GSON.fromJson(it.content, JsonElement::class.java).asJsonObject.get("count").asLong
                    totalListener.invoke(count)
                }
                .onError {
                    UIUtil.invokeLaterIfNeeded {
                        Messages.showErrorDialog(it.message, "Error")
                    }
                }
        }
    }

    companion object {
        private val GSON_PRETTY = GsonBuilder().setPrettyPrinting().create()
        private val GSON = Gson()
        private val COUNT_QUERY_PARAMS = setOf(
            "allow_no_indices",
            "analyzer",
            "analyze_wildcard",
            "default_operator",
            "df",
            "expand_wildcards",
            "ignore_throttled",
            "ignore_unavailable",
            "lenient",
            "min_score",
            "preference",
            "q",
            "routing",
            "terminate_after"
        )
    }
}

class RequestAndResponse(
    val isNewRequest: Boolean,
    val request: Request,
    val response: String,
    val mappingResponse: String?
) {
    fun isSearchRequest(): Boolean {
        return mappingResponse != null
    }
}

typealias Listener<T> = (T) -> Unit
