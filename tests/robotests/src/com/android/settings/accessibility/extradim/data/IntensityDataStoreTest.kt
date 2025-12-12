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

package com.android.settings.accessibility.extradim.data

import android.content.Context
import android.provider.Settings
import androidx.test.core.app.ApplicationProvider
import com.android.settings.accessibility.extradim.data.IntensityDataStore.Companion.MAX_VALUE
import com.android.settings.testutils.SettingsStoreRule
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Tests for [IntensityDataStore]. */
@RunWith(RobolectricTestRunner::class)
class IntensityDataStoreTest {
    @get:Rule val settingsStoreRule = SettingsStoreRule()
    private lateinit var context: Context
    private lateinit var intensityDataStore: IntensityDataStore

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        intensityDataStore = IntensityDataStore(context)
    }

    @Test
    fun getDefaultValue_returnMaxValue() {
        val defaultValue = intensityDataStore.getDefaultValue("prefKey", Int::class.java)
        assertThat(defaultValue).isEqualTo(MAX_VALUE)
    }

    @Test
    fun getValue_settingValue70_returns30() {
        Settings.Secure.putInt(
            context.contentResolver,
            Settings.Secure.REDUCE_BRIGHT_COLORS_LEVEL,
            70,
        )

        assertThat(intensityDataStore.getInt("prefKey")).isEqualTo(30)
    }

    @Test
    fun setValue_value70_setSettingValue30() {
        intensityDataStore.setInt("prefKey", 70)

        assertThat(
                Settings.Secure.getInt(
                    context.contentResolver,
                    Settings.Secure.REDUCE_BRIGHT_COLORS_LEVEL,
                )
            )
            .isEqualTo(30)
    }
}
