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

package com.android.settings.accessibility.shared.data

import android.content.ComponentName
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.android.settings.testutils.SettingsStoreRule
import com.android.settingslib.datastore.SettingsSecureStore
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Tests for [ToggleFeatureDataStore]. */
@RunWith(RobolectricTestRunner::class)
class ToggleFeatureDataStoreTest {
    @get:Rule val settingStoreRule = SettingsStoreRule()

    private val appContext: Context = ApplicationProvider.getApplicationContext()
    private val store =
        ToggleFeatureDataStore(
            ComponentName(appContext, "ToggleFeatureDataStoreTest"),
            SettingsSecureStore.get(appContext),
        )

    @Test
    fun setValue_turnOn_updateSetting() {
        store.setBoolean(SETTING_KEY, true)

        assertThat(store.getBoolean(SETTING_KEY)).isTrue()
    }

    @Test
    fun setValue_turnOff_updateSetting() {
        store.setBoolean(SETTING_KEY, false)

        assertThat(store.getBoolean(SETTING_KEY)).isFalse()
    }

    companion object {
        private const val SETTING_KEY = "fake_setting"
    }
}
