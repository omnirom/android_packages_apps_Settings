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

package com.android.settings.accessibility

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.android.settings.R
import com.android.settings.core.BasePreferenceController
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowSystemProperties

@RunWith(RobolectricTestRunner::class)
class SystemControlsPreferenceControllerTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val controller = SystemControlsPreferenceController(context, "key")

    @Test
    fun getAvailabilityStatus_returnsAvailable() {

        assertThat(controller.getAvailabilityStatus()).isEqualTo(BasePreferenceController.AVAILABLE)
    }

    @Test
    fun getSummary_oneHandedSupported_returnsCorrectSummary() {
        ShadowSystemProperties.override(SUPPORT_ONE_HANDED_MODE, "true")

        assertThat(controller.summary.toString())
            .isEqualTo(context.getString(R.string.accessibility_system_controls_subtext))
    }

    @Test
    fun getSummary_oneHandedNotSupported_returnsCorrectSummary() {
        ShadowSystemProperties.override(SUPPORT_ONE_HANDED_MODE, "false")

        assertThat(controller.summary.toString())
            .isEqualTo(
                context.getString(
                    R.string.accessibility_system_controls_subtext_one_handed_not_supported
                )
            )
    }

    companion object {
        private const val SUPPORT_ONE_HANDED_MODE: String = "ro.support_one_handed_mode"
    }
}
