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
package org.elasticsearch4idea.service

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.util.EventDispatcher
import com.intellij.util.concurrency.AppExecutorUtil
import org.apache.http.Header
import org.apache.http.HttpHeaders
import org.apache.http.message.BasicHeader
import org.elasticsearch4idea.model.*
import org.elasticsearch4idea.rest.ElasticsearchClient
import org.elasticsearch4idea.ui.ElasticsearchClustersListener
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit

@Service
class ElasticsearchManager(project: Project) : Disposable {

    private val clusters: MutableMap<String, ElasticsearchCluster> = ConcurrentHashMap()
    private val eventDispatcher: EventDispatcher<ElasticsearchClustersListener> =
        EventDispatcher.create(ElasticsearchClustersListener::class.java)
    private val elasticsearchClient = project.service<ElasticsearchClient>()
    private val elasticsearchConfiguration = project.service<ElasticsearchConfiguration>()
    private val scheduler = AppExecutorUtil.getAppScheduledExecutorService()
    private var future: Future<*>? = null

    init {
        scheduleAutoRefresh(elasticsearchConfiguration.autoRefresh)
    }

    @Synchronized
    fun scheduleAutoRefresh(autoRefresh: AutoRefreshOptions) {
        elasticsearchConfiguration.autoRefresh = autoRefresh

        future?.cancel(false)
        if (autoRefresh != AutoRefreshOptions.DISABLED) {
            future = scheduler.scheduleWithFixedDelay(
                {
                    fetchAllClusters(false)
                    eventDispatcher.multicaster.clustersLoaded()
                },
                autoRefresh.seconds.toLong(),
                autoRefresh.seconds.toLong(),
                TimeUnit.SECONDS
            )
        }
    }

    fun addClustersListener(listener: ElasticsearchClustersListener) {
        eventDispatcher.addListener(listener)
    }

    fun removeClustersListener(listener: ElasticsearchClustersListener) {
        eventDispatcher.removeListener(listener)
    }

    fun getClusters(): Collection<ElasticsearchCluster> {
        return clusters.values
    }

    fun fetchAllClusters(needNotify: Boolean = true) {
        elasticsearchConfiguration.getConfigurations()
            .forEach { fetchCluster(it, needNotify) }
    }

    fun fetchClusters(clusterLabels: Collection<String>) {
        clusterLabels.asSequence()
            .map { elasticsearchConfiguration.getConfiguration(it) }
            .filterNotNull()
            .forEach { fetchCluster(it, true) }
    }

    fun addCluster(clusterConfig: ClusterConfiguration) {
        fetchCluster(clusterConfig, true)
    }

    fun changeCluster(previousLabel: String, clusterConfig: ClusterConfiguration) {
        val isNewLabel = clusterConfig.label != previousLabel
        if (isNewLabel) {
            removeCluster(previousLabel)
        }
        fetchCluster(clusterConfig, true)
    }

    private fun fetchCluster(clusterConfig: ClusterConfiguration, needNotify: Boolean) {
        val isNewCluster = !clusters.containsKey(clusterConfig.label)
        elasticsearchConfiguration.putClusterConfiguration(clusterConfig)
        val cluster = createOrUpdateCluster(clusterConfig)
        cluster.status = ElasticsearchCluster.Status.LOADING
        if (needNotify) {
            if (isNewCluster) {
                ApplicationManager.getApplication()
                    .invokeAndWait { eventDispatcher.multicaster.clusterAdded(cluster) }
            } else {
                ApplicationManager.getApplication()
                    .invokeAndWait { eventDispatcher.multicaster.clusterChanged(cluster) }
            }
        }

        fetchClusterStats(cluster)

        if (cluster.status == ElasticsearchCluster.Status.LOADED) {
            fetchIndices(cluster, needNotify)
        } else {
            cluster.indices.clear()
        }
        if (needNotify) {
            ApplicationManager.getApplication()
                .invokeAndWait { eventDispatcher.multicaster.clusterChanged(cluster) }
        }
    }

    private fun createOrUpdateCluster(configuration: ClusterConfiguration): ElasticsearchCluster {
        return clusters.getOrPut(configuration.label) { ElasticsearchCluster(configuration.label) }
            .apply { host = configuration.url }
    }

    private fun fetchClusterStats(cluster: ElasticsearchCluster): ElasticsearchCluster {
        val clusterConfig = elasticsearchConfiguration.getConfiguration(cluster.label)!!
        val clusterStats = runSafely {
            elasticsearchClient.getClusterStats(
                clusterConfig.getHttpHost(),
                getHeaders(cluster)
            )
        }
        cluster.apply {
            status =
                if (clusterStats == null) ElasticsearchCluster.Status.NOT_LOADED else ElasticsearchCluster.Status.LOADED
            clusterName = clusterStats?.clusterName
            healthStatus = clusterStats?.status
        }
        return cluster
    }

    private fun fetchIndices(cluster: ElasticsearchCluster, needNotify: Boolean): ElasticsearchCluster {
        val clusterConfig = elasticsearchConfiguration.getConfiguration(cluster.label)!!
        val indexesByName = cluster.indices.associateBy { it.name }
        cluster.indices.clear()
        cluster.status = ElasticsearchCluster.Status.NOT_LOADED
        runSafely {
            val indices = elasticsearchClient.getIndices(
                clusterConfig.getHttpHost(),
                getHeaders(cluster)
            )
            indices.forEach {
                val index = indexesByName.getOrDefault(it.name, ElasticsearchIndex(it.name))
                    .apply {
                        healthStatus = it.health
                        status = it.status
                        this.cluster = cluster
                    }
                cluster.addIndex(index)
            }
            cluster.status = ElasticsearchCluster.Status.LOADED
        }
        if (needNotify) {
            ApplicationManager.getApplication()
                .invokeLater { eventDispatcher.multicaster.clusterChanged(cluster) }
        }
        return cluster
    }

    fun removeCluster(label: String) {
        elasticsearchConfiguration.removeClusterConfiguration(label)
        val cluster = clusters.remove(label)!!
        ApplicationManager.getApplication()
            .invokeLater { eventDispatcher.multicaster.clusterRemoved(cluster) }
    }

    fun getClusterInfo(cluster: ElasticsearchCluster): ElasticsearchClusterInfo? {
        val configuration = elasticsearchConfiguration.getConfiguration(cluster.label)!!
        return runSafely {
            val clusterStats = elasticsearchClient.getClusterStats(
                configuration.getHttpHost(),
                getHeaders(cluster)
            )
            ElasticsearchClusterInfo(clusterStats)
        }
    }

    fun getIndexInfo(index: ElasticsearchIndex): ElasticsearchIndexInfo? {
        val configuration = elasticsearchConfiguration.getConfiguration(index.cluster.label)!!
        return runSafely {
            val indexShortInfo = elasticsearchClient.getIndex(
                configuration.getHttpHost(),
                getHeaders(index.cluster),
                index.name
            )
            val indexInfo = elasticsearchClient.getIndexInfo(
                configuration.getHttpHost(),
                getHeaders(index.cluster), index.name
            )
            ElasticsearchIndexInfo(indexShortInfo, indexInfo)
        }
    }

    fun deleteIndex(index: ElasticsearchIndex) {
        runSafely(true) {
            val response = elasticsearchClient.execute(
                Request(
                    urlPath = "${index.cluster.host}/${index.name}",
                    method = Method.DELETE
                ),
                getHeaders(index.cluster)
            )
            Messages.showInfoMessage(response.content, "Index '${index.name}' deleted")
        }
        fetchIndices(index.cluster, true)
    }

    fun createIndex(cluster: ElasticsearchCluster, indexName: String, numberOfShards: Int, numberOfReplicas: Int) {
        runSafely(true) {
            val response = elasticsearchClient.execute(
                Request(
                    urlPath = "${cluster.host}/$indexName",
                    method = Method.PUT,
                    body = """{
        "settings" : {
            "index" : {
                "number_of_shards" : $numberOfShards, 
                "number_of_replicas" : $numberOfReplicas 
            }
        }
    }"""
                ),
                getHeaders(cluster)
            )
            Messages.showInfoMessage(response.content, "Index '$indexName' created")
        }
        fetchIndices(cluster, true)
    }

    fun createAlias(index: ElasticsearchIndex, alias: String) {
        runSafely(true) {
            val response = elasticsearchClient.execute(
                Request(
                    urlPath = "${index.cluster.host}/${index.name}/_alias/$alias",
                    method = Method.PUT
                ),
                getHeaders(index.cluster)
            )
            Messages.showInfoMessage(response.content, "Alias '$alias' created")
        }
    }

    fun refreshIndex(index: ElasticsearchIndex) {
        runSafely(true) {
            val response = elasticsearchClient.execute(
                Request(
                    urlPath = "${index.cluster.host}/${index.name}/_refresh",
                    method = Method.POST
                ),
                getHeaders(index.cluster)
            )
            Messages.showInfoMessage(response.content, "Index '${index.name}' refreshed")
        }
    }

    fun flushIndex(index: ElasticsearchIndex) {
        runSafely(true) {
            val response = elasticsearchClient.execute(
                Request(
                    urlPath = "${index.cluster.host}/${index.name}/_flush",
                    method = Method.POST
                ),
                getHeaders(index.cluster)
            )
            Messages.showInfoMessage(response.content, "Index '${index.name}' flushed")
        }
    }

    fun forceMergeIndex(index: ElasticsearchIndex, maxNumSegments: Int, onlyExpungeDeletes: Boolean, flush: Boolean) {
        runSafely(true) {
            val response = elasticsearchClient.execute(
                Request(
                    urlPath = "${index.cluster.host}/${index.name}/_forcemerge?" +
                            "only_expunge_deletes=$onlyExpungeDeletes&max_num_segments=$maxNumSegments&flush=$flush",
                    method = Method.POST
                ),
                getHeaders(index.cluster)
            )
            Messages.showInfoMessage(response.content, "Force merge '${index.name}' performed")
        }
    }

    fun closeIndex(index: ElasticsearchIndex) {
        runSafely(true) {
            val response = elasticsearchClient.execute(
                Request(
                    urlPath = "${index.cluster.host}/${index.name}/_close",
                    method = Method.POST
                ),
                getHeaders(index.cluster)
            )
            Messages.showInfoMessage(response.content, "Index '${index.name}' closed")
        }
        fetchIndices(index.cluster, true)
    }

    fun openIndex(index: ElasticsearchIndex) {
        runSafely(true) {
            val response = elasticsearchClient.execute(
                Request(
                    urlPath = "${index.cluster.host}/${index.name}/_open",
                    method = Method.POST
                ),
                getHeaders(index.cluster)
            )
            Messages.showInfoMessage(response.content, "Index '${index.name}' opened")
        }
        fetchIndices(index.cluster, true)
    }


    fun executeRequest(request: Request, cluster: ElasticsearchCluster): Response {
        return elasticsearchClient.execute(request, getHeaders(cluster), false)
    }

    private fun getHeaders(cluster: ElasticsearchCluster): List<Header> {
        val basicAuthHeader = getBasicAuthHeader(cluster)
        return if (basicAuthHeader == null) emptyList() else listOf(basicAuthHeader)
    }

    private fun getBasicAuthHeader(cluster: ElasticsearchCluster): Header? {
        val credentials = elasticsearchConfiguration.getConfiguration(cluster.label)?.credentials ?: return null
        return BasicHeader(HttpHeaders.AUTHORIZATION, credentials.toBasicAuthHeader())
    }

    private fun <R> runSafely(showErrorMessage: Boolean = false, function: () -> R): R? {
        return try {
            function.invoke()
        } catch (e: Exception) {
            if (showErrorMessage) {
                Messages.showErrorDialog(e.message ?: e.toString(), "Error")
            }
            null
        }
    }

    override fun dispose() {
        future?.cancel(false)
    }

}