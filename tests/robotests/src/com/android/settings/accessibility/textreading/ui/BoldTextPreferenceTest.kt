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
import com.android.settings.accessibility.textreading.data.BoldTextDataStore
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

/** Test for [BoldTextPreference]. */
@RunWith(RobolectricTestRunner::class)
class BoldTextPreferenceTest {
    @get:Rule val settingsStoreRule = SettingsStoreRule()
    private val context = ApplicationProvider.getApplicationContext<Context>()
    @EntryPoint private val entryPoint = EntryPoint.DISPLAY_SETTINGS
    private val boldTextPreference = BoldTextPreference(context, entryPoint)
    private val preferenceManager = PreferenceManager(context)
    private val preferenceScreen = preferenceManager.createPreferenceScreen(context)

    @Test
    fun getKey_matchSettingKey() {
        // pref key must match setting key to automatically observe the setting change
        assertThat(boldTextPreference.key).isEqualTo(Settings.Secure.FONT_WEIGHT_ADJUSTMENT)
    }

    @Test
    fun getTitleRes() {
        assertThat(boldTextPreference.title).isEqualTo(R.string.force_bold_text)
    }

    @Test
    fun getSummaryRes_noSummary() {
        assertThat(boldTextPreference.summary).isEqualTo(0)
    }

    @Test
    fun getKeywordsRes() {
        assertThat(boldTextPreference.keywords).isEqualTo(R.string.keywords_bold_text)
    }

    @Test
    fun clickPreference_wasOff_turnOnBoldTextSetting() {
        SettingsSecureStore.get(context).setInt(Settings.Secure.FONT_WEIGHT_ADJUSTMENT, 0)
        val preference = inflateAndBindPreference()
        check(!preference.isChecked)

        preference.performClick()

        assertThat(preference.isChecked).isTrue()
        assertThat(SettingsSecureStore.get(context).getInt(Settings.Secure.FONT_WEIGHT_ADJUSTMENT))
            .isEqualTo(BoldTextDataStore.BOLD_TEXT_ADJUSTMENT)
    }

    @Test
    fun clickPreference_wasOn_turnOffBoldTextSetting() {
        SettingsSecureStore.get(context)
            .setInt(Settings.Secure.FONT_WEIGHT_ADJUSTMENT, BoldTextDataStore.BOLD_TEXT_ADJUSTMENT)
        val preference = inflateAndBindPreference()
        check(preference.isChecked)

        preference.performClick()

        assertThat(preference.isChecked).isFalse()
        assertThat(SettingsSecureStore.get(context).getInt(Settings.Secure.FONT_WEIGHT_ADJUSTMENT))
            .isEqualTo(0)
    }

    @Test
    fun getReadPermissions_matchSettingReadPermissions() {
        assertThat(boldTextPreference.getReadPermissions(context))
            .isEqualTo(SettingsSecureStore.getReadPermissions())
    }

    @Test
    fun getWritePermissions_matchSettingWritePermissions() {
        assertThat(boldTextPreference.getWritePermissions(context))
            .isEqualTo(SettingsSecureStore.getWritePermissions())
    }

    @Test
    fun getReadPermit_returnsAllow() {
        assertThat(
                boldTextPreference.getReadPermit(context = context, callingPid = 0, callingUid = 0)
            )
            .isEqualTo(ReadWritePermit.ALLOW)
    }

    @Test
    fun getWritePermit_returnsAllow() {
        assertThat(
                boldTextPreference.getWritePermit(context = context, callingPid = 0, callingUid = 0)
            )
            .isEqualTo(ReadWritePermit.ALLOW)
    }

    @Test
    fun getSensitivityLevel_returnsNoSensitivity() {
        assertThat(boldTextPreference.sensitivityLevel).isEqualTo(SensitivityLevel.NO_SENSITIVITY)
    }

    private fun inflateAndBindPreference(): SwitchPreferenceCompat {
        return boldTextPreference.createAndBindWidget(
            context = context,
            preferenceScreen = preferenceScreen,
        )
    }
}
