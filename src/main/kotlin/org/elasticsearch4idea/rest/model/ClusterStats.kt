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

package org.elasticsearch4idea.rest.model

class ClusterStats(
    val clusterName: String,
    val status: HealthStatus,
    val indices: Indices,
    val nodes: Nodes
) {

    class Indices(
        val count: Int,
        val shards: Shards,
        val docs: Docs,
        val store: Store
    )

    class Shards(
        val total: Int,
        val primaries: Int
    )

    class Docs(
        val count: Long,
        val deleted: Long
    )

    class Store(
        val sizeInBytes: Long
    )

    class Nodes(
        val count: Count,
        val versions: List<String>
    )

    class Count(
        val total: Int
    )
}