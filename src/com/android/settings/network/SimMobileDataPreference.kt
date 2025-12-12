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

package com.android.settings.network

import android.annotation.SuppressLint
import android.content.Context
import android.telephony.SubscriptionManager
import com.android.settings.R
import com.android.settingslib.metadata.PreferenceAvailabilityProvider
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.metadata.PreferenceSummaryProvider
import com.android.settingslib.preference.PreferenceBinding
import com.android.settingslib.preference.PreferenceBindingPlaceholder

/** Show primary mobile data's preference in dual active SIMs. */
@SuppressLint("MissingPermission")
class SimMobileDataPreference :
    PreferenceMetadata,
    PreferenceBinding,
    PreferenceBindingPlaceholder,
    PreferenceSummaryProvider,
    PreferenceAvailabilityProvider {

    override val key: String
        get() = KEY

    override val title: Int
        get() = R.string.mobile_data_settings_title

    override val icon: Int
        get() = R.drawable.ic_settings_data_usage

    override fun isAvailable(context: Context): Boolean {
        return context
            .getSystemService(SubscriptionManager::class.java)
            .activeSubscriptionIdList
            .size > 1
    }

    override fun getSummary(context: Context): CharSequence? {
        val subInfo =
            context
                .getSystemService(SubscriptionManager::class.java)
                ?.getActiveSubscriptionInfo(SubscriptionManager.getDefaultDataSubscriptionId())
        if (subInfo == null) {
            return ""
        }
        return subInfo.displayName
    }

    companion object {
        const val KEY = "sim_mobile_datas_preference_key"
        const val TAG = "SimMobileDatasPreference"
    }
}
