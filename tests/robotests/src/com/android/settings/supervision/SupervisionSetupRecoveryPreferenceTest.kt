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

import android.app.settings.SettingsEnums.ACTION_SUPERVISION_ADD_RECOVERY
import android.app.settings.SettingsEnums.ACTION_SUPERVISION_VERIFY_RECOVERY
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
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.preference.Preference
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.R
import com.android.settings.testutils.MetricsRule
import com.android.settingslib.metadata.PreferenceLifecycleContext
import com.android.settingslib.preference.createAndBindWidget
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@RunWith(AndroidJUnit4::class)
class SupervisionSetupRecoveryPreferenceTest {

    private val appContext: Context = ApplicationProvider.getApplicationContext()
    private val mockLifeCycleContext = mock<PreferenceLifecycleContext>()
    private val mockSupervisionManager = mock<SupervisionManager>()

    private val mockActivityResultLauncher = mock<ActivityResultLauncher<Intent>>()

    @get:Rule val metricsRule = MetricsRule()
    @get:Rule val setFlagsRule = SetFlagsRule()
    private var preference = SupervisionSetupRecoveryPreference()
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
        whenever(
                mockLifeCycleContext.registerForActivityResult(
                    any<ActivityResultContracts.StartActivityForResult>(),
                    any(),
                )
            )
            .thenReturn(mockActivityResultLauncher)
        preference.onCreate(mockLifeCycleContext)
    }

    @Test
    fun getTitle_addRecovery() {
        whenever(mockSupervisionManager.supervisionRecoveryInfo).thenReturn(null)

        assertThat(preference.getTitle(context))
            .isEqualTo(context.getString(R.string.supervision_add_pin_recovery_title))
    }

    @Test
    fun getTitle_verifyRecovery() {
        val recoveryInfo =
            SupervisionRecoveryInfo(
                /* accountName */ "email",
                /* accountType */ "default",
                /* state */ STATE_PENDING,
                /* accountData */ null,
            )
        whenever(mockSupervisionManager.supervisionRecoveryInfo).thenReturn(recoveryInfo)

        assertThat(preference.getTitle(context))
            .isEqualTo(context.getString(R.string.supervision_verify_pin_recovery_title))
    }

    @Test
    fun getSummary_addRecovery() {
        whenever(mockSupervisionManager.supervisionRecoveryInfo).thenReturn(null)

        assertThat(preference.getSummary(context)).isNull()
    }

    @Test
    fun getSummary_verifyRecovery() {
        val recoveryInfo =
            SupervisionRecoveryInfo(
                /* accountName */ "test@gmail.com",
                /* accountType */ "default",
                /* state */ STATE_PENDING,
                /* accountData */ null,
            )
        whenever(mockSupervisionManager.supervisionRecoveryInfo).thenReturn(recoveryInfo)

        assertThat(preference.getSummary(context)).isEqualTo("t••t@gmail.com")
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_SUPERVISION_PIN_RECOVERY_SCREEN)
    fun flagEnabled_recoveryNotExist_isAvailable() {
        whenever(mockSupervisionManager.supervisionRecoveryInfo).thenReturn(null)

        assertThat(preference.isAvailable(context)).isTrue()
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_SUPERVISION_PIN_RECOVERY_SCREEN)
    fun flagEnabled_recoveryExistNotVerified_isAvailable() {
        val recoveryInfo =
            SupervisionRecoveryInfo(
                /* accountName */ "email",
                /* accountType */ "default",
                /* state */ STATE_PENDING,
                /* accountData */ null,
            )
        whenever(mockSupervisionManager.supervisionRecoveryInfo).thenReturn(recoveryInfo)

        assertThat(preference.isAvailable(context)).isTrue()
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_SUPERVISION_PIN_RECOVERY_SCREEN)
    fun flagEnabled_recoveryVerified_NotAvailable() {
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
    @DisableFlags(Flags.FLAG_ENABLE_SUPERVISION_PIN_RECOVERY_SCREEN)
    fun flagDisabled_recoveryInfoNotExist_notAvailable() {
        whenever(mockSupervisionManager.supervisionRecoveryInfo).thenReturn(null)

        assertThat(preference.isAvailable(context)).isFalse()
    }

    @Test
    fun addRecovery_onClick_triggersPinRecoveryActivity() {
        whenever(mockSupervisionManager.supervisionRecoveryInfo).thenReturn(null)
        val widget: Preference = preference.createAndBindWidget(context)

        mockLifeCycleContext.stub {
            on { findPreference<Preference>(SupervisionSetupRecoveryPreference.KEY) } doReturn
                widget
            on { getSystemService(SupervisionManager::class.java) } doReturn mockSupervisionManager
        }
        widget.performClick()

        verifyPinRecoveryActivityStarted(SupervisionPinRecoveryActivity.ACTION_SETUP_VERIFIED)
        verify(metricsRule.metricsFeatureProvider).action(context, ACTION_SUPERVISION_ADD_RECOVERY)
    }

    @Test
    fun verifyRecovery_onClick_triggersPinRecoveryActivity() {
        val recoveryInfo =
            SupervisionRecoveryInfo(
                /* accountName */ "email",
                /* accountType */ "default",
                /* state */ STATE_PENDING,
                /* accountData */ null,
            )
        whenever(mockSupervisionManager.supervisionRecoveryInfo).thenReturn(recoveryInfo)
        val widget: Preference = preference.createAndBindWidget(context)

        mockLifeCycleContext.stub {
            on { findPreference<Preference>(SupervisionSetupRecoveryPreference.KEY) } doReturn
                widget
            on { getSystemService(SupervisionManager::class.java) } doReturn mockSupervisionManager
        }

        widget.performClick()

        verifyPinRecoveryActivityStarted(SupervisionPinRecoveryActivity.ACTION_POST_SETUP_VERIFY)
        verify(metricsRule.metricsFeatureProvider)
            .action(context, ACTION_SUPERVISION_VERIFY_RECOVERY)
    }

    private fun verifyPinRecoveryActivityStarted(expectedAction: String) {
        val intentCaptor = argumentCaptor<Intent>()
        verify(mockActivityResultLauncher).launch(intentCaptor.capture())
        assertThat(intentCaptor.allValues.size).isEqualTo(1)
        val intent = intentCaptor.firstValue
        assertThat(intent.component?.className)
            .isEqualTo(SupervisionPinRecoveryActivity::class.java.name)
        assertThat(intent.action).isEqualTo(expectedAction)
    }
}
