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
package com.android.settings.network.telephony.satellite

import android.content.Context
import android.content.Intent
import android.os.PersistableBundle
import android.telephony.CarrierConfigManager.CARRIER_ROAMING_NTN_CONNECT_AUTOMATIC
import android.telephony.CarrierConfigManager.KEY_CARRIER_ROAMING_NTN_CONNECT_TYPE_INT
import android.telephony.CarrierConfigManager.KEY_SATELLITE_ATTACH_SUPPORTED_BOOL
import androidx.lifecycle.testing.TestLifecycleOwner
import androidx.preference.Preference
import androidx.preference.PreferenceManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.network.CarrierConfigCache
import com.android.settings.network.SatelliteRepository
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub
import org.mockito.kotlin.whenever

@RunWith(AndroidJUnit4::class)
class SatelliteSettingPreferenceControllerTest {
    private val mockCarrierConfigCache = mock<CarrierConfigCache>()
    private var mockSatelliteRepository =
        mock<SatelliteRepository>().stub {
            on { isSatelliteAccessConfigurationForCurrentLocationFlow(TEST_SUB_ID) }
                .thenReturn(flowOf(true))
        }
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val preferenceIntent = Intent()
    private val preference =
        Preference(context).apply {
            key = KEY
            intent = preferenceIntent
        }
    private val preferenceScreen = PreferenceManager(context).createPreferenceScreen(context)

    private val controller =
        SatelliteSettingPreferenceController(
            context = context,
            key = KEY,
            carrierConfigCache = mockCarrierConfigCache,
            satelliteRepository = mockSatelliteRepository,
        )

    @Test
    fun isVisible_outOfFence_disabled() = runBlocking {
        val carrierConfigs = PersistableBundle()
        carrierConfigs.putBoolean(KEY_SATELLITE_ATTACH_SUPPORTED_BOOL, true)
        whenever(mockCarrierConfigCache.getSpecificConfigsForSubId(eq(TEST_SUB_ID), any()))
            .thenReturn(carrierConfigs)
        whenever(
                mockSatelliteRepository.isSatelliteAccessConfigurationForCurrentLocationFlow(
                    TEST_SUB_ID
                )
            )
            .thenReturn(flowOf(false))

        preference.key = controller.preferenceKey

        controller.initialize(TEST_SUB_ID)
        preferenceScreen.addPreference(preference)

        controller.displayPreference(preferenceScreen)
        controller.onViewCreated(TestLifecycleOwner())
        delay(100)

        assertThat(preference.isEnabled).isEqualTo(false)
    }

    @Test
    fun isVisible_satelliteIsNotSupported_inVisible() = runBlocking {
        val carrierConfigs = PersistableBundle()
        carrierConfigs.putBoolean(KEY_SATELLITE_ATTACH_SUPPORTED_BOOL, false)
        whenever(mockCarrierConfigCache.getSpecificConfigsForSubId(eq(TEST_SUB_ID), any()))
            .thenReturn(carrierConfigs)
        controller.initialize(TEST_SUB_ID)
        preferenceScreen.addPreference(preference)
        controller.displayPreference(preferenceScreen)

        controller.onViewCreated(TestLifecycleOwner())
        delay(100)

        assertThat(preference.isVisible).isEqualTo(false)
    }

    @Test
    fun isVisible_autoType_visible() = runBlocking {
        val carrierConfigs = PersistableBundle()
        carrierConfigs.putBoolean(KEY_SATELLITE_ATTACH_SUPPORTED_BOOL, true)
        carrierConfigs.putInt(
            KEY_CARRIER_ROAMING_NTN_CONNECT_TYPE_INT,
            CARRIER_ROAMING_NTN_CONNECT_AUTOMATIC,
        )
        whenever(mockCarrierConfigCache.getSpecificConfigsForSubId(eq(TEST_SUB_ID), any()))
            .thenReturn(carrierConfigs)

        controller.initialize(TEST_SUB_ID)
        preferenceScreen.addPreference(preference)
        controller.displayPreference(preferenceScreen)

        controller.onViewCreated(TestLifecycleOwner())
        delay(100)

        assertThat(preference.isVisible).isEqualTo(true)
    }

    companion object {
        private const val KEY = "telephony_satellite_setting_key"
        private const val TEST_SUB_ID = 5
    }
}
