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
import android.app.admin.DevicePolicyManager
import android.app.role.RoleManager
import android.app.supervision.SupervisionManager
import android.app.supervision.flags.Flags
import android.content.Context
import android.content.pm.UserInfo
import android.os.Build
import android.os.UserManager
import android.os.UserManager.USER_TYPE_PROFILE_SUPERVISING
import android.platform.test.annotations.DisableFlags
import android.provider.Settings.Secure.USER_SETUP_COMPLETE
import android.provider.Settings.Secure.putInt
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.testutils.shadow.SettingsShadowResources
import com.google.common.truth.Truth.assertThat
import java.util.function.Consumer
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.robolectric.Robolectric
import org.robolectric.Shadows.shadowOf
import org.robolectric.android.controller.ActivityController
import org.robolectric.annotation.Config
import org.robolectric.shadow.api.Shadow
import org.robolectric.shadows.ShadowActivity
import org.robolectric.shadows.ShadowContextImpl
import org.robolectric.shadows.ShadowDialog

@Config(shadows = [SettingsShadowResources::class])
@RunWith(AndroidJUnit4::class)
class EnableSupervisionActivityTest {
    private val mockSupervisionManager = mock<SupervisionManager>()
    private val mockRoleManager = mock<RoleManager>()
    private val mockUserManager = mock<UserManager>()
    private val mockDevicePolicyManager = mock<DevicePolicyManager>()

    private lateinit var mActivity: EnableSupervisionActivity
    private lateinit var mActivityController: ActivityController<EnableSupervisionActivity>
    private lateinit var shadowActivity: ShadowActivity

    private val callingPackage = "com.example.caller"

    @Before
    fun setUp() {
        // Note, we have to use ActivityController (instead of ActivityScenario) in order to access
        // the activity before it is created, so we can set up various mocked responses before they
        // are referenced in onCreate.
        mActivityController = Robolectric.buildActivity(EnableSupervisionActivity::class.java)
        mActivity = mActivityController.get()

        shadowActivity = shadowOf(mActivity)
        shadowActivity.setCallingPackage(callingPackage)
        Shadow.extract<ShadowContextImpl>(mActivity.baseContext).apply {
            setSystemService(Context.DEVICE_POLICY_SERVICE, mockDevicePolicyManager)
            setSystemService(Context.SUPERVISION_SERVICE, mockSupervisionManager)
            setSystemService(Context.ROLE_SERVICE, mockRoleManager)
            setSystemService(Context.USER_SERVICE, mockUserManager)
        }
    }

    @After
    fun teardown() {
        mActivityController.pause().stop().destroy()
    }

    @Test
    fun onCreate_callerAcquiredSupervisionRole_EnablesSupervision() = runBlocking {
        whenever(mockUserManager.users).thenReturn(listOf(SUPERVISING_USER_INFO))

        mActivityController.setup()

        val captor = argumentCaptor<Consumer<Boolean>>()
        verify(mockRoleManager)
            .addRoleHolderAsUser(any(), any(), any(), any(), any(), captor.capture())
        captor.firstValue.accept(true)

        verify(mockSupervisionManager).setSupervisionEnabled(true)

        assertThat(shadowActivity.resultCode).isEqualTo(Activity.RESULT_OK)
        assertThat(mActivity.isFinishing).isTrue()
    }

    @Test
    fun onCreate_callerDoesNotAcquireSupervisionRole_DoesNotEnableSupervision() = runBlocking {
        whenever(mockUserManager.users).thenReturn(listOf(SUPERVISING_USER_INFO))

        mActivityController.setup()

        val captor = argumentCaptor<Consumer<Boolean>>()
        verify(mockRoleManager)
            .addRoleHolderAsUser(any(), any(), any(), any(), any(), captor.capture())
        captor.firstValue.accept(false)
        verifyNoInteractions(mockSupervisionManager)

        assertThat(shadowActivity.resultCode).isEqualTo(Activity.RESULT_CANCELED)
        assertThat(mActivity.isFinishing).isTrue()
    }

    @Test
    fun onCreate_SystemSupervisionHolderAndIsProfileOwnerAndUserSetupComplete_SkipsUserConfirmation() =
        runBlocking {
            whenever(mockUserManager.users).thenReturn(listOf(SUPERVISING_USER_INFO))
            whenever(mockRoleManager.getRoleHolders(any())).thenReturn(listOf(callingPackage))
            whenever(mockDevicePolicyManager.isProfileOwnerApp(any())).thenReturn(true)
            putInt(mActivity.contentResolver, USER_SETUP_COMPLETE, 1)

            mActivityController.setup()

            val captor = argumentCaptor<Consumer<Boolean>>()
            verify(mockRoleManager)
                .addRoleHolderAsUser(any(), any(), any(), any(), any(), captor.capture())
            captor.firstValue.accept(true)

            assertThat(mActivity.canSkipUserConfirmation(callingPackage)).isTrue()
            assertThat(ShadowDialog.getLatestDialog()).isNull()

            verify(mockSupervisionManager).setSupervisionEnabled(true)

            assertThat(shadowActivity.resultCode).isEqualTo(Activity.RESULT_OK)
            assertThat(mActivity.isFinishing).isTrue()
        }

    @Test
    fun onCreate_SystemSupervisionHolderAndNotProfileOwnerAndUserSetupNotCompleted_SkipsUserConfirmation() =
        runBlocking {
            whenever(mockUserManager.users).thenReturn(listOf(SUPERVISING_USER_INFO))
            whenever(mockRoleManager.getRoleHolders(any())).thenReturn(listOf(callingPackage))
            whenever(mockDevicePolicyManager.isProfileOwnerApp(any())).thenReturn(false)
            putInt(mActivity.contentResolver, USER_SETUP_COMPLETE, 0)

            mActivityController.setup()

            val captor = argumentCaptor<Consumer<Boolean>>()
            verify(mockRoleManager)
                .addRoleHolderAsUser(any(), any(), any(), any(), any(), captor.capture())
            captor.firstValue.accept(true)

            assertThat(mActivity.canSkipUserConfirmation(callingPackage)).isTrue()
            assertThat(ShadowDialog.getLatestDialog()).isNull()

            verify(mockSupervisionManager).setSupervisionEnabled(true)

            assertThat(shadowActivity.resultCode).isEqualTo(Activity.RESULT_OK)
            assertThat(mActivity.isFinishing).isTrue()
        }

    @Test
    @Config(sdk = [Build.VERSION_CODES.BAKLAVA])
    @DisableFlags(Flags.FLAG_ENABLE_CONFIRMATION_DIALOG_BYPASS)
    fun onCreate_SystemSupervisionHolderAndNotProfileOwnerAndUserSetupCompletedBypassFlagDisabled_CannotSkipUserConfirmation() =
        runBlocking {
            whenever(mockUserManager.users).thenReturn(listOf(SUPERVISING_USER_INFO))
            whenever(mockRoleManager.getRoleHolders(any())).thenReturn(listOf(callingPackage))
            whenever(mockDevicePolicyManager.isProfileOwnerApp(any())).thenReturn(false)
            putInt(mActivity.contentResolver, USER_SETUP_COMPLETE, 1)

            mActivityController.setup()

            assertThat(mActivity.canSkipUserConfirmation(callingPackage)).isFalse()

            assertThat(shadowActivity.resultCode).isEqualTo(Activity.RESULT_CANCELED)
            assertThat(mActivity.isFinishing).isFalse()
        }

    @Test
    fun packageInAllowedSupervisionRolePackages_isAllowed() = runBlocking {
        val resourceId =
            mActivity.resources.getIdentifier(
                "config_allowedSupervisionRolePackages",
                "string",
                "android",
            )
        SettingsShadowResources.overrideResource(
            resourceId,
            "com.example.caller;com.example.other.caller",
        )

        mActivityController.setup()

        assertThat(mActivity.isAllowedPackage(callingPackage)).isTrue()
    }

    @Test
    fun packageNotInAllowedSupervisionRolePackages_isNotAllowed() = runBlocking {
        val resourceId =
            mActivity.resources.getIdentifier(
                "config_allowedSupervisionRolePackages",
                "string",
                "android",
            )
        SettingsShadowResources.overrideResource(
            resourceId,
            "com.example.not.that.caller;com.example.other.caller",
        )

        mActivityController.setup()

        assertThat(mActivity.isAllowedPackage(callingPackage)).isFalse()
    }

    // TODO: b/393418334 - Add show dialog tests when full versions are supported and
    //  setDeviceUpgrading faking is possible.

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
