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
import com.android.settings.accessibility.textreading.data.FontSizeDataStore.Companion.FONT_SCALE_DEF_VALUE
import com.android.settings.testutils.SettingsStoreRule
import com.android.settingslib.R as SettingsLibR
import com.android.settingslib.datastore.SettingsSecureStore
import com.android.settingslib.datastore.SettingsSystemStore
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Test for [FontSizeDataStore] */
@RunWith(RobolectricTestRunner::class)
class FontSizeDataStoreTest {
    @get:Rule val settingsStoreRule = SettingsStoreRule()
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private lateinit var dataStore: FontSizeDataStore
    private val settingsSecureStore = SettingsSecureStore.get(context)
    private val settingsSystemStore = SettingsSystemStore.get(context)

    @Before
    fun setUp() {
        dataStore =
            FontSizeDataStore(
                context = context,
                settingsSecure = settingsSecureStore,
                settingsSystem = settingsSystemStore,
            )
    }

    @Test
    fun init_setDefaultValues() {
        val defaultFontSize =
            Settings.System.getFloat(
                context.contentResolver,
                Settings.System.DEFAULT_DEVICE_FONT_SCALE,
                FONT_SCALE_DEF_VALUE,
            )
        assertThat(
                settingsSystemStore.getDefaultValue(
                    Settings.System.DEFAULT_DEVICE_FONT_SCALE,
                    Float::class.java,
                )
            )
            .isEqualTo(defaultFontSize)
        assertThat(
                settingsSystemStore.getDefaultValue(Settings.System.FONT_SCALE, Float::class.java)
            )
            .isEqualTo(defaultFontSize)
    }

    @Test
    fun getKeyValueStoreDelegate() {
        assertThat(dataStore.keyValueStoreDelegate).isEqualTo(settingsSystemStore)
    }

    @Test
    fun fontSizeData_loadCorrectFontSizeData() {
        val fontSizes: FloatArray =
            context.resources
                .getStringArray(SettingsLibR.array.entryvalues_font_size)
                .mapNotNull { it.toFloatOrNull() }
                .toFloatArray()
        val defaultFontSize =
            Settings.System.getFloat(
                context.contentResolver,
                Settings.System.DEFAULT_DEVICE_FONT_SCALE,
                FONT_SCALE_DEF_VALUE,
            )
        val expectedFontSize =
            FontSize(
                currentIndex = getDefaultFontSizeIndex(),
                values = fontSizes,
                defaultValue = defaultFontSize,
            )

        assertThat(dataStore.fontSizeData.value).isEqualTo(expectedFontSize)
    }

    @Test
    fun getInt_returnFontSizeCurrentIndex() {
        assertThat(dataStore.getInt("font_size"))
            .isEqualTo(dataStore.fontSizeData.value.currentIndex)
    }

    @Test
    fun setValue_updateFontSizeDataAndSettings() {
        val fontSizes: FloatArray =
            context.resources
                .getStringArray(SettingsLibR.array.entryvalues_font_size)
                .mapNotNull { it.toFloatOrNull() }
                .toFloatArray()
        val currentIndex = dataStore.fontSizeData.value.currentIndex
        val newIndex = (currentIndex + 1) % (dataStore.fontSizeData.value.values.size)

        dataStore.setInt("font_size", newIndex)

        assertThat(
                Settings.System.getFloat(
                    context.contentResolver,
                    Settings.System.FONT_SCALE,
                    FONT_SCALE_DEF_VALUE,
                )
            )
            .isEqualTo(fontSizes[newIndex])
        assertThat(dataStore.fontSizeData.value.currentIndex).isEqualTo(newIndex)
    }

    @Test
    fun setValue_fontScalingHasBeenChangedIsTrue() {
        dataStore.setInt("font_size", value = 0)

        assertThat(
                settingsSecureStore.getBoolean(
                    Settings.Secure.ACCESSIBILITY_FONT_SCALING_HAS_BEEN_CHANGED
                )
            )
            .isTrue()
    }

    @Test
    fun resetToDefault_resetFontSizeDataAndSettingsToDefault() {
        val defaultFontSize =
            Settings.System.getFloat(
                context.contentResolver,
                Settings.System.DEFAULT_DEVICE_FONT_SCALE,
                FONT_SCALE_DEF_VALUE,
            )
        val currentIndex = dataStore.fontSizeData.value.currentIndex
        val newIndex = (currentIndex + 1) % (dataStore.fontSizeData.value.values.size)

        dataStore.setInt("font_size", newIndex)
        dataStore.resetToDefault()

        assertThat(
                Settings.System.getFloat(
                    context.contentResolver,
                    Settings.System.FONT_SCALE,
                    FONT_SCALE_DEF_VALUE,
                )
            )
            .isEqualTo(defaultFontSize)
    }

    @Test
    fun getDefaultValue_returnsDefaultFontSizeIndex() {
        val defaultFontSizeIndex = getDefaultFontSizeIndex()

        assertThat(dataStore.getDefaultValue("key", Int::class.javaObjectType))
            .isEqualTo(defaultFontSizeIndex)
    }

    // Appfunction calls getValue with int type instead of getInt.
    // Calling getValue with int won't invoke getInt; where as getInt will invoke getValue.
    // Adding simple test to make sure that getValue is properly implemented
    @Test
    fun getValue_intType_equalsGetInt() {
        assertThat(dataStore.getValue("key", Int::class.javaObjectType))
            .isEqualTo(dataStore.getInt("key"))
    }

    // Appfunction calls setValue with int type instead of setInt.
    // Calling setValue with int won't invoke setInt; where as setInt will invoke setValue.
    // Adding simple test to make sure that setValue is properly implemented
    @Test
    fun setValue_intType_updatesValue() {
        val newValue = dataStore.fontSizeData.value.values.size - 1
        dataStore.setValue("key", Int::class.javaObjectType, newValue)

        assertThat(dataStore.getInt("key")).isEqualTo(newValue)
    }

    private fun getDefaultFontSizeIndex(): Int {
        val defaultFontSize =
            Settings.System.getFloat(
                context.contentResolver,
                Settings.System.DEFAULT_DEVICE_FONT_SCALE,
                FONT_SCALE_DEF_VALUE,
            )

        val fontSizes: FloatArray =
            context.resources
                .getStringArray(SettingsLibR.array.entryvalues_font_size)
                .mapNotNull { it.toFloatOrNull() }
                .toFloatArray()

        for (i in 0 until fontSizes.size) {
            if (fontSizes[i] == defaultFontSize) {
                return i
            }
        }
        return -1
    }
}
