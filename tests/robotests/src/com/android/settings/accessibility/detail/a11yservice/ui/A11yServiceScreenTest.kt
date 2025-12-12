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

package com.android.settings.accessibility.detail.a11yservice.ui

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import androidx.fragment.app.testing.FragmentScenario
import androidx.preference.PreferenceFragmentCompat
import com.android.settings.R
import com.android.settings.accessibility.AccessibilitySettings
import com.android.settings.accessibility.Flags
import com.android.settings.accessibility.data.AccessibilityRepositoryProvider
import com.android.settings.accessibility.detail.a11yservice.A11yServicePreferenceFragment
import com.android.settings.overlay.FeatureFactory.Companion.featureFactory
import com.android.settings.testutils.AccessibilityTestUtils
import com.android.settings.testutils.FakeFeatureFactory
import com.android.settings.testutils.SettingsStoreRule
import com.android.settings.testutils.shadow.ShadowAccessibilityManager
import com.android.settings.testutils2.SettingsCatalystTestCase
import com.android.settingslib.RestrictedPreference
import com.android.settingslib.preference.createAndBindWidget
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.whenever
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadow.api.Shadow
import org.robolectric.shadows.ShadowPackageManager

/** Tests for [A11yServiceScreen]. */
class A11yServiceScreenTest : SettingsCatalystTestCase() {
    @get:Rule val settingStoreRule = SettingsStoreRule()

    private val arguments =
        Bundle().apply {
            putParcelable(AccessibilitySettings.EXTRA_COMPONENT_NAME, A11Y_SERVICE_COMPONENT)
        }

    override val preferenceScreenCreator: A11yServiceScreen by lazy {
        A11yServiceScreen(appContext, arguments)
    }
    private val a11yManager: ShadowAccessibilityManager =
        Shadow.extract(appContext.getSystemService(AccessibilityManager::class.java))
    private val packageManager: ShadowPackageManager = shadowOf(appContext.packageManager)

    @Before
    fun setUp() {
        FakeFeatureFactory.setupForTest()
        a11yManager.setInstalledAccessibilityServiceList(listOf(createA11yServiceInfo()))
    }

    @After
    fun cleanUp() {
        AccessibilityRepositoryProvider.resetInstanceForTesting()
    }

    @Test
    fun getKey() {
        assertThat(preferenceScreenCreator.key).isEqualTo(A11yServiceScreen.KEY)
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
                    A11Y_SERVICE_COMPONENT
                )
            )
            .thenReturn(expectedPageId)
        assertThat(preferenceScreenCreator.metricsCategory).isEqualTo(expectedPageId)
    }

    @Test
    fun getTitle() {
        assertThat(preferenceScreenCreator.getTitle(appContext).toString())
            .isEqualTo(A11Y_SERVICE_COMPONENT.className)
    }

    @Test
    fun createWidget_verifyWidgetTypeAndIconSpaceReserved() {
        val widget = preferenceScreenCreator.createWidget(appContext)
        assertThat(widget).isInstanceOf(RestrictedPreference::class.java)
        assertThat(widget.isIconSpaceReserved).isTrue()
    }

    @Test
    fun bind_verifyIconNotNull() {
        val widget = preferenceScreenCreator.createAndBindWidget<RestrictedPreference>(appContext)
        assertThat(widget.icon).isNotNull()
    }

    @Test
    fun getFragmentClass_equalsA11yServicePreferenceFragment() {
        assertThat(preferenceScreenCreator.fragmentClass())
            .isEqualTo(A11yServicePreferenceFragment::class.java)
    }

    @Test
    fun getBindingKey() {
        assertThat(preferenceScreenCreator.bindingKey)
            .isEqualTo(A11Y_SERVICE_COMPONENT.flattenToString())
    }

    @Test
    fun getLaunchIntent_hasStringExtraForComponent() {
        val intent = preferenceScreenCreator.getLaunchIntent(appContext, null)
        assertThat(intent.action).isEqualTo(Settings.ACTION_ACCESSIBILITY_DETAILS_SETTINGS)
        assertThat(intent.getStringExtra(Intent.EXTRA_COMPONENT_NAME))
            .isEqualTo(A11Y_SERVICE_COMPONENT.flattenToString())
    }

    @Test
    fun parameters_hasTwoA11yServices_returnTwoItems() {
        AccessibilityRepositoryProvider.resetInstanceForTesting()
        runTest {
            val serviceInfo1 = createA11yServiceInfo(serviceComponent = A11Y_SERVICE_COMPONENT)
            val serviceInfo2 = createA11yServiceInfo(serviceComponent = A11Y_SERVICE_COMPONENT2)

            a11yManager.setInstalledAccessibilityServiceList(listOf(serviceInfo1, serviceInfo2))
            val collectedItems = mutableListOf<String?>()
            A11yServiceScreen.parameters(appContext).collect {
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
                        A11Y_SERVICE_COMPONENT.flattenToString(),
                        A11Y_SERVICE_COMPONENT2.flattenToString(),
                    )
                )
        }
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
            fragment.preferenceScreen.title = "FakeA11yService"
        }

        return scenario
    }

    private fun createA11yServiceInfo(
        isAlwaysOnService: Boolean = false,
        serviceComponent: ComponentName = A11Y_SERVICE_COMPONENT,
    ): AccessibilityServiceInfo {
        return AccessibilityTestUtils.createAccessibilityServiceInfo(
                appContext,
                serviceComponent,
                isAlwaysOnService,
            )
            .apply { isAccessibilityTool = true }
    }

    override val flagName: String
        get() = Flags.FLAG_CATALYST_A11Y_SERVICE_DETAIL

    companion object {
        private const val PACKAGE_NAME = "com.foo.bar"
        private val A11Y_SERVICE_COMPONENT = ComponentName(PACKAGE_NAME, "FakeA11yService")
        private val A11Y_SERVICE_COMPONENT2 = ComponentName(PACKAGE_NAME, "FakeA11yService2")
    }
}
