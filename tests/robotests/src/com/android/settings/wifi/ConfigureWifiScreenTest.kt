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
import android.content.res.Resources
import android.net.wifi.WifiManager
import android.os.PowerManager
import android.os.UserManager
import android.platform.test.annotations.EnableFlags
import com.android.settings.R
import com.android.settings.flags.Flags
import com.android.settings.network.AirplaneModePreference
import com.android.settings.testutils.SettingsStoreRule
import com.android.settings.testutils2.SettingsCatalystTestCase
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub

class ConfigureWifiScreenTest : SettingsCatalystTestCase() {
    @get:Rule(order = 0) val settingsStoreRule = SettingsStoreRule()

    private val mockWifiManager = mock<WifiManager>()
    private val mockPowerManager = mock<PowerManager>()
    private val mockUserManager = mock<UserManager>()

    private val mockResources = mock<Resources>()

    private val context =
        object : ContextWrapper(appContext) {
            override fun getResources(): Resources = mockResources

            override fun getSystemService(name: String): Any? =
                when (name) {
                    getSystemServiceName(WifiManager::class.java) -> mockWifiManager
                    getSystemServiceName(PowerManager::class.java) -> mockPowerManager
                    getSystemServiceName(UserManager::class.java) -> mockUserManager
                    else -> super.getSystemService(name)
                }
        }

    private val airplaneModeDataStore = AirplaneModePreference.createDataStore(context)
    override val preferenceScreenCreator = ConfigureWifiScreen(context)

    override val flagName: String
        get() = Flags.FLAG_CATALYST_CONFIGURE_NETWORK_SETTINGS

    @Test
    @EnableFlags(Flags.FLAG_CATALYST_CONFIGURE_NETWORK_SETTINGS)
    fun isFlagEnabled_nonGuestUser_configIsTrue_shouldBeTrue() {
        mockUserManager.stub { on { isGuestUser } doReturn false }
        mockResources.stub { on { getBoolean(anyInt()) } doReturn true }

        assertThat(preferenceScreenCreator.isFlagEnabled(context)).isTrue()
    }

    @Test
    @EnableFlags(Flags.FLAG_CATALYST_CONFIGURE_NETWORK_SETTINGS)
    fun isFlagEnabled_guestUser_configIsTrue_shouldBeFalse() {
        mockUserManager.stub { on { isGuestUser } doReturn true }
        mockResources.stub { on { getBoolean(anyInt()) } doReturn true }

        assertThat(preferenceScreenCreator.isFlagEnabled(context)).isFalse()
    }

    @Test
    @EnableFlags(Flags.FLAG_CATALYST_CONFIGURE_NETWORK_SETTINGS)
    fun isFlagEnabled_nonGuestUser_configIsFalse_shouldBeFalse() {
        mockUserManager.stub { on { isGuestUser } doReturn false }
        mockResources.stub { on { getBoolean(anyInt()) } doReturn false }

        assertThat(preferenceScreenCreator.isFlagEnabled(context)).isFalse()
    }

    @Test
    fun getSummary_isAutoWakeupEnabled_showWakeupOn() {
        mockWifiManager.stub {
            on { isAutoWakeupEnabled } doReturn true
            on { isScanAlwaysAvailable } doReturn true
        }

        mockPowerManager.stub { on { isPowerSaveMode } doReturn false }

        airplaneModeDataStore.setBoolean(AirplaneModePreference.KEY, false)

        assertThat(preferenceScreenCreator.getSummary(context))
            .isEqualTo(
                context.getString(R.string.wifi_configure_settings_preference_summary_wakeup_on)
            )
    }

    @Test
    fun getSummary_isAutoWakeupDisabled_showWakeupOff() {
        mockWifiManager.stub {
            on { isAutoWakeupEnabled } doReturn false
            on { isScanAlwaysAvailable } doReturn true
        }

        mockPowerManager.stub { on { isPowerSaveMode } doReturn false }

        airplaneModeDataStore.setBoolean(AirplaneModePreference.KEY, false)

        assertThat(preferenceScreenCreator.getSummary(context))
            .isEqualTo(
                context.getString(R.string.wifi_configure_settings_preference_summary_wakeup_off)
            )
    }

    @Test
    fun getSummary_isPowerSaveModeEnabled_showWakeupOff() {
        mockWifiManager.stub {
            on { isAutoWakeupEnabled } doReturn false
            on { isScanAlwaysAvailable } doReturn true
        }

        mockPowerManager.stub { on { isPowerSaveMode } doReturn true }

        airplaneModeDataStore.setBoolean(AirplaneModePreference.KEY, false)

        assertThat(preferenceScreenCreator.getSummary(context))
            .isEqualTo(
                context.getString(R.string.wifi_configure_settings_preference_summary_wakeup_off)
            )
    }

    @Test
    fun getSummary_isAirplaneModeEnabled_showWakeupOff() {
        mockWifiManager.stub {
            on { isAutoWakeupEnabled } doReturn true
            on { isScanAlwaysAvailable } doReturn true
        }

        mockPowerManager.stub { on { isPowerSaveMode } doReturn false }

        airplaneModeDataStore.setBoolean(AirplaneModePreference.KEY, true)

        assertThat(preferenceScreenCreator.getSummary(context))
            .isEqualTo(
                context.getString(R.string.wifi_configure_settings_preference_summary_wakeup_off)
            )
    }
}
