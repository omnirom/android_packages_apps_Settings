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

package com.android.settings.accessibility.colorcorrection.ui

import android.content.Context
import android.widget.TextView
import androidx.test.core.app.ApplicationProvider
import com.android.settings.R
import com.android.settings.accessibility.colorcorrection.data.ColorCorrectionModeDataStore
import com.android.settings.testutils.SettingsStoreRule
import com.android.settings.testutils.inflateViewHolder
import com.android.settingslib.datastore.SettingsSecureStore
import com.android.settingslib.metadata.ReadWritePermit
import com.android.settingslib.preference.createAndBindWidget
import com.android.settingslib.widget.SelectorWithWidgetPreference
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ModePreferenceTest {
    @get:Rule val settingsStoreRule = SettingsStoreRule()
    private val appContext: Context = ApplicationProvider.getApplicationContext()
    private val colorCorrectionModeDataStore = ColorCorrectionModeDataStore(appContext)

    @Test
    fun storage_returnPassedInStorage() {
        val modePreference = DeuteranomalyModePreference(colorCorrectionModeDataStore)
        assertThat(modePreference.storage(appContext)).isEqualTo(colorCorrectionModeDataStore)
    }

    @Test
    fun getReadPermissions_returnSettingsSecureStoreReadPermissions() {
        val modePreference = DeuteranomalyModePreference(colorCorrectionModeDataStore)
        assertThat(modePreference.getReadPermissions(appContext))
            .isEqualTo(SettingsSecureStore.getReadPermissions())
    }

    @Test
    fun getWritePermissions_returnSettingsSecureStoreWritePermissions() {
        val modePreference = DeuteranomalyModePreference(colorCorrectionModeDataStore)
        assertThat(modePreference.getWritePermissions(appContext))
            .isEqualTo(SettingsSecureStore.getWritePermissions())
    }

    @Test
    fun getReadPermit_returnsAllow() {
        val modePreference = DeuteranomalyModePreference(colorCorrectionModeDataStore)
        assertThat(modePreference.getReadPermit(appContext, 0, 0)).isEqualTo(ReadWritePermit.ALLOW)
    }

    @Test
    fun getWritePermit_returnsAllow() {
        val modePreference = DeuteranomalyModePreference(colorCorrectionModeDataStore)
        assertThat(modePreference.getWritePermit(appContext, 0, 0)).isEqualTo(ReadWritePermit.ALLOW)
    }

    @Test
    fun createWidget_returnsSelectorWithWidgetPreference() {
        val modePreference = DeuteranomalyModePreference(colorCorrectionModeDataStore)
        val widget = modePreference.createWidget(appContext)
        assertThat(widget).isInstanceOf(SelectorWithWidgetPreference::class.java)
    }

    @Test
    fun bindWidget_setTitleMaxLinesToFour() {
        val modePreference = DeuteranomalyModePreference(colorCorrectionModeDataStore)
        val widget = modePreference.createAndBindWidget<SelectorWithWidgetPreference>(appContext)
        val viewHolder = widget.inflateViewHolder()
        val titleView = viewHolder.itemView.findViewById<TextView>(android.R.id.title)

        assertThat(titleView.maxLines).isEqualTo(4)
    }

    @Test
    fun performClick_setWidgetChecked() {
        val modePreference = DeuteranomalyModePreference(colorCorrectionModeDataStore)
        val widget =
            modePreference.createAndBindWidget<SelectorWithWidgetPreference>(appContext).apply {
                inflateViewHolder()
            }

        widget.performClick()

        assertThat(widget.isChecked).isTrue()
    }

    @Test
    fun deuteranomalyModePreference_verifyKeyTitleSummary() {
        val modePreference = DeuteranomalyModePreference(colorCorrectionModeDataStore)

        assertThat(modePreference.key).isEqualTo("daltonizer_mode_deuteranomaly")
        assertThat(modePreference.title).isEqualTo(R.string.daltonizer_mode_deuteranomaly_title)
        assertThat(modePreference.summary).isEqualTo(R.string.daltonizer_mode_deuteranomaly_summary)
    }

    @Test
    fun protanomalyModePreference_verifyKeyTitleSummary() {
        val modePreference = ProtanomalyModePreference(colorCorrectionModeDataStore)

        assertThat(modePreference.key).isEqualTo("daltonizer_mode_protanomaly")
        assertThat(modePreference.title).isEqualTo(R.string.daltonizer_mode_protanomaly_title)
        assertThat(modePreference.summary).isEqualTo(R.string.daltonizer_mode_protanomaly_summary)
    }

    @Test
    fun tritanomalyModePreference_verifyKeyTitleSummary() {
        val modePreference = TritanomalyModePreference(colorCorrectionModeDataStore)

        assertThat(modePreference.key).isEqualTo("daltonizer_mode_tritanomaly")
        assertThat(modePreference.title).isEqualTo(R.string.daltonizer_mode_tritanomaly_title)
        assertThat(modePreference.summary).isEqualTo(R.string.daltonizer_mode_tritanomaly_summary)
    }

    @Test
    fun grayscaleModePreference_verifyKeyTitle() {
        val modePreference = GrayscaleModePreference(colorCorrectionModeDataStore)

        assertThat(modePreference.key).isEqualTo("daltonizer_mode_grayscale")
        assertThat(modePreference.title).isEqualTo(R.string.daltonizer_mode_grayscale_title)
        assertThat(modePreference.summary).isEqualTo(0)
    }
}
