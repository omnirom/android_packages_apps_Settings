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

import android.content.Context
import android.telephony.SubscriptionInfo
import android.telephony.SubscriptionManager
import androidx.test.core.app.ApplicationProvider
import com.android.dx.mockito.inline.extended.ExtendedMockito
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.MockitoSession
import org.mockito.kotlin.mock
import org.mockito.kotlin.spy
import org.mockito.kotlin.whenever
import org.mockito.quality.Strictness

class SimMobileDataPreferenceTest {
    private lateinit var mockSession: MockitoSession

    private val subInfo = mock<SubscriptionInfo>()

    private val mockSubscriptionManager =
        mock<SubscriptionManager>() {
            on { getActiveSubscriptionInfo(TEST_SUB_ID) }.thenReturn(subInfo)
        }
    private val context: Context =
        spy(ApplicationProvider.getApplicationContext()) {
            on { getSystemService(SubscriptionManager::class.java) }
                .thenReturn(mockSubscriptionManager)
        }

    private val preference = SimMobileDataPreference()

    @Before
    fun setUp() {
        mockSession =
            ExtendedMockito.mockitoSession()
                .initMocks(this)
                .mockStatic(SubscriptionManager::class.java)
                .strictness(Strictness.LENIENT)
                .startMocking()
    }

    @After
    fun tearDown() {
        mockSession.finishMocking()
    }

    @Test
    fun getSummary_returnSubDisplayName() {
        whenever(SubscriptionManager.getDefaultDataSubscriptionId()).thenReturn(TEST_SUB_ID)
        whenever(subInfo.displayName).thenReturn(TEST_DISPLAY_NAME)

        val result = preference.getSummary(context)

        assertThat(result).isEqualTo(TEST_DISPLAY_NAME)
    }

    @Test
    fun getSummary_noSubInfo_returnSubDisplayName() {
        whenever(SubscriptionManager.getDefaultDataSubscriptionId()).thenReturn(TEST_SUB_ID)
        whenever(mockSubscriptionManager.getActiveSubscriptionInfo(TEST_SUB_ID)).thenReturn(null)

        val result = preference.getSummary(context)

        assertThat(result).isEqualTo("")
    }

    companion object {
        const val TEST_SUB_ID = 10
        const val TEST_DISPLAY_NAME = "test_display_name"
    }
}
