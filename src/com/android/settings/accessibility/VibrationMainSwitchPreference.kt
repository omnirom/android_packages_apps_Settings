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
package com.android.settings.accessibility

import android.app.settings.SettingsEnums.ACTION_VIBRATION_HAPTICS
import android.content.Context
import android.os.VibrationAttributes
import android.os.VibrationAttributes.Usage
import android.os.Vibrator
import android.provider.Settings.System.VIBRATE_ON
import androidx.preference.Preference
import androidx.preference.TwoStatePreference
import com.android.settings.R
import com.android.settings.accessibility.VibrationMainSwitchPreference.Companion.DEFAULT_VALUE
import com.android.settings.contract.KEY_VIBRATION_HAPTICS
import com.android.settings.metrics.PreferenceActionMetricsProvider
import com.android.settingslib.datastore.KeyValueStore
import com.android.settingslib.datastore.SettingsSystemStore
import com.android.settingslib.metadata.BooleanValuePreference
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.metadata.ReadWritePermit
import com.android.settingslib.metadata.SensitivityLevel
import com.android.settingslib.widget.MainSwitchPreferenceBinding

/** Accessibility settings for vibration. */
// LINT.IfChange
class VibrationMainSwitchPreference(override val key: String) :
    BooleanValuePreference,
    MainSwitchPreferenceBinding,
    PreferenceActionMetricsProvider,
    Preference.OnPreferenceChangeListener {

    override val title
        get() = R.string.accessibility_vibration_primary_switch_title

    override val keywords: Int
        get() = R.string.keywords_accessibility_vibration_primary_switch

    override val preferenceActionMetrics: Int
        get() = ACTION_VIBRATION_HAPTICS

    override fun tags(context: Context) = arrayOf(KEY_VIBRATION_HAPTICS)

    override fun storage(context: Context): KeyValueStore = VibrationMainSwitchStore(context, key)

    override fun getReadPermissions(context: Context) = SettingsSystemStore.getReadPermissions()

    override fun getReadPermit(context: Context, callingPid: Int, callingUid: Int) =
        ReadWritePermit.ALLOW

    override fun getWritePermissions(context: Context) = SettingsSystemStore.getWritePermissions()

    override fun getWritePermit(context: Context, callingPid: Int, callingUid: Int) =
        ReadWritePermit.ALLOW

    override val sensitivityLevel: Int
        get() = SensitivityLevel.NO_SENSITIVITY

    override fun bind(preference: Preference, metadata: PreferenceMetadata) {
        super.bind(preference, metadata)
        preference.onPreferenceChangeListener = this
    }

    override fun onPreferenceChange(preference: Preference, newValue: Any?): Boolean {
        val isChecked = newValue as Boolean
        // must make new value effective before preview
        (preference as TwoStatePreference).setChecked(isChecked)
        if (isChecked) {
            // Play a haptic as preview for the main toggle only when touch feedback is enabled.
            preference.context.playVibrationSettingsPreview(VibrationAttributes.USAGE_TOUCH)
        }
        return false // value has been updated
    }

    companion object {
        const val DEFAULT_VALUE = true
    }
}

/** Provides SettingsStore for vibration main switch with custom default value. */
class VibrationMainSwitchStore(context: Context, key: String = VIBRATE_ON) :
    VibrationToggleSettingsStore(
        context,
        preferenceKey = key,
        settingsProviderKey = VIBRATE_ON,
        defaultValue = DEFAULT_VALUE,
    )

/** Play vibration preview for given usage. */
fun Context.playVibrationSettingsPreview(@Usage vibrationUsage: Int) {
    getSystemService(Vibrator::class.java)?.let {
        VibrationPreferenceConfig.playVibrationPreview(it, vibrationUsage)
    }
}

// LINT.ThenChange(VibrationMainSwitchPreferenceController.java)
