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

package com.android.settings.wifi

import android.util.Log
import androidx.preference.PreferenceCategory
import com.android.settings.dashboard.RestrictedDashboardFragment

class WifiCategory(
    private val fragment: RestrictedDashboardFragment,
) {

    val preferenceCategory: PreferenceCategory?
        get() = fragment.findPreference(KEY)

    fun removeWifiEntryPreferences() {
        val category = preferenceCategory ?: run {
            Log.w(TAG, "Can't find PreferenceCategory to remove WifiEntryPreference!")
            return
        }
        for (i in category.preferenceCount - 1 downTo 0) {
            val preference = category.getPreference(i)
            if (preference is WifiEntryPreference) category.removePreference(preference)
        }
    }

    companion object {
        const val TAG = "WifiCategory"
        const val KEY = "wifi_category"
    }
}