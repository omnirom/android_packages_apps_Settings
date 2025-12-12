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

package com.android.settings.datetime

import android.app.time.TimeCapabilitiesAndConfig
import android.app.time.TimeManager
import android.app.time.TimeZoneCapabilitiesAndConfig
import com.android.settings.Settings
import com.android.settings.flags.Flags
import com.android.settings.testutils2.SettingsCatalystTestCase
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements
import org.robolectric.shadow.api.Shadow

@Config(shadows = [ShadowTimeManager::class])
class DateTimeSettingsScreenTest : SettingsCatalystTestCase() {

    override val flagName: String
        get() = Flags.FLAG_DEEPLINK_SYSTEM_25Q4

    override val preferenceScreenCreator = DateTimeSettingsScreen()

    @Test
    fun key_isEqualToStatic() {
        assertThat(preferenceScreenCreator.key).isEqualTo(DateTimeSettingsScreen.KEY)
    }

    @Test
    fun getSummary_isNotNullOrEmpty() {
        assertThat(preferenceScreenCreator.getSummary(appContext).isNullOrEmpty()).isFalse()
    }

    @Test
    fun getLaunchIntent_correctActivity() {
        val underTest = preferenceScreenCreator.getLaunchIntent(appContext, null)

        assertThat(underTest.component?.className)
            .isEqualTo(Settings.DateTimeSettingsActivity::class.java.getName())
    }

    @Test
    override fun migration() {
        ShadowTimeManager.getShadow()
            .setTimeCapabilitiesAndConfigInstance(
                DatePreferenceControllerTest.createCapabilitiesAndConfig(true)
            )
        ShadowTimeManager.getShadow()
            .setTimeZoneCapabilitiesAndConfig(
                TimeZonePreferenceControllerTest.createCapabilitiesAndConfig(true)
            )
        super.migration()
    }
}

@Implements(TimeManager::class)
class ShadowTimeManager {
    private var capabilitiesAndConfig: TimeCapabilitiesAndConfig? = null
    private var timeZoneCapabilitiesAndConfig: TimeZoneCapabilitiesAndConfig? = null

    @Implementation
    fun getTimeCapabilitiesAndConfig(): TimeCapabilitiesAndConfig? {
        return capabilitiesAndConfig
    }

    @Implementation
    fun getTimeZoneCapabilitiesAndConfig(): TimeZoneCapabilitiesAndConfig? {
        return timeZoneCapabilitiesAndConfig
    }

    fun setTimeCapabilitiesAndConfigInstance(instance: TimeCapabilitiesAndConfig?) {
        this.capabilitiesAndConfig = instance
    }

    fun setTimeZoneCapabilitiesAndConfig(instance: TimeZoneCapabilitiesAndConfig?) {
        this.timeZoneCapabilitiesAndConfig = instance
    }

    companion object {
        fun getShadow(): ShadowTimeManager {
            return Shadow.extract(
                RuntimeEnvironment.getApplication().getSystemService(TimeManager::class.java)
            )
        }
    }
}
