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

package org.elasticsearch4idea.model

enum class AutoRefreshOptions(val seconds: Int, val description: String) {
    DISABLED(0, "Disable auto-refresh"),
    ONE_SECOND(1, "Auto-refresh every 1 second"),
    FIVE_SECONDS(5, "Auto-refresh every 5 seconds"),
    THIRTY_SECONDS(30, "Auto-refresh every 30 seconds"),
    SIXTY_SECONDS(60, "Auto-refresh every 60 seconds")
}