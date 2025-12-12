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

import android.app.Activity.RESULT_CANCELED
import android.app.Activity.RESULT_OK
import android.app.Application
import android.app.KeyguardManager
import android.app.supervision.SupervisionManager
import android.app.supervision.flags.Flags
import android.content.Context
import android.content.pm.UserInfo
import android.os.UserHandle
import android.os.UserManager
import android.os.UserManager.USER_TYPE_PROFILE_SUPERVISING
import android.platform.test.annotations.EnableFlags
import android.view.View
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.R
import com.android.settings.password.ChooseLockGeneric
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadow.api.Shadow
import org.robolectric.shadows.ShadowContextImpl
import org.robolectric.shadows.ShadowKeyguardManager

@RunWith(AndroidJUnit4::class)
class SetupSupervisionActivityTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    private val mockSupervisionManager = mock<SupervisionManager>()
    private val mockUserManager = mock<UserManager>()

    private lateinit var shadowKeyguardManager: ShadowKeyguardManager

    @Before
    fun setUp() {
        shadowKeyguardManager = shadowOf(context.getSystemService(KeyguardManager::class.java))
        Shadow.extract<ShadowContextImpl>((context as Application).baseContext).apply {
            setSystemService(Context.SUPERVISION_SERVICE, mockSupervisionManager)
            setSystemService(Context.USER_SERVICE, mockUserManager)
        }

        mockUserManager.stub { on { users } doReturn listOf(SUPERVISING_USER_INFO) }
        shadowKeyguardManager.setIsDeviceSecure(SUPERVISING_USER_ID, false)
    }

    @Test
    fun onCreate_noSupervisingUser_loadProgressBar() {
        mockUserManager.stub { on { users } doReturn emptyList() }

        ActivityScenario.launch(SetupSupervisionActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val progressBar =
                    activity.findViewById<LinearProgressIndicator>(R.id.linearProgressIndicator)

                assertThat(progressBar).isNotNull()
                assertThat(progressBar.visibility).isEqualTo(View.VISIBLE)
            }
        }
    }

    @Test
    fun onCreate_noSupervisingUser_createProfile_startSetPinActivity() {
        mockUserManager.stub {
            on { users } doReturn emptyList()
            on {
                createProfileForUserEvenWhenDisallowed(any(), any(), any(), any(), anyOrNull())
            } doReturn SUPERVISING_USER_INFO
        }
        shadowKeyguardManager.setIsDeviceSecure(SUPERVISING_USER_ID, true)

        ActivityScenario.launch(SetupSupervisionActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                assertThat(shadowOf(activity).nextStartedActivity.component?.className)
                    .isEqualTo(ChooseLockGeneric::class.java.name)

                assertThat(activity.isFinishing).isFalse()
            }
        }

        verify(mockUserManager)
            .createProfileForUserEvenWhenDisallowed(
                "Supervising",
                USER_TYPE_PROFILE_SUPERVISING,
                /* flags= */ 0,
                UserHandle.USER_NULL,
                null,
            )
    }

    @Test
    fun onCreate_existingSupervisingUser_startSetPinActivity() {
        ActivityScenario.launch(SetupSupervisionActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                assertThat(shadowOf(activity).nextStartedActivity.component?.className)
                    .isEqualTo(ChooseLockGeneric::class.java.name)

                assertThat(activity.isFinishing).isFalse()
            }
        }

        verify(mockUserManager, never())
            .createProfileForUserEvenWhenDisallowed(any(), any(), any(), any(), any())
    }

    @Test
    fun onCreate_createUserFails_canceled() {
        mockUserManager.stub {
            on { users } doReturn emptyList()
            on {
                createProfileForUserEvenWhenDisallowed(any(), any(), any(), any(), any())
            } doReturn null
        }

        ActivityScenario.launchActivityForResult(SetupSupervisionActivity::class.java).use {
            scenario ->
            assertThat(scenario.state).isEqualTo(Lifecycle.State.RESUMED)
            assertThat(scenario.result.resultCode).isEqualTo(RESULT_CANCELED)
        }
    }

    @Test
    fun onCreate_existingSupervisingLock_finishes() {
        shadowKeyguardManager.setIsDeviceSecure(SUPERVISING_USER_ID, true)

        ActivityScenario.launchActivityForResult(SetupSupervisionActivity::class.java).use {
            scenario ->
            assertThat(scenario.state).isEqualTo(Lifecycle.State.DESTROYED)
            assertThat(scenario.result.resultCode).isEqualTo(RESULT_CANCELED)
        }
    }

    @Test
    fun onResume_existingSupervisingLock_finishes() {
        shadowKeyguardManager.setIsDeviceSecure(SUPERVISING_USER_ID, false)

        ActivityScenario.launchActivityForResult(SetupSupervisionActivity::class.java).use {
            scenario ->
            scenario.moveToState(Lifecycle.State.STARTED)

            shadowKeyguardManager.setIsDeviceSecure(SUPERVISING_USER_ID, true)
            scenario.moveToState(Lifecycle.State.RESUMED)

            scenario.onActivity { activity -> assertThat(activity.isFinishing).isTrue() }
            assertThat(scenario.result.resultCode).isEqualTo(RESULT_OK)
        }
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_SUPERVISION_PIN_RECOVERY_SCREEN)
    fun onSetLockResult_startsRecoveryActivity() {
        shadowKeyguardManager.setIsDeviceSecure(SUPERVISING_USER_ID, false)

        ActivityScenario.launch(SetupSupervisionActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val shadowActivity = shadowOf(activity)
                shadowKeyguardManager.setIsDeviceSecure(SUPERVISING_USER_ID, true)
                shadowActivity.receiveResult(
                    shadowActivity.nextStartedActivityForResult.intent,
                    RESULT_OK,
                    null,
                )

                assertThat(shadowActivity.nextStartedActivity.component?.className)
                    .isEqualTo(SupervisionPinRecoveryActivity::class.java.name)
                assertThat(activity.isFinishing).isFalse()
            }
        }
        verify(mockSupervisionManager).setSupervisionEnabled(true)
    }

    @Test
    fun onSetLockResult_supervisingUserNull_canceled() {
        ActivityScenario.launchActivityForResult(SetupSupervisionActivity::class.java).use {
            scenario ->
            scenario.onActivity { activity ->
                mockUserManager.stub { on { users } doReturn emptyList() }

                val shadowActivity = shadowOf(activity)
                shadowActivity.receiveResult(
                    shadowActivity.nextStartedActivityForResult.intent,
                    RESULT_OK,
                    null,
                )

                assertThat(activity.isFinishing).isTrue()
            }
            assertThat(scenario.result.resultCode).isEqualTo(RESULT_CANCELED)
        }
        verify(mockSupervisionManager, never()).setSupervisionEnabled(any())
    }

    @Test
    fun onSetLockResult_supervisingUserNotSecure_canceled() {
        ActivityScenario.launchActivityForResult(SetupSupervisionActivity::class.java).use {
            scenario ->
            scenario.onActivity { activity ->
                val shadowActivity = shadowOf(activity)
                shadowActivity.receiveResult(
                    shadowActivity.nextStartedActivityForResult.intent,
                    RESULT_OK,
                    null,
                )

                assertThat(activity.isFinishing).isTrue()
            }
            assertThat(scenario.result.resultCode).isEqualTo(RESULT_CANCELED)
        }
        verify(mockSupervisionManager, never()).setSupervisionEnabled(any())
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_SUPERVISION_PIN_RECOVERY_SCREEN)
    fun onPinRecoveryResult_finishesOk() {
        shadowKeyguardManager.setIsDeviceSecure(SUPERVISING_USER_ID, false)

        ActivityScenario.launchActivityForResult(SetupSupervisionActivity::class.java).use {
            scenario ->
            scenario.onActivity { activity ->
                val shadowActivity = shadowOf(activity)
                shadowKeyguardManager.setIsDeviceSecure(SUPERVISING_USER_ID, true)
                // Set PIN result.
                shadowActivity.receiveResult(
                    shadowActivity.nextStartedActivityForResult.intent,
                    RESULT_OK,
                    null,
                )

                // PIN recovery result.
                shadowActivity.receiveResult(
                    shadowActivity.nextStartedActivityForResult.intent,
                    RESULT_OK,
                    null,
                )

                assertThat(activity.isFinishing).isTrue()
            }
            assertThat(scenario.result.resultCode).isEqualTo(RESULT_OK)
        }
    }

    @Test
    fun onCreate_createUserFails_showsErrorScreen() {
        mockUserManager.stub {
            on { users } doReturn emptyList()
            on {
                createProfileForUserEvenWhenDisallowed(any(), any(), any(), any(), anyOrNull())
            } doReturn null
        }

        ActivityScenario.launch(SetupSupervisionActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val nextStartedActivity = shadowOf(activity).nextStartedActivity
                assertThat(nextStartedActivity.component?.className)
                    .isEqualTo(SupervisionErrorActivity::class.java.name)

                assertThat(activity.isFinishing).isTrue()
            }
        }
    }

    private companion object {
        const val SUPERVISING_USER_ID = 5
        val SUPERVISING_USER_INFO =
            UserInfo(
                SUPERVISING_USER_ID,
                /* name */ "supervising",
                /* iconPath */ "",
                /* flags */ 0,
                USER_TYPE_PROFILE_SUPERVISING,
            )
    }
}
