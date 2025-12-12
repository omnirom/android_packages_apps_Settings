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

import android.app.usage.NetworkStats
import android.app.usage.NetworkStatsManager
import android.content.ContextWrapper
import android.content.pm.PackageManager
import android.content.pm.PackageManager.FEATURE_WIFI
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub

@RunWith(AndroidJUnit4::class)
class WifiDataUsagePreferenceTest {

    private val mockPackageManager = mock<PackageManager>()
    private val mockNetworkStatsManager = mock<NetworkStatsManager>()
    private val mockBucket = mock<NetworkStats.Bucket>()

    private val context =
        object : ContextWrapper(ApplicationProvider.getApplicationContext()) {
            override fun getPackageManager() = mockPackageManager

            override fun getSystemService(name: String): Any? =
                when (name) {
                    getSystemServiceName(NetworkStatsManager::class.java) -> mockNetworkStatsManager
                    else -> super.getSystemService(name)
                }
        }

    @Before
    fun setUp() {
        mockPackageManager.stub { on { hasSystemFeature(FEATURE_WIFI) } doReturn true }
        mockBucket.stub {
            on { rxBytes } doReturn 1
            on { txBytes } doReturn 1
        }
        mockNetworkStatsManager.stub {
            on { querySummaryForDevice(any(), any(), any()) } doReturn mockBucket
        }
    }

    @Test
    fun getTitle_notNull() {
        val preference = WifiDataUsagePreference(context)

        assertThat(preference.title).isNotNull()
    }

    @Test
    fun getSummary_notNull() {
        val preference = WifiDataUsagePreference(context)

        assertThat(preference.summary).isNotNull()
    }

    @Test
    fun isAvailable_hasFeatureWifi_returnTrue() {
        mockPackageManager.stub { on { hasSystemFeature(FEATURE_WIFI) } doReturn true }
        val preference = WifiDataUsagePreference(context)

        assertThat(preference.isAvailable(context)).isTrue()
    }

    @Test
    fun isAvailable_noFeatureWifi_returnFalse() {
        mockPackageManager.stub { on { hasSystemFeature(FEATURE_WIFI) } doReturn false }
        val preference = WifiDataUsagePreference(context)

        assertThat(preference.isAvailable(context)).isFalse()
    }

    @Test
    fun isEnabled_hasHistoricalUsageLevel_returnTrue() {
        mockBucket.stub {
            on { rxBytes } doReturn 1
            on { txBytes } doReturn 1
        }
        mockNetworkStatsManager.stub {
            on { querySummaryForDevice(any(), any(), any()) } doReturn mockBucket
        }
        val preference = WifiDataUsagePreference(context)

        assertThat(preference.isEnabled(context)).isTrue()
    }

    @Test
    fun isEnabled_noHistoricalUsageLevel_returnFalse() {
        mockBucket.stub {
            on { rxBytes } doReturn 0
            on { txBytes } doReturn 0
        }
        mockNetworkStatsManager.stub {
            on { querySummaryForDevice(any(), any(), any()) } doReturn mockBucket
        }
        val preference = WifiDataUsagePreference(context)

        assertThat(preference.isEnabled(context)).isFalse()
    }

    @Test
    fun intent_notNull() {
        val preference = WifiDataUsagePreference(context)

        assertThat(preference.intent(context)).isNotNull()
    }
}