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

package com.android.settings.accessibility.extradim.ui

import android.content.Context
import android.hardware.display.ColorDisplayManager
import androidx.test.core.app.ApplicationProvider
import com.android.settings.R
import com.android.settings.accessibility.extradim.data.ExtraDimDataStore
import com.android.settings.testutils.SettingsStoreRule
import com.android.settingslib.metadata.ReadWritePermit
import com.android.settingslib.preference.createAndBindWidget
import com.android.settingslib.testutils.shadow.ShadowColorDisplayManager
import com.android.settingslib.widget.MainSwitchPreference
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadow.api.Shadow

@Config(shadows = [ShadowColorDisplayManager::class])
@RunWith(RobolectricTestRunner::class)
class ExtraDimMainSwitchPreferenceTest {
    @get:Rule val settingStoreRule = SettingsStoreRule()

    private lateinit var shadowColorDisplayManager: ShadowColorDisplayManager
    private lateinit var appContext: Context
    private lateinit var preference: ExtraDimMainSwitchPreference

    @Before
    fun setUp() {
        appContext = ApplicationProvider.getApplicationContext()
        preference = ExtraDimMainSwitchPreference(appContext)
        shadowColorDisplayManager =
            Shadow.extract(appContext.getSystemService(ColorDisplayManager::class.java))
    }

    @Test
    fun bindWidget_featureOn_toggleIsChecked() {
        shadowColorDisplayManager.isReduceBrightColorsActivated = true
        val widget = preference.createAndBindWidget<MainSwitchPreference>(appContext)

        assertThat(widget.isChecked).isTrue()
    }

    @Test
    fun bindWidget_featureOff_toggleIsNotChecked() {
        shadowColorDisplayManager.isReduceBrightColorsActivated = false
        val widget = preference.createAndBindWidget<MainSwitchPreference>(appContext)

        assertThat(widget.isChecked).isFalse()
    }

    @Test
    fun turnOnFeature_reduceBrightColorsIsActivated() {
        shadowColorDisplayManager.isReduceBrightColorsActivated = false
        val widget = preference.createAndBindWidget<MainSwitchPreference>(appContext)

        widget.performClick()

        assertThat(shadowColorDisplayManager.isReduceBrightColorsActivated).isTrue()
    }

    @Test
    fun turnOffFeature_reduceBrightColorIsDeactivated() {
        shadowColorDisplayManager.isReduceBrightColorsActivated = true
        val widget = preference.createAndBindWidget<MainSwitchPreference>(appContext)

        widget.performClick()

        assertThat(shadowColorDisplayManager.isReduceBrightColorsActivated).isFalse()
    }

    @Test
    fun getReadPermissions_returnsExtraDimDataStoreReadPermissions() {
        assertThat(preference.getReadPermissions(appContext))
            .isEqualTo(ExtraDimDataStore.getReadPermissions())
    }

    @Test
    fun getWritePermissions_returnsExtraDimDataStoreWritePermissions() {
        assertThat(preference.getWritePermissions(appContext))
            .isEqualTo(ExtraDimDataStore.getWritePermissions())
    }

    @Test
    fun getReadPermit_returnsAllow() {
        assertThat(preference.getReadPermit(appContext, 0, 0)).isEqualTo(ReadWritePermit.ALLOW)
    }

    @Test
    fun getWritePermit_returnsAllow() {
        assertThat(preference.getWritePermit(appContext, 0, 0)).isEqualTo(ReadWritePermit.ALLOW)
    }

    @Test
    fun getKey() {
        assertThat(preference.key).isEqualTo(ExtraDimMainSwitchPreference.KEY)
    }

    @Test
    fun getTitle_returnCorrectTitle() {
        assertThat(preference.title).isEqualTo(R.string.reduce_bright_colors_switch_title)
    }
}
