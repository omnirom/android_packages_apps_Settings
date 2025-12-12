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
import androidx.test.core.app.ApplicationProvider
import com.android.settings.accessibility.textreading.ui.BoldTextPreference
import com.android.settings.testutils.SettingsStoreRule
import com.android.settingslib.datastore.SettingsSecureStore
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Test for [BoldTextDataStore] */
@RunWith(RobolectricTestRunner::class)
class BoldTextDataStoreTest {
    @get:Rule val settingsStoreRule = SettingsStoreRule()
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private lateinit var dataStore: BoldTextDataStore
    private val settingsStore = SettingsSecureStore.get(context)

    @Before
    fun setUp() {
        dataStore = BoldTextDataStore(context = context, settingsStore = settingsStore)
    }

    @Test
    fun getKeyValueStoreDelegate_equalsSettingsStore() {
        assertThat(dataStore.keyValueStoreDelegate).isEqualTo(settingsStore)
    }

    @Test
    fun getBoolean_boldTextOn_returnTrue() {
        settingsStore.setInt(BoldTextDataStore.KEY, BoldTextDataStore.BOLD_TEXT_ADJUSTMENT)

        assertThat(dataStore.getBoolean(BoldTextPreference.KEY)).isTrue()
    }

    @Test
    fun getBoolean_boldTextOff_returnFalse() {
        settingsStore.setInt(BoldTextDataStore.KEY, 0)

        assertThat(dataStore.getBoolean(BoldTextPreference.KEY)).isFalse()
    }

    @Test
    fun setBoolean_true_settingsUpdated() {
        dataStore.setBoolean(BoldTextDataStore.KEY, true)

        assertThat(settingsStore.getInt(BoldTextDataStore.KEY))
            .isEqualTo(BoldTextDataStore.BOLD_TEXT_ADJUSTMENT)
    }

    @Test
    fun setBoolean_false_settingsUpdated() {
        dataStore.setBoolean(BoldTextDataStore.KEY, false)

        assertThat(settingsStore.getInt(BoldTextDataStore.KEY)).isEqualTo(0)
    }

    // Appfunction calls setValue with boolean type instead of setBoolean.
    // Calling setValue with boolean won't invoke setBoolean; where as setBoolean will invoke
    // setValue.
    // Adding simple test to make sure that setValue is properly implemented
    @Test
    fun setValue_true_settingsUpdated() {
        dataStore.setValue(BoldTextDataStore.KEY, Boolean::class.javaObjectType, true)

        assertThat(settingsStore.getInt(BoldTextDataStore.KEY))
            .isEqualTo(BoldTextDataStore.BOLD_TEXT_ADJUSTMENT)
    }

    // Appfunction calls getValue with boolean type instead of getBoolean.
    // Calling getValue with boolean won't invoke getBoolean; where as getBoolean will invoke
    // getValue.
    // Adding simple test to make sure that getValue is properly implemented
    @Test
    fun getValue_boldTextOn_returnTrue() {
        settingsStore.setInt(BoldTextDataStore.KEY, BoldTextDataStore.BOLD_TEXT_ADJUSTMENT)

        assertThat(dataStore.getValue(BoldTextPreference.KEY, Boolean::class.javaObjectType))
            .isTrue()
    }

    @Test
    fun getDefaultValue_returnFalse() {
        assertThat(dataStore.getDefaultValue(BoldTextPreference.KEY, Boolean::class.javaObjectType))
            .isFalse()
    }
}
