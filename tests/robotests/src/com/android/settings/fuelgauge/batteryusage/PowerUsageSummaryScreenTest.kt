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
package com.android.settings.fuelgauge.batteryusage

import android.content.ContextWrapper
import android.content.res.Resources
import android.platform.test.annotations.EnableFlags
import androidx.fragment.app.testing.FragmentScenario
import androidx.preference.PreferenceFragmentCompat
import com.android.settings.R
import com.android.settings.flags.Flags
import com.android.settings.testutils.shadow.ShadowUtils
import com.android.settings.testutils2.SettingsCatalystTestCase
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Test
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub
import org.robolectric.annotation.Config

@Config(shadows = [ShadowUtils::class])
class PowerUsageSummaryScreenTest : SettingsCatalystTestCase() {

    override val preferenceScreenCreator = PowerUsageSummaryScreen()

    override val flagName: String
        get() = Flags.FLAG_CATALYST_POWER_USAGE_SUMMARY_SCREEN

    private val mockResources = mock<Resources>()

    private val context =
        object : ContextWrapper(appContext) {
            override fun getResources(): Resources = mockResources
        }

    @After
    fun tearDown() {
        ShadowUtils.reset()
    }

    @Test
    fun isAvailable_configTrue_shouldReturnTrue() {
        mockResources.stub { on { getBoolean(anyInt()) } doReturn true }

        assertThat(preferenceScreenCreator.isAvailable(context)).isTrue()
    }

    @Test
    fun isAvailable_configFalse_shouldReturnFalse() {
        mockResources.stub { on { getBoolean(anyInt()) } doReturn false }

        assertThat(preferenceScreenCreator.isAvailable(context)).isFalse()
    }

    @Test
    @EnableFlags(com.android.settingslib.widget.theme.flags.Flags.FLAG_IS_EXPRESSIVE_DESIGN_ENABLED)
    fun getIcon_isExpressiveTheme() {
        assertThat(preferenceScreenCreator.getIcon(context))
            .isEqualTo(R.drawable.ic_homepage_battery)
    }

    @Test
    override fun migration() {
        ShadowUtils.setIsBatteryPresent(false)

        super.migration()
    }

    override fun launchFragmentScenario(fragmentClass: Class<PreferenceFragmentCompat>) =
        FragmentScenario.launch(
            fragmentClass,
            themeResId = R.style.Theme_CollapsingToolbar_Settings,
        )
}
