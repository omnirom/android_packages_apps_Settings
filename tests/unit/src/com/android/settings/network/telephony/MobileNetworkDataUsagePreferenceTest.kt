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
import android.telephony.SubscriptionInfo
import android.telephony.SubscriptionManager
import android.telephony.SubscriptionPlan
import android.telephony.TelephonyManager
import android.util.Range
import androidx.test.core.app.ApplicationProvider
import com.android.settings.datausage.DataPlanInfo
import com.android.settings.datausage.DataPlanRepository
import com.android.settings.datausage.lib.INetworkCycleDataRepository
import com.android.settings.datausage.lib.NetworkUsageData
import com.android.settings.network.ProxySubscriptionManager
import com.android.settings.network.policy.NetworkPolicyRepository
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.spy
import org.mockito.kotlin.stub

class MobileNetworkDataUsagePreferenceTest {
    private val mockTelephonyManager = mock<TelephonyManager> { on { isDataCapable } doReturn true }
    private val context: Context =
        spy(ApplicationProvider.getApplicationContext()) {
            on { getSystemService(TelephonyManager::class.java) } doReturn mockTelephonyManager
        }
    private var policy: NetworkPolicy? = mock<NetworkPolicy>()
    private val mockSubscriptionManager =
        mock<SubscriptionManager> { on { getSubscriptionPlans(any()) } doReturn emptyList() }
    private val mockProxySubscriptionManager =
        mock<ProxySubscriptionManager> { on { get() } doReturn mockSubscriptionManager }
    private val mockNetworkPolicyRepository = mock<NetworkPolicyRepository>()

    private val fakeNetworkCycleDataRepository =
        object : INetworkCycleDataRepository {
            override fun getCycles(): List<Range<Long>> = emptyList()

            override fun getPolicy() = policy

            override fun queryUsage(range: Range<Long>) = NetworkUsageData.AllZero
        }

    private var dataPlanInfo = EMPTY_DATA_PLAN_INFO

    private val fakeDataPlanRepository =
        object : DataPlanRepository {
            override fun getDataPlanInfo(policy: NetworkPolicy, plans: List<SubscriptionPlan>) =
                dataPlanInfo
        }

    private val preference =
        MobileNetworkDataUsagePreference(
            context = context,
            subId = SUB_ID,
            coroutineScope = null,
            proxySubscriptionManager = mockProxySubscriptionManager,
            networkPolicyRepository = mockNetworkPolicyRepository,
            networkCycleDataRepositoryFactory = { fakeNetworkCycleDataRepository },
            dataPlanRepositoryFactory = { fakeDataPlanRepository },
        )

    @Test
    fun isAvailable_noDataCapable_returnFalse() {
        mockTelephonyManager.stub { on { isDataCapable } doReturn false }

        assertThat(preference.isAvailable(context)).isFalse()
    }

    @Test
    fun isAvailable_noSubInfo_conditionallyUnavailable() {
        mockProxySubscriptionManager.stub {
            on { getAccessibleSubscriptionInfo(SUB_ID) } doReturn null
        }

        assertThat(preference.isAvailable(context)).isFalse()
    }

    @Test
    fun isAvailable_hasSubInfo_returnTrue() {
        mockProxySubscriptionManager.stub {
            on { getAccessibleSubscriptionInfo(SUB_ID) } doReturn SubscriptionInfo.Builder().build()
        }

        assertThat(preference.isAvailable(context)).isTrue()
    }

    @Test
    fun getSummary_hasDataUsage_returnSummary() {
        preference.dataUsageDataFlow.value =
            MobileNetworkDataUsagePreference.DataUsageData(summary = SUMMARY)

        assertThat(preference.getSummary(context)).isEqualTo(SUMMARY)
    }

    private companion object {
        const val SUB_ID = 1234
        const val SUMMARY = "12.34 MB"
        val EMPTY_DATA_PLAN_INFO =
            DataPlanInfo(
                dataPlanCount = 0,
                dataPlanSize = SubscriptionPlan.BYTES_UNKNOWN,
                dataBarSize = SubscriptionPlan.BYTES_UNKNOWN,
                dataPlanUse = 0,
                cycleEnd = null,
                snapshotTime = SubscriptionPlan.TIME_UNKNOWN,
            )
    }
}
