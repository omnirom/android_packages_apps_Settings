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
import android.content.ComponentName
import android.content.Context
import android.content.pm.ActivityInfo
import android.content.pm.ApplicationInfo
import android.view.accessibility.AccessibilityManager
import androidx.test.core.app.ApplicationProvider
import com.android.settings.R
import com.android.settings.testutils.FakeFeatureFactory
import com.android.settings.testutils.shadow.ShadowAccessibilityManager
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadow.api.Shadow

@RunWith(RobolectricTestRunner::class)
class ShortcutPreferenceTest {
    private val appContext: Context = ApplicationProvider.getApplicationContext()
    private val a11yManager: ShadowAccessibilityManager =
        Shadow.extract(appContext.getSystemService(AccessibilityManager::class.java))

    private lateinit var preference: ShortcutPreference

    @Before
    fun setUp() {
        FakeFeatureFactory.setupForTest()

        val activityInfo: ActivityInfo =
            mock<ActivityInfo> { on { loadLabel(any()) }.thenReturn(DEFAULT_LABEL) }
                .apply {
                    packageName = PACKAGE_NAME
                    name = A11Y_ACTIVITY_COMPONENT.className
                    applicationInfo = ApplicationInfo()
                }
        val mockInfo: AccessibilityShortcutInfo = mock {
            on { componentName }.thenReturn(A11Y_ACTIVITY_COMPONENT)
            on { this.activityInfo }.thenReturn(activityInfo)
        }
        a11yManager.setInstalledAccessibilityShortcutListAsUser(listOf(mockInfo))
        preference = ShortcutPreference(appContext, mockInfo, METRICS_CATEGORY)
    }

    @Test
    fun getTitle_returnA11yActivityShortcutTitle() {
        assertThat(preference.title).isEqualTo(0)
        assertThat(preference.getTitle(appContext).toString())
            .isEqualTo(appContext.getString(R.string.accessibility_shortcut_title, DEFAULT_LABEL))
    }

    @Test
    fun getFeatureName_returnA11yActivityLabel() {
        assertThat(preference.featureName).isEqualTo(0)
        assertThat(preference.getFeatureName(appContext).toString()).isEqualTo(DEFAULT_LABEL)
    }

    @Test
    fun getMetricsCategory() {
        assertThat(preference.metricsCategory).isEqualTo(METRICS_CATEGORY)
    }

    companion object {
        private const val PACKAGE_NAME = "com.foo.bar"
        private val A11Y_ACTIVITY_COMPONENT = ComponentName(PACKAGE_NAME, "FakeA11yActivity")
        private const val DEFAULT_LABEL = "default label"
        private const val METRICS_CATEGORY = 123
    }
}
