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
import android.app.supervision.SupervisionManager
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import androidx.preference.Preference
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.supervision.SupervisionMainSwitchPreference.Companion.REQUEST_CODE_CONFIRM_SUPERVISION_CREDENTIALS
import com.android.settingslib.metadata.PreferenceLifecycleContext
import com.android.settingslib.preference.createAndBindWidget
import com.android.settingslib.widget.MainSwitchPreference
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify

@RunWith(AndroidJUnit4::class)
class SupervisionMainSwitchPreferenceTest {
    private val mockLifeCycleContext = mock<PreferenceLifecycleContext>()
    private val mockSupervisionManager = mock<SupervisionManager>()

    private val appContext: Context = ApplicationProvider.getApplicationContext()
    private val context =
        object : ContextWrapper(appContext) {
            override fun getSystemService(name: String): Any =
                when (name) {
                    Context.SUPERVISION_SERVICE -> mockSupervisionManager
                    else -> super.getSystemService(name)
                }
        }

    private val preference = SupervisionMainSwitchPreference(context)

    @Before
    fun setUp() {
        preference.onCreate(mockLifeCycleContext)
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
    fun toggleOn_triggersPinVerification() {
        setSupervisionEnabled(false)
        val widget = getMainSwitchPreference()

        assertThat(widget.isChecked).isFalse()

        widget.performClick()

        verifyConfirmSupervisionCredentialsActivityStarted()
        assertThat(widget.isChecked).isFalse()
        verify(mockSupervisionManager, never()).setSupervisionEnabled(false)
    }

    @Test
    fun toggleOn_pinVerificationSucceeded_supervisionEnabled() {
        setSupervisionEnabled(false)
        val widget = getMainSwitchPreference()

        assertThat(widget.isChecked).isFalse()

        preference.onActivityResult(
            mockLifeCycleContext,
            REQUEST_CODE_CONFIRM_SUPERVISION_CREDENTIALS,
            Activity.RESULT_OK,
            null,
        )

        assertThat(widget.isChecked).isTrue()
        verify(mockSupervisionManager).setSupervisionEnabled(true)
    }

    @Test
    fun toggleOff_pinVerificationSucceeded_supervisionDisabled() {
        setSupervisionEnabled(true)
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
    }

    @Test
    fun toggleOff_pinVerificationFailed_supervisionNotEnabled() {
        setSupervisionEnabled(true)
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

    private fun setSupervisionEnabled(enabled: Boolean) =
        mockSupervisionManager.stub { on { isSupervisionEnabled } doReturn enabled }

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

    private fun verifyConfirmSupervisionCredentialsActivityStarted() {
        val intentCaptor = argumentCaptor<Intent>()
        verify(mockLifeCycleContext)
            .startActivityForResult(
                intentCaptor.capture(),
                eq(REQUEST_CODE_CONFIRM_SUPERVISION_CREDENTIALS),
                eq(null),
            )
        assertThat(intentCaptor.allValues.size).isEqualTo(1)
        assertThat(intentCaptor.firstValue.component?.className)
            .isEqualTo(ConfirmSupervisionCredentialsActivity::class.java.name)
    }
}
