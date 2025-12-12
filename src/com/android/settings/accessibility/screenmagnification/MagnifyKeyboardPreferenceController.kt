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
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.annotation.StringRes
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.preference.Preference
import androidx.preference.PreferenceScreen
import com.android.internal.accessibility.util.AccessibilityUtils
import com.android.server.accessibility.Flags
import com.android.settings.R
import com.android.settings.accessibility.AccessibilityUtil
import com.android.settings.accessibility.MagnificationCapabilities
import com.android.settings.accessibility.extensions.isWindowMagnificationSupported
import com.android.settings.core.TogglePreferenceController

/**
 * Controller that accesses and switches the preference status of magnifying the software keyboard
 * feature.
 */
// LINT.IfChange
class MagnifyKeyboardPreferenceController(context: Context, prefKey: String) :
    TogglePreferenceController(context, prefKey), DefaultLifecycleObserver {
    private var preference: Preference? = null
    private val contentObserver: ContentObserver =
        object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean, uri: Uri?) {
                preference?.let { updateState(it) }
            }
        }

    override fun onResume(owner: LifecycleOwner) {
        MagnificationCapabilities.registerObserver(mContext, contentObserver)
    }

    override fun onPause(owner: LifecycleOwner) {
        MagnificationCapabilities.unregisterObserver(mContext, contentObserver)
    }

    override fun displayPreference(screen: PreferenceScreen?) {
        super.displayPreference(screen)
        screen?.findPreference<Preference?>(preferenceKey)?.apply {
            preference = this
            updateState(this)
        }
    }

    override fun isChecked(): Boolean {
        return Settings.Secure.getInt(
            mContext.getContentResolver(),
            Settings.Secure.ACCESSIBILITY_MAGNIFICATION_MAGNIFY_NAV_AND_IME,
            AccessibilityUtils.getMagnificationMagnifyKeyboardDefaultValue(mContext),
        ) == AccessibilityUtil.State.ON
    }

    override fun setChecked(isChecked: Boolean): Boolean {
        return Settings.Secure.putInt(
            mContext.getContentResolver(),
            Settings.Secure.ACCESSIBILITY_MAGNIFICATION_MAGNIFY_NAV_AND_IME,
            if (isChecked) AccessibilityUtil.State.ON else AccessibilityUtil.State.OFF,
        )
    }

    override fun getSliceHighlightMenuRes(): Int {
        return R.string.menu_key_accessibility
    }

    override fun isSliceable(): Boolean {
        return false
    }

    override fun getAvailabilityStatus(): Int {
        return if (
            Flags.enableMagnificationMagnifyNavBarAndIme() &&
                mContext.isWindowMagnificationSupported()
        ) {
            AVAILABLE
        } else {
            CONDITIONALLY_UNAVAILABLE
        }
    }

    override fun updateState(preference: Preference?) {
        super.updateState(preference)
        if (preference?.key == preferenceKey) {
            @MagnificationCapabilities.MagnificationMode
            val mode = MagnificationCapabilities.getCapabilities(mContext)
            preference.isEnabled =
                mode == MagnificationCapabilities.MagnificationMode.FULLSCREEN ||
                    mode == MagnificationCapabilities.MagnificationMode.ALL

            @StringRes
            val resId =
                if (preference.isEnabled) {
                    R.string.accessibility_screen_magnification_nav_ime_summary
                } else {
                    R.string.accessibility_screen_magnification_nav_ime_unavailable_summary
                }
            preference.setSummary(mContext.getString(resId))
        }
    }
}
// LINT.ThenChange(/src/com/android/settings/accessibility/screenmagnification/ui/MagnifyKeyboardSwitchPreference.kt)
