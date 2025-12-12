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

package com.android.settings.accessibility.detail.a11yservice

import android.accessibilityservice.AccessibilityServiceInfo
import android.app.settings.SettingsEnums
import android.content.ComponentName
import android.content.ContextWrapper
import android.content.DialogInterface
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageInfo
import android.os.Bundle
import android.os.UserHandle
import android.provider.Settings
import android.text.Html
import androidx.appcompat.app.AlertDialog
import androidx.core.net.toUri
import androidx.fragment.app.FragmentFactory
import androidx.fragment.app.testing.FragmentScenario
import androidx.lifecycle.Lifecycle
import androidx.preference.Preference
import androidx.preference.TwoStatePreference
import com.android.internal.accessibility.common.ShortcutConstants.UserShortcutType.DEFAULT
import com.android.internal.accessibility.common.ShortcutConstants.UserShortcutType.HARDWARE
import com.android.internal.accessibility.common.ShortcutConstants.UserShortcutType.QUICK_SETTINGS
import com.android.internal.accessibility.common.ShortcutConstants.UserShortcutType.SOFTWARE
import com.android.settings.R
import com.android.settings.SettingsActivity
import com.android.settings.accessibility.AccessibilityFooterPreference
import com.android.settings.accessibility.AccessibilitySettings
import com.android.settings.accessibility.BaseShortcutInteractionsTestCases
import com.android.settings.accessibility.PreferredShortcut
import com.android.settings.accessibility.PreferredShortcuts
import com.android.settings.accessibility.ShortcutPreference
import com.android.settings.accessibility.data.AccessibilityRepositoryProvider
import com.android.settings.accessibility.shared.dialogs.AccessibilityServiceWarningDialogFragment
import com.android.settings.accessibility.shortcuts.EditShortcutsPreferenceFragment
import com.android.settings.testutils.AccessibilityTestUtils
import com.android.settings.testutils.shadow.SettingsShadowResources
import com.android.settingslib.accessibility.AccessibilityUtils
import com.android.settingslib.widget.IllustrationPreference
import com.google.common.truth.Truth.assertThat
import com.google.testing.junit.testparameterinjector.TestParameters
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.spy
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestParameterInjector
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowDeviceConfig
import org.robolectric.shadows.ShadowDialog
import org.robolectric.shadows.ShadowLooper
import org.robolectric.shadows.ShadowPackageManager

@RunWith(RobolectricTestParameterInjector::class)
class A11yServicePreferenceFragmentTest :
    BaseShortcutInteractionsTestCases<A11yServicePreferenceFragment>() {
    private var fragScenario: FragmentScenario<A11yServicePreferenceFragment>? = null
    private var fragment: A11yServicePreferenceFragment? = null
    private val packageManager: ShadowPackageManager = spy(shadowOf(context.packageManager))

    @Before
    fun setUpTestFragment() {
        packageManager.installPackage(
            PackageInfo().apply { packageName = PLACEHOLDER_PACKAGE_NAME }
        )

        packageManager.addServiceIfNotPresent(PLACEHOLDER_A11Y_SERVICE)
        packageManager.addActivityIfNotPresent(PLACEHOLDER_A11Y_SERVICE_SETTINGS)
    }

    @After
    fun cleanUp() {
        fragScenario?.close()
        AccessibilityRepositoryProvider.resetInstanceForTesting()
        SettingsShadowResources.reset()
    }

    @Test
    fun showFragment_unknownA11yService_closesFragment() {
        launchFragment(a11yServiceInfo = createA11yServiceInfo(), isInstalledServices = false)

        assertThat(fragment?.activity?.isFinishing).isTrue()
    }

    @Test
    fun showFragment_a11yServiceBecomesUninstalled_closesFragment() {
        val a11yServiceInfo = createA11yServiceInfo()
        launchFragment(a11yServiceInfo)

        val intent =
            Intent(Intent.ACTION_PACKAGE_REMOVED).apply {
                data = ("package:${a11yServiceInfo.componentName.packageName}").toUri()
            }
        context.sendBroadcast(intent)

        fragScenario!!.onFragment { frag -> fragment = frag }

        assertThat(fragment?.activity?.isFinishing).isTrue()
    }

    @Test
    fun showFragment_emptyOnDescriptionSummaryImage_relatedPrefAreNotVisible() {
        val a11yServiceInfo = spy(createA11yServiceInfo())
        whenever(a11yServiceInfo.loadIntro(any())).thenReturn("")
        whenever(a11yServiceInfo.animatedImageRes).thenReturn(0)
        whenever(a11yServiceInfo.loadHtmlDescription(any())).thenReturn("")
        whenever(a11yServiceInfo.loadDescription(any())).thenReturn("")

        launchFragment(a11yServiceInfo)

        assertPrefExistsButInvisible(TOP_INTRO_PREF_KEY)
        assertPrefExistsButInvisible(ILLUSTRATION_PREF_KEY)
        assertPrefExistsButInvisible(A11Y_SERVICE_SETTINGS_PREF_KEY)
        assertPrefExistsButInvisible(HTML_FOOTER_PERF_KEY)
        assertPrefExistsButInvisible(PLAIN_TEXT_FOOTER_PREF_KEY)
    }

    @Test
    fun showFragment_serviceRequestsA11yButton_useServicePreferenceIsNotVisible() {
        val a11yServiceInfo = createA11yServiceInfo(isAlwaysOnService = true)
        launchFragment(a11yServiceInfo)

        assertPrefExistsButInvisible(USE_SERVICE_PREF_KEY)
    }

    @Test
    fun showFragment_verifyTopIntroText() {
        val a11yServiceInfo = spy(createA11yServiceInfo())
        whenever(a11yServiceInfo.loadIntro(any())).thenReturn(DEFAULT_INTRO)

        launchFragment(a11yServiceInfo)

        val topIntroPref: Preference? = fragment?.findPreference(TOP_INTRO_PREF_KEY)
        assertThat(topIntroPref).isNotNull()
        assertThat(topIntroPref!!.isVisible).isTrue()
        assertThat(topIntroPref.title.toString()).isEqualTo(DEFAULT_INTRO)
    }

    @Test
    fun showFragment_verifyIllustration() {
        val a11yServiceInfo = spy(createA11yServiceInfo())
        whenever(a11yServiceInfo.animatedImageRes).thenReturn(IMAGE_RES)

        launchFragment(a11yServiceInfo)

        val illustrationPref: IllustrationPreference? =
            fragment?.findPreference(ILLUSTRATION_PREF_KEY)
        assertThat(illustrationPref).isNotNull()
        assertThat(illustrationPref!!.isVisible).isTrue()
        assertThat(illustrationPref.imageUri).isEqualTo(IMAGE_URI)
    }

    @Test
    @TestParameters(
        value =
            [
                "{serviceOn: true, useServiceToggleChecked: true}",
                "{serviceOn: false, useServiceToggleChecked: false}",
            ]
    )
    fun showFragment(serviceOn: Boolean, useServiceToggleChecked: Boolean) {
        val a11yServiceInfo = createA11yServiceInfo()
        AccessibilityUtils.setAccessibilityServiceState(
            context,
            a11yServiceInfo.componentName,
            serviceOn,
        )

        launchFragment(a11yServiceInfo)

        val useServiceToggle = getUseServiceToggle()
        assertThat(useServiceToggle).isNotNull()
        assertThat(useServiceToggle!!.isVisible).isTrue()
        assertThat(useServiceToggle.isChecked).isEqualTo(useServiceToggleChecked)
    }

    @Test
    @TestParameters(
        value = ["{allow: true, expectServiceOn: true}", "{allow: false, expectServiceOn: false}"]
    )
    fun turnOnService_serviceWarningRequired_click(allow: Boolean, expectServiceOn: Boolean) {
        val a11yServiceInfo = createA11yServiceInfo()
        launchFragment(a11yServiceInfo = a11yServiceInfo, serviceWarningRequired = true)

        val useServiceToggle = getUseServiceToggle()!!
        useServiceToggle.performClick()

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

        assertThat(useServiceToggle.isChecked).isEqualTo(expectServiceOn)
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

        val useServiceToggle = getUseServiceToggle()!!
        useServiceToggle.performClick()
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()

        assertThat(ShadowDialog.getLatestDialog()).isNull()
        assertThat(useServiceToggle.isChecked).isTrue()
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

        val useServiceToggle = getUseServiceToggle()!!
        useServiceToggle.performClick()
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

    @Test
    @TestParameters(
        value = ["{allow: true, expectShortcutOn: true}", "{allow: false, expectShortcutOn: false}"]
    )
    fun turnOnShortcut_serviceWarningRequired_click(allow: Boolean, expectShortcutOn: Boolean) {
        val a11yServiceInfo = createA11yServiceInfo()
        launchFragment(a11yServiceInfo = a11yServiceInfo, serviceWarningRequired = true)

        val shortcutToggle = getShortcutToggle()
        assertThat(shortcutToggle).isNotNull()
        assertThat(shortcutToggle!!.isVisible).isTrue()
        val viewHolder =
            AccessibilityTestUtils.inflateShortcutPreferenceView(
                fragment!!.requireContext(),
                shortcutToggle,
            )
        val widget = viewHolder.findViewById(shortcutToggle.switchResId)
        assertThat(widget).isNotNull()
        widget!!.performClick()
        ShadowLooper.idleMainLooper()

        val buttonId =
            if (allow) {
                com.android.internal.R.id.accessibility_permission_enable_allow_button
            } else {
                com.android.internal.R.id.accessibility_permission_enable_deny_button
            }

        assertServiceWarningDialogShown()
        val dialog: ShadowDialog = shadowOf(ShadowDialog.getLatestDialog())
        dialog.clickOn(buttonId)

        assertThat(shortcutToggle.isChecked).isEqualTo(expectShortcutOn)
    }

    @Test
    @TestParameters(
        customName = "userPreferVolumeKeysShortcut_noQsTile_enableVolumeKeysShortcut",
        value = ["{preferredShortcut: $HARDWARE, hasQsTile: false, expectedShortcut: $HARDWARE}"],
    )
    @TestParameters(
        customName = "userPreferVolumeKeysShortcut_hasQsTile_enableVolumeKeysShortcut",
        value = ["{preferredShortcut: $HARDWARE, hasQsTile: true, expectedShortcut: $HARDWARE}"],
    )
    @TestParameters(
        customName = "noUserPreferredShortcut_hasQsTile_enableQsShortcut",
        value =
            ["{preferredShortcut: $DEFAULT, hasQsTile: true, expectedShortcut: $QUICK_SETTINGS}"],
    )
    @TestParameters(
        customName = "noUserPreferredShortcut_noQsTile_enableSoftwareShortcut",
        value = ["{preferredShortcut: $DEFAULT, hasQsTile: false, expectedShortcut: $SOFTWARE}"],
    )
    fun turnOnShortcut(preferredShortcut: Int, hasQsTile: Boolean, expectedShortcut: Int) {
        val a11yServiceInfo = spy(createA11yServiceInfo())
        if (preferredShortcut != DEFAULT) {
            PreferredShortcuts.saveUserShortcutType(
                context,
                PreferredShortcut(
                    a11yServiceInfo.componentName.flattenToString(),
                    preferredShortcut,
                ),
            )
        }
        if (hasQsTile) {
            whenever(a11yServiceInfo.tileServiceName)
                .thenReturn(PLACEHOLDER_A11Y_SERVICE_TILE_CLASS_NAME)
        }

        launchFragment(a11yServiceInfo)
        val shortcutToggle = getShortcutToggle()
        assertThat(shortcutToggle).isNotNull()
        assertThat(shortcutToggle!!.isVisible).isTrue()
        val viewHolder =
            AccessibilityTestUtils.inflateShortcutPreferenceView(
                fragment!!.requireContext(),
                shortcutToggle,
            )
        val widget = viewHolder.findViewById(shortcutToggle.switchResId)
        assertThat(widget).isNotNull()
        widget!!.performClick()
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()

        assertThat(shortcutToggle.isChecked).isTrue()
        assertThat(a11yManager.getAccessibilityShortcutTargets(expectedShortcut))
            .containsExactlyElementsIn(listOf(a11yServiceInfo.componentName.flattenToString()))
    }

    @Test
    @TestParameters(
        value =
            [
                "{allow: true, expectEditShortcutsScreenShown: true}",
                "{allow: false, expectEditShortcutsScreenShown: false}",
            ]
    )
    fun clickShortcutSetting_serviceWarningRequired_dialogShown_click(
        allow: Boolean,
        expectEditShortcutsScreenShown: Boolean,
    ) {
        val a11yServiceInfo = createA11yServiceInfo()
        launchFragment(a11yServiceInfo = a11yServiceInfo, serviceWarningRequired = true)

        val pref: ShortcutPreference? = getShortcutToggle()
        assertThat(pref).isNotNull()
        pref!!.performClick()
        ShadowLooper.idleMainLooper()
        val buttonId =
            if (allow) {
                com.android.internal.R.id.accessibility_permission_enable_allow_button
            } else {
                com.android.internal.R.id.accessibility_permission_enable_deny_button
            }

        assertServiceWarningDialogShown()
        val dialog: ShadowDialog = shadowOf(ShadowDialog.getLatestDialog())
        dialog.clickOn(buttonId)

        assertThat(isEditShortcutsScreenShown()).isEqualTo(expectEditShortcutsScreenShown)
    }

    @Test
    fun clickSettings_launchA11yServiceSettings() {
        val a11yServiceInfo = spy(createA11yServiceInfo())
        whenever(a11yServiceInfo.settingsActivityName)
            .thenReturn(FAKE_A11Y_SERVICE_SETTINGS_CLASS_NAME)
        val settingsIntent = Intent(Intent.ACTION_MAIN, null)
        settingsIntent.setComponent(PLACEHOLDER_A11Y_SERVICE_SETTINGS)
        packageManager.addIntentFilterForActivity(
            settingsIntent.component,
            IntentFilter(settingsIntent.action),
        )

        launchFragment(a11yServiceInfo)
        val preference: Preference? = fragment!!.findPreference(A11Y_SERVICE_SETTINGS_PREF_KEY)
        assertThat(preference).isNotNull()

        preference!!.performClick()
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()

        val intent = shadowOf(fragment!!.context as ContextWrapper?).peekNextStartedActivity()
        assertThat(intent).isNotNull()
        assertThat(intent.component).isEqualTo(PLACEHOLDER_A11Y_SERVICE_SETTINGS)
    }

    @Test
    fun showFragment_verifyHtmlFooterText() {
        val a11yServiceInfo = spy(createA11yServiceInfo())
        whenever(a11yServiceInfo.loadHtmlDescription(any())).thenReturn(DEFAULT_HTML_DESCRIPTION)

        launchFragment(a11yServiceInfo)
        val preference: AccessibilityFooterPreference? =
            fragment!!.findPreference(HTML_FOOTER_PERF_KEY)

        assertThat(preference).isNotNull()
        assertThat(preference!!.isVisible).isTrue()
        assertThat(preference.summary.toString())
            .isEqualTo(
                Html.fromHtml(
                        DEFAULT_HTML_DESCRIPTION,
                        Html.FROM_HTML_MODE_COMPACT,
                        /* imageGetter= */ null,
                        /* tagHandler= */ null,
                    )
                    .toString()
            )
        assertThat(preference.contentDescription.toString())
            .isEqualTo("About ${DEFAULT_LABEL}\n\n${preference.summary}")
    }

    @Test
    fun showFragment_verifyPlainTextFooter() {
        val a11yServiceInfo = spy(createA11yServiceInfo())
        whenever(a11yServiceInfo.loadDescription(any())).thenReturn(DEFAULT_DESCRIPTION)

        launchFragment(a11yServiceInfo)
        val preference: AccessibilityFooterPreference? =
            fragment!!.findPreference(PLAIN_TEXT_FOOTER_PREF_KEY)

        assertThat(preference).isNotNull()
        assertThat(preference!!.isVisible).isTrue()
        assertThat(preference.summary.toString()).isEqualTo(DEFAULT_DESCRIPTION)
        assertThat(preference.contentDescription.toString())
            .isEqualTo("About ${DEFAULT_LABEL}\n\n${preference.summary}")
    }

    @Test
    @Config(shadows = [SettingsShadowResources::class])
    @TestParameters(
        value =
            [
                "{isDefaultA11yService: true, volumeShortcutTargets: null, expectedVolumeShortcutTargets: $PLACEHOLDER_A11Y_SERVICE_COMPONENT_STRING}",
                "{isDefaultA11yService: true, volumeShortcutTargets: '', expectedVolumeShortcutTargets: ''}",
                "{isDefaultA11yService: false, volumeShortcutTargets: null, expectedVolumeShortcutTargets: null}",
            ]
    )
    fun showFragment_writeDefaultServiceIfNeeded(
        isDefaultA11yService: Boolean,
        volumeShortcutTargets: String?,
        expectedVolumeShortcutTargets: String?,
    ) {
        val a11yServiceInfo = createA11yServiceInfo()
        if (isDefaultA11yService) {
            SettingsShadowResources.overrideResource(
                com.android.internal.R.string.config_defaultAccessibilityService,
                a11yServiceInfo.componentName.flattenToString(),
            )
        }
        Settings.Secure.putString(
            context.contentResolver,
            Settings.Secure.ACCESSIBILITY_SHORTCUT_TARGET_SERVICE,
            volumeShortcutTargets,
        )

        launchFragment(a11yServiceInfo)

        assertThat(
                Settings.Secure.getString(
                    context.contentResolver,
                    Settings.Secure.ACCESSIBILITY_SHORTCUT_TARGET_SERVICE,
                )
            )
            .isEqualTo(expectedVolumeShortcutTargets)
    }

    @Test
    @Config(shadows = [SettingsShadowResources::class, ShadowDeviceConfig::class])
    fun onAttach_writesDefaultService() {
        val a11yServiceInfo = createA11yServiceInfo()
        // Arrange: Set this service as the default accessibility service in resources.
        SettingsShadowResources.overrideResource(
            com.android.internal.R.string.config_defaultAccessibilityService,
            a11yServiceInfo.componentName.flattenToString(),
        )
        // Arrange: Ensure the shortcut target service setting is initially null, so we can
        // verify it gets written.
        Settings.Secure.putString(
            context.contentResolver,
            Settings.Secure.ACCESSIBILITY_SHORTCUT_TARGET_SERVICE,
            null,
        )

        // Act: Launch the fragment.
        launchFragment(a11yServiceInfo)

        // Assert: The default service was written to settings, proving
        // writeConfigDefaultAccessibilityServiceShortcutTargetIfNeeded() was called.
        assertThat(
                Settings.Secure.getString(
                    context.contentResolver,
                    Settings.Secure.ACCESSIBILITY_SHORTCUT_TARGET_SERVICE,
                )
            )
            .isEqualTo(a11yServiceInfo.componentName.flattenToString())
    }

    @Test
    fun getMetricsCategory() {
        launchFragment()

        assertThat(fragment!!.metricsCategory).isEqualTo(SettingsEnums.ACCESSIBILITY_SERVICE)
    }

    private fun assertPrefExistsButInvisible(prefKey: String) {
        val preference: Preference? = fragment!!.findPreference(prefKey)
        assertThat(preference).isNotNull()
        assertThat(preference!!.isVisible).isFalse()
    }

    override fun getShortcutToggle(): ShortcutPreference? {
        return fragment?.findPreference(SHORTCUT_PREF_KEY)
    }

    private fun getUseServiceToggle(): TwoStatePreference? {
        return fragment?.findPreference(USE_SERVICE_PREF_KEY)
    }

    override fun launchFragment(): A11yServicePreferenceFragment {
        launchFragment(createA11yServiceInfo())
        return fragment!!
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
                    A11yServicePreferenceFragment::class.java,
                    bundle,
                    androidx.appcompat.R.style.Theme_AppCompat,
                    null as FragmentFactory?,
                )
                .moveToState(Lifecycle.State.RESUMED)

        fragScenario!!.onFragment { frag: A11yServicePreferenceFragment? -> fragment = frag }
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

    override fun getFeatureComponent(): ComponentName = PLACEHOLDER_A11Y_SERVICE

    companion object {
        private const val TOP_INTRO_PREF_KEY = "top_intro"
        private const val ILLUSTRATION_PREF_KEY = "animated_image"
        private const val USE_SERVICE_PREF_KEY = "use_service"
        private const val SHORTCUT_PREF_KEY = "service_shortcut"
        private const val A11Y_SERVICE_SETTINGS_PREF_KEY = "accessibility_service_settings"
        private const val HTML_FOOTER_PERF_KEY = "html_footer_info"
        private const val PLAIN_TEXT_FOOTER_PREF_KEY = "footer_info"
        private const val DEFAULT_INTRO = "default intro"
        private const val DEFAULT_DESCRIPTION = "default description"
        private const val DEFAULT_HTML_DESCRIPTION = "<b>default html description</b><br/>"
        private const val PLACEHOLDER_PACKAGE_NAME = "com.placeholder.example"
        private const val A11Y_SERVICE_CLASS_NAME = "fakeA11yServiceClass"
        private const val DEFAULT_LABEL = A11Y_SERVICE_CLASS_NAME
        private const val PLACEHOLDER_A11Y_SERVICE_TILE_CLASS_NAME = "tileServiceClass"
        private const val FAKE_A11Y_SERVICE_SETTINGS_CLASS_NAME = "fakeA11ySettingsClass"
        private const val PLACEHOLDER_A11Y_SERVICE_COMPONENT_STRING =
            "com.placeholder.example/fakeA11yServiceClass"
        private val PLACEHOLDER_A11Y_SERVICE =
            ComponentName(PLACEHOLDER_PACKAGE_NAME, A11Y_SERVICE_CLASS_NAME)
        private val PLACEHOLDER_A11Y_SERVICE_SETTINGS =
            ComponentName(PLACEHOLDER_PACKAGE_NAME, FAKE_A11Y_SERVICE_SETTINGS_CLASS_NAME)
        private val IMAGE_RES = R.drawable.ic_accessibility_visibility
        private val IMAGE_URI = "android.resource://$PLACEHOLDER_PACKAGE_NAME/$IMAGE_RES".toUri()
    }
}
