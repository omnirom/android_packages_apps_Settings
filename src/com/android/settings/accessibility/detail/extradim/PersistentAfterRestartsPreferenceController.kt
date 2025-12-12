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
import android.provider.Settings
import androidx.preference.Preference
import androidx.preference.PreferenceScreen
import com.android.settings.R
import com.android.settings.core.TogglePreferenceController

/** PreferenceController for "Keep on after device restart" setting on extra dim feature */
class PersistentAfterRestartsPreferenceController(override val context: Context, prefKey: String) :
    TogglePreferenceController(context, prefKey), ExtraDimSettingDependent {

    private val colorDisplayManager: ColorDisplayManager? =
        context.getSystemService(ColorDisplayManager::class.java)
    private var preference: Preference? = null
    override val contentObserver: ContentObserver =
        object : ContentObserver(Looper.myLooper()?.run { Handler(/* async= */ false) }) {
            override fun onChange(selfChange: Boolean) {
                preference?.let { updateState(it) }
            }
        }

    override fun getAvailabilityStatus(): Int {
        if (!ColorDisplayManager.isReduceBrightColorsAvailable(mContext)) {
            return UNSUPPORTED_ON_DEVICE
        }
        if (colorDisplayManager?.isReduceBrightColorsActivated == true) {
            return AVAILABLE
        }
        return DISABLED_DEPENDENT_SETTING
    }

    override fun displayPreference(screen: PreferenceScreen?) {
        super.displayPreference(screen)
        preference = screen?.findPreference(preferenceKey)
    }

    override fun updateState(preference: Preference?) {
        super.updateState(preference)
        preference?.isEnabled = colorDisplayManager?.isReduceBrightColorsActivated == true
    }

    override fun isChecked(): Boolean {
        return Settings.Secure.getInt(
            context.contentResolver,
            Settings.Secure.REDUCE_BRIGHT_COLORS_PERSIST_ACROSS_REBOOTS,
            0,
        ) == 1
    }

    override fun setChecked(isChecked: Boolean): Boolean {
        return Settings.Secure.putInt(
            context.contentResolver,
            Settings.Secure.REDUCE_BRIGHT_COLORS_PERSIST_ACROSS_REBOOTS,
            (if (isChecked) 1 else 0),
        )
    }

    override fun getSliceHighlightMenuRes(): Int = R.string.menu_key_accessibility

    override fun isSliceable(): Boolean = false
}
