/*
 * Copyright (C) 2025 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings.datausage

import android.app.settings.SettingsEnums
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.telephony.SubscriptionManager
import androidx.fragment.app.Fragment
import com.android.settings.R
import com.android.settings.Settings.MobileDataUsageListActivity
import com.android.settings.core.PreferenceScreenMixin
import com.android.settings.datausage.lib.BillingCycleRepository
import com.android.settings.datausage.lib.DataUsageLib
import com.android.settings.datausage.lib.NetworkCycleDataRepository
import com.android.settings.datausage.lib.NetworkStatsRepository.Companion.AllTimeRange
import com.android.settings.flags.Flags
import com.android.settings.network.telephony.MobileNetworkScreen
import com.android.settings.network.telephony.subscriptionManager
import com.android.settings.utils.makeLaunchIntent
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.metadata.PreferenceSummaryProvider
import com.android.settingslib.metadata.ProvidePreferenceScreen
import com.android.settingslib.metadata.preferenceHierarchy
import com.android.settingslib.spaprivileged.framework.common.userManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow

// LINT.IfChange
/** Preference screen for Network & Internet -> SIMs -> [SIM] -> App data usage. */
@ProvidePreferenceScreen(DataUsageListScreen.KEY, parameterized = true)
open class DataUsageListScreen(override val arguments: Bundle) :
    PreferenceScreenMixin, PreferenceSummaryProvider {

    private val subId =
        arguments.getInt(Settings.EXTRA_SUB_ID, SubscriptionManager.INVALID_SUBSCRIPTION_ID)

    override val key: String
        get() = KEY

    override val title: Int
        get() = R.string.app_cellular_data_usage

    override val screenTitle: Int
        get() = R.string.cellular_data_usage

    override val highlightMenuKey: Int
        get() = R.string.menu_key_network

    override fun getMetricsCategory() = SettingsEnums.DATA_USAGE_LIST

    override fun isFlagEnabled(context: Context) = Flags.deeplinkNetworkAndInternet25q4()

    override fun fragmentClass(): Class<out Fragment> = DataUsageList::class.java

    override fun hasCompleteHierarchy() = false

    override fun getPreferenceHierarchy(context: Context, coroutineScope: CoroutineScope) =
        preferenceHierarchy(context) {}

    override fun getLaunchIntent(context: Context, metadata: PreferenceMetadata?): Intent? {
        val intent =
            makeLaunchIntent(context, MobileDataUsageListActivity::class.java, metadata?.key)
        intent.putExtra(Settings.EXTRA_SUB_ID, subId)
        return intent
    }

    override fun isEnabled(context: Context) =
        (context.subscriptionManager?.isActiveSubscriptionId(subId) ?: false) &&
            !context.userManager.isGuestUser &&
            BillingCycleRepository(context).isBandwidthControlEnabled() &&
            getDataUsageInfo(context, subId).isEnabled

    override fun getSummary(context: Context) = getDataUsageInfo(context, subId).summary

    companion object {
        const val KEY = "data_usage_summary"

        @JvmStatic
        fun parameters(context: Context): Flow<Bundle> {
            return MobileNetworkScreen.parameters(context)
        }

        /* Coupled info about data usage like the summary and if it's enabled. */
        data class DataUsageInfo(val summary: String?, val isEnabled: Boolean)

        /** Obtains data usage info like summary and if it's enabled in one request. */
        private fun getDataUsageInfo(context: Context, subId: Int): DataUsageInfo {
            val repository =
                getNetworkTemplate(context, subId)?.let { NetworkCycleDataRepository(context, it) }
            return getDataUsageSummaryAndEnabledHelper(context, repository)
        }

        private fun getNetworkTemplate(context: Context, subId: Int) =
            if (SubscriptionManager.isValidSubscriptionId(subId)) {
                DataUsageLib.getMobileTemplate(context, subId)
            } else null

        private fun getDataUsageSummaryAndEnabledHelper(
            context: Context,
            repository: NetworkCycleDataRepository?,
        ): DataUsageInfo {
            if (repository == null) {
                return DataUsageInfo(null, false)
            }

            repository.loadFirstCycle()?.let { usageData ->
                val formattedDataUsage =
                    context.getString(
                        R.string.data_usage_template,
                        usageData.formatUsage(context),
                        usageData.formatDateRange(context),
                    )
                val hasUsage = usageData.usage > 0 || repository.queryUsage(AllTimeRange).usage > 0
                return DataUsageInfo(formattedDataUsage, hasUsage)
            }

            val allTimeUsage = repository.queryUsage(AllTimeRange)
            return DataUsageInfo(allTimeUsage.getDataUsedString(context), allTimeUsage.usage > 0)
        }
    }
}
// LINT.ThenChange(DataUsageList.kt)
