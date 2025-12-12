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
import android.provider.Settings.Secure.AccessibilityMagnificationCursorFollowingMode
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.preference.Preference
import androidx.preference.PreferenceScreen
import com.android.settings.R
import com.android.settings.accessibility.Flags
import com.android.settings.accessibility.MagnificationCapabilities
import com.android.settings.accessibility.extensions.isInSetupWizard
import com.android.settings.accessibility.screenmagnification.dialogs.CursorFollowingModeChooser
import com.android.settings.core.BasePreferenceController
import com.android.settings.inputmethod.InputPeripheralsSettingsUtils

/**
 * Controller that shows the magnification cursor following mode and the preference click behavior.
 */
// LINT.IfChange
class CursorFollowingModePreferenceController(context: Context, prefKey: String) :
    BasePreferenceController(context, prefKey), DefaultLifecycleObserver {

    private var preference: Preference? = null
    private lateinit var fragmentManager: FragmentManager

    private val contentObserver: ContentObserver =
        object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean, uri: Uri?) {
                preference?.run { updateState(this) }
            }
        }

    fun setFragmentManager(fragmentManager: FragmentManager) {
        this.fragmentManager = fragmentManager
    }

    override fun onResume(owner: LifecycleOwner) {
        MagnificationCapabilities.registerObserver(mContext, contentObserver)
        mContext.contentResolver.registerContentObserver(
            Settings.Secure.getUriFor(
                Settings.Secure.ACCESSIBILITY_MAGNIFICATION_CURSOR_FOLLOWING_MODE
            ),
            /* notifyForDescendants= */ false,
            contentObserver,
        )
    }

    override fun onPause(owner: LifecycleOwner) {
        mContext.contentResolver.unregisterContentObserver(contentObserver)
    }

    override fun displayPreference(screen: PreferenceScreen?) {
        super.displayPreference(screen)
        preference = screen?.findPreference(preferenceKey)
        updateState(preference)
    }

    override fun getAvailabilityStatus(): Int {
        return if (
            !mContext.isInSetupWizard() && isMagnificationCursorFollowingModeDialogSupported()
        ) {
            AVAILABLE
        } else {
            CONDITIONALLY_UNAVAILABLE
        }
    }

    override fun updateState(preference: Preference?) {
        super.updateState(preference)
        if (preference == null) {
            return
        }
        @MagnificationCapabilities.MagnificationMode
        val mode = MagnificationCapabilities.getCapabilities(mContext)
        preference.isEnabled =
            mode == MagnificationCapabilities.MagnificationMode.FULLSCREEN ||
                mode == MagnificationCapabilities.MagnificationMode.ALL
        refreshSummary(preference)
    }

    override fun getSummary(): CharSequence? {
        if (preference?.isEnabled == true) {
            val mode = getCursorFollowingMode()

            val stringId: Int =
                when (mode) {
                    Settings.Secure.ACCESSIBILITY_MAGNIFICATION_CURSOR_FOLLOWING_MODE_CENTER ->
                        R.string.accessibility_magnification_cursor_following_center

                    Settings.Secure.ACCESSIBILITY_MAGNIFICATION_CURSOR_FOLLOWING_MODE_EDGE ->
                        R.string.accessibility_magnification_cursor_following_edge

                    else -> R.string.accessibility_magnification_cursor_following_continuous
                }
            return mContext.getString(stringId)
        }

        return mContext.getString(
            R.string.accessibility_magnification_cursor_following_unavailable_summary
        )
    }

    @AccessibilityMagnificationCursorFollowingMode
    private fun getCursorFollowingMode(): Int {
        return Settings.Secure.getInt(
            mContext.contentResolver,
            Settings.Secure.ACCESSIBILITY_MAGNIFICATION_CURSOR_FOLLOWING_MODE,
            Settings.Secure.ACCESSIBILITY_MAGNIFICATION_CURSOR_FOLLOWING_MODE_CONTINUOUS,
        )
    }

    private fun isMagnificationCursorFollowingModeDialogSupported(): Boolean {
        return Flags.enableMagnificationCursorFollowingDialog() &&
            InputPeripheralsSettingsUtils.isMouse()
    }

    override fun handlePreferenceTreeClick(preference: Preference?): Boolean {
        if (preference?.key == preferenceKey) {
            CursorFollowingModeChooser.showDialog(
                fragmentManager,
                CURSOR_FOLLOWING_MODE_CHOOSER_REQUEST_KEY,
            )
            return true
        }

        return super.handlePreferenceTreeClick(preference)
    }

    companion object {
        private const val CURSOR_FOLLOWING_MODE_CHOOSER_REQUEST_KEY = "cursorFollowingModeChooser"
    }
}
// LINT.ThenChange(/src/com/android/settings/accessibility/screenmagnification/ui/CursorFollowingPreference.kt)
