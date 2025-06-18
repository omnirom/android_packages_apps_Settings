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
import android.os.Vibrator
import android.provider.Settings
import androidx.preference.Preference
import com.android.settings.R
import com.android.settings.contract.KEY_VIBRATION_HAPTICS
import com.android.settings.metrics.PreferenceActionMetricsProvider
import com.android.settingslib.datastore.KeyValueStore
import com.android.settingslib.datastore.KeyValueStoreDelegate
import com.android.settingslib.datastore.SettingsSystemStore
import com.android.settingslib.metadata.BooleanValuePreference
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.metadata.ReadWritePermit
import com.android.settingslib.metadata.SensitivityLevel
import com.android.settingslib.preference.MainSwitchPreferenceBinding

/** Accessibility settings for vibration. */
// LINT.IfChange
class VibrationMainSwitchPreference :
    BooleanValuePreference,
    MainSwitchPreferenceBinding,
    PreferenceActionMetricsProvider,
    Preference.OnPreferenceChangeListener {

    override val key
        get() = KEY

    override val title
        get() = R.string.accessibility_vibration_primary_switch_title

    override val keywords: Int
        get() = R.string.keywords_accessibility_vibration_primary_switch

    override val preferenceActionMetrics: Int
        get() = ACTION_VIBRATION_HAPTICS

    override fun tags(context: Context) = arrayOf(KEY_VIBRATION_HAPTICS)

    override fun storage(context: Context): KeyValueStore = VibrationMainSwitchStore(context)

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

    override fun onPreferenceChange(preference: Preference, newValue: Any): Boolean {
        if (newValue == true) {
            // Play a haptic as preview for the main toggle only when touch feedback is enabled.
            VibrationPreferenceConfig.playVibrationPreview(
                preference.context.vibrator,
                VibrationAttributes.USAGE_TOUCH,
            )
        }
        return true
    }

    companion object {
        const val KEY = Settings.System.VIBRATE_ON
    }
}

/** Provides SettingsStore for vibration main switch with custom default value. */
@Suppress("UNCHECKED_CAST")
class VibrationMainSwitchStore(
    context: Context,
    private val settingsStore: KeyValueStore = SettingsSystemStore.get(context),
) : KeyValueStoreDelegate {

    override val keyValueStoreDelegate
        get() = settingsStore

    override fun <T : Any> getDefaultValue(key: String, valueType: Class<T>) = DEFAULT_VALUE as T

    companion object {
        private const val DEFAULT_VALUE = true
    }
}

val Context.vibrator: Vibrator
    get() = getSystemService(Vibrator::class.java)!!

// LINT.ThenChange(VibrationMainSwitchPreferenceController.java)
