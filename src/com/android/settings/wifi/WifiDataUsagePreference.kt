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

import android.app.settings.SettingsEnums
import android.content.Context
import android.content.Intent
import android.net.NetworkTemplate
import android.os.Bundle
import android.telephony.SubscriptionManager
import android.util.Log
import com.android.settings.R
import com.android.settings.core.SubSettingLauncher
import com.android.settings.datausage.DataUsageList
import com.android.settings.datausage.DataUsageUtils
import com.android.settings.datausage.lib.DataUsageFormatter
import com.android.settingslib.metadata.PreferenceAvailabilityProvider
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.metadata.PreferenceSummaryProvider
import com.android.settingslib.net.DataUsageController

class WifiDataUsagePreference(context: Context):
    PreferenceMetadata,
    PreferenceSummaryProvider,
    PreferenceAvailabilityProvider {

    private val isAvailable: Boolean = DataUsageUtils.hasWifiRadio(context)
    private var dataUsage: String = context.getString(R.string.summary_placeholder)
    private var isEnabled: Boolean = true
    private var launchIntent: Intent? = null

    init {
        val networkTemplate = NetworkTemplate.Builder(NetworkTemplate.MATCH_WIFI).build()
        val dataUsageController = DataUsageController(context)
        val usageInfo = dataUsageController.getDataUsageInfo(networkTemplate)
        val dataUsageFormatter = DataUsageFormatter(context)
        dataUsage = context.getString(
            R.string.data_usage_template,
            dataUsageFormatter.formatDataUsage(usageInfo.usageLevel),
            usageInfo.period
        )
        Log.d(TAG, "wifiDataUsage=$dataUsage")
        isEnabled = dataUsageController.getHistoricalUsageLevel(networkTemplate) > 0
        if (isEnabled) {
            val args = Bundle().apply {
                putParcelable(DataUsageList.EXTRA_NETWORK_TEMPLATE, networkTemplate)
                putInt(DataUsageList.EXTRA_SUB_ID, SubscriptionManager.INVALID_SUBSCRIPTION_ID)
            }
            launchIntent = SubSettingLauncher(context)
                .setArguments(args)
                .setDestination(DataUsageList::class.java.name)
                .setTitleRes(R.string.non_carrier_data_usage)
                .setSourceMetricsCategory(SettingsEnums.PAGE_UNKNOWN)
                .toIntent()
        }
    }

    override val key
        get() = KEY

    override val title
        get() = R.string.non_carrier_data_usage

    override fun getSummary(context: Context): CharSequence? = dataUsage

    override fun isAvailable(context: Context): Boolean = isAvailable

    override fun isEnabled(context: Context): Boolean = isEnabled

    override fun intent(context: Context): Intent? = launchIntent

    companion object {
        private const val TAG = "WifiDataUsagePreference"
        const val KEY = "non_carrier_data_usage"
    }
}