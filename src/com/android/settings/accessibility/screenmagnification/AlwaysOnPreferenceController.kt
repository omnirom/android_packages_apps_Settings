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
import android.provider.DeviceConfig
import android.provider.Settings
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.preference.Preference
import androidx.preference.PreferenceScreen
import com.android.internal.accessibility.AccessibilityShortcutController
import com.android.internal.accessibility.common.ShortcutConstants.UserShortcutType
import com.android.internal.accessibility.util.ShortcutUtils
import com.android.settings.R
import com.android.settings.accessibility.AccessibilityUtil
import com.android.settings.accessibility.MagnificationCapabilities
import com.android.settings.accessibility.extensions.isInSetupWizard
import com.android.settings.accessibility.extensions.isWindowMagnificationSupported
import com.android.settings.core.TogglePreferenceController

/**
 * Controller that accesses and switches the preference status of the magnification always on
 * feature, where the magnifier will not deactivate on Activity transitions; it will only zoom out
 * to 100%.
 */
// LINT.IfChange
class AlwaysOnPreferenceController(context: Context, prefKey: String) :
    TogglePreferenceController(context, prefKey),
    DefaultLifecycleObserver {

    private var preference: Preference? = null

    private val contentObserver: ContentObserver =
        object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean, uri: Uri?) {
                preference?.run { updateState(this) }
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
        preference = screen?.findPreference(preferenceKey)
    }

    override fun getAvailabilityStatus(): Int {
        return if (
            !mContext.isInSetupWizard() &&
            mContext.isWindowMagnificationSupported() &&
            isAlwaysOnSupported(mContext)
        ) {
            // This preference's title "Keep on while switching apps" does not
            // mention magnification so it may confuse users who search a term
            // like "Keep on".
            // So we hide it from search if the user has no magnification shortcut enabled.
            if (ShortcutUtils.getEnabledShortcutTypes(
                    mContext, AccessibilityShortcutController.MAGNIFICATION_CONTROLLER_NAME
                ) == UserShortcutType.DEFAULT
            ) {
                AVAILABLE_UNSEARCHABLE
            } else {
                AVAILABLE
            }
        } else {
            CONDITIONALLY_UNAVAILABLE
        }
    }

    private fun isAlwaysOnSupported(context: Context): Boolean {
        val defaultValue =
            context
                .getResources()
                .getBoolean(com.android.internal.R.bool.config_magnification_always_on_enabled)

        return DeviceConfig.getBoolean(
            DeviceConfig.NAMESPACE_WINDOW_MANAGER,
            "AlwaysOnMagnifier__enable_always_on_magnifier",
            defaultValue,
        )
    }

    override fun isChecked(): Boolean {
        return Settings.Secure.getInt(
            mContext.getContentResolver(),
            SETTING_KEY,
            AccessibilityUtil.State.ON,
        ) == AccessibilityUtil.State.ON
    }

    override fun setChecked(isChecked: Boolean): Boolean {
        return Settings.Secure.putInt(
            mContext.getContentResolver(),
            SETTING_KEY,
            if (isChecked) AccessibilityUtil.State.ON else AccessibilityUtil.State.OFF,
        )
    }

    override fun updateState(preference: Preference?) {
        super.updateState(preference)
        preference?.run {
            @MagnificationCapabilities.MagnificationMode
            val mode = MagnificationCapabilities.getCapabilities(mContext)

            isEnabled =
                mode == MagnificationCapabilities.MagnificationMode.FULLSCREEN ||
                        mode == MagnificationCapabilities.MagnificationMode.ALL
            refreshSummary(this)
        }
    }

    override fun refreshSummary(preference: Preference?) {
        preference?.run {
            summary =
                mContext.getText(
                    if (isEnabled) {
                        R.string.accessibility_screen_magnification_always_on_summary
                    } else {
                        R.string.accessibility_screen_magnification_always_on_unavailable_summary
                    }
                )
        }
    }

    override fun getSliceHighlightMenuRes(): Int {
        return R.string.menu_key_accessibility
    }

    companion object {
        private const val SETTING_KEY = Settings.Secure.ACCESSIBILITY_MAGNIFICATION_ALWAYS_ON_ENABLED
    }
}
// LINT.ThenChange(/src/com/android/settings/accessibility/screenmagnification/ui/AlwaysOnSwitchPreference.kt)
