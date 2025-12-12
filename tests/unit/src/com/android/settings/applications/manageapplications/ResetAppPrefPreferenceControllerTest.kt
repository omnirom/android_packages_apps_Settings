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

package com.android.settings.applications.manageapplications

import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Resources
import android.telephony.TelephonyManager
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.anyInt
import org.mockito.Mockito.`when`
import org.mockito.junit.MockitoJUnit
import org.mockito.junit.MockitoRule

@RunWith(AndroidJUnit4::class)
class ResetAppPrefPreferenceControllerTest {

    @get:Rule val mRule: MockitoRule = MockitoJUnit.rule()

    @Mock private lateinit var mContext: Context
    @Mock private lateinit var mPackageManager: PackageManager
    @Mock private lateinit var mTelephonyManager: TelephonyManager
    @Mock private lateinit var mResources: Resources

    private lateinit var mController: ResetAppPrefPreferenceController

    @Before
    fun setUp() {
        `when`(mContext.packageManager).thenReturn(mPackageManager)
        `when`(mContext.getSystemServiceName(TelephonyManager::class.java))
            .thenReturn(Context.TELEPHONY_SERVICE)
        `when`(mContext.getSystemService(Context.TELEPHONY_SERVICE)).thenReturn(mTelephonyManager)
        `when`(mContext.resources).thenReturn(mResources)
        `when`(mResources.getBoolean(anyInt())).thenReturn(true)
        mController = ResetAppPrefPreferenceController(mContext, null)
    }

    @Test
    fun isInCallState_noTelephonyFeature_shouldReturnFalse() {
        `when`(mTelephonyManager.isDeviceVoiceCapable).thenReturn(false)

        assertThat(mController.isInCallState()).isFalse()
    }

    @Test
    fun isInCallState_callStateIdle_shouldReturnFalse() {
        `when`(mTelephonyManager.isDeviceVoiceCapable).thenReturn(true)
        `when`(mTelephonyManager.getCallState(anyInt()))
            .thenReturn(TelephonyManager.CALL_STATE_IDLE)

        assertThat(mController.isInCallState()).isFalse()
    }

    @Test
    fun isInCallState_callStateRinging_shouldReturnTrue() {
        `when`(mTelephonyManager.isDeviceVoiceCapable).thenReturn(true)
        `when`(mTelephonyManager.getCallState(anyInt()))
            .thenReturn(TelephonyManager.CALL_STATE_RINGING)

        assertThat(mController.isInCallState()).isTrue()
    }

    @Test
    fun isInCallState_nullTelephonyManager_shouldReturnFalse() {
        `when`(mContext.getSystemService(Context.TELEPHONY_SERVICE)).thenReturn(null)

        assertThat(mController.isInCallState()).isFalse()
    }
}
