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

package com.android.settings.display.darkmode

import android.app.TimePickerDialog
import android.app.settings.SettingsEnums
import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentResultListener
import androidx.fragment.app.testing.FragmentScenario
import androidx.fragment.app.testing.launchFragment
import com.android.settings.display.darkmode.DarkModeTimePicker.Companion.RESULT_HOUR
import com.android.settings.display.darkmode.DarkModeTimePicker.Companion.RESULT_MINUTE
import com.google.common.truth.Truth.assertThat
import java.time.LocalTime
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.Captor
import org.mockito.junit.MockitoJUnit
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowDialog
import org.robolectric.shadows.ShadowLooper

@RunWith(RobolectricTestRunner::class)
class DarkModeTimePickerTest {

    @get:Rule val mockito = MockitoJUnit.rule()
    private val requestKey = "requestFromTest"
    private lateinit var fragmentScenario: FragmentScenario<Fragment>
    private lateinit var fragment: Fragment
    private val mockFragResultListener = mock<FragmentResultListener>()
    @Captor lateinit var responseCaptor: ArgumentCaptor<Bundle>

    @Before
    fun setUp() {
        fragmentScenario = launchFragment(themeResId = androidx.appcompat.R.style.Theme_AppCompat)
        fragmentScenario.onFragment { frag ->
            fragment = frag
            fragment.childFragmentManager.setFragmentResultListener(
                requestKey,
                fragment,
                mockFragResultListener,
            )
        }
    }

    @After
    fun cleanUp() {
        fragmentScenario.close()
    }

    @Test
    fun onFragmentResult_verifyResult() {
        DarkModeTimePicker.showDialog(
            fragment.childFragmentManager,
            requestKey,
            SettingsEnums.DIALOG_DARK_THEME_SET_START_TIME,
            LocalTime.of(10, 30),
        )
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()
        val dialog = ShadowDialog.getLatestDialog() as TimePickerDialog

        dialog.onClick(null, TimePickerDialog.BUTTON_POSITIVE)
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()

        verify(mockFragResultListener).onFragmentResult(eq(requestKey), responseCaptor.capture())
        val response = responseCaptor.value
        assertThat(response).isNotNull()
        assertThat(response.getInt(RESULT_HOUR)).isEqualTo(10)
        assertThat(response.getInt(RESULT_MINUTE)).isEqualTo(30)
    }

    @Test
    fun getMetricsCategory_startTimePicker() {
        assertThat(
                DarkModeTimePicker(SettingsEnums.DIALOG_DARK_THEME_SET_START_TIME).metricsCategory
            )
            .isEqualTo(SettingsEnums.DIALOG_DARK_THEME_SET_START_TIME)
    }

    @Test
    fun getMetricsCategory_endTimePicker() {
        assertThat(DarkModeTimePicker(SettingsEnums.DIALOG_DARK_THEME_SET_END_TIME).metricsCategory)
            .isEqualTo(SettingsEnums.DIALOG_DARK_THEME_SET_END_TIME)
    }
}
