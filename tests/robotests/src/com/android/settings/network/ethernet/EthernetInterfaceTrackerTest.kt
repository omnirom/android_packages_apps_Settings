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
import android.net.EthernetManager
import android.net.IpConfiguration
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock

@Ignore
@RunWith(AndroidJUnit4::class)
class EthernetInterfaceTrackerTest {
    private val mockEthernetManager = mock<EthernetManager>()

    private val context: Context =
        object : ContextWrapper(ApplicationProvider.getApplicationContext()) {
            override fun getSystemService(name: String): Any? =
                when (name) {
                    Context.ETHERNET_SERVICE -> mockEthernetManager
                    else -> super.getSystemService(name)
                }
        }

    private val ethernetTrackerImpl = EthernetTrackerImpl.getInstance(context)

    @Before
    fun setUp() {
        ethernetTrackerImpl.onInterfaceStateChanged(
            "eth0",
            EthernetManager.STATE_ABSENT,
            EthernetManager.ROLE_NONE,
            IpConfiguration(),
        )
    }

    @Test
    fun getInterface_shouldReturnEmpty() {
        assertNull(ethernetTrackerImpl.getInterface("eth0"))
    }

    @Test
    fun getAvailableInterfaces_shouldReturnEmpty() {
        assertEquals(ethernetTrackerImpl.availableInterfaces.size, 0)
    }

    @Test
    fun interfacesChanged_shouldUpdateInterfaces() {
        ethernetTrackerImpl.onInterfaceStateChanged(
            "eth0",
            EthernetManager.STATE_LINK_DOWN,
            EthernetManager.ROLE_NONE,
            IpConfiguration(),
        )

        assertNotNull(ethernetTrackerImpl.getInterface("eth0"))

        ethernetTrackerImpl.onInterfaceStateChanged(
            "eth0",
            EthernetManager.STATE_ABSENT,
            EthernetManager.ROLE_NONE,
            IpConfiguration(),
        )

        assertNull(ethernetTrackerImpl.getInterface("eth0"))
    }
}
