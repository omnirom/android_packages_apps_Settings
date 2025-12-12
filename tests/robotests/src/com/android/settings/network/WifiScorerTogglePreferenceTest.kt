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

package com.android.settings.network

import android.content.Context
import android.content.ContextWrapper
import android.content.res.Resources
import android.net.wifi.WifiManager
import android.provider.Settings.Secure.ADAPTIVE_CONNECTIVITY_WIFI_ENABLED
import android.telephony.SubscriptionInfo
import android.telephony.SubscriptionManager
import androidx.preference.SwitchPreferenceCompat
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.R
import com.android.settingslib.preference.createAndBindWidget
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.shadows.ShadowSubscriptionManager

@RunWith(AndroidJUnit4::class)
class WifiScorerTogglePreferenceTest {
    private val mockWifiManager = mock<WifiManager>()
    private val mockSubscriptionManager = mock<SubscriptionManager>()
    private val mockResources: Resources = mock()

    private val context: Context =
        object : ContextWrapper(ApplicationProvider.getApplicationContext()) {
            override fun getSystemService(name: String): Any? =
                when (name) {
                    getSystemServiceName(WifiManager::class.java) -> mockWifiManager
                    getSystemServiceName(SubscriptionManager::class.java) -> mockSubscriptionManager
                    else -> super.getSystemService(name)
                }

            override fun getResources(): Resources = mockResources
        }

    private val wifiScorerTogglePreference = WifiScorerTogglePreference()

    @Test
    fun switchClick_defaultDisabled_returnFalse() {
        setWifiScorerEnabled(false)

        assertThat(getSwitchPreference().isChecked).isFalse()
    }

    @Test
    fun switchClick_defaultEnabled_returnTrue() {
        setWifiScorerEnabled(true)

        assertThat(getSwitchPreference().isChecked).isTrue()
    }

    @Test
    fun setChecked_defaultEnabled_updatesCorrectly() {
        val preference = getSwitchPreference()
        assertThat(preference.isChecked).isTrue()

        preference.performClick()

        assertThat(preference.isChecked).isFalse()

        preference.performClick()

        assertThat(preference.isChecked).isTrue()
    }

    @Test
    fun storeSetTrue_wifiManagerSetWifiScoringEnabled() {
        setWifiScorerEnabled(true)

        assertThat(getWifiScorerEnabled()).isTrue()
        verify(mockWifiManager).setWifiScoringEnabled(true)
    }

    @Test
    fun storeSetFalse_wifiManagerSetWifiScoringDisabled() {
        setWifiScorerEnabled(false)

        assertThat(getWifiScorerEnabled()).isFalse()
        verify(mockWifiManager).setWifiScoringEnabled(false)
    }

    @Test
    fun defaultValue_carrierHasWifiScorerToggleOff_returnFalse() {
        val subId = 1
        val carrierId = 1234
        whenever(mockResources.getIntArray(R.array.config_carrier_for_wifi_scorer_toggle_off))
            .thenReturn(intArrayOf(carrierId))
        val subscriptionInfo = mock<SubscriptionInfo>()
        whenever(subscriptionInfo.subscriptionId).thenReturn(subId)
        whenever(subscriptionInfo.carrierId).thenReturn(carrierId)
        whenever(mockSubscriptionManager.getActiveSubscriptionInfo(subId)).thenReturn(subscriptionInfo)
        whenever(mockSubscriptionManager.getAllSubscriptionInfoList())
            .thenReturn(listOf(subscriptionInfo))
        whenever(mockSubscriptionManager.getActiveSubscriptionInfoList())
            .thenReturn(listOf(subscriptionInfo))
        ShadowSubscriptionManager.setDefaultDataSubscriptionId(subId)

        val preference = getSwitchPreference()

        assertThat(preference.isChecked).isFalse()

        // Clean up
        ShadowSubscriptionManager.setDefaultDataSubscriptionId(
            SubscriptionManager.INVALID_SUBSCRIPTION_ID
        )
    }

    @Test
    fun defaultValue_carrierDoesNotHaveWifiScorerToggleOff_returnTrue() {
        val subId = 1
        val carrierId = 5678
        whenever(mockResources.getIntArray(R.array.config_carrier_for_wifi_scorer_toggle_off))
            .thenReturn(intArrayOf())
        val subscriptionInfo = mock<SubscriptionInfo>()
        whenever(subscriptionInfo.subscriptionId).thenReturn(subId)
        whenever(subscriptionInfo.carrierId).thenReturn(carrierId)
        whenever(mockSubscriptionManager.getActiveSubscriptionInfo(subId)).thenReturn(subscriptionInfo)
        whenever(mockSubscriptionManager.getAllSubscriptionInfoList())
            .thenReturn(listOf(subscriptionInfo))
        whenever(mockSubscriptionManager.getActiveSubscriptionInfoList())
            .thenReturn(listOf(subscriptionInfo))
        ShadowSubscriptionManager.setDefaultDataSubscriptionId(subId)

        val preference = getSwitchPreference()

        assertThat(preference.isChecked).isTrue()

        // Clean up
        ShadowSubscriptionManager.setDefaultDataSubscriptionId(
            SubscriptionManager.INVALID_SUBSCRIPTION_ID
        )
    }

    @Test
    fun defaultValue_noDefaultDataSubscription_returnTrue() {
        ShadowSubscriptionManager.setDefaultDataSubscriptionId(
            SubscriptionManager.INVALID_SUBSCRIPTION_ID
        )

        val preference = getSwitchPreference()

        assertThat(preference.isChecked).isTrue()
    }

    @Test
    fun defaultValue_noSubscriptionInfo_returnTrue() {
        val subId = 1
        whenever(mockSubscriptionManager.getAllSubscriptionInfoList()).thenReturn(emptyList())
        ShadowSubscriptionManager.setDefaultDataSubscriptionId(subId)

        val preference = getSwitchPreference()

        assertThat(preference.isChecked).isTrue()

        // Clean up
        ShadowSubscriptionManager.setDefaultDataSubscriptionId(
            SubscriptionManager.INVALID_SUBSCRIPTION_ID
        )
    }

    @Test
    fun defaultValue_noSubscriptionManager_returnTrue() {
        val contextWithoutSubManager: Context =
            object : ContextWrapper(ApplicationProvider.getApplicationContext()) {
                override fun getSystemService(name: String): Any? =
                    when (name) {
                        getSystemServiceName(WifiManager::class.java) -> mockWifiManager
                        getSystemServiceName(SubscriptionManager::class.java) -> null
                        else -> super.getSystemService(name)
                    }
            }

        val preference: SwitchPreferenceCompat =
            wifiScorerTogglePreference.createAndBindWidget(contextWithoutSubManager)

        assertThat(preference.isChecked).isTrue()
    }

    @Test
    fun defaultValue_noActiveSubscriptionInfo_returnTrue() {
        val subId = 1
        val carrierId = 1234
        whenever(mockResources.getIntArray(R.array.config_carrier_for_wifi_scorer_toggle_off))
            .thenReturn(intArrayOf(carrierId))
        val subscriptionInfo = mock<SubscriptionInfo>()
        whenever(subscriptionInfo.subscriptionId).thenReturn(subId)
        whenever(subscriptionInfo.carrierId).thenReturn(carrierId)
        whenever(mockSubscriptionManager.getAllSubscriptionInfoList())
            .thenReturn(listOf(subscriptionInfo))
        whenever(mockSubscriptionManager.getActiveSubscriptionInfoList())
            .thenReturn(emptyList())
        ShadowSubscriptionManager.setDefaultDataSubscriptionId(subId)

        val preference = getSwitchPreference()

        assertThat(preference.isChecked).isTrue()

        // Clean up
        ShadowSubscriptionManager.setDefaultDataSubscriptionId(
            SubscriptionManager.INVALID_SUBSCRIPTION_ID
        )
    }

    private fun getSwitchPreference(): SwitchPreferenceCompat =
        wifiScorerTogglePreference.createAndBindWidget(context)

    private fun setWifiScorerEnabled(enabled: Boolean) =
        wifiScorerTogglePreference
            .storage(context)
            .setBoolean(ADAPTIVE_CONNECTIVITY_WIFI_ENABLED, enabled)

    private fun getWifiScorerEnabled() =
        wifiScorerTogglePreference.storage(context).getBoolean(ADAPTIVE_CONNECTIVITY_WIFI_ENABLED)
}
