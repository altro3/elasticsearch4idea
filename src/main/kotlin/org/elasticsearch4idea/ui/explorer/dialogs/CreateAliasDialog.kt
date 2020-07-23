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
import com.intellij.ui.layout.ValidationInfoBuilder
import com.intellij.ui.layout.panel
import org.elasticsearch4idea.ui.explorer.ElasticsearchExplorer
import javax.swing.JTextField


class CreateAliasDialog(parent: ElasticsearchExplorer) : DialogWrapper(parent, true) {

    var alias = ""

    init {
        title = "New Alias"
        init()
    }

    override fun createCenterPanel() = panel {
        row("Alias name:") {
            textField({ alias }, { alias = it }, 20)
                .withValidationOnInput(validateAlias())
                .withValidationOnApply(validateAlias())
                .focused()
        }
    }

    private fun validateAlias(): ValidationInfoBuilder.(JTextField) -> ValidationInfo? {
        return {
            if (it.text.isNullOrBlank()) this.error("Alias name must be set")
            else null
        }
    }

}