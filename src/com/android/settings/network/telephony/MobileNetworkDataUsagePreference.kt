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

package com.android.settings.network.telephony

import android.content.Context
import android.net.NetworkPolicy
import android.net.NetworkTemplate
import android.telephony.SubscriptionManager
import android.text.TextUtils
import android.util.Log
import androidx.preference.Preference
import com.android.settings.R
import com.android.settings.datausage.DataPlanInfo
import com.android.settings.datausage.DataPlanRepository
import com.android.settings.datausage.DataPlanRepositoryImpl
import com.android.settings.datausage.DataUsageSummaryPreference
import com.android.settings.datausage.DataUsageUtils
import com.android.settings.datausage.lib.DataUsageFormatter
import com.android.settings.datausage.lib.DataUsageLib.getMobileTemplate
import com.android.settings.datausage.lib.INetworkCycleDataRepository
import com.android.settings.datausage.lib.NetworkCycleDataRepository
import com.android.settings.network.ProxySubscriptionManager
import com.android.settings.network.policy.NetworkPolicyRepository
import com.android.settingslib.metadata.PreferenceAvailabilityProvider
import com.android.settingslib.metadata.PreferenceLifecycleContext
import com.android.settingslib.metadata.PreferenceLifecycleProvider
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.metadata.PreferenceSummaryProvider
import com.android.settingslib.preference.PreferenceBinding
import kotlin.math.max
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

// LINT.IfChange
class MobileNetworkDataUsagePreference(
    private val context: Context,
    private val coroutineScope: CoroutineScope? = null,
    private val subId: Int = SubscriptionManager.INVALID_SUBSCRIPTION_ID,
    private val proxySubscriptionManager: ProxySubscriptionManager =
        ProxySubscriptionManager.getInstance(context),
    private val networkPolicyRepository: NetworkPolicyRepository = NetworkPolicyRepository(context),
    private val networkCycleDataRepositoryFactory:
        (template: NetworkTemplate) -> INetworkCycleDataRepository =
        {
            NetworkCycleDataRepository(context, it)
        },
    private val dataPlanRepositoryFactory:
        (networkCycleDataRepository: INetworkCycleDataRepository) -> DataPlanRepository =
        {
            DataPlanRepositoryImpl(it)
        },
) :
    PreferenceMetadata,
    PreferenceLifecycleProvider,
    PreferenceBinding,
    PreferenceAvailabilityProvider,
    PreferenceSummaryProvider {

    data class DataUsageData(
        val summary: CharSequence? = null,
        val policy: NetworkPolicy? = null,
        val dataPlanInfo: DataPlanInfo? = null,
    )

    val dataUsageDataFlow = MutableStateFlow(DataUsageData())

    private val subInfo by lazy {
        if (DataUsageUtils.hasMobileData(context)) {
            proxySubscriptionManager.getAccessibleSubscriptionInfo(subId)
        } else null
    }

    private val networkTemplate by lazy { getMobileTemplate(context, subId) }

    private val networkCycleDataRepository by lazy {
        networkCycleDataRepositoryFactory(networkTemplate)
    }

    private val dataUsageFormatter = DataUsageFormatter(context)

    init {
        coroutineScope?.launch(Dispatchers.Default) {
            networkPolicyRepository.getNetworkPolicy(networkTemplate)?.run {
                val dataPlanInfo =
                    dataPlanRepositoryFactory(networkCycleDataRepository)
                        .getDataPlanInfo(
                            policy = this,
                            plans = proxySubscriptionManager.get().getSubscriptionPlans(subId),
                        )
                Log.d(TAG, "subId=$subId, dataPlanInfo=$dataPlanInfo")
                val format = dataUsageFormatter.formatDataUsageWithUnits(dataPlanInfo.dataPlanUse)
                val template: CharSequence? = context.getText(R.string.data_used_formatted)
                val usageText = TextUtils.expandTemplate(template, format.number, format.units)
                Log.d(TAG, "subId=$subId, usageText=$usageText")
                dataUsageDataFlow.value = DataUsageData(usageText, this, dataPlanInfo)
            }
        }
    }

    override val key: String
        get() = KEY

    override fun isAvailable(context: Context) = subInfo != null

    override fun getSummary(context: Context) = dataUsageDataFlow.value.summary

    override fun createWidget(context: Context) = DataUsageSummaryPreference(context, null)

    override fun onCreate(context: PreferenceLifecycleContext) {
        val preference = context.findPreference<Preference>(key) as DataUsageSummaryPreference?
        preference?.let {
            val isVisible = subInfo != null && dataUsageDataFlow.value.policy != null
            preference.isVisible = isVisible
            if (!isVisible) return@let
            dataUsageDataFlow.value.policy?.let { flow -> update(preference, flow) }
        }
    }

    private fun update(preference: DataUsageSummaryPreference, policy: NetworkPolicy) {
        preference.setLimitInfo(policy.getLimitInfo())
        val dataBarSize = max(policy.limitBytes, policy.warningBytes)
        if (dataBarSize > NetworkPolicy.WARNING_DISABLED) setDataBarSize(preference, dataBarSize)

        val dataPlanInfo = dataUsageDataFlow.value.dataPlanInfo ?: return
        preference.setUsageNumbers(dataPlanInfo.dataPlanUse, dataPlanInfo.dataPlanSize)
        if (dataPlanInfo.dataBarSize > 0) {
            preference.setChartEnabled(true)
            setDataBarSize(preference, dataPlanInfo.dataBarSize)
            preference.setProgress(dataPlanInfo.dataPlanUse / dataPlanInfo.dataBarSize.toFloat())
        } else {
            preference.setChartEnabled(false)
        }

        preference.setUsageInfo(
            dataPlanInfo.cycleEnd,
            dataPlanInfo.snapshotTime,
            subInfo?.carrierName,
            dataPlanInfo.dataPlanCount,
        )
    }

    private fun setDataBarSize(preference: DataUsageSummaryPreference, dataBarSize: Long) {
        preference.setLabels(
            dataUsageFormatter.formatDataUsage(/* byteValue= */ 0),
            dataUsageFormatter.formatDataUsage(dataBarSize),
        )
    }

    private fun NetworkPolicy.getLimitInfo(): CharSequence? =
        when {
            warningBytes > 0 && limitBytes > 0 -> {
                TextUtils.expandTemplate(
                    context.getText(R.string.cell_data_warning_and_limit),
                    dataUsageFormatter.formatDataUsage(warningBytes),
                    dataUsageFormatter.formatDataUsage(limitBytes),
                )
            }

            warningBytes > 0 -> {
                TextUtils.expandTemplate(
                    context.getText(R.string.cell_data_warning),
                    dataUsageFormatter.formatDataUsage(warningBytes),
                )
            }

            limitBytes > 0 -> {
                TextUtils.expandTemplate(
                    context.getText(R.string.cell_data_limit),
                    dataUsageFormatter.formatDataUsage(limitBytes),
                )
            }

            else -> null
        }

    companion object {
        private const val TAG = "MobileNetworkDataUsagePreference"
        const val KEY = "status_header"
    }
}
// LINT.ThenChange(DataUsageSummaryPreferenceController.kt)
