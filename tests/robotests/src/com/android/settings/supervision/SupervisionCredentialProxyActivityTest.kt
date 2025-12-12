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
import android.app.ActivityManager
import android.app.ComponentCaller
import android.app.admin.DevicePolicyManager
import android.content.Context
import android.content.pm.UserInfo
import android.os.UserHandle
import android.os.UserManager
import android.os.UserManager.USER_TYPE_PROFILE_SUPERVISING
import android.os.UserManager.USER_TYPE_PROFILE_TEST
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.internal.widget.LockPatternUtils
import com.android.settings.password.ChooseLockGeneric
import com.android.settings.supervision.SupervisionCredentialProxyActivity.Companion.REQUEST_CODE_SUPERVISION_CREDENTIALS_PROXY
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify
import org.robolectric.Robolectric
import org.robolectric.Shadows.shadowOf
import org.robolectric.android.controller.ActivityController
import org.robolectric.shadow.api.Shadow
import org.robolectric.shadows.ShadowActivity
import org.robolectric.shadows.ShadowContextImpl

@RunWith(AndroidJUnit4::class)
class SupervisionCredentialProxyActivityTest {
    private val mockActivityManager = mock<ActivityManager>()
    private val mockUserManager = mock<UserManager>()

    private lateinit var mActivity: SupervisionCredentialProxyActivity
    private lateinit var mActivityController: ActivityController<SupervisionCredentialProxyActivity>

    private lateinit var shadowActivity: ShadowActivity

    @Before
    fun setUp() {
        // Note, we have to use ActivityController (instead of ActivityScenario) in order to access
        // the activity before it is created, so we can set up various mocked responses before they
        // are referenced in onCreate.
        mActivityController =
            Robolectric.buildActivity(SupervisionCredentialProxyActivity::class.java)
        mActivity = mActivityController.get()

        shadowActivity = shadowOf(mActivity)
        Shadow.extract<ShadowContextImpl>(mActivity.baseContext).apply {
            setSystemService(Context.ACTIVITY_SERVICE, mockActivityManager)
            setSystemService(Context.USER_SERVICE, mockUserManager)
        }
    }

    @Test
    fun onCreate_supervisingUser_canStartProfile_startSetPinActivity() {
        mockUserManager.stub { on { users } doReturn listOf(SUPERVISING_USER_INFO) }
        mockActivityManager.stub { on { startProfile(any()) } doReturn true }

        mActivityController.setup()

        assertThat(mActivity.isFinishing).isFalse()

        val startedIntent = shadowActivity.nextStartedActivity
        assertThat(startedIntent.component?.className).isEqualTo(ChooseLockGeneric::class.java.name)
        assertThat(startedIntent.hasExtra(LockPatternUtils.PASSWORD_TYPE_KEY)).isTrue()
        assertThat(
                startedIntent.getIntExtra(
                    LockPatternUtils.PASSWORD_TYPE_KEY,
                    -1, /* Default value not expected */
                )
            )
            .isEqualTo(DevicePolicyManager.PASSWORD_QUALITY_NUMERIC)

        // Ensure that the supervising profile is started
        val userCaptor = argumentCaptor<UserHandle>()
        verify(mockActivityManager).startProfile(userCaptor.capture())
        assert(userCaptor.lastValue.identifier == SUPERVISING_USER_ID)
    }

    @Test
    fun onCreate_callerNotHasSupervisionRole_setPinActivityNotStarted() {
        mockUserManager.stub { on { users } doReturn listOf(TESTING_USER_INFO) }
        mockActivityManager.stub { on { startProfile(any()) } doReturn true }

        mActivityController.setup()

        assertThat(shadowActivity.resultCode).isEqualTo(Activity.RESULT_CANCELED)
        assertThat(mActivity.isFinishing).isTrue()
    }

    @Test
    fun onCreate_failsToStartSupervisingProfile_setPinActivityNotStarted() {
        mockUserManager.stub { on { users } doReturn listOf(SUPERVISING_USER_INFO) }
        mockActivityManager.stub { on { startProfile(any()) } doReturn false }

        mActivityController.setup()

        assertThat(shadowActivity.resultCode).isEqualTo(Activity.RESULT_CANCELED)
        assertThat(mActivity.isFinishing).isTrue()
    }

    @Test
    fun onActivityResult_stopProfile_finishSuccessfully() {
        mockUserManager.stub { on { users } doReturn listOf(SUPERVISING_USER_INFO) }
        mockActivityManager.stub { on { stopProfile(any()) } doReturn true }

        mActivity.onActivityResult(
            REQUEST_CODE_SUPERVISION_CREDENTIALS_PROXY,
            Activity.RESULT_OK,
            null,
            ComponentCaller(null, null),
        )

        // Ensure that the supervising profile is stopped
        val userCaptor = argumentCaptor<UserHandle>()
        verify(mockActivityManager).stopProfile(userCaptor.capture())
        assert(userCaptor.lastValue.identifier == SUPERVISING_USER_ID)
        assertThat(mActivity.isFinishing).isTrue()
        assertThat(shadowActivity.resultCode).isEqualTo(Activity.RESULT_OK)
    }

    @Test
    fun onActivityResult_requestCodeNotMatch_setResultCanceled() {
        mockUserManager.stub { on { users } doReturn listOf(SUPERVISING_USER_INFO) }
        mockActivityManager.stub { on { stopProfile(any()) } doReturn true }

        mActivity.onActivityResult(
            REQUEST_CODE_OTHER,
            Activity.RESULT_OK,
            null,
            ComponentCaller(null, null),
        )

        assertThat(shadowActivity.resultCode).isEqualTo(Activity.RESULT_CANCELED)
        assertThat(mActivity.isFinishing).isTrue()
    }

    @Test
    fun onActivityResult_callerNotHasSupervisionRole_setResultCanceled() {
        mockUserManager.stub { on { users } doReturn listOf(TESTING_USER_INFO) }
        mockActivityManager.stub { on { stopProfile(any()) } doReturn true }

        mActivity.onActivityResult(
            REQUEST_CODE_SUPERVISION_CREDENTIALS_PROXY,
            Activity.RESULT_OK,
            null,
            ComponentCaller(null, null),
        )

        assertThat(shadowActivity.resultCode).isEqualTo(Activity.RESULT_CANCELED)
        assertThat(mActivity.isFinishing).isTrue()
    }

    @Test
    fun onActivityResult_failsToStopProfile_setResultCanceled() {
        mockUserManager.stub { on { users } doReturn listOf(SUPERVISING_USER_INFO) }
        mockActivityManager.stub { on { stopProfile(any()) } doReturn false }

        mActivity.onActivityResult(
            REQUEST_CODE_SUPERVISION_CREDENTIALS_PROXY,
            Activity.RESULT_OK,
            null,
            ComponentCaller(null, null),
        )

        // Ensure that the supervising profile is stopped
        val userCaptor = argumentCaptor<UserHandle>()
        verify(mockActivityManager).stopProfile(userCaptor.capture())
        assert(userCaptor.lastValue.identifier == SUPERVISING_USER_ID)
        assertThat(shadowActivity.resultCode).isEqualTo(Activity.RESULT_CANCELED)
        assertThat(mActivity.isFinishing).isTrue()
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
        const val TESTING_USER_ID = 6
        val TESTING_USER_INFO =
            UserInfo(
                TESTING_USER_ID,
                /* name */ "testing",
                /* iconPath */ "",
                /* flags */ 0,
                USER_TYPE_PROFILE_TEST,
            )
        const val REQUEST_CODE_OTHER = 0
    }
}
