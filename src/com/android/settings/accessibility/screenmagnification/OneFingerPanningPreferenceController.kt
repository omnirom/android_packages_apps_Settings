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
import android.content.res.Resources
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.preference.Preference
import androidx.preference.PreferenceScreen
import androidx.preference.TwoStatePreference
import com.android.server.accessibility.Flags
import com.android.settings.R
import com.android.settings.accessibility.AccessibilityUtil
import com.android.settings.accessibility.MagnificationCapabilities
import com.android.settings.accessibility.MagnificationCapabilities.MagnificationMode
import com.android.settings.accessibility.MagnificationCapabilities.MagnificationMode.ALL
import com.android.settings.accessibility.MagnificationCapabilities.MagnificationMode.FULLSCREEN
import com.android.settings.accessibility.extensions.isInSetupWizard
import com.android.settings.accessibility.extensions.isWindowMagnificationSupported
import com.android.settings.core.TogglePreferenceController

// LINT.IfChange
class OneFingerPanningPreferenceController(context: Context, prefKey: String) :
    TogglePreferenceController(context, prefKey), DefaultLifecycleObserver {
    private var switchPreference: TwoStatePreference? = null

    private val contentObserver: ContentObserver =
        object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean, uri: Uri?) {
                switchPreference?.run { updateState(this) }
            }
        }

    override fun onResume(owner: LifecycleOwner) {
        MagnificationCapabilities.registerObserver(mContext, contentObserver)
    }

    override fun onPause(owner: LifecycleOwner) {
        MagnificationCapabilities.unregisterObserver(mContext, contentObserver)
    }

    override fun getAvailabilityStatus(): Int {
        return if (
            Flags.enableMagnificationOneFingerPanningGesture() &&
                !mContext.isInSetupWizard() &&
                mContext.isWindowMagnificationSupported()
        ) {
            AVAILABLE
        } else {
            CONDITIONALLY_UNAVAILABLE
        }
    }

    override fun displayPreference(screen: PreferenceScreen?) {
        super.displayPreference(screen)
        switchPreference = screen?.findPreference(preferenceKey)
    }

    override fun updateState(preference: Preference?) {
        super.updateState(preference)
        if (preference?.key == preferenceKey) {
            @MagnificationMode val mode = MagnificationCapabilities.getCapabilities(mContext)
            preference.isEnabled = mode == FULLSCREEN || mode == ALL
            refreshSummary(preference)
        }
    }

    override fun refreshSummary(preference: Preference?) {
        super.refreshSummary(preference)
        preference?.run {
            summary =
                mContext.getText(
                    if (isEnabled) {
                        R.string.accessibility_magnification_one_finger_panning_summary
                    } else {
                        R.string.accessibility_magnification_one_finger_panning_summary_unavailable
                    }
                )
        }
    }

    override fun isChecked(): Boolean {
        return isOneFingerPanningEnabled(mContext)
    }

    override fun setChecked(isChecked: Boolean): Boolean {
        return Settings.Secure.putInt(
            mContext.contentResolver,
            SETTING_KEY,
            if (isChecked) AccessibilityUtil.State.ON else AccessibilityUtil.State.OFF,
        )
    }

    override fun getSliceHighlightMenuRes(): Int {
        return R.string.menu_key_accessibility
    }

    companion object {
        const val SETTING_KEY = Settings.Secure.ACCESSIBILITY_SINGLE_FINGER_PANNING_ENABLED

        fun isOneFingerPanningEnabled(context: Context): Boolean {
            return Settings.Secure.getInt(
                context.contentResolver,
                SETTING_KEY,
                if (getOneFingerPanningDefaultValue(context)) AccessibilityUtil.State.ON
                else AccessibilityUtil.State.OFF,
            ) == AccessibilityUtil.State.ON
        }

        private fun getOneFingerPanningDefaultValue(context: Context): Boolean {
            return try {
                context.resources.getBoolean(
                    com.android.internal.R.bool.config_enable_a11y_magnification_single_panning
                )
            } catch (e: Resources.NotFoundException) {
                false
            }
        }
    }
}
// LINT.ThenChange(/src/com/android/settings/accessibility/screenmagnification/ui/OneFingerPanningSwitchPreference.kt)
