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

package com.android.settings.accessibility.detail.a11yservice.data

import android.content.ComponentName
import android.content.Context
import android.os.UserHandle
import android.provider.Settings
import androidx.test.core.app.ApplicationProvider
import com.android.settings.accessibility.data.AccessibilityRepositoryProvider
import com.android.settings.testutils.AccessibilityTestUtils
import com.android.settings.testutils.SettingsStoreRule
import com.android.settingslib.accessibility.AccessibilityUtils
import com.android.settingslib.datastore.DataChangeReason
import com.android.settingslib.datastore.HandlerExecutor
import com.android.settingslib.datastore.KeyedObserver
import com.android.settingslib.datastore.SettingsSecureStore
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowLooper

/** Tests for [UseServiceDataStore]. */
@RunWith(RobolectricTestRunner::class)
class UseServiceDataStoreTest {
    @get:Rule val settingsStoreRule = SettingsStoreRule()
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val fakeComponentName = ComponentName("FakePackage", "StandardA11yService")
    private val serviceInfo =
        AccessibilityTestUtils.createAccessibilityServiceInfo(
            context,
            fakeComponentName,
            /* isAlwaysOnService= */ false,
        )
    private val settingsSecureStore = SettingsSecureStore.get(context)
    private val dataStore = UseServiceDataStore(context, serviceInfo, settingsSecureStore)
    private val settingKey = Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
    private val prefKey = "any pref key"

    @After
    fun cleanUp() {
        AccessibilityRepositoryProvider.resetInstanceForTesting()
    }

    @Test
    fun getDefaultValue_returnFalse() {
        assertThat(dataStore.getDefaultValue(prefKey, Boolean::class.javaObjectType)).isFalse()
    }

    @Test
    fun getValue_serviceOn_returnTrue() {
        settingsSecureStore.setValue(
            settingKey,
            String::class.javaObjectType,
            fakeComponentName.flattenToString(),
        )

        assertThat(dataStore.getBoolean(prefKey)).isTrue()
    }

    @Test
    fun getValue_serviceOff_returnFalse() {
        settingsSecureStore.setValue(settingKey, String::class.javaObjectType, null)

        assertThat(dataStore.getBoolean(prefKey)).isFalse()
    }

    @Test
    fun setValue_turnOffService_removeServiceFromSetting() {
        settingsSecureStore.setValue(
            settingKey,
            String::class.javaObjectType,
            fakeComponentName.flattenToString(),
        )

        dataStore.setBoolean(prefKey, false)

        assertThat(
                AccessibilityUtils.getEnabledServicesFromSettings(context, UserHandle.myUserId())
            )
            .doesNotContain(fakeComponentName)
    }

    @Test
    fun setValue_turnOnService_addServiceToSetting() {
        settingsSecureStore.setValue(settingKey, String::class.javaObjectType, null)

        dataStore.setBoolean(prefKey, true)

        assertThat(
                AccessibilityUtils.getEnabledServicesFromSettings(context, UserHandle.myUserId())
            )
            .contains(fakeComponentName)
    }

    @Test
    fun onFirstObserverAdded_addObserverToSettingStore() {
        val observer = KeyedObserver<String> { key, reason -> }
        dataStore.addObserver(prefKey, observer, HandlerExecutor.main)
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()

        assertThat(dataStore.hasAnyObserver()).isTrue()
        assertThat(settingsSecureStore.hasAnyObserver()).isTrue()
    }

    @Test
    fun onLastObserverRemoved_removeObserver() {
        val observer = KeyedObserver<String> { key, reason -> }
        dataStore.addObserver(prefKey, observer, HandlerExecutor.main)
        dataStore.removeObserver(prefKey, observer)
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()

        assertThat(dataStore.hasAnyObserver()).isFalse()
        assertThat(settingsSecureStore.hasAnyObserver()).isFalse()
    }

    @Test
    fun addObserver_settingChanged_notifyChange() {
        var observerCalled = false
        val observer = KeyedObserver<String> { key, reason -> observerCalled = true }
        dataStore.addObserver(prefKey, observer, HandlerExecutor.main)

        settingsSecureStore.notifyChange(settingKey, DataChangeReason.UPDATE)
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()

        assertThat(observerCalled).isTrue()
    }
}
