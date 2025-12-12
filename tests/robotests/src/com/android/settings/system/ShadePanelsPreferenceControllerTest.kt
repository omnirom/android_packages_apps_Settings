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

package com.android.settings.system

import android.content.Context
import android.platform.test.annotations.DisableFlags
import android.platform.test.annotations.EnableFlags
import android.platform.test.flag.junit.SetFlagsRule
import androidx.test.core.app.ApplicationProvider
import com.android.settings.R
import com.android.settings.core.BasePreferenceController.AVAILABLE
import com.android.settings.core.BasePreferenceController.CONDITIONALLY_UNAVAILABLE
import com.android.settings.core.BasePreferenceController.UNSUPPORTED_ON_DEVICE
import com.android.settings.system.ShadePanelsPreferenceController.Companion.setDualShadeEnabled
import com.android.systemui.Flags
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowDisplayManager

@RunWith(RobolectricTestRunner::class)
@Config(shadows = [ShadowDisplayManager::class])
class ShadePanelsPreferenceControllerTest {

    @get:Rule
    val setFlagsRule = SetFlagsRule()

    private lateinit var context: Context
    private lateinit var controller: ShadePanelsPreferenceController

    private val dualShadePreferenceTitle: CharSequence
        get() = context.getText(R.string.shade_panels_separate_title)

    private val singleShadePreferenceTitle: CharSequence
        get() = context.getText(R.string.shade_panels_combined_title)

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        controller = ShadePanelsPreferenceController(context, "testPreferenceKey")
    }

    @Test
    @EnableFlags(Flags.FLAG_SCENE_CONTAINER)
    @Config(qualifiers = "w360dp-h640dp")
    fun getAvailabilityStatus_sceneContainerEnabled_onPhone_isAvailable() {
        assertThat(controller.availabilityStatus).isEqualTo(AVAILABLE)
    }

    @Test
    @DisableFlags(Flags.FLAG_SCENE_CONTAINER)
    @Config(qualifiers = "w360dp-h640dp")
    fun getAvailabilityStatus_sceneContainerDisabled_onPhone_isConditionallyUnavailable() {
        assertThat(controller.availabilityStatus).isEqualTo(CONDITIONALLY_UNAVAILABLE)
    }

    @Test
    @EnableFlags(Flags.FLAG_SCENE_CONTAINER)
    @Config(qualifiers = "w800dp-h600dp")
    fun getAvailabilityStatus_sceneContainerEnabled_onFoldable_isAvailable() {
        ShadowDisplayManager.addDisplay("w360dp-h640dp") // Emulate the front display

        assertThat(controller.availabilityStatus).isEqualTo(AVAILABLE)
    }

    @Test
    @EnableFlags(Flags.FLAG_SCENE_CONTAINER)
    @Config(qualifiers = "w1200dp-h800dp")
    fun getAvailabilityStatus_sceneContainerEnabled_onTablet_isUnsupportedOnDevice() {
        assertThat(controller.availabilityStatus).isEqualTo(UNSUPPORTED_ON_DEVICE)
    }

    @Test
    @EnableFlags(Flags.FLAG_SCENE_CONTAINER)
    @Config(qualifiers = "w360dp-h640dp")
    fun getAvailabilityStatus_sceneContainerEnabled_onPhoneConnectedToTablet_isAvailable() {
        ShadowDisplayManager.addDisplay("w1200dp-h800dp")

        assertThat(controller.availabilityStatus).isEqualTo(AVAILABLE)
    }

    @Test
    @DisableFlags(Flags.FLAG_SCENE_CONTAINER)
    fun getSummary_dualShadeUnavailable_null() {
        context.contentResolver.setDualShadeEnabled(enable = true)

        assertThat(controller.summary).isNull()
    }

    @Test
    @EnableFlags(Flags.FLAG_SCENE_CONTAINER)
    fun getSummary_dualShadeEnabled_separate() {
        context.contentResolver.setDualShadeEnabled(enable = true)

        assertThat(controller.summary).isEqualTo(dualShadePreferenceTitle)
    }

    @Test
    @EnableFlags(Flags.FLAG_SCENE_CONTAINER)
    fun getSummary_dualShadeDisabled_combined() {
        context.contentResolver.setDualShadeEnabled(enable = false)

        assertThat(controller.summary).isEqualTo(singleShadePreferenceTitle)
    }
}
