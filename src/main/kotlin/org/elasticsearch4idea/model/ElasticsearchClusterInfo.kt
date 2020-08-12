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

import org.elasticsearch4idea.rest.model.ClusterStats
import org.elasticsearch4idea.ui.explorer.table.TableEntry
import org.elasticsearch4idea.utils.FileUtils

class ElasticsearchClusterInfo(
    private val clusterStats: ClusterStats
) {

    fun toTableEntryList(): List<TableEntry> {
        return listOf(
            TableEntry("name", clusterStats.clusterName),
            TableEntry(
                "version",
                clusterStats.nodes.versions.joinToString(", ")
            ),
            TableEntry(
                "health",
                clusterStats.status.name.toLowerCase()
            ),
            TableEntry(
                "size",
                FileUtils.byteCountToDisplaySize(clusterStats.indices.store.sizeInBytes)
            ),
            TableEntry(
                "documents",
                clusterStats.indices.docs.count.toString()
            ),
            TableEntry(
                "deleted documents",
                clusterStats.indices.docs.deleted.toString()
            ),
            TableEntry(
                "nodes",
                clusterStats.nodes.count.total.toString()
            ),
            TableEntry(
                "indices",
                clusterStats.indices.count.toString()
            ),
            TableEntry(
                "shards",
                clusterStats.indices.shards.total.toString()
            ),
            TableEntry(
                "primary shards",
                clusterStats.indices.shards.primaries.toString()
            )
        )
    }
}