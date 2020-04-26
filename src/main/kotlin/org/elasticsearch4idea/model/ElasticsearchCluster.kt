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

import org.elasticsearch4idea.rest.model.HealthStatus

class ElasticsearchCluster(
    val label: String
) {
    var clusterName: String? = null
    var healthStatus: HealthStatus? = null
    var host: String? = null
    val indices: MutableList<ElasticsearchIndex> = ArrayList()
    var status: Status = Status.NOT_LOADED

    fun addIndex(index: ElasticsearchIndex) {
        indices.add(index)
    }

    fun isLoaded(): Boolean {
        return status == Status.LOADED
    }

    fun isLoading(): Boolean {
        return status == Status.LOADING
    }

    enum class Status {
        LOADED,
        LOADING,
        NOT_LOADED
    }
}