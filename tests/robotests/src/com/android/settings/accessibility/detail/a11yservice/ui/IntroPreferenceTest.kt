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

package com.android.settings.accessibility.detail.a11yservice.ui

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.test.core.app.ApplicationProvider
import com.android.settingslib.preference.createAndBindWidget
import com.android.settingslib.widget.TopIntroPreference
import com.google.android.setupcompat.util.WizardManagerHelper.EXTRA_IS_SETUP_FLOW
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.robolectric.RobolectricTestRunner
import org.robolectric.android.controller.ActivityController

/** Tests for [IntroPreference]. */
@RunWith(RobolectricTestRunner::class)
class IntroPreferenceTest {
    private val appContext: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun getKey() {
        val introPreference = IntroPreference(mock())

        assertThat(introPreference.key).isEqualTo(IntroPreference.KEY)
    }

    @Test
    fun bindWidget_hasTitle_preferenceIsVisibleWithCorrectTitle() {
        val topIntroTest = "top intro text"
        val fakeAccessibilityServiceInfo = createFakeAccessibilityServiceInfo(topIntroTest)

        val introPreference = IntroPreference(fakeAccessibilityServiceInfo)
        val widget: TopIntroPreference = introPreference.createAndBindWidget(appContext)

        assertThat(widget.title).isEqualTo(topIntroTest)
        assertThat(widget.isVisible).isEqualTo(true)
    }

    @Test
    fun bindWidget_hasNoTitle_veryWidgetTypeAndPreferenceInvisible() {
        val fakeAccessibilityServiceInfo = createFakeAccessibilityServiceInfo(topIntro = "")

        val introPreference = IntroPreference(fakeAccessibilityServiceInfo)
        val widget: TopIntroPreference = introPreference.createAndBindWidget(appContext)

        assertThat(widget.title).isEqualTo("")
        assertThat(widget.isVisible).isEqualTo(false)
    }

    @Test
    fun isAvailable_hasTitle_returnsTrue() {
        val fakeAccessibilityServiceInfo = createFakeAccessibilityServiceInfo("Top intro text")
        val introPreference = IntroPreference(fakeAccessibilityServiceInfo)

        assertThat(introPreference.isAvailable(appContext)).isEqualTo(true)
    }

    @Test
    fun isAvailable_hasNoTitle_returnsFalse() {
        val fakeAccessibilityServiceInfo = createFakeAccessibilityServiceInfo(null)
        val introPreference = IntroPreference(fakeAccessibilityServiceInfo)

        assertThat(introPreference.isAvailable(appContext)).isEqualTo(false)
    }

    @Test
    fun isAvailable_inSetupWizard_hasTitle_returnFalse() {
        var activityController: ActivityController<ComponentActivity>? = null
        try {
            activityController =
                ActivityController.of(
                        ComponentActivity(),
                        Intent().apply { putExtra(EXTRA_IS_SETUP_FLOW, true) },
                    )
                    .create()

            val fakeAccessibilityServiceInfo = createFakeAccessibilityServiceInfo(null)
            val introPreference = IntroPreference(fakeAccessibilityServiceInfo)

            assertThat(introPreference.isAvailable(activityController.get())).isEqualTo(false)
        } finally {
            activityController?.destroy()
        }
    }

    private fun createFakeAccessibilityServiceInfo(topIntro: String?): AccessibilityServiceInfo {
        return mock { on { loadIntro(any()) } doReturn topIntro }
    }
}
