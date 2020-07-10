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
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.fileChooser.PathChooserDialog
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTabbedPane
import com.intellij.ui.components.JBTextField
import com.intellij.ui.layout.PropertyBinding
import com.intellij.ui.layout.ValidationInfoBuilder
import com.intellij.ui.layout.panel
import com.intellij.ui.layout.withTextBinding
import icons.Icons
import org.elasticsearch4idea.model.ClusterConfiguration
import org.elasticsearch4idea.service.ElasticsearchConfiguration
import org.elasticsearch4idea.service.ElasticsearchManager
import org.elasticsearch4idea.ui.explorer.ElasticsearchExplorer
import org.elasticsearch4idea.utils.TaskUtils
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
    private val elasticsearchManager = project.service<ElasticsearchManager>()
    private val dialogPanel: DialogPanel
    private val feedbackLabel: JBLabel = JBLabel()
    private val urlField: JBTextField = JBTextField()
    private val passwordField: JPasswordField = JPasswordField()
    private val truststorePasswordField: JPasswordField = JPasswordField()
    private val keystorePasswordField: JPasswordField = JPasswordField()
    private val tabbedPane: JBTabbedPane = JBTabbedPane(1)
    private val generalPanel: DialogPanel
    private val sslPanel: DialogPanel

    private val previousName = if (editing) previousConfiguration?.label else null
    private val previousCredentialsStored = if (editing) previousConfiguration?.credentialsStored ?: false else false
    private val previousSslConfigStored = if (editing) previousConfiguration?.sslConfigStored ?: false else false
    private var name = previousConfiguration?.label ?: "@localhost"
    private var url = previousConfiguration?.url ?: "http://localhost:9200"
    private var user = previousConfiguration?.credentials?.user ?: ""
    private var password = previousConfiguration?.credentials?.password ?: ""
    private var trustStorePath = previousConfiguration?.sslConfig?.trustStorePath ?: ""
    private var keyStorePath = previousConfiguration?.sslConfig?.keyStorePath ?: ""
    private var trustStorePassword = previousConfiguration?.sslConfig?.trustStorePassword ?: ""
    private var keyStorePassword = previousConfiguration?.sslConfig?.keyStorePassword ?: ""

    init {
        feedbackLabel.isVisible = false
        feedbackLabel.maximumSize = Dimension(400, 20)
        urlField.text = url
        passwordField.text = password
        truststorePasswordField.text = trustStorePassword
        keystorePasswordField.text = keyStorePassword
        generalPanel = createGeneralPanel()
        sslPanel = createSSLPanel()
        tabbedPane.addTab("General", generalPanel)
        tabbedPane.addTab("SSL", sslPanel)
        dialogPanel = createDialogPanel()
        init()
    }

    private fun createDialogPanel() = panel {
        row {
            component(tabbedPane)
                .withValidationOnApply {
                    validateTabs()
                }
        }
        row {
            button("Test Connection", ::testConnection)
        }
        row {
            feedbackLabel()
        }
        onGlobalApply {
            generalPanel.apply()
            sslPanel.apply()
        }
    }

    private fun createGeneralPanel() = panel {
        row("Name:") {
            textField({ name }, { name = it })
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
    }

    private fun createSSLPanel() = panel {
        row("Truststore:") {
            textFieldWithBrowseButton(PropertyBinding({ trustStorePath }, { trustStorePath = it }),
                fileChooserDescriptor = FileChooserDescriptorFactory.createSingleFileDescriptor(".p12")
                    .also { it.putUserData(PathChooserDialog.PREFER_LAST_OVER_EXPLICIT, false) }
            )
        }
        row("Truststore password:") {
            truststorePasswordField()
                .withTextBinding(PropertyBinding({ trustStorePassword }, { trustStorePassword = it }))
        }
        row("Keystore:") {
            textFieldWithBrowseButton(PropertyBinding({ keyStorePath }, { keyStorePath = it }),
                fileChooserDescriptor = FileChooserDescriptorFactory.createSingleFileDescriptor(".p12")
                    .also { it.putUserData(PathChooserDialog.PREFER_LAST_OVER_EXPLICIT, false) }
            )
        }
        row("Keystore password:") {
            keystorePasswordField()
                .withTextBinding(PropertyBinding({ keyStorePassword }, { keyStorePassword = it }))
        }
    }

    private fun validateTabs(): ValidationInfo? {
        var valInfo: ValidationInfo? = generalPanel.componentValidateCallbacks.values.asSequence()
            .map { it.invoke() }
            .filterNotNull()
            .firstOrNull()
        if (valInfo != null) {
            tabbedPane.selectedIndex = 0
            return valInfo
        }
        valInfo = sslPanel.componentValidateCallbacks.values.asSequence()
            .map { it.invoke() }
            .filterNotNull()
            .firstOrNull()
        if (valInfo != null) {
            tabbedPane.selectedIndex = 1
            return valInfo
        }
        return null
    }

    override fun createCenterPanel() = dialogPanel

    private fun testConnection(event: ActionEvent) {
        TaskUtils.runBackgroundTask("Connecting to Elasticsearch cluster...") {
            generalPanel.apply()
            sslPanel.apply()
            elasticsearchManager.prepareTestConnection(getConfiguration())
                .onError {
                    val errorMessage = it.message
                    setErrorMessage("Connection test failed" + if (errorMessage.isNullOrBlank()) "" else ": $errorMessage")
                }
                .onSuccess {
                    setSuccessMessage("Connection test successful")
                }
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
        return ClusterConfiguration(
            label = name,
            url = url,
            credentials = getCredentials(),
            sslConfig = getSSLConfig(),
            credentialsStored = previousCredentialsStored,
            sslConfigStored = previousSslConfigStored
        )
    }

    private fun getCredentials(): ClusterConfiguration.Credentials? {
        return if (user.isNotBlank()) {
            ClusterConfiguration.Credentials(user, password)
        } else {
            null
        }
    }

    private fun getSSLConfig(): ClusterConfiguration.SSLConfig? {
        if (trustStorePath.isBlank() && keyStorePath.isBlank()) {
            return null
        }
        return ClusterConfiguration.SSLConfig(
            trustStorePath = if (trustStorePath.isBlank()) null else trustStorePath,
            keyStorePath = if (keyStorePath.isBlank()) null else keyStorePath,
            trustStorePassword = trustStorePassword,
            keyStorePassword = keyStorePassword
        )
    }

}