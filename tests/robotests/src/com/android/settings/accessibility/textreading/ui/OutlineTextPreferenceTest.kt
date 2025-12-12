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

package com.android.settings.accessibility.textreading.ui

import android.content.Context
import android.provider.Settings
import androidx.preference.PreferenceManager
import androidx.preference.SwitchPreferenceCompat
import androidx.test.core.app.ApplicationProvider
import com.android.settings.R
import com.android.settings.accessibility.TextReadingPreferenceFragment.EntryPoint
import com.android.settings.testutils.SettingsStoreRule
import com.android.settingslib.datastore.SettingsSecureStore
import com.android.settingslib.metadata.ReadWritePermit
import com.android.settingslib.metadata.SensitivityLevel
import com.android.settingslib.preference.createAndBindWidget
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Tests for [OutlineTextPreference]. */
@RunWith(RobolectricTestRunner::class)
class OutlineTextPreferenceTest {
    @get:Rule val settingsStoreRule = SettingsStoreRule()
    private val context = ApplicationProvider.getApplicationContext<Context>()
    @EntryPoint private val entryPoint = EntryPoint.DISPLAY_SETTINGS
    private val outlineTextPreference = OutlineTextPreference(context, entryPoint)
    private val preferenceManager = PreferenceManager(context)
    private val preferenceScreen = preferenceManager.createPreferenceScreen(context)

    @Test
    fun getKey_matchSettingKey() {
        // pref key must match setting key to automatically observe the setting change
        assertThat(outlineTextPreference.key)
            .isEqualTo(Settings.Secure.ACCESSIBILITY_HIGH_TEXT_CONTRAST_ENABLED)
    }

    @Test
    fun getTitleRes() {
        assertThat(outlineTextPreference.title)
            .isEqualTo(R.string.accessibility_toggle_maximize_text_contrast_preference_title)
    }

    @Test
    fun getSummaryRes() {
        assertThat(outlineTextPreference.summary)
            .isEqualTo(R.string.accessibility_toggle_maximize_text_contrast_preference_summary)
    }

    @Test
    fun getKeywordsRes() {
        assertThat(outlineTextPreference.keywords)
            .isEqualTo(R.string.keywords_maximize_text_contrast)
    }

    @Test
    fun clickOutlinePreference_wasOff_turnOnOutlineTextSetting() {
        SettingsSecureStore.get(context)
            .setBoolean(Settings.Secure.ACCESSIBILITY_HIGH_TEXT_CONTRAST_ENABLED, false)
        val preference = inflateAndBindPreference()
        check(!preference.isChecked)

        preference.performClick()

        assertThat(preference.isChecked).isTrue()
        assertThat(
                SettingsSecureStore.get(context)
                    .getBoolean(Settings.Secure.ACCESSIBILITY_HIGH_TEXT_CONTRAST_ENABLED)
            )
            .isEqualTo(true)
    }

    @Test
    fun clickOutlinePreference_wasOn_turnOffOutlineTextSetting() {
        SettingsSecureStore.get(context)
            .setBoolean(Settings.Secure.ACCESSIBILITY_HIGH_TEXT_CONTRAST_ENABLED, true)
        val preference = inflateAndBindPreference()
        check(preference.isChecked)

        preference.performClick()

        assertThat(preference.isChecked).isFalse()
        assertThat(
                SettingsSecureStore.get(context)
                    .getBoolean(Settings.Secure.ACCESSIBILITY_HIGH_TEXT_CONTRAST_ENABLED)
            )
            .isEqualTo(false)
    }

    @Test
    fun getReadPermissions_matchSettingReadPermissions() {
        assertThat(outlineTextPreference.getReadPermissions(context))
            .isEqualTo(SettingsSecureStore.getReadPermissions())
    }

    @Test
    fun getWritePermissions_matchSettingWritePermissions() {
        assertThat(outlineTextPreference.getWritePermissions(context))
            .isEqualTo(SettingsSecureStore.getWritePermissions())
    }

    @Test
    fun getReadPermit_returnsAllow() {
        assertThat(
                outlineTextPreference.getReadPermit(
                    context = context,
                    callingPid = 0,
                    callingUid = 0,
                )
            )
            .isEqualTo(ReadWritePermit.ALLOW)
    }

    @Test
    fun getWritePermit_returnsAllow() {
        assertThat(
                outlineTextPreference.getWritePermit(
                    context = context,
                    callingPid = 0,
                    callingUid = 0,
                )
            )
            .isEqualTo(ReadWritePermit.ALLOW)
    }

    @Test
    fun getSensitivityLevel_returnsNoSensitivity() {
        assertThat(outlineTextPreference.sensitivityLevel)
            .isEqualTo(SensitivityLevel.NO_SENSITIVITY)
    }

    private fun inflateAndBindPreference(): SwitchPreferenceCompat {
        return outlineTextPreference.createAndBindWidget(
            context = context,
            preferenceScreen = preferenceScreen,
        )
    }
}
