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

package com.android.settings.accessibility.colorcorrection.ui

import android.content.Context
import android.provider.Settings
import androidx.test.core.app.ApplicationProvider
import com.android.settings.R
import com.android.settings.testutils.SettingsStoreRule
import com.android.settingslib.datastore.SettingsSecureStore
import com.android.settingslib.metadata.ReadWritePermit
import com.android.settingslib.preference.createAndBindWidget
import com.android.settingslib.widget.MainSwitchPreference
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ColorCorrectionMainSwitchPreferenceTest {
    @get:Rule val settingStoreRule = SettingsStoreRule()

    private lateinit var appContext: Context
    private lateinit var preference: ColorCorrectionMainSwitchPreference

    @Before
    fun setUp() {
        appContext = ApplicationProvider.getApplicationContext()
        preference = ColorCorrectionMainSwitchPreference(appContext)
    }

    @Test
    fun bindWidget_featureOn_toggleIsChecked() {
        setColorCorrectionStateInSetting(enable = true)
        val widget = preference.createAndBindWidget<MainSwitchPreference>(appContext)

        assertThat(widget.isChecked).isTrue()
    }

    @Test
    fun bindWidget_featureOff_toggleIsNotChecked() {
        setColorCorrectionStateInSetting(enable = false)
        val widget = preference.createAndBindWidget<MainSwitchPreference>(appContext)

        assertThat(widget.isChecked).isFalse()
    }

    @Test
    fun turnOnFeature_settingSetToTrue() {
        setColorCorrectionStateInSetting(enable = false)
        val widget = preference.createAndBindWidget<MainSwitchPreference>(appContext)

        widget.performClick()

        assertThat(getColorCorrectionStateInSetting()).isTrue()
    }

    @Test
    fun turnOffFeature_settingSetToFalse() {
        setColorCorrectionStateInSetting(enable = true)
        val widget = preference.createAndBindWidget<MainSwitchPreference>(appContext)

        widget.performClick()

        assertThat(getColorCorrectionStateInSetting()).isFalse()
    }

    @Test
    fun getReadPermissions_returnsSettingsSecureStoreReadPermissions() {
        assertThat(preference.getReadPermissions(appContext))
            .isEqualTo(SettingsSecureStore.getReadPermissions())
    }

    @Test
    fun getWritePermissions_returnsSettingsSecureStoreWritePermissions() {
        assertThat(preference.getWritePermissions(appContext))
            .isEqualTo(SettingsSecureStore.getWritePermissions())
    }

    @Test
    fun getReadPermit_returnsAllow() {
        assertThat(preference.getReadPermit(appContext, 0, 0)).isEqualTo(ReadWritePermit.ALLOW)
    }

    @Test
    fun getWritePermit_returnsAllow() {
        assertThat(preference.getWritePermit(appContext, 0, 0)).isEqualTo(ReadWritePermit.ALLOW)
    }

    @Test
    fun getKey_matchesSettingKey() {
        assertThat(preference.key)
            .isEqualTo(Settings.Secure.ACCESSIBILITY_DISPLAY_DALTONIZER_ENABLED)
    }

    @Test
    fun getTitle_returnCorrectTitle() {
        assertThat(preference.title)
            .isEqualTo(R.string.accessibility_daltonizer_primary_switch_title)
    }

    private fun setColorCorrectionStateInSetting(enable: Boolean) {
        Settings.Secure.putInt(
            appContext.contentResolver,
            Settings.Secure.ACCESSIBILITY_DISPLAY_DALTONIZER_ENABLED,
            if (enable) 1 else 0,
        )
    }

    private fun getColorCorrectionStateInSetting(): Boolean =
        Settings.Secure.getInt(
            appContext.contentResolver,
            Settings.Secure.ACCESSIBILITY_DISPLAY_DALTONIZER_ENABLED,
            0,
        ) == 1
}
