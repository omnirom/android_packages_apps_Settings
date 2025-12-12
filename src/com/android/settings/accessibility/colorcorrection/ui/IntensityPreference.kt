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
import android.view.accessibility.AccessibilityManager
import androidx.preference.Preference
import com.android.settings.R
import com.android.settings.accessibility.shared.utils.DebounceConfigurationChangeCommitController
import com.android.settingslib.datastore.HandlerExecutor
import com.android.settingslib.datastore.KeyValueStore
import com.android.settingslib.datastore.KeyedObserver
import com.android.settingslib.datastore.SettingsSecureStore
import com.android.settingslib.metadata.IntRangeValuePreference
import com.android.settingslib.metadata.PreferenceLifecycleContext
import com.android.settingslib.metadata.PreferenceLifecycleProvider
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.metadata.PreferenceSummaryProvider
import com.android.settingslib.metadata.ReadWritePermit
import com.android.settingslib.widget.SliderPreference
import com.android.settingslib.widget.SliderPreferenceBinding
import kotlin.time.Duration.Companion.milliseconds

/** A preference that allows the user to adjust the intensity of color correction. */
class IntensityPreference(context: Context) :
    IntRangeValuePreference,
    SliderPreferenceBinding,
    PreferenceSummaryProvider,
    PreferenceLifecycleProvider {

    private var debounceCommitController: DebounceConfigurationChangeCommitController? = null
    private var settingsKeyedObserver: KeyedObserver<String>? = null
    private val dataStore: KeyValueStore by lazy {
        SettingsSecureStore.get(context).apply {
            setDefaultValue(KEY, DEFAULT_SATURATION_LEVEL)
            setDefaultValue(SETTING_KEY_COLOR_CORRECTION_MODE, DEFAULT_MODE)
        }
    }

    override val key: String
        get() = KEY

    override val title: Int
        get() = R.string.daltonizer_saturation_title

    override fun getSummary(context: Context): CharSequence? =
        if (isEnabled(context)) ""
        else context.getText(R.string.daltonizer_saturation_unavailable_summary)

    override fun storage(context: Context): KeyValueStore = dataStore

    override fun getReadPermissions(context: Context) = SettingsSecureStore.getReadPermissions()

    override fun getWritePermissions(context: Context) = SettingsSecureStore.getWritePermissions()

    override fun getReadPermit(context: Context, callingPid: Int, callingUid: Int) =
        ReadWritePermit.ALLOW

    override fun getWritePermit(context: Context, callingPid: Int, callingUid: Int) =
        ReadWritePermit.ALLOW

    override fun isEnabled(context: Context): Boolean {
        val colorCorrectionEnabled =
            dataStore.getBoolean(SETTING_KEY_COLOR_CORRECTION_ENABLED) ?: false
        val colorCorrectionMode =
            dataStore.getInt(SETTING_KEY_COLOR_CORRECTION_MODE) ?: DEFAULT_MODE

        val isDaltonizerActive = colorCorrectionMode != AccessibilityManager.DALTONIZER_DISABLED
        val isNotMonochromacy =
            colorCorrectionMode != AccessibilityManager.DALTONIZER_SIMULATE_MONOCHROMACY

        // Saturation level isn't applicable if color correction is off,
        // or if the mode is disabled, or if the mode is monochromacy.
        return colorCorrectionEnabled && isDaltonizerActive && isNotMonochromacy
    }

    override fun bind(preference: Preference, metadata: PreferenceMetadata) {
        super.bind(preference, metadata)
        preference as SliderPreference
        preference.updatesContinuously = true
    }

    override fun getMinValue(context: Context): Int = SATURATION_MIN

    override fun getMaxValue(context: Context): Int = SATURATION_MAX

    override fun onStart(context: PreferenceLifecycleContext) {
        debounceCommitController =
            DebounceConfigurationChangeCommitController(scope = context.lifecycleScope)
        if (settingsKeyedObserver == null) {
            settingsKeyedObserver = KeyedObserver { key, _ ->
                val notifyChange = { context.notifyPreferenceChange(bindingKey) }
                if (key == SETTING_KEY_COLOR_CORRECTION_MODE) {
                    notifyChange.invoke()
                } else {
                    // Add delay to prevent slider flickering due to configuration change cause by
                    // toggling color correction main switch.
                    debounceCommitController?.commitDelayed(
                        delay = 100.milliseconds,
                        commitAction = notifyChange,
                    )
                }
            }
        }

        settingsKeyedObserver?.let { observer ->
            dataStore.addObserver(SETTING_KEY_COLOR_CORRECTION_MODE, observer, HandlerExecutor.main)
            dataStore.addObserver(
                SETTING_KEY_COLOR_CORRECTION_ENABLED,
                observer,
                HandlerExecutor.main,
            )
        }
    }

    override fun onStop(context: PreferenceLifecycleContext) {
        settingsKeyedObserver?.let { observer ->
            dataStore.removeObserver(SETTING_KEY_COLOR_CORRECTION_MODE, observer)
            dataStore.removeObserver(SETTING_KEY_COLOR_CORRECTION_ENABLED, observer)
        }
        settingsKeyedObserver = null
        debounceCommitController?.cancelPendingCommit()
        debounceCommitController = null
    }

    companion object {
        internal const val KEY = Settings.Secure.ACCESSIBILITY_DISPLAY_DALTONIZER_SATURATION_LEVEL
        private const val SETTING_KEY_COLOR_CORRECTION_ENABLED =
            Settings.Secure.ACCESSIBILITY_DISPLAY_DALTONIZER_ENABLED
        private const val SETTING_KEY_COLOR_CORRECTION_MODE =
            Settings.Secure.ACCESSIBILITY_DISPLAY_DALTONIZER
        internal const val DEFAULT_MODE = AccessibilityManager.DALTONIZER_CORRECT_DEUTERANOMALY
        internal const val DEFAULT_SATURATION_LEVEL = 7
        internal const val SATURATION_MAX = 10
        internal const val SATURATION_MIN = 1
    }
}
