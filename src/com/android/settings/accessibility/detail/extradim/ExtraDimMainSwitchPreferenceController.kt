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

package com.android.settings.accessibility.detail.extradim

import android.content.Context
import android.database.ContentObserver
import android.hardware.display.ColorDisplayManager
import android.os.Handler
import android.os.Looper
import androidx.preference.Preference
import androidx.preference.PreferenceScreen
import androidx.preference.TwoStatePreference
import com.android.internal.accessibility.AccessibilityShortcutController.REDUCE_BRIGHT_COLORS_COMPONENT_NAME
import com.android.settings.accessibility.AccessibilityStatsLogUtils
import com.android.settings.core.BasePreferenceController

class ExtraDimMainSwitchPreferenceController(override val context: Context, prefKey: String) :
    BasePreferenceController(context, prefKey),
    ExtraDimSettingDependent,
    Preference.OnPreferenceChangeListener {

    private val colorDisplayManager: ColorDisplayManager? =
        context.getSystemService(ColorDisplayManager::class.java)
    private var preference: TwoStatePreference? = null
    override val contentObserver: ContentObserver =
        object : ContentObserver(Looper.myLooper()?.run { Handler(/* async= */ false) }) {
            override fun onChange(selfChange: Boolean) {
                preference?.let { updateState(it) }
            }
        }

    override fun getAvailabilityStatus(): Int = AVAILABLE

    override fun displayPreference(screen: PreferenceScreen?) {
        super.displayPreference(screen)
        preference = screen?.findPreference(preferenceKey)
    }

    override fun updateState(preference: Preference?) {
        super.updateState(preference)
        if (preference is TwoStatePreference) {
            preference.isChecked = isChecked()
        }
    }

    private fun setChecked(preference: TwoStatePreference, checked: Boolean) {
        AccessibilityStatsLogUtils.logAccessibilityServiceEnabled(
            REDUCE_BRIGHT_COLORS_COMPONENT_NAME,
            checked,
        )
        colorDisplayManager?.setReduceBrightColorsActivated(checked)
        updateState(preference)
    }

    private fun isChecked(): Boolean {
        return colorDisplayManager?.isReduceBrightColorsActivated == true
    }

    override fun onPreferenceChange(preference: Preference, newValue: Any?): Boolean {
        if (preference is TwoStatePreference && newValue is Boolean) {
            setChecked(preference, newValue)
        }
        return false
    }
}
