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

package com.android.settings.deviceinfo

import android.content.ContextWrapper
import android.content.res.Resources
import android.os.Build
import android.provider.Settings.Global.DEVICE_NAME
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.R
import com.android.settingslib.datastore.SettingsGlobalStore
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub

@RunWith(AndroidJUnit4::class)
class DeviceNamePreferenceTest {

    private val mockResources = mock<Resources>()
    private val context =
        object : ContextWrapper(ApplicationProvider.getApplicationContext()) {
            override fun getResources(): Resources = mockResources
        }

    private val deviceNamePreference = DeviceNamePreference(context)

    @Test
    fun isAvailable_configIsShowDeviceName_returnsTrue() {
        mockResources.stub { on { getBoolean(R.bool.config_show_device_name) } doReturn true }

        assertThat(deviceNamePreference.isAvailable(context)).isTrue()
    }

    @Test
    fun isAvailable_configIsNotShowDeviceName_returnsFalse() {
        mockResources.stub { on { getBoolean(R.bool.config_show_device_name) } doReturn false }

        assertThat(deviceNamePreference.isAvailable(context)).isFalse()
    }

    @Test
    fun getSummary_isGlobalDeviceNameNull_returnsBuildModel() {
        SettingsGlobalStore.get(context).setString(DEVICE_NAME, null)

        assertThat(deviceNamePreference.getSummary(context)).isEqualTo(Build.MODEL)
    }

    @Test
    fun getSummary_setGlobalDeviceNameAsSpecificName_returnsSettedName() {
        SettingsGlobalStore.get(context).setString(DEVICE_NAME, TEST_DEVICE_NAME)

        assertThat(deviceNamePreference.getSummary(context)).isEqualTo(TEST_DEVICE_NAME)
    }

    companion object {
        private val TEST_DEVICE_NAME = "Terepan"
    }
}
