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
import android.content.Context
import android.content.res.Configuration
import android.location.LocationManager
import android.os.PowerManager
import androidx.preference.DropDownPreference
import androidx.preference.Preference
import com.android.settings.R
import com.android.settings.display.TwilightLocationDialog
import com.android.settingslib.metadata.DiscreteStringValue
import com.android.settingslib.metadata.PreferenceLifecycleContext
import com.android.settingslib.metadata.PreferenceLifecycleProvider
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.metadata.PreferenceSummaryProvider
import com.android.settingslib.preference.PreferenceBinding

// LINT.IfChange
class DarkModeSchedulePreference(
    private var uiModeManager: UiModeManager,
    private var bedtimeSettings: BedtimeSettings,
) :
    PreferenceMetadata,
    DiscreteStringValue,
    PreferenceBinding,
    PreferenceSummaryProvider,
    PreferenceLifecycleProvider,
    Preference.OnPreferenceChangeListener {

    private lateinit var lifecycleContext: PreferenceLifecycleContext

    override val key: String
        get() = KEY

    override val keywords: Int
        get() = R.string.keywords_dark_ui_mode

    override val title: Int
        get() = R.string.dark_ui_auto_mode_title

    override val values: Int
        get() =
            if (bedtimeSettings.getBedtimeSettingsIntent() != null) {
                R.array.dark_ui_scheduler_with_bedtime_preference_titles
            } else {
                R.array.dark_ui_scheduler_preference_titles
            }

    override val valuesDescription: Int
        get() =
            if (bedtimeSettings.getBedtimeSettingsIntent() != null) {
                R.array.dark_ui_scheduler_with_bedtime_preference_titles
            } else {
                R.array.dark_ui_scheduler_preference_titles
            }

    override fun getSummary(context: Context): CharSequence? = "%s"

    override fun createWidget(context: Context) = DropDownPreference(context)

    override fun bind(preference: Preference, metadata: PreferenceMetadata) {
        super.bind(preference, metadata)
        preference as DropDownPreference
        preference.setValue(getCurrentMode(preference.context))
        preference.onPreferenceChangeListener = this
    }

    override fun onCreate(context: PreferenceLifecycleContext) {
        super.onCreate(context)
        lifecycleContext = context
    }

    override fun isEnabled(context: Context) =
        context.getSystemService(PowerManager::class.java)?.isPowerSaveMode == false

    override fun onPreferenceChange(preference: Preference, newValue: Any?): Boolean {
        val context = preference.context
        if (newValue == getCurrentMode(context)) {
            return false
        }

        when (newValue) {
            context.getString(R.string.dark_ui_auto_mode_never) -> {
                val active =
                    (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_YES) !=
                        0
                uiModeManager.nightMode =
                    if (active) UiModeManager.MODE_NIGHT_YES else UiModeManager.MODE_NIGHT_NO
            }
            context.getString(R.string.dark_ui_auto_mode_auto) -> {
                val locationManager = context.getSystemService(LocationManager::class.java)
                if (locationManager?.isLocationEnabled == false) {
                    TwilightLocationDialog.showLocationOff(context)
                    return false
                }

                if (locationManager?.lastLocation == null) {
                    TwilightLocationDialog.showLocationPending(context)
                }
                uiModeManager.nightMode = UiModeManager.MODE_NIGHT_AUTO
            }
            context.getString(R.string.dark_ui_auto_mode_custom) -> {
                uiModeManager.nightMode = UiModeManager.MODE_NIGHT_CUSTOM
            }
            context.getString(R.string.dark_ui_auto_mode_custom_bedtime) -> {
                uiModeManager.nightModeCustomType = UiModeManager.MODE_NIGHT_CUSTOM_TYPE_BEDTIME
            }
        }
        return true
    }

    private fun getCurrentMode(context: Context): String =
        context.getString(
            when (uiModeManager.nightMode) {
                UiModeManager.MODE_NIGHT_AUTO -> R.string.dark_ui_auto_mode_auto
                UiModeManager.MODE_NIGHT_CUSTOM -> {
                    val isCustomBedtime =
                        bedtimeSettings.getBedtimeSettingsIntent() != null &&
                            (uiModeManager.nightModeCustomType ==
                                UiModeManager.MODE_NIGHT_CUSTOM_TYPE_BEDTIME)
                    if (isCustomBedtime) R.string.dark_ui_auto_mode_custom_bedtime
                    else R.string.dark_ui_auto_mode_custom
                }

                else -> R.string.dark_ui_auto_mode_never
            }
        )

    companion object {
        const val KEY = "dark_ui_auto_mode"
    }
}
// LINT.ThenChange(DarkModeScheduleSelectorController.java)
