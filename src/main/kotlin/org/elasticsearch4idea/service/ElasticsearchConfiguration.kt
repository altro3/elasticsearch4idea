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

package org.elasticsearch4idea.service

import com.intellij.credentialStore.CredentialAttributes
import com.intellij.credentialStore.Credentials
import com.intellij.credentialStore.generateServiceName
import com.intellij.ide.passwordSafe.PasswordSafe
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.project.Project
import com.intellij.util.xmlb.annotations.Property
import org.elasticsearch4idea.model.ClusterConfiguration
import org.elasticsearch4idea.model.ViewMode
import java.util.concurrent.ConcurrentHashMap


@Service
@State(
    name = "ElasticsearchConfiguration",
    storages = [Storage(value = "\$PROJECT_CONFIG_DIR$/elasticsearchSettings.xml")]
)
class ElasticsearchConfiguration(private val project: Project) :
    PersistentStateComponent<ElasticsearchConfiguration.State> {

    private val clusterConfigurations: MutableMap<String, ClusterConfiguration> = ConcurrentHashMap()
    var viewMode: ViewMode = ViewMode.TEXT

    override fun getState(): State {
        val clusters = HashMap<String, ClusterConfigInternal>()
        clusterConfigurations.forEach { (label, config) ->
            var credentialsStored = false
            if (config.credentialsStored || config.credentials != null) {
                credentialsStored = storeCredentials(label, config.credentials)
            }
            var sslConfigStored = false
            if (config.sslConfigStored || config.sslConfig != null) {
                sslConfigStored = storeSSLConfig(label, config.sslConfig)
            }
            clusters.put(label, ClusterConfigInternal(config.label, config.url, credentialsStored, sslConfigStored))
        }
        return State(clusters, viewMode)
    }

    override fun loadState(state: State) {
        this.viewMode = state.viewMode
        clusterConfigurations.clear()

        state.clusterConfigurations.asSequence().map {
            ClusterConfiguration(
                label = it.value.label,
                url = it.value.url,
                credentials = if (it.value.credentialsStored) readCredentials(it.key) else null,
                sslConfig = if (it.value.sslConfigStored) readSSLConfig(it.key) else null,
                credentialsStored = it.value.credentialsStored,
                sslConfigStored = it.value.sslConfigStored
            )
        }
            .forEach { clusterConfigurations.put(it.label, it) }
    }

    private fun readCredentials(label: String): ClusterConfiguration.Credentials? {
        val credentialAttributes = createCredentialAttributes(label)
        val credentials = PasswordSafe.instance.get(credentialAttributes)
        if (credentials?.userName == null || credentials.password == null) {
            return null
        }
        return ClusterConfiguration.Credentials(credentials.userName!!, credentials.getPasswordAsString()!!)
    }

    private fun readSSLConfig(label: String): ClusterConfiguration.SSLConfig? {
        val trustStoreAttributes = createCredentialAttributes("$label-trustStore")
        val keyStoreAttributes = createCredentialAttributes("$label-keyStore")
        val trustStoreCred = PasswordSafe.instance.get(trustStoreAttributes)
        val keyStoreCred = PasswordSafe.instance.get(keyStoreAttributes)
        if (trustStoreCred?.userName == null && keyStoreCred?.userName == null) {
            return null
        }
        return ClusterConfiguration.SSLConfig(
            trustStorePath = trustStoreCred?.userName,
            keyStorePath = keyStoreCred?.userName,
            trustStorePassword = trustStoreCred?.getPasswordAsString(),
            keyStorePassword = keyStoreCred?.getPasswordAsString()
        )
    }

    private fun storeCredentials(label: String, configCredentials: ClusterConfiguration.Credentials?): Boolean {
        val credentialAttributes = createCredentialAttributes(label)
        val credentials = if (configCredentials == null) null
        else Credentials(configCredentials.user, configCredentials.password)
        PasswordSafe.instance.set(credentialAttributes, credentials)
        return credentials != null
    }

    private fun storeSSLConfig(label: String, sslConfig: ClusterConfiguration.SSLConfig?): Boolean {
        val trustStoreAttributes = createCredentialAttributes("$label-trustStore")
        val trustStoreCred = if (sslConfig?.trustStorePath == null) null
        else Credentials(sslConfig.trustStorePath, sslConfig.trustStorePassword)
        PasswordSafe.instance.set(trustStoreAttributes, trustStoreCred)

        val keyStoreAttributes = createCredentialAttributes("$label-keyStore")
        val keyStoreCred = if (sslConfig?.keyStorePath == null) null
        else Credentials(sslConfig.keyStorePath, sslConfig.keyStorePassword)
        PasswordSafe.instance.set(keyStoreAttributes, keyStoreCred)
        return trustStoreCred != null || keyStoreCred != null
    }

    private fun createCredentialAttributes(key: String): CredentialAttributes {
        return CredentialAttributes(generateServiceName("ElasticsearchPlugin", key))
    }

    fun putClusterConfiguration(clusterConfiguration: ClusterConfiguration) {
        clusterConfigurations[clusterConfiguration.label] = clusterConfiguration
    }

    fun removeClusterConfiguration(label: String) {
        val clusterConfiguration = clusterConfigurations.remove(label)
        if (clusterConfiguration?.credentialsStored == true) {
            storeCredentials(label, null)
        }
        if (clusterConfiguration?.sslConfigStored == true) {
            storeSSLConfig(label, null)
        }
    }

    fun hasConfiguration(label: String): Boolean {
        return clusterConfigurations.containsKey(label)
    }

    fun getConfigurations(): List<ClusterConfiguration> {
        return clusterConfigurations.values.sortedBy { it.label }
    }

    fun getConfiguration(name: String): ClusterConfiguration? {
        return clusterConfigurations[name]
    }

    class State(
        var clusterConfigurations: Map<String, ClusterConfigInternal> = HashMap(),
        var viewMode: ViewMode = ViewMode.TEXT
    )

    class ClusterConfigInternal(
        var label: String = "",
        var url: String = "",
        @Property(alwaysWrite = true) var credentialsStored: Boolean = true, // TODO true for backward compatibility, change to false in future release
        @Property(alwaysWrite = true) var sslConfigStored: Boolean = false
    )

}