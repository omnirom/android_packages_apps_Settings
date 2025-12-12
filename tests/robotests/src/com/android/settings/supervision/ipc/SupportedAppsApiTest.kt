/*
 * Copyright (C) 2025 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.settings.supervision.ipc

import org.junit.Assert.assertEquals
import org.junit.Test

class SupportedAppsApiTest {

    private val supportedAppsApi = SupportedAppsApi()

    @Test
    fun testRequestCodec() {
        val request = SupportedAppsRequest(listOf("key1", "key2"))
        val encoded = supportedAppsApi.requestCodec.encode(request)
        val decoded = supportedAppsApi.requestCodec.decode(encoded)
        assertEquals(request, decoded)
    }

    @Test
    fun testResponseCodec() {
        val response =
            mapOf(
                "key1" to listOf(SupportedApp(title = "title1", packageName = "app1")),
                "key2" to
                    listOf(
                        SupportedApp(title = "title2", packageName = "app2"),
                        SupportedApp(title = "title3", packageName = "app3"),
                    ),
                "key3" to
                    listOf(
                        SupportedApp(title = "title2", packageName = "app2"),
                        SupportedApp(title = "title3", packageName = "app3"),
                        SupportedApp(title = "title4", packageName = "app4"),
                    ),
                "key4" to listOf(SupportedApp(title = "title4", packageName = "app4")),
                "key5" to
                    listOf(
                        SupportedApp(title = "title5", packageName = "app5"),
                        SupportedApp(title = "title6", packageName = "app6"),
                    ),
            )
        val encoded = supportedAppsApi.responseCodec.encode(response)
        val decoded = supportedAppsApi.responseCodec.decode(encoded)
        assertEquals(response, decoded)
    }

    @Test
    fun testRequestCodec_emptyList() {
        val request = SupportedAppsRequest(emptyList())
        val encoded = supportedAppsApi.requestCodec.encode(request)
        val decoded = supportedAppsApi.requestCodec.decode(encoded)
        assertEquals(request, decoded)
    }

    @Test
    fun testResponseCodec_emptyMap() {
        val response = emptyMap<String, List<SupportedApp>>()
        val encoded = supportedAppsApi.responseCodec.encode(response)
        val decoded = supportedAppsApi.responseCodec.decode(encoded)
        assertEquals(response, decoded)
    }
}
