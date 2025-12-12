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

package com.android.settings.wifi.details2

import android.content.Context
import android.platform.test.annotations.DisableFlags
import android.platform.test.annotations.EnableFlags
import android.platform.test.flag.junit.SetFlagsRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.connectivity.Flags
import com.android.settings.core.BasePreferenceController
import com.android.wifitrackerlib.WifiEntry
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify

@RunWith(AndroidJUnit4::class)
class WifiEditConfigPreferenceControllerTest {
    @get:Rule val setFlagsRule = SetFlagsRule()

    private var mockWifiEntry = mock<WifiEntry>()

    private val context: Context = mock<Context>()

    private var controller = WifiEditConfigPreferenceController(context, "edit_configuration", mockWifiEntry)

    @Test
    fun isChecked_returnsWifiEntry_allowEditConfig_Value() {
        mockWifiEntry.stub { on { isModifiableByOtherUsers() } doReturn false }

        assertThat(controller.isChecked()).isFalse()
    }

    @Test
    fun setChecked_setsWifiEntryValue() {
        controller.setChecked(true)

        verify(mockWifiEntry).setModifiableByOtherUsers(true)
    }

    @Test
    @DisableFlags(Flags.FLAG_WIFI_MULTIUSER)
    fun getAvailabilityStatus_flagDisabled() {
        assertThat(controller.getAvailabilityStatus())
            .isEqualTo(BasePreferenceController.CONDITIONALLY_UNAVAILABLE)
    }

    @Test
    @EnableFlags(Flags.FLAG_WIFI_MULTIUSER)
    fun getAvailabilityStatus_flagEnabled() {
        assertThat(controller.getAvailabilityStatus()).isEqualTo(BasePreferenceController.AVAILABLE)
    }
}
