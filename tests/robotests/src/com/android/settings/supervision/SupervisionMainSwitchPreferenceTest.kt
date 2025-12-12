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
import android.app.KeyguardManager
import android.app.settings.SettingsEnums.ACTION_SUPERVISION_MAIN_TOGGLE_OFF
import android.app.settings.SettingsEnums.ACTION_SUPERVISION_MAIN_TOGGLE_ON
import android.app.supervision.SupervisionManager
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.UserInfo
import android.os.UserManager
import android.os.UserManager.USER_TYPE_FULL_SYSTEM
import android.os.UserManager.USER_TYPE_PROFILE_SUPERVISING
import androidx.preference.Preference
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.R
import com.android.settings.supervision.SupervisionMainSwitchPreference.Companion.REQUEST_CODE_CONFIRM_SUPERVISION_CREDENTIALS
import com.android.settings.supervision.SupervisionMainSwitchPreference.Companion.REQUEST_CODE_SET_UP_SUPERVISION
import com.android.settings.supervision.ipc.PreferenceData
import com.android.settings.testutils.MetricsRule
import com.android.settings.testutils.shadow.ShadowAlertDialogCompat
import com.android.settingslib.metadata.PreferenceLifecycleContext
import com.android.settingslib.preference.createAndBindWidget
import com.android.settingslib.widget.MainSwitchPreference
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(shadows = [ShadowAlertDialogCompat::class])
class SupervisionMainSwitchPreferenceTest {
    private val mockLifeCycleContext = mock<PreferenceLifecycleContext>()
    private val mockKeyguardManager = mock<KeyguardManager>()
    private val mockSupervisionManager = mock<SupervisionManager>()
    private val mockUserManager = mock<UserManager>()
    private val appContext: Context = ApplicationProvider.getApplicationContext()
    private val context =
        object : ContextWrapper(appContext) {
            override fun getSystemService(name: String): Any =
                when (name) {
                    getSystemServiceName(SupervisionManager::class.java) -> mockSupervisionManager
                    getSystemServiceName(UserManager::class.java) -> mockUserManager
                    getSystemServiceName(KeyguardManager::class.java) -> mockKeyguardManager
                    else -> super.getSystemService(name)
                }
        }

    private val preferenceDataProvider: PreferenceDataProvider = mock {
        onBlocking { getPreferenceData(any()) }.thenAnswer { mapOf<String, PreferenceData>() }
    }
    private val preference = SupervisionMainSwitchPreference(context, preferenceDataProvider)

    @get:Rule val metricsRule = MetricsRule()

    @Before
    fun setUp() {
        preference.onCreate(mockLifeCycleContext)
        context.setTheme(R.style.Theme_AppCompat) // Needed for AlertDialog creation
    }

    @Test
    fun getSummary_noPinSet_returnsNoPinSummary() {
        setSupervisionEnabled(false)
        setSupervisingProfileCreated(false)

        assertThat(preference.getSummary(context)).isEqualTo("Set up PIN")
    }

    @Test
    fun getSummary_disabled_pinSet_returnsNullSummary() {
        setSupervisionEnabled(false)
        setSupervisingProfileCreated(true)

        assertThat(preference.getSummary(context)).isNull()
    }

    @Test
    fun getSummary_enabled_pinSet_returnsNullSummary() {
        setSupervisionEnabled(true)
        setSupervisingProfileCreated(true)

        assertThat(preference.getSummary(context)).isNull()
    }

    @Test
    fun checked_supervisionEnabled_returnTrue() {
        setSupervisionEnabled(true)

        assertThat(getMainSwitchPreference().isChecked).isTrue()
    }

    @Test
    fun checked_supervisionDisabled_returnFalse() {
        setSupervisionEnabled(false)

        assertThat(getMainSwitchPreference().isChecked).isFalse()
    }

    @Test
    fun toggleOn_supervisionSetUp_triggersPinVerification() {
        setSupervisionEnabled(false)
        setSupervisingProfileCreated(true)
        val widget = getMainSwitchPreference()

        assertThat(widget.isChecked).isFalse()

        widget.performClick()

        verifyConfirmPinActivityStarted()
        assertThat(widget.isChecked).isFalse()
        verify(mockSupervisionManager, never()).setSupervisionEnabled(false)
    }

    @Test
    fun toggleOff_supervisionSetUp_triggersPinVerification() {
        setSupervisionEnabled(true)
        setSupervisingProfileCreated(true)
        val widget = getMainSwitchPreference()

        assertThat(widget.isChecked).isTrue()

        widget.performClick()

        verifyConfirmPinActivityStarted()
        assertThat(widget.isChecked).isTrue()
        verify(mockSupervisionManager, never()).setSupervisionEnabled(false)
    }

    @Test
    fun toggleOn_supervisionNotSetUp_triggersSupervisionSetup() {
        setSupervisionEnabled(false)
        setSupervisingProfileCreated(false)
        val widget = getMainSwitchPreference()

        assertThat(widget.isChecked).isFalse()

        widget.performClick()

        verifyActivityStarted(
            REQUEST_CODE_SET_UP_SUPERVISION,
            SetupSupervisionActivity::class.java.name,
        )
        assertThat(widget.isChecked).isFalse()
        verify(mockSupervisionManager, never()).setSupervisionEnabled(false)
    }

    @Test
    fun toggleOn_supervisionSetUp_pinVerificationSucceeded_supervisionEnabled() {
        setSupervisionEnabled(false)
        setSupervisingProfileCreated(true)
        val widget = getMainSwitchPreference()

        assertThat(widget.isChecked).isFalse()

        widget.performClick()
        preference.onActivityResult(
            mockLifeCycleContext,
            REQUEST_CODE_CONFIRM_SUPERVISION_CREDENTIALS,
            Activity.RESULT_OK,
            null,
        )

        assertThat(widget.isChecked).isTrue()
        verify(mockSupervisionManager).setSupervisionEnabled(true)
        verify(metricsRule.metricsFeatureProvider)
            .changed(any(), eq(SupervisionMainSwitchPreference.KEY), eq(1))
        verify(metricsRule.metricsFeatureProvider)
            .action(mockLifeCycleContext, ACTION_SUPERVISION_MAIN_TOGGLE_ON)
    }

    @Test
    fun toggleOn_supervisionNotSetUp_setupSucceeded_supervisionEnabled() {
        setSupervisionEnabled(false)
        setSupervisingProfileCreated(false)
        val widget = getMainSwitchPreference()

        assertThat(widget.isChecked).isFalse()

        widget.performClick()
        preference.onActivityResult(
            mockLifeCycleContext,
            REQUEST_CODE_SET_UP_SUPERVISION,
            Activity.RESULT_OK,
            null,
        )

        assertThat(widget.isChecked).isTrue()
        verify(mockSupervisionManager).setSupervisionEnabled(true)
    }

    @Test
    fun toggleOn_supervisionNotSetUp_setupFailed_supervisionNotEnabled() {
        setSupervisionEnabled(false)
        setSupervisingProfileCreated(false)
        val widget = getMainSwitchPreference()

        assertThat(widget.isChecked).isFalse()

        preference.onActivityResult(
            mockLifeCycleContext,
            REQUEST_CODE_SET_UP_SUPERVISION,
            Activity.RESULT_CANCELED,
            null,
        )

        assertThat(widget.isChecked).isFalse()
        verify(mockSupervisionManager, never()).setSupervisionEnabled(true)
    }

    @Test
    fun toggleOn_supervisionNotSetUp_setupSucceeded_supervisionDisabled() {
        setSupervisionEnabled(true)
        setSupervisingProfileCreated(false)
        val widget = getMainSwitchPreference()

        assertThat(widget.isChecked).isTrue()
        widget.performClick()

        preference.onActivityResult(
            mockLifeCycleContext,
            REQUEST_CODE_SET_UP_SUPERVISION,
            Activity.RESULT_OK,
            null,
        )

        assertThat(widget.isChecked).isFalse()
        verify(mockSupervisionManager).setSupervisionEnabled(false)
    }

    @Test
    fun toggleOff_pinVerificationSucceeded_supervisionDisabled() {
        setSupervisionEnabled(true)
        setSupervisingProfileCreated(true)
        val widget = getMainSwitchPreference()

        assertThat(widget.isChecked).isTrue()

        preference.onActivityResult(
            mockLifeCycleContext,
            REQUEST_CODE_CONFIRM_SUPERVISION_CREDENTIALS,
            Activity.RESULT_OK,
            null,
        )

        assertThat(widget.isChecked).isFalse()
        verify(mockSupervisionManager).setSupervisionEnabled(false)
        verify(metricsRule.metricsFeatureProvider)
            .changed(any(), eq(SupervisionMainSwitchPreference.KEY), eq(0))
        verify(metricsRule.metricsFeatureProvider)
            .action(mockLifeCycleContext, ACTION_SUPERVISION_MAIN_TOGGLE_OFF)
    }

    @Test
    fun toggleOff_pinVerificationFailed_supervisionNotDisabled() {
        setSupervisionEnabled(true)
        setSupervisingProfileCreated(true)
        val widget = getMainSwitchPreference()

        assertThat(widget.isChecked).isTrue()

        preference.onActivityResult(
            mockLifeCycleContext,
            REQUEST_CODE_CONFIRM_SUPERVISION_CREDENTIALS,
            Activity.RESULT_CANCELED,
            null,
        )

        assertThat(widget.isChecked).isTrue()
        verify(mockSupervisionManager, never()).setSupervisionEnabled(true)
    }

    @Test
    fun toggleOn_multipleProfilesExist_showsErrorDialog() {
        setSupervisionEnabled(false)
        mockUserManager.stub {
            on { userProfiles } doReturn
                listOf(
                    MAIN_USER.userHandle,
                    WORK_PROFILE.userHandle,
                    SUPERVISING_PROFILE.userHandle,
                )
        }

        getMainSwitchPreference().performClick()

        val dialog = ShadowAlertDialogCompat.getLatestAlertDialog()
        val shadowDialog = ShadowAlertDialogCompat.shadowOf(dialog)
        assertThat(shadowDialog.title)
            .isEqualTo(context.getString(R.string.supervision_multi_profile_error_title))
        verify(mockLifeCycleContext, never()).startActivityForResult(any(), any(), any())
    }

    private fun setSupervisionEnabled(enabled: Boolean) =
        mockSupervisionManager.stub { on { isSupervisionEnabled } doReturn enabled }

    private fun setSupervisingProfileCreated(supervisingProfileCreated: Boolean) {
        // TODO(408027029): Might be better to mock SupervisionHelper
        mockUserManager.stub {
            on { users } doReturn
                if (supervisingProfileCreated) listOf(MAIN_USER, SUPERVISING_PROFILE)
                else listOf(MAIN_USER)
        }
        mockKeyguardManager.stub {
            on { isDeviceSecure(SUPERVISING_PROFILE.id) } doReturn supervisingProfileCreated
        }
    }

    private fun getMainSwitchPreference(): MainSwitchPreference {
        val widget: MainSwitchPreference = preference.createAndBindWidget(context)

        mockLifeCycleContext.stub {
            on { findPreference<Preference>(SupervisionMainSwitchPreference.KEY) } doReturn widget
            on {
                requirePreference<MainSwitchPreference>(SupervisionMainSwitchPreference.KEY)
            } doReturn widget
        }
        return widget
    }

    private fun verifyActivityStarted(requestCode: Int, className: String) {
        val intentCaptor = argumentCaptor<Intent>()
        verify(mockLifeCycleContext)
            .startActivityForResult(intentCaptor.capture(), eq(requestCode), eq(null))
        assertThat(intentCaptor.allValues.size).isEqualTo(1)
        assertThat(intentCaptor.firstValue.component?.className).isEqualTo(className)
    }

    private fun verifyConfirmPinActivityStarted() {
        val intentCaptor = argumentCaptor<Intent>()
        verify(mockLifeCycleContext)
            .startActivityForResult(
                intentCaptor.capture(),
                eq(REQUEST_CODE_CONFIRM_SUPERVISION_CREDENTIALS),
                eq(null),
            )
        assertThat(intentCaptor.allValues.size).isEqualTo(1)
        val intent = intentCaptor.firstValue
        assertThat(intent.component?.className)
            .isEqualTo(ConfirmSupervisionCredentialsActivity::class.java.name)
        val extras = intent.extras
        assertThat(extras).isNotNull()
        assertThat(
                extras!!.getBoolean(ConfirmSupervisionCredentialsActivity.EXTRA_FORCE_CONFIRMATION)
            )
            .isTrue()
    }

    companion object {
        private val MAIN_USER = UserInfo(0, "Main", null, 0, USER_TYPE_FULL_SYSTEM)
        private val SUPERVISING_PROFILE =
            UserInfo(10, "Supervising", null, 0, USER_TYPE_PROFILE_SUPERVISING)
        private val WORK_PROFILE =
            UserInfo(11, "Work", null, 0, UserManager.USER_TYPE_PROFILE_MANAGED)
    }
}
