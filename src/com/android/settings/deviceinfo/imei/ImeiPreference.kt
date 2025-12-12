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

package com.android.settings.deviceinfo.imei

import android.content.Context
import android.util.Log
import androidx.preference.Preference
import com.android.settings.R
import com.android.settings.Utils
import com.android.settings.wifi.utils.isAdminUser
import com.android.settings.wifi.utils.telephonyManager
import com.android.settingslib.metadata.PreferenceAvailabilityProvider
import com.android.settingslib.metadata.PreferenceLifecycleContext
import com.android.settingslib.metadata.PreferenceLifecycleProvider
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.metadata.PreferenceSummaryProvider
import com.android.settingslib.metadata.PreferenceTitleProvider
import com.android.settingslib.preference.PreferenceBinding
import com.android.settingslib.preference.PreferenceBindingPlaceholder

/**
 * Preference to show IMEI information for single and multi modem devices.
 */
class ImeiPreference(
    context: Context,
    private val slotIndex: Int,
    private val activeModemCount: Int,
) :
    PreferenceMetadata,
    PreferenceBinding,
    PreferenceBindingPlaceholder,
    PreferenceLifecycleProvider,
    PreferenceTitleProvider,
    PreferenceSummaryProvider,
    PreferenceAvailabilityProvider {

    private val imei: String? = context.getImei()
    private val formattedTitle: String = context.getFormattedTitle()

    override val key: String
        get() = KEY_PREFIX + "${slotIndex + 1}"

    override fun isAvailable(context: Context): Boolean =
        context.isAdminUser == true &&
                (Utils.isMobileDataCapable(context) || Utils.isVoiceCapable(context))

    override fun getTitle(context: Context): CharSequence? = formattedTitle

    override fun getSummary(context: Context): CharSequence? = imei

    override fun bind(preference: Preference, metadata: PreferenceMetadata) {
        super.bind(preference, metadata)
        preference.isCopyingEnabled = true
    }

    override fun onCreate(context: PreferenceLifecycleContext) {
        context.requirePreference<Preference>(key).onPreferenceClickListener =
            Preference.OnPreferenceClickListener {
                ImeiInfoDialogFragment.show(context.childFragmentManager, slotIndex, formattedTitle)
                return@OnPreferenceClickListener true
            }
    }

    private fun Context.getImei(): String? = telephonyManager?.getImei(slotIndex) ?: run {
        Log.e(TAG, "Failed to get IMEI for slot $slotIndex")
        null
    }

    private fun Context.getFormattedTitle(): String =
        if (activeModemCount <= 1) {
            getString(R.string.status_imei)
        } else {
            try {
                val titleId =
                    if (imei == telephonyManager?.primaryImei) R.string.imei_multi_sim_primary
                    else R.string.imei_multi_sim
                getString(titleId, slotIndex + 1)
            } catch (exception: Exception) {
                Log.e(TAG, "PrimaryImei not available.", exception)
                getString(R.string.imei_multi_sim, slotIndex + 1)
            }
        }

    companion object {
        private const val TAG = "ImeiPreference"
        const val KEY_PREFIX = "imei_info"
    }
}
