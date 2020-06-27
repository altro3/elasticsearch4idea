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
import org.elasticsearch4idea.model.*
import org.elasticsearch4idea.rest.ElasticsearchClient
import org.elasticsearch4idea.ui.ElasticsearchClustersListener
import java.util.concurrent.ConcurrentHashMap

@Service
class ElasticsearchManager(project: Project) : Disposable {

    private val clusters: MutableMap<String, ElasticsearchCluster> = ConcurrentHashMap()
    private val clients: MutableMap<String, ElasticsearchClient> = ConcurrentHashMap()
    private val eventDispatcher: EventDispatcher<ElasticsearchClustersListener> =
        EventDispatcher.create(ElasticsearchClustersListener::class.java)
    private val elasticsearchConfiguration = project.service<ElasticsearchConfiguration>()


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
        clients.remove(configuration.label)?.close()
        clients.put(configuration.label, ElasticsearchClient(configuration))
        return clusters.getOrPut(configuration.label) { ElasticsearchCluster(configuration.label) }
            .apply { host = configuration.url }
    }

    private fun fetchClusterStats(cluster: ElasticsearchCluster): ElasticsearchCluster {
        val clusterStats = runSafely {
            getClient(cluster).getClusterStats()
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
        val indexesByName = cluster.indices.associateBy { it.name }
        cluster.indices.clear()
        cluster.status = ElasticsearchCluster.Status.NOT_LOADED
        runSafely {
            val indices = getClient(cluster).getIndices()
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
        clients.remove(label)?.close()
        ApplicationManager.getApplication()
            .invokeLater { eventDispatcher.multicaster.clusterRemoved(cluster) }
    }

    fun getClusterInfo(cluster: ElasticsearchCluster): ElasticsearchClusterInfo? {
        return runSafely {
            val clusterStats = getClient(cluster).getClusterStats()
            ElasticsearchClusterInfo(clusterStats)
        }
    }

    fun getIndexInfo(index: ElasticsearchIndex): ElasticsearchIndexInfo? {
        return runSafely {
            val indexShortInfo = getClient(index).getIndex(index.name)
            val indexInfo = getClient(index).getIndexInfo(index.name)
            ElasticsearchIndexInfo(indexShortInfo, indexInfo)
        }
    }

    fun deleteIndex(index: ElasticsearchIndex) {
        runSafely(true) {
            val response = getClient(index).execute(
                Request(
                    urlPath = "${index.cluster.host}/${index.name}",
                    method = Method.DELETE
                )
            )
            Messages.showInfoMessage(response.content, "Index '${index.name}' deleted")
        }
        fetchIndices(index.cluster, true)
    }

    fun createIndex(cluster: ElasticsearchCluster, indexName: String, numberOfShards: Int, numberOfReplicas: Int) {
        runSafely(true) {
            val response = getClient(cluster).execute(
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
                )
            )
            Messages.showInfoMessage(response.content, "Index '$indexName' created")
        }
        fetchIndices(cluster, true)
    }

    fun createAlias(index: ElasticsearchIndex, alias: String) {
        runSafely(true) {
            val response = getClient(index).execute(
                Request(
                    urlPath = "${index.cluster.host}/${index.name}/_alias/$alias",
                    method = Method.PUT
                )
            )
            Messages.showInfoMessage(response.content, "Alias '$alias' created")
        }
    }

    fun refreshIndex(index: ElasticsearchIndex) {
        runSafely(true) {
            val response = getClient(index).execute(
                Request(
                    urlPath = "${index.cluster.host}/${index.name}/_refresh",
                    method = Method.POST
                )
            )
            Messages.showInfoMessage(response.content, "Index '${index.name}' refreshed")
        }
    }

    fun flushIndex(index: ElasticsearchIndex) {
        runSafely(true) {
            val response = getClient(index).execute(
                Request(
                    urlPath = "${index.cluster.host}/${index.name}/_flush",
                    method = Method.POST
                )
            )
            Messages.showInfoMessage(response.content, "Index '${index.name}' flushed")
        }
    }

    fun forceMergeIndex(index: ElasticsearchIndex, maxNumSegments: Int, onlyExpungeDeletes: Boolean, flush: Boolean) {
        runSafely(true) {
            val response = getClient(index).execute(
                Request(
                    urlPath = "${index.cluster.host}/${index.name}/_forcemerge?" +
                            "only_expunge_deletes=$onlyExpungeDeletes&max_num_segments=$maxNumSegments&flush=$flush",
                    method = Method.POST
                )
            )
            Messages.showInfoMessage(response.content, "Force merge '${index.name}' performed")
        }
    }

    fun closeIndex(index: ElasticsearchIndex) {
        runSafely(true) {
            val response = getClient(index).execute(
                Request(
                    urlPath = "${index.cluster.host}/${index.name}/_close",
                    method = Method.POST
                )
            )
            Messages.showInfoMessage(response.content, "Index '${index.name}' closed")
        }
        fetchIndices(index.cluster, true)
    }

    fun openIndex(index: ElasticsearchIndex) {
        runSafely(true) {
            val response = getClient(index).execute(
                Request(
                    urlPath = "${index.cluster.host}/${index.name}/_open",
                    method = Method.POST
                )
            )
            Messages.showInfoMessage(response.content, "Index '${index.name}' opened")
        }
        fetchIndices(index.cluster, true)
    }

    fun testConnection(configuration: ClusterConfiguration) {
        ElasticsearchClient(configuration).use {
            it.testConnection()
        }
    }

    fun executeRequest(request: Request, cluster: ElasticsearchCluster): Response {
        return getClient(cluster).execute(request, false)
    }

    private fun getClient(index: ElasticsearchIndex): ElasticsearchClient {
        return getClient(index.cluster)
    }

    private fun getClient(cluster: ElasticsearchCluster): ElasticsearchClient {
        return getClient(cluster.label)
    }

    private fun getClient(label: String): ElasticsearchClient {
        return clients[label]!!
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
        clients.values.forEach { it.close() }
    }

}