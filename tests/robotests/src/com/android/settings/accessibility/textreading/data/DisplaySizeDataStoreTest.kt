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
import com.android.settings.testutils.SettingsStoreRule
import com.android.settingslib.display.DisplayDensityUtils
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnit
import org.mockito.junit.MockitoRule
import org.mockito.kotlin.any
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner

/** Test for [DisplaySizeDataStore] */
@RunWith(RobolectricTestRunner::class)
class DisplaySizeDataStoreTest {
    @get:Rule val settingsStoreRule = SettingsStoreRule()
    @get:Rule val mockitoRule: MockitoRule = MockitoJUnit.rule()
    @Mock private lateinit var mockDisplayDensityUtils: DisplayDensityUtils
    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Test
    fun displaySizeData_unableToGetData_useCurrentDensityWithOnlyOneAvailableValue() {
        val currentDensity = context.resources.displayMetrics.densityDpi
        val expectedDisplaySize =
            DisplaySize(
                currentIndex = 0,
                values = intArrayOf(currentDensity),
                defaultValue = currentDensity,
            )
        whenever(mockDisplayDensityUtils.currentIndex).thenReturn(-1)
        whenever(mockDisplayDensityUtils.values).thenReturn(null)

        val dataStore = DisplaySizeDataStore(context, mockDisplayDensityUtils)

        assertThat(dataStore.displaySizeData.value).isEqualTo(expectedDisplaySize)
    }

    @Test
    fun displaySizeData_loadCorrectDisplaySizeData() {
        val expectedDisplaySize =
            DisplaySize(currentIndex = 1, values = intArrayOf(356, 420, 490), defaultValue = 420)
        setupMockDisplayDensityUtils(
            expectedDisplaySize.currentIndex,
            expectedDisplaySize.values,
            expectedDisplaySize.defaultValue,
        )

        val dataStore = DisplaySizeDataStore(context, mockDisplayDensityUtils)

        assertThat(dataStore.displaySizeData.value).isEqualTo(expectedDisplaySize)
    }

    @Test
    fun getDefaultValue_returnTheIndexOfDefaultValue() {
        setupMockDisplayDensityUtils(
            currentIndex = 2,
            values = intArrayOf(356, 420, 490),
            defaultDensity = 420,
        )

        val dataStore = DisplaySizeDataStore(context, mockDisplayDensityUtils)

        assertThat(dataStore.getDefaultValue("display_size", Int::class.javaObjectType))
            .isEqualTo(1)
    }

    @Test
    fun setValue_currentValueMatchSetValue_dontCallDisplayDensityUtilsToSetDensity() {
        val currentIndex = 2
        setupMockDisplayDensityUtils(
            currentIndex = currentIndex,
            values = intArrayOf(356, 420, 490),
            defaultDensity = 420,
        )
        val dataStore = DisplaySizeDataStore(context, mockDisplayDensityUtils)
        dataStore.setInt("display_size", currentIndex)

        verify(mockDisplayDensityUtils, never()).clearForcedDisplayDensity()
        verify(mockDisplayDensityUtils, never()).setForcedDisplayDensity(any())
    }

    @Test
    fun setInt_updateValue_callDisplayDensityUtilsToSetDensity() {
        val currentIndex = 2
        setupMockDisplayDensityUtils(
            currentIndex = currentIndex,
            values = intArrayOf(356, 420, 490),
            defaultDensity = 420,
        )
        val dataStore = DisplaySizeDataStore(context, mockDisplayDensityUtils)
        dataStore.setInt("display_size", 0)

        verify(mockDisplayDensityUtils, never()).clearForcedDisplayDensity()
        verify(mockDisplayDensityUtils).setForcedDisplayDensity(0)
    }

    @Test
    fun resetToDefault_callDisplayDensityUtilsToReset() {
        val currentIndex = 2
        setupMockDisplayDensityUtils(
            currentIndex = currentIndex,
            values = intArrayOf(356, 420, 490),
            defaultDensity = 420,
        )
        val dataStore = DisplaySizeDataStore(context, mockDisplayDensityUtils)
        dataStore.resetToDefault()

        verify(mockDisplayDensityUtils).clearForcedDisplayDensity()
        verify(mockDisplayDensityUtils, never()).setForcedDisplayDensity(any())
    }

    // Appfunction calls setValue with int type instead of setInt.
    // Calling setValue with int won't invoke setInt; where as setInt will invoke setValue.
    // Adding simple test to make sure that setValue is properly implemented
    @Test
    fun setValue_updateValue_callDisplayDensityUtilsToSetDensity() {
        val currentIndex = 2
        setupMockDisplayDensityUtils(
            currentIndex = currentIndex,
            values = intArrayOf(356, 420, 490),
            defaultDensity = 420,
        )
        val dataStore = DisplaySizeDataStore(context, mockDisplayDensityUtils)
        dataStore.setValue("display_size", Int::class.javaObjectType, 0)

        verify(mockDisplayDensityUtils, never()).clearForcedDisplayDensity()
        verify(mockDisplayDensityUtils).setForcedDisplayDensity(0)
    }

    // Appfunction calls getValue with int type instead of getInt.
    // Calling getValue with int won't invoke getInt; where as getInt will invoke getValue.
    // Adding simple test to make sure that getValue is properly implemented
    @Test
    fun getValue_intType_equalsGetInt() {
        setupMockDisplayDensityUtils(
            currentIndex = 2,
            values = intArrayOf(356, 420, 490),
            defaultDensity = 420,
        )

        val dataStore = DisplaySizeDataStore(context, mockDisplayDensityUtils)
        assertThat(dataStore.getValue("display_size", Int::class.javaObjectType))
            .isEqualTo(dataStore.getInt("display_size"))
    }

    private fun setupMockDisplayDensityUtils(
        currentIndex: Int,
        values: IntArray,
        defaultDensity: Int,
    ) {
        whenever(mockDisplayDensityUtils.currentIndex).thenReturn(currentIndex)
        whenever(mockDisplayDensityUtils.values).thenReturn(values)
        whenever(mockDisplayDensityUtils.defaultDensity).thenReturn(defaultDensity)
    }
}
