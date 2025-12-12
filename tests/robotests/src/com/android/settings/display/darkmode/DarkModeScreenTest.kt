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

import android.app.UiModeManager
import android.app.settings.SettingsEnums
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import android.platform.test.annotations.DisableFlags
import android.platform.test.annotations.EnableFlags
import androidx.test.core.app.ApplicationProvider
import com.android.settings.R
import com.android.settings.Settings
import com.android.settings.SettingsActivity.EXTRA_FRAGMENT_ARG_KEY
import com.android.settings.accessibility.Flags
import com.android.settings.testutils.SettingsStoreRule
import com.android.settings.testutils2.SettingsCatalystTestCase
import com.android.settingslib.metadata.PreferenceLifecycleContext
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.notification.modes.TestModeBuilder
import com.android.settingslib.notification.modes.ZenMode
import com.android.settingslib.notification.modes.ZenModesBackend
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowLooper

class DarkModeScreenTest : SettingsCatalystTestCase() {
    @get:Rule val settingsStoreRule = SettingsStoreRule()
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val shadowPowerManager = shadowOf(context.getSystemService(PowerManager::class.java))!!

    override val preferenceScreenCreator = DarkModeScreen(context)
    override val flagName: String
        get() = Flags.FLAG_CATALYST_DARK_UI_MODE

    override fun migration() {
        // For custom modes footer preference to display title.
        val mockZenModesBackend = mock<ZenModesBackend>()
        ZenModesBackend.setInstance(mockZenModesBackend)
        val modeThatDoesNotChange = TestModeBuilder().setName("Unrelated").build()
        mockZenModesBackend.stub {
            on { getModes() } doReturn listOf<ZenMode?>(modeThatDoesNotChange)
        }

        // For update the availability of the custom preference controller.
        val uiModeManager = context.getSystemService(UiModeManager::class.java)!!
        uiModeManager.nightMode = UiModeManager.MODE_NIGHT_CUSTOM
        uiModeManager.nightModeCustomType = UiModeManager.MODE_NIGHT_CUSTOM_TYPE_SCHEDULE

        super.migration()
    }

    @After
    fun cleanup() {
        PowerSaveModeObservable.resetInstance()
    }

    @Test
    fun key() {
        assertThat(preferenceScreenCreator.key).isEqualTo("dark_ui_mode")
    }

    @Test
    fun getTitle() {
        assertThat(preferenceScreenCreator.title).isEqualTo(R.string.dark_ui_mode)
    }

    @Test
    fun getKeywords() {
        assertThat(preferenceScreenCreator.keywords).isEqualTo(R.string.keywords_dark_ui_mode)
    }

    @Test
    fun getHighlightMenuKey() {
        assertThat(preferenceScreenCreator.highlightMenuKey).isEqualTo(R.string.menu_key_display)
    }

    @Test
    fun getMetricsCategory() {
        assertThat(preferenceScreenCreator.metricsCategory)
            .isEqualTo(SettingsEnums.DARK_UI_SETTINGS)
    }

    @Test
    fun isEnabled_isTrue() {
        shadowPowerManager.setIsPowerSaveMode(false)

        assertThat(preferenceScreenCreator.isEnabled(context)).isTrue()
    }

    @Test
    fun isEnabled_isFalse() {
        shadowPowerManager.setIsPowerSaveMode(true)

        assertThat(preferenceScreenCreator.isEnabled(context)).isFalse()
    }

    @Test
    @EnableFlags(Flags.FLAG_CATALYST_DARK_UI_MODE)
    fun isIndexable_flagOn_isTrue() {
        shadowPowerManager.setIsPowerSaveMode(false)

        assertThat(preferenceScreenCreator.isIndexable(context)).isTrue()
    }

    @Test
    @EnableFlags(Flags.FLAG_CATALYST_DARK_UI_MODE)
    fun isIndexable_flagOn_isFalse() {
        shadowPowerManager.setIsPowerSaveMode(true)

        assertThat(preferenceScreenCreator.isIndexable(context)).isFalse()
    }

    @Test
    @DisableFlags(Flags.FLAG_CATALYST_DARK_UI_MODE)
    fun isIndexable_flagOFF_isFalse() {
        shadowPowerManager.setIsPowerSaveMode(false)

        assertThat(preferenceScreenCreator.isIndexable(context)).isFalse()
    }

    @Test
    @EnableFlags(Flags.FLAG_CATALYST_DARK_UI_MODE)
    fun isFlagEnabled_isTrue() {
        assertThat(preferenceScreenCreator.isFlagEnabled(context)).isTrue()
    }

    @Test
    @DisableFlags(Flags.FLAG_CATALYST_DARK_UI_MODE)
    fun isFlagEnabled_isFalse() {
        assertThat(preferenceScreenCreator.isFlagEnabled(context)).isFalse()
    }

    @Test
    fun onStart_settingChanges_notifyPrefChange() {
        val mockLifecycleContext =
            mock<PreferenceLifecycleContext> {
                on { preferenceScreenKey } doReturn preferenceScreenCreator.key
                on { applicationContext } doReturn context
            }
        preferenceScreenCreator.onStart(mockLifecycleContext)

        context.sendBroadcast(Intent(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED))
        ShadowLooper.idleMainLooper()

        verify(mockLifecycleContext).notifyPreferenceChange(DarkModeScreen.KEY)
    }

    @Test
    fun onStop_settingChanges_doNotNotifyPrefChange() {
        val mockLifecycleContext =
            mock<PreferenceLifecycleContext> {
                on { preferenceScreenKey } doReturn preferenceScreenCreator.key
                on { applicationContext } doReturn context
            }
        preferenceScreenCreator.onStart(mockLifecycleContext)
        preferenceScreenCreator.onStop(mockLifecycleContext)

        context.sendBroadcast(Intent(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED))
        ShadowLooper.idleMainLooper()

        verify(mockLifecycleContext, never()).notifyPreferenceChange(DarkModeScreen.KEY)
    }

    @Test
    fun getLaunchIntent_noMetadata_correctActivity() {
        val underTest = preferenceScreenCreator.getLaunchIntent(context, null)!!

        assertThat(underTest.getComponent()?.getClassName())
            .isEqualTo(Settings.DarkThemeSettingsActivity::class.java.getName())
        assertThat(underTest.hasExtra(EXTRA_FRAGMENT_ARG_KEY)).isFalse()
    }

    @Test
    fun getLaunchIntent_metadata_correctActivityWithExtraKey() {
        val underTest =
            preferenceScreenCreator.getLaunchIntent(context, TestMetadata("preference_key"))!!

        assertThat(underTest.hasExtra(EXTRA_FRAGMENT_ARG_KEY)).isTrue()
        assertThat(underTest.getStringExtra(EXTRA_FRAGMENT_ARG_KEY)).isEqualTo("preference_key")
    }
}

private data class TestMetadata(override val key: String) : PreferenceMetadata
