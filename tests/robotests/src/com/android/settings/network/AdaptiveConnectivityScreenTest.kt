/*
 * Copyright (C) 2024 The Android Open Source Project
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
import android.platform.test.annotations.EnableFlags
import android.provider.Settings
import android.provider.Settings.Secure.ADAPTIVE_CONNECTIVITY_ENABLED
import android.provider.Settings.Secure.ADAPTIVE_CONNECTIVITY_MOBILE_NETWORK_ENABLED
import android.provider.Settings.Secure.ADAPTIVE_CONNECTIVITY_WIFI_ENABLED
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.preference.SwitchPreferenceCompat
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.flags.Flags
import com.android.settingslib.metadata.PreferenceHierarchy
import com.android.settingslib.preference.CatalystScreenTestCase
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@Suppress("DEPRECATION")
@RunWith(AndroidJUnit4::class)
class AdaptiveConnectivityScreenTest() : CatalystScreenTestCase() {
    override val preferenceScreenCreator = AdaptiveConnectivityScreen()
    override val flagName
        get() = Flags.FLAG_CATALYST_ADAPTIVE_CONNECTIVITY
    private lateinit var fragment: AdaptiveConnectivitySettings
    private val mContext: Context = ApplicationProvider.getApplicationContext()
    override fun migration() {}

    @Test
    fun key() {
        assertThat(preferenceScreenCreator.key).isEqualTo(AdaptiveConnectivityScreen.KEY)
    }

    @Test
    fun getPreferenceHierarchy_returnsHierarchy() {
        val hierarchy: PreferenceHierarchy =
            preferenceScreenCreator.getPreferenceHierarchy(mContext)
        (appContext)
        assertThat(hierarchy.find(ADAPTIVE_CONNECTIVITY_ENABLED)).isNotNull()
        assertThat(hierarchy.find(ADAPTIVE_CONNECTIVITY_WIFI_ENABLED)).isNull()
        assertThat(hierarchy.find(ADAPTIVE_CONNECTIVITY_MOBILE_NETWORK_ENABLED)).isNull()
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_NESTED_TOGGLE_SWITCHES)
    fun getPreferenceHierarchy_flagEnabled_returnsHierarchyWithNestedToggle() {
        val hierarchy: PreferenceHierarchy =
            preferenceScreenCreator.getPreferenceHierarchy(mContext)
        (appContext)
        assertThat(hierarchy.find(ADAPTIVE_CONNECTIVITY_ENABLED)).isNotNull()
        assertThat(hierarchy.find(ADAPTIVE_CONNECTIVITY_WIFI_ENABLED)).isNotNull()
        assertThat(hierarchy.find(ADAPTIVE_CONNECTIVITY_MOBILE_NETWORK_ENABLED)).isNotNull()

    }

    @Test
    fun flagDefaultDisabled_noSwitchPreferenceCompatExists() {
        val scenario = launchFragmentInContainer<AdaptiveConnectivitySettings>()
        scenario.onFragment { fragment ->
            this.fragment = fragment
            assertSwitchPreferenceCompatVisibility(
                ADAPTIVE_CONNECTIVITY_WIFI_ENABLED, fragment,
                false
            )
            assertSwitchPreferenceCompatVisibility(
                ADAPTIVE_CONNECTIVITY_MOBILE_NETWORK_ENABLED,
                fragment,
                false
            )
        }
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_NESTED_TOGGLE_SWITCHES)
    fun flagEnabled_switchPreferenceCompatExists() {
        val scenario = launchFragmentInContainer<AdaptiveConnectivitySettings>()
        scenario.onFragment { fragment ->
            this.fragment = fragment
            assertSwitchPreferenceCompatVisibility(
                ADAPTIVE_CONNECTIVITY_WIFI_ENABLED, fragment,
                true
            )
            assertSwitchPreferenceCompatVisibility(
                ADAPTIVE_CONNECTIVITY_MOBILE_NETWORK_ENABLED,
                fragment,
                true
            )
        }
    }


    @Test
    @EnableFlags(Flags.FLAG_ENABLE_NESTED_TOGGLE_SWITCHES)
    fun flagEnabled_onWifiScorerSwitchClick_shouldUpdateSetting() {
        val scenario = launchFragmentInContainer<AdaptiveConnectivitySettings>()
        scenario.onFragment { fragment: AdaptiveConnectivitySettings ->
            this.fragment = fragment
            val switchPreference =
                fragment.findPreference<SwitchPreferenceCompat>(ADAPTIVE_CONNECTIVITY_WIFI_ENABLED)
            assertThat(switchPreference?.isChecked).isTrue()
            switchPreference?.performClick()
            assertThat(switchPreference?.isChecked).isFalse()
            assertThat(updateSetting(ADAPTIVE_CONNECTIVITY_WIFI_ENABLED)).isFalse()
            switchPreference?.performClick()
            assertThat(switchPreference?.isChecked).isTrue()
            assertThat(updateSetting(ADAPTIVE_CONNECTIVITY_WIFI_ENABLED)).isTrue()
        }
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_NESTED_TOGGLE_SWITCHES)
    fun flagEnabled_onAdaptiveMobileNetworkSwitchClick_shouldUpdateSetting() {
        val scenario = launchFragmentInContainer<AdaptiveConnectivitySettings>()
        scenario.onFragment { fragment: AdaptiveConnectivitySettings ->
            this.fragment = fragment
            val switchPreference =
                fragment.findPreference<SwitchPreferenceCompat>(
                    ADAPTIVE_CONNECTIVITY_MOBILE_NETWORK_ENABLED
                )
            assertThat(switchPreference?.isChecked).isTrue()
            switchPreference?.performClick()
            assertThat(switchPreference?.isChecked).isFalse()
            assertThat(updateSetting(ADAPTIVE_CONNECTIVITY_MOBILE_NETWORK_ENABLED)).isFalse()
            switchPreference?.performClick()
            assertThat(switchPreference?.isChecked).isTrue()
            assertThat(updateSetting(ADAPTIVE_CONNECTIVITY_MOBILE_NETWORK_ENABLED)).isTrue()
        }
    }

    /**
     * Helper function to get the setting value from Settings.Secure.
     *
     * @param key the key of the setting to get.
     */
    private fun updateSetting(key: String): Boolean {
        return (Settings.Secure.getInt(
            mContext.contentResolver,
            key,
            0
        ) == 1)
    }

    private fun assertSwitchPreferenceCompatVisibility(
        key: String,
        fragment: AdaptiveConnectivitySettings,
        isVisible: Boolean
    ) {
        val switchPreference = fragment.findPreference<SwitchPreferenceCompat>(key)
        assertThat(switchPreference?.isVisible).isEqualTo(isVisible)
    }

}
