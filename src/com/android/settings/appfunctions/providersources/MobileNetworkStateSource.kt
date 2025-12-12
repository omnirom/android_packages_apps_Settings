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
package com.android.settings.appfunctions.providersources

import android.content.Context
import android.net.NetworkPolicyManager
import android.net.NetworkTemplate
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import android.text.TextUtils
import androidx.preference.ListPreference
import com.android.settings.R
import com.android.settings.appfunctions.DeviceStateAppFunctionType
import com.android.settings.datausage.DataPlanRepository
import com.android.settings.datausage.DataPlanRepositoryImpl
import com.android.settings.datausage.lib.DataUsageFormatter
import com.android.settings.datausage.lib.DataUsageLib
import com.android.settings.datausage.lib.INetworkCycleDataRepository
import com.android.settings.datausage.lib.NetworkCycleDataRepository
import com.android.settings.network.ProxySubscriptionManager
import com.android.settings.network.SubscriptionUtil
import com.android.settings.network.telephony.EnabledNetworkModePreferenceController
import com.google.android.appfunctions.schema.common.v1.devicestate.DeviceStateItem
import com.google.android.appfunctions.schema.common.v1.devicestate.PerScreenDeviceStates

class MobileNetworkStateSource : DeviceStateSource {
    override val appFunctionType: DeviceStateAppFunctionType =
        DeviceStateAppFunctionType.GET_MOBILE_DATA

    override suspend fun get(
        context: Context,
        sharedDeviceStateData: SharedDeviceStateData,
    ): List<PerScreenDeviceStates> {
        return listOf(
            PerScreenDeviceStates(
                description = "Saved Networks",
                deviceStateItems = getDeviceStateItems(context),
            )
        )
    }

    private fun getDeviceStateItems(context: Context): List<DeviceStateItem> {
        val subscriptionManager = context.getSystemService(SubscriptionManager::class.java)
        return SubscriptionUtil.getSelectableSubscriptionInfoList(context).flatMap {
            val subId = it.subscriptionId
            val telephonyManager =
                context
                    .getSystemService(TelephonyManager::class.java)
                    ?.createForSubscriptionId(subId)!!
            listOf(
                DeviceStateItem(
                    key = "sim_enabled_$subId",
                    jsonValue = subscriptionManager.isSubscriptionEnabled(subId).toString(),
                    hintText = "Whether the SIM with subscription ID $subId is enabled",
                ),
                DeviceStateItem(
                    key = "mobile_data_used_$subId",
                    jsonValue = getDataUsedString(context, subId),
                    hintText =
                        "Data used of subscription ID $subId in this cycle | This SIM data usage screen shows the amount of data each app has consumed from a specific SIM card.",
                ),
                DeviceStateItem(
                    key = "mobile_network_name_$subId",
                    jsonValue =
                        SubscriptionUtil.getUniqueSubscriptionDisplayName(it, context).toString(),
                    hintText = "Mobile network name of subscription ID $subId",
                ),
                DeviceStateItem(
                    key = "phone_number_$subId",
                    jsonValue = SubscriptionUtil.getBidiFormattedPhoneNumber(context, it),
                    hintText = "Phone number of subscription ID $subId",
                ),
                DeviceStateItem(
                    key = "data_roaming_enabled_$subId",
                    jsonValue = telephonyManager.isDataRoamingEnabled.toString(),
                    hintText = "Whether the data roaming of subscription ID $subId is enabled",
                ),
                DeviceStateItem(
                    key = "preferred_network_type_$subId",
                    jsonValue = getPreferredNetworkMode(context, subId),
                    hintText = "The preferred network type of subscription ID $subId",
                ),
                DeviceStateItem(
                    key = "auto_select_network_$subId",
                    jsonValue = telephonyManager.serviceState?.isManualSelection?.not().toString(),
                    hintText =
                        "Whether the subscription ID $subId enables automatically select network",
                ),
            )
        }
    }

    private fun getDataUsedString(context: Context, subId: Int): String {
        val networkPolicyManager = context.getSystemService(NetworkPolicyManager::class.java)!!
        val networkTemplate = DataUsageLib.getMobileTemplate(context, subId)
        val networkPolicy =
            networkPolicyManager.networkPolicies.find { policy ->
                policy.template == networkTemplate
            }
        val proxySubscriptionManager: ProxySubscriptionManager =
            ProxySubscriptionManager.getInstance(context)
        val networkCycleDataRepositoryFactory:
            (template: NetworkTemplate) -> INetworkCycleDataRepository =
            {
                NetworkCycleDataRepository(context, it)
            }
        val networkCycleDataRepository = networkCycleDataRepositoryFactory(networkTemplate)
        val dataPlanRepositoryFactory:
            (networkCycleDataRepository: INetworkCycleDataRepository) -> DataPlanRepository =
            {
                DataPlanRepositoryImpl(it)
            }
        val dataPlanInfo =
            dataPlanRepositoryFactory(networkCycleDataRepository)
                .getDataPlanInfo(
                    policy = networkPolicy!!,
                    plans = proxySubscriptionManager.get().getSubscriptionPlans(subId),
                )
        val dataUsageFormatter = DataUsageFormatter(context)

        val (number, units) = dataUsageFormatter.formatDataUsageWithUnits(dataPlanInfo.dataPlanUse)
        val template: CharSequence = context.getText(R.string.data_used_formatted)

        return TextUtils.expandTemplate(template, number, units).toString()
    }

    private fun getPreferredNetworkMode(context: Context, subId: Int): String {
        val preference = ListPreference(context)
        EnabledNetworkModePreferenceController(context, "enabled_networks_key").apply {
            init(subId, null)
            updateState(preference)
        }
        return preference.summary.toString()
    }
}
