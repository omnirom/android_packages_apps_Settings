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
import android.app.role.RoleManager
import android.content.ComponentName
import android.content.Context
import android.content.IntentFilter
import android.os.Process
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.overlay.FeatureFactory
import com.android.settings.supervision.ipc.PreferenceData
import com.android.settings.supervision.ipc.SupervisionMessengerClient
import com.android.settings.testutils.FakeFeatureFactory
import com.android.settings.testutils.shadow.SettingsShadowResources
import com.android.settingslib.drawer.DashboardCategory
import com.android.settingslib.ipc.MessengerService
import com.android.settingslib.ipc.MessengerServiceRule
import com.android.settingslib.ipc.PermissionChecker
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.stub
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode
import org.robolectric.shadows.ShadowPackageManager
import org.robolectric.shadows.ShadowRoleManager

private val fakePreferenceDataApi = TestPreferenceDataApiImp()

class FakeSupervisionMessengerService :
    MessengerService(listOf(fakePreferenceDataApi), PermissionChecker { _, _, _ -> true })

@Config(shadows = [SettingsShadowResources::class])
@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
@LooperMode(LooperMode.Mode.INSTRUMENTATION_TEST)
class SupervisionDashboardLoadingActivityTest {

    private lateinit var activityScenario: ActivityScenario<SupervisionDashboardLoadingActivity>
    private lateinit var featureFactory: FeatureFactory
    private lateinit var applicationContext: Context
    private lateinit var shadowPackageManager: ShadowPackageManager
    private val testSupervisionPackage = "com.google.android.test.supervision"
    private val testDispatcher = UnconfinedTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    @get:Rule
    val serviceRule =
        MessengerServiceRule<SupervisionMessengerClient>(
            FakeSupervisionMessengerService::class.java
        )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        SettingsShadowResources.overrideResource(
            com.android.internal.R.string.config_systemSupervision,
            testSupervisionPackage,
        )

        featureFactory = FakeFeatureFactory.setupForTest()
        featureFactory.stub {
            on { dashboardFeatureProvider.getTilesForCategory(any()) } doReturn
                DashboardCategory("DashboardCategoryKey")
        }

        applicationContext = ApplicationProvider.getApplicationContext<Context>()
        shadowPackageManager = shadowOf(applicationContext.packageManager)

        fakePreferenceDataApi.preferenceData =
            mapOf(
                SupervisionPromoFooterPreference.KEY to
                    PreferenceData(icon = 1, title = "Title 1", summary = "Summary 1"),
                SupervisionAocFooterPreference.KEY to
                    PreferenceData(icon = 2, title = "Title 2", summary = "Summary 2"),
            )
        ShadowRoleManager.addRoleHolder(
            RoleManager.ROLE_SYSTEM_SUPERVISION,
            testSupervisionPackage,
            Process.myUserHandle(),
        )
    }

    @After
    fun tearDown() {
        ShadowRoleManager.reset()
        activityScenario.close()
        Dispatchers.resetMain()
    }

    @Test
    fun enableSupervisionApp_success_startDashboardActivity() =
        testScope.runTest {
            setUpMessengerComponent()

            activityScenario =
                ActivityScenario.launch(SupervisionDashboardLoadingActivity::class.java)
            advanceUntilIdle()

            activityScenario.onActivity { activity ->
                runBlocking { delay(2000L) }
                advanceUntilIdle()
                val nextActivity = shadowOf(activity).nextStartedActivity
                assertThat(nextActivity.component?.className)
                    .isEqualTo(SupervisionDashboardActivity::class.java.name)
                assertThat(activity.isFinishing).isTrue()
            }
        }

    @Test
    fun enableSupervisionApp_finishBeforeResolved_resultCanceled() =
        testScope.runTest {
            activityScenario =
                ActivityScenario.launchActivityForResult(
                    SupervisionDashboardLoadingActivity::class.java
                )

            activityScenario.onActivity { activity ->
                activity.finish()
                val nextActivity = shadowOf(activity).nextStartedActivity
                assertThat(nextActivity).isNull()
            }

            assertThat(activityScenario.result.resultCode).isEqualTo(Activity.RESULT_CANCELED)
        }

    @Test
    fun enableSupervisionApp_noSupervisionComponent_startInstallActivity() =
        testScope.runTest {
            val activityComponentName = ComponentName(testSupervisionPackage, "FakeClassName")
            val intentFilter = IntentFilter(SupervisionHelper.INSTALL_SUPERVISION_APP_ACTION)

            shadowPackageManager.addActivityIfNotPresent(activityComponentName)
            shadowPackageManager.addIntentFilterForActivity(activityComponentName, intentFilter)

            activityScenario =
                ActivityScenario.launch(SupervisionDashboardLoadingActivity::class.java)
            advanceUntilIdle()

            activityScenario.onActivity { activity ->
                val nextActivity = shadowOf(activity).nextStartedActivity
                assertThat(nextActivity.action)
                    .isEqualTo(SupervisionHelper.INSTALL_SUPERVISION_APP_ACTION)
                assertThat(activity.isFinishing).isTrue()
            }
        }

    @Test
    fun enableSupervisionApp_alreadyEnabled_doesNotThrow() =
        testScope.runTest {
            ShadowRoleManager.addRoleHolder(
                RoleManager.ROLE_SUPERVISION,
                testSupervisionPackage,
                Process.myUserHandle(),
            )
            setUpMessengerComponent()

            try {
                activityScenario =
                    ActivityScenario.launch(SupervisionDashboardLoadingActivity::class.java)
                advanceUntilIdle()

                activityScenario.onActivity { activity ->
                    runBlocking { delay(2000L) }
                    advanceUntilIdle()
                    val nextActivity = shadowOf(activity).nextStartedActivity
                    assertThat(nextActivity.component?.className)
                        .isEqualTo(SupervisionDashboardActivity::class.java.name)
                    assertThat(activity.isFinishing).isTrue()
                }
            } catch (e: SecurityException) {
                fail("Should not have thrown $e")
            }
        }

    private fun setUpMessengerComponent() {
        val serviceIntentAction =
            SupervisionMessengerClient.SUPERVISION_MESSENGER_SERVICE_BIND_ACTION
        val fakeServiceClassName = "FakeSupervisionMessengerService"
        val serviceComponentName = ComponentName(testSupervisionPackage, fakeServiceClassName)
        val intentFilter = IntentFilter(serviceIntentAction)

        shadowPackageManager.addServiceIfNotPresent(serviceComponentName)
        shadowPackageManager.addIntentFilterForService(serviceComponentName, intentFilter)
    }
}
