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

import android.Manifest
import android.content.Context
import android.hardware.display.ColorDisplayManager
import android.provider.Settings
import androidx.test.core.app.ApplicationProvider
import com.android.settings.testutils.SettingsStoreRule
import com.android.settingslib.datastore.HandlerExecutor
import com.android.settingslib.datastore.KeyedObserver
import com.android.settingslib.datastore.Permissions
import com.android.settingslib.datastore.SettingsSecureStore
import com.android.settingslib.testutils.shadow.ShadowColorDisplayManager
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadow.api.Shadow
import org.robolectric.shadows.ShadowLooper

@Config(shadows = [ShadowColorDisplayManager::class])
@RunWith(RobolectricTestRunner::class)
class ExtraDimDataStoreTest {
    @get:Rule val settingsStoreRule = SettingsStoreRule()

    private lateinit var shadowColorDisplayManager: ShadowColorDisplayManager
    private lateinit var dataStore: ExtraDimDataStore
    private lateinit var appContext: Context

    @Before
    fun setUp() {
        appContext = ApplicationProvider.getApplicationContext()
        dataStore = ExtraDimDataStore(appContext)
        shadowColorDisplayManager =
            Shadow.extract(appContext.getSystemService(ColorDisplayManager::class.java))
    }

    @Test
    fun getValue_extraDimEnabled_returnTrue() {
        shadowColorDisplayManager.isReduceBrightColorsActivated = true
        assertThat(dataStore.getBoolean(ExtraDimDataStore.SETTING_KEY)).isTrue()
    }

    @Test
    fun getValue_extraDimDisabled_returnFalse() {
        shadowColorDisplayManager.isReduceBrightColorsActivated = false
        assertThat(dataStore.getBoolean(ExtraDimDataStore.SETTING_KEY)).isFalse()
    }

    @Test
    fun setValue_turnOnExtraDim_extraDimEnabled() {
        dataStore.setBoolean(ExtraDimDataStore.SETTING_KEY, true)
        assertThat(shadowColorDisplayManager.isReduceBrightColorsActivated).isTrue()
    }

    @Test
    fun setValue_turnOffExtraDim_extraDimDisabled() {
        dataStore.setBoolean(ExtraDimDataStore.SETTING_KEY, false)
        assertThat(shadowColorDisplayManager.isReduceBrightColorsActivated).isFalse()
    }

    @Test
    fun onFirstObserverAdded_hasObserverInSecureStore() {
        val observer = createTestObserver()
        dataStore.addObserver(ExtraDimDataStore.SETTING_KEY, observer, HandlerExecutor.main)

        assertThat(SettingsSecureStore.get(appContext).hasAnyObserver()).isTrue()
    }

    @Test
    fun onLastObserverRemoved_hasNoObserverInSecureStore() {
        val observer = createTestObserver()
        dataStore.addObserver(ExtraDimDataStore.SETTING_KEY, observer, HandlerExecutor.main)
        dataStore.removeObserver(ExtraDimDataStore.SETTING_KEY, observer)

        assertThat(SettingsSecureStore.get(appContext).hasAnyObserver()).isFalse()
    }

    @Test
    fun setSettingOutsideOfDataStore_hasObserver_notifyObserver() {
        var observerCalled = false
        val observer = createTestObserver { _, _ -> observerCalled = true }
        dataStore.addObserver(ExtraDimDataStore.SETTING_KEY, observer, HandlerExecutor.main)

        Settings.Secure.putInt(appContext.contentResolver, ExtraDimDataStore.SETTING_KEY, 1)
        ShadowLooper.idleMainLooper()

        assertThat(observerCalled).isTrue()
    }

    @Test
    fun getReadPermissions_returnEmpty() {
        assertThat(ExtraDimDataStore.getReadPermissions()).isEqualTo(Permissions.EMPTY)
    }

    @Test
    fun getWritePermissions_returnControlDisplayColorTransformsPermission() {
        assertThat(ExtraDimDataStore.getWritePermissions())
            .isEqualTo(Permissions.allOf(Manifest.permission.CONTROL_DISPLAY_COLOR_TRANSFORMS))
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
}
