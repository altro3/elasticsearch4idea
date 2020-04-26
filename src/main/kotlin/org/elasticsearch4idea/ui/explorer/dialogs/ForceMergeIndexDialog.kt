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


class ForceMergeIndexDialog(parent: ElasticsearchExplorer) : DialogWrapper(parent, true) {

    var maxNumSegments = "1"
    var onlyExpungeDeletes = false
    var flush = true

    init {
        title = "Force Merge"
        init()
    }

    override fun createCenterPanel() = panel {
        row("Max number of segments:") {
            textField({ maxNumSegments }, { maxNumSegments = it }, 20)
                .withValidationOnInput(validateMaxNumSegments())
                .withValidationOnApply(validateMaxNumSegments())
                .focused()
        }
        row("Only expunge deletes:") {
            checkBox("", { onlyExpungeDeletes }, { onlyExpungeDeletes = it })
        }
        row("Flush after merge:") {
            checkBox("", { flush }, { flush = it })
        }
    }

    private fun validateMaxNumSegments(): ValidationInfoBuilder.(JBTextField) -> ValidationInfo? {
        return {
            val number = it.text.toIntOrNull()
            if (number == null || number <= 0) {
                this.error("Max number of segments must be positive number")
            } else {
                null
            }
        }
    }

}