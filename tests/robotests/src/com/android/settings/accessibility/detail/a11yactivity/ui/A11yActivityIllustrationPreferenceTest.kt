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
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class A11yActivityIllustrationPreferenceTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun getImageUri_hasImage_returnUri() {
        val mockInfo: AccessibilityShortcutInfo = mock {
            on { componentName }.thenReturn(A11Y_ACTIVITY_COMPONENT)
            on { animatedImageRes }.thenReturn(123)
        }
        val preference = A11yActivityIllustrationPreference(mockInfo)
        assertThat(preference.getImageUri(context).toString())
            .isEqualTo("android.resource://$PACKAGE_NAME/123")
    }

    @Test
    fun getImageUri_noImage_returnNull() {
        val mockInfo: AccessibilityShortcutInfo = mock {
            on { componentName }.thenReturn(A11Y_ACTIVITY_COMPONENT)
            on { animatedImageRes }.thenReturn(0)
        }
        val preference = A11yActivityIllustrationPreference(mockInfo)
        assertThat(preference.getImageUri(context)).isNull()
    }

    @Test
    fun getContentDescription() {
        val mockActivityInfo =
            mock<ActivityInfo>().apply {
                packageName = PACKAGE_NAME
                name = A11Y_ACTIVITY_COMPONENT.className
                applicationInfo = ApplicationInfo()
            }
        whenever(mockActivityInfo.loadLabel(any())).thenReturn(A11Y_FEATURE_NAME)
        val mockInfo: AccessibilityShortcutInfo = mock {
            on { componentName }.thenReturn(A11Y_ACTIVITY_COMPONENT)
            on { activityInfo }.thenReturn(mockActivityInfo)
        }

        val preference = A11yActivityIllustrationPreference(mockInfo)
        assertThat(preference.getContentDescription(context))
            .isEqualTo("$A11Y_FEATURE_NAME animation")
    }

    companion object {
        private const val PACKAGE_NAME = "com.foo.bar"
        private val A11Y_ACTIVITY_COMPONENT = ComponentName(PACKAGE_NAME, "FakeA11yActivity")
        private const val A11Y_FEATURE_NAME = "Fake Name"
    }
}
