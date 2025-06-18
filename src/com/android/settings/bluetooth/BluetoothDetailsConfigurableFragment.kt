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

package com.android.settings.bluetooth

import android.os.Bundle
import android.os.UserManager
import android.view.View
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceGroup
import com.android.settings.dashboard.RestrictedDashboardFragment

/** Base class for bluetooth settings which makes the preference visibility/order configurable. */
abstract class BluetoothDetailsConfigurableFragment :
    RestrictedDashboardFragment(UserManager.DISALLOW_CONFIG_BLUETOOTH) {
    private var displayOrder: List<String>? = null

    fun setPreferenceDisplayOrder(prefKeyOrder: List<String>?) {
        if (displayOrder == prefKeyOrder) {
            return
        }
        displayOrder = prefKeyOrder
        updatePreferenceOrder()
    }

    private val invisiblePrefCategory: PreferenceGroup by lazy {
        preferenceScreen.findPreference<PreferenceGroup>(INVISIBLE_CATEGORY)
            ?: run {
                PreferenceCategory(requireContext())
                    .apply {
                        key = INVISIBLE_CATEGORY
                        isVisible = false
                        isOrderingAsAdded = true
                    }
                    .also { preferenceScreen.addPreference(it) }
            }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        updatePreferenceOrder()
    }

    private fun updatePreferenceOrder() {
        val order = displayOrder?: return
        if (preferenceScreen == null) {
            return
        }
        preferenceScreen.isOrderingAsAdded = true
        val allPrefs =
            (invisiblePrefCategory.getAndRemoveAll() + preferenceScreen.getAndRemoveAll()).filter {
                it != invisiblePrefCategory
            }
        allPrefs.forEach { it.order = Preference.DEFAULT_ORDER }
        val visiblePrefs =
            allPrefs.filter { order.contains(it.key) }.sortedBy { order.indexOf(it.key) }
        val invisiblePrefs = allPrefs.filter { !order.contains(it.key) }
        preferenceScreen.addPreferences(visiblePrefs)
        preferenceScreen.addPreference(invisiblePrefCategory)
        invisiblePrefCategory.addPreferences(invisiblePrefs)
    }

    private fun PreferenceGroup.getAndRemoveAll(): List<Preference> {
        val prefs = mutableListOf<Preference>()
        for (i in 0..<preferenceCount) {
            prefs.add(getPreference(i))
        }
        removeAll()
        return prefs
    }

    private fun PreferenceGroup.addPreferences(prefs: List<Preference>) {
        for (pref in prefs) {
            addPreference(pref)
        }
    }

    private companion object {
        const val INVISIBLE_CATEGORY = "invisible_profile_category"
    }
}
