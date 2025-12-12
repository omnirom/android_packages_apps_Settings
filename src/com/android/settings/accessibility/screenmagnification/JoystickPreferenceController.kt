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

package com.android.settings.accessibility.screenmagnification

import android.content.Context
import android.provider.DeviceConfig
import android.provider.Settings
import com.android.settings.R
import com.android.settings.accessibility.AccessibilityUtil
import com.android.settings.accessibility.extensions.isInSetupWizard
import com.android.settings.accessibility.extensions.isWindowMagnificationSupported
import com.android.settings.core.TogglePreferenceController

/**
 * Controller that accesses and switches the preference status of the magnification joystick feature
 */
// LINT.IfChange
class JoystickPreferenceController(context: Context, prefKey: String) :
    TogglePreferenceController(context, prefKey) {

    override fun getAvailabilityStatus(): Int {
        return if (
            !mContext.isInSetupWizard() &&
                mContext.isWindowMagnificationSupported() &&
                isJoystickSupported()
        ) {
            AVAILABLE
        } else {
            CONDITIONALLY_UNAVAILABLE
        }
    }

    private fun isJoystickSupported(): Boolean {
        return DeviceConfig.getBoolean(
            DeviceConfig.NAMESPACE_WINDOW_MANAGER,
            "MagnificationJoystick__enable_magnification_joystick",
            false,
        )
    }

    override fun isChecked(): Boolean {
        return Settings.Secure.getInt(
            mContext.getContentResolver(),
            SETTING_KEY,
            AccessibilityUtil.State.OFF,
        ) == AccessibilityUtil.State.ON
    }

    override fun setChecked(isChecked: Boolean): Boolean {
        return Settings.Secure.putInt(
            mContext.getContentResolver(),
            SETTING_KEY,
            if (isChecked) AccessibilityUtil.State.ON else AccessibilityUtil.State.OFF,
        )
    }

    override fun getSliceHighlightMenuRes(): Int {
        return R.string.menu_key_accessibility
    }

    companion object {
        private val SETTING_KEY = Settings.Secure.ACCESSIBILITY_MAGNIFICATION_JOYSTICK_ENABLED
    }
}
// LINT.ThenChange(/src/com/android/settings/accessibility/screenmagnification/ui/JoystickPreferenceController.kt)
