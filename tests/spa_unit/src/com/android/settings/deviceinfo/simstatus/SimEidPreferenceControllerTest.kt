/*
 * Copyright (C) 2023 The Android Open Source Project
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

import android.content.Context
import android.content.res.Resources
import android.os.UserManager
import android.telephony.TelephonyManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.dx.mockito.inline.extended.ExtendedMockito
import com.android.settings.R
import com.android.settings.core.BasePreferenceController
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.spy
import org.mockito.kotlin.stub
import org.mockito.kotlin.whenever

@RunWith(AndroidJUnit4::class)
class SimEidPreferenceControllerTest {

    private val mockUserManager = mock<UserManager>()
    private val mockTelephonyManager = mock<TelephonyManager>()
    private val mockResources = mock<Resources>()

    private val context: Context =
        spy(ApplicationProvider.getApplicationContext()) {
            on { getSystemService(UserManager::class.java) } doReturn mockUserManager
            on { getSystemService(TelephonyManager::class.java) } doReturn mockTelephonyManager
	    on { getSystemService(Context.TELEPHONY_SERVICE) } doReturn mockTelephonyManager
            on { resources } doReturn mockResources
        }

    private val controller = SimEidPreferenceController(context, TEST_KEY)

    @Before
    fun setUp() {
        // By default, available
        mockTelephonyManager.stub {
            on { isDataCapable } doReturn true
            on { isDeviceVoiceCapable } doReturn true
        }
        mockResources.stub {
            on { getBoolean(R.bool.config_show_sim_info) } doReturn true
        }
        mockUserManager.stub {
            on { isAdminUser } doReturn true
        }
    }

    @Test
    fun getAvailabilityStatus_default_displayed() {
        // Use defaults from setup()
        val availabilityStatus = controller.availabilityStatus
        assertThat(availabilityStatus).isEqualTo(BasePreferenceController.AVAILABLE)
    }

    @Test
    fun getAvailabilityStatus_noShowSimInfo_notDisplayed() {
        mockResources.stub {
            on { getBoolean(R.bool.config_show_sim_info) } doReturn false
        }

        val availabilityStatus = controller.availabilityStatus
        assertThat(availabilityStatus).isEqualTo(BasePreferenceController.UNSUPPORTED_ON_DEVICE)
    }

    @Test
    fun getAvailabilityStatus_voiceCapable_notDataCapable_displayed() {
        mockTelephonyManager.stub {
            on { isDeviceVoiceCapable } doReturn true
            on { isDataCapable } doReturn false
        }

        val availabilityStatus = controller.availabilityStatus
        assertThat(availabilityStatus).isEqualTo(BasePreferenceController.AVAILABLE)
    }

    @Test
    fun getAvailabilityStatus_notVoiceCapable_dataCapable_displayed() {
        mockTelephonyManager.stub {
            on { isDeviceVoiceCapable } doReturn false
            on { isDataCapable } doReturn true
        }

        val availabilityStatus = controller.availabilityStatus
        assertThat(availabilityStatus).isEqualTo(BasePreferenceController.AVAILABLE)
    }

    @Test
    fun getAvailabilityStatus_notVoiceCapable_notDataCapable_notDisplayed() {
        mockTelephonyManager.stub {
            on { isDeviceVoiceCapable } doReturn false
            on { isDataCapable } doReturn false
        }

        val availabilityStatus = controller.availabilityStatus
        assertThat(availabilityStatus).isEqualTo(BasePreferenceController.UNSUPPORTED_ON_DEVICE)
    }

    @Test
    fun getAvailabilityStatus_noUserAdmin_notDisplayed() {
        mockUserManager.stub {
            on { isAdminUser } doReturn false
        }

        val availabilityStatus = controller.availabilityStatus
        assertThat(availabilityStatus).isEqualTo(BasePreferenceController.DISABLED_FOR_USER)
    }

    private companion object {
        const val TEST_KEY = "test_key"
    }
}
