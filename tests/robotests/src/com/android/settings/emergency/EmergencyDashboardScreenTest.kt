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

package com.android.settings.emergency

import android.content.ContextWrapper
import android.content.res.Resources
import android.platform.test.annotations.DisableFlags
import android.platform.test.annotations.EnableFlags
import com.android.settings.R
import com.android.settings.flags.Flags
import com.android.settings.testutils2.SettingsCatalystTestCase
import com.android.settingslib.widget.theme.flags.Flags as LibFlags
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub

class EmergencyDashboardScreenTest : SettingsCatalystTestCase() {
    private val mockResources = mock<Resources>()
    private val context =
        object : ContextWrapper(appContext) {
            override fun getResources(): Resources = mockResources
        }

    override val flagName: String
        get() = Flags.FLAG_DEEPLINK_OTHERS_25Q4

    override val preferenceScreenCreator = EmergencyDashboardScreen()

    @Test
    @EnableFlags(LibFlags.FLAG_IS_EXPRESSIVE_DESIGN_ENABLED)
    fun getIcon_enabledExpressive_returnsExpressiveIcon() {
        assertThat(preferenceScreenCreator.getIcon(appContext))
            .isEqualTo(R.drawable.ic_homepage_emergency)
    }

    @Test
    @DisableFlags(LibFlags.FLAG_IS_EXPRESSIVE_DESIGN_ENABLED)
    fun getIcon_disabledExpressive_returnsNonExpressiveIcon() {
        assertThat(preferenceScreenCreator.getIcon(appContext))
            .isEqualTo(R.drawable.ic_settings_emergency_filled)
    }

    @Test
    fun isAvailable_configIsTrue_returnsTrue() {
        mockResources.stub { on { getBoolean(anyInt()) } doReturn true }

        assertThat(preferenceScreenCreator.isAvailable(context)).isTrue()
    }

    @Test
    fun isAvailable_configIsFalse_returnsFalse() {
        mockResources.stub { on { getBoolean(anyInt()) } doReturn false }

        assertThat(preferenceScreenCreator.isAvailable(context)).isFalse()
    }
}
