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

import com.intellij.openapi.components.service
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.openapi.util.Ref
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTextField
import com.intellij.ui.layout.PropertyBinding
import com.intellij.ui.layout.ValidationInfoBuilder
import com.intellij.ui.layout.panel
import com.intellij.ui.layout.withTextBinding
import icons.Icons
import org.apache.http.Header
import org.apache.http.HttpHeaders
import org.apache.http.HttpHost
import org.apache.http.message.BasicHeader
import org.elasticsearch4idea.model.ClusterConfiguration
import org.elasticsearch4idea.rest.ElasticsearchClient
import org.elasticsearch4idea.service.ElasticsearchConfiguration
import org.elasticsearch4idea.ui.explorer.ElasticsearchExplorer
import java.awt.Dimension
import java.awt.event.ActionEvent
import javax.swing.JPasswordField

class ClusterConfigurationDialog(
    parent: ElasticsearchExplorer,
    project: Project,
    previousConfiguration: ClusterConfiguration?,
    private val editing: Boolean
) : DialogWrapper(parent, true) {

    private val elasticsearchConfiguration = project.service<ElasticsearchConfiguration>()
    private val elasticsearchClient = project.service<ElasticsearchClient>()
    private val dialogPanel: DialogPanel
    private val feedbackLabel: JBLabel = JBLabel()
    private val urlField: JBTextField = JBTextField()
    private val passwordField: JPasswordField = JPasswordField()

    private val previousName = if (editing) previousConfiguration?.label else null
    private var name = previousConfiguration?.label ?: "@localhost"
    private var url = previousConfiguration?.url ?: "http://localhost:9200"
    private var user = previousConfiguration?.credentials?.user ?: ""
    private var password = previousConfiguration?.credentials?.password ?: ""

    init {
        feedbackLabel.isVisible = false
        feedbackLabel.maximumSize = Dimension(400, 20)
        urlField.text = url
        passwordField.text = password
        dialogPanel = createDialogPanel()
        init()
    }

    private fun createDialogPanel() = panel {
        row("Name:") {
            textField({ name }, { name = it }, 30)
                .withValidationOnInput(validateName())
                .withValidationOnApply(validateName())
                .focused()
        }
        row("URL:") {
            urlField()
                .withTextBinding(PropertyBinding({ url }, { url = it }))
                .withValidationOnInput(validateURL())
                .withValidationOnApply(validateURL())
        }
        row("User:") {
            textField({ user }, { user = it })
        }
        row("Password:") {
            passwordField()
                .withTextBinding(PropertyBinding({ password }, { password = it }))
        }
        row {
            button("Test Connection", ::testConnection)
        }
        row {
            feedbackLabel()
        }
    }

    override fun createCenterPanel() = dialogPanel

    private fun testConnection(event: ActionEvent) {
        val excRef = Ref<Exception>()
        val progressManager = ProgressManager.getInstance()
        progressManager.runProcessWithProgressSynchronously({
            val progressIndicator = progressManager.progressIndicator
            if (progressIndicator != null) {
                progressIndicator.text = "Connecting to Elasticsearch cluster..."
            }
            dialogPanel.apply()
            try {
                if (url.isBlank()) {
                    throw java.lang.IllegalArgumentException("URL must be set")
                } else {
                    val basicAuthHeader =
                        getCredentials()?.let { BasicHeader(HttpHeaders.AUTHORIZATION, it.toBasicAuthHeader()) }
                    val headers = if (basicAuthHeader == null) emptyList<Header>() else listOf(basicAuthHeader)
                    elasticsearchClient.getClusterInfo(HttpHost.create(urlField.text), headers)
                }
            } catch (ex: Exception) {
                excRef.set(ex)
            }
        }, "Testing Connection", true, null)
        if (!excRef.isNull) {
            val errorMessage = excRef.get().message
            setErrorMessage("Connection test failed" + if (errorMessage.isNullOrBlank()) "" else ": $errorMessage")
        } else {
            setSuccessMessage("Connection test successful")
        }
    }

    private fun setErrorMessage(message: String?) {
        feedbackLabel.isVisible = true
        feedbackLabel.icon = Icons.ERROR
        feedbackLabel.foreground = JBColor.RED
        feedbackLabel.text = message
        feedbackLabel.toolTipText = "<html><p width=\"500\">$message</p></html>"
    }

    private fun setSuccessMessage(message: String) {
        feedbackLabel.isVisible = true
        feedbackLabel.icon = Icons.SUCCESS
        feedbackLabel.foreground = JBColor.GRAY
        feedbackLabel.text = message
    }

    private fun validateName(): ValidationInfoBuilder.(JBTextField) -> ValidationInfo? {
        return {
            val name = it.text
            if (name.isNullOrBlank()) {
                this.error("Name must be set")
            } else if (!editing || previousName != name) {
                val isDuplicateName = elasticsearchConfiguration.hasConfiguration(name)
                if (isDuplicateName) {
                    this.error("Name must be unique")
                } else {
                    null
                }
            } else {
                null
            }
        }
    }

    private fun validateURL(): ValidationInfoBuilder.(JBTextField) -> ValidationInfo? {
        return {
            if (it.text.isNullOrBlank()) {
                this.error("URL must be set")
            } else {
                null
            }
        }
    }

    fun getConfiguration(): ClusterConfiguration {
        return ClusterConfiguration(name, url, getCredentials())
    }

    private fun getCredentials(): ClusterConfiguration.Credentials? {
        return if (user.isNotBlank()) {
            ClusterConfiguration.Credentials(user, password)
        } else {
            null
        }
    }

}