/*
 * Copyright (C) 2024 The Android Open Source Project
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

package com.android.settings.deviceinfo.aboutphone

import android.content.pm.UserInfo
import android.os.Build
import android.provider.Settings.Global.DEVICE_NAME
import androidx.preference.Preference
import com.android.settings.deviceinfo.simstatus.SimEidInfoPreference
import com.android.settings.flags.Flags
import com.android.settings.testutils.shadow.ShadowUserManager
import com.android.settings.testutils2.SettingsCatalystTestCase
import com.android.settingslib.datastore.SettingsGlobalStore
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.mockito.kotlin.mock
import org.robolectric.annotation.Config

@Config(shadows = [ShadowUserManager::class])
class MyDeviceInfoScreenTest : SettingsCatalystTestCase() {
    override val preferenceScreenCreator = MyDeviceInfoScreen()

    override val flagName: String
        get() = Flags.FLAG_CATALYST_MY_DEVICE_INFO_PREF_SCREEN

    override fun getPreferenceClass(preference: Preference) =
        when (preference) {
            is SimEidInfoPreference -> Preference::class.java // Preference is used after migration
            else -> super.getPreferenceClass(preference)
        }

    @Test
    override fun migration() {
        ShadowUserManager.getShadow().addAliveUser(mock<UserInfo>())
        super.migration()
    }

    @Test
    fun getSummary_deviceNameNotSet_shouldReturnDeviceModel() {
        SettingsGlobalStore.get(appContext).setString(DEVICE_NAME, null)

        assertThat(preferenceScreenCreator.getSummary(appContext)).isEqualTo(Build.MODEL)
    }

    @Test
    fun getSummary_deviceNameSet_shouldReturnDeviceName() {
        SettingsGlobalStore.get(appContext).setString(DEVICE_NAME, "Test")

        assertThat(preferenceScreenCreator.getSummary(appContext)).isEqualTo("Test")
    }
}
