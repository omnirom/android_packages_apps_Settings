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

package com.android.settings.gestures

import android.content.Context
import androidx.preference.PreferenceManager
import androidx.test.core.app.ApplicationProvider
import com.android.settings.testutils.SettingsStoreRule
import com.android.settingslib.preference.createAndBindWidget
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ButtonNavigationSettingsOrderPreferenceTest {
    @get:Rule val settingsStoreRule = SettingsStoreRule()
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val orderStore = ButtonNavigationSettingsOrderStore(context)
    private val preferenceScreen = PreferenceManager(context).createPreferenceScreen(context)
    private val defaultPreference = DefaultButtonNavigationSettingsOrderPreference(orderStore)
    private val reversePreference = ReverseButtonNavigationSettingsOrderPreference(orderStore)
    private val defaultRadioButton: ButtonNavigationSettingsOrderRadioButton =
        defaultPreference.createAndBindWidget(context, preferenceScreen)
    private val reverseRadioButton: ButtonNavigationSettingsOrderRadioButton =
        reversePreference.createAndBindWidget(context, preferenceScreen)

    @Before
    fun setup() {
        preferenceScreen.addPreference(defaultRadioButton)
        preferenceScreen.addPreference(reverseRadioButton)
    }

    @Test
    fun onRadioButtonClicked_normal_setsNormalChecked() {
        defaultPreference.createWidget(context)
        defaultPreference.onRadioButtonClicked(defaultRadioButton)

        assertThat(defaultRadioButton.isChecked).isTrue()
        assertThat(reverseRadioButton.isChecked).isFalse()
        assertThat(orderStore.getBoolean(DefaultButtonNavigationSettingsOrderPreference.KEY))
            .isEqualTo(true)
        assertThat(orderStore.getBoolean(ReverseButtonNavigationSettingsOrderPreference.KEY))
            .isEqualTo(false)
    }

    @Test
    fun onRadioButtonClicked_reverse_setsReverseChecked() {
        reversePreference.onRadioButtonClicked(reverseRadioButton)

        assertThat(defaultRadioButton.isChecked).isFalse()
        assertThat(reverseRadioButton.isChecked).isTrue()
        assertThat(orderStore.getBoolean(DefaultButtonNavigationSettingsOrderPreference.KEY))
            .isEqualTo(false)
        assertThat(orderStore.getBoolean(ReverseButtonNavigationSettingsOrderPreference.KEY))
            .isEqualTo(true)
    }
}
