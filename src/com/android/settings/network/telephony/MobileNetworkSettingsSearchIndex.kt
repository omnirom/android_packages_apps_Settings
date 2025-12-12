/*
 * Copyright (C) 2024 The Android Open Source Project
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

package com.android.settings.network.telephony

import android.content.Context
import android.provider.Settings
import android.telephony.SubscriptionInfo
import com.android.settings.R
import com.android.settings.datausage.BillingCyclePreferenceController.Companion.BillingCycleSearchItem
import com.android.settings.network.telephony.CarrierSettingsVersionPreferenceController.Companion.CarrierSettingsVersionSearchItem
import com.android.settings.network.telephony.DataUsagePreferenceController.Companion.DataUsageSearchItem
import com.android.settings.network.telephony.MmsMessagePreferenceController.Companion.MmsMessageSearchItem
import com.android.settings.network.telephony.NrAdvancedCallingPreferenceController.Companion.NrAdvancedCallingSearchItem
import com.android.settings.network.telephony.RoamingPreferenceController.Companion.RoamingSearchItem
import com.android.settings.network.telephony.VideoCallingPreferenceController.Companion.VideoCallingSearchItem
import com.android.settings.network.telephony.WifiCallingPreferenceController.Companion.WifiCallingSearchItem
import com.android.settings.network.telephony.satellite.SatelliteSettingPreferenceController.Companion.SatelliteConnectivitySearchItem
import com.android.settingslib.spa.search.SpaSearchIndexableItem
import com.android.settingslib.spa.search.SpaSearchIndexablePage
import com.android.settingslib.spa.search.SpaSearchLanding.BundleValue
import com.android.settingslib.spa.search.SpaSearchLanding.SpaSearchLandingFragment
import com.android.settingslib.spa.search.SpaSearchLanding.SpaSearchLandingKey

class MobileNetworkSettingsSearchIndex(
    private val searchItemsFactory: (context: Context) -> List<MobileNetworkSettingsSearchItem> =
        ::createSearchItems
) {
    data class MobileNetworkSettingsSearchResult(
        val key: String,
        val title: String,
        val keywords: String? = null,
    )

    interface MobileNetworkSettingsSearchItem {
        fun getSearchResult(subId: Int): MobileNetworkSettingsSearchResult?
    }

    fun getSearchIndexablePage(): SpaSearchIndexablePage {
        return SpaSearchIndexablePage(targetClass = MobileNetworkSettings::class.java) { context ->
            if (!isMobileNetworkSettingsSearchable(context)) {
                return@SpaSearchIndexablePage emptyList()
            }
            val subInfos = context.requireSubscriptionManager().activeSubscriptionInfoList
            if (subInfos.isNullOrEmpty()) {
                return@SpaSearchIndexablePage emptyList()
            }
            searchItemsFactory(context).flatMap { searchItem ->
                spaSearchIndexableItemList(context, searchItem, subInfos)
            }
        }
    }

    private fun spaSearchIndexableItemList(
        context: Context,
        searchItem: MobileNetworkSettingsSearchItem,
        subInfos: List<SubscriptionInfo>,
    ): List<SpaSearchIndexableItem> =
        subInfos.mapNotNull { subInfo ->
            searchItem.getSearchResult(subInfo.subscriptionId)?.let { searchResult ->
                searchIndexableItem(context, searchResult, subInfo)
            }
        }

    private fun searchIndexableItem(
        context: Context,
        searchResult: MobileNetworkSettingsSearchResult,
        subInfo: SubscriptionInfo,
    ): SpaSearchIndexableItem {
        val key =
            SpaSearchLandingKey.newBuilder()
                .setFragment(
                    SpaSearchLandingFragment.newBuilder()
                        .setFragmentName(MobileNetworkSettings::class.java.name)
                        .setPreferenceKey(searchResult.key)
                        .putArguments(
                            Settings.EXTRA_SUB_ID,
                            BundleValue.newBuilder().setIntValue(subInfo.subscriptionId).build(),
                        )
                )
                .build()
        val simsTitle = context.getString(R.string.provider_network_settings_title)
        return SpaSearchIndexableItem(
            searchLandingKey = key,
            pageTitle = "$simsTitle > ${subInfo.displayName}",
            itemTitle = searchResult.title,
            keywords = searchResult.keywords,
        )
    }

    companion object {
        /** suppress full page if user is not admin */
        @JvmStatic
        fun isMobileNetworkSettingsSearchable(context: Context): Boolean =
            SimRepository(context).canEnterMobileNetworkPage()

        fun createSearchItems(context: Context): List<MobileNetworkSettingsSearchItem> =
            listOf(
                BillingCycleSearchItem(context),
                CarrierSettingsVersionSearchItem(context),
                DataUsageSearchItem(context),
                MmsMessageSearchItem(context),
                NrAdvancedCallingSearchItem(context),
                PreferredNetworkModeSearchItem(context),
                RoamingSearchItem(context),
                VideoCallingSearchItem(context),
                WifiCallingSearchItem(context),
                SatelliteConnectivitySearchItem(context),
            )
    }
}
