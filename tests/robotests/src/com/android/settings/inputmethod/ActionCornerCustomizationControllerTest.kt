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

package com.android.settings.inputmethod

import android.app.ActivityManager
import android.app.settings.SettingsEnums
import android.content.Context
import android.provider.Settings
import android.provider.Settings.Secure.ACTION_CORNER_ACTION_HOME
import android.provider.Settings.Secure.ACTION_CORNER_ACTION_LOCKSCREEN
import android.provider.Settings.Secure.ACTION_CORNER_ACTION_NONE
import android.provider.Settings.Secure.ACTION_CORNER_ACTION_NOTIFICATIONS
import android.provider.Settings.Secure.ACTION_CORNER_ACTION_OVERVIEW
import android.provider.Settings.Secure.ACTION_CORNER_ACTION_QUICK_SETTINGS
import android.provider.Settings.Secure.ACTION_CORNER_BOTTOM_LEFT_ACTION
import androidx.preference.ListPreference
import androidx.preference.PreferenceManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.R
import com.android.settings.testutils.MetricsRule
import com.android.settings.testutils.shadow.ShadowSystemSettings
import com.google.common.truth.Truth.assertThat
import kotlin.test.Test
import org.junit.Before
import org.junit.Rule
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnit
import org.mockito.junit.MockitoRule
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify
import org.robolectric.annotation.Config

/** Test for [ActionCornerCustomizationController] */
@RunWith(AndroidJUnit4::class)
@Config(shadows = [ShadowSystemSettings::class])
class ActionCornerCustomizationControllerTest {
    @get:Rule val mockitoRule: MockitoRule = MockitoJUnit.rule()

    @get:Rule val metricsRule = MetricsRule()

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val preferenceManager = PreferenceManager(context)
    private val preferenceScreen = preferenceManager.createPreferenceScreen(context)
    private lateinit var preference: ListPreference

    private val controller = ActionCornerCustomizationController(context, PREF_KEY_BOTTOM_LEFT)

    @Before
    fun setUp() {
        preference =
            ListPreference(context).apply {
                key = PREF_KEY_BOTTOM_LEFT
                setEntries(R.array.action_corner_action_titles)
                entryValues = entryValueArray
            }
        preferenceScreen.addPreference(preference)
        controller.displayPreference(preferenceScreen)
    }

    @Test
    fun displayPreference_hasCorrectDialogTitle() {
        val dialogTitle = getDialogTitle()
        assertThat(preference.dialogTitle).isEqualTo(dialogTitle)
    }

    private fun getDialogTitle(): String {
        val name = context.getString(R.string.action_corner_bottom_left_name)
        return context.getString(R.string.action_corner_action_dialog_title, name)
    }

    @Test
    fun updateState_default_showNone() {
        controller.updateState(preference)

        assertThat(preference.value).isEqualTo(ACTION_CORNER_ACTION_NONE.toString())
        assertThat(preference.summary).isEqualTo(NONE_TITLE)
    }

    @Test
    fun updateState_actionIsHome_showHome() {
        Settings.Secure.putIntForUser(
            context.contentResolver,
            TARGET,
            ACTION_CORNER_ACTION_HOME,
            ActivityManager.getCurrentUser(),
        )
        controller.updateState(preference)

        assertThat(preference.value).isEqualTo(ACTION_CORNER_ACTION_HOME.toString())
        assertThat(preference.summary).isEqualTo(HOME_TITLE)
    }

    @Test
    fun onPreferenceChange_setToHome() {
        controller.onPreferenceChange(preference, ACTION_CORNER_ACTION_HOME)

        val action =
            Settings.Secure.getIntForUser(
                context.contentResolver,
                TARGET,
                ACTION_CORNER_ACTION_NONE,
                ActivityManager.getCurrentUser(),
            )
        assertThat(action).isEqualTo(ACTION_CORNER_ACTION_HOME)
        verify(metricsRule.metricsFeatureProvider).action(any(), eq(METRICS), eq(action))
    }

    private companion object {
        const val PREF_KEY_BOTTOM_LEFT = "action_corner_bottom_left"
        const val TARGET = ACTION_CORNER_BOTTOM_LEFT_ACTION
        const val NONE_TITLE = "None"
        const val HOME_TITLE = "Home"
        const val METRICS = SettingsEnums.ACTION_BOTTOM_LEFT_ACTION_CORNER_SHORTCUT_CHANGED

        val entryValueArray =
            arrayOf<CharSequence>(
                ACTION_CORNER_ACTION_NONE.toString(),
                ACTION_CORNER_ACTION_HOME.toString(),
                ACTION_CORNER_ACTION_OVERVIEW.toString(),
                ACTION_CORNER_ACTION_NOTIFICATIONS.toString(),
                ACTION_CORNER_ACTION_QUICK_SETTINGS.toString(),
                ACTION_CORNER_ACTION_LOCKSCREEN.toString(),
            )
    }
}
