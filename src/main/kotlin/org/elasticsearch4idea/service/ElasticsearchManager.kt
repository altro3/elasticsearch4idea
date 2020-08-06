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
import com.intellij.util.ui.UIUtil
import org.elasticsearch4idea.model.*
import org.elasticsearch4idea.rest.ElasticsearchClient
import org.elasticsearch4idea.rest.model.Index
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

    fun createAllClusters() {
        elasticsearchConfiguration.getConfigurations().forEach {
            val cluster = createOrUpdateCluster(it)
            ApplicationManager.getApplication()
                .invokeLater { eventDispatcher.multicaster.clusterAdded(cluster) }
        }
    }

    fun prepareFetchCluster(clusterLabel: String): RequestExecution<*> {
        val clusterConfiguration = elasticsearchConfiguration.getConfiguration(clusterLabel)
        if (clusterConfiguration != null) {
            return prepareFetchCluster(clusterConfiguration)
        }
        return RequestExecution.empty()
    }

    fun prepareAddCluster(clusterConfig: ClusterConfiguration): RequestExecution<*> {
        return prepareFetchCluster(clusterConfig)
    }

    fun prepareChangeCluster(previousLabel: String, clusterConfig: ClusterConfiguration): RequestExecution<*> {
        val isNewLabel = clusterConfig.label != previousLabel
        if (isNewLabel) {
            removeCluster(previousLabel)
        }
        return prepareFetchCluster(clusterConfig)
    }

    private fun prepareFetchCluster(clusterConfig: ClusterConfiguration): RequestExecution<*> {
        val isNewCluster = !clusters.containsKey(clusterConfig.label)
        elasticsearchConfiguration.putClusterConfiguration(clusterConfig)
        val cluster = createOrUpdateCluster(clusterConfig)
        val clusterStatsRequest = getClient(cluster).prepareGetClusterStats()
        val indicesRequest = getClient(cluster).prepareGetIndices()
        return RequestExecution(
            execution = {
                cluster.status = ElasticsearchCluster.Status.LOADING
                if (isNewCluster) {
                    ApplicationManager.getApplication()
                        .invokeAndWait { eventDispatcher.multicaster.clusterAdded(cluster) }
                } else {
                    ApplicationManager.getApplication()
                        .invokeAndWait { eventDispatcher.multicaster.clusterChanged(cluster) }
                }

                val clusterStatsFuture = clusterStatsRequest.executeOnPooledThread()
                val indices = indicesRequest.execute()
                val clusterStats = clusterStatsFuture.get()
                cluster.apply {
                    clusterName = clusterStats.clusterName
                    healthStatus = clusterStats.status
                }
                mergeIndices(cluster, indices)
            },
            onAbort = {
                clusterStatsRequest.abort()
                indicesRequest.abort()
            }
        )
            .onSuccess {
                cluster.status = ElasticsearchCluster.Status.LOADED
            }
            .onError {
                cluster.indices.clear()
                cluster.status = ElasticsearchCluster.Status.NOT_LOADED
            }
            .finally { _, _ ->
                ApplicationManager.getApplication()
                    .invokeAndWait { eventDispatcher.multicaster.clusterChanged(cluster) }
            }
    }

    private fun mergeIndices(cluster: ElasticsearchCluster, indices: List<Index>) {
        synchronized(cluster) {
            val indexesByName = indices.associateBy { it.name }
            cluster.indices.removeIf { !indexesByName.containsKey(it.name) }
            val oldIndexesByName = cluster.indices.associateBy { it.name }
            indices.forEach {
                val index = if (oldIndexesByName.containsKey(it.name)) {
                    oldIndexesByName[it.name]!!
                } else {
                    val index = ElasticsearchIndex(it.name)
                    cluster.addIndex(index)
                    index
                }
                index.apply {
                    healthStatus = it.health
                    status = it.status
                    this.cluster = cluster
                }
            }
        }
    }

    private fun createOrUpdateCluster(configuration: ClusterConfiguration): ElasticsearchCluster {
        clients.remove(configuration.label)?.close()
        return clusters.getOrPut(configuration.label) { ElasticsearchCluster(configuration.label) }
            .apply { host = configuration.url }
    }

    private fun prepareFetchIndices(cluster: ElasticsearchCluster): RequestExecution<*> {
        cluster.status = ElasticsearchCluster.Status.NOT_LOADED
        return getClient(cluster).prepareGetIndices()
            .onSuccess {
                mergeIndices(cluster, it)
                cluster.status = ElasticsearchCluster.Status.LOADED

            }
            .onError {
                cluster.indices.clear()
                cluster.status = ElasticsearchCluster.Status.NOT_LOADED
            }
            .finally { _, _ ->
                ApplicationManager.getApplication()
                    .invokeLater { eventDispatcher.multicaster.clusterChanged(cluster) }
            }
    }

    fun removeCluster(label: String) {
        elasticsearchConfiguration.removeClusterConfiguration(label)
        val cluster = clusters.remove(label)!!
        clients.remove(label)?.close()
        ApplicationManager.getApplication()
            .invokeLater { eventDispatcher.multicaster.clusterRemoved(cluster) }
    }

    fun prepareGetClusterInfo(cluster: ElasticsearchCluster): RequestExecution<ElasticsearchClusterInfo> {
        return getClient(cluster).prepareGetClusterStats()
            .map { ElasticsearchClusterInfo(it) }
    }

    fun prepareGetIndexInfo(index: ElasticsearchIndex): RequestExecution<ElasticsearchIndexInfo> {
        val indexShortInfoRequest = getClient(index).prepareGetIndex(index.name)
        val indexInfoRequest = getClient(index).prepareGetIndexInfo(index.name)
        return RequestExecution(
            execution = {
                val indexShortInfoFuture = indexShortInfoRequest.executeOnPooledThread()
                val indexInfo = indexInfoRequest.execute()
                val indexShortInfo = indexShortInfoFuture.get()
                ElasticsearchIndexInfo(indexShortInfo, indexInfo)
            },
            onAbort = {
                indexShortInfoRequest.abort()
                indexInfoRequest.abort()
            }
        )
    }

    fun prepareDeleteIndex(index: ElasticsearchIndex): RequestExecution<*> {
        return getClient(index).prepareExecution(
            Request(
                host = index.cluster.host,
                path = index.name,
                method = Method.DELETE
            )
        )
            .onError(::showErrorMessage)
            .onSuccess {
                showInfoMessage(it.content, "Index '${index.name}' deleted")
                prepareFetchIndices(index.cluster).executeSafely()
            }
    }

    fun prepareCreateIndex(
        cluster: ElasticsearchCluster,
        indexName: String,
        numberOfShards: Int,
        numberOfReplicas: Int
    ): RequestExecution<*> {
        return getClient(cluster).prepareExecution(
            Request(
                host = cluster.host,
                path = indexName,
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
            .onError(::showErrorMessage)
            .onSuccess {
                showInfoMessage(it.content, "Index '$indexName' created")
                prepareFetchIndices(cluster).executeSafely()
            }
    }

    fun prepareCreateAlias(index: ElasticsearchIndex, alias: String): RequestExecution<*> {
        return getClient(index).prepareExecution(
            Request(
                host = index.cluster.host,
                path = "${index.name}/_alias/$alias",
                method = Method.PUT
            )
        )
            .onError(::showErrorMessage)
            .onSuccess {
                showInfoMessage(it.content, "Alias '$alias' created")
            }
    }

    fun prepareRefreshIndex(index: ElasticsearchIndex): RequestExecution<*> {
        return getClient(index).prepareExecution(
            Request(
                host = index.cluster.host,
                path = "${index.name}/_refresh",
                method = Method.POST
            )
        )
            .onError(::showErrorMessage)
            .onSuccess {
                showInfoMessage(it.content, "Index '${index.name}' refreshed")
            }
    }

    fun prepareFlushIndex(index: ElasticsearchIndex): RequestExecution<*> {
        return getClient(index).prepareExecution(
            Request(
                host = index.cluster.host,
                path = "${index.name}/_flush",
                method = Method.POST
            )
        )
            .onError(::showErrorMessage)
            .onSuccess {
                showInfoMessage(it.content, "Index '${index.name}' flushed")
            }
    }

    fun prepareForceMergeIndex(
        index: ElasticsearchIndex,
        maxNumSegments: Int,
        onlyExpungeDeletes: Boolean,
        flush: Boolean
    ): RequestExecution<*> {
        return getClient(index).prepareExecution(
            Request(
                host = index.cluster.host,
                path = "${index.name}/_forcemerge?" +
                        "only_expunge_deletes=$onlyExpungeDeletes&max_num_segments=$maxNumSegments&flush=$flush",
                method = Method.POST
            )
        )
            .onError(::showErrorMessage)
            .onSuccess {
                showInfoMessage(it.content, "Force merge '${index.name}' performed")
            }
    }

    fun prepareCloseIndex(index: ElasticsearchIndex): RequestExecution<*> {
        return getClient(index).prepareExecution(
            Request(
                host = index.cluster.host,
                path = "${index.name}/_close",
                method = Method.POST
            )
        )
            .onError(::showErrorMessage)
            .onSuccess {
                showInfoMessage(it.content, "Index '${index.name}' closed")
                prepareFetchIndices(index.cluster).executeSafely()
            }
    }

    fun prepareOpenIndex(index: ElasticsearchIndex): RequestExecution<*> {
        return getClient(index).prepareExecution(
            Request(
                host = index.cluster.host,
                path = "${index.name}/_open",
                method = Method.POST
            )
        )
            .onError(::showErrorMessage)
            .onSuccess {
                showInfoMessage(it.content, "Index '${index.name}' opened")
                prepareFetchIndices(index.cluster).executeSafely()
            }
    }

    fun prepareTestConnection(configuration: ClusterConfiguration): RequestExecution<*> {
        val elasticsearchClient = ElasticsearchClient(configuration)
        return elasticsearchClient.prepareTestConnection()
            .finally { _, _ ->
                elasticsearchClient.close()
            }
    }

    fun prepareExecuteRequest(request: Request, cluster: ElasticsearchCluster): RequestExecution<Response> {
        return getClient(cluster).prepareExecution(request, false)
    }

    private fun getClient(index: ElasticsearchIndex): ElasticsearchClient {
        return getClient(index.cluster)
    }

    private fun getClient(cluster: ElasticsearchCluster): ElasticsearchClient {
        return getClient(cluster.label)
    }

    @Synchronized
    private fun getClient(label: String): ElasticsearchClient {
        if (!clients.contains(label)) {
            clients.put(label, ElasticsearchClient(elasticsearchConfiguration.getConfiguration(label)!!))
        }
        return clients[label]!!
    }

    private fun showErrorMessage(e: Exception) {
        UIUtil.invokeLaterIfNeeded {
            Messages.showErrorDialog(e.message ?: e.toString(), "Error")
        }
    }

    private fun showInfoMessage(message: String, title: String) {
        UIUtil.invokeLaterIfNeeded {
            Messages.showInfoMessage(message, title)
        }
    }

    override fun dispose() {
        clients.values.forEach { it.close() }
    }

}