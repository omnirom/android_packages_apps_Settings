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
package com.android.settings.supervision

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.pm.PackageManager.MATCH_UNINSTALLED_PACKAGES
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import androidx.preference.Preference
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.spy
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify

@RunWith(AndroidJUnit4::class)
class SupervisionSupportedAppPreferenceTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val title = "title"
    private val summary = "summary"
    private val appPackageName = "packageName"
    private val appIcon = ColorDrawable(Color.RED)
    private val mockPackageManager = mock(PackageManager::class.java)
    private lateinit var preference: Preference

    @Before
    fun setUp() {
        val fakeApplicationInfo = ApplicationInfo().apply { packageName = appPackageName }
        val spyContext = spy(context) { on { packageManager } doReturn mockPackageManager }

        mockPackageManager.stub {
            on { getApplicationInfo(eq(appPackageName), any<Int>()) } doReturn fakeApplicationInfo
            on { getApplicationIcon(fakeApplicationInfo) } doReturn appIcon
        }

        preference =
            SupervisionSupportedAppPreference(title, summary, appPackageName)
                .createWidget(spyContext)
    }

    @Test
    fun getTitle() {
        assertThat(preference.title).isEqualTo(title)
    }

    @Test
    fun getSummary() {
        assertThat(preference.summary).isEqualTo(summary)
    }

    @Test
    fun getIcon() {
        assertThat(preference.icon).isEqualTo(appIcon)
    }

    @Test
    fun createWidget_callsGetApplicationInfoWithCorrectFlags() {
        val expectedFlags = MATCH_UNINSTALLED_PACKAGES
        verify(mockPackageManager).getApplicationInfo(eq(appPackageName), eq(expectedFlags))
    }
}
