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
import android.telephony.TelephonyManager
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub

class MobileNetworkSpnPreferenceTest {
    private val mockUserManager = mock<UserManager>()
    private val mockTelephonyManager = mock<TelephonyManager>()

    private val context =
        object : ContextWrapper(ApplicationProvider.getApplicationContext()) {
            override fun getApplicationContext() = this

            override fun getSystemService(name: String): Any? =
                when (name) {
                    getSystemServiceName(UserManager::class.java) -> mockUserManager
                    getSystemServiceName(TelephonyManager::class.java) -> mockTelephonyManager
                    else -> super.getSystemService(name)
                }
        }

    private lateinit var preference: MobileNetworkSpnPreference

    @Before
    fun setUp() {
        mockUserManager.stub { on { isAdminUser } doReturn true }
        mockTelephonyManager.stub {
            on { isDataCapable } doReturn true
            on { isDeviceVoiceCapable } doReturn true
        }
        preference = MobileNetworkSpnPreference(context, 0)
    }

    @Test
    fun isAvailable_byDefault_returnTrue() {
        assertThat(preference.isAvailable(context)).isTrue()
    }

    @Test
    fun isAvailable_isNotAdminUser_returnFalse() {
        mockUserManager.stub { on { isAdminUser } doReturn false }

        preference = MobileNetworkSpnPreference(context, 0)

        assertThat(preference.isAvailable(context)).isFalse()
    }

    @Test
    fun isAvailable_noDataNorVoiceCapable_returnFalse() {
        mockTelephonyManager.stub {
            on { isDataCapable } doReturn false
            on { isDeviceVoiceCapable } doReturn false
        }

        preference = MobileNetworkSpnPreference(context, 0)

        assertThat(preference.isAvailable(context)).isFalse()
    }

    // ToDo: Add test cases for getSummary.
}
