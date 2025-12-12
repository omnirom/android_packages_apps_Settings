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

package com.android.settings.bluetooth

import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import com.android.settings.bluetooth.BluetoothDetailsHearingDeviceController.KEY_HEARING_DEVICE_GROUP
import com.android.settings.bluetooth.BluetoothDetailsPresetPreferenceController.Companion.KEY_HEARING_AIDS_PRESETS
import com.android.settings.testutils.shadow.ShadowThreadUtils
import com.android.settingslib.bluetooth.BluetoothEventManager
import com.android.settingslib.bluetooth.HapClientProfile
import com.android.settingslib.bluetooth.LocalBluetoothManager
import com.android.settingslib.bluetooth.LocalBluetoothProfileManager
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub
import org.robolectric.annotation.Config

/** Tests for [BluetoothDetailsPresetPreferenceController]. */
@Config(shadows = [ShadowThreadUtils::class])
class BluetoothDetailsPresetPreferenceControllerTest : BluetoothDetailsControllerTestBase() {

    val mockBluetoothManager = mock<LocalBluetoothManager>()
    val mockEventManager = mock<BluetoothEventManager>()
    val mockProfileManager = mock<LocalBluetoothProfileManager>()
    val mockHapClientProfile = mock<HapClientProfile>()

    private lateinit var controller: BluetoothDetailsPresetPreferenceController

    @Before
    override fun setUp() {
        super.setUp()
        mockBluetoothManager.stub {
            on { eventManager } doReturn mockEventManager
            on { profileManager } doReturn mockProfileManager
        }
        mockProfileManager.stub { on { hapClientProfile } doReturn mockHapClientProfile }
        controller =
            BluetoothDetailsPresetPreferenceController(
                mContext,
                mockBluetoothManager,
                mFragment,
                mCachedDevice,
                mLifecycle,
            )

        val deviceControls = PreferenceCategory(mContext)
        deviceControls.setKey(KEY_HEARING_DEVICE_GROUP)
        mScreen.addPreference(deviceControls)
    }

    @Test
    fun init_presetControlsAdded() {
        controller.init(mScreen)

        val presetControl = mScreen.findPreference<Preference>(KEY_HEARING_AIDS_PRESETS)
        assertThat(presetControl).isNotNull()
    }

    @Test
    fun isAvailable_notSupportHap_returnFalse() {
        mCachedDevice.stub { on { profiles } doReturn listOf() }

        assertThat(controller.isAvailable).isFalse()
    }

    @Test
    fun isAvailable_supportHap_returnTrue() {
        mCachedDevice.stub { on { profiles } doReturn listOf(mockHapClientProfile) }

        assertThat(controller.isAvailable).isTrue()
    }
}
