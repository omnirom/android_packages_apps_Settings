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

import android.content.ContextWrapper
import android.os.UserManager
import android.telephony.SubscriptionInfo
import android.telephony.SubscriptionManager
import android.telephony.SubscriptionManager.INVALID_SUBSCRIPTION_ID
import android.telephony.TelephonyManager
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub

class MobileNetworkDataTest {
    private val mockUserManager = mock<UserManager>()
    private val mockTelephonyManager = mock<TelephonyManager>()
    private val mockSubscriptionManager = mock<SubscriptionManager>()
    private val mockSubscriptionInfo = mock<SubscriptionInfo>()

    private val context =
        object : ContextWrapper(ApplicationProvider.getApplicationContext()) {
            override fun getApplicationContext() = this

            override fun getSystemService(name: String): Any? =
                when (name) {
                    getSystemServiceName(UserManager::class.java) -> mockUserManager
                    getSystemServiceName(TelephonyManager::class.java) -> mockTelephonyManager
                    getSystemServiceName(SubscriptionManager::class.java) -> mockSubscriptionManager
                    else -> super.getSystemService(name)
                }
        }

    private lateinit var mobileNetworkData: MobileNetworkData

    @Before
    fun setUp() {
        mockUserManager.stub { on { isAdminUser } doReturn true }
        mockTelephonyManager.stub {
            on { isDataCapable } doReturn true
            on { isDeviceVoiceCapable } doReturn true
        }
        mockSubscriptionInfo.stub { on { getMccString() } doReturn MCC }
        mockSubscriptionManager.stub {
            on { getActiveSubscriptionInfo(0) } doReturn mockSubscriptionInfo
            on { getPhoneNumber(0) } doReturn PHONE_NUMBER
        }
        mobileNetworkData = MobileNetworkData(context, null, 0)
    }

    @Test
    fun isMobileNetworkAvailable_byDefault_returnTrue() {
        assertThat(mobileNetworkData.isMobileNetworkAvailable()).isTrue()
    }

    @Test
    fun isMobileNetworkAvailable_isNotAdminUser_returnFalse() {
        mockUserManager.stub { on { isAdminUser } doReturn false }

        mobileNetworkData = MobileNetworkData(context, null, 0)

        assertThat(mobileNetworkData.isMobileNetworkAvailable()).isFalse()
    }

    @Test
    fun isAvailable_noDataNorVoiceCapable_returnFalse() {
        mockTelephonyManager.stub {
            on { isDataCapable } doReturn false
            on { isDeviceVoiceCapable } doReturn false
        }

        mobileNetworkData = MobileNetworkData(context, null, 0)

        assertThat(mobileNetworkData.isMobileNetworkAvailable()).isFalse()
    }

    @Test
    fun getPhoneNumber_invalidSubscriptionId_returnEmptyString() {
        mobileNetworkData = MobileNetworkData(context, null, INVALID_SUBSCRIPTION_ID)

        assertThat(mobileNetworkData.getPhoneNumber().isEmpty()).isTrue()
    }

    @Test
    fun getPhoneNumber_hasPhoneNumber_returnFormattedPhoneNumber() {
        assertThat(mobileNetworkData.getPhoneNumber()).isEqualTo(FORMATTED_PHONE_NUMBER)
    }

    companion object {
        private const val MCC = "310"
        private const val PHONE_NUMBER = "8881234567"
        private const val FORMATTED_PHONE_NUMBER = "(888) 123-4567"
    }
}
