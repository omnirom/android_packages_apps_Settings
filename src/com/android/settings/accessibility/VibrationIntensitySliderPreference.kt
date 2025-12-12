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
import android.os.VibrationAttributes.Usage
import android.os.Vibrator
import android.provider.Settings.System.VIBRATE_ON
import androidx.annotation.CallSuper
import androidx.annotation.StringRes
import androidx.preference.Preference
import androidx.preference.Preference.OnPreferenceChangeListener
import com.android.settingslib.datastore.KeyValueStore
import com.android.settingslib.metadata.IntRangeValuePreference
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.metadata.PreferenceSummaryProvider
import com.android.settingslib.widget.SliderPreference
import com.android.settingslib.widget.SliderPreferenceBinding
import kotlin.math.min

/**
 * SliderPreference for vibration intensity.
 *
 * This implementation uses VibrationIntensitySettingsStore to save the vibration intensity value,
 * also playing a haptic preview on slider changes.
 *
 * This preference observes the state of the VibrationMainSwitchPreference in this fragment,
 * disabling and displaying intensity OFF this slider when the main switch is unchecked. This "off"
 * state should not be persisted, as the original user settings value must be preserved and restored
 * once the main switch is turned back on. This behavior reflects the actual system behavior that
 * restricts all vibrations when the main switch is off.
 */
// LINT.IfChange
open class VibrationIntensitySliderPreference(
    context: Context,
    override val key: String,
    @Usage val vibrationUsage: Int,
    @StringRes override val title: Int = 0,
    @StringRes override val summary: Int = 0,
) :
    IntRangeValuePreference,
    PreferenceSummaryProvider,
    SliderPreferenceBinding,
    OnPreferenceChangeListener {

    private val storage by lazy {
        VibrationIntensitySettingsStore(
            context,
            preferenceKey = key,
            settingsProviderKey = key,
            vibrationUsage,
        )
    }

    override fun getMinValue(context: Context) = Vibrator.VIBRATION_INTENSITY_OFF

    override fun getMaxValue(context: Context) =
        min(Vibrator.VIBRATION_INTENSITY_HIGH, context.getSupportedVibrationIntensityLevels())

    override fun getIncrementStep(context: Context) = 1

    override fun storage(context: Context): KeyValueStore = storage

    override fun dependencies(context: Context) = arrayOf(VIBRATE_ON)

    override fun getSummary(context: Context) = storage.getSummary()

    override fun createWidget(context: Context) = SliderPreference(context)

    @CallSuper
    override fun bind(preference: Preference, metadata: PreferenceMetadata) {
        super.bind(preference, metadata)
        preference.onPreferenceChangeListener = this
        (preference as SliderPreference).apply {
            setTickVisible(true) // Show ticks on slider
            // Haptics previews played by the Settings app don't bypass user settings to be played.
            // The sliders continuously updates the intensity value so the previews can apply them.
            updatesContinuously = true
        }
    }

    override fun onPreferenceChange(preference: Preference, newValue: Any?): Boolean {
        val intensity = newValue as Int
        // must make new value effective before preview
        (preference as SliderPreference).value = intensity
        if (intensity != Vibrator.VIBRATION_INTENSITY_OFF) {
            preference.context.playVibrationSettingsPreview(vibrationUsage)
        }
        return false // value has been updated
    }

    @CallSuper override fun isEnabled(context: Context) = storage.isPreferenceEnabled()
}
// LINT.ThenChange(VibrationIntensityPreferenceController.java)
