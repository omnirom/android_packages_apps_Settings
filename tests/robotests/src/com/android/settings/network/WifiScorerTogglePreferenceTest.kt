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
import android.net.wifi.WifiManager
import android.provider.Settings.Secure.ADAPTIVE_CONNECTIVITY_WIFI_ENABLED
import androidx.preference.SwitchPreferenceCompat
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settingslib.preference.createAndBindWidget
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

@RunWith(AndroidJUnit4::class)
class WifiScorerTogglePreferenceTest {
    private val mockWifiManager = mock<WifiManager>()

    private val context: Context =
        object : ContextWrapper(ApplicationProvider.getApplicationContext()) {
            override fun getSystemService(name: String): Any? =
                when {
                    name == getSystemServiceName(WifiManager::class.java) -> mockWifiManager
                    else -> super.getSystemService(name)
                }
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

    private fun getSwitchPreference(): SwitchPreferenceCompat =
        wifiScorerTogglePreference.createAndBindWidget(context)

    private fun setWifiScorerEnabled(enabled: Boolean) =
        wifiScorerTogglePreference
            .storage(context)
            .setBoolean(ADAPTIVE_CONNECTIVITY_WIFI_ENABLED, enabled)

    private fun getWifiScorerEnabled() =
        wifiScorerTogglePreference
            .storage(context)
            .getBoolean(ADAPTIVE_CONNECTIVITY_WIFI_ENABLED)
}
