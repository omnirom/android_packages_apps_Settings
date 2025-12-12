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

import android.app.UiModeManager
import android.app.settings.SettingsEnums
import android.content.Context
import android.provider.Settings
import androidx.preference.Preference
import com.android.settings.R
import com.android.settingslib.datastore.HandlerExecutor
import com.android.settingslib.datastore.KeyedObserver
import com.android.settingslib.datastore.SettingsSecureStore
import com.android.settingslib.metadata.PreferenceAvailabilityProvider
import com.android.settingslib.metadata.PreferenceLifecycleContext
import com.android.settingslib.metadata.PreferenceLifecycleProvider
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.metadata.PreferenceSummaryProvider
import com.android.settingslib.preference.PreferenceBinding
import java.time.LocalTime

// LINT.IfChange
sealed class DarkModeCustomTimePreference(protected val uiModeManager: UiModeManager) :
    PreferenceMetadata,
    PreferenceBinding,
    PreferenceSummaryProvider,
    PreferenceAvailabilityProvider,
    PreferenceLifecycleProvider,
    Preference.OnPreferenceClickListener {

    protected lateinit var lifecycleContext: PreferenceLifecycleContext

    private var nightModeCustomTypeObserver: KeyedObserver<String>? = null

    override val indexable
        get() = false

    override fun bind(preference: Preference, metadata: PreferenceMetadata) {
        super.bind(preference, metadata)
        preference.onPreferenceClickListener = this
    }

    override fun onCreate(context: PreferenceLifecycleContext) {
        super.onCreate(context)
        lifecycleContext = context
        lifecycleContext.childFragmentManager.setFragmentResultListener(
            key,
            lifecycleContext.lifecycleOwner,
        ) { requestKey, bundle ->
            val selectedTime =
                LocalTime.of(
                    DarkModeTimePicker.getResultHour(bundle),
                    DarkModeTimePicker.getResultMinute(bundle),
                )

            updateCustomTime(selectedTime)
            lifecycleContext.notifyPreferenceChange(key)
        }
    }

    override fun onStart(context: PreferenceLifecycleContext) {
        val observer = KeyedObserver<String> { _, _ -> context.notifyPreferenceChange(key) }
        nightModeCustomTypeObserver = observer
        SettingsSecureStore.get(context)
            .addObserver(NIGHT_MODE_CUSTOM_TYPE_SETTING_KEY, observer, HandlerExecutor.main)
    }

    override fun onStop(context: PreferenceLifecycleContext) {
        nightModeCustomTypeObserver?.let {
            SettingsSecureStore.get(context).removeObserver(NIGHT_MODE_CUSTOM_TYPE_SETTING_KEY, it)
            nightModeCustomTypeObserver = null
        }
    }

    override fun isAvailable(context: Context) =
        uiModeManager.nightMode == UiModeManager.MODE_NIGHT_CUSTOM &&
            uiModeManager.nightModeCustomType == UiModeManager.MODE_NIGHT_CUSTOM_TYPE_SCHEDULE

    abstract fun updateCustomTime(time: LocalTime)

    companion object {
        const val NIGHT_MODE_CUSTOM_TYPE_SETTING_KEY = Settings.Secure.UI_NIGHT_MODE_CUSTOM_TYPE
    }
}

/** The "Start Time" preference. */
class StartTimePreference(uiModeManager: UiModeManager) :
    DarkModeCustomTimePreference(uiModeManager) {

    override val key
        get() = KEY

    override val title
        get() = R.string.night_display_start_time_title

    override fun getSummary(context: Context): CharSequence? =
        TimeFormatter(context).of(uiModeManager.customNightModeStart)

    override fun onPreferenceClick(preference: Preference): Boolean {
        DarkModeTimePicker.showDialog(
            lifecycleContext.childFragmentManager,
            key,
            SettingsEnums.DIALOG_DARK_THEME_SET_START_TIME,
            uiModeManager.customNightModeStart,
        )
        return true
    }

    override fun updateCustomTime(time: LocalTime) {
        uiModeManager.customNightModeStart = time
    }

    companion object {
        const val KEY = "dark_theme_start_time"
    }
}

/** The "End Time" preference. */
class EndTimePreference(uiModeManager: UiModeManager) :
    DarkModeCustomTimePreference(uiModeManager) {

    override val key
        get() = KEY

    override val title
        get() = R.string.night_display_end_time_title

    override fun getSummary(context: Context): CharSequence? =
        TimeFormatter(context).of(uiModeManager.customNightModeEnd)

    override fun onPreferenceClick(preference: Preference): Boolean {
        DarkModeTimePicker.showDialog(
            lifecycleContext.childFragmentManager,
            key,
            SettingsEnums.DIALOG_DARK_THEME_SET_END_TIME,
            uiModeManager.customNightModeEnd,
        )
        return true
    }

    override fun updateCustomTime(time: LocalTime) {
        uiModeManager.customNightModeEnd = time
    }

    companion object {
        const val KEY = "dark_theme_end_time"
    }
}
// LINT.ThenChange(DarkModeCustomPreferenceController.java)
