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

import org.elasticsearch4idea.rest.model.Index
import org.elasticsearch4idea.rest.model.IndexInfo
import org.elasticsearch4idea.ui.explorer.table.TableEntry
import java.text.DateFormat
import java.util.*

class ElasticsearchIndexInfo(
    private val index: Index,
    private val indexInfo: IndexInfo
) {

    fun toTableEntryList(): List<TableEntry> {
        return listOfNotNull(
            TableEntry("name", index.name),
            TableEntry(
                "health",
                index.health?.name?.toLowerCase()
            ),
            TableEntry(
                "status",
                index.status.name.toLowerCase()
            ),
            TableEntry(
                "aliases",
                indexInfo.aliases.keys.joinToString()
            ),
            if (indexInfo.mappings.isEmpty() || indexInfo.mappings.containsKey("properties")) null
            else TableEntry(
                "types",
                indexInfo.mappings.keys.joinToString()
            ),
            TableEntry(
                "creation date",
                DateFormat.getInstance().format(Date(indexInfo.settings.index.creationDate))
            ),
            TableEntry("size", index.size),
            TableEntry(
                "documents",
                index.documents
            ),
            TableEntry(
                "deleted documents",
                index.deletedDocuments
            ),
            TableEntry(
                "primary shards",
                index.primaries
            ),
            TableEntry(
                "replica shards",
                index.replicas
            )
        )
    }
}