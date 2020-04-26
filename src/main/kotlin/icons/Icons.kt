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

package icons

import com.intellij.ui.IconManager
import javax.swing.Icon

object Icons {
    val CLUSTER = load("/icons/cluster.svg")
    val CLUSTER_GREEN = load("/icons/cluster_green.svg")
    val CLUSTER_YELLOW = load("/icons/cluster_yellow.svg")
    val CLUSTER_RED = load("/icons/cluster_red.svg")
    val INDEX_GREEN = load("/icons/index_green.svg")
    val INDEX_YELLOW = load("/icons/index_yellow.svg")
    val INDEX_RED = load("/icons/index_red.svg")
    val INDEX_CLOSED = load("/icons/index_brown.svg")
    val ERROR = load("/icons/error.svg")
    val SUCCESS = load("/icons/success.svg")
    val ELASTICSEARCH_LOGO = load("/icons/logo-elasticsearch-13.svg")
    val QUERY_EDITOR = load("/icons/query_editor.svg")
    val AUTO_REFRSH = load("/icons/auto_refresh.svg")

    private fun load(path: String): Icon {
        return IconManager.getInstance().getIcon(path, Icons::class.java)
    }
}