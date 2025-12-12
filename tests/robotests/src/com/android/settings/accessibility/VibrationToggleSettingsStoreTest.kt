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
package com.android.settings.accessibility

import android.content.Context
import android.os.VibrationAttributes
import android.os.Vibrator
import android.provider.Settings
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settingslib.datastore.SettingsSystemStore
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class VibrationToggleSettingsStoreTest {
    private companion object {
        const val SETTINGS_KEY: String = Settings.System.HAPTIC_FEEDBACK_INTENSITY
        const val DEFAULT_VALUE: Boolean = true
    }

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val settingsStore = SettingsSystemStore.get(context)
    private val store =
        VibrationToggleSettingsStore(
            context = context,
            preferenceKey = "some_key",
            settingsProviderKey = SETTINGS_KEY,
            keyValueStoreDelegate = settingsStore,
            defaultValue = DEFAULT_VALUE,
        )

    @Test
    fun isPreferenceEnabled_returnsVibrateOnOrTrue() {
        settingsStore.setBoolean(Settings.System.VIBRATE_ON, null)
        assertThat(store.isPreferenceEnabled()).isTrue()

        settingsStore.setBoolean(Settings.System.VIBRATE_ON, true)
        assertThat(store.isPreferenceEnabled()).isTrue()

        settingsStore.setBoolean(Settings.System.VIBRATE_ON, false)
        assertThat(store.isPreferenceEnabled()).isFalse()
    }

    @Test
    fun isPreferenceEnabled_returnsDependencySettingOrTrue() {
        val testStore =
            VibrationToggleSettingsStore(
                context = context,
                preferenceKey = "some_key",
                settingsProviderKey = SETTINGS_KEY,
                keyValueStoreDelegate = settingsStore,
                defaultValue = DEFAULT_VALUE,
                dependencyStore =
                    VibrationIntensitySettingsStore(
                        context,
                        preferenceKey = "some_key",
                        settingsProviderKey = Settings.System.ALARM_VIBRATION_INTENSITY,
                        vibrationUsage = VibrationAttributes.USAGE_ALARM,
                        defaultIntensity = Vibrator.VIBRATION_INTENSITY_MEDIUM,
                        supportedIntensityLevels = 3,
                        keyValueStoreDelegate = settingsStore,
                    ),
            )
        settingsStore.setBoolean(Settings.System.ALARM_VIBRATION_INTENSITY, null)
        assertThat(testStore.isPreferenceEnabled()).isTrue()

        settingsStore.setBoolean(Settings.System.ALARM_VIBRATION_INTENSITY, true)
        assertThat(testStore.isPreferenceEnabled()).isTrue()

        settingsStore.setBoolean(Settings.System.ALARM_VIBRATION_INTENSITY, false)
        assertThat(testStore.isPreferenceEnabled()).isFalse()
    }

    @Test
    fun getValue_badKey_returnOriginalKey() {
        settingsStore.setBoolean(SETTINGS_KEY, true)
        settingsStore.setBoolean("bad_key", false)

        assertThat(settingsStore.getBoolean("bad_key")).isFalse()
        assertThat(store.getBoolean("bad_key")).isTrue()
    }

    @Test
    fun getValue_valueNull_returnDefaultIntensity() {
        settingsStore.setBoolean(SETTINGS_KEY, null)

        assertThat(settingsStore.getBoolean(SETTINGS_KEY)).isNull()
        assertThat(store.getBoolean(SETTINGS_KEY)).isEqualTo(DEFAULT_VALUE)
    }

    @Test
    fun getValue_valueTrue_returnTrue() {
        settingsStore.setBoolean(SETTINGS_KEY, true)

        assertThat(settingsStore.getBoolean(SETTINGS_KEY)).isTrue()
        assertThat(store.getBoolean(SETTINGS_KEY)).isTrue()
    }

    @Test
    fun getValue_valueFalse_returnFalse() {
        settingsStore.setBoolean(SETTINGS_KEY, false)

        assertThat(settingsStore.getBoolean(SETTINGS_KEY)).isFalse()
        assertThat(store.getBoolean(SETTINGS_KEY)).isFalse()
    }

    @Test
    fun setValue_updatesCorrectly() {
        settingsStore.setBoolean(SETTINGS_KEY, null)

        assertThat(settingsStore.getBoolean(SETTINGS_KEY)).isNull()
        assertThat(store.getBoolean(SETTINGS_KEY)).isTrue()
        assertThat(store.getBoolean(SETTINGS_KEY)).isEqualTo(DEFAULT_VALUE)

        settingsStore.setBoolean(SETTINGS_KEY, false)

        assertThat(settingsStore.getBoolean(SETTINGS_KEY)).isFalse()
        assertThat(store.getBoolean(SETTINGS_KEY)).isFalse()

        settingsStore.setBoolean(SETTINGS_KEY, true)

        assertThat(settingsStore.getBoolean(SETTINGS_KEY)).isTrue()
        assertThat(store.getBoolean(SETTINGS_KEY)).isTrue()
    }
}
