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
import androidx.lifecycle.Lifecycle
import androidx.preference.PreferenceScreen
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settingslib.RestrictedSwitchPreference
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify

@RunWith(AndroidJUnit4::class)
class EthernetSwitchPreferenceControllerTest {
    private val mockEthernetManager = mock<EthernetManager>()
    private val preferenceScreen = mock<PreferenceScreen>()
    private val switchPreference = mock<RestrictedSwitchPreference>()

    private val context: Context =
        object : ContextWrapper(ApplicationProvider.getApplicationContext()) {
            override fun getSystemService(name: String): Any? =
                when (name) {
                    getSystemServiceName(EthernetManager::class.java) -> mockEthernetManager
                    else -> super.getSystemService(name)
                }
        }

    private val lifecycle = mock<Lifecycle>()

    private val controller: EthernetSwitchPreferenceController =
        EthernetSwitchPreferenceController(context, lifecycle)

    @Before
    fun setUp() {
        preferenceScreen.stub {
            on { findPreference<RestrictedSwitchPreference>("main_toggle_ethernet") } doReturn
                switchPreference
        }
        controller.displayPreference(preferenceScreen)
    }

    @Test
    fun getPreferenceKey_shouldReturnCorrectKey() {
        assertEquals(controller.getPreferenceKey(), "main_toggle_ethernet")
    }

    @Test
    fun onPreferenceChange_shouldCallEthernetManager() {
        assertTrue(controller.onPreferenceChange(switchPreference, true))
        verify(mockEthernetManager).setEthernetEnabled(true)

        assertTrue(controller.onPreferenceChange(switchPreference, false))
        verify(mockEthernetManager).setEthernetEnabled(false)
    }

    @Test
    fun ethernetEnabled_shouldUpdatePreferenceState() {
        switchPreference.stub { on { isChecked } doReturn false }

        controller.onEthernetStateChanged(EthernetManager.ETHERNET_STATE_ENABLED)

        verify(switchPreference).setChecked(true)
    }

    @Test
    fun ethernetDisabled_shouldUpdatePreferenceState() {
        switchPreference.stub { on { isChecked } doReturn true }

        controller.onEthernetStateChanged(EthernetManager.ETHERNET_STATE_DISABLED)

        verify(switchPreference).setChecked(false)
    }
}
