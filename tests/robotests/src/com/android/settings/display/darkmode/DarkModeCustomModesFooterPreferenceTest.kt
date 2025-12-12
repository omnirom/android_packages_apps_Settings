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

package com.android.settings.display.darkmode

import android.content.Context
import android.platform.test.annotations.DisableFlags
import android.platform.test.annotations.EnableFlags
import android.platform.test.flag.junit.SetFlagsRule
import android.service.notification.ZenDeviceEffects
import android.view.View
import android.view.accessibility.Flags
import android.widget.TextView
import androidx.test.core.app.ApplicationProvider
import com.android.settings.R
import com.android.settings.testutils.inflateViewHolder
import com.android.settingslib.notification.modes.TestModeBuilder
import com.android.settingslib.notification.modes.ZenMode
import com.android.settingslib.notification.modes.ZenModesBackend
import com.android.settingslib.preference.createAndBindWidget
import com.android.settingslib.widget.FooterPreference
import com.android.settingslib.widget.preference.footer.R as SettingsLibR
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DarkModeCustomModesFooterPreferenceTest {
    @get:Rule val setFlagsRule = SetFlagsRule()
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val preference = DarkModeCustomModesFooterPreference()

    private val DEVICE_EFFECTS_WITH_DARK_THEME: ZenDeviceEffects? =
        ZenDeviceEffects.Builder().setShouldUseNightMode(true).build()

    private val mockZenModesBackend = mock<ZenModesBackend>()

    @Before
    fun setUp() {
        ZenModesBackend.setInstance(mockZenModesBackend)
    }

    @After
    fun tearDown() {
        ZenModesBackend.setInstance(null)
    }

    @Test
    fun key() {
        assertThat(preference.key).isEqualTo("dark_theme_custom_bedtime_footer")
    }

    @Test
    fun isIndexable() {
        assertThat(preference.indexable).isFalse()
    }

    @Test
    fun getTitle_zeroMode() {
        val modeThatDoesNotChange = TestModeBuilder().setName("Unrelated").build()
        mockZenModesBackend.stub {
            on { getModes() } doReturn listOf<ZenMode?>(modeThatDoesNotChange)
        }

        assertThat(preference.getTitle(context)).isEqualTo("Modes can also turn on dark theme")
    }

    @Test
    fun getTitle_oneMode() {
        val modeThatChanges1 =
            TestModeBuilder()
                .setName("Inactive")
                .setDeviceEffects(DEVICE_EFFECTS_WITH_DARK_THEME)
                .setActive(false)
                .build()
        mockZenModesBackend.stub { on { getModes() } doReturn listOf<ZenMode?>(modeThatChanges1) }

        assertThat(preference.getTitle(context)).isEqualTo("Inactive also turns on dark theme")
    }

    @Test
    fun getTitle_twoModes() {
        val modeThatChanges1 =
            TestModeBuilder()
                .setName("Inactive")
                .setDeviceEffects(DEVICE_EFFECTS_WITH_DARK_THEME)
                .setActive(false)
                .build()
        val modeThatChanges2 =
            TestModeBuilder()
                .setName("Active")
                .setDeviceEffects(DEVICE_EFFECTS_WITH_DARK_THEME)
                .setActive(true)
                .build()
        mockZenModesBackend.stub {
            on { getModes() } doReturn listOf<ZenMode?>(modeThatChanges1, modeThatChanges2)
        }

        assertThat(preference.getTitle(context))
            .isEqualTo("Inactive and Active also turn on dark theme")
    }

    @Test
    fun getTitle_threeModes() {
        val modeThatChanges1 =
            TestModeBuilder()
                .setName("Inactive")
                .setDeviceEffects(DEVICE_EFFECTS_WITH_DARK_THEME)
                .setActive(false)
                .build()
        val modeThatChanges2 =
            TestModeBuilder()
                .setName("Active")
                .setDeviceEffects(DEVICE_EFFECTS_WITH_DARK_THEME)
                .setActive(true)
                .build()
        val modeThatChanges3 =
            TestModeBuilder()
                .setName("Display")
                .setDeviceEffects(DEVICE_EFFECTS_WITH_DARK_THEME)
                .setActive(true)
                .build()
        mockZenModesBackend.stub {
            on { getModes() } doReturn
                listOf<ZenMode?>(modeThatChanges1, modeThatChanges2, modeThatChanges3)
        }

        assertThat(preference.getTitle(context))
            .isEqualTo("Inactive, Active, and Display also turn on dark theme")
    }

    @Test
    fun getTitle_moreModes() {
        val modeThatChanges1 =
            TestModeBuilder()
                .setName("Inactive")
                .setDeviceEffects(DEVICE_EFFECTS_WITH_DARK_THEME)
                .setActive(false)
                .build()
        val modeThatChanges2 =
            TestModeBuilder()
                .setName("Active")
                .setDeviceEffects(DEVICE_EFFECTS_WITH_DARK_THEME)
                .setActive(true)
                .build()
        val modeThatChanges3 =
            TestModeBuilder()
                .setName("Display")
                .setDeviceEffects(DEVICE_EFFECTS_WITH_DARK_THEME)
                .setActive(true)
                .build()
        val modeThatChanges4 =
            TestModeBuilder()
                .setName("private")
                .setDeviceEffects(DEVICE_EFFECTS_WITH_DARK_THEME)
                .setActive(true)
                .build()
        mockZenModesBackend.stub {
            on { getModes() } doReturn
                listOf<ZenMode?>(
                    modeThatChanges1,
                    modeThatChanges2,
                    modeThatChanges3,
                    modeThatChanges4,
                )
        }

        assertThat(preference.getTitle(context))
            .isEqualTo("Inactive, Active, and 2 more also turn on dark theme")
    }

    @Test
    fun learnMoreTextExists() {
        val widget = preference.createAndBindWidget<FooterPreference>(context)
        val prefViewHolder = widget.inflateViewHolder()
        val learnMoreView =
            prefViewHolder.itemView.findViewById<TextView>(SettingsLibR.id.settingslib_learn_more)

        assertThat(learnMoreView).isNotNull()
        assertThat(learnMoreView!!.visibility).isEqualTo(View.VISIBLE)
        assertThat(learnMoreView.text.toString())
            .isEqualTo(context.getString(R.string.dark_ui_modes_footer_action))
    }

    @EnableFlags(Flags.FLAG_FORCE_INVERT_COLOR)
    @Test
    fun getIconVisibility_flagOn_isGone() {
        val widget = preference.createAndBindWidget<FooterPreference>(context)
        val prefViewHolder = widget.inflateViewHolder()
        val iconView = prefViewHolder.itemView.findViewById<View>(SettingsLibR.id.icon_frame)

        assertThat(iconView).isNotNull()
        assertThat(iconView!!.visibility).isEqualTo(View.GONE)
    }

    @DisableFlags(Flags.FLAG_FORCE_INVERT_COLOR)
    @Test
    fun getIconVisibility_flagOff_isVisible() {
        val widget = preference.createAndBindWidget<FooterPreference>(context)
        val prefViewHolder = widget.inflateViewHolder()
        val iconView = prefViewHolder.itemView.findViewById<View>(SettingsLibR.id.icon_frame)

        assertThat(iconView).isNotNull()
        assertThat(iconView!!.visibility).isEqualTo(View.VISIBLE)
    }

    @EnableFlags(Flags.FLAG_FORCE_INVERT_COLOR)
    @Test
    fun testOrder() {
        val widget = preference.createAndBindWidget<FooterPreference>(context)

        assertThat(widget.order).isEqualTo(DarkModePreferenceOrderUtil.Order.MODES_FOOTER.value)
    }
}
