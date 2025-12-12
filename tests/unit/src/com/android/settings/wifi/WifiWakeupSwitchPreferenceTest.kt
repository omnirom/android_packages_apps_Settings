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

package com.android.settings.wifi

import android.content.ContextWrapper
import android.location.LocationManager
import android.net.wifi.WifiManager
import androidx.test.core.app.ApplicationProvider
import com.android.settings.testutils.ResourcesUtils
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify

@Suppress("DEPRECATION")
class WifiWakeupSwitchPreferenceTest {
    private val mockWifiManager = mock<WifiManager>()
    private val mockLocationManager = mock<LocationManager>()

    private val context =
        object : ContextWrapper(ApplicationProvider.getApplicationContext()) {
            override fun getApplicationContext() = this

            override fun getSystemService(name: String): Any? =
                when (name) {
                    getSystemServiceName(WifiManager::class.java) -> mockWifiManager
                    getSystemServiceName(LocationManager::class.java) -> mockLocationManager
                    else -> super.getSystemService(name)
                }
        }

    private val preference = WifiWakeupSwitchPreference()
    private lateinit var defaultSummary: String

    @Before
    fun setUp() {
        mockWifiManager.stub {
            on { isAutoWakeupEnabled } doReturn true
            on { isScanAlwaysAvailable } doReturn true
        }
        mockLocationManager.stub { on { isLocationEnabled } doReturn true }
        defaultSummary = ResourcesUtils.getResourcesString(context, "wifi_wakeup_summary")
    }

    @Test
    fun getSummary_locationIsEnabled_returnDefaultString() {
        assertThat(preference.getSummary(context)).isEqualTo(defaultSummary)
    }

    @Test
    fun getSummary_locationIsNotEnabled_returnNotDefaultString() {
        mockLocationManager.stub { on { isLocationEnabled } doReturn false }

        assertThat(preference.getSummary(context)).isNotEqualTo(defaultSummary)
    }

    @Test
    fun getValue_allConditionsAvailable_returnTrue() {
        assertThat(preference.storage(context).getBoolean(KEY)).isTrue()
    }

    @Test
    fun getValue_autoWakeupIsNotEnabled_returnFalse() {
        mockWifiManager.stub { on { isAutoWakeupEnabled } doReturn false }

        assertThat(preference.storage(context).getBoolean(KEY)).isFalse()
    }

    @Test
    fun getValue_scanAlwaysIsNotAvailable_returnFalse() {
        mockWifiManager.stub { on { isScanAlwaysAvailable } doReturn false }

        assertThat(preference.storage(context).getBoolean(KEY)).isFalse()
    }

    @Test
    fun getValue_locationIsNotEnabled_returnFalse() {
        mockLocationManager.stub { on { isLocationEnabled } doReturn false }

        assertThat(preference.storage(context).getBoolean(KEY)).isFalse()
    }

    @Test
    fun setValue_valueTrue_setAutoWakeupEnabled() {
        preference.storage(context).setBoolean(KEY, true)

        verify(mockWifiManager).isAutoWakeupEnabled = true
    }

    @Test
    fun setValue_valueFalse_setAutoWakeupDisabled() {
        preference.storage(context).setBoolean(KEY, false)

        verify(mockWifiManager).isAutoWakeupEnabled = false
    }

    companion object {
        const val KEY = WifiWakeupSwitchPreference.KEY
    }
}
