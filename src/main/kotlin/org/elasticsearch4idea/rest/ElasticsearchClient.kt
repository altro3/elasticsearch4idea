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

package org.elasticsearch4idea.rest

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.MapperFeature
import com.fasterxml.jackson.databind.PropertyNamingStrategy
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.intellij.openapi.util.text.StringUtil
import com.intellij.util.LineSeparator
import org.apache.http.Header
import org.apache.http.HttpHeaders
import org.apache.http.HttpHost
import org.apache.http.HttpResponse
import org.apache.http.client.ResponseHandler
import org.apache.http.client.config.RequestConfig
import org.apache.http.client.methods.*
import org.apache.http.conn.ssl.NoopHostnameVerifier
import org.apache.http.entity.StringEntity
import org.apache.http.impl.client.CloseableHttpClient
import org.apache.http.impl.client.HttpClientBuilder
import org.apache.http.message.BasicHeader
import org.elasticsearch4idea.model.*
import org.elasticsearch4idea.rest.model.*
import org.elasticsearch4idea.utils.SSLUtils
import java.io.BufferedReader
import java.io.Closeable
import java.io.InputStream
import java.nio.charset.StandardCharsets

class ElasticsearchClient(clusterConfiguration: ClusterConfiguration) : Closeable {
    private val httpHost: HttpHost = clusterConfiguration.getHttpHost()
    private val client: CloseableHttpClient by lazy {
        val headers = if (clusterConfiguration.credentials == null) {
            emptyList<Header>()
        } else {
            listOf(BasicHeader(HttpHeaders.AUTHORIZATION, clusterConfiguration.credentials.toBasicAuthHeader()))
        }

        val config = RequestConfig.custom()
            .setConnectTimeout(5_000)
            .build()

        val sslContext = SSLUtils.createSSLContext(clusterConfiguration.sslConfig)
        HttpClientBuilder.create()
            .setDefaultHeaders(headers)
            .setSSLContext(sslContext)
            .setSSLHostnameVerifier(NoopHostnameVerifier())
            .setDefaultRequestConfig(config)
            .build()
    }

    fun prepareGetIndexInfo(index: String): RequestExecution<IndexInfo> {
        val request = HttpGet("$httpHost/$index")
        return prepareExecution(request, ConvertingResponseHandler {
            val jsonNode = objectMapper.readTree(it)
            objectMapper.treeToValue(jsonNode.get(index), IndexInfo::class.java)
        })
    }

    fun prepareGetIndex(index: String): RequestExecution<Index> {
        return prepareGetIndices(index).map { it.first() }
    }

    fun prepareGetIndices(index: String? = null): RequestExecution<List<Index>> {
        val url = if (index == null) "$httpHost/_cat/indices?v" else "$httpHost/_cat/indices/$index?v"
        val request = HttpGet(url)
        return prepareExecution(request, TableDataResponseHandler())
            .map { table ->
                val header = table[0].asSequence()
                    .mapIndexed { i, value -> Pair(value, i) }
                    .associate { it }
                table.asSequence()
                    .drop(1)
                    .map {
                        Index(
                            name = it[header.getValue("index")],
                            health = it[header.getValue("health")].toUpperCase()
                                .let { health ->
                                    HealthStatus.values().firstOrNull { status -> status.name == health }
                                },
                            status = IndexStatus.valueOf(it[header.getValue("status")].toUpperCase()),
                            primaries = it[header.getValue("pri")],
                            replicas = it[header.getValue("rep")],
                            documents = it[header.getValue("docs.count")],
                            deletedDocuments = it[header.getValue("docs.deleted")],
                            size = it[header.getValue("store.size")]
                        )
                    }.toList()
            }
    }

    fun prepareTestConnection(): RequestExecution<*> {
        val request = HttpGet("$httpHost")
        return prepareExecution(request, JsonResponseHandler(Map::class.java))
    }

    fun prepareGetClusterStats(): RequestExecution<ClusterStats> {
        val request = HttpGet("$httpHost/_cluster/stats")
        return prepareExecution(request, JsonResponseHandler(ClusterStats::class.java))
    }

    fun prepareExecution(request: Request, checkResponseSuccess: Boolean = true): RequestExecution<Response> {
        val httpRequest = when (request.method) {
            Method.GET -> {
                val get = MyHttpGet(request.getUrl())
                get.setHeader(HttpHeaders.CONTENT_TYPE, "application/json")
                get.entity = StringEntity(request.body, StandardCharsets.UTF_8)
                get
            }
            Method.POST -> {
                val post = HttpPost(request.getUrl())
                post.setHeader(HttpHeaders.CONTENT_TYPE, "application/json")
                post.entity = StringEntity(request.body, StandardCharsets.UTF_8)
                post
            }
            Method.PUT -> {
                val put = HttpPut(request.getUrl())
                put.setHeader(HttpHeaders.CONTENT_TYPE, "application/json")
                put.entity = StringEntity(request.body, StandardCharsets.UTF_8)
                put
            }
            Method.HEAD -> HttpHead(request.getUrl())
            Method.DELETE -> HttpDelete(request.getUrl())
        }
        return prepareExecution(
            httpRequest,
            ResponseHandler { response ->
                if (checkResponseSuccess) {
                    checkResponse(response)
                }
                val content = response.entity.content.use {
                    it.bufferedReader().use(BufferedReader::readText)
                }
                    .let { StringUtil.convertLineSeparators(it, LineSeparator.LF.separatorString) }
                Response(content, response.statusLine)
            }
        )
    }

    private fun <T> prepareExecution(
        request: HttpUriRequest,
        responseHandler: ResponseHandler<T>
    ): RequestExecution<T> {
        return RequestExecution({ client.execute(request, responseHandler) }, request::abort)
    }

    private class JsonResponseHandler<T>(private val clazz: Class<T>) : ConvertingResponseHandler<T>({
        objectMapper.readValue(it, clazz)
    })

    private class TableDataResponseHandler : ConvertingResponseHandler<List<List<String>>>({
        val lines = it.bufferedReader().useLines { lineSequence ->
            lineSequence.toList()
        }
        val header = lines[0]
        val columnStarts = ArrayList<Int>()
        for (i in header.indices) {
            if (i == 0 || (header[i - 1] == ' ' && header[i] != ' ')) {
                columnStarts.add(i)
            }
        }
        columnStarts.add(header.length)
        val columnRanges = columnStarts.zipWithNext()
        lines.asSequence()
            .map { line ->
                columnRanges.asSequence()
                    .map { range -> line.substring(range.first, range.second).trim() }
                    .toList()
            }.toList()
    })

    private open class ConvertingResponseHandler<T>(private val converter: (InputStream) -> T) :
        ResponseHandler<T> {

        override fun handleResponse(response: HttpResponse): T {
            checkResponse(response)

            return response.entity.content.use {
                converter.invoke(it)
            }
        }
    }

    override fun close() {
        try {
            client.close()
        } catch (ignore: Exception) {
        }
    }

    companion object {

        private fun checkResponse(response: HttpResponse) {
            if (response.statusLine.statusCode !in 200..299) {
                val content = response.entity.content.use {
                    it.bufferedReader().use(BufferedReader::readText)
                }
                throw ElasticsearchException(response.statusLine, content)
            }
        }

        private val objectMapper = jacksonObjectMapper()
            .enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS)
            .setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE)
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    }

}