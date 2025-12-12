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
import android.app.role.RoleManager.ROLE_SYSTEM_SUPERVISION
import android.app.settings.SettingsEnums
import android.app.supervision.SupervisionManager
import android.app.supervision.SupervisionRecoveryInfo
import android.app.supervision.SupervisionRecoveryInfo.EXTRA_SUPERVISION_RECOVERY_INFO
import android.app.supervision.SupervisionRecoveryInfo.STATE_PENDING
import android.app.supervision.SupervisionRecoveryInfo.STATE_VERIFIED
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.UserInfo
import android.os.Build
import android.os.PersistableBundle
import android.os.UserHandle
import android.os.UserManager
import android.os.UserManager.USER_TYPE_PROFILE_SUPERVISING
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import com.android.settings.password.ChooseLockGeneric
import com.android.settings.supervision.ConfirmSupervisionCredentialsActivity.Companion.EXTRA_FORCE_CONFIRMATION
import com.android.settings.testutils.MetricsRule
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.argThat
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadow.api.Shadow
import org.robolectric.shadows.ShadowContextImpl
import org.robolectric.shadows.ShadowPackageManager
import org.robolectric.shadows.ShadowRoleManager

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.BAKLAVA])
class SupervisionPinRecoveryActivityTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private lateinit var shadowPackageManager: ShadowPackageManager
    private val mockSupervisionManager = mock<SupervisionManager>()
    private val mockUserManager = mock<UserManager>()
    @get:Rule val metricsRule = MetricsRule()

    @Before
    fun setUp() {
        ShadowRoleManager.addRoleHolder(ROLE_SYSTEM_SUPERVISION, CALLING_PACKAGE, context.user)
        shadowPackageManager = shadowOf(context.packageManager)

        // Intent filter for ConfirmSupervisionCredentialsActivity.
        val confirmPinIntentFilter = IntentFilter(ACTION_CONFIRM_PIN)
        val confirmPinComponentName =
            ComponentName(CALLING_PACKAGE, ConfirmSupervisionCredentialsActivity::class.java.name)
        shadowPackageManager.addActivityIfNotPresent(confirmPinComponentName)
        shadowPackageManager.addIntentFilterForActivity(
            confirmPinComponentName,
            confirmPinIntentFilter,
        )

        // Intent filters for PinRecoveryActivity actions.
        val recoveryComponentName = ComponentName(CALLING_PACKAGE, "SupervisionRecoveryActivity")
        val recoveryIntentFilter =
            IntentFilter(ACTION_SETUP_PIN_RECOVERY).apply {
                addAction(ACTION_UPDATE_PIN_RECOVERY)
                addAction(ACTION_VERIFY_PIN_RECOVERY)
                addAction(ACTION_SET_VERIFIED_PIN_RECOVERY)
                addAction(ACTION_POST_SETUP_VERIFY_PIN_RECOVERY)
            }
        shadowPackageManager.addActivityIfNotPresent(recoveryComponentName)
        shadowPackageManager.addIntentFilterForActivity(recoveryComponentName, recoveryIntentFilter)

        Shadow.extract<ShadowContextImpl>((context as Application).baseContext).apply {
            setSystemService(Context.SUPERVISION_SERVICE, mockSupervisionManager)
            setSystemService(Context.USER_SERVICE, mockUserManager)
        }
        mockUserManager.stub {
            on { users } doReturn listOf(SUPERVISING_USER_INFO)
            on {
                createProfileForUserEvenWhenDisallowed(
                    /* name= */ "Supervising",
                    /* userType= */ USER_TYPE_PROFILE_SUPERVISING,
                    /* flags= */ 0,
                    /* userId= */ UserHandle.USER_NULL,
                    /* disallowedPackages = */ null,
                )
            } doReturn SUPERVISING_USER_INFO
            on { removeUserEvenWhenDisallowed(SUPERVISING_USER_ID) } doReturn true
        }
    }

    @Test
    fun onCreate_nullAction_activityCanceled() {
        val intent = Intent(context, SupervisionPinRecoveryActivity::class.java)
        ActivityScenario.launchActivityForResult<SupervisionPinRecoveryActivity>(intent).use {
            scenario ->
            assertThat(scenario.state).isEqualTo(Lifecycle.State.DESTROYED)
            assertThat(scenario.result.resultCode).isEqualTo(Activity.RESULT_CANCELED)
        }
    }

    @Test
    fun onCreate_setupVerifiedAction_startsConfirmPinActivity() {
        // Verifies that ACTION_SETUP_VERIFIED starts ConfirmSupervisionCredentialsActivity.
        val intent =
            Intent(context, SupervisionPinRecoveryActivity::class.java).apply {
                action = SupervisionPinRecoveryActivity.ACTION_SETUP_VERIFIED
            }
        ActivityScenario.launch<SupervisionPinRecoveryActivity>(intent).use { scenario ->
            scenario.onActivity { activity ->
                val startedIntent = shadowOf(activity).nextStartedActivity
                assertThat(startedIntent.action).isEqualTo(ACTION_CONFIRM_PIN)
                assertThat(startedIntent.getBooleanExtra(EXTRA_FORCE_CONFIRMATION, false)).isTrue()
            }
        }
    }

    @Test
    fun onCreate_updateAction_startsConfirmPinActivity() {
        // Verifies that ACTION_UPDATE starts ConfirmSupervisionCredentialsActivity.
        val intent =
            Intent(context, SupervisionPinRecoveryActivity::class.java).apply {
                action = SupervisionPinRecoveryActivity.ACTION_UPDATE
            }
        ActivityScenario.launch<SupervisionPinRecoveryActivity>(intent).use { scenario ->
            scenario.onActivity { activity ->
                val startedIntent = shadowOf(activity).nextStartedActivity
                assertThat(startedIntent.action).isEqualTo(ACTION_CONFIRM_PIN)
                assertThat(startedIntent.getBooleanExtra(EXTRA_FORCE_CONFIRMATION, false))
                    .isEqualTo(true)
            }
        }
    }

    @Test
    fun onCreate_postSetupVerifyAction_startsConfirmPinActivity() {
        // Verifies that ACTION_POST_SETUP_VERIFY starts ConfirmSupervisionCredentialsActivity.
        val intent =
            Intent(context, SupervisionPinRecoveryActivity::class.java).apply {
                action = SupervisionPinRecoveryActivity.ACTION_POST_SETUP_VERIFY
            }
        ActivityScenario.launch<SupervisionPinRecoveryActivity>(intent).use { scenario ->
            scenario.onActivity { activity ->
                val startedIntent = shadowOf(activity).nextStartedActivity
                assertThat(startedIntent.action).isEqualTo(ACTION_CONFIRM_PIN)
                assertThat(startedIntent.getBooleanExtra(EXTRA_FORCE_CONFIRMATION, false))
                    .isEqualTo(true)
            }
        }
    }

    @Test
    fun onCreate_setupAction_startsPinRecoveryActivity() {
        // Verifies that ACTION_SETUP starts the setup pin recovery activity.
        val intent =
            Intent(context, SupervisionPinRecoveryActivity::class.java).apply {
                action = SupervisionPinRecoveryActivity.ACTION_SETUP
            }
        ActivityScenario.launch<SupervisionPinRecoveryActivity>(intent).use { scenario ->
            scenario.onActivity { activity ->
                val startedIntent = shadowOf(activity).nextStartedActivity
                assertThat(startedIntent.action).isEqualTo(ACTION_SETUP_PIN_RECOVERY)
            }
        }
    }

    @Test
    fun onCreate_recoveryAction_startsPinRecoveryActivity() {
        // Verifies that ACTION_RECOVERY starts the verify pin recovery activity.
        val intent =
            Intent(context, SupervisionPinRecoveryActivity::class.java).apply {
                action = SupervisionPinRecoveryActivity.ACTION_RECOVERY
            }
        ActivityScenario.launch<SupervisionPinRecoveryActivity>(intent).use { scenario ->
            scenario.onActivity { activity ->
                val startedIntent = shadowOf(activity).nextStartedActivity
                assertThat(startedIntent.action).isEqualTo(ACTION_VERIFY_PIN_RECOVERY)
            }
        }
    }

    @Test
    fun onPinConfirmed_setupVerifiedAction_canceledResult_finishesWithCanceled() {
        // Test scenario where PIN confirmation for SETUP_VERIFIED action is canceled.
        val intent =
            Intent(context, SupervisionPinRecoveryActivity::class.java).apply {
                action = SupervisionPinRecoveryActivity.ACTION_SETUP_VERIFIED
            }
        ActivityScenario.launch<SupervisionPinRecoveryActivity>(intent).use { scenario ->
            scenario.onActivity { activity ->
                val shadowActivity = shadowOf(activity)
                val startedIntent = shadowActivity.nextStartedActivity
                val testActivityResult = Activity.RESULT_CANCELED

                shadowActivity.receiveResult(startedIntent, testActivityResult, null)

                assertEquals(testActivityResult, shadowActivity.resultCode)
                assertThat(activity.isFinishing).isTrue()
            }
        }
    }

    @Test
    fun onPinConfirmed_setupVerifiedAction_successfulResult_startsPinRecoveryActivity() {
        // Test scenario where PIN confirmation for SETUP_VERIFIED action is successful,
        // leading to the start of the SET_VERIFIED_PIN_RECOVERY activity.
        val intent =
            Intent(context, SupervisionPinRecoveryActivity::class.java).apply {
                action = SupervisionPinRecoveryActivity.ACTION_SETUP_VERIFIED
            }
        ActivityScenario.launch<SupervisionPinRecoveryActivity>(intent).use { scenario ->
            scenario.onActivity { activity ->
                val shadowActivity = shadowOf(activity)
                // Simulates successful result of confirm PIN
                val startedIntent = shadowActivity.nextStartedActivity
                shadowActivity.receiveResult(startedIntent, Activity.RESULT_OK, null)

                assertThat(shadowActivity.nextStartedActivity.action)
                    .isEqualTo(ACTION_SET_VERIFIED_PIN_RECOVERY)
            }
        }
    }

    @Test
    fun onVerification_setupVerifiedAction_successfulResult_setsRecoveryInfoAndFinishes() {
        // Test scenario where the PIN recovery verification for SETUP_VERIFIED action completes
        // successfully,
        // setting the recovery info and finishing the activity.
        val intent =
            Intent(context, SupervisionPinRecoveryActivity::class.java).apply {
                action = SupervisionPinRecoveryActivity.ACTION_SETUP_VERIFIED
            }
        ActivityScenario.launch<SupervisionPinRecoveryActivity>(intent).use { scenario ->
            scenario.onActivity { activity ->
                val shadowActivity = shadowOf(activity)
                // First, simulates successful PIN confirmation
                val confirmPinIntent = shadowActivity.nextStartedActivity
                shadowActivity.receiveResult(confirmPinIntent, Activity.RESULT_OK, null)

                // Now, simulates successful verification after setting verified
                val resultIntent =
                    Intent().apply {
                        putExtra(
                            EXTRA_SUPERVISION_RECOVERY_INFO,
                            EXPECTED_SUPERVISION_RECOVERY_INFO,
                        )
                    }
                val testActivityResult = Activity.RESULT_OK

                val setVerifiedIntent = shadowActivity.nextStartedActivity
                shadowActivity.receiveResult(setVerifiedIntent, testActivityResult, resultIntent)

                // Verifies that supervisionRecoveryInfo is set correctly.
                verifySupervisionRecoveryInfo(EXPECTED_SUPERVISION_RECOVERY_INFO)
                assertEquals(testActivityResult, shadowActivity.resultCode)
                assertThat(activity.isFinishing).isTrue()
            }
        }
    }

    @Test
    fun onVerification_setupVerifiedAction_canceledResult_finishesWithCanceled() {
        // Test scenario where verification is canceled for SETUP_VERIFIED action.
        val intent =
            Intent(context, SupervisionPinRecoveryActivity::class.java).apply {
                action = SupervisionPinRecoveryActivity.ACTION_SETUP_VERIFIED
            }
        ActivityScenario.launch<SupervisionPinRecoveryActivity>(intent).use { scenario ->
            scenario.onActivity { activity ->
                val shadowActivity = shadowOf(activity)

                // First, simulates successful PIN confirmation (for ACTION_CONFIRM_PIN)
                val confirmPinIntent = shadowActivity.nextStartedActivity
                shadowActivity.receiveResult(confirmPinIntent, Activity.RESULT_OK, null)

                // After successful PIN confirmation, ACTION_SET_VERIFIED_PIN_RECOVERY is started.
                // Simulates canceled verification for ACTION_SET_VERIFIED_PIN_RECOVERY
                val setVerifiedIntent = shadowActivity.nextStartedActivity
                val testActivityResult = Activity.RESULT_CANCELED
                shadowActivity.receiveResult(setVerifiedIntent, testActivityResult, null)

                assertEquals(testActivityResult, shadowActivity.resultCode)
                assertThat(activity.isFinishing).isTrue()
            }
        }
    }

    @Test
    fun onPinConfirmed_updateAction_canceledResult_finishesWithCanceled() {
        // Test scenario where PIN confirmation is canceled for UPDATE action.
        val intent =
            Intent(context, SupervisionPinRecoveryActivity::class.java).apply {
                action = SupervisionPinRecoveryActivity.ACTION_UPDATE
            }
        ActivityScenario.launch<SupervisionPinRecoveryActivity>(intent).use { scenario ->
            scenario.onActivity { activity ->
                val shadowActivity = shadowOf(activity)
                val startedIntent = shadowActivity.nextStartedActivity
                val testActivityResult = Activity.RESULT_CANCELED

                shadowActivity.receiveResult(startedIntent, testActivityResult, null)

                assertEquals(testActivityResult, shadowActivity.resultCode)
                assertThat(activity.isFinishing).isTrue()
            }
        }
    }

    @Test
    fun onPinConfirmed_updateAction_successfulResult_startsPinRecoveryActivity() {
        // Test scenario where PIN confirmation for UPDATE action is successful,
        // leading to the start of the UPDATE_PIN_RECOVERY activity.
        val intent =
            Intent(context, SupervisionPinRecoveryActivity::class.java).apply {
                action = SupervisionPinRecoveryActivity.ACTION_UPDATE
            }
        ActivityScenario.launch<SupervisionPinRecoveryActivity>(intent).use { scenario ->
            scenario.onActivity { activity ->
                val shadowActivity = shadowOf(activity)
                // Simulates successful result of confirm PIN
                val startedIntent = shadowActivity.nextStartedActivity
                shadowActivity.receiveResult(startedIntent, Activity.RESULT_OK, null)

                assertThat(shadowActivity.nextStartedActivity.action)
                    .isEqualTo(ACTION_UPDATE_PIN_RECOVERY)
            }
        }
    }

    @Test
    fun onVerification_updateAction_successfulResult_setsRecoveryInfoAndFinishes() {
        // Test scenario where the UPDATE action completes successfully,
        // setting the recovery info and finishing the activity.
        val intent =
            Intent(context, SupervisionPinRecoveryActivity::class.java).apply {
                action = SupervisionPinRecoveryActivity.ACTION_UPDATE
            }
        ActivityScenario.launch<SupervisionPinRecoveryActivity>(intent).use { scenario ->
            scenario.onActivity { activity ->
                val shadowActivity = shadowOf(activity)
                // First, simulates successful PIN confirmation
                val confirmPinIntent = shadowActivity.nextStartedActivity
                shadowActivity.receiveResult(confirmPinIntent, Activity.RESULT_OK, null)

                // Then, simulates successful verification after update
                val resultIntent =
                    Intent().apply {
                        putExtra(
                            EXTRA_SUPERVISION_RECOVERY_INFO,
                            EXPECTED_SUPERVISION_RECOVERY_INFO,
                        )
                    }
                val testActivityResult = Activity.RESULT_OK

                val updatePinIntent = shadowActivity.nextStartedActivity
                shadowActivity.receiveResult(updatePinIntent, testActivityResult, resultIntent)

                // Verifies that supervisionRecoveryInfo is set correctly.
                verifySupervisionRecoveryInfo(EXPECTED_SUPERVISION_RECOVERY_INFO)

                assertEquals(testActivityResult, shadowActivity.resultCode)
                assertThat(activity.isFinishing).isTrue()
            }
        }
    }

    @Test
    fun onVerification_updateAction_canceledResult_finishesWithCanceled() {
        // Test scenario where the UPDATE action is canceled during verification.
        val intent =
            Intent(context, SupervisionPinRecoveryActivity::class.java).apply {
                action = SupervisionPinRecoveryActivity.ACTION_UPDATE
            }
        ActivityScenario.launch<SupervisionPinRecoveryActivity>(intent).use { scenario ->
            scenario.onActivity { activity ->
                val shadowActivity = shadowOf(activity)
                // First, simulates successful PIN confirmation
                val confirmPinIntent = shadowActivity.nextStartedActivity
                shadowActivity.receiveResult(confirmPinIntent, Activity.RESULT_OK, null)

                // Then, simulates canceled verification after update
                val updatePinIntent = shadowActivity.nextStartedActivity
                val testActivityResult = Activity.RESULT_CANCELED

                shadowActivity.receiveResult(updatePinIntent, testActivityResult, null)

                assertEquals(testActivityResult, shadowActivity.resultCode)
                assertThat(activity.isFinishing).isTrue()
            }
        }
    }

    @Test
    fun onPinConfirmed_postSetupVerifyAction_canceledResult_finishesWithCanceled() {
        // Test scenario where PIN confirmation for POST_SETUP_VERIFY action is canceled.
        val intent =
            Intent(context, SupervisionPinRecoveryActivity::class.java).apply {
                action = SupervisionPinRecoveryActivity.ACTION_POST_SETUP_VERIFY
            }
        ActivityScenario.launch<SupervisionPinRecoveryActivity>(intent).use { scenario ->
            scenario.onActivity { activity ->
                val shadowActivity = shadowOf(activity)
                val startedIntent = shadowActivity.nextStartedActivity
                val testActivityResult = Activity.RESULT_CANCELED

                shadowActivity.receiveResult(startedIntent, testActivityResult, null)

                assertEquals(testActivityResult, shadowActivity.resultCode)
                assertThat(activity.isFinishing).isTrue()
            }
        }
    }

    @Test
    fun onPinConfirmed_postSetupVerifyAction_successfulResult_startsPinRecoveryActivity() {
        // Test scenario where PIN confirmation for POST_SETUP_VERIFY action is successful,
        // leading to the start of the POST_SETUP_VERIFY_PIN_RECOVERY activity.
        val intent =
            Intent(context, SupervisionPinRecoveryActivity::class.java).apply {
                action = SupervisionPinRecoveryActivity.ACTION_POST_SETUP_VERIFY
            }
        ActivityScenario.launch<SupervisionPinRecoveryActivity>(intent).use { scenario ->
            scenario.onActivity { activity ->
                val shadowActivity = shadowOf(activity)
                // Simulates successful result of confirm PIN
                val startedIntent = shadowActivity.nextStartedActivity
                shadowActivity.receiveResult(startedIntent, Activity.RESULT_OK, null)

                assertThat(shadowActivity.nextStartedActivity.action)
                    .isEqualTo(ACTION_POST_SETUP_VERIFY_PIN_RECOVERY)
            }
        }
    }

    @Test
    fun onVerification_postSetupVerifyAction_successfulResult_setsRecoveryInfoAndFinishes() {
        // Test scenario where the POST_SETUP_VERIFY action completes successfully,
        // setting the recovery info and finishing the activity.
        val intent =
            Intent(context, SupervisionPinRecoveryActivity::class.java).apply {
                action = SupervisionPinRecoveryActivity.ACTION_POST_SETUP_VERIFY
            }
        ActivityScenario.launch<SupervisionPinRecoveryActivity>(intent).use { scenario ->
            scenario.onActivity { activity ->
                val shadowActivity = shadowOf(activity)
                // First, simulates successful PIN confirmation
                val confirmPinIntent = shadowActivity.nextStartedActivity
                shadowActivity.receiveResult(confirmPinIntent, Activity.RESULT_OK, null)

                // Then, simulates successful verification after post setup verify
                val resultIntent =
                    Intent().apply {
                        putExtra(
                            EXTRA_SUPERVISION_RECOVERY_INFO,
                            EXPECTED_SUPERVISION_RECOVERY_INFO,
                        )
                    }

                val postSetupVerifyIntent = shadowActivity.nextStartedActivity
                shadowActivity.receiveResult(
                    postSetupVerifyIntent,
                    Activity.RESULT_OK,
                    resultIntent,
                )

                // Verifies that supervisionRecoveryInfo is set correctly.
                verifySupervisionRecoveryInfo(EXPECTED_SUPERVISION_RECOVERY_INFO)
                assertThat(shadowActivity.resultCode).isEqualTo(Activity.RESULT_OK)
                assertThat(activity.isFinishing).isTrue()
            }
        }
    }

    @Test
    fun onVerification_recoveryAction_supervisionEnabled_successfulResult_startsSetPinActivity() {
        // Test scenario where the RECOVERY action successfully verifies,
        // leading to the start of the ChooseLockGeneric activity (to set PIN).
        val intent =
            Intent(context, SupervisionPinRecoveryActivity::class.java).apply {
                action = SupervisionPinRecoveryActivity.ACTION_RECOVERY
            }
        mockSupervisionManager.stub { on { isSupervisionEnabled } doReturn true }
        ActivityScenario.launch<SupervisionPinRecoveryActivity>(intent).use { scenario ->
            scenario.onActivity { activity ->
                val shadowActivity = shadowOf(activity)
                val testActivityResult = Activity.RESULT_OK

                val verifyPinIntent = shadowActivity.nextStartedActivity
                shadowActivity.receiveResult(verifyPinIntent, testActivityResult, null)

                val startedIntent = shadowActivity.nextStartedActivity
                assertThat(startedIntent.component?.className)
                    .isEqualTo(ChooseLockGeneric::class.java.name)
            }
        }
    }

    @Test
    fun onVerification_recoveryAction_supervisionDisabled_successfulResult_startsSetPinActivity() {
        // Test scenario where the RECOVERY action successfully verifies,
        // leading to the start of the ChooseLockGeneric activity (to set PIN).
        val intent =
            Intent(context, SupervisionPinRecoveryActivity::class.java).apply {
                action = SupervisionPinRecoveryActivity.ACTION_RECOVERY
            }
        mockSupervisionManager.stub { on { isSupervisionEnabled } doReturn false }
        ActivityScenario.launch<SupervisionPinRecoveryActivity>(intent).use { scenario ->
            scenario.onActivity { activity ->
                val shadowActivity = shadowOf(activity)
                val testActivityResult = Activity.RESULT_OK

                val verifyPinIntent = shadowActivity.nextStartedActivity
                shadowActivity.receiveResult(verifyPinIntent, testActivityResult, null)

                val startedIntent = shadowActivity.nextStartedActivity
                assertThat(startedIntent.component?.className)
                    .isEqualTo(ChooseLockGeneric::class.java.name)
            }
        }
    }

    @Test
    fun onVerification_recoveryAction_canceledResult_finishesWithCanceled() {
        // Test scenario where the RECOVERY action is canceled during verification.
        val intent =
            Intent(context, SupervisionPinRecoveryActivity::class.java).apply {
                action = SupervisionPinRecoveryActivity.ACTION_RECOVERY
            }
        ActivityScenario.launch<SupervisionPinRecoveryActivity>(intent).use { scenario ->
            scenario.onActivity { activity ->
                val shadowActivity = shadowOf(activity)
                val testActivityResult = Activity.RESULT_CANCELED

                val verifyPinIntent = shadowActivity.nextStartedActivity
                shadowActivity.receiveResult(verifyPinIntent, testActivityResult, null)

                verify(metricsRule.metricsFeatureProvider)
                    .action(activity, SettingsEnums.ACTION_SUPERVISION_PIN_RESET_SUCCEED, false)
                assertEquals(testActivityResult, shadowActivity.resultCode)
                assertThat(activity.isFinishing).isTrue()
            }
        }
    }

    @Test
    fun onPinSet_recoveryAction_successfulResult_finishesWithOk() {
        // Test scenario where the RECOVERY action successfully sets the PIN.
        val intent =
            Intent(context, SupervisionPinRecoveryActivity::class.java).apply {
                action = SupervisionPinRecoveryActivity.ACTION_RECOVERY
            }
        ActivityScenario.launch<SupervisionPinRecoveryActivity>(intent).use { scenario ->
            scenario.onActivity { activity ->
                val shadowActivity = shadowOf(activity)
                // Simulates successful verification and starting of SetPinActivity
                val verifyIntent = shadowActivity.nextStartedActivity
                shadowActivity.receiveResult(verifyIntent, Activity.RESULT_OK, null)

                // Simulates successful SetPinActivity
                val testActivityResult = Activity.RESULT_FIRST_USER
                val setPinIntent = shadowActivity.nextStartedActivity
                shadowActivity.receiveResult(setPinIntent, testActivityResult, null)

                verify(metricsRule.metricsFeatureProvider)
                    .action(activity, SettingsEnums.ACTION_SUPERVISION_PIN_RESET_SUCCEED, true)
                assertEquals(Activity.RESULT_OK, shadowActivity.resultCode)
                assertThat(activity.isFinishing).isTrue()
            }
        }
    }

    @Test
    fun onPinSet_recoveryAction_canceledResult_finishesWithCanceled() {
        // Test scenario where the RECOVERY action is canceled during PIN setting.
        val intent =
            Intent(context, SupervisionPinRecoveryActivity::class.java).apply {
                action = SupervisionPinRecoveryActivity.ACTION_RECOVERY
            }
        ActivityScenario.launch<SupervisionPinRecoveryActivity>(intent).use { scenario ->
            scenario.onActivity { activity ->
                val shadowActivity = shadowOf(activity)
                // Simulates successful verification and starting of SetPinActivity
                val verifyIntent = shadowActivity.nextStartedActivity
                shadowActivity.receiveResult(verifyIntent, Activity.RESULT_OK, null)

                // Simulates canceled SetPinActivity
                val testActivityResult = Activity.RESULT_CANCELED
                val setPinIntent = shadowActivity.nextStartedActivity
                shadowActivity.receiveResult(setPinIntent, testActivityResult, null)

                verify(metricsRule.metricsFeatureProvider)
                    .action(activity, SettingsEnums.ACTION_SUPERVISION_PIN_RESET_SUCCEED, false)
                assertEquals(testActivityResult, shadowActivity.resultCode)
                assertThat(activity.isFinishing).isTrue()
            }
        }
    }

    @Test
    fun onVerification_setupAction_successfulResult_setsRecoveryInfoAndFinishes() {
        // Test scenario where the SETUP action completes successfully,
        // setting the recovery info to PENDING and finishing the activity.
        val intent =
            Intent(context, SupervisionPinRecoveryActivity::class.java).apply {
                action = SupervisionPinRecoveryActivity.ACTION_SETUP
            }
        ActivityScenario.launch<SupervisionPinRecoveryActivity>(intent).use { scenario ->
            scenario.onActivity { activity ->
                val shadowActivity = shadowOf(activity)
                val resultIntent =
                    Intent().apply {
                        putExtra(
                            EXTRA_SUPERVISION_RECOVERY_INFO,
                            EXPECTED_PENDING_SUPERVISION_RECOVERY_INFO,
                        )
                    }
                val testActivityResult = Activity.RESULT_OK

                val setupPinIntent = shadowActivity.nextStartedActivity
                shadowActivity.receiveResult(setupPinIntent, testActivityResult, resultIntent)

                // Verifies that supervisionRecoveryInfo is set correctly with PENDING state.
                verifySupervisionRecoveryInfo(EXPECTED_PENDING_SUPERVISION_RECOVERY_INFO)
                assertEquals(testActivityResult, shadowActivity.resultCode)
                assertThat(activity.isFinishing).isTrue()
            }
        }
    }

    @Test
    fun onVerification_setupAction_canceledResult_finishesWithCanceled() {
        // Test scenario where SETUP setup action is canceled during verification.
        val intent =
            Intent(context, SupervisionPinRecoveryActivity::class.java).apply {
                action = SupervisionPinRecoveryActivity.ACTION_SETUP
            }
        ActivityScenario.launch<SupervisionPinRecoveryActivity>(intent).use { scenario ->
            scenario.onActivity { activity ->
                val shadowActivity = shadowOf(activity)
                val testActivityResult = Activity.RESULT_CANCELED

                val setupPinIntent = shadowActivity.nextStartedActivity
                shadowActivity.receiveResult(setupPinIntent, testActivityResult, null)

                assertEquals(testActivityResult, shadowActivity.resultCode)
                assertThat(activity.isFinishing).isTrue()
            }
        }
    }

    @Test
    fun onVerification_setupAction_invalidResult_finishesWithCanceled() {
        // Test scenario where SETUP setup action is finished with invalid data during verification.
        val intent =
            Intent(context, SupervisionPinRecoveryActivity::class.java).apply {
                action = SupervisionPinRecoveryActivity.ACTION_SETUP
            }
        ActivityScenario.launch<SupervisionPinRecoveryActivity>(intent).use { scenario ->
            scenario.onActivity { activity ->
                val shadowActivity = shadowOf(activity)
                val resultIntent =
                    Intent().apply { putExtra(EXTRA_SUPERVISION_RECOVERY_INFO, "dummy") }
                val setupPinIntent = shadowActivity.nextStartedActivity
                shadowActivity.receiveResult(setupPinIntent, Activity.RESULT_OK, resultIntent)

                // Verifies that the activity finishes with result CANCELED.
                verify(mockSupervisionManager, never()).setSupervisionRecoveryInfo(any())
                assertEquals(Activity.RESULT_CANCELED, shadowActivity.resultCode)
                assertThat(activity.isFinishing).isTrue()
            }
        }
    }

    /**
     * Helper function to verify that the SupervisionRecoveryInfo is set correctly.
     *
     * @param expectedInfo The expected SupervisionRecoveryInfo.
     */
    private fun verifySupervisionRecoveryInfo(expectedInfo: SupervisionRecoveryInfo) {
        verify(mockSupervisionManager).supervisionRecoveryInfo = argThat { info ->
            info != null &&
                info.accountName == expectedInfo.accountName &&
                info.accountType == expectedInfo.accountType &&
                info.state == expectedInfo.state &&
                info.accountData.getString("id") == expectedInfo.accountData.getString("id")
        }
    }

    companion object {
        // Intent actions used by SupervisionPinRecoveryActivity and related activities.
        const val ACTION_CONFIRM_PIN =
            "android.app.supervision.action.CONFIRM_SUPERVISION_CREDENTIALS"
        const val ACTION_SETUP_PIN_RECOVERY = "android.settings.supervision.action.SET_PIN_RECOVERY"
        const val ACTION_VERIFY_PIN_RECOVERY =
            "android.settings.supervision.action.VERIFY_PIN_RECOVERY"
        const val ACTION_UPDATE_PIN_RECOVERY =
            "android.settings.supervision.action.UPDATE_PIN_RECOVERY"
        const val ACTION_SET_VERIFIED_PIN_RECOVERY =
            "android.settings.supervision.action.SET_VERIFIED_PIN_RECOVERY"
        const val ACTION_POST_SETUP_VERIFY_PIN_RECOVERY =
            "android.settings.supervision.action.POST_SETUP_VERIFY_PIN_RECOVERY"

        // Constants for a supervising user.
        const val SUPERVISING_USER_ID = 5
        const val CALLING_PACKAGE = "com.android.settings"
        val SUPERVISING_USER_INFO =
            UserInfo(
                SUPERVISING_USER_ID,
                /* name */ "supervising",
                /* iconPath */ "",
                /* flags */ 0,
                USER_TYPE_PROFILE_SUPERVISING,
            )

        // Common expected SupervisionRecoveryInfo for verification
        val EXPECTED_SUPERVISION_RECOVERY_INFO =
            SupervisionRecoveryInfo(
                /* accountName */
                "test@example.com",
                /* accountType */
                "default",
                /* state */
                STATE_VERIFIED,
                /* accountData */
                PersistableBundle().apply { putString("id", "testId") },
            )

        val EXPECTED_PENDING_SUPERVISION_RECOVERY_INFO =
            SupervisionRecoveryInfo(
                /* accountName */
                "test@example.com",
                /* accountType */
                "default",
                /* state */
                STATE_PENDING,
                /* accountData */
                null,
            )
    }
}
