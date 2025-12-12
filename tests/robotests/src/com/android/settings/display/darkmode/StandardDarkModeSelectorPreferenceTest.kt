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

package com.android.settings.display.darkmode

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.android.settings.R
import com.android.settings.testutils.SettingsStoreRule
import com.android.settingslib.datastore.KeyValueStore
import com.android.settingslib.metadata.ReadWritePermit
import com.android.settingslib.metadata.SensitivityLevel
import com.android.settingslib.preference.createAndBindWidget
import com.android.settingslib.widget.SelectorWithWidgetPreference
import com.google.common.truth.Truth.assertThat
import com.google.testing.junit.testparameterinjector.TestParameters
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestParameterInjector

@RunWith(RobolectricTestParameterInjector::class)
class StandardDarkModeSelectorPreferenceTest {
    @get:Rule val settingsStoreRule = SettingsStoreRule()
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val preference = StandardDarkModeSelectorPreference(DarkThemeModeStorage(context))

    @Test
    fun key() {
        assertThat(preference.key).isEqualTo("standard_dark_theme")
    }

    @Test
    fun getTitle() {
        assertThat(preference.title).isEqualTo(R.string.accessibility_standard_dark_theme_title)
    }

    @Test
    fun getSummary() {
        assertThat(preference.summary).isEqualTo(R.string.accessibility_standard_dark_theme_summary)
    }

    @Test
    fun getReadPermissions_returnsSettingsSecureStoreReadPermissions() {
        assertThat(preference.getReadPermissions(context))
            .isEqualTo(DarkThemeModeStorage.getReadPermissions())
    }

    @Test
    fun getWritePermissions_returnsSettingsSecureStoreWritePermissions() {
        assertThat(preference.getWritePermissions(context))
            .isEqualTo(DarkThemeModeStorage.getWritePermissions())
    }

    @Test
    fun getReadPermit_returnsAllow() {
        assertThat(preference.getReadPermit(context, 0, 0)).isEqualTo(ReadWritePermit.ALLOW)
    }

    @Test
    fun getWritePermit_returnsAllow() {
        assertThat(preference.getWritePermit(context, 0, 0)).isEqualTo(ReadWritePermit.ALLOW)
    }

    @Test
    fun sensitivityLevel_isNoSensitivity() {
        assertThat(preference.sensitivityLevel).isEqualTo(SensitivityLevel.NO_SENSITIVITY)
    }

    @Test
    fun getIndexableTitle() {
        assertThat(preference.getIndexableTitle(context))
            .isEqualTo(
                context.getString(R.string.accessibility_standard_dark_theme_title_in_search)
            )
    }

    @Test
    @TestParameters(value = ["{settingsEnabled: false}", "{settingsEnabled: true}"])
    fun isChecked(settingsEnabled: Boolean) {
        val darkThemeModeStorage = getStorage() as DarkThemeModeStorage
        darkThemeModeStorage.setBoolean(preference.key, settingsEnabled)
        assertThat(darkThemeModeStorage.settingsStore.getBoolean(DarkThemeModeStorage.KEY))
            .isEqualTo(!settingsEnabled)

        val widget: SelectorWithWidgetPreference = preference.createAndBindWidget(context)
        assertThat(widget.isChecked).isEqualTo(settingsEnabled)
    }

    @Test
    fun performClick() {
        val darkThemeModeStorage = getStorage() as DarkThemeModeStorage
        darkThemeModeStorage.setBoolean(preference.key, false)
        val widget: SelectorWithWidgetPreference = preference.createAndBindWidget(context)
        assertThat(widget.isChecked).isEqualTo(false)
        assertThat(darkThemeModeStorage.settingsStore.getBoolean(DarkThemeModeStorage.KEY))
            .isEqualTo(true)

        widget.performClick()

        assertThat(widget.isChecked).isEqualTo(true)
        assertThat(darkThemeModeStorage.getBoolean(preference.key)).isEqualTo(true)
        assertThat(darkThemeModeStorage.settingsStore.getBoolean(DarkThemeModeStorage.KEY))
            .isEqualTo(false)
    }

    private fun getStorage(): KeyValueStore = preference.storage(context)
}
