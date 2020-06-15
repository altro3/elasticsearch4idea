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
package org.elasticsearch4idea.ui.explorer.dialogs

import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.ui.components.JBTextField
import com.intellij.ui.layout.ValidationInfoBuilder
import com.intellij.ui.layout.panel
import org.elasticsearch4idea.ui.explorer.ElasticsearchExplorer
import org.elasticsearch4idea.utils.isNotLowercase


class CreateIndexDialog(parent: ElasticsearchExplorer) : DialogWrapper(parent, true) {

    var indexName = ""
    var numberOfShards = "1"
    var numberOfReplicas = "0"

    init {
        title = "New Index"
        init()
    }

    override fun createCenterPanel() = panel {
        row("Index name:") {
            textField({ indexName }, { indexName = it }, 20)
                .withValidationOnInput(validateIndexName())
                .withValidationOnApply(validateIndexName())
        }
        row("Number of shards:") {
            textField({ numberOfShards }, { numberOfShards = it }, 20)
                .withValidationOnInput(validateNumberOfShards())
                .withValidationOnApply(validateNumberOfShards())
        }
        row("Number of replicas:") {
            textField({ numberOfReplicas }, { numberOfReplicas = it }, 20)
                .withValidationOnInput(validateNumberOfReplicas())
                .withValidationOnApply(validateNumberOfReplicas())
        }
    }

    private fun validateIndexName(): ValidationInfoBuilder.(JBTextField) -> ValidationInfo? {
        return {
            when {
                it.text.isNullOrBlank() -> this.error("Index name must be set")
                sequenceOf('-', '_', '+')
                    .any { ch -> it.text.startsWith(ch) } -> this.error("Index name cannot start with '-', '_', '+'")
                sequenceOf('\\', '/', '*', '?', '"', '<', '>', '|', ' ', ',', '#', ':')
                    .any { ch -> it.text.contains(ch) } -> this.error("Index name cannot contain '\\', '/', '*', '?', '\"', '<', '>', '|', ' ', ',', '#', ':'")
                sequenceOf(".", "..")
                    .any { str -> it.text == str } -> this.error("Index name cannot be . or ..")
                it.text.isNotLowercase() -> this.error("Index name must be in lowercase")
                else -> null
            }
        }
    }

    private fun validateNumberOfShards(): ValidationInfoBuilder.(JBTextField) -> ValidationInfo? {
        return {
            val number = it.text.toIntOrNull()
            if (number == null || number <= 0) {
                this.error("Number of shards must be positive number")
            } else {
                null
            }
        }
    }

    private fun validateNumberOfReplicas(): ValidationInfoBuilder.(JBTextField) -> ValidationInfo? {
        return {
            val number = it.text.toIntOrNull()
            if (number == null || number < 0) {
                this.error("Number of replicas must be non-negative number")
            } else {
                null
            }
        }
    }
}