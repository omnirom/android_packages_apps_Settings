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

package com.android.settings.deviceinfo.hardwareinfo

import android.app.settings.SettingsEnums
import android.content.Context
import androidx.fragment.app.Fragment
import androidx.preference.Preference
import com.android.settings.R
import com.android.settings.core.PreferenceScreenMixin
import com.android.settings.deviceinfo.HardwareInfoPreferenceController.getDeviceModel
import com.android.settings.flags.Flags
import com.android.settingslib.metadata.PreferenceAvailabilityProvider
import com.android.settingslib.metadata.PreferenceSummaryProvider
import com.android.settingslib.metadata.ProvidePreferenceScreen
import com.android.settingslib.metadata.preferenceHierarchy
import com.android.settingslib.preference.PreferenceBinding
import kotlinx.coroutines.CoroutineScope

// LINT.IfChange
@ProvidePreferenceScreen(HardwareInfoScreen.KEY)
open class HardwareInfoScreen :
    PreferenceScreenMixin,
    PreferenceBinding,
    PreferenceAvailabilityProvider,
    PreferenceSummaryProvider {
    override val key: String
        get() = KEY

    override val title: Int
        get() = R.string.model_info

    override val keywords: Int
        get() = R.string.keywords_model_and_hardware

    override val highlightMenuKey: Int
        get() = R.string.menu_key_about_device

    override fun getMetricsCategory() = SettingsEnums.DIALOG_SETTINGS_HARDWARE_INFO

    override fun isFlagEnabled(context: Context) = Flags.catalystDeviceModel()

    override fun hasCompleteHierarchy() = false

    override fun fragmentClass(): Class<out Fragment>? = HardwareInfoFragment::class.java

    override fun isAvailable(context: Context) =
        context.resources.getBoolean(R.bool.config_show_device_model)

    override fun getSummary(context: Context): CharSequence? = getDeviceModel()

    override fun createWidget(context: Context): Preference {
        return super.createWidget(context).apply { isCopyingEnabled = true }
    }

    override fun getPreferenceHierarchy(context: Context, coroutineScope: CoroutineScope) =
        preferenceHierarchy(context) {
            +DeviceModelPreference()
            +HardwareVersionPreference()
        }

    companion object {
        const val KEY = "device_model"
    }
}
// LINT.ThenChange(HardwareInfoFragment.java)
