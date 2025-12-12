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
import android.net.wifi.WifiConfiguration
import android.net.wifi.WifiManager
import android.platform.test.annotations.DisableFlags
import android.platform.test.annotations.EnableFlags
import android.platform.test.flag.junit.SetFlagsRule
import androidx.appcompat.app.AlertDialog
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.R
import com.android.settings.connectivity.Flags
import com.android.settings.core.BasePreferenceController
import com.android.settings.testutils.shadow.ShadowAlertDialogCompat
import com.android.settings.wifi.WifiPickerTrackerHelper
import com.android.wifitrackerlib.WifiEntry
import com.android.wifitrackerlib.WifiPickerTracker
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.Mockito.any
import org.mockito.Mockito.verify
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.spy
import org.mockito.kotlin.stub
import org.mockito.kotlin.whenever
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowDialog
import org.robolectric.shadows.ShadowLooper

@RunWith(AndroidJUnit4::class)
@Config(shadows = [ShadowAlertDialogCompat::class])
class WifiSharedPreferenceControllerTest {
    @get:Rule val setFlagsRule = SetFlagsRule()

    private var mockWifiEntry = mock<WifiEntry>()

    private val context: Context = spy(RuntimeEnvironment.application)

    private val mockWifiPickerTracker: WifiPickerTracker = mock<WifiPickerTracker>()

    private val mockWifiPickerTrackerHelper: WifiPickerTrackerHelper =
        mock<WifiPickerTrackerHelper>()

    private var controller =
        WifiSharedPreferenceController(
            context,
            "share_configuration",
            mockWifiPickerTrackerHelper,
            mockWifiEntry,
        )

    @Before
    fun setUp() {
        mockWifiEntry.stub { on { getWifiConfiguration() } doReturn WifiConfiguration() }
        mockWifiEntry.stub { on { getSsid() } doReturn "testSSID" }
        mockWifiEntry.stub { on { getKey() } doReturn "testKey" }

        context.setTheme(R.style.Theme_Settings_Home)
    }

    @Test
    fun isChecked_returnsWifiEntry_allowEditConfig_Value() {
        mockWifiEntry.stub { on { isSharedWithOtherUsers() } doReturn false }

        assertThat(controller.isChecked()).isFalse()
    }

    @Test
    fun setChecked_conflictingEntry_showsAlertDialog_validateMessages() {
        mockWifiPickerTrackerHelper.stub {
            on { getWifiPickerTracker() } doReturn mockWifiPickerTracker
        }
        whenever(mockWifiPickerTracker.wifiEntries).thenReturn(listOf(mockWifiEntry))
        ShadowDialog.reset()

        controller.setChecked(true)

        val dialog = ShadowAlertDialogCompat.getLatestAlertDialog()
        val shadowDialog = ShadowAlertDialogCompat.shadowOf(dialog)
        assertThat(shadowDialog).isNotNull()
        assertThat(shadowDialog.getTitle().toString())
            .isEqualTo(context.getString(R.string.wifi_conflict_dialog_title, "shared", "testSSID"))
        assertThat(shadowDialog.getMessage().toString())
            .isEqualTo(
                context.getString(
                    R.string.wifi_conflict_dialog_message,
                    "private",
                    "private",
                    "shared",
                )
            )
    }

    @Test
    fun setChecked_conflictingEntry_showsAlertDialog_clickNegativeButton() {
        mockWifiPickerTrackerHelper.stub {
            on { getWifiPickerTracker() } doReturn mockWifiPickerTracker
        }
        whenever(mockWifiPickerTracker.wifiEntries).thenReturn(listOf(mockWifiEntry))
        ShadowDialog.reset()

        controller.setChecked(true)

        var dialog: AlertDialog? = ShadowAlertDialogCompat.getLatestAlertDialog()
        assertThat(dialog).isNotNull()
        dialog?.getButton(AlertDialog.BUTTON_NEGATIVE)?.performClick()
        ShadowLooper.idleMainLooper()

        assertThat(dialog?.isShowing()).isFalse()
    }

    @Ignore("b/424068182")
    @Test
    fun setChecked_noConflict_doesNotShowAlertDialog() {
        mockWifiPickerTrackerHelper.stub {
            on { getWifiPickerTracker() } doReturn mockWifiPickerTracker
        }
        whenever(mockWifiPickerTracker.wifiEntries).thenReturn(listOf())
        ShadowDialog.reset()

        controller.setChecked(true)

        val dialog = ShadowAlertDialogCompat.getLatestAlertDialog()
        assertThat(dialog).isNull()
    }

    @Test
    fun setChecked_noConflict_savesWifiEntry() {
        mockWifiPickerTrackerHelper.stub {
            on { getWifiPickerTracker() } doReturn mockWifiPickerTracker
        }
        whenever(mockWifiPickerTracker.wifiEntries).thenReturn(listOf())

        controller.setChecked(false)

        val wifiConfigCaptor = ArgumentCaptor.forClass(WifiConfiguration::class.java)
        verify(mockWifiEntry).setSharedWithOtherUsers(false)
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
