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

package com.android.settings.development

import android.content.Context
import android.content.res.Resources
import android.provider.Settings
import android.util.Log
import android.view.View
import androidx.preference.Preference
import androidx.preference.PreferenceScreen
import com.android.settings.accessibility.TextCursorBlinkRateSliderPreference
import com.android.settings.core.PreferenceControllerMixin
import com.android.settingslib.development.DeveloperOptionsPreferenceController
import com.google.android.material.slider.Slider

class TextCursorBlinkRatePreferenceController(val context: Context) :
    DeveloperOptionsPreferenceController(context),
    Preference.OnPreferenceChangeListener,
    PreferenceControllerMixin {

    private val TAG = "TextCursorBlinkRatePrefController"
    private val resources: Resources = context.getResources()

    private val defaultDurationMs =
        resources.getInteger(
            com.android.internal.R.integer.def_accessibility_text_cursor_blink_interval_ms
        )

    private val noBlinkDurationMs =
        resources.getInteger(
            com.android.internal.R.integer.no_blink_accessibility_text_cursor_blink_interval_ms
        )

    // The blink intervals are displayed from no-blink to slow to fast on the slider, so the
    // intervals should be reversed from how they are stored (smallest to largest).
    // The resulting array should look like this:
    // [0, 1000, 833, 714, 625, 556, 500, 455, 417, 385, 357, 333]
    private val durationValues =
        resources
            .getIntArray(com.android.internal.R.array.accessibility_text_cursor_blink_intervals)
            .plus(noBlinkDurationMs)
            .reversed()

    private val noBlinkLabel =
        resources.getString(
            com.android.internal.R.string.no_blink_accessibility_text_cursor_blink_label
        )

    // The blink intervals are displayed from no-blink to slow to fast on the slider, so the
    // corresponding labels should be reversed from how they are stored (smallest to largest).
    // The resulting array should look like this:
    // [Don't blink, 50%, 60%, 70%, 80%, 90%, 100% (default), 110%, 120%, 130%, 140%, 150%]
    private val durationLabels =
        resources
            .getStringArray(com.android.internal.R.array.accessibility_text_cursor_blink_labels)
            .plus(noBlinkLabel)
            .reversed()

    override fun getPreferenceKey(): String = "accessibility_text_cursor_blink_interval_ms"

    override fun displayPreference(screen: PreferenceScreen) {
        super.displayPreference(screen)
        val preference: TextCursorBlinkRateSliderPreference? =
            screen.findPreference(getPreferenceKey())
        preference?.apply {
            setResetClickListener(
                object : View.OnClickListener {
                    override fun onClick(view: View?) {
                        if (view == null) return
                        Settings.Secure.putInt(
                            mContext.getContentResolver(),
                            Settings.Secure.ACCESSIBILITY_TEXT_CURSOR_BLINK_INTERVAL_MS,
                            defaultDurationMs,
                        )
                        updateState(preference)
                    }
                }
            )
            setExtraChangeListener(
                object : Slider.OnChangeListener {
                    override fun onValueChange(slider: Slider, value: Float, fromUser: Boolean) {
                        val intVal: Int = value.toInt()
                        onPreferenceChange(preference, intVal)
                    }
                }
            )
            min = 0
            max = durationValues.size - 1
        }
    }

    override fun onPreferenceChange(pref: Preference, newValue: Any): Boolean {
        if (newValue !is Int) {
            Log.e(TAG, "Given non integer newValue: $newValue")
            return false
        }
        Settings.Secure.putInt(
            mContext.getContentResolver(),
            Settings.Secure.ACCESSIBILITY_TEXT_CURSOR_BLINK_INTERVAL_MS,
            convertIndexToDuration(newValue),
        )
        setSliderStateDescription(pref, newValue)
        return true
    }

    override fun updateState(preference: Preference) {
        super.updateState(preference)
        if (preference !is TextCursorBlinkRateSliderPreference) {
            // This should never happen.
            Log.e(TAG, "Given non TextCursorBlinkRateSliderPreference: " + preference)
            return
        }

        val durationMs =
            Settings.Secure.getInt(
                mContext.getContentResolver(),
                Settings.Secure.ACCESSIBILITY_TEXT_CURSOR_BLINK_INTERVAL_MS,
                defaultDurationMs,
            )
        val index = convertDurationToIndex(durationMs)

        preference.setValue(index)
        setSliderStateDescription(preference, index)
    }

    public override fun onDeveloperOptionsSwitchDisabled() {
        super.onDeveloperOptionsSwitchDisabled()
        Settings.Secure.putInt(
            mContext.getContentResolver(),
            Settings.Secure.ACCESSIBILITY_TEXT_CURSOR_BLINK_INTERVAL_MS,
            defaultDurationMs,
        )
    }

    override fun isAvailable(): Boolean {
        return android.view.accessibility.Flags.textCursorBlinkInterval()
    }

    private fun setSliderStateDescription(preference: Preference, index: Int) {
        if (preference !is TextCursorBlinkRateSliderPreference) {
            // This should never happen.
            Log.e(TAG, "Given non TextCursorBlinkRateSliderPreference: " + preference)
            return
        }
        preference.setSliderStateDescription(durationLabels[index])
    }

    private fun convertDurationToIndex(duration: Int): Int {
        val index = durationValues.indexOf(duration)
        return if (index != -1) {
            index
        } else {
            durationValues.indexOf(defaultDurationMs)
        }
    }

    private fun convertIndexToDuration(index: Int): Int {
        return if (index in durationValues.indices) {
            durationValues[index]
        } else {
            defaultDurationMs
        }
    }
}
