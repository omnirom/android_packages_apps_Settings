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
import android.provider.Settings.Secure.ADAPTIVE_CONNECTIVITY_MOBILE_NETWORK_ENABLED
import androidx.preference.SwitchPreferenceCompat
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settingslib.preference.createAndBindWidget
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AdaptiveMobileNetworkTogglePreferenceTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    private val adaptiveMobileNetworkTogglePreference = AdaptiveMobileNetworkTogglePreference()

    @Test
    fun switchClick_defaultDisabled_returnFalse() {
        setAdaptiveMobileNetworkEnabled(false)

        assertThat(getSwitchPreference().isChecked).isFalse()
    }

    @Test
    fun switchClick_defaultEnabled_returnTrue() {
        setAdaptiveMobileNetworkEnabled(true)

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
    fun storeSetTrue_setAdaptiveMobileNetworkEnabled() {
        setAdaptiveMobileNetworkEnabled(true)

        assertThat(
            getAdaptiveMobileNetworkEnabled()
        ).isTrue()
    }

    @Test
    fun storeSetFalse_setAdaptiveMobileNetworkDisabled() {
        setAdaptiveMobileNetworkEnabled(false)

        assertThat(
            getAdaptiveMobileNetworkEnabled()
        ).isFalse()
    }

    private fun getSwitchPreference(): SwitchPreferenceCompat =
        adaptiveMobileNetworkTogglePreference.createAndBindWidget(context)

    private fun setAdaptiveMobileNetworkEnabled(enabled: Boolean) =
        adaptiveMobileNetworkTogglePreference
            .storage(context)
            .setBoolean(ADAPTIVE_CONNECTIVITY_MOBILE_NETWORK_ENABLED, enabled)

    private fun getAdaptiveMobileNetworkEnabled() =
        adaptiveMobileNetworkTogglePreference
            .storage(context)
            .getBoolean(ADAPTIVE_CONNECTIVITY_MOBILE_NETWORK_ENABLED)
}
