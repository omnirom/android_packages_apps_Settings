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

package com.android.settings.accessibility.colorcorrection.data

import android.content.Context
import android.provider.Settings
import androidx.test.core.app.ApplicationProvider
import com.android.settings.testutils.SettingsStoreRule
import com.android.settingslib.datastore.HandlerExecutor
import com.android.settingslib.datastore.KeyedObserver
import com.android.settingslib.datastore.SettingsSecureStore
import com.google.common.truth.Truth.assertThat
import com.google.testing.junit.testparameterinjector.TestParameters
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestParameterInjector
import org.robolectric.shadows.ShadowLooper

@RunWith(RobolectricTestParameterInjector::class)
class ColorCorrectionModeDataStoreTest {
    @get:Rule val settingsStoreRule = SettingsStoreRule()
    private lateinit var appContext: Context
    private lateinit var dataStore: ColorCorrectionModeDataStore

    @Before
    fun setUp() {
        appContext = ApplicationProvider.getApplicationContext()
        dataStore = ColorCorrectionModeDataStore(appContext)
    }

    @TestParameters(
        value =
            [
                "{mode: $DEUTERANOMALY_MODE_PREF_KEY, value: $DEUTERANOMALY_MODE_VALUE}",
                "{mode: $PROTANOMALY_MODE_PREF_KEY, value: $PROTANOMALY_MODE_VALUE}",
                "{mode: $TRITANOMALY_MODE_PREF_KEY, value: $TRITANOMALY_MODE_VALUE}",
                "{mode: $GRAYSCALE_MODE_PREF_KEY, value: $GRAYSCALE_MODE_VALUE}",
            ]
    )
    @Test
    fun setMode_settingUpdated(mode: String, value: Int) {
        dataStore.setBoolean(mode, true)

        assertThat(
                Settings.Secure.getInt(
                    appContext.contentResolver,
                    ColorCorrectionModeDataStore.KEY,
                    0,
                )
            )
            .isEqualTo(value)
    }

    @TestParameters(
        value =
            [
                "{mode: $DEUTERANOMALY_MODE_PREF_KEY}",
                "{mode: $PROTANOMALY_MODE_PREF_KEY}",
                "{mode: $TRITANOMALY_MODE_PREF_KEY}",
                "{mode: $GRAYSCALE_MODE_PREF_KEY}",
            ]
    )
    @Test
    fun getMode_currentMode_returnsTrueWhenGetValueForCurrentMode_falseOtherwise(mode: String) {
        dataStore.setBoolean(mode, true)

        assertThat(dataStore.getBoolean(DEUTERANOMALY_MODE_PREF_KEY))
            .isEqualTo(mode == DEUTERANOMALY_MODE_PREF_KEY)
        assertThat(dataStore.getBoolean(PROTANOMALY_MODE_PREF_KEY))
            .isEqualTo(mode == PROTANOMALY_MODE_PREF_KEY)
        assertThat(dataStore.getBoolean(TRITANOMALY_MODE_PREF_KEY))
            .isEqualTo(mode == TRITANOMALY_MODE_PREF_KEY)
        assertThat(dataStore.getBoolean(GRAYSCALE_MODE_PREF_KEY))
            .isEqualTo(mode == GRAYSCALE_MODE_PREF_KEY)
    }

    @Test
    fun setSettingOutsideOfDataStore_hasObserver_notifyObserver() {
        var observerCalled = false
        val observer = createTestObserver { _, _ -> observerCalled = true }
        dataStore.addObserver(DEUTERANOMALY_MODE_PREF_KEY, observer, HandlerExecutor.main)

        Settings.Secure.putInt(
            appContext.contentResolver,
            ColorCorrectionModeDataStore.KEY,
            DEUTERANOMALY_MODE_VALUE,
        )
        ShadowLooper.idleMainLooper()

        assertThat(observerCalled).isTrue()
    }

    @Test
    fun onFirstObserverAdded_hasObserverInSecureStore() {
        val observer = createTestObserver()
        dataStore.addObserver(DEUTERANOMALY_MODE_PREF_KEY, observer, HandlerExecutor.main)

        assertThat(SettingsSecureStore.get(appContext).hasAnyObserver()).isTrue()
    }

    @Test
    fun onLastObserverRemoved_hasNoObserverInSecureStore() {
        val observer = createTestObserver()
        dataStore.addObserver(DEUTERANOMALY_MODE_PREF_KEY, observer, HandlerExecutor.main)
        dataStore.removeObserver(DEUTERANOMALY_MODE_PREF_KEY, observer)

        assertThat(SettingsSecureStore.get(appContext).hasAnyObserver()).isFalse()
    }

    private fun createTestObserver(
        onKeyChangedAction: (key: String, reason: Int) -> Unit = { _, _ -> }
    ): KeyedObserver<String> {
        return object : KeyedObserver<String> {
            override fun onKeyChanged(key: String, reason: Int) {
                onKeyChangedAction(key, reason)
            }
        }
    }

    companion object {
        /* pref keys defined in R.array.daltonizer_mode_keys */
        private const val DEUTERANOMALY_MODE_PREF_KEY = "daltonizer_mode_deuteranomaly"
        private const val PROTANOMALY_MODE_PREF_KEY = "daltonizer_mode_protanomaly"
        private const val TRITANOMALY_MODE_PREF_KEY = "daltonizer_mode_tritanomaly"
        private const val GRAYSCALE_MODE_PREF_KEY = "daltonizer_mode_grayscale"
        /* pref keys defined in R.array.daltonizer_mode_values */
        private const val DEUTERANOMALY_MODE_VALUE = 12
        private const val PROTANOMALY_MODE_VALUE = 11
        private const val TRITANOMALY_MODE_VALUE = 13
        private const val GRAYSCALE_MODE_VALUE = 0
    }
}
