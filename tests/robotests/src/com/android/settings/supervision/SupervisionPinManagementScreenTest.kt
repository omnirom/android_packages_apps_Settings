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

import android.app.KeyguardManager
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.UserInfo
import android.os.UserManager
import android.os.UserManager.USER_TYPE_PROFILE_SUPERVISING
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.R
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@RunWith(AndroidJUnit4::class)
class SupervisionPinManagementScreenTest {
    private val mockKeyguardManager = mock<KeyguardManager>()
    private val mockUserManager = mock<UserManager>()

    private val context: Context =
        object : ContextWrapper(ApplicationProvider.getApplicationContext()) {
            override fun getSystemService(name: String): Any? =
                when (name) {
                    Context.KEYGUARD_SERVICE -> mockKeyguardManager
                    Context.USER_SERVICE -> mockUserManager
                    else -> super.getSystemService(name)
                }
        }

    private val supervisionPinManagementScreen = SupervisionPinManagementScreen()

    @Before
    fun setup() {
        SupervisionHelper.sInstance = null
    }

    @Test
    fun key() {
        assertThat(supervisionPinManagementScreen.key).isEqualTo(SupervisionPinManagementScreen.KEY)
    }

    @Test
    fun isAvailable() {
        whenever(mockUserManager.users).thenReturn(listOf(SUPERVISING_USER_INFO))
        whenever(mockKeyguardManager.isDeviceSecure(SUPERVISING_USER_ID)).thenReturn(true)

        assertThat(supervisionPinManagementScreen.isAvailable(context)).isTrue()
    }

    @Test
    fun isAvailable_noSupervisingUser_returnsFalse() {
        whenever(mockUserManager.users).thenReturn(emptyList())
        whenever(mockKeyguardManager.isDeviceSecure(SUPERVISING_USER_ID)).thenReturn(true)

        assertThat(supervisionPinManagementScreen.isAvailable(context)).isFalse()
    }

    @Test
    fun isAvailable_noSupervisingCredential_returnsFalse() {
        whenever(mockUserManager.users).thenReturn(listOf(SUPERVISING_USER_INFO))
        whenever(mockKeyguardManager.isDeviceSecure(SUPERVISING_USER_ID)).thenReturn(false)

        assertThat(supervisionPinManagementScreen.isAvailable(context)).isFalse()
    }

    @Test
    fun getTitle() {
        assertThat(supervisionPinManagementScreen.title)
            .isEqualTo(R.string.supervision_pin_management_preference_title)
    }

    @Test
    fun getSummary_addPin() {
        assertThat(supervisionPinManagementScreen.summary)
            .isEqualTo(R.string.supervision_pin_management_preference_summary_add)
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
