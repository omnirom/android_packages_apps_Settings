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
import android.content.DialogInterface
import android.os.Build
import android.os.Bundle
import android.os.UserHandle
import android.view.accessibility.AccessibilityManager
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.FragmentFactory
import androidx.fragment.app.testing.FragmentScenario
import androidx.lifecycle.Lifecycle
import androidx.preference.Preference
import androidx.test.core.app.ApplicationProvider
import com.android.settings.R
import com.android.settings.accessibility.AccessibilitySettings
import com.android.settings.accessibility.data.AccessibilityRepositoryProvider
import com.android.settings.accessibility.shared.dialogs.AccessibilityServiceWarningDialogFragment
import com.android.settings.testutils.AccessibilityTestUtils
import com.android.settings.testutils.SettingsStoreRule
import com.android.settings.testutils.shadow.ShadowAccessibilityManager
import com.android.settingslib.accessibility.AccessibilityUtils
import com.android.settingslib.datastore.SettingsSecureStore
import com.android.settingslib.metadata.PreferenceLifecycleContext
import com.android.settingslib.metadata.ReadWritePermit
import com.android.settingslib.preference.PreferenceFragment
import com.android.settingslib.preference.createAndBindWidget
import com.android.settingslib.widget.MainSwitchPreference
import com.google.common.truth.Truth.assertThat
import com.google.testing.junit.testparameterinjector.TestParameters
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestParameterInjector
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadow.api.Shadow
import org.robolectric.shadows.ShadowDialog
import org.robolectric.shadows.ShadowLooper

@RunWith(RobolectricTestParameterInjector::class)
class UseServicePreferenceTest {
    @get:Rule val settingsStoreRule = SettingsStoreRule()
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val a11yManager: ShadowAccessibilityManager =
        Shadow.extract(context.getSystemService(AccessibilityManager::class.java))
    private var fragScenario: FragmentScenario<PreferenceFragment>? = null
    private var fragment: PreferenceFragment? = null
    private var prefLifecycleContext: PreferenceLifecycleContext? = null

    @After
    fun cleanUp() {
        fragScenario?.close()
        AccessibilityRepositoryProvider.resetInstanceForTesting()
    }

    @Test
    fun getKey() {
        val preference =
            UseServicePreference(
                context,
                createA11yServiceInfo(isAlwaysOnService = true),
                sourceMetricsCategory = 0,
            )
        assertThat(preference.key).isEqualTo(UseServicePreference.KEY)
    }

    @Test
    fun getReadPermissions_equalsToSettingsSecureStorePermissions() {
        val preference =
            UseServicePreference(context, createA11yServiceInfo(), sourceMetricsCategory = 0)
        assertThat(preference.getReadPermissions(context))
            .isEqualTo(SettingsSecureStore.getReadPermissions())
    }

    @Test
    fun getWritePermissions_equalsToSettingsSecureStorePermissions() {
        val preference =
            UseServicePreference(context, createA11yServiceInfo(), sourceMetricsCategory = 0)
        assertThat(preference.getWritePermissions(context))
            .isEqualTo(SettingsSecureStore.getWritePermissions())
    }

    @Test
    fun getReadPermit_alwaysAllow() {
        val preference =
            UseServicePreference(context, createA11yServiceInfo(), sourceMetricsCategory = 0)
        assertThat(preference.getReadPermit(context, 0, 0)).isEqualTo(ReadWritePermit.ALLOW)
    }

    @Test
    fun getWritePermit_turnOff_returnDisallow() {
        val preference =
            UseServicePreference(context, createA11yServiceInfo(), sourceMetricsCategory = 0)
        assertThat(preference.getWritePermit(context, false, 0, 0))
            .isEqualTo(ReadWritePermit.DISALLOW)
    }

    @Test
    fun getWritePermit_turnOn_serviceWarningRequired_returnDisallow() {
        val preference =
            UseServicePreference(context, createA11yServiceInfo(), sourceMetricsCategory = 0)

        assertThat(preference.getWritePermit(context, true, 0, 0))
            .isEqualTo(ReadWritePermit.DISALLOW)
    }

    @Test
    fun getWritePermit_turnOn_serviceWarningNotRequired_returnAllow() {
        val a11yServiceInfo = createA11yServiceInfo()
        val preference = UseServicePreference(context, a11yServiceInfo, sourceMetricsCategory = 0)
        a11yManager.setAccessibilityServiceWarningExempted(a11yServiceInfo.componentName)

        assertThat(preference.getWritePermit(context, true, 0, 0)).isEqualTo(ReadWritePermit.ALLOW)
    }

    @Test
    fun isAvailable_alwaysOnService_returnFalse() {
        val preference =
            UseServicePreference(
                context,
                createA11yServiceInfo(isAlwaysOnService = true),
                sourceMetricsCategory = 0,
            )
        assertThat(preference.isAvailable(context)).isFalse()
    }

    @Test
    fun isAvailable_alwaysOnService_targetSdkVersionLessThenR_returnTrue() {
        val a11yServiceInfo = createA11yServiceInfo(isAlwaysOnService = true)
        a11yServiceInfo.resolveInfo.serviceInfo.applicationInfo.targetSdkVersion =
            Build.VERSION_CODES.Q
        val preference = UseServicePreference(context, a11yServiceInfo, sourceMetricsCategory = 0)

        assertThat(preference.isAvailable(context)).isTrue()
    }

    @Test
    fun isAvailable_notAlwaysOnService_returnTrue() {
        val preference =
            UseServicePreference(context, createA11yServiceInfo(), sourceMetricsCategory = 0)
        assertThat(preference.isAvailable(context)).isTrue()
    }

    @Test
    fun getTitle_returnsUseFeature() {
        val preference =
            UseServicePreference(context, createA11yServiceInfo(), sourceMetricsCategory = 0)

        assertThat(preference.getTitle(context))
            .isEqualTo(
                context.getString(
                    R.string.accessibility_service_primary_switch_title,
                    DEFAULT_LABEL,
                )
            )
    }

    @Test
    fun createWidget_returnsMainSwitchPreference() {
        val preference =
            UseServicePreference(context, createA11yServiceInfo(), sourceMetricsCategory = 0)

        assertThat(preference.createWidget(context)).isInstanceOf(MainSwitchPreference::class.java)
    }

    @Test
    fun bind_serviceOff_toggleIsNotChecked() {
        val a11yServiceInfo = createA11yServiceInfo()
        AccessibilityUtils.setAccessibilityServiceState(
            context,
            a11yServiceInfo.componentName,
            false,
        )
        val preference = UseServicePreference(context, a11yServiceInfo, sourceMetricsCategory = 0)
        val widget = preference.createAndBindWidget<MainSwitchPreference>(context)

        assertThat(widget.isChecked).isFalse()
    }

    @Test
    fun bind_serviceOn_toggleIsChecked() {
        val a11yServiceInfo = createA11yServiceInfo()
        AccessibilityUtils.setAccessibilityServiceState(
            context,
            a11yServiceInfo.componentName,
            true,
        )
        val preference = UseServicePreference(context, a11yServiceInfo, sourceMetricsCategory = 0)
        val widget = preference.createAndBindWidget<MainSwitchPreference>(context)

        assertThat(widget.isChecked).isTrue()
    }

    @Test
    @TestParameters(
        value = ["{allow: true, expectServiceOn: true}", "{allow: false, expectServiceOn: false}"]
    )
    fun turnOnService_serviceWarningRequired_click(allow: Boolean, expectServiceOn: Boolean) {
        val a11yServiceInfo = createA11yServiceInfo()
        launchFragment(a11yServiceInfo = a11yServiceInfo, serviceWarningRequired = true)

        val preference = UseServicePreference(context, a11yServiceInfo, sourceMetricsCategory = 0)
        val widget =
            preference.createAndBindWidget<MainSwitchPreference>(fragment!!.requireContext())
        whenever(prefLifecycleContext!!.findPreference<Preference>(preference.key))
            .thenReturn(widget)
        preference.onCreate(prefLifecycleContext!!)

        widget.performClick()

        val buttonId =
            if (allow) {
                com.android.internal.R.id.accessibility_permission_enable_allow_button
            } else {
                com.android.internal.R.id.accessibility_permission_enable_deny_button
            }
        assertServiceWarningDialogShown()
        val dialog: ShadowDialog = shadowOf(ShadowDialog.getLatestDialog())
        dialog.clickOn(buttonId)
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()

        assertThat(
                AccessibilityUtils.getEnabledServicesFromSettings(context, UserHandle.myUserId())
                    .contains(a11yServiceInfo.componentName)
            )
            .isEqualTo(expectServiceOn)
    }

    @Test
    fun turnOnService_serviceWarningNotRequired_serviceTurnedOn() {
        val a11yServiceInfo = createA11yServiceInfo()
        launchFragment(a11yServiceInfo = a11yServiceInfo, serviceWarningRequired = false)

        val preference = UseServicePreference(context, a11yServiceInfo, sourceMetricsCategory = 0)
        val widget =
            preference.createAndBindWidget<MainSwitchPreference>(
                fragment!!.requireContext(),
                fragment!!.preferenceScreen,
            )
        whenever(prefLifecycleContext!!.findPreference<Preference>(preference.key))
            .thenReturn(widget)
        preference.onCreate(prefLifecycleContext!!)

        widget.performClick()
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()

        assertThat(ShadowDialog.getLatestDialog()).isNull()
        assertThat(
                AccessibilityUtils.getEnabledServicesFromSettings(context, UserHandle.myUserId())
            )
            .containsExactlyElementsIn(setOf(a11yServiceInfo.componentName))
    }

    @Test
    @TestParameters(
        value =
            [
                "{turnOff: true, expectedServiceOff: true}",
                "{turnOff: false, expectedServiceOff: false}",
            ]
    )
    fun turnOffService_showDisableServiceDialog_click(
        turnOff: Boolean,
        expectedServiceOff: Boolean,
    ) {
        val a11yServiceInfo = createA11yServiceInfo()
        AccessibilityUtils.setAccessibilityServiceState(
            context,
            a11yServiceInfo.componentName,
            /* enabled= */ true,
        )
        launchFragment(a11yServiceInfo = a11yServiceInfo)

        val preference = UseServicePreference(context, a11yServiceInfo, sourceMetricsCategory = 0)
        val widget =
            preference.createAndBindWidget<MainSwitchPreference>(
                fragment!!.requireContext(),
                fragment!!.preferenceScreen,
            )
        whenever(prefLifecycleContext!!.findPreference<Preference>(preference.key))
            .thenReturn(widget)
        preference.onCreate(prefLifecycleContext!!)

        widget.performClick()
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()

        val dialog = ShadowDialog.getLatestDialog() as AlertDialog
        assertThat(dialog).isNotNull()
        dialog
            .getButton(
                if (turnOff) DialogInterface.BUTTON_POSITIVE else DialogInterface.BUTTON_NEGATIVE
            )
            .performClick()
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()

        assertThat(
                !AccessibilityUtils.getEnabledServicesFromSettings(context, UserHandle.myUserId())
                    .contains(a11yServiceInfo.componentName)
            )
            .isEqualTo(expectedServiceOff)
    }

    private fun createA11yServiceInfo(
        isAlwaysOnService: Boolean = false
    ): AccessibilityServiceInfo {
        return AccessibilityTestUtils.createAccessibilityServiceInfo(
                context,
                PLACEHOLDER_A11Y_SERVICE,
                isAlwaysOnService,
            )
            .apply { isAccessibilityTool = true }
    }

    private fun launchFragment(
        a11yServiceInfo: AccessibilityServiceInfo,
        serviceWarningRequired: Boolean = false,
        isInstalledServices: Boolean = true,
    ) {
        if (isInstalledServices) {
            a11yManager.setInstalledAccessibilityServiceList(listOf(a11yServiceInfo))
        }
        if (!serviceWarningRequired) {
            a11yManager.setAccessibilityServiceWarningExempted(a11yServiceInfo.componentName)
        }
        val bundle = Bundle()
        bundle.putParcelable(
            AccessibilitySettings.EXTRA_COMPONENT_NAME,
            a11yServiceInfo.componentName,
        )
        fragScenario =
            FragmentScenario.launch(
                    PreferenceFragment::class.java,
                    bundle,
                    androidx.appcompat.R.style.Theme_AppCompat,
                    null as FragmentFactory?,
                )
                .moveToState(Lifecycle.State.RESUMED)

        fragScenario!!.onFragment { frag: PreferenceFragment? ->
            fragment = frag
            prefLifecycleContext = mock {
                on { childFragmentManager } doReturn frag!!.childFragmentManager
                on { lifecycleOwner } doReturn frag
            }
        }
    }

    private fun assertServiceWarningDialogShown(): AccessibilityServiceWarningDialogFragment {
        ShadowLooper.idleMainLooper()
        val fragments = fragment!!.getChildFragmentManager().fragments
        assertThat(fragments).isNotEmpty()
        assertThat(fragments).hasSize(1)
        assertThat(fragments[0]).isInstanceOf(AccessibilityServiceWarningDialogFragment::class.java)
        return fragments[0] as AccessibilityServiceWarningDialogFragment
    }

    companion object {
        private const val PLACEHOLDER_PACKAGE_NAME = "com.placeholder.example"
        private const val A11Y_SERVICE_CLASS_NAME = "fakeA11yServiceClass"
        private const val DEFAULT_LABEL = A11Y_SERVICE_CLASS_NAME
        private val PLACEHOLDER_A11Y_SERVICE =
            ComponentName(PLACEHOLDER_PACKAGE_NAME, A11Y_SERVICE_CLASS_NAME)
    }
}
