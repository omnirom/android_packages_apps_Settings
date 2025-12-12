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
import android.platform.test.flag.junit.SetFlagsRule
import android.telephony.SubscriptionInfo
import android.telephony.TelephonyManager
import android.telephony.euicc.EuiccManager
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.dx.mockito.inline.extended.ExtendedMockito
import com.android.settings.R
import com.android.settings.core.BasePreferenceController
import com.android.settings.flags.Flags
import com.android.settings.network.SubscriptionInfoListViewModel
import com.android.settings.network.SubscriptionUtil
import com.android.settingslib.CustomDialogPreferenceCompat
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.MockitoSession
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.spy
import org.mockito.kotlin.stub
import org.mockito.kotlin.whenever
import org.mockito.quality.Strictness

@RunWith(AndroidJUnit4::class)
class MobileNetworkEidPreferenceControllerTest {
    @get:Rule val setFlagsRule: SetFlagsRule = SetFlagsRule()

    private lateinit var mockSession: MockitoSession

    private val mockViewModels =  mock<Lazy<SubscriptionInfoListViewModel>>()
    private val mockFragment = mock<Fragment>{
        val viewmodel = mockViewModels
    }

    private val mockUserManager = mock<UserManager>()

    private var mockEid = String()
    private val mockTelephonyManager = mock<TelephonyManager> {
        on {uiccCardsInfo} doReturn listOf()
        on { createForSubscriptionId(any()) } doReturn mock
    }
    private val mockEuiccManager = mock<EuiccManager> {
        on {isEnabled} doReturn true
        on {eid} doReturn mockEid
    }
    private val context: Context = spy(ApplicationProvider.getApplicationContext()) {
        on { getSystemService(TelephonyManager::class.java) } doReturn mockTelephonyManager
        on { getSystemService(Context.TELEPHONY_SERVICE) } doReturn mockTelephonyManager
        on { getSystemService(EuiccManager::class.java) } doReturn mockEuiccManager
        on { getSystemService(UserManager::class.java) } doReturn mockUserManager
    }

    private val spyResources = spy(context.resources)

    private val controller = MobileNetworkEidPreferenceController(context, TEST_KEY)
    private val preference = CustomDialogPreferenceCompat(context).apply { key = TEST_KEY }
    private val preferenceScreen = PreferenceManager(context).createPreferenceScreen(context)

    @Before
    fun setUp() {
        mockSession = ExtendedMockito.mockitoSession()
            .initMocks(this)
            .mockStatic(SubscriptionUtil::class.java)
            .strictness(Strictness.LENIENT)
            .startMocking()

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
        controller.displayPreference(preferenceScreen)
    }

    @After
    fun tearDown() {
        mockSession.finishMocking()
    }

    fun initControllerWithSubscriptions() {
        whenever(SubscriptionUtil.getActiveSubscriptions(any())).thenReturn(
            listOf(
                SUB_INFO_1,
                SUB_INFO_2
            )
        )
        var mockSubId = 2
        controller.init(mockFragment, mockSubId)
    }

    suspend fun initControllerWithSubscriptionsAndValidEid() {
        initControllerWithSubscriptions()
        mockEid = "test eid"
        mockEuiccManager.stub {
            on {eid} doReturn mockEid
        }
        controller.refreshData(SUB_INFO_2)
    }

    @Test
    fun refreshData_getEmptyEid_preferenceIsNotVisible() = runBlocking {
        initControllerWithSubscriptions()
        // empty EID
        mockEid = String()
        controller.refreshData(SUB_INFO_2)

        assertThat(preference.isVisible).isEqualTo(false)
    }

    @Test
    fun refreshData_getNotEmptyEid_preferenceSummaryIsExpected() = runBlocking {
        initControllerWithSubscriptionsAndValidEid()

        assertThat(preference.summary).isEqualTo(mockEid)
    }

    @Test
    fun getAvailabilityStatus_default() = runBlocking {
        initControllerWithSubscriptionsAndValidEid();

        val availabilityStatus = controller.availabilityStatus

        assertThat(availabilityStatus).isEqualTo(BasePreferenceController.AVAILABLE)
    }

    @Test
    fun getAvailabilityStatus_notShowSimInfo() = runBlocking {
        initControllerWithSubscriptionsAndValidEid();
        spyResources.stub {
            on { getBoolean(R.bool.config_show_sim_info) } doReturn false
        }

        val availabilityStatus = controller.availabilityStatus

        assertThat(availabilityStatus).isEqualTo(BasePreferenceController.UNSUPPORTED_ON_DEVICE)
    }

    @Test
    fun getAvailabilityStatus_notDataCapable_notVoiceCapable() = runBlocking {
        initControllerWithSubscriptionsAndValidEid();
        mockTelephonyManager.stub {
            on { isDataCapable } doReturn false
            on { isDeviceVoiceCapable } doReturn false
        }

        val availabilityStatus = controller.availabilityStatus

        assertThat(availabilityStatus).isEqualTo(BasePreferenceController.UNSUPPORTED_ON_DEVICE)
    }

    @Test
    fun getAvailabilityStatus_dataCapable_notVoiceCapable() = runBlocking {
        initControllerWithSubscriptionsAndValidEid();
        mockTelephonyManager.stub {
            on { isDataCapable } doReturn true
            on { isDeviceVoiceCapable } doReturn false
        }

        val availabilityStatus = controller.availabilityStatus

        assertThat(availabilityStatus).isEqualTo(BasePreferenceController.AVAILABLE)
    }

    @Test
    fun getAvailabilityStatus_notDataCapable_voiceCapable() = runBlocking {
        initControllerWithSubscriptionsAndValidEid();
        mockTelephonyManager.stub {
            on { isDataCapable } doReturn false
            on { isDeviceVoiceCapable } doReturn true
        }

        val availabilityStatus = controller.availabilityStatus

        assertThat(availabilityStatus).isEqualTo(BasePreferenceController.AVAILABLE)
    }

    @Test
    fun getAvailabilityStatus_notAdmin() = runBlocking {
        initControllerWithSubscriptionsAndValidEid();
        mockUserManager.stub {
            on { isAdminUser } doReturn false
        }

        val availabilityStatus = controller.availabilityStatus

        assertThat(availabilityStatus).isEqualTo(BasePreferenceController.DISABLED_FOR_USER)
    }

    private companion object {
        const val TEST_KEY = "test_key"
        const val DISPLAY_NAME_1 = "Sub 1"
        const val DISPLAY_NAME_2 = "Sub 2"

        val SUB_INFO_1: SubscriptionInfo = SubscriptionInfo.Builder().apply {
            setId(1)
            setDisplayName(DISPLAY_NAME_1)
        }.build()

        val SUB_INFO_2: SubscriptionInfo = SubscriptionInfo.Builder().apply {
            setId(2)
            setDisplayName(DISPLAY_NAME_2)
        }.build()

    }
}
