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

package com.android.settings.accessibility.detail.a11yactivity.ui

import android.accessibilityservice.AccessibilityShortcutInfo
import android.content.ComponentName
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.ApplicationInfo
import android.os.Bundle
import android.view.accessibility.AccessibilityManager
import androidx.fragment.app.testing.FragmentScenario
import androidx.preference.PreferenceFragmentCompat
import com.android.settings.R
import com.android.settings.accessibility.AccessibilitySettings
import com.android.settings.accessibility.Flags
import com.android.settings.accessibility.LaunchAccessibilityActivityPreferenceFragment
import com.android.settings.accessibility.data.AccessibilityRepositoryProvider
import com.android.settings.overlay.FeatureFactory.Companion.featureFactory
import com.android.settings.testutils.FakeFeatureFactory
import com.android.settings.testutils.SettingsStoreRule
import com.android.settings.testutils.shadow.ShadowAccessibilityManager
import com.android.settings.testutils2.SettingsCatalystTestCase
import com.android.settingslib.preference.createAndBindWidget
import com.android.settingslib.widget.TwoTargetPreference
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.robolectric.shadow.api.Shadow

/** Tests for [A11yActivityScreen]. */
class A11yActivityScreenTest : SettingsCatalystTestCase() {
    @get:Rule val settingStoreRule = SettingsStoreRule()

    private val arguments =
        Bundle().apply {
            putParcelable(AccessibilitySettings.EXTRA_COMPONENT_NAME, A11Y_ACTIVITY_COMPONENT)
        }

    override val preferenceScreenCreator: A11yActivityScreen by lazy {
        A11yActivityScreen(appContext, arguments)
    }
    private val a11yManager: ShadowAccessibilityManager =
        Shadow.extract(appContext.getSystemService(AccessibilityManager::class.java))

    @Before
    fun setUp() {
        FakeFeatureFactory.setupForTest()
        val mockInfo: AccessibilityShortcutInfo = createMockShortcutInfo(A11Y_ACTIVITY_COMPONENT)
        val activityInfo =
            mock<ActivityInfo>().apply {
                packageName = PACKAGE_NAME
                name = A11Y_ACTIVITY_COMPONENT.className
                applicationInfo = ApplicationInfo()
            }
        whenever(activityInfo.loadLabel(any())).thenReturn(DEFAULT_LABEL)
        whenever(mockInfo.activityInfo).thenReturn(activityInfo)
        whenever(mockInfo.loadSummary(any())).thenReturn(DEFAULT_SUMMARY)
        a11yManager.setInstalledAccessibilityShortcutListAsUser(listOf(mockInfo))
    }

    @After
    fun cleanUp() {
        AccessibilityRepositoryProvider.resetInstanceForTesting()
    }

    @Test
    fun getKey() {
        assertThat(preferenceScreenCreator.key).isEqualTo(A11yActivityScreen.KEY)
    }

    @Test
    fun getHighlightMenuKey() {
        assertThat(preferenceScreenCreator.highlightMenuKey)
            .isEqualTo(R.string.menu_key_accessibility)
    }

    @Test
    fun getMetricsCategory() {
        val expectedPageId = 123
        whenever(
                featureFactory.accessibilityPageIdFeatureProvider.getCategory(
                    A11Y_ACTIVITY_COMPONENT
                )
            )
            .thenReturn(expectedPageId)
        assertThat(preferenceScreenCreator.getMetricsCategory()).isEqualTo(expectedPageId)
    }

    @Test
    fun getSummary() {
        assertThat(preferenceScreenCreator.getSummary(appContext)).isEqualTo(DEFAULT_SUMMARY)
    }

    @Test
    fun getTitle() {
        assertThat(preferenceScreenCreator.getTitle(appContext)).isEqualTo(DEFAULT_LABEL)
    }

    @Test
    fun createWidget_verifyWidgetTypeAndIconSpaceReserved() {
        val widget = preferenceScreenCreator.createWidget(appContext)
        assertThat(widget).isInstanceOf(TwoTargetPreference::class.java)
        assertThat(widget.isIconSpaceReserved).isTrue()
    }

    @Test
    fun bind_verifyIcon() {
        val widget = preferenceScreenCreator.createAndBindWidget<TwoTargetPreference>(appContext)
        assertThat(widget.icon).isNotNull()
    }

    @Test
    fun getFragmentClass() {
        assertThat(preferenceScreenCreator.fragmentClass())
            .isEqualTo(LaunchAccessibilityActivityPreferenceFragment::class.java)
    }

    @Test
    fun getBindingKey() {
        assertThat(preferenceScreenCreator.bindingKey)
            .isEqualTo(A11Y_ACTIVITY_COMPONENT.flattenToString())
    }

    @Test
    fun getLaunchIntent_hasStringExtraForComponent() {
        val intent = preferenceScreenCreator.getLaunchIntent(appContext, null)
        assertThat(intent.getStringExtra(Intent.EXTRA_COMPONENT_NAME))
            .isEqualTo(A11Y_ACTIVITY_COMPONENT.flattenToString())
    }

    @Test
    fun parameters_hasTwoA11yActivities_returnTwoItems() = runTest {
        AccessibilityRepositoryProvider.resetInstanceForTesting()
        val shortcutInfo1 = createMockShortcutInfo(A11Y_ACTIVITY_COMPONENT)
        val shortcutInfo2 = createMockShortcutInfo(A11Y_ACTIVITY_COMPONENT2)

        a11yManager.setInstalledAccessibilityShortcutListAsUser(
            listOf(shortcutInfo1, shortcutInfo2)
        )
        val collectedItems = mutableListOf<String?>()
        A11yActivityScreen.parameters(appContext).collect {
            collectedItems.add(
                it.getParcelable(
                        AccessibilitySettings.EXTRA_COMPONENT_NAME,
                        ComponentName::class.java,
                    )
                    ?.flattenToString()
            )
        }
        assertThat(collectedItems).hasSize(2)
        assertThat(collectedItems)
            .containsExactlyElementsIn(
                listOf(
                    A11Y_ACTIVITY_COMPONENT.flattenToString(),
                    A11Y_ACTIVITY_COMPONENT2.flattenToString(),
                )
            )
    }

    override fun launchFragmentScenario(
        fragmentClass: Class<PreferenceFragmentCompat>
    ): FragmentScenario<PreferenceFragmentCompat> {
        val scenario = FragmentScenario.launch(fragmentClass, arguments)
        scenario.onFragment { fragment ->
            // Pre catalyst, we didn't set up the preference screen's title.
            // Hence, we had to add the title to preference screen directly in order to test the
            // migration test case.
            // We also have a separate test case to test the title in post-catalyst scenario
            fragment.preferenceScreen.title = DEFAULT_LABEL
        }
        return scenario
    }

    private fun createMockShortcutInfo(componentName: ComponentName): AccessibilityShortcutInfo {
        val mockInfo: AccessibilityShortcutInfo = mock()
        whenever(mockInfo.componentName).thenReturn(componentName)
        return mockInfo
    }

    override val flagName: String
        get() = Flags.FLAG_CATALYST_A11Y_ACTIVITY_DETAIL

    companion object {
        private const val PACKAGE_NAME = "com.foo.bar"
        private val A11Y_ACTIVITY_COMPONENT = ComponentName(PACKAGE_NAME, "FakeA11yActivity")
        private val A11Y_ACTIVITY_COMPONENT2 = ComponentName(PACKAGE_NAME, "FakeA11yActivity2")

        private const val DEFAULT_LABEL = "default label"
        private const val DEFAULT_SUMMARY = "default summary"
    }
}
