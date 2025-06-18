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

import android.bluetooth.BluetoothDevice
import com.android.settings.R
import com.android.settings.testutils.FakeFeatureFactory
import com.android.settingslib.widget.LayoutPreference
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.junit.MockitoJUnit
import org.mockito.junit.MockitoRule
import org.mockito.kotlin.whenever

class BluetoothDetailsBannerControllerTest : BluetoothDetailsControllerTestBase() {
    @get:Rule val mockitoRule: MockitoRule = MockitoJUnit.rule()

    private lateinit var controller: BluetoothDetailsBannerController
    private lateinit var preference: LayoutPreference

    override fun setUp() {
        super.setUp()
        FakeFeatureFactory.setupForTest()
        controller =
            BluetoothDetailsBannerController(mContext, mFragment, mCachedDevice, mLifecycle)
        preference = LayoutPreference(mContext, R.layout.bluetooth_details_banner)
        preference.key = controller.getPreferenceKey()
        mScreen.addPreference(preference)
    }

    @Test
    fun iaAvailable_notKeyMissing_false() {
        setupDevice(makeDefaultDeviceConfig())

        assertThat(controller.isAvailable).isFalse()
    }

    // TODO(b/379729762): add more tests after BluetoothDevice.getKeyMissingCount is available.
}
