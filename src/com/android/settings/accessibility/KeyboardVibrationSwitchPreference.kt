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

import android.app.settings.SettingsEnums
import android.content.Context
import android.os.VibrationAttributes
import android.provider.Settings.System.KEYBOARD_VIBRATION_ENABLED
import android.provider.Settings.System.VIBRATE_ON
import androidx.preference.Preference
import androidx.preference.Preference.OnPreferenceChangeListener
import androidx.preference.TwoStatePreference
import com.android.settings.R
import com.android.settings.metrics.PreferenceActionMetricsProvider
import com.android.settingslib.datastore.KeyValueStore
import com.android.settingslib.metadata.PreferenceAvailabilityProvider
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.metadata.SwitchPreference
import com.android.settingslib.preference.SwitchPreferenceBinding

/** Accessibility settings for keyboard vibration, using a switch toggle. */
// LINT.IfChange
class KeyboardVibrationSwitchPreference(
    context: Context,
    key: String,
    private val mainSwitchPreferenceKey: String = VIBRATE_ON,
) :
    SwitchPreference(key = key, title = R.string.accessibility_keyboard_vibration_title),
    PreferenceActionMetricsProvider,
    PreferenceAvailabilityProvider,
    OnPreferenceChangeListener,
    SwitchPreferenceBinding {

    private val storage by lazy {
        VibrationToggleSettingsStore(
            context,
            preferenceKey = key,
            settingsProviderKey = KEYBOARD_VIBRATION_ENABLED,
            defaultValue = DEFAULT_VALUE,
        )
    }

    override val preferenceActionMetrics: Int
        get() = SettingsEnums.ACTION_KEYBOARD_VIBRATION_CHANGED

    override val keywords: Int
        get() = R.string.keywords_keyboard_vibration

    override fun isAvailable(context: Context) =
        context.resources.getBoolean(
            com.android.internal.R.bool.config_keyboardVibrationSettingsSupported
        )

    override fun isEnabled(context: Context) = storage.isPreferenceEnabled()

    override fun storage(context: Context): KeyValueStore = storage

    override fun dependencies(context: Context) = arrayOf(mainSwitchPreferenceKey)

    override fun bind(preference: Preference, metadata: PreferenceMetadata) {
        super.bind(preference, metadata)
        preference.onPreferenceChangeListener = this
    }

    override fun onPreferenceChange(preference: Preference, newValue: Any?): Boolean {
        val isChecked = newValue as Boolean
        // must make new value effective before preview
        (preference as TwoStatePreference).setChecked(isChecked)
        if (isChecked) {
            // Vibrate when toggle is enabled for consistency with all the other toggle/slides
            // in the same screen. Use IME feedback intensity for this preview.
            preference.context.playVibrationSettingsPreview(VibrationAttributes.USAGE_IME_FEEDBACK)
        }
        return false
    }

    companion object {
        private const val DEFAULT_VALUE = true
    }
}
// LINT.ThenChange(KeyboardVibrationTogglePreferenceController.java)
