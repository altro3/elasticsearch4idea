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

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.apache.http.NameValuePair
import org.apache.http.client.utils.URIBuilder
import org.elasticsearch4idea.model.Request

class ResponseContext(
    val isNewRequest: Boolean,
    val request: Request,
    val response: String,
    private val mappingResponse: String?
) {

    private var responseJsonNode: JsonNode? = null
    private var requestBodyJsonNode: JsonNode? = null
    private var hits: List<Hit>? = null
    private var mappings: List<Mapping>? = null
    private var queryParams: Map<String, NameValuePair>? = null

    private fun getResponseJsonNode(): JsonNode {
        if (responseJsonNode == null) {
            responseJsonNode = objectMapper.readValue(response)
        }
        return responseJsonNode!!
    }

    private fun getRequestBodyJsonNode(): JsonNode {
        if (requestBodyJsonNode == null) {
            requestBodyJsonNode = objectMapper.readValue(if (request.body.isBlank()) "{}" else request.body)
        }
        return requestBodyJsonNode!!
    }

    fun getHitsNode(): JsonNode? {
        return getResponseJsonNode().get("hits")
    }

    fun getTotal(): Long {
        val totalNode = getHitsNode()?.get("total") ?: return 0
        return when {
            totalNode.isInt -> {
                totalNode.asLong()
            }
            totalNode.isObject -> {
                totalNode.get("value").asLong()
            }
            else -> {
                0
            }
        }
    }

    fun getHits(): List<Hit> {
        if (hits == null) {
            hits = if (getHitsNode() == null) {
                emptyList()
            } else {
                (getHitsNode()!!.get("hits") as ArrayNode).asIterable().asSequence()
                    .map { Hit.create(it as ObjectNode) }
                    .toList()
            }
        }
        return hits!!
    }

    fun getMappings(): List<Mapping> {
        if (mappings == null) {
            mappings = if (mappingResponse == null) {
                return emptyList()
            } else {
                Mapping.parseMappings(mappingResponse)
            }
        }
        return mappings!!
    }

    private fun getQueryParams(): Map<String, NameValuePair> {
        if (queryParams == null) {
            queryParams = URIBuilder(request.path)
                .queryParams.associateBy { it.name }
        }
        return queryParams!!
    }

    fun getFrom(): Long {
        var from = getQueryParams()["from"]?.value?.toLongOrNull()
            ?: getRequestBodyJsonNode().get("from")?.asLong() ?: 0
        if (from < 0) {
            from = 0
        }
        return from
    }

    fun getSize(): Long {
        var size = getQueryParams()["size"]?.value?.toLongOrNull()
            ?: getRequestBodyJsonNode().get("size")?.asLong() ?: getHits().size.toLong()
        if (size < 0) {
            size = getHits().size.toLong()
        }
        return size
    }

    fun isValidSearchRequest(): Boolean {
        return mappingResponse != null && isResponseValid() && isRequestBodyValid() && hasHits()
    }

    private fun isRequestBodyValid(): Boolean {
        return try {
            getRequestBodyJsonNode()
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun isResponseValid(): Boolean {
        return try {
            getResponseJsonNode()
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun hasHits(): Boolean {
        if (!isResponseValid() || getHitsNode() == null) {
            return false
        }
        return getHitsNode()!!.has("hits")
    }

    companion object {
        private val objectMapper = jacksonObjectMapper()
    }
}