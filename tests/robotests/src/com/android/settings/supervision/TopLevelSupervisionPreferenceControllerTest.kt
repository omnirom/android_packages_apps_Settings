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
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.content.res.Resources
import androidx.preference.Preference
import com.android.settings.core.BasePreferenceController.AVAILABLE
import com.android.settings.core.BasePreferenceController.UNSUPPORTED_ON_DEVICE
import com.android.settings.supervision.SupervisionHelper.INSTALL_SUPERVISION_APP_ACTION
import com.android.settings.supervision.ipc.SupervisionMessengerClient.Companion.SUPERVISION_MESSENGER_SERVICE_BIND_ACTION
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.argThat
import org.mockito.kotlin.mock
import org.mockito.kotlin.spy
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class TopLevelSupervisionPreferenceControllerTest {
    private val mockPackageManager = mock<PackageManager>()
    private val mockResources = mock<Resources>()
    private val context =
        spy(Robolectric.buildActivity(Activity::class.java).get()) {
            on { packageManager }.thenReturn(mockPackageManager)
            on { resources }.thenReturn(mockResources)
        }

    private val preference = Preference(context)

    @Before
    fun setUp() {
        preference.key = PREFERENCE_KEY
        mockResources.stub {
            on { getString(com.android.internal.R.string.config_systemSupervision) }
                .thenReturn(SUPERVISION_PACKAGE_NAME)
        }
    }

    @Test
    fun navigateToDashboard() {
        setupMessengerServiceActionResolution(true)
        setUpSupervisionInstallActionResolution(true)

        val preferenceController = TopLevelSupervisionPreferenceController(context, PREFERENCE_KEY)

        assertThat(preferenceController.availabilityStatus).isEqualTo(AVAILABLE)

        preferenceController.handlePreferenceTreeClick(preference)
        verify(context)
            .startActivity(componentIntentMatcher(SupervisionDashboardActivity::class.java))
    }

    @Test
    fun noSupervisionMessengerService_canNotInstall_returnUnsupported() {
        setupMessengerServiceActionResolution(false)
        setUpSupervisionInstallActionResolution(false)

        val preferenceController = TopLevelSupervisionPreferenceController(context, PREFERENCE_KEY)
        assertThat(preferenceController.availabilityStatus).isEqualTo(UNSUPPORTED_ON_DEVICE)
    }

    @Test
    fun hasSupervisionMessengerService_canNotInstall_returnAvailable() {
        setupMessengerServiceActionResolution(true)
        setUpSupervisionInstallActionResolution(false)

        val preferenceController = TopLevelSupervisionPreferenceController(context, PREFERENCE_KEY)
        assertThat(preferenceController.availabilityStatus).isEqualTo(AVAILABLE)
    }

    @Test
    fun noSupervisionMessengerService_canInstall_returnAvailable() {
        setupMessengerServiceActionResolution(false)
        setUpSupervisionInstallActionResolution(true)

        val preferenceController = TopLevelSupervisionPreferenceController(context, PREFERENCE_KEY)
        assertThat(preferenceController.availabilityStatus).isEqualTo(AVAILABLE)
    }

    private fun setupMessengerServiceActionResolution(canResolve: Boolean) {
        val resolveInfoList = if (canResolve) listOf(ResolveInfo()) else listOf()
        mockPackageManager.stub {
            on {
                    queryIntentServices(
                        actionIntentMatcher(SUPERVISION_MESSENGER_SERVICE_BIND_ACTION),
                        any<Int>(),
                    )
                }
                .thenReturn(resolveInfoList)
        }
    }

    private fun setUpSupervisionInstallActionResolution(canResolve: Boolean) {
        val resolveInfoList =
            if (canResolve) listOf(ResolveInfo().apply { activityInfo = ActivityInfo() })
            else listOf()
        mockPackageManager.stub {
            on {
                    queryIntentActivities(
                        actionIntentMatcher(INSTALL_SUPERVISION_APP_ACTION),
                        any<Int>(),
                    )
                }
                .thenReturn(resolveInfoList)
        }
    }

    private fun componentIntentMatcher(cls: Class<*>) =
        argThat<Intent> { this.component?.className == cls.name }

    private fun actionIntentMatcher(action: String) = argThat<Intent> { this.action == action }

    private companion object {
        const val PREFERENCE_KEY = "test_key"
        const val SUPERVISION_PACKAGE_NAME = "com.android.supervision"
    }
}
