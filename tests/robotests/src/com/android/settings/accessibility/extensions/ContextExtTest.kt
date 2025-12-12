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

package com.android.settings.accessibility.extensions

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.activity.ComponentActivity
import androidx.test.core.app.ApplicationProvider
import com.android.settings.testutils.shadow.SettingsShadowResources
import com.google.android.setupcompat.util.WizardManagerHelper.EXTRA_IS_SETUP_FLOW
import com.google.common.truth.Truth.assertThat
import com.google.testing.junit.testparameterinjector.TestParameters
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestParameterInjector
import org.robolectric.Shadows.shadowOf
import org.robolectric.android.controller.ActivityController
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowPackageManager

/** Tests for Context extension functions */
@Config(shadows = [SettingsShadowResources::class])
@RunWith(RobolectricTestParameterInjector::class)
class ContextExtTest {
    private val appContext: Context = ApplicationProvider.getApplicationContext()
    private val shadowPackageManager: ShadowPackageManager = shadowOf(appContext.packageManager)

    @Test
    @TestParameters(
        "{supportMagnificationArea: false, hasWindowMagnificationFeature: false, expectSupported: false}",
        "{supportMagnificationArea: false, hasWindowMagnificationFeature: true, expectSupported: false}",
        "{supportMagnificationArea: true, hasWindowMagnificationFeature: false, expectSupported: false}",
        "{supportMagnificationArea: true, hasWindowMagnificationFeature: true, expectSupported: true}",
    )
    fun isWindowMagnificationSupported(
        supportMagnificationArea: Boolean,
        hasWindowMagnificationFeature: Boolean,
        expectSupported: Boolean,
    ) {
        SettingsShadowResources.overrideResource(
            com.android.internal.R.bool.config_magnification_area,
            supportMagnificationArea,
        )
        shadowPackageManager.setSystemFeature(
            PackageManager.FEATURE_WINDOW_MAGNIFICATION,
            hasWindowMagnificationFeature,
        )

        assertThat(appContext.isWindowMagnificationSupported()).isEqualTo(expectSupported)
    }

    @Test
    @TestParameters(
        "{inSetupWizard: false, expectedReturnValue: false}",
        "{inSetupWizard: true, expectedReturnValue: true}",
    )
    fun isInSetupWizard_contextIsActivity(inSetupWizard: Boolean, expectedReturnValue: Boolean) {
        var activityController: ActivityController<ComponentActivity>? = null
        try {
            activityController =
                ActivityController.of(
                        ComponentActivity(),
                        Intent().apply { putExtra(EXTRA_IS_SETUP_FLOW, inSetupWizard) },
                    )
                    .create()

            assertThat(activityController.get().isInSetupWizard()).isEqualTo(expectedReturnValue)
        } finally {
            activityController?.destroy()
        }
    }

    @Test
    fun isInSetupWizard_contextIsNotActivity_returnFalse() {
        assertThat(appContext.isInSetupWizard()).isFalse()
    }
}
