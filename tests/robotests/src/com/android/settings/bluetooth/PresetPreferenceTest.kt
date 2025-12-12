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

package com.android.settings.bluetooth

import android.bluetooth.BluetoothHapPresetInfo
import android.content.Context
import android.os.Parcelable
import android.view.LayoutInflater
import androidx.preference.PreferenceManager
import androidx.test.core.app.ApplicationProvider
import com.android.settings.R
import com.android.settings.bluetooth.BluetoothDetailsPresetPreferenceController.Companion.KEY_HEARING_AIDS_PRESETS
import com.android.settingslib.bluetooth.HearingAidInfo.DeviceSide.SIDE_LEFT
import com.android.settingslib.bluetooth.HearingAidInfo.DeviceSide.SIDE_RIGHT
import com.android.settingslib.bluetooth.hearingdevices.ui.ExpandableControlUi.Companion.SIDE_UNIFIED
import com.android.settingslib.bluetooth.hearingdevices.ui.PresetUi
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.robolectric.RobolectricTestRunner

/** Tests for [PresetPreference]. */
@RunWith(RobolectricTestRunner::class)
class PresetPreferenceTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val dialogView =
        LayoutInflater.from(context).inflate(R.layout.hearing_device_preset_dialog, null)
    private val mockPresetUiListener = mock<PresetUi.PresetUiListener>()
    private val mockPresetInfos =
        listOf(
            createMockPresetInfo(TEST_ACTIVE_RESET_INDEX1, TEST_ACTIVE_RESET_NAME1),
            createMockPresetInfo(TEST_ACTIVE_RESET_INDEX2, TEST_ACTIVE_RESET_NAME2),
            createMockPresetInfo(TEST_ACTIVE_RESET_INDEX3, TEST_ACTIVE_RESET_NAME3),
        )
    private lateinit var preference: PresetPreference

    @Before
    fun setUp() {
        preference = PresetPreference(context)
        preference.setKey(KEY_HEARING_AIDS_PRESETS)
        preference.setListener(mockPresetUiListener)
        preference.setupControls(setOf(SIDE_LEFT, SIDE_RIGHT))
        preference.setControlExpanded(true)
        setupControlForTest(SIDE_LEFT, TEST_ACTIVE_RESET_INDEX1)
        setupControlForTest(SIDE_RIGHT, TEST_ACTIVE_RESET_INDEX2)
        setupControlForTest(SIDE_UNIFIED, TEST_ACTIVE_RESET_INDEX3)
        val preferenceManager = PreferenceManager(context)
        val preferenceScreen = preferenceManager.createPreferenceScreen(context)
        preferenceScreen.addPreference(preference)
    }

    @Test
    fun testSaveAndRestoreInstanceState() {
        val parcelable: Parcelable? = preference.onSaveInstanceState()
        preference.onRestoreInstanceState(parcelable)

        assertThat(preference.isControlExpanded()).isTrue()
        assertThat(preference.getControls()[SIDE_LEFT]?.getValue())
            .isEqualTo(TEST_ACTIVE_RESET_INDEX1)
        assertThat(preference.getControls()[SIDE_RIGHT]?.getValue())
            .isEqualTo(TEST_ACTIVE_RESET_INDEX2)
        assertThat(preference.getControls()[SIDE_UNIFIED]?.getValue())
            .isEqualTo(TEST_ACTIVE_RESET_INDEX3)
        assertThat(preference.getControls()[SIDE_LEFT]?.getList()).isEqualTo(mockPresetInfos)
        assertThat(preference.getControls()[SIDE_RIGHT]?.getList()).isEqualTo(mockPresetInfos)
        assertThat(preference.getControls()[SIDE_UNIFIED]?.getList()).isEqualTo(mockPresetInfos)
    }

    @Test
    fun getSummary_expandedAndNoValidControl_showEmptyListMessage() {
        preference.setControlExpanded(true)
        preference.setControlList(SIDE_LEFT, emptyList())
        preference.setControlList(SIDE_RIGHT, emptyList())

        assertThat(preference.summary)
            .isEqualTo(
                context.getString(R.string.bluetooth_hearing_aids_presets_empty_list_message)
            )
    }

    @Test
    fun getSummary_notExpandedAndNoValidControl_showEmptyListMessage() {
        preference.setControlExpanded(false)
        preference.setControlList(SIDE_UNIFIED, emptyList())

        assertThat(preference.summary)
            .isEqualTo(
                context.getString(R.string.bluetooth_hearing_aids_presets_empty_list_message)
            )
    }

    @Test
    fun getSummary_expandedAndValidControl_showCorrectMessage() {
        preference.setControlExpanded(true)

        assertThat(preference.summary)
            .isEqualTo(
                context.getString(
                    R.string.bluetooth_hearing_aids_presets_binaural_summary,
                    TEST_ACTIVE_RESET_NAME1,
                    TEST_ACTIVE_RESET_NAME2,
                )
            )
    }

    @Test
    fun getSummary_notExpandedAndValidControl_showCorrectMessage() {
        preference.setControlExpanded(false)

        assertThat(preference.summary).isEqualTo(TEST_ACTIVE_RESET_NAME3)
    }

    @Test
    fun setControlEnabled_expandedAndNoEnabledControl_preferenceIsNotEnabled() {
        preference.setControlExpanded(true)
        preference.setControlEnabled(SIDE_LEFT, false)
        preference.setControlEnabled(SIDE_RIGHT, false)

        assertThat(preference.isEnabled).isFalse()
    }

    @Test
    fun setControlEnabled_expandedAndHasEnabledControl_preferenceIsEnabled() {
        preference.setControlExpanded(true)
        preference.setControlEnabled(SIDE_LEFT, false)
        preference.setControlEnabled(SIDE_RIGHT, true)

        assertThat(preference.isEnabled).isTrue()
    }

    @Test
    fun setControlEnabled_notExpandedAndNoEnabledControl_preferenceIsNotEnabled() {
        preference.setControlExpanded(false)
        preference.setControlEnabled(SIDE_UNIFIED, false)

        assertThat(preference.isEnabled).isFalse()
    }

    @Test
    fun setControlEnabled_notExpandedAndHasEnabledControl_preferenceIsEnabled() {
        preference.setControlExpanded(false)
        preference.setControlEnabled(SIDE_UNIFIED, true)

        assertThat(preference.isEnabled).isTrue()
    }

    private fun setupControlForTest(side: Int, activeIndex: Int) {
        preference.setControlList(side, mockPresetInfos)
        preference.setControlValue(side, activeIndex)
        preference.setControlEnabled(side, true)
    }

    private fun createMockPresetInfo(index: Int, name: String): BluetoothHapPresetInfo {
        return mock<BluetoothHapPresetInfo> {
            on { this.index } doReturn index
            on { this.name } doReturn name
            on { isAvailable } doReturn true
        }
    }

    companion object {
        const val TEST_ACTIVE_RESET_INDEX1 = 1
        const val TEST_ACTIVE_RESET_INDEX2 = 2
        const val TEST_ACTIVE_RESET_INDEX3 = 3
        const val TEST_ACTIVE_RESET_NAME1 = "preset1"
        const val TEST_ACTIVE_RESET_NAME2 = "preset2"
        const val TEST_ACTIVE_RESET_NAME3 = "preset3"
    }
}
