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
import android.app.Application
import android.app.role.RoleManager
import android.app.role.RoleManager.ROLE_SYSTEM_SUPERVISION
import android.app.settings.SettingsEnums
import android.app.supervision.SupervisionManager
import android.app.supervision.flags.Flags
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.platform.test.annotations.DisableFlags
import android.platform.test.annotations.EnableFlags
import android.platform.test.flag.junit.SetFlagsRule
import android.provider.Settings.Global
import android.text.Spanned
import android.text.style.ClickableSpan
import android.view.View
import android.widget.TextView
import androidx.preference.Preference
import androidx.preference.PreferenceGroup
import androidx.preference.PreferenceGroupAdapter
import androidx.preference.SwitchPreferenceCompat
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.android.settings.R
import com.android.settings.testutils.SettingsStoreRule
import com.android.settingslib.ipc.MessengerServiceClient
import com.android.settingslib.ipc.MessengerServiceRule
import com.android.settingslib.preference.launchFragmentScenario
import com.android.settingslib.utils.StringUtil
import com.android.settingslib.widget.FooterPreference
import com.android.settingslib.widget.TopIntroPreference
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.LooperMode
import org.robolectric.shadow.api.Shadow
import org.robolectric.shadows.ShadowContextImpl
import org.robolectric.shadows.ShadowLooper
import org.robolectric.shadows.ShadowPackageManager

@RunWith(AndroidJUnit4::class)
@LooperMode(LooperMode.Mode.INSTRUMENTATION_TEST)
class SupervisionWebContentFiltersScreenTest {
    private lateinit var shadowPackageManager: ShadowPackageManager
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val supervisionWebContentFiltersScreen = SupervisionWebContentFiltersScreen()
    private val mockRoleManager = mock<RoleManager>()
    private val mockSupervisionManager = mock<SupervisionManager>()

    @get:Rule(order = 0) val setFlagsRule = SetFlagsRule()
    @get:Rule(order = 1) val settingsStoreRule = SettingsStoreRule()
    @get:Rule(order = 2)
    val serviceRule =
        MessengerServiceRule<MessengerServiceClient>(TestSupervisionMessengerService::class.java)

    @Before
    fun setUp() {
        val testPackageName = "com.android.supervision"
        mockRoleManager.stub {
            on { getRoleHolders(ROLE_SYSTEM_SUPERVISION) } doReturn listOf(testPackageName)
        }
        shadowPackageManager = shadowOf(context.packageManager)
        val appPackageName = "com.android.chrome"
        shadowPackageManager.installPackage(
            PackageInfo().apply {
                packageName = appPackageName
                this.applicationInfo = ApplicationInfo().apply { packageName = appPackageName }
            }
        )
        shadowPackageManager.setApplicationIcon(appPackageName, ColorDrawable(Color.RED))
        val intentFilter =
            IntentFilter("android.app.supervision.action.CONFIRM_SUPERVISION_CREDENTIALS")
        val componentName =
            ComponentName(
                "com.android.settings",
                ConfirmSupervisionCredentialsActivity::class.java.name,
            )
        shadowPackageManager.addActivityIfNotPresent(componentName)
        shadowPackageManager.addIntentFilterForActivity(componentName, intentFilter)
        (Shadow.extract((context as Application).baseContext) as ShadowContextImpl).apply {
            setSystemService(Context.ROLE_SERVICE, mockRoleManager)
            setSystemService(Context.SUPERVISION_SERVICE, mockSupervisionManager)
        }
    }

    @Test
    fun key() {
        assertThat(supervisionWebContentFiltersScreen.key)
            .isEqualTo(SupervisionWebContentFiltersScreen.KEY)
    }

    @Test
    fun getTitle() {
        assertThat(supervisionWebContentFiltersScreen.title)
            .isEqualTo(R.string.supervision_web_content_filters_title)
    }

    @Test
    fun getKeywords() {
        assertThat(supervisionWebContentFiltersScreen.keywords)
            .isEqualTo(R.string.supervision_web_content_filters_keywords)
    }

    @Test
    fun isIndexable() {
        assertThat(supervisionWebContentFiltersScreen.indexable).isTrue()
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_WEB_CONTENT_FILTERS_SCREEN)
    fun flagEnabled() {
        assertThat(supervisionWebContentFiltersScreen.isFlagEnabled(context)).isTrue()
    }

    @Test
    @DisableFlags(Flags.FLAG_ENABLE_WEB_CONTENT_FILTERS_SCREEN)
    fun flagDisabled() {
        assertThat(supervisionWebContentFiltersScreen.isFlagEnabled(context)).isFalse()
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_WEB_CONTENT_FILTERS_SCREEN)
    fun getMetricsCategory() {
        assertThat(supervisionWebContentFiltersScreen.getMetricsCategory())
            .isEqualTo(SettingsEnums.SUPERVISION_WEB_CONTENT_FILTERS)
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_WEB_CONTENT_FILTERS_SCREEN)
    fun topIntroExists() {
        supervisionWebContentFiltersScreen.launchFragmentScenario().onFragment { fragment ->
            val topIntroPreference =
                fragment.findPreference<TopIntroPreference>(
                    SupervisionWebContentFiltersTopIntroPreference.KEY
                )
            assertThat(topIntroPreference).isNotNull()
        }
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_WEB_CONTENT_FILTERS_SCREEN)
    fun switchSafeSitesPreferences_succeedWithParentPin() {
        supervisionWebContentFiltersScreen.launchFragmentScenario().onFragment { fragment ->
            val browserSwitchPreference =
                fragment.findPreference<SwitchPreferenceCompat>(
                    SupervisionSafeSitesSwitchPreference.KEY
                )!!

            assertThat(browserSwitchPreference.isChecked).isFalse()

            browserSwitchPreference.performClick()

            // Pretend the PIN verification succeeded.
            val activity = shadowOf(fragment.activity)
            activity.receiveResult(
                activity.nextStartedActivityForResult.intent,
                Activity.RESULT_OK,
                null,
            )
            ShadowLooper.runUiThreadTasksIncludingDelayedTasks()

            assertThat(browserSwitchPreference.isChecked).isTrue()
        }
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_WEB_CONTENT_FILTERS_SCREEN)
    fun switchSafeSitesPreferences_failWithoutParentPin() {
        supervisionWebContentFiltersScreen.launchFragmentScenario().onFragment { fragment ->
            val browserSwitchPreference =
                fragment.findPreference<SwitchPreferenceCompat>(
                    SupervisionSafeSitesSwitchPreference.KEY
                )!!

            assertThat(browserSwitchPreference.isChecked).isFalse()

            browserSwitchPreference.performClick()

            // Pretend the PIN verification succeeded.
            val activity = shadowOf(fragment.activity)
            activity.receiveResult(
                activity.nextStartedActivityForResult.intent,
                Activity.RESULT_CANCELED,
                null,
            )
            ShadowLooper.runUiThreadTasksIncludingDelayedTasks()

            assertThat(browserSwitchPreference.isChecked).isFalse()
        }
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_WEB_CONTENT_FILTERS_SCREEN)
    fun switchSafeSearchPreferences_succeedWithParentPin() {
        supervisionWebContentFiltersScreen.launchFragmentScenario().onFragment { fragment ->
            val searchSwitchWidget =
                fragment.findPreference<SwitchPreferenceCompat>(
                    SupervisionSafeSearchSwitchPreference.KEY
                )!!

            assertThat(searchSwitchWidget.isChecked).isFalse()

            searchSwitchWidget.performClick()

            // Pretend the PIN verification succeeded.
            val activity = shadowOf(fragment.activity)
            activity.receiveResult(
                activity.nextStartedActivityForResult.intent,
                Activity.RESULT_OK,
                null,
            )
            ShadowLooper.runUiThreadTasksIncludingDelayedTasks()

            assertThat(searchSwitchWidget.isChecked).isTrue()
        }
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_WEB_CONTENT_FILTERS_SCREEN)
    fun switchSafeSearchPreferences_failedWithParentPin() {
        supervisionWebContentFiltersScreen.launchFragmentScenario().onFragment { fragment ->
            val searchSwitchWidget =
                fragment.findPreference<SwitchPreferenceCompat>(
                    SupervisionSafeSearchSwitchPreference.KEY
                )!!

            assertThat(searchSwitchWidget.isChecked).isFalse()

            searchSwitchWidget.performClick()

            // Pretend the PIN verification failed.
            val activity = shadowOf(fragment.activity)
            activity.receiveResult(
                activity.nextStartedActivityForResult.intent,
                Activity.RESULT_CANCELED,
                null,
            )
            ShadowLooper.runUiThreadTasksIncludingDelayedTasks()

            assertThat(searchSwitchWidget.isChecked).isFalse()
        }
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_WEB_CONTENT_FILTERS_SCREEN)
    fun supportedApps() {
        supervisionWebContentFiltersScreen.launchFragmentScenario().onFragment { fragment ->
            val summaryString =
                StringUtil.getIcuPluralsString(
                    context,
                    1,
                    R.string.supervision_web_content_filters_switch_summary,
                )
            InstrumentationRegistry.getInstrumentation().waitForIdleSync()
            val safeSitesSwitch: SwitchPreferenceCompat =
                fragment.findPreference(SupervisionSafeSitesSwitchPreference.KEY)!!

            assertThat(safeSitesSwitch.summaryOn).isEqualTo(summaryString)
            assertThat(safeSitesSwitch.summaryOff).isEqualTo(summaryString)

            val broswerFilterGroup: PreferenceGroup =
                fragment.findPreference("browser_filters_group")!!
            assertThat(broswerFilterGroup.preferenceCount).isEqualTo(2)

            val supportedApp: Preference = broswerFilterGroup.getPreference(1)

            assertThat(supportedApp.title).isEqualTo("Supported app")
            assertThat(supportedApp.summary).isEqualTo("App summary")
        }
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_WEB_CONTENT_FILTERS_SCREEN)
    fun footerPreference() {
        supervisionWebContentFiltersScreen.launchFragmentScenario().onFragment { fragment ->
            val footerPreference: FooterPreference =
                fragment.findPreference(SupervisionWebContentFiltersFooterPreference.KEY)!!
            val context = footerPreference.context
            val learnMoreLink =
                context.getString(R.string.supervision_web_content_filters_learn_more_link)

            // setup for HelpUtils.getHelpIntent
            Global.putInt(context.contentResolver, Global.DEVICE_PROVISIONED, 1)
            shadowOf(context.packageManager).apply {
                val componentName = ComponentName(context, "browser")
                val intentFilter =
                    IntentFilter(Intent.ACTION_VIEW).apply {
                        addCategory(Intent.CATEGORY_DEFAULT)
                        addDataScheme(Uri.parse(learnMoreLink).scheme)
                    }
                addActivityIfNotPresent(componentName)
                addIntentFilterForActivity(componentName, intentFilter)
            }

            // ensure the footer preference is visible
            val recyclerView = fragment.listView
            val adapter = recyclerView.adapter as PreferenceGroupAdapter
            val position = adapter.getPreferenceAdapterPosition(footerPreference)
            recyclerView.scrollToPosition(position)
            InstrumentationRegistry.getInstrumentation().waitForIdleSync()
            val viewHolder = recyclerView.findViewHolderForAdapterPosition(position)!!
            val learnMoreView =
                viewHolder.itemView.findViewById<TextView>(
                    com.android.settingslib.widget.preference.footer.R.id.settingslib_learn_more
                )
            assertThat(learnMoreView.visibility).isEqualTo(View.VISIBLE)

            val text = learnMoreView.text
            (text as Spanned).getSpans(0, text.length, ClickableSpan::class.java).apply {
                assertThat(this).hasLength(1)
                get(0).onClick(learnMoreView)
            }

            val intent = shadowOf(fragment.activity).nextStartedActivity
            assertThat(intent.dataString).isEqualTo(learnMoreLink)
            assertThat(intent.action).isEqualTo(Intent.ACTION_VIEW)
        }
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_WEB_CONTENT_FILTERS_SCREEN_SEARCH_REDIRECTION)
    fun redirectToSupervisionDashboard_whenSupervisionIsNotEnabled() {
        mockSupervisionManager.stub { on { isSupervisionEnabled } doReturn false }
        ActivityScenario.launch(SupervisionWebContentFiltersActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val nextActivity = shadowOf(activity).nextStartedActivity
                assertThat(nextActivity.component?.className)
                    .isEqualTo(SupervisionDashboardActivity::class.java.name)
                assertThat(activity.isFinishing).isTrue()
            }
        }
    }
}
