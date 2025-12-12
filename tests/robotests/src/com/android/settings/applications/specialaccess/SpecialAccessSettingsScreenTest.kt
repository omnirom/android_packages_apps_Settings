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

package com.android.settings.applications.specialaccess

import androidx.lifecycle.LifecycleOwner
import com.android.settings.Settings
import com.android.settings.flags.Flags
import com.android.settings.testutils2.SettingsCatalystTestCase
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.robolectric.annotation.Config
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements

class SpecialAccessSettingsScreenTest : SettingsCatalystTestCase() {

    override val flagName: String
        get() = Flags.FLAG_DEEPLINK_APPS_25Q4

    override val preferenceScreenCreator = SpecialAccessSettingsScreen()

    @Test
    fun key_isEqualToStatic() {
        assertThat(preferenceScreenCreator.key).isEqualTo(SpecialAccessSettingsScreen.KEY)
    }

    @Test
    fun getLaunchIntent_correctActivity() {
        val underTest = preferenceScreenCreator.getLaunchIntent(appContext, null)

        assertThat(underTest.component?.className)
            .isEqualTo(Settings.SpecialAccessSettingsActivity::class.java.getName())
    }

    @Test
    @Config(shadows = [ShadowDataSaverController::class])
    override fun migration() {
        super.migration()
    }
}

@Implements(DataSaverController::class)
class ShadowDataSaverController {
    @Implementation fun onViewCreated(viewLifecycleOwner: LifecycleOwner) {}
}
