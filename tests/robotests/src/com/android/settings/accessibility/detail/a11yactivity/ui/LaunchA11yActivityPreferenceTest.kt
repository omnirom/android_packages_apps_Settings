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

package com.android.settings.accessibility.detail.a11yactivity.ui

import android.accessibilityservice.AccessibilityShortcutInfo
import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import androidx.test.core.app.ApplicationProvider
import com.android.settings.testutils.inflateViewHolder
import com.android.settingslib.preference.createAndBindWidget
import com.android.settingslib.widget.ButtonPreference
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnit
import org.mockito.junit.MockitoRule
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class LaunchA11yActivityPreferenceTest {
    @get:Rule val mockito: MockitoRule = MockitoJUnit.rule()
    private val appContext: Context = ApplicationProvider.getApplicationContext()
    @Mock private lateinit var shortcutInfo: AccessibilityShortcutInfo
    @Mock private lateinit var activityInfo: ActivityInfo

    @Before
    fun setUp() {
        whenever(activityInfo.loadLabel(any())).thenReturn(FAKE_FEATURE_NAME)
        whenever(shortcutInfo.activityInfo).thenReturn(activityInfo)
        whenever(shortcutInfo.componentName).thenReturn(FAKE_COMPONENT_NAME)
    }

    @Test
    fun getTitle() {
        val preference = LaunchA11yActivityPreference(shortcutInfo)
        assertThat(preference.getTitle(appContext)).isEqualTo("Open $FAKE_FEATURE_NAME")
    }

    @Test
    fun isIndexable_returnFalse() {
        val preference = LaunchA11yActivityPreference(shortcutInfo)
        assertThat(preference.indexable).isFalse()
    }

    @Test
    fun createWidget_returnButtonPreference() {
        val preference = LaunchA11yActivityPreference(shortcutInfo)
        assertThat(preference.createWidget(appContext)).isInstanceOf(ButtonPreference::class.java)
    }

    @Test
    fun clickButton_startsActivity() {
        val buttonPreferenceWidget =
            LaunchA11yActivityPreference(shortcutInfo)
                .createAndBindWidget<ButtonPreference>(appContext)
        buttonPreferenceWidget.inflateViewHolder()
        buttonPreferenceWidget.button.performClick()

        val startedIntent: Intent = shadowOf(appContext as Application).nextStartedActivity

        assertThat(startedIntent).isNotNull()
        assertThat(startedIntent.component).isEqualTo(FAKE_COMPONENT_NAME)
        assertThat(startedIntent.flags and Intent.FLAG_ACTIVITY_NEW_TASK).isNotEqualTo(0)
    }

    companion object {
        private const val FAKE_FEATURE_NAME = "Fake feature name"
        private val FAKE_COMPONENT_NAME = ComponentName("foo.bar", "FakeActivity")
    }
}
