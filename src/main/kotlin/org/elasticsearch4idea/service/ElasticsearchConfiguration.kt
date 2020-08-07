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

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
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
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.collections.HashMap


@Service
@State(
    name = "ElasticsearchConfiguration",
    storages = [Storage(value = "\$PROJECT_CONFIG_DIR$/elasticsearchSettings.xml")]
)
class ElasticsearchConfiguration(private val project: Project) :
    PersistentStateComponent<ElasticsearchConfiguration.State> {

    private val clusterConfigurations: MutableMap<String, ClusterConfiguration> = ConcurrentHashMap()

    override fun getState(): State {
        val clusters = HashMap<String, ClusterConfigInternal>()
        clusterConfigurations.forEach { (label, config) ->
            var credentialsStored = false
            if (config.credentialsStored || config.credentials != null || config.sslConfigStored || config.sslConfig != null) {
                credentialsStored = storeCredentials(config.id, config.credentials, config.sslConfig)
            }
            val id = if (config.id.isEmpty()) UUID.randomUUID().toString() else config.id
            clusters.put(
                label,
                ClusterConfigInternal(id, config.label, config.url, credentialsStored, credentialsStored)
            )
        }
        return State(clusters)
    }

    override fun loadState(state: State) {
        clusterConfigurations.clear()

        state.clusterConfigurations.asSequence().map {
            var credentials: ClusterConfiguration.Credentials? = null
            var sslConfig: ClusterConfiguration.SSLConfig? = null
            if (it.value.credentialsStored || it.value.sslConfigStored) {
                val secrets = readSecrets(it.value.id)
                if (secrets == null) {
                    // TODO remove in future release
                    credentials = if (it.value.credentialsStored) readCredentialsOld(it.key) else null
                    sslConfig = if (it.value.sslConfigStored) readSSLConfigOld(it.key) else null
                } else {
                    credentials = if (secrets.user == null || secrets.password == null)
                        null
                    else ClusterConfiguration.Credentials(
                        user = secrets.user,
                        password = secrets.password
                    )
                    sslConfig = if (secrets.trustStorePath == null && secrets.keyStorePath == null) null
                    else ClusterConfiguration.SSLConfig(
                        trustStorePath = secrets.trustStorePath,
                        trustStorePassword = secrets.trustStorePassword,
                        keyStorePath = secrets.keyStorePath,
                        keyStorePassword = secrets.keyStorePassword
                    )
                }
            }

            ClusterConfiguration(
                id = it.value.id,
                label = it.value.label,
                url = it.value.url,
                credentials = credentials,
                sslConfig = sslConfig,
                credentialsStored = it.value.credentialsStored,
                sslConfigStored = it.value.sslConfigStored
            )
        }
            .forEach { clusterConfigurations.put(it.label, it) }
    }

    private fun readSecrets(key: String): Secrets? {
        val credentialAttributes = createCredentialAttributes(key)
        val credentials = PasswordSafe.instance.get(credentialAttributes)
        if (credentials?.password == null) {
            return null
        }
        val secretsJson = String(Base64.getDecoder().decode(credentials.getPasswordAsString()))
        return jacksonObjectMapper().readValue<Secrets>(secretsJson)
    }

    private fun readCredentialsOld(label: String): ClusterConfiguration.Credentials? {
        val credentialAttributes = createCredentialAttributes(label)
        val credentials = PasswordSafe.instance.get(credentialAttributes)
        if (credentials?.userName == null || credentials.password == null) {
            return null
        }
        return ClusterConfiguration.Credentials(credentials.userName!!, credentials.getPasswordAsString()!!)
    }

    private fun readSSLConfigOld(label: String): ClusterConfiguration.SSLConfig? {
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

    private fun storeCredentials(
        key: String,
        configCredentials: ClusterConfiguration.Credentials?,
        sslConfig: ClusterConfiguration.SSLConfig?
    ): Boolean {
        val credentialAttributes = createCredentialAttributes(key)
        val secrets =
            if (configCredentials?.user == null
                && sslConfig?.trustStorePath == null
                && sslConfig?.keyStorePath == null
            ) null
            else Secrets(
                user = configCredentials?.user,
                password = configCredentials?.password,
                trustStorePath = sslConfig?.trustStorePath,
                trustStorePassword = sslConfig?.trustStorePassword,
                keyStorePath = sslConfig?.keyStorePath,
                keyStorePassword = sslConfig?.keyStorePassword
            )
        if (secrets == null) {
            PasswordSafe.instance.set(credentialAttributes, null)
        } else {
            val secretsJson = jacksonObjectMapper().writeValueAsString(secrets)
            val secretsBase64 = Base64.getEncoder().encodeToString(secretsJson.toByteArray())
            PasswordSafe.instance.set(credentialAttributes, Credentials(null, secretsBase64))
        }
        return secrets != null
    }

    private fun createCredentialAttributes(key: String): CredentialAttributes {
        return CredentialAttributes(generateServiceName("ElasticsearchPlugin", key))
    }

    fun putClusterConfiguration(clusterConfiguration: ClusterConfiguration) {
        clusterConfigurations[clusterConfiguration.label] = clusterConfiguration
    }

    fun removeClusterConfiguration(label: String) {
        val clusterConfiguration = clusterConfigurations.remove(label)
        if (clusterConfiguration?.credentialsStored == true || clusterConfiguration?.sslConfigStored == true) {
            storeCredentials(clusterConfiguration.id, null, null)
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
        var id: String = "",
        var label: String = "",
        var url: String = "",
        @Property(alwaysWrite = true) var credentialsStored: Boolean = true, // TODO true for backward compatibility, change to false in future release
        @Property(alwaysWrite = true) var sslConfigStored: Boolean = false
    )

    class Secrets(
        val user: String?,
        val password: String?,
        val trustStorePath: String?,
        val keyStorePath: String?,
        val trustStorePassword: String?,
        val keyStorePassword: String?
    )
}