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

package com.android.settings.system

import android.platform.test.annotations.DisableFlags
import android.platform.test.annotations.EnableFlags
import com.android.settings.R
import com.android.settings.flags.Flags
import com.android.settings.testutils2.SettingsCatalystTestCase
import com.android.settingslib.widget.theme.flags.Flags as LibFlags
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class SystemDashboardScreenTest : SettingsCatalystTestCase() {
    override val flagName: String
        get() = Flags.FLAG_DEEPLINK_SYSTEM_25Q4

    override val preferenceScreenCreator = SystemDashboardScreen()

    @Test
    @EnableFlags(LibFlags.FLAG_IS_EXPRESSIVE_DESIGN_ENABLED)
    fun getIcon_enabledExpressive_returnsExpressiveIcon() {
        assertThat(preferenceScreenCreator.getIcon(appContext))
            .isEqualTo(R.drawable.ic_homepage_system_dashboard)
    }

    @Test
    @DisableFlags(LibFlags.FLAG_IS_EXPRESSIVE_DESIGN_ENABLED)
    fun getIcon_disabledExpressive_returnsNonExpressiveIcon() {
        assertThat(preferenceScreenCreator.getIcon(appContext))
            .isEqualTo(R.drawable.ic_settings_system_dashboard_filled)
    }
}
