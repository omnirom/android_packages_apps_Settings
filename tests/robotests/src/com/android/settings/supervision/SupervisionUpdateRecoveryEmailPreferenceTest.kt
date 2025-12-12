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

import android.app.supervision.SupervisionManager
import android.app.supervision.SupervisionRecoveryInfo
import android.app.supervision.SupervisionRecoveryInfo.STATE_PENDING
import android.app.supervision.SupervisionRecoveryInfo.STATE_VERIFIED
import android.app.supervision.flags.Flags
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.platform.test.annotations.DisableFlags
import android.platform.test.annotations.EnableFlags
import android.platform.test.flag.junit.SetFlagsRule
import androidx.preference.Preference
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.R
import com.android.settingslib.metadata.PreferenceLifecycleContext
import com.android.settingslib.preference.createAndBindWidget
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@RunWith(AndroidJUnit4::class)
class SupervisionUpdateRecoveryEmailPreferenceTest {

    private val appContext: Context = ApplicationProvider.getApplicationContext()
    private val mockLifeCycleContext = mock<PreferenceLifecycleContext>()
    private val mockSupervisionManager = mock<SupervisionManager>()

    @get:Rule val setFlagsRule = SetFlagsRule()
    private val preference = SupervisionUpdateRecoveryEmailPreference()
    private val context =
        object : ContextWrapper(appContext) {
            override fun getSystemService(name: String): Any =
                when (name) {
                    Context.SUPERVISION_SERVICE -> mockSupervisionManager
                    else -> super.getSystemService(name)
                }
        }

    @Before
    fun setUp() {
        preference.onCreate(mockLifeCycleContext)
    }

    @Test
    fun getTitle() {
        assertThat(preference.title)
            .isEqualTo(R.string.supervision_update_recovery_email_preference_title)
    }

    @Test
    fun getSummary_accountNameAvailable_regularEmail() {
        val recoveryInfo =
            SupervisionRecoveryInfo(
                /* accountName */ "test@example.com",
                /* accountType */ "default",
                /* state */ STATE_VERIFIED,
                /* accountData */ null,
            )
        whenever(mockSupervisionManager.supervisionRecoveryInfo).thenReturn(recoveryInfo)

        assertThat(preference.getSummary(context).toString()).isEqualTo("t••t@example.com %s")
    }

    @Test
    fun getSummary_accountNameAvailable_invalidEmail() {
        val recoveryInfo =
            SupervisionRecoveryInfo(
                /* accountName */ "test.com",
                /* accountType */ "default",
                /* state */ STATE_VERIFIED,
                /* accountData */ null,
            )
        whenever(mockSupervisionManager.supervisionRecoveryInfo).thenReturn(recoveryInfo)

        assertThat(preference.getSummary(context)).isNull()
    }

    @Test
    fun getSummary_accountNameAvailable_singleCharEmail() {
        val recoveryInfo =
            SupervisionRecoveryInfo(
                /* accountName */ "t@example.com",
                /* accountType */ "default",
                /* state */ STATE_VERIFIED,
                /* accountData */ null,
            )
        whenever(mockSupervisionManager.supervisionRecoveryInfo).thenReturn(recoveryInfo)

        assertThat(preference.getSummary(context).toString()).isEqualTo("t@example.com %s")
    }

    @Test
    fun getSummary_accountNameAvailable_twoCharsEmail() {
        val recoveryInfo =
            SupervisionRecoveryInfo(
                /* accountName */ "te@example.com",
                /* accountType */ "default",
                /* state */ STATE_VERIFIED,
                /* accountData */ null,
            )
        whenever(mockSupervisionManager.supervisionRecoveryInfo).thenReturn(recoveryInfo)

        assertThat(preference.getSummary(context).toString()).isEqualTo("t•@example.com %s")
    }

    @Test
    fun getSummary_accountNameNotAvailable() {
        whenever(mockSupervisionManager.supervisionRecoveryInfo).thenReturn(null)

        assertThat(preference.getSummary(context)).isNull()
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_SUPERVISION_PIN_RECOVERY_SCREEN)
    fun flagEnabled_recoveryInfoNotExist_notAvailable() {
        whenever(mockSupervisionManager.supervisionRecoveryInfo).thenReturn(null)

        assertThat(preference.isAvailable(context)).isFalse()
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_SUPERVISION_PIN_RECOVERY_SCREEN)
    fun flagEnabled_recoveryNotVerified_notAvailable() {
        val recoveryInfo =
            SupervisionRecoveryInfo(
                /* accountName */ "test@example.com",
                /* accountType */ "default",
                /* state */ STATE_PENDING,
                /* accountData */ null,
            )
        whenever(mockSupervisionManager.supervisionRecoveryInfo).thenReturn(recoveryInfo)

        assertThat(preference.isAvailable(context)).isFalse()
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_SUPERVISION_PIN_RECOVERY_SCREEN)
    fun flagEnabled_recoveryVerified_isAvailable() {
        val recoveryInfo =
            SupervisionRecoveryInfo(
                /* accountName */ "email",
                /* accountType */ "default",
                /* state */ STATE_VERIFIED,
                /* accountData */ null,
            )
        whenever(mockSupervisionManager.supervisionRecoveryInfo).thenReturn(recoveryInfo)

        assertThat(preference.isAvailable(context)).isTrue()
    }

    @Test
    @DisableFlags(Flags.FLAG_ENABLE_SUPERVISION_PIN_RECOVERY_SCREEN)
    fun flagDisabled_recoveryVerified_notAvailable() {
        val recoveryInfo =
            SupervisionRecoveryInfo(
                /* accountName */ "email",
                /* accountType */ "default",
                /* state */ STATE_VERIFIED,
                /* accountData */ null,
            )
        whenever(mockSupervisionManager.supervisionRecoveryInfo).thenReturn(recoveryInfo)

        assertThat(preference.isAvailable(context)).isFalse()
    }

    @Test
    fun onClick_triggersPinRecoveryActivity() {
        val widget: Preference = preference.createAndBindWidget(context)

        mockLifeCycleContext.stub {
            on { findPreference<Preference>(SupervisionUpdateRecoveryEmailPreference.KEY) } doReturn
                widget
        }

        widget.performClick()

        verifyPinRecoveryActivityStarted()
    }

    private fun verifyPinRecoveryActivityStarted() {
        val intentCaptor = argumentCaptor<Intent>()
        verify(mockLifeCycleContext)
            .startActivityForResult(
                intentCaptor.capture(),
                eq(SupervisionUpdateRecoveryEmailPreference.REQUEST_CODE_UPDATE_RECOVERY),
                eq(null),
            )
        assertThat(intentCaptor.allValues.size).isEqualTo(1)
        assertThat(intentCaptor.firstValue.component?.className)
            .isEqualTo(SupervisionPinRecoveryActivity::class.java.name)
        assertThat(intentCaptor.firstValue.action)
            .isEqualTo(SupervisionPinRecoveryActivity.ACTION_UPDATE)
    }
}
