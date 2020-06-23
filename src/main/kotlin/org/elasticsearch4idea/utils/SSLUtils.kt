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

import org.apache.http.ssl.SSLContexts
import org.elasticsearch4idea.model.ClusterConfiguration
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.security.KeyStore
import javax.net.ssl.SSLContext


object SSLUtils {

    fun createSSLContext(sslConfig: ClusterConfiguration.SSLConfig?): SSLContext? {
        if (sslConfig == null) {
            return null
        }
        return createSSLContext(
            sslConfig.trustStorePath,
            sslConfig.keyStorePath,
            sslConfig.trustStorePassword,
            sslConfig.keyStorePassword
        )
    }

    private fun createSSLContext(
        trustStorePathStr: String?,
        keyStorePathStr: String?,
        trustStorePass: String?,
        keyStorePass: String?
    ): SSLContext? {
        val trustStore: KeyStore? = loadKeyStore(trustStorePathStr, trustStorePass)
        val keyStore: KeyStore? = loadKeyStore(keyStorePathStr, keyStorePass)
        if (trustStore == null && keyStore == null) {
            return null
        }
        val builder = SSLContexts.custom()
        if (trustStore != null) {
            builder.loadTrustMaterial(trustStore, null)
        }
        if (keyStore != null) {
            builder.loadKeyMaterial(keyStore, keyStorePass?.toCharArray())
        }
        return builder.build()
    }

    private fun loadKeyStore(keyStorePathStr: String?, keyStorePass: String?): KeyStore? {
        val keyStorePath: Path? = if (keyStorePathStr.isNullOrBlank()) null else Paths.get(keyStorePathStr)
        var keyStore: KeyStore? = null
        if (keyStorePath != null) {
            keyStore = KeyStore.getInstance("pkcs12")
            Files.newInputStream(keyStorePath)
                .use { inputStream -> keyStore.load(inputStream, keyStorePass?.toCharArray()) }
        }
        return keyStore
    }
}