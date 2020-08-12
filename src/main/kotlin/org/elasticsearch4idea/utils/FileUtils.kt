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

import java.math.BigInteger


object FileUtils {
    const val ONE_KB: Long = 1024
    val ONE_KB_BI = BigInteger.valueOf(ONE_KB)
    const val ONE_MB = ONE_KB * ONE_KB
    val ONE_MB_BI = ONE_KB_BI.multiply(ONE_KB_BI)
    const val ONE_GB = ONE_KB * ONE_MB
    val ONE_GB_BI = ONE_KB_BI.multiply(ONE_MB_BI)
    const val ONE_TB = ONE_KB * ONE_GB
    val ONE_TB_BI = ONE_KB_BI.multiply(ONE_GB_BI)
    const val ONE_PB = ONE_KB * ONE_TB
    val ONE_PB_BI = ONE_KB_BI.multiply(ONE_TB_BI)
    const val ONE_EB = ONE_KB * ONE_PB
    val ONE_EB_BI = ONE_KB_BI.multiply(ONE_PB_BI)
    val ONE_ZB = BigInteger.valueOf(ONE_KB).multiply(BigInteger.valueOf(ONE_EB))
    val ONE_YB = ONE_KB_BI.multiply(ONE_ZB)

    fun byteCountToDisplaySize(size: BigInteger): String {
        return when {
            size.divide(ONE_EB_BI) > BigInteger.ZERO -> {
                size.divide(ONE_EB_BI).toString() + " EB"
            }
            size.divide(ONE_PB_BI) > BigInteger.ZERO -> {
                size.divide(ONE_PB_BI).toString() + " PB"
            }
            size.divide(ONE_TB_BI) > BigInteger.ZERO -> {
                size.divide(ONE_TB_BI).toString() + " TB"
            }
            size.divide(ONE_GB_BI) > BigInteger.ZERO -> {
                size.divide(ONE_GB_BI).toString() + " GB"
            }
            size.divide(ONE_MB_BI) > BigInteger.ZERO -> {
                size.divide(ONE_MB_BI).toString() + " MB"
            }
            size.divide(ONE_KB_BI) > BigInteger.ZERO -> {
                size.divide(ONE_KB_BI).toString() + " KB"
            }
            else -> {
                "$size bytes"
            }
        }
    }

    fun byteCountToDisplaySize(size: Long): String {
        return byteCountToDisplaySize(BigInteger.valueOf(size))
    }
}