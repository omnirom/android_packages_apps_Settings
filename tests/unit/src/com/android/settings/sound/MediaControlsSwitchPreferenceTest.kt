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

package com.android.settings.sound

import android.content.Context

import androidx.preference.SwitchPreferenceCompat
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.sound.MediaControlsSwitchPreference.Companion.mediaControlsDataStore

import com.android.settingslib.datastore.SettingsSecureStore
import com.android.settingslib.preference.createAndBindWidget
import com.google.common.truth.Truth.assertThat

import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

// LINT.IfChange
@RunWith(AndroidJUnit4::class)
class MediaControlsSwitchPreferenceTest {
    private val appContext: Context = ApplicationProvider.getApplicationContext()
    private var originalValue: Boolean? = null
    private val store = appContext.mediaControlsDataStore
    private val key = MediaControlsSwitchPreference.KEY

    private val preference = MediaControlsSwitchPreference(store)

    @Before
    fun setUp() {
        originalValue = preference.storage(appContext).getBoolean(key)
    }

    @After
    fun tearDown() {
        preference.storage(appContext).setBoolean(key, originalValue)
    }

    @Test
    fun mediaControlsLockScreenDefaultValue_isChecked() {
        preference.storage(appContext).setBoolean(key, null)

        val switchPreference = getSwitchPreferenceCompat()

        assertThat(switchPreference.isChecked).isTrue()
    }

    @Test
    fun mediaControlsLockScreenEnabled_switchPreferenceIsChecked() {
        setMediaControlsResumeEnabled(true)

        val switchPreference = getSwitchPreferenceCompat()

        assertThat(switchPreference.isChecked).isTrue()
    }

    @Test
    fun mediaControlsLockScreenDisabled_switchPreferenceIsNotChecked() {
        setMediaControlsResumeEnabled(false)

        val switchPreference = getSwitchPreferenceCompat()

        assertThat(switchPreference.isChecked).isFalse()
    }

    @Test
    fun click_defaultMediaControlsLockScreenEnabled_turnOff() {
        setMediaControlsResumeEnabled(true)

        val switchPreference = getSwitchPreferenceCompat().apply { performClick() }

        assertThat(switchPreference.isChecked).isFalse()
    }

    @Test
    fun click_defaultMediaControlsLockScreenDisabled_turnOn() {
        setMediaControlsResumeEnabled(false)

        val switchPreference = getSwitchPreferenceCompat().apply { performClick() }

        assertThat(switchPreference.isChecked).isTrue()
    }

    private fun getSwitchPreferenceCompat(): SwitchPreferenceCompat =
        preference.createAndBindWidget(appContext)


    private fun setMediaControlsResumeEnabled(value: Boolean) =
        preference.storage(appContext).setBoolean(key, value)
}
// LINT.ThenChange(MediaControlsPreferenceControllerTest.java)