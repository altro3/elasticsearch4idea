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

package org.elasticsearch4idea.utils

import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.progress.impl.BackgroundableProcessIndicator
import com.intellij.openapi.progress.util.AbstractProgressIndicatorExBase
import org.elasticsearch4idea.model.RequestExecution

object TaskUtils {

    fun runBackgroundTask(title: String, task: () -> RequestExecution<*>) {
        ProgressManager.getInstance().run(object : Task.Backgroundable(null, title, true) {

            override fun run(indicator: ProgressIndicator) {
                val requestExecution = task.invoke()
                val progressIndicator = object : AbstractProgressIndicatorExBase() {
                    override fun cancel() {
                        requestExecution.abort()
                    }
                }
                progressIndicator.isIndeterminate = false
                (indicator as BackgroundableProcessIndicator).addStateDelegate(progressIndicator)
                requestExecution.execute()
            }
        })
    }
}