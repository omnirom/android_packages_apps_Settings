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

package com.android.settings.network.ethernet

import android.content.Context
import android.content.ContextWrapper
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock

@RunWith(AndroidJUnit4::class)
class EthernetInterfaceDetailsControllerTest {
    private val ethernetInterfaceDetailsFragment = EthernetInterfaceDetailsFragment()
    private val lifecycle = mock<Lifecycle>()

    private val context: Context =
        object : ContextWrapper(ApplicationProvider.getApplicationContext()) {}

    private val ethernetInterfaceDetailsController =
        EthernetInterfaceDetailsController(
            context,
            ethernetInterfaceDetailsFragment,
            "eth0",
            lifecycle,
        )

    @Test
    fun isAvailable_ShouldReturnTrue() {
        assertTrue(ethernetInterfaceDetailsController.isAvailable())
    }

    @Test
    fun getPreferencKey_shouldReturnExpectedKey() {
        assertEquals(ethernetInterfaceDetailsController.getPreferenceKey(), "ethernet_details")
    }
}
