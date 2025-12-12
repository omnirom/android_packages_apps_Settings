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
import android.provider.Settings.Secure.NAVIGATIONBAR_KEY_ORDER
import androidx.test.core.app.ApplicationProvider
import com.android.settings.testutils.SettingsStoreRule
import com.android.settingslib.datastore.SettingsSecureStore
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ButtonNavigationSettingsOrderStoreTest {
    @get:Rule val settingsStoreRule = SettingsStoreRule()
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private lateinit var orderStore: ButtonNavigationSettingsOrderStore
    private val settingsStore = SettingsSecureStore.get(context)

    @Before
    fun setup() {
        orderStore = ButtonNavigationSettingsOrderStore(context)
    }

    @Test
    fun defaultValue_matchesDefaultOrder() {
        settingsStore.setInt(NAVIGATIONBAR_KEY_ORDER, 0)

        assertThat(orderStore.getBoolean(DefaultButtonNavigationSettingsOrderPreference.KEY))
            .isEqualTo(true)
        assertThat(orderStore.getBoolean(ReverseButtonNavigationSettingsOrderPreference.KEY))
            .isEqualTo(false)
    }

    @Test
    fun reverseValue_matchesReverseOrder() {
        settingsStore.setInt(NAVIGATIONBAR_KEY_ORDER, 1)

        assertThat(orderStore.getBoolean(DefaultButtonNavigationSettingsOrderPreference.KEY))
            .isEqualTo(false)
        assertThat(orderStore.getBoolean(ReverseButtonNavigationSettingsOrderPreference.KEY))
            .isEqualTo(true)
    }

    @Test
    fun setDefault_writesDefaultSetting() {
        orderStore.setBoolean(DefaultButtonNavigationSettingsOrderPreference.KEY, true)
        assertThat(settingsStore.getBoolean(NAVIGATIONBAR_KEY_ORDER)).isEqualTo(false)
    }

    @Test
    fun setReverse_writesReverseSetting() {
        orderStore.setBoolean(ReverseButtonNavigationSettingsOrderPreference.KEY, true)
        assertThat(settingsStore.getBoolean(NAVIGATIONBAR_KEY_ORDER)).isEqualTo(true)
    }
}
