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

import android.app.Activity
import android.app.Application
import android.app.admin.DevicePolicyManager
import android.app.supervision.SupervisionManager
import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.never
import org.mockito.kotlin.any
import org.mockito.kotlin.clearInvocations
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadow.api.Shadow
import org.robolectric.shadows.ShadowContextImpl

@RunWith(RobolectricTestRunner::class)
class SupervisionRestrictionBypassActivityTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val mockDevicePolicyManager: DevicePolicyManager = mock()

    @Before
    fun setUp() {
        // Mock the DevicePolicyManager service using ShadowContextImpl
        Shadow.extract<ShadowContextImpl>((context as Application).baseContext)
            .setSystemService(Context.DEVICE_POLICY_SERVICE, mockDevicePolicyManager)

        // Clear any previous invocations on mocks between tests
        clearInvocations(mockDevicePolicyManager)
    }

    @Test
    fun onCreate_invalidIntentData_noUserRestrictionExtra_finishesActivity() {
        val intent =
            Intent(context, SupervisionRestrictionBypassActivity::class.java).apply {
                action = Settings.ACTION_BYPASS_SUPERVISION_RESTRICTION
            }
        val scenario =
            ActivityScenario.launchActivityForResult<SupervisionRestrictionBypassActivity>(intent)
        assertThat(scenario.result.resultCode).isEqualTo(Activity.RESULT_CANCELED)
        assertThat(scenario.state).isEqualTo(Lifecycle.State.DESTROYED)
    }

    @Test
    fun onCreate_invalidIntentData_emptyRestriction_finishesActivity() {
        val intent =
            Intent(context, SupervisionRestrictionBypassActivity::class.java).apply {
                action = Settings.ACTION_BYPASS_SUPERVISION_RESTRICTION
                putExtra(Settings.EXTRA_SUPERVISION_RESTRICTION, "")
            }

        val scenario =
            ActivityScenario.launchActivityForResult<SupervisionRestrictionBypassActivity>(intent)
        assertThat(scenario.result.resultCode).isEqualTo(Activity.RESULT_CANCELED)
        assertThat(scenario.state).isEqualTo(Lifecycle.State.DESTROYED)
    }

    @Test
    fun onCreate_validIntentData_launchesConfirmCredentialsActivity() {
        val intent =
            Intent(context, SupervisionRestrictionBypassActivity::class.java).apply {
                action = Settings.ACTION_BYPASS_SUPERVISION_RESTRICTION
                putExtra(Settings.EXTRA_SUPERVISION_RESTRICTION, TEST_RESTRICTION)
            }

        ActivityScenario.launch<SupervisionRestrictionBypassActivity>(intent).use { scenario ->
            scenario.onActivity { activity ->
                assertThat(activity.isFinishing).isFalse()

                val startedActivity = shadowOf(activity).nextStartedActivityForResult
                assertThat(startedActivity).isNotNull()
                assertThat(startedActivity.intent.component?.className)
                    .isEqualTo(ConfirmSupervisionCredentialsActivity::class.java.name)
            }
        }
    }

    @Test
    fun onCredentialConfirmed_resultOk_clearsRestriction() {
        val intent =
            Intent(context, SupervisionRestrictionBypassActivity::class.java).apply {
                action = Settings.ACTION_BYPASS_SUPERVISION_RESTRICTION
                putExtra(Settings.EXTRA_SUPERVISION_RESTRICTION, TEST_RESTRICTION)
            }
        // Launch with a valid intent so restriction is set
        ActivityScenario.launch<SupervisionRestrictionBypassActivity>(intent).use { scenario ->
            scenario.onActivity { activity ->
                val shadowActivity = shadowOf(activity)
                val launchedConfirmCredentialsIntent =
                    shadowActivity.nextStartedActivityForResult?.intent

                assertThat(launchedConfirmCredentialsIntent).isNotNull()
                shadowActivity.receiveResult(
                    launchedConfirmCredentialsIntent,
                    Activity.RESULT_OK,
                    null,
                )

                verify(mockDevicePolicyManager)
                    .clearUserRestriction(
                        SupervisionManager.SUPERVISION_SYSTEM_ENTITY,
                        TEST_RESTRICTION,
                        context.userId,
                    )
                assertThat(activity.isFinishing).isTrue()
            }
        }
    }

    @Test
    fun onCredentialConfirmed_resultCancelled_doesNotClearRestrictionAndFinishes() {
        val intent =
            Intent(context, SupervisionRestrictionBypassActivity::class.java).apply {
                action = Settings.ACTION_BYPASS_SUPERVISION_RESTRICTION
                putExtra(Settings.EXTRA_SUPERVISION_RESTRICTION, TEST_RESTRICTION)
            }

        ActivityScenario.launch<SupervisionRestrictionBypassActivity>(intent).use { scenario ->
            scenario.onActivity { activity ->
                val shadowActivity = shadowOf(activity)
                val launchedConfirmCredentialsIntent =
                    shadowActivity.nextStartedActivityForResult?.intent

                assertThat(launchedConfirmCredentialsIntent).isNotNull()
                shadowActivity.receiveResult(
                    launchedConfirmCredentialsIntent,
                    Activity.RESULT_CANCELED,
                    null,
                )

                verify(mockDevicePolicyManager, never()).clearUserRestriction(any(), any(), any())
                assertThat(activity.isFinishing).isTrue()
            }
        }
    }

    private companion object {
        const val TEST_RESTRICTION = "factory_reset"
    }
}
