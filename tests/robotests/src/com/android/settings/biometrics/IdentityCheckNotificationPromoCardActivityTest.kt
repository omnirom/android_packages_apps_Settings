/*
 * Copyright (C) 2025 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings.biometrics

import android.app.Application
import android.content.Intent
import android.provider.Settings
import androidx.lifecycle.Lifecycle.State
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnit
import org.mockito.junit.MockitoRule
import org.robolectric.Shadows.shadowOf

@RunWith(AndroidJUnit4::class)
@SmallTest
class IdentityCheckNotificationPromoCardActivityTest {
    @get:Rule val mockitoRule: MockitoRule = MockitoJUnit.rule()

    private val context: Application = ApplicationProvider.getApplicationContext()
    private val shadowApplication = shadowOf(context)

    @Test
    fun launchActivity_launchesSafetyCenter() {
        setIdentityCheckNotificationBeenClickedToFalse()
        ActivityScenario.launch<IdentityCheckNotificationPromoCardActivity>(getIntent()).use {
            scenario ->
            {
                assertThat(scenario.state).isEqualTo(State.DESTROYED)
                assertThat(shadowApplication.peekNextStartedActivity().action)
                    .isEqualTo(Intent.ACTION_SAFETY_CENTER)
            }
        }
        assertThat(hasIdentityCheckNotificationBeenClicked()).isTrue()
    }

    private fun getIntent() =
        Intent(context, IdentityCheckNotificationPromoCardActivity::class.java)

    private fun setIdentityCheckNotificationBeenClickedToFalse() =
        Settings.Secure.putInt(
            context.contentResolver,
            Settings.Secure.IDENTITY_CHECK_NOTIFICATION_VIEW_DETAILS_CLICKED,
            0,
        )

    private fun hasIdentityCheckNotificationBeenClicked(): Boolean =
        Settings.Secure.getInt(
            context.contentResolver,
            Settings.Secure.IDENTITY_CHECK_NOTIFICATION_VIEW_DETAILS_CLICKED,
            0,
        ) == 1
}
