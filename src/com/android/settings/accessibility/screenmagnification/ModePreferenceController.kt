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
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.preference.Preference
import androidx.preference.PreferenceScreen
import com.android.settings.accessibility.MagnificationCapabilities
import com.android.settings.accessibility.extensions.isInSetupWizard
import com.android.settings.accessibility.extensions.isWindowMagnificationSupported
import com.android.settings.accessibility.screenmagnification.dialogs.MagnificationModeChooser
import com.android.settings.core.BasePreferenceController

/** Handles the magnification mode preference content and its click behavior */
// LINT.IfChange
class ModePreferenceController(context: Context, prefKey: String) :
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

    override fun getAvailabilityStatus(): Int {
        return if (mContext.isInSetupWizard() || !mContext.isWindowMagnificationSupported()) {
            CONDITIONALLY_UNAVAILABLE
        } else {
            AVAILABLE
        }
    }

    override fun onStart(owner: LifecycleOwner) {
        MagnificationCapabilities.registerObserver(mContext, contentObserver)
    }

    override fun onStop(owner: LifecycleOwner) {
        MagnificationCapabilities.unregisterObserver(mContext, contentObserver)
    }

    override fun displayPreference(screen: PreferenceScreen?) {
        super.displayPreference(screen)
        preference = screen?.findPreference(preferenceKey)
        updateState(preference)
    }

    override fun getSummary(): CharSequence? {
        return MagnificationCapabilities.getSummary(
            mContext,
            MagnificationCapabilities.getCapabilities(mContext),
        )
    }

    override fun handlePreferenceTreeClick(preference: Preference?): Boolean {
        if (preference?.key == preferenceKey) {
            MagnificationModeChooser.showDialog(fragmentManager, MODE_CHOOSER_REQUEST_KEY)
            return true
        }

        return super.handlePreferenceTreeClick(preference)
    }

    companion object {
        private const val MODE_CHOOSER_REQUEST_KEY = "magnificationModeChooser"
    }
}
// LINT.ThenChange(/src/com/android/settings/accessibility/screenmagnification/ui/MagnificationModePreference.kt)
