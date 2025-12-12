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
package com.android.settings.notification.modes

import android.app.settings.SettingsEnums
import android.os.UserManager.DISALLOW_ADJUST_VOLUME
import android.platform.test.annotations.DisableFlags
import android.platform.test.annotations.EnableFlags
import android.provider.Settings
import com.android.settings.R
import com.android.settings.Settings.ModesSettingsActivity
import com.android.settings.contract.TAG_DEVICE_STATE_PREFERENCE
import com.android.settings.contract.TAG_DEVICE_STATE_SCREEN
import com.android.settings.flags.Flags
import com.android.settings.testutils2.SettingsCatalystTestCase
import com.android.settingslib.metadata.PreferenceLifecycleContext
import com.android.settingslib.notification.modes.ZenMode
import com.android.settingslib.notification.modes.ZenModesBackend
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.mock
import org.mockito.kotlin.spy
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.Shadows.shadowOf

class ZenModesListScreenTest : SettingsCatalystTestCase() {

    private lateinit var zenModesBackend: ZenModesBackend
    private lateinit var mockLifeCycleContext: PreferenceLifecycleContext

    override val preferenceScreenCreator = ZenModesListScreen()

    override val flagName: String
        get() = Flags.FLAG_DEEPLINK_MODES_25Q4

    @Before
    fun setUp() {
        zenModesBackend = mock<ZenModesBackend>()
        ZenModesBackend.setInstance(zenModesBackend)
        mockLifeCycleContext = mock<PreferenceLifecycleContext>()
    }

    @Test
    fun key() {
        assertThat(preferenceScreenCreator.key).isEqualTo(ZenModesListScreen.KEY)
    }

    @Test
    fun title() {
        assertThat(preferenceScreenCreator.title).isEqualTo(R.string.zen_modes_list_title)
    }

    @Test
    fun highlightMenuKey() {
        assertThat(preferenceScreenCreator.highlightMenuKey)
            .isEqualTo(R.string.menu_key_priority_modes)
    }

    @Test
    fun getSummary_withSecurityException_returnsNull() {
        val zenModesListScreen = spy(preferenceScreenCreator)
        whenever(zenModesBackend.getModes()).doThrow(SecurityException())

        assertThat(zenModesListScreen.getSummary(appContext)).isNull()
    }

    @Test
    fun getSummary_success_returnsSummary() {
        val zenModesListScreen = spy(preferenceScreenCreator)
        doReturn(emptyList<ZenMode>()).whenever(zenModesBackend).getModes()

        assertThat(zenModesListScreen.getSummary(appContext)).isNotNull()
    }

    @Test
    fun isEnabled_returnsTrue() {
        assertThat(preferenceScreenCreator.isEnabled(appContext)).isTrue()
    }

    @Test
    fun restrictionKeys() {
        assertThat(preferenceScreenCreator.restrictionKeys)
            .isEqualTo(arrayOf(DISALLOW_ADJUST_VOLUME))
    }

    @Test
    fun getMetricsCategory() {
        assertThat(preferenceScreenCreator.getMetricsCategory())
            .isEqualTo(SettingsEnums.ZEN_PRIORITY_MODES_LIST)
    }

    @Test
    fun tags_correct() {
        assertThat(preferenceScreenCreator.tags(appContext).toList())
            .containsExactly(TAG_DEVICE_STATE_SCREEN, TAG_DEVICE_STATE_PREFERENCE)
    }

    @Test
    @EnableFlags(Flags.FLAG_DEEPLINK_MODES_25Q4)
    fun isFlagEnabled_enabled() {
        assertThat(preferenceScreenCreator.isFlagEnabled(appContext)).isTrue()
    }

    @Test
    @DisableFlags(Flags.FLAG_DEEPLINK_MODES_25Q4)
    fun isFlagEnabled_disabled() {
        assertThat(preferenceScreenCreator.isFlagEnabled(appContext)).isFalse()
    }

    @Test
    fun hasCompleteHierarchy_correct() {
        assertThat(preferenceScreenCreator.hasCompleteHierarchy()).isFalse()
    }

    @Test
    fun fragmentClass_correct() {
        assertThat(preferenceScreenCreator.fragmentClass())
            .isEqualTo(ZenModesListFragment::class.java)
    }

    @Test
    fun getLaunchIntent_correctActivity() {
        val underTest = preferenceScreenCreator.getLaunchIntent(appContext, null)

        assertThat(underTest.getComponent()?.getClassName())
            .isEqualTo(ModesSettingsActivity::class.java.getName())
    }

    @Test
    fun onStart_registersObserver() {
        mockLifeCycleContext.stub { on { contentResolver } doReturn appContext.contentResolver }

        preferenceScreenCreator.onStart(mockLifeCycleContext)

        // Trigger a change to the setting
        appContext.contentResolver.notifyChange(
            Settings.Global.getUriFor(Settings.Global.ZEN_MODE),
            null,
        )

        // Verify that the observer callback was triggered
        verify(mockLifeCycleContext).notifyPreferenceChange(ZenModesListScreen.KEY)
    }

    @Test
    fun onStop_unregistersObserver() {
        mockLifeCycleContext.stub { on { contentResolver } doReturn appContext.contentResolver }

        val shadowContentResolver = shadowOf(appContext.contentResolver)
        val zenModeUri = Settings.Global.getUriFor(Settings.Global.ZEN_MODE)

        // Start observing
        preferenceScreenCreator.onStart(mockLifeCycleContext)
        assertThat(shadowContentResolver.getContentObservers(zenModeUri)).isNotEmpty()

        // Stop observing
        preferenceScreenCreator.onStop(mockLifeCycleContext)
        assertThat(shadowContentResolver.getContentObservers(zenModeUri)).isEmpty()
    }
}
