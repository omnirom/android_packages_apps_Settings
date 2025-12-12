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
package com.android.settings.fuelgauge.batteryusage

import android.content.Context
import com.android.settings.flags.Flags
import com.android.settings.testutils2.SettingsCatalystTestCase
import com.android.settingslib.core.AbstractPreferenceController
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.robolectric.annotation.Config
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements

class PowerUsageAdvancedScreenTest : SettingsCatalystTestCase() {

    override val preferenceScreenCreator = PowerUsageAdvancedScreen()

    override val flagName: String
        get() = Flags.FLAG_DEEPLINK_BATTERY_25Q4

    @Test
    fun key() {
        assertThat(preferenceScreenCreator.key).isEqualTo(PowerUsageAdvancedScreen.KEY)
    }

    @Config(shadows = [ShadowPowerUsageAdvanced::class])
    @Test
    override fun migration() {
        super.migration()
    }

    @Implements(PowerUsageAdvanced::class)
    class ShadowPowerUsageAdvanced {
        @Implementation
        protected fun createPreferenceControllers(
            context: Context?
        ): MutableList<AbstractPreferenceController?>? = ArrayList<AbstractPreferenceController?>()
    }
}
