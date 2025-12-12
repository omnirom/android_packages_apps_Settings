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

package com.android.settings.accessibility.textreading.data

import android.content.Context
import android.provider.Settings
import androidx.test.core.app.ApplicationProvider
import com.android.settings.accessibility.HighContrastTextMigrationReceiver
import com.android.settingslib.datastore.KeyValueStore
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnit
import org.mockito.junit.MockitoRule
import org.mockito.kotlin.verify
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class OutlineTextDataStoreTest {
    @get:Rule val mockito: MockitoRule = MockitoJUnit.rule()

    @Mock lateinit var mockSettingsStore: KeyValueStore
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private lateinit var dataStore: OutlineTextDataStore

    @Before
    fun setUp() {
        dataStore = OutlineTextDataStore(context = context, settingsStore = mockSettingsStore)
    }

    @Test
    fun getKeyValueStoreDelegate_equalsSettingsStore() {
        assertThat(dataStore.keyValueStoreDelegate).isEqualTo(mockSettingsStore)
    }

    @Test
    fun setValue_callsSettingsStoreSetValue() {
        dataStore.setValue("key", Boolean::class.javaObjectType, true)

        verify(mockSettingsStore)
            .setBoolean(Settings.Secure.ACCESSIBILITY_HIGH_TEXT_CONTRAST_ENABLED, true)
    }

    @Test
    fun setValue_setHctRectPromptStatusToPromptUnnecessary() {
        dataStore.setValue("key", Boolean::class.javaObjectType, true)

        verify(mockSettingsStore)
            .setInt(
                Settings.Secure.ACCESSIBILITY_HCT_RECT_PROMPT_STATUS,
                HighContrastTextMigrationReceiver.PromptState.PROMPT_UNNECESSARY,
            )
    }

    @Test
    fun getDefaultValue_returnFalse() {
        assertThat(dataStore.getDefaultValue("key", Boolean::class.javaObjectType)).isFalse()
    }
}
