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

package com.android.settings.spa.network

import android.content.Context
import android.telephony.SubscriptionInfo
import android.telephony.SubscriptionManager
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.R
import com.android.settings.network.SimOnboardingService
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.spy
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify

@RunWith(AndroidJUnit4::class)
class SimOnboardingPrimarySimTest {
    @get:Rule val composeTestRule = createComposeRule()

    private var mockSimOnboardingService =
        spy(SimOnboardingService()) {
            on { targetSubId }.doReturn(SUB_INFO_3.subscriptionId)
            on { targetSubInfo }.doReturn(SUB_INFO_3)
            on { uiccCardInfoList }.doReturn(listOf())

            on { activeSubInfoList }.doReturn(listOf(SUB_INFO_1, SUB_INFO_2))
            on { availableSubInfoList }.doReturn(listOf(SUB_INFO_1, SUB_INFO_2, SUB_INFO_3))
            on { userSelectedSubInfoList }.doReturn(mutableListOf(SUB_INFO_1, SUB_INFO_2))
            on { getSelectableSubscriptionInfoList() }
                .doReturn(listOf(SUB_INFO_1, SUB_INFO_2, SUB_INFO_3))
            on { getSelectedSubscriptionInfoListWithRenaming() }.doReturn(listOf())
            on { targetPrimarySimAutoDataSwitch }.doReturn(MutableStateFlow(false))
        }

    private val mockSubscriptionManager =
        mock<SubscriptionManager> {
            on { addOnSubscriptionsChangedListener(any(), any()) } doAnswer
                {
                    val listener =
                        it.arguments[1] as SubscriptionManager.OnSubscriptionsChangedListener
                    listener.onSubscriptionsChanged()
                }
            on { getPhoneNumber(SUB_ID_1) } doReturn NUMBER_1
            on { getPhoneNumber(SUB_ID_2) } doReturn NUMBER_2
        }
    private val context: Context =
        spy(ApplicationProvider.getApplicationContext()) {
            on { getSystemService(SubscriptionManager::class.java) } doReturn
                mockSubscriptionManager
        }

    private val nextAction: () -> Unit = mock()
    private val cancelAction: () -> Unit = mock()

    @Test
    fun simOnboardingPrimarySimImpl_showTitle() {
        composeTestRule.setContent {
            SimOnboardingPrimarySimImpl(nextAction, cancelAction, mockSimOnboardingService)
        }

        composeTestRule
            .onNodeWithText(context.getString(R.string.sim_onboarding_primary_sim_title))
            .assertIsDisplayed()
    }

    @Test
    fun simOnboardingPrimarySimImpl_showSubTitle() {
        composeTestRule.setContent {
            SimOnboardingPrimarySimImpl(nextAction, cancelAction, mockSimOnboardingService)
        }

        composeTestRule
            .onNodeWithText(context.getString(R.string.sim_onboarding_primary_sim_msg))
            .assertIsDisplayed()
    }

    @Test
    fun simOnboardingPrimarySimImpl_clickCancelAction_verifyCancelAction() {
        composeTestRule.setContent {
            SimOnboardingPrimarySimImpl(nextAction, cancelAction, mockSimOnboardingService)
        }

        composeTestRule.onNodeWithText(context.getString(R.string.cancel)).performClick()

        verify(cancelAction)
    }

    @Test
    fun simOnboardingPrimarySimImpl_showVoiceItem() {
        mockSimOnboardingService.stub {
            on { getSelectedSubscriptionInfoListWithRenaming() }
                .doReturn(listOf(SUB_INFO_1, SUB_INFO_3))
        }

        composeTestRule.setContent {
            SimOnboardingPrimarySimImpl(nextAction, cancelAction, mockSimOnboardingService)
        }

        composeTestRule
            .onNodeWithText(context.getString(R.string.primary_sim_calls_title))
            .assertIsDisplayed()
    }

    @Test
    fun simOnboardingPrimarySimImpl_showDefaultVoice() {
        mockSimOnboardingService.stub {
            on { getSelectedSubscriptionInfoListWithRenaming() }
                .doReturn(listOf(SUB_INFO_1, SUB_INFO_3))
        }
        mockSimOnboardingService.targetPrimarySimCalls = SUB_INFO_2.subscriptionId
        composeTestRule.setContent {
            SimOnboardingPrimarySimImpl(nextAction, cancelAction, mockSimOnboardingService)
        }

        assertThat(mockSimOnboardingService.targetPrimarySimCalls)
            .isEqualTo(SUB_INFO_1.subscriptionId)
    }

    @Test
    fun simOnboardingPrimarySimImpl_showSelectedVoice() {
        mockSimOnboardingService.stub {
            on { getSelectedSubscriptionInfoListWithRenaming() }
                .doReturn(listOf(SUB_INFO_1, SUB_INFO_3))
        }
        mockSimOnboardingService.targetPrimarySimCalls = SUB_INFO_3.subscriptionId
        composeTestRule.setContent {
            SimOnboardingPrimarySimImpl(nextAction, cancelAction, mockSimOnboardingService)
        }

        assertThat(mockSimOnboardingService.targetPrimarySimCalls)
            .isEqualTo(SUB_INFO_3.subscriptionId)
    }

    @Test
    fun simOnboardingPrimarySimImpl_showSmsItem() {
        mockSimOnboardingService.stub {
            on { getSelectedSubscriptionInfoListWithRenaming() }
                .doReturn(listOf(SUB_INFO_1, SUB_INFO_3))
        }
        composeTestRule.setContent {
            SimOnboardingPrimarySimImpl(nextAction, cancelAction, mockSimOnboardingService)
        }

        composeTestRule
            .onNodeWithText(context.getString(R.string.primary_sim_texts_title))
            .assertIsDisplayed()
    }

    @Test
    fun simOnboardingPrimarySimImpl_showDefaultSms() {
        mockSimOnboardingService.stub {
            on { getSelectedSubscriptionInfoListWithRenaming() }
                .doReturn(listOf(SUB_INFO_1, SUB_INFO_3))
        }
        mockSimOnboardingService.targetPrimarySimTexts = SUB_INFO_2.subscriptionId
        composeTestRule.setContent {
            SimOnboardingPrimarySimImpl(nextAction, cancelAction, mockSimOnboardingService)
        }

        assertThat(mockSimOnboardingService.targetPrimarySimTexts)
            .isEqualTo(SUB_INFO_1.subscriptionId)
    }

    @Test
    fun simOnboardingPrimarySimImpl_showSelectedSms() {
        mockSimOnboardingService.stub {
            on { getSelectedSubscriptionInfoListWithRenaming() }
                .doReturn(listOf(SUB_INFO_1, SUB_INFO_3))
        }
        mockSimOnboardingService.targetPrimarySimTexts = SUB_INFO_3.subscriptionId
        composeTestRule.setContent {
            SimOnboardingPrimarySimImpl(nextAction, cancelAction, mockSimOnboardingService)
        }

        assertThat(mockSimOnboardingService.targetPrimarySimTexts)
            .isEqualTo(SUB_INFO_3.subscriptionId)
    }

    @Test
    fun simOnboardingPrimarySimImpl_showDataItem() {
        mockSimOnboardingService.stub {
            on { getSelectedSubscriptionInfoListWithRenaming() }
                .doReturn(listOf(SUB_INFO_1, SUB_INFO_3))
        }
        composeTestRule.setContent {
            SimOnboardingPrimarySimImpl(nextAction, cancelAction, mockSimOnboardingService)
        }

        composeTestRule
            .onNodeWithText(context.getString(R.string.mobile_data_settings_title))
            .assertIsDisplayed()
    }

    @Test
    fun simOnboardingPrimarySimImpl_showDefaultData() {
        mockSimOnboardingService.stub {
            on { getSelectedSubscriptionInfoListWithRenaming() }
                .doReturn(listOf(SUB_INFO_1, SUB_INFO_3))
        }
        mockSimOnboardingService.targetPrimarySimMobileData = SUB_INFO_2.subscriptionId
        composeTestRule.setContent {
            SimOnboardingPrimarySimImpl(nextAction, cancelAction, mockSimOnboardingService)
        }

        assertThat(mockSimOnboardingService.targetPrimarySimMobileData)
            .isEqualTo(SUB_INFO_1.subscriptionId)
    }

    @Test
    fun simOnboardingPrimarySimImpl_showSelectedData() {
        mockSimOnboardingService.stub {
            on { getSelectedSubscriptionInfoListWithRenaming() }
                .doReturn(listOf(SUB_INFO_1, SUB_INFO_3))
        }
        mockSimOnboardingService.targetPrimarySimMobileData = SUB_INFO_3.subscriptionId
        composeTestRule.setContent {
            SimOnboardingPrimarySimImpl(nextAction, cancelAction, mockSimOnboardingService)
        }

        assertThat(mockSimOnboardingService.targetPrimarySimMobileData)
            .isEqualTo(SUB_INFO_3.subscriptionId)
    }

    private companion object {
        const val SUB_ID_1 = 1
        const val SUB_ID_2 = 2
        const val SUB_ID_3 = 3
        const val DISPLAY_NAME_1 = "Sub 1"
        const val DISPLAY_NAME_2 = "Sub 2"
        const val DISPLAY_NAME_3 = "Sub 3"
        const val NUMBER_1 = "000000001"
        const val NUMBER_2 = "000000002"
        const val NUMBER_3 = "000000003"
        const val PRIMARY_SIM_ASK_EVERY_TIME = -1

        val SUB_INFO_1: SubscriptionInfo =
            SubscriptionInfo.Builder()
                .apply {
                    setId(SUB_ID_1)
                    setDisplayName(DISPLAY_NAME_1)
                    setNumber(NUMBER_1)
                }
                .build()

        val SUB_INFO_2: SubscriptionInfo =
            SubscriptionInfo.Builder()
                .apply {
                    setId(SUB_ID_2)
                    setDisplayName(DISPLAY_NAME_2)
                    setNumber(NUMBER_2)
                }
                .build()

        val SUB_INFO_3: SubscriptionInfo =
            SubscriptionInfo.Builder()
                .apply {
                    setId(SUB_ID_3)
                    setDisplayName(DISPLAY_NAME_3)
                    setNumber(NUMBER_3)
                }
                .build()
    }
}
