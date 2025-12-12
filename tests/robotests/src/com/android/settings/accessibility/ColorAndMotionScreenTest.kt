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

package com.android.settings.accessibility

import android.app.settings.SettingsEnums
import android.content.ComponentName
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.R
import com.android.settings.Settings.ColorAndMotionActivity
import com.android.settings.core.PreferenceScreenMixin
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ColorAndMotionScreenTest {
    private val preferenceScreenCreator: PreferenceScreenMixin = ColorAndMotionScreen()
    private val appContext: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun isIndexable_returnTrue() {
        assertThat(preferenceScreenCreator.indexable).isTrue()
    }

    @Test
    fun getMetricsCategory() {
        assertThat(preferenceScreenCreator.getMetricsCategory())
            .isEqualTo(SettingsEnums.ACCESSIBILITY_COLOR_AND_MOTION)
    }

    @Test
    fun getKey() {
        assertThat(preferenceScreenCreator.key).isEqualTo(ColorAndMotionScreen.KEY)
    }

    @Test
    fun getTitle() {
        assertThat(preferenceScreenCreator.title)
            .isEqualTo(R.string.accessibility_color_and_motion_title)
    }

    @Test
    fun getSummary() {
        assertThat(preferenceScreenCreator.summary)
            .isEqualTo(R.string.accessibility_color_and_motion_subtext)
    }

    @Test
    fun getIcon() {
        assertThat(preferenceScreenCreator.icon).isEqualTo(R.drawable.ic_color_and_motion)
    }

    @Test
    fun getHighlightMenuKey() {
        assertThat(preferenceScreenCreator.highlightMenuKey)
            .isEqualTo(R.string.menu_key_accessibility)
    }

    @Test
    fun hasCompleteHierarchy() {
        assertThat(preferenceScreenCreator.hasCompleteHierarchy()).isTrue()
    }

    @Test
    fun getFragmentClass() {
        assertThat(preferenceScreenCreator.fragmentClass())
            .isEqualTo(ColorAndMotionFragment::class.java)
    }

    @Test
    fun getLaunchIntent_returnColorAndMotionActivityIntent() {
        val expectedComponent = ComponentName(appContext, ColorAndMotionActivity::class.java)
        val intent = preferenceScreenCreator.getLaunchIntent(appContext, null)
        assertThat(intent).isNotNull()
        assertThat(intent!!.component).isEqualTo(expectedComponent)
    }
}
