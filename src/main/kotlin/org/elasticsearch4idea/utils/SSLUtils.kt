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

import org.apache.http.conn.ssl.TrustSelfSignedStrategy
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
            sslConfig.keyStorePassword,
            sslConfig.selfSigned
        )
    }

    private fun createSSLContext(
        trustStorePathStr: String?,
        keyStorePathStr: String?,
        trustStorePass: String?,
        keyStorePass: String?,
        selfSigned: Boolean
    ): SSLContext? {
        val trustStore: KeyStore? = loadKeyStore(trustStorePathStr, trustStorePass)
        val keyStore: KeyStore? = loadKeyStore(keyStorePathStr, keyStorePass)
        if (trustStore == null && keyStore == null) {
            return null
        }
        val builder = SSLContexts.custom()
        if (trustStore != null) {
            val trustStrategy = if (selfSigned) TrustSelfSignedStrategy.INSTANCE else null
            builder.loadTrustMaterial(trustStore, trustStrategy)
        }
        if (keyStore != null) {
            builder.loadKeyMaterial(keyStore, keyStorePass?.toCharArray())
        }
        return builder.build()
    }

    private fun loadKeyStore(keyStorePathStr: String?, keyStorePass: String?): KeyStore? {
        val keyStorePath: Path = if (keyStorePathStr.isNullOrBlank()) return null else Paths.get(keyStorePathStr)
        val keyStore: KeyStore = if (keyStorePathStr.endsWith(".jks")) {
            KeyStore.getInstance("jks")
        } else {
            KeyStore.getInstance("pkcs12")
        }
        Files.newInputStream(keyStorePath)
            .use { inputStream -> keyStore.load(inputStream, keyStorePass?.toCharArray()) }
        return keyStore
    }
}