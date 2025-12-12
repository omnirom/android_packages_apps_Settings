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

package com.android.settings.deviceinfo.imei

import android.content.ContextWrapper
import android.os.UserManager
import android.telephony.TelephonyManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub

@RunWith(AndroidJUnit4::class)
class ImeiPreferenceTest {
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

    private lateinit var preference: ImeiPreference

    @Before
    fun setUp() {
        mockUserManager.stub { on { isAdminUser } doReturn true }
        mockTelephonyManager.stub {
            on { isDataCapable } doReturn true
            on { isDeviceVoiceCapable } doReturn true
            on { getImei(0) } doReturn IMEI_1
            on { getImei(1) } doReturn IMEI_2
        }
        preference = ImeiPreference(context, 0, 1)
    }

    @Test
    fun init_getPrimaryImeiThrowException_doNotCrash() {
        mockTelephonyManager.stub {
            on { primaryImei } doThrow(IllegalStateException())
        }

        preference = ImeiPreference(context, 0, 2)
    }

    @Test
    fun getKey_slotIndex0_returnImeiWithIndex() {
        assertThat(preference.key).isEqualTo(ImeiPreference.KEY_PREFIX + "1")
    }

    @Test
    fun getKey_slotIndex1_returnImeiWithIndex() {
        preference = ImeiPreference(context, 1, 2);

        assertThat(preference.key).isEqualTo(ImeiPreference.KEY_PREFIX + "2")
    }

    @Test
    fun isAvailable_byDefault_returnTrue() {
        assertThat(preference.isAvailable(context)).isTrue()
    }

    @Test
    fun isAvailable_isNotAdminUser_returnFalse() {
        mockUserManager.stub { on { isAdminUser } doReturn false }

        preference = ImeiPreference(context, 0, 1)

        assertThat(preference.isAvailable(context)).isFalse()
    }

    @Test
    fun isAvailable_noDataNorVoiceCapable_returnFalse() {
        mockTelephonyManager.stub {
            on { isDataCapable } doReturn false
            on { isDeviceVoiceCapable } doReturn false
        }

        preference = ImeiPreference(context, 0, 1)

        assertThat(preference.isAvailable(context)).isFalse()
    }

    @Test
    fun getSummary_index0_returnImei1() {
        assertThat(preference.getSummary(context)).isEqualTo(IMEI_1)
    }

    @Test
    fun getSummary_index1_returnImei2() {
        preference = ImeiPreference(context, 1, 2);

        assertThat(preference.getSummary(context)).isEqualTo(IMEI_2)
    }

    companion object {
        const val IMEI_1 = "111111111111115"
        const val IMEI_2 = "222222222222225"
    }
}