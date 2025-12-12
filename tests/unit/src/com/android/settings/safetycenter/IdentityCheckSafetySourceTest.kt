/*
 * Copyright (C) 2025 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings.safetycenter

import android.content.Context
import android.hardware.biometrics.BiometricManager
import android.hardware.biometrics.Flags
import android.platform.test.annotations.RequiresFlagsDisabled
import android.platform.test.annotations.RequiresFlagsEnabled
import android.platform.test.flag.junit.CheckFlagsRule
import android.platform.test.flag.junit.DeviceFlagsValueProvider
import android.provider.Settings
import android.safetycenter.SafetyEvent
import android.safetycenter.SafetyEvent.SAFETY_EVENT_TYPE_REFRESH_REQUESTED
import android.safetycenter.SafetyEvent.SAFETY_EVENT_TYPE_SOURCE_STATE_CHANGED
import android.safetycenter.SafetySourceData
import android.safetycenter.SafetySourceIssue
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.R
import com.android.settings.safetycenter.IdentityCheckSafetySource.Companion.ACTION_ISSUE_CARD_SHOW_DETAILS
import com.android.settings.safetycenter.IdentityCheckSafetySource.Companion.ACTION_ISSUE_CARD_WATCH_SHOW_DETAILS
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.any
import org.mockito.Mock
import org.mockito.junit.MockitoJUnit
import org.mockito.junit.MockitoRule
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@RunWith(AndroidJUnit4::class)
class IdentityCheckSafetySourceTest {
    @get:Rule val mockitoRule: MockitoRule = MockitoJUnit.rule()

    @get:Rule val mCheckFlagsRule: CheckFlagsRule = DeviceFlagsValueProvider.createCheckFlagsRule()

    @Mock lateinit var safetyCenterManagerWrapper: SafetyCenterManagerWrapper
    @Mock lateinit var biometricManager: BiometricManager

    private val applicationContext: Context = ApplicationProvider.getApplicationContext()
    private val refreshSafetyEvent =
        SafetyEvent.Builder(SAFETY_EVENT_TYPE_REFRESH_REQUESTED)
            .setRefreshBroadcastId(IdentityCheckSafetySource.SAFETY_SOURCE_ID)
            .build()
    private val sourceChangeSafetyEvent =
        SafetyEvent.Builder(SAFETY_EVENT_TYPE_SOURCE_STATE_CHANGED).build()
    private val safetySourceDataArgumentCaptor = argumentCaptor<SafetySourceData>()

    private lateinit var identityCheckSafetySource: IdentityCheckSafetySource

    @Before
    fun setUp() {
        SafetyCenterManagerWrapper.sInstance = safetyCenterManagerWrapper
        identityCheckSafetySource = IdentityCheckSafetySource()
        whenever(biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG))
            .thenReturn(BiometricManager.BIOMETRIC_SUCCESS)
    }

    @After
    fun tearDown() {
        SafetyCenterManagerWrapper.sInstance = null
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_IDENTITY_CHECK_ALL_SURFACES)
    fun refreshSafetySources_whenSafetyCenterIsDisabled_doesNotSetData() {
        whenever(safetyCenterManagerWrapper.isEnabled(applicationContext)).thenReturn(false)

        setIdentityCheckPromoCardShown(false)

        IdentityCheckSafetySource.setSafetySourceData(
            applicationContext,
            refreshSafetyEvent,
            biometricManager,
        )

        verify(safetyCenterManagerWrapper, never()).setSafetySourceData(any(), any(), any(), any())
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_IDENTITY_CHECK_ALL_SURFACES)
    fun refreshSafetySources_whenPromoCardAlreadyShown_setsNullData() {
        whenever(safetyCenterManagerWrapper.isEnabled(applicationContext)).thenReturn(true)

        setIdentityCheckPromoCardShown(true)

        IdentityCheckSafetySource.setSafetySourceData(
            applicationContext,
            refreshSafetyEvent,
            biometricManager,
        )

        verify(safetyCenterManagerWrapper)
            .setSafetySourceData(
                eq(applicationContext),
                eq(IdentityCheckSafetySource.SAFETY_SOURCE_ID),
                eq(null),
                eq(refreshSafetyEvent),
            )
    }

    @Test
    @RequiresFlagsDisabled(Flags.FLAG_IDENTITY_CHECK_ALL_SURFACES)
    fun refreshSafetySources_whenFlagDisabled_setsNullData() {
        whenever(safetyCenterManagerWrapper.isEnabled(applicationContext)).thenReturn(true)

        setIdentityCheckPromoCardShown(false)

        IdentityCheckSafetySource.setSafetySourceData(
            applicationContext,
            refreshSafetyEvent,
            biometricManager,
        )

        verify(safetyCenterManagerWrapper)
            .setSafetySourceData(
                eq(applicationContext),
                eq(IdentityCheckSafetySource.SAFETY_SOURCE_ID),
                eq(null),
                eq(refreshSafetyEvent),
            )
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_IDENTITY_CHECK_ALL_SURFACES)
    fun refreshSafetySources_whenInvalidDevice_setsNullData() {
        whenever(safetyCenterManagerWrapper.isEnabled(applicationContext)).thenReturn(true)
        whenever(biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG))
            .thenReturn(BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE)
        setIdentityCheckPromoCardShown(false)

        IdentityCheckSafetySource.setSafetySourceData(
            applicationContext,
            refreshSafetyEvent,
            biometricManager,
        )

        verify(safetyCenterManagerWrapper)
            .setSafetySourceData(
                eq(applicationContext),
                eq(IdentityCheckSafetySource.SAFETY_SOURCE_ID),
                eq(null),
                eq(refreshSafetyEvent),
            )
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_IDENTITY_CHECK_ALL_SURFACES)
    fun refreshSafetySources_whenDeviceIsATablet_setsNullData() {
        whenever(safetyCenterManagerWrapper.isEnabled(applicationContext)).thenReturn(true)
        setIdentityCheckPromoCardShown(false)

        IdentityCheckSafetySource.setSafetySourceData(
            applicationContext,
            refreshSafetyEvent,
            biometricManager,
            isTablet = true,
        )

        verify(safetyCenterManagerWrapper)
            .setSafetySourceData(
                eq(applicationContext),
                eq(IdentityCheckSafetySource.SAFETY_SOURCE_ID),
                eq(null),
                eq(refreshSafetyEvent),
            )
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_IDENTITY_CHECK_ALL_SURFACES)
    fun refreshSafetySources_whenDeviceIsALowRamTablet_setsNullData() {
        whenever(safetyCenterManagerWrapper.isEnabled(applicationContext)).thenReturn(true)
        setIdentityCheckPromoCardShown(false)

        IdentityCheckSafetySource.setSafetySourceData(
            applicationContext,
            refreshSafetyEvent,
            biometricManager,
            isLowRamDevice = true,
        )

        verify(safetyCenterManagerWrapper)
            .setSafetySourceData(
                eq(applicationContext),
                eq(IdentityCheckSafetySource.SAFETY_SOURCE_ID),
                eq(null),
                eq(refreshSafetyEvent),
            )
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_IDENTITY_CHECK_ALL_SURFACES, Flags.FLAG_IDENTITY_CHECK_WATCH)
    fun refreshSafetySources_whenWatchSupportedValueNotSet_setsNullData() {
        whenever(safetyCenterManagerWrapper.isEnabled(applicationContext)).thenReturn(true)

        setIdentityCheckPromoCardShown(false)
        resetWatchRangingSupportedValue()

        IdentityCheckSafetySource.setSafetySourceData(
            applicationContext,
            refreshSafetyEvent,
            biometricManager,
        )

        verify(safetyCenterManagerWrapper)
            .setSafetySourceData(
                eq(applicationContext),
                eq(IdentityCheckSafetySource.SAFETY_SOURCE_ID),
                eq(null),
                eq(refreshSafetyEvent),
            )
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_IDENTITY_CHECK_ALL_SURFACES)
    fun refreshSafetySources_notificationNotClicked_setsSafetySourceDataWithNotification() {
        whenever(safetyCenterManagerWrapper.isEnabled(applicationContext)).thenReturn(true)

        setIdentityCheckNotificationBeenClicked(false)
        setIdentityCheckPromoCardShown(false)
        setWatchRangingSupportedValue(false)

        IdentityCheckSafetySource.setSafetySourceData(
            applicationContext,
            refreshSafetyEvent,
            biometricManager,
        )

        verify(safetyCenterManagerWrapper)
            .setSafetySourceData(
                eq(applicationContext),
                eq(IdentityCheckSafetySource.SAFETY_SOURCE_ID),
                safetySourceDataArgumentCaptor.capture(),
                eq(refreshSafetyEvent),
            )

        val safetySourceIssue: SafetySourceIssue =
            safetySourceDataArgumentCaptor.firstValue.issues[0]!!
        val actionPendingIntent = safetySourceIssue.actions[0].pendingIntent

        assertThat(safetySourceIssue.customNotification).isNotNull()
        assertThat(actionPendingIntent.intent.action).isEqualTo(ACTION_ISSUE_CARD_SHOW_DETAILS)
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_IDENTITY_CHECK_ALL_SURFACES)
    fun refreshSafetySources_notificationClicked_setsSafetySourceDataWithoutNotification() {
        whenever(safetyCenterManagerWrapper.isEnabled(applicationContext)).thenReturn(true)

        setIdentityCheckNotificationBeenClicked(true)
        setIdentityCheckPromoCardShown(false)
        setWatchRangingSupportedValue(false)

        IdentityCheckSafetySource.setSafetySourceData(
            applicationContext,
            refreshSafetyEvent,
            biometricManager,
        )

        verify(safetyCenterManagerWrapper)
            .setSafetySourceData(
                eq(applicationContext),
                eq(IdentityCheckSafetySource.SAFETY_SOURCE_ID),
                safetySourceDataArgumentCaptor.capture(),
                eq(refreshSafetyEvent),
            )

        val safetySourceIssue: SafetySourceIssue =
            safetySourceDataArgumentCaptor.firstValue.issues[0]!!
        val actionPendingIntent = safetySourceIssue.actions[0].pendingIntent

        assertThat(safetySourceIssue.customNotification).isNull()
        assertThat(actionPendingIntent.intent.action).isEqualTo(ACTION_ISSUE_CARD_SHOW_DETAILS)
    }

    @Test
    @Ignore("b/353706169")
    @RequiresFlagsEnabled(Flags.FLAG_IDENTITY_CHECK_ALL_SURFACES, Flags.FLAG_IDENTITY_CHECK_WATCH)
    fun refreshSafetySources_watchAvailableOnPrimaryDevice_setsSafetySourceData() {
        whenever(safetyCenterManagerWrapper.isEnabled(applicationContext)).thenReturn(true)

        setWatchRangingSupportedValue(true)
        setIdentityCheckPromoCardShown(false)

        IdentityCheckSafetySource.setSafetySourceData(
            applicationContext,
            refreshSafetyEvent,
            biometricManager,
        )

        verify(safetyCenterManagerWrapper)
            .setSafetySourceData(
                eq(applicationContext),
                eq(IdentityCheckSafetySource.SAFETY_SOURCE_ID),
                safetySourceDataArgumentCaptor.capture(),
                eq(refreshSafetyEvent),
            )

        val safetySourceData = safetySourceDataArgumentCaptor.firstValue
        val actionPendingIntent = safetySourceData.issues[0].actions[0].pendingIntent
        val showWatchPromo =
            applicationContext.resources.getBoolean(R.bool.config_show_identity_check_watch_promo)
        val expectedAction =
            if (showWatchPromo) ACTION_ISSUE_CARD_WATCH_SHOW_DETAILS
            else ACTION_ISSUE_CARD_SHOW_DETAILS

        assertThat(actionPendingIntent.intent.action).isEqualTo(expectedAction)
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_IDENTITY_CHECK_ALL_SURFACES, Flags.FLAG_IDENTITY_CHECK_WATCH)
    fun watchContentObserver_onChange_setsSafetySourceData() {
        whenever(safetyCenterManagerWrapper.isEnabled(applicationContext)).thenReturn(true)

        setWatchRangingSupportedValue(true)
        setIdentityCheckPromoCardShown(false)

        val observer = IdentityCheckSafetySource.WatchContentObserver(applicationContext)
        val uri =
            Settings.Global.getUriFor(Settings.Global.WATCH_RANGING_SUPPORTED_BY_PRIMARY_DEVICE)

        observer.onChange(false, uri)

        verify(safetyCenterManagerWrapper)
            .setSafetySourceData(
                eq(applicationContext),
                eq(IdentityCheckSafetySource.SAFETY_SOURCE_ID),
                any(),
                eq(sourceChangeSafetyEvent),
            )
    }

    private fun setIdentityCheckNotificationBeenClicked(clicked: Boolean) {
        Settings.Secure.putInt(
            applicationContext.contentResolver,
            Settings.Secure.IDENTITY_CHECK_NOTIFICATION_VIEW_DETAILS_CLICKED,
            if (clicked) 1 else 0,
        )
    }

    private fun setIdentityCheckPromoCardShown(hasShown: Boolean) {
        Settings.Secure.putInt(
            applicationContext.contentResolver,
            Settings.Secure.IDENTITY_CHECK_PROMO_CARD_SHOWN,
            if (hasShown) 1 else 0,
        )
    }

    private fun setWatchRangingSupportedValue(isWatchAvailable: Boolean) {
        Settings.Global.putInt(
            applicationContext.contentResolver,
            Settings.Global.WATCH_RANGING_SUPPORTED_BY_PRIMARY_DEVICE,
            if (isWatchAvailable) 1 else 0,
        )
    }

    private fun resetWatchRangingSupportedValue() {
        Settings.Global.putString(
            applicationContext.contentResolver,
            Settings.Global.WATCH_RANGING_SUPPORTED_BY_PRIMARY_DEVICE,
            null,
        )
    }
}
