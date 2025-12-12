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
package com.android.settings.network.telephony.satellite

import android.content.Context
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceScreen
import com.android.settings.core.BasePreferenceController

/** Preference controller for Satellite functions in mobile network settings. */
class SatelliteSettingsPreferenceCategoryController(context: Context, key: String) :
    BasePreferenceController(context, key), DefaultLifecycleObserver {
    private var preferenceCategory: PreferenceCategory? = null

    override fun getAvailabilityStatus() = AVAILABLE

    override fun displayPreference(screen: PreferenceScreen) {
        preferenceCategory = screen.findPreference(preferenceKey) as PreferenceCategory?
    }

    override fun onResume(owner: LifecycleOwner) {
        preferenceCategory?.isVisible = hasVisiblePreference(preferenceCategory)
    }

    fun hasVisiblePreference(category: PreferenceCategory?): Boolean {
        if (category == null) {
            return false
        }

        for (i in 0 until category.preferenceCount) {
            val preference = category.getPreference(i)
            if (preference.isVisible) {
                return true
            }
        }
        return false
    }
}
