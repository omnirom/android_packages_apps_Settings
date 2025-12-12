/*
 * Copyright (C) 2024 The Android Open Source Project
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
import android.content.res.Resources
import android.os.UserManager
import android.telephony.TelephonyManager
import androidx.lifecycle.testing.TestLifecycleOwner
import androidx.preference.Preference
import androidx.preference.PreferenceManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.dx.mockito.inline.extended.ExtendedMockito
import com.android.settings.R
import com.android.settings.core.BasePreferenceController
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
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
class MobileNetworkPhoneNumberPreferenceControllerTest {

    private val mockTelephonyManager = mock<TelephonyManager>()
    private val mockUserManager = mock<UserManager>()
    private val mockSubscriptionRepository = mock<SubscriptionRepository>()

    private val context: Context =
        spy(ApplicationProvider.getApplicationContext()) {
            on { getSystemService(TelephonyManager::class.java) } doReturn mockTelephonyManager
            on { getSystemService(Context.TELEPHONY_SERVICE) } doReturn mockTelephonyManager
            on { getSystemService(UserManager::class.java) } doReturn mockUserManager
        }

    private val spyResources = spy(context.resources)

    private val controller =
        MobileNetworkPhoneNumberPreferenceController(context, TEST_KEY, mockSubscriptionRepository)
    private val preference = Preference(context).apply { key = TEST_KEY }
    private val preferenceScreen = PreferenceManager(context).createPreferenceScreen(context)

    @Before
    fun setUp() {
        context.stub { on { resources } doReturn spyResources }

        // By default, available
        spyResources.stub {
            on { getBoolean(R.bool.config_show_sim_info) } doReturn true
        }
        mockTelephonyManager.stub {
            on { isDataCapable } doReturn true
            on { isDeviceVoiceCapable } doReturn true
        }
        mockUserManager.stub {
            on { isAdminUser } doReturn true
        }

        preferenceScreen.addPreference(preference)
        controller.init(SUB_ID)
        controller.displayPreference(preferenceScreen)
    }

    @Test
    fun onViewCreated_cannotGetPhoneNumber_displayUnknown() = runBlocking {
        mockSubscriptionRepository.stub {
            on { phoneNumberFlow(SUB_ID) } doReturn flowOf(null)
        }

        controller.onViewCreated(TestLifecycleOwner())
        delay(100)

        assertThat(preference.summary).isEqualTo(context.getString(R.string.device_info_default))
    }

    @Test
    fun onViewCreated_canGetPhoneNumber_displayPhoneNumber() = runBlocking {
        mockSubscriptionRepository.stub {
            on { phoneNumberFlow(SUB_ID) } doReturn flowOf(PHONE_NUMBER)
        }

        controller.onViewCreated(TestLifecycleOwner())
        delay(100)

        assertThat(preference.summary).isEqualTo(PHONE_NUMBER)
    }

    @Test
    fun getAvailabilityStatus_default_displayed() {
        // Use defaults from setup()
        val availabilityStatus = controller.availabilityStatus
        assertThat(availabilityStatus).isEqualTo(BasePreferenceController.AVAILABLE)
    }

    @Test
    fun getAvailabilityStatus_notShowSimInfo_notDisplayed() {
        spyResources.stub {
            on { getBoolean(R.bool.config_show_sim_info) } doReturn false
        }

        val availabilityStatus = controller.availabilityStatus
        assertThat(availabilityStatus).isEqualTo(BasePreferenceController.UNSUPPORTED_ON_DEVICE)
    }

    @Test
    fun getAvailabilityStatus_notVoiceCapable_notDataCapable_notDisplayed() {
        mockTelephonyManager.stub {
            on { isDataCapable } doReturn false
            on { isDeviceVoiceCapable } doReturn false
        }

        val availabilityStatus = controller.availabilityStatus
        assertThat(availabilityStatus).isEqualTo(BasePreferenceController.UNSUPPORTED_ON_DEVICE)
    }

    @Test
    fun getAvailabilityStatus_voiceCapable_notDataCapable_displayed() {
        mockTelephonyManager.stub {
            on { isDataCapable } doReturn false
            on { isDeviceVoiceCapable } doReturn true
        }

        val availabilityStatus = controller.availabilityStatus
        assertThat(availabilityStatus).isEqualTo(BasePreferenceController.AVAILABLE)
    }

    @Test
    fun getAvailabilityStatus_notVoiceCapable_dataCapable_displayed() {
        mockTelephonyManager.stub {
            on { isDataCapable } doReturn true
            on { isDeviceVoiceCapable } doReturn false
        }

        val availabilityStatus = controller.availabilityStatus
        assertThat(availabilityStatus).isEqualTo(BasePreferenceController.AVAILABLE)
    }

    @Test
    fun getAvailabilityStatus_notUserAdmin_notDisplayed() {
        mockUserManager.stub {
            on { isAdminUser } doReturn false
        }

        val availabilityStatus = controller.availabilityStatus
        assertThat(availabilityStatus).isEqualTo(BasePreferenceController.DISABLED_FOR_USER)
    }

    private companion object {
        const val TEST_KEY = "test_key"
        const val SUB_ID = 10
        const val PHONE_NUMBER = "1234567890"
    }
}
