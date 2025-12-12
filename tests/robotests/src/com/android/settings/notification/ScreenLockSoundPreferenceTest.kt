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
package com.android.settings.notification

import android.content.ContextWrapper
import android.content.res.Resources
import android.provider.Settings.System.LOCKSCREEN_SOUNDS_ENABLED
import androidx.preference.SwitchPreferenceCompat
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settingslib.datastore.SettingsSystemStore
import com.android.settingslib.preference.createAndBindWidget
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub

// LINT.IfChange
@RunWith(AndroidJUnit4::class)
class ScreenLockSoundPreferenceTest {
    private val mockResources = mock<Resources>()

    private val context =
        object : ContextWrapper(ApplicationProvider.getApplicationContext()) {
            override fun getResources(): Resources = mockResources
        }

    private val screenLockSoundPreference = ScreenLockSoundPreference()

    @Test
    fun isAvailable_configTrue_shouldReturnTrue() {
        mockResources.stub { on { getBoolean(anyInt()) } doReturn true }

        assertThat(screenLockSoundPreference.isAvailable(context)).isTrue()
    }

    @Test
    fun isAvailable_configFalse_shouldReturnFalse() {
        mockResources.stub { on { getBoolean(anyInt()) } doReturn false }

        assertThat(screenLockSoundPreference.isAvailable(context)).isFalse()
    }

    @Test
    fun performClick_shouldPreferenceChangeToChecked() {
        enableScreenLockSound(false)

        val preference = getSwitchPreference().apply { performClick() }

        assertThat(preference.isChecked).isTrue()
    }

    @Test
    fun performClick_shouldPreferenceChangeToUnchecked() {
        enableScreenLockSound(true)

        val preference = getSwitchPreference().apply { performClick() }

        assertThat(preference.isChecked).isFalse()
    }

    @Test
    fun screenLockSoundEnabled_shouldCheckedPreference() {
        enableScreenLockSound(true)

        assertThat(getSwitchPreference().isChecked).isTrue()
    }

    @Test
    fun screenLockSoundDisabled_shouldUncheckedPreference() {
        enableScreenLockSound(false)

        assertThat(getSwitchPreference().isChecked).isFalse()
    }

    @Test
    fun noValueInStorage_shouldCheckedPreference() {
        enableScreenLockSound(null)

        assertThat(getSwitchPreference().isChecked).isTrue()
    }

    private fun getSwitchPreference(): SwitchPreferenceCompat =
        screenLockSoundPreference.createAndBindWidget(context)

    private fun enableScreenLockSound(enabled: Boolean?) =
        SettingsSystemStore.get(context).setBoolean(LOCKSCREEN_SOUNDS_ENABLED, enabled)
}
// LINT.ThenChange(ScreenLockSoundPreferenceControllerTest.java)
