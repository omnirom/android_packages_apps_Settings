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
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import android.view.View
import android.view.accessibility.AccessibilityManager
import androidx.fragment.app.FragmentFactory
import androidx.fragment.app.testing.FragmentScenario
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ApplicationProvider
import com.android.internal.accessibility.common.ShortcutConstants.USER_SHORTCUT_TYPES
import com.android.internal.accessibility.common.ShortcutConstants.UserShortcutType.SOFTWARE
import com.android.settings.R
import com.android.settings.SettingsActivity
import com.android.settings.accessibility.ShortcutPreference
import com.android.settings.accessibility.data.AccessibilityRepositoryProvider
import com.android.settings.accessibility.detail.a11yservice.ui.FakePreferenceScreen.Companion.KEY
import com.android.settings.accessibility.shared.dialogs.AccessibilityServiceWarningDialogFragment
import com.android.settings.accessibility.shortcuts.EditShortcutsPreferenceFragment
import com.android.settings.core.PreferenceScreenMixin
import com.android.settings.testutils.AccessibilityTestUtils
import com.android.settings.testutils.SettingsStoreRule
import com.android.settings.testutils.shadow.ShadowAccessibilityManager
import com.android.settingslib.metadata.FixedArrayMap
import com.android.settingslib.metadata.PreferenceScreenRegistry
import com.android.settingslib.metadata.preferenceHierarchy
import com.android.settingslib.preference.PreferenceFragment
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CoroutineScope
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.spy
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadow.api.Shadow
import org.robolectric.shadows.ShadowDialog
import org.robolectric.shadows.ShadowLooper

/** Tests for [A11yServiceShortcutPreference]. */
@RunWith(RobolectricTestRunner::class)
class A11yServiceShortcutPreferenceTest {
    @get:Rule val settingsStoreRule = SettingsStoreRule()
    private var fragScenario: FragmentScenario<TestA11yServiceShortcutFragment>? = null
    private var fragment: TestA11yServiceShortcutFragment? = null
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val a11yManager: ShadowAccessibilityManager =
        Shadow.extract(context.getSystemService(AccessibilityManager::class.java))
    private val fakeComponentName = ComponentName("FakePackage", "StandardA11yService")
    private val serviceInfo =
        spy(
            AccessibilityTestUtils.createAccessibilityServiceInfo(
                context,
                fakeComponentName,
                /* isAlwaysOnService= */ false,
            )
        )
    private val serviceWarningAllowButtonId =
        com.android.internal.R.id.accessibility_permission_enable_allow_button
    private val serviceWarningDenyButtonId =
        com.android.internal.R.id.accessibility_permission_enable_deny_button

    @After
    fun cleanUp() {
        fragment = null
        fragScenario?.close()
        AccessibilityRepositoryProvider.resetInstanceForTesting()
    }

    @Test
    fun getTitle_returnsCorrectString() {
        val preference = A11yServiceShortcutPreference(context, serviceInfo, 0)
        assertThat(preference.getTitle(context).toString())
            .isEqualTo(
                context.getString(R.string.accessibility_shortcut_title, "StandardA11yService")
            )
    }

    @Test
    fun getFeatureName_returnsCorrectString() {
        val preference = A11yServiceShortcutPreference(context, serviceInfo, 0)
        assertThat(preference.getFeatureName(context).toString()).isEqualTo("StandardA11yService")
    }

    @Test
    fun bindWidget_serviceInfoTargetSdkIsAtLeastR_widgetIsEditable() {
        launchFragment(
            serviceWarningRequired = false,
            shortcutEnabled = false,
            targetSdkIsAtLeastR = true,
        )

        assertThat(getShortcutWidget().isSettingsEditable).isTrue()
    }

    @Test
    fun bindWidget_serviceInfoTargetSdkLessThanR_widgetIsNotEditable() {
        launchFragment(
            serviceWarningRequired = false,
            shortcutEnabled = false,
            targetSdkIsAtLeastR = false,
        )

        assertThat(getShortcutWidget().isSettingsEditable).isFalse()
    }

    @Test
    fun clickSettingsWhileShortcutIsOff_serviceWarningRequired_showServiceWarningDialog() {
        launchFragment(serviceWarningRequired = true, shortcutEnabled = false)

        getShortcutWidget().performClick()
        ShadowLooper.idleMainLooper()
        assertServiceWarningDialogShown()
    }

    @Test
    fun clickSettingsWhileShortcutIsOff_serviceWarningRequired_clickAllow_showEditShortcutsScreen() {
        launchFragment(serviceWarningRequired = true, shortcutEnabled = false)

        getShortcutWidget().performClick()
        ShadowLooper.idleMainLooper()
        assertServiceWarningDialogShown()

        val dialog: ShadowDialog = shadowOf(ShadowDialog.getLatestDialog())
        dialog.clickOn(serviceWarningAllowButtonId)

        assertThat(isEditShortcutsScreenShown()).isEqualTo(true)
    }

    @Test
    fun clickSettingsWhileShortcutIsOff_serviceWarningRequired_clickDeny_shortcutKeepsOff() {
        launchFragment(serviceWarningRequired = true, shortcutEnabled = false)

        getShortcutWidget().performClick()
        ShadowLooper.idleMainLooper()
        assertServiceWarningDialogShown()

        val dialog: ShadowDialog = shadowOf(ShadowDialog.getLatestDialog())
        dialog.clickOn(serviceWarningDenyButtonId)

        assertThat(getShortcutWidget().isChecked).isFalse()
        assertThat(hasAnyShortcutTargets()).isFalse()
    }

    @Test
    fun clickSettingsWhileShortcutIsOff_serviceWarningNotRequired_showEditShortcutsScreen() {
        launchFragment(serviceWarningRequired = false, shortcutEnabled = false)

        getShortcutWidget().performClick()
        ShadowLooper.idleMainLooper()

        assertThat(isEditShortcutsScreenShown()).isEqualTo(true)
    }

    @Test
    fun clickSettingsWhileShortcutIsOn_serviceWarningNotRequired_showEditShortcutsScreen() {
        launchFragment(serviceWarningRequired = false, shortcutEnabled = true)

        getShortcutWidget().performClick()
        ShadowLooper.idleMainLooper()

        assertThat(isEditShortcutsScreenShown()).isEqualTo(true)
    }

    @Test
    fun clickToggleWhileShortcutIsOff_serviceWarningRequired_showServiceWarningDialog() {
        launchFragment(serviceWarningRequired = true, shortcutEnabled = false)

        getShortcutWidgetToggle().performClick()

        ShadowLooper.idleMainLooper()
        assertServiceWarningDialogShown()
    }

    @Test
    fun clickToggleWhileShortcutIsOff_serviceWarningRequired_clickAllow_turnsOnShortcut() {
        launchFragment(serviceWarningRequired = true, shortcutEnabled = false)

        getShortcutWidgetToggle().performClick()

        ShadowLooper.idleMainLooper()
        assertServiceWarningDialogShown()

        val dialog: ShadowDialog = shadowOf(ShadowDialog.getLatestDialog())
        dialog.clickOn(serviceWarningAllowButtonId)

        assertThat(hasAnyShortcutTargets()).isTrue()
    }

    @Test
    fun clickToggleWhileShortcutIsOff_serviceWarningRequired_clickDeny_shortcutKeepsOff() {
        launchFragment(serviceWarningRequired = true, shortcutEnabled = false)

        getShortcutWidgetToggle().performClick()
        ShadowLooper.idleMainLooper()
        assertServiceWarningDialogShown()

        val dialog: ShadowDialog = shadowOf(ShadowDialog.getLatestDialog())
        dialog.clickOn(serviceWarningDenyButtonId)

        assertThat(hasAnyShortcutTargets()).isFalse()
    }

    @Test
    fun clickToggleWhileShortcutIsOn_serviceWarningRequired_turnOffShortcut() {
        launchFragment(serviceWarningRequired = true, shortcutEnabled = true)

        getShortcutWidgetToggle().performClick()
        ShadowLooper.idleMainLooper()

        assertThat(hasAnyShortcutTargets()).isFalse()
    }

    @Test
    fun clickToggleWhileShortcutIsOff_serviceWarningNotRequired_turnsOnShortcut() {
        launchFragment(serviceWarningRequired = false, shortcutEnabled = false)

        getShortcutWidgetToggle().performClick()
        ShadowLooper.idleMainLooper()

        assertThat(hasAnyShortcutTargets()).isTrue()
    }

    @Test
    fun clickToggleWhileShortcutIsOn_serviceWarningNotRequired_turnsOffShortcut() {
        launchFragment(serviceWarningRequired = false, shortcutEnabled = true)

        getShortcutWidgetToggle().performClick()
        ShadowLooper.idleMainLooper()

        assertThat(hasAnyShortcutTargets()).isFalse()
    }

    private fun launchFragment(
        serviceWarningRequired: Boolean,
        shortcutEnabled: Boolean,
        targetSdkIsAtLeastR: Boolean = true,
    ) {
        if (!serviceWarningRequired) {
            a11yManager.setAccessibilityServiceWarningExempted(serviceInfo.componentName)
        }
        if (shortcutEnabled) {
            a11yManager.setAccessibilityShortcutTargets(
                SOFTWARE,
                listOf(serviceInfo.componentName.flattenToString()),
            )
        }

        if (!targetSdkIsAtLeastR) {
            serviceInfo.resolveInfo.serviceInfo.applicationInfo.targetSdkVersion =
                Build.VERSION_CODES.Q
        }

        a11yManager.setInstalledAccessibilityServiceList(listOf(serviceInfo))
        setupTestData(serviceInfo)

        fragScenario =
            FragmentScenario.launch(
                    TestA11yServiceShortcutFragment::class.java,
                    null,
                    androidx.appcompat.R.style.Theme_AppCompat,
                    null as FragmentFactory?,
                )
                .moveToState(Lifecycle.State.RESUMED)
        fragScenario!!.onFragment { fragment = it }
    }

    private fun getShortcutWidget(): ShortcutPreference {
        return fragment!!.findPreference(A11yServiceShortcutPreference.KEY)!!
    }

    private fun getShortcutWidgetToggle(): View {
        val shortcutPrefWidget = getShortcutWidget()
        val viewHolder =
            AccessibilityTestUtils.inflateShortcutPreferenceView(
                fragment!!.requireContext(),
                shortcutPrefWidget,
            )
        return requireNotNull(viewHolder.findViewById(shortcutPrefWidget.switchResId))
    }

    private fun setupTestData(serviceInfo: AccessibilityServiceInfo) {
        PreferenceScreenRegistry.preferenceScreenMetadataFactories =
            FixedArrayMap(1) { it.put(KEY) { FakePreferenceScreen(serviceInfo) } }
    }

    private fun assertServiceWarningDialogShown(): AccessibilityServiceWarningDialogFragment {
        ShadowLooper.idleMainLooper()
        val fragments = fragment!!.getChildFragmentManager().getFragments()
        assertThat(fragments).isNotEmpty()
        assertThat(fragments).hasSize(1)
        assertThat(fragments[0]).isInstanceOf(AccessibilityServiceWarningDialogFragment::class.java)
        return fragments[0] as AccessibilityServiceWarningDialogFragment
    }

    private fun isEditShortcutsScreenShown(): Boolean {
        ShadowLooper.idleMainLooper()
        val intent = shadowOf(fragment!!.context as ContextWrapper?).peekNextStartedActivity()
        return intent
            ?.getExtra(SettingsActivity.EXTRA_SHOW_FRAGMENT)
            ?.equals(EditShortcutsPreferenceFragment::class.java.getName()) == true
    }

    private fun hasAnyShortcutTargets(): Boolean {
        for (shortcutType in USER_SHORTCUT_TYPES) {
            if (a11yManager.getAccessibilityShortcutTargets(shortcutType).isNotEmpty()) {
                return true
            }
        }
        return false
    }
}

class TestA11yServiceShortcutFragment : PreferenceFragment() {
    override fun getPreferenceScreenBindingKey(context: Context): String? {
        return KEY
    }
}

private class FakePreferenceScreen(private val serviceInfo: AccessibilityServiceInfo) :
    PreferenceScreenMixin {
    override val highlightMenuKey = 0

    override fun getPreferenceHierarchy(context: Context, coroutineScope: CoroutineScope) =
        preferenceHierarchy(context) {
            +A11yServiceShortcutPreference(context, serviceInfo, metricsCategory)
        }

    override val key: String
        get() = KEY

    override fun getMetricsCategory(): Int = 0

    companion object {
        const val KEY = "fake_screen"
    }
}
