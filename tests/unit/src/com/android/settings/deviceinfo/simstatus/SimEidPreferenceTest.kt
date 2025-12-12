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

package com.android.settings.deviceinfo.simstatus

import android.content.ContextWrapper
import android.os.UserManager
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import android.telephony.euicc.EuiccManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.R
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub

@RunWith(AndroidJUnit4::class)
class SimEidPreferenceTest {
    private val mockUserManager = mock<UserManager>()
    private val mockTelephonyManager = mock<TelephonyManager>()
    private val mockSubscriptionManager = mock<SubscriptionManager>()
    private val mockEuiccManager = mock<EuiccManager>()

    private val context =
        object : ContextWrapper(ApplicationProvider.getApplicationContext()) {
            override fun getApplicationContext() = this

            override fun getSystemService(name: String): Any? =
                when (name) {
                    getSystemServiceName(UserManager::class.java) -> mockUserManager
                    getSystemServiceName(TelephonyManager::class.java) -> mockTelephonyManager
                    getSystemServiceName(SubscriptionManager::class.java) -> mockSubscriptionManager
                    getSystemServiceName(EuiccManager::class.java) -> mockEuiccManager
                    else -> super.getSystemService(name)
                }
        }

    private lateinit var preference: SimEidPreference

    @Before
    fun setUp() {
        mockUserManager.stub { on { isAdminUser } doReturn true }
        mockTelephonyManager.stub {
            on { isDataCapable } doReturn true
            on { isDeviceVoiceCapable } doReturn true
            on { phoneCount } doReturn 0
        }
        mockEuiccManager.stub {
            on { isEnabled } doReturn true
            on { eid } doReturn EID
        }
        preference = SimEidPreference(context)
    }

    @Test
    fun getKey_returnEidInfo() {
        assertThat(preference.key).isEqualTo("eid_info")
    }

    @Test
    fun isAvailable_byDefault_returnTrue() {
        assertThat(preference.isAvailable(context)).isTrue()
    }

    @Test
    fun isAvailable_isNotAdminUser_returnFalse() {
        mockUserManager.stub { on { isAdminUser } doReturn false }

        preference = SimEidPreference(context)

        assertThat(preference.isAvailable(context)).isFalse()
    }

    @Test
    fun isAvailable_noDataNorVoiceCapable_returnFalse() {
        mockTelephonyManager.stub {
            on { isDataCapable } doReturn false
            on { isDeviceVoiceCapable } doReturn false
        }

        preference = SimEidPreference(context)

        assertThat(preference.isAvailable(context)).isFalse()
    }

    @Test
    fun getSummary_returnEid() {
        assertThat(preference.getSummary(context)).isEqualTo(EID)
    }

    @Test
    fun getTitle_returnEid() {
        assertThat(preference.getTitle(context)).isEqualTo(context.getString(R.string.status_eid))
    }

    companion object {
        const val EID = "111111111111115"
    }
}
