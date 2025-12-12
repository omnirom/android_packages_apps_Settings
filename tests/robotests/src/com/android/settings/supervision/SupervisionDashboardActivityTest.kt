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

import android.app.Application
import android.app.admin.DevicePolicyManager
import android.app.role.RoleManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_DISABLED
import android.content.pm.PackageManager.DONT_KILL_APP
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.supervision.SupervisionDashboardActivity.Companion.INTERSTITIAL_REDIRECT_ACTION
import com.android.settings.supervision.SupervisionHelper.INSTALL_SUPERVISION_APP_ACTION
import com.android.settings.supervision.ipc.SupervisionMessengerClient
import com.android.settings.testutils.shadow.SettingsShadowResources
import com.android.settingslib.ipc.MessengerServiceRule
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode
import org.robolectric.shadow.api.Shadow
import org.robolectric.shadows.ShadowContextImpl
import org.robolectric.shadows.ShadowDevicePolicyManager

@Config(shadows = [SettingsShadowResources::class])
@RunWith(AndroidJUnit4::class)
@LooperMode(LooperMode.Mode.INSTRUMENTATION_TEST)
class SupervisionDashboardActivityTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val shadowPackageManager = shadowOf(context.packageManager)
    private val shadowDpm =
        shadowOf(context.getSystemService(DevicePolicyManager::class.java))
            as ShadowDevicePolicyManager
    private val mockRoleManager = mock<RoleManager>()

    @get:Rule
    val serviceRule =
        MessengerServiceRule<SupervisionMessengerClient>(
            TestSupervisionMessengerService::class.java
        )

    @Before
    fun setup() {
        Shadow.extract<ShadowContextImpl>((context as Application).baseContext).apply {
            setSystemService(Context.ROLE_SERVICE, mockRoleManager)
        }
        SettingsShadowResources.overrideResource(
            com.android.internal.R.string.config_systemSupervision,
            TEST_SUPERVISION_PACKAGE,
        )
        SettingsShadowResources.overrideResource(
            com.android.internal.R.string.config_defaultSupervisionProfileOwnerComponent,
            DEFAULT_SUPERVISION_COMPONENT,
        )
        shadowDpm.setProfileOwner(null)
        mockRoleManager.stub {
            on { getRoleHolders(RoleManager.ROLE_SUPERVISION) } doReturn emptyList()
        }
    }

    @Test
    fun hasNecessaryComponent_enabled_loadInitialFragment() = runTest {
        // Setup necessary supervision component to be present
        mockRoleManager.stub {
            on { getRoleHolders(eq(RoleManager.ROLE_SYSTEM_SUPERVISION)) } doReturn
                listOf(TEST_SUPERVISION_PACKAGE)
        }
        setUpMessengerServiceComponent(disabled = false)

        val activityScenario = ActivityScenario.launch(SupervisionDashboardActivity::class.java)

        activityScenario.onActivity { activity ->
            val fragmentName = activity.getInitialFragmentName(Intent())
            assertThat(fragmentName).isEqualTo(SupervisionDashboardFragment::class.java.name)
        }
    }

    @Test
    fun hasNecessaryComponent_disabled_startLoadingActivityAndFinishSelf() = runTest {
        // No supervision component to be present
        mockRoleManager.stub {
            on { getRoleHolders(eq(RoleManager.ROLE_SYSTEM_SUPERVISION)) } doReturn
                listOf(TEST_SUPERVISION_PACKAGE)
        }

        setUpMessengerServiceComponent(disabled = true)

        val activityScenario = ActivityScenario.launch(SupervisionDashboardActivity::class.java)
        activityScenario.onActivity { activity ->
            assertThat(shadowOf(activity).nextStartedActivity.component?.className)
                .isEqualTo(SupervisionDashboardLoadingActivity::class.java.name)
            assertThat(activity.isFinishing).isTrue()
        }
    }

    @Test
    fun supervisionRoleHolderExists_redirectIntentResolved_redirect() = runTest {
        mockRoleManager.stub {
            on { getRoleHolders(any()) } doReturn listOf(TEST_SUPERVISION_PACKAGE)
        }
        setUpRedirectActivityComponent(DEFAULT_SUPERVISION_PACKAGE, INTERSTITIAL_REDIRECT_ACTION)

        val activityScenario = ActivityScenario.launch(SupervisionDashboardActivity::class.java)
        activityScenario.onActivity { activity ->
            assertThat(shadowOf(activity).nextStartedActivity.action)
                .isEqualTo(INTERSTITIAL_REDIRECT_ACTION)
            assertThat(activity.isFinishing).isTrue()
        }
    }

    @Test
    fun supervisionPackageIsProfileOwner_redirectIntentResolved_redirect() = runTest {
        shadowDpm.setProfileOwner(ComponentName.unflattenFromString(DEFAULT_SUPERVISION_COMPONENT))
        setUpRedirectActivityComponent(DEFAULT_SUPERVISION_PACKAGE, INTERSTITIAL_REDIRECT_ACTION)

        val activityScenario = ActivityScenario.launch(SupervisionDashboardActivity::class.java)
        activityScenario.onActivity { activity ->
            assertThat(shadowOf(activity).nextStartedActivity.action)
                .isEqualTo(INTERSTITIAL_REDIRECT_ACTION)
            assertThat(activity.isFinishing).isTrue()
        }
    }

    @Test
    fun supervisionRoleHolderExists_redirectIntentNotResolved_doNotRedirect() = runTest {
        mockRoleManager.stub {
            on { getRoleHolders(any()) } doReturn listOf(TEST_SUPERVISION_PACKAGE)
        }
        setUpMessengerServiceComponent(disabled = false)

        val activityScenario = ActivityScenario.launch(SupervisionDashboardActivity::class.java)
        val nextActivityIntent = shadowOf(context as Application).nextStartedActivity

        // Check that the loading activity is not started
        assertThat(nextActivityIntent).isNull()

        // Check that the dashboard activity is not finished
        assertThat(activityScenario.state).isEqualTo(Lifecycle.State.RESUMED)
    }

    @Test
    fun noProfileOwnerOrSupervisionRoleHolder_doNotRedirect() = runTest {
        setUpMessengerServiceComponent(disabled = false)

        val activityScenario = ActivityScenario.launch(SupervisionDashboardActivity::class.java)
        val nextActivityIntent = shadowOf(context as Application).nextStartedActivity

        // Check that the loading activity is not started
        assertThat(nextActivityIntent).isNull()

        // Check that the dashboard activity is not finished
        assertThat(activityScenario.state).isEqualTo(Lifecycle.State.RESUMED)
    }

    @Test
    fun noNecessaryComponent_appInstallIntentNotResolved_doNotRedirect() = runTest {
        mockRoleManager.stub { on { getRoleHolders(any()) } doReturn emptyList() }
        val activityScenario = ActivityScenario.launch(SupervisionDashboardActivity::class.java)
        activityScenario.onActivity { activity ->
            assertThat(shadowOf(activity).nextStartedActivity).isNull()
            assertThat(activity.isFinishing).isTrue()
        }
    }

    @Test
    fun noNecessaryComponent_appInstallIntentResolved_redirectAppInstall() = runTest {
        setUpRedirectActivityComponent(TEST_SUPERVISION_PACKAGE, INSTALL_SUPERVISION_APP_ACTION)

        val activityScenario = ActivityScenario.launch(SupervisionDashboardActivity::class.java)
        activityScenario.onActivity { activity ->
            assertThat(shadowOf(activity).nextStartedActivity.action)
                .isEqualTo(INSTALL_SUPERVISION_APP_ACTION)
            assertThat(activity.isFinishing).isTrue()
        }
    }

    @Test
    fun noNecessaryComponent_appInstallIntentResolvedButDisabled_startLoadingActivity() = runTest {
        setUpRedirectActivityComponent(
            TEST_SUPERVISION_PACKAGE,
            INSTALL_SUPERVISION_APP_ACTION,
            disabled = true,
        )

        val activityScenario = ActivityScenario.launch(SupervisionDashboardActivity::class.java)
        activityScenario.onActivity { activity ->
            assertThat(shadowOf(activity).nextStartedActivity.component?.className)
                .isEqualTo(SupervisionDashboardLoadingActivity::class.java.name)
            assertThat(activity.isFinishing).isTrue()
        }
    }

    private fun setUpMessengerServiceComponent(disabled: Boolean) {
        val serviceComponentName =
            ComponentName(TEST_SUPERVISION_PACKAGE, TEST_SUPERVISION_MESSENGER_SERVICE)
        val intentFilter =
            IntentFilter(SupervisionMessengerClient.SUPERVISION_MESSENGER_SERVICE_BIND_ACTION)

        if (disabled) {
            context.packageManager.setComponentEnabledSetting(
                serviceComponentName,
                COMPONENT_ENABLED_STATE_DISABLED,
                DONT_KILL_APP,
            )
        }

        shadowPackageManager.addServiceIfNotPresent(serviceComponentName)
        shadowPackageManager.addIntentFilterForService(serviceComponentName, intentFilter)
    }

    private fun setUpRedirectActivityComponent(
        packageName: String,
        action: String,
        disabled: Boolean = false,
    ) {
        val redirectComponentName = ComponentName(packageName, TEST_REDIRECT_ACTIVITY)
        val intentFilter = IntentFilter(action)

        shadowPackageManager.addActivityIfNotPresent(redirectComponentName)
        shadowPackageManager.addIntentFilterForActivity(redirectComponentName, intentFilter)
        if (disabled) {
            context.packageManager.setApplicationEnabledSetting(
                packageName,
                COMPONENT_ENABLED_STATE_DISABLED,
                /* flags= */ 0,
            )
        }
    }

    companion object {
        const val TEST_SUPERVISION_PACKAGE = "com.android.settings.test"
        const val TEST_REDIRECT_ACTIVITY = "com.example.FakeRedirectActivity"
        const val TEST_SUPERVISION_MESSENGER_SERVICE = "FakeSupervisionMessengerService"

        const val DEFAULT_SUPERVISION_COMPONENT =
            "com.android.supervision.default/.ProfileOwnerReceiver"
        const val DEFAULT_SUPERVISION_PACKAGE = "com.android.supervision.default"
    }
}
