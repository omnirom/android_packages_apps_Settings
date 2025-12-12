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

import android.content.Context
import com.android.settings.R
import com.android.settings.deviceinfo.HardwareInfoPreferenceController
import com.android.settingslib.metadata.PreferenceAvailabilityProvider
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.metadata.PreferenceSummaryProvider
import com.android.settingslib.preference.PreferenceBinding

// LINT.IfChange
class DeviceModelPreference :
    PreferenceMetadata,
    PreferenceBinding,
    PreferenceSummaryProvider,
    PreferenceAvailabilityProvider {
    override val key: String
        get() = KEY

    override val title: Int
        get() = R.string.model_info

    override fun getSummary(context: Context): CharSequence? =
        HardwareInfoPreferenceController.getDeviceModel()

    override fun isAvailable(context: Context) =
        context.resources.getBoolean(R.bool.config_show_device_model)

    override fun createWidget(context: Context) =
        super.createWidget(context).apply {
            isCopyingEnabled = true
            isSelectable = false
        }

    companion object {
        const val KEY = "hardware_info_device_model"
    }
}
// LINT.ThenChange(DeviceModelPreferenceController.java)

