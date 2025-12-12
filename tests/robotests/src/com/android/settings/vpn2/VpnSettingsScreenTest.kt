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

package com.android.settings.vpn2

import android.widget.TextView
import androidx.test.core.app.ApplicationProvider
import com.android.settings.Settings
import com.android.settings.dashboard.RestrictedDashboardFragment
import com.android.settings.flags.Flags
import com.android.settings.testutils2.SettingsCatalystTestCase
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.robolectric.annotation.Config
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements

@Config(shadows = [ShadowRestrictedDashboardFragment::class, ShadowUpdatePreferences::class])
class VpnSettingsScreenTest : SettingsCatalystTestCase() {
    override val preferenceScreenCreator = VpnSettingsScreen()

    override val flagName: String
        get() = Flags.FLAG_DEEPLINK_NETWORK_AND_INTERNET_25Q4

    @Test
    fun key_isEqualToStatic() {
        assertThat(preferenceScreenCreator.key).isEqualTo(VpnSettingsScreen.KEY)
    }

    @Test
    fun getLaunchIntent_correctActivity() {
        val underTest = preferenceScreenCreator.getLaunchIntent(appContext, null)

        assertThat(underTest.component?.className)
            .isEqualTo(Settings.VpnSettingsActivity::class.java.getName())
    }
}

@Implements(VpnSettings.UpdatePreferences::class)
class ShadowUpdatePreferences {
    @Implementation fun run() {}
}

@Implements(RestrictedDashboardFragment::class)
class ShadowRestrictedDashboardFragment {
    @Implementation
    fun getEmptyTextView(): TextView? {
        return TextView(ApplicationProvider.getApplicationContext())
    }
}
