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

import android.content.Context
import android.content.res.Resources
import android.net.ConnectivitySettingsManager.NETWORK_AVOID_BAD_WIFI_AVOID
import android.net.ConnectivitySettingsManager.NETWORK_AVOID_BAD_WIFI_IGNORE
import android.net.ConnectivitySettingsManager.NETWORK_AVOID_BAD_WIFI_PROMPT
import android.net.platform.flags.Flags
import android.platform.test.annotations.EnableFlags
import android.platform.test.annotations.DisableFlags
import android.platform.test.flag.junit.SetFlagsRule
import android.telephony.SubscriptionManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.Rule
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub
import org.mockito.kotlin.whenever


@RunWith(AndroidJUnit4::class)
class CellularFallbackPreferenceControllerTest {
    @get:Rule val setFlagsRule: SetFlagsRule = SetFlagsRule()

    private val context: Context = ApplicationProvider.getApplicationContext()
    val resources: Resources = mock<Resources>()
    private var defaultDataSubId = SUB_1_ID
    private var showAvoidBadWifiToggle = false
    private var networkAvoidBadWifi = false
    private var valueOfSetNetworkAvoidBadWifi = -1
    private val controller: CellularFallbackPreferenceController =
        CellularFallbackPreferenceController(
            context = context,
            key = KEY_CELLULAR_FALLBACK,
            getActiveDataSubscriptionId = { defaultDataSubId },
            getResourcesForSubId = { resources },
            shouldShowAvoidBadWifiToggle = { showAvoidBadWifiToggle },
            getNetworkAvoidBadWifi = { networkAvoidBadWifi },
            setNetworkAvoidBadWifi = { valueOfSetNetworkAvoidBadWifi = it },
        )

    @Test
    @EnableFlags(Flags.FLAG_AVOID_BAD_WIFI_FROM_CARRIER_CONFIG)
    fun isAvailable_shouldShowAvoidBadWifiToggleReturnTrue_shouldReturnTrue() {
        showAvoidBadWifiToggle = true

        assertThat(controller.isAvailable()).isTrue()
    }

    @Test
    @EnableFlags(Flags.FLAG_AVOID_BAD_WIFI_FROM_CARRIER_CONFIG)
    fun isAvailable_shouldShowAvoidBadWifiToggleReturnFalse_shouldReturnFalse() {
        showAvoidBadWifiToggle = false

        assertThat(controller.isAvailable()).isFalse()
    }

    @Test
    @EnableFlags(Flags.FLAG_AVOID_BAD_WIFI_FROM_CARRIER_CONFIG)
    fun isChecked_getNetworkAvoidBadWifiReturnTrue_shouldReturnTrue() {
        networkAvoidBadWifi = true

        assertThat(controller.isChecked()).isTrue()
    }

    @Test
    @EnableFlags(Flags.FLAG_AVOID_BAD_WIFI_FROM_CARRIER_CONFIG)
    fun isChecked_getNetworkAvoidBadWifiReturnFalse_shouldReturnFalse() {
        networkAvoidBadWifi = false

        assertThat(controller.isChecked()).isFalse()
    }

    @Test
    @EnableFlags(Flags.FLAG_AVOID_BAD_WIFI_FROM_CARRIER_CONFIG)
    fun setCheck_setTrue_valueIsAvoid() {
        controller.setChecked(true)

        assertThat(valueOfSetNetworkAvoidBadWifi).isEqualTo(NETWORK_AVOID_BAD_WIFI_AVOID)
    }

    @Test
    @EnableFlags(Flags.FLAG_AVOID_BAD_WIFI_FROM_CARRIER_CONFIG)
    fun setCheck_setFalse_valueIsAvoid() {
        controller.setChecked(false)

        assertThat(valueOfSetNetworkAvoidBadWifi).isEqualTo(NETWORK_AVOID_BAD_WIFI_IGNORE)
    }

    @Test
    @DisableFlags(Flags.FLAG_AVOID_BAD_WIFI_FROM_CARRIER_CONFIG)
    fun setCheck_noFlag_setFalse_valueIsAvoid() {
        controller.setChecked(false)

        assertThat(valueOfSetNetworkAvoidBadWifi).isEqualTo(NETWORK_AVOID_BAD_WIFI_PROMPT)
    }

    @Test
    @DisableFlags(Flags.FLAG_AVOID_BAD_WIFI_FROM_CARRIER_CONFIG)
    fun isAvailable_invalidActiveSubscriptionId_shouldReturnFalse() {
        defaultDataSubId = SubscriptionManager.INVALID_SUBSCRIPTION_ID

        assertThat(controller.isAvailable()).isFalse()
    }

    @Test
    @DisableFlags(Flags.FLAG_AVOID_BAD_WIFI_FROM_CARRIER_CONFIG)
    fun isAvailable_avoidBadWifiConfigIsFalse_shouldReturnTrue() {
        defaultDataSubId = SubscriptionManager.DEFAULT_SUBSCRIPTION_ID
        resources.stub {
            on { getInteger(com.android.internal.R.integer.config_networkAvoidBadWifi) } doReturn 0
        }

        assertThat(controller.isAvailable()).isTrue()
    }

    @Test
    @DisableFlags(Flags.FLAG_AVOID_BAD_WIFI_FROM_CARRIER_CONFIG)
    fun isAvailable_avoidBadWifiConfigIsTrue_shouldReturnFalse() {
        defaultDataSubId = SubscriptionManager.DEFAULT_SUBSCRIPTION_ID
        resources.stub {
            on { getInteger(com.android.internal.R.integer.config_networkAvoidBadWifi) } doReturn 1
        }

        assertThat(controller.isAvailable()).isFalse()
    }

    private companion object {
        const val KEY_CELLULAR_FALLBACK: String = "wifi_cellular_data_fallback"
        const val SUB_1_ID = 1
    }
}