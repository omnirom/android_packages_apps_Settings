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
import android.os.SystemProperties
import android.util.Log
import androidx.annotation.VisibleForTesting
import androidx.preference.Preference
import androidx.preference.TwoStatePreference
import com.android.settings.core.PreferenceControllerMixin
import com.android.settingslib.development.DeveloperOptionsPreferenceController

class PrintVerboseLoggingController(val context: Context) :
    DeveloperOptionsPreferenceController(context),
    Preference.OnPreferenceChangeListener,
    PreferenceControllerMixin {

    private val TAG = "PrintVerboseLoggingController"

    companion object {
        @VisibleForTesting val PRINT_DEBUG_LOG_PROP = "debug.printing.logs.enabled"

        @VisibleForTesting val PRINT_DEBUG_LOG_PROP_ENABLED = "true"

        @VisibleForTesting val PRINT_DEBUG_LOG_PROP_DISABLED = "false"
    }

    override fun getPreferenceKey(): String = "verbose_printer_logging"

    override fun onPreferenceChange(pref: Preference, newValue: Any): Boolean {
        if (newValue !is Boolean) {
            Log.e(TAG, "Given non bool newValue: " + newValue)
            return false
        }
        if (newValue) {
            SystemProperties.set(PRINT_DEBUG_LOG_PROP, PRINT_DEBUG_LOG_PROP_ENABLED)
        } else {
            SystemProperties.set(PRINT_DEBUG_LOG_PROP, PRINT_DEBUG_LOG_PROP_DISABLED)
        }
        return true
    }

    override fun updateState(preference: Preference) {
        super.updateState(preference)
        if (preference !is TwoStatePreference) {
            // This should never happen.
            Log.e(TAG, "Given non TwoStatePreference: " + preference)
            return
        }
        preference.setChecked(
            PRINT_DEBUG_LOG_PROP_ENABLED.equals(SystemProperties.get(PRINT_DEBUG_LOG_PROP))
        )
    }

    override public fun onDeveloperOptionsSwitchDisabled() {
        super.onDeveloperOptionsSwitchDisabled()
        SystemProperties.set(PRINT_DEBUG_LOG_PROP, PRINT_DEBUG_LOG_PROP_DISABLED)
    }

    override fun isAvailable(): Boolean {
        return Flags.enablePrintDebugOption()
    }
}
