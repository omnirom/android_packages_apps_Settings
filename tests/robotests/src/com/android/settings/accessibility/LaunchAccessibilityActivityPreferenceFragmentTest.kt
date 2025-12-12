/*
 * Copyright (C) 2022 The Android Open Source Project
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
package com.android.settings.accessibility

import android.accessibilityservice.AccessibilityShortcutInfo
import android.app.settings.SettingsEnums
import android.content.ComponentName
import android.content.ContextWrapper
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ActivityInfo
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.os.Bundle
import android.text.Html
import android.view.LayoutInflater
import android.view.View
import androidx.core.net.toUri
import androidx.fragment.app.FragmentFactory
import androidx.fragment.app.testing.FragmentScenario
import androidx.fragment.app.testing.FragmentScenario.FragmentAction
import androidx.lifecycle.Lifecycle
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder
import com.android.settings.R
import com.android.settings.accessibility.data.AccessibilityRepositoryProvider
import com.android.settingslib.widget.ButtonPreference
import com.android.settingslib.widget.IllustrationPreference
import com.android.settingslib.widget.TopIntroPreference
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.spy
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.shadows.ShadowLooper
import org.robolectric.shadows.ShadowPackageManager

/** Tests for [LaunchAccessibilityActivityPreferenceFragment] */
@RunWith(RobolectricTestRunner::class)
class LaunchAccessibilityActivityPreferenceFragmentTest :
    BaseShortcutInteractionsTestCases<LaunchAccessibilityActivityPreferenceFragment>() {
    private var fragScenario: FragmentScenario<LaunchAccessibilityActivityPreferenceFragment>? =
        null
    private var fragment: LaunchAccessibilityActivityPreferenceFragment? = null
    private val packageManager: ShadowPackageManager = spy(Shadows.shadowOf(context.packageManager))

    @Before
    fun setUpTestFragment() {
        packageManager.installPackage(
            PackageInfo().apply { packageName = PLACEHOLDER_PACKAGE_NAME }
        )

        packageManager.addActivityIfNotPresent(PLACEHOLDER_A11Y_ACTIVITY)
        packageManager.addActivityIfNotPresent(PLACEHOLDER_A11Y_ACTIVITY_SETTINGS)
    }

    @After
    fun cleanUp() {
        AccessibilityRepositoryProvider.resetInstanceForTesting()
        fragScenario?.close()
    }

    @Test
    fun onResume_emptyOnDescriptionSummaryImage_relatedPrefAreNotVisible() {
        val mockInfo: AccessibilityShortcutInfo = mock<AccessibilityShortcutInfo>()
        whenever(mockInfo.componentName).thenReturn(PLACEHOLDER_A11Y_ACTIVITY)
        val activityInfo =
            mock<ActivityInfo>().apply {
                packageName = PLACEHOLDER_PACKAGE_NAME
                name = A11Y_ACTIVITY_CLASS_NAME
                applicationInfo = ApplicationInfo()
            }
        whenever(activityInfo.loadLabel(any())).thenReturn(DEFAULT_LABEL)

        whenever(mockInfo.activityInfo).thenReturn(activityInfo)

        launchFragment(mockInfo)

        assertPrefExistsButInvisible(TOP_INTRO_PREF_KEY)
        assertPrefExistsButInvisible(ILLUSTRATION_PREF_KEY)
        assertPrefExistsButInvisible(A11Y_ACTIVITY_SETTINGS_PREF_KEY)
        assertPrefExistsButInvisible(HTML_FOOTER_PERF_KEY)
        assertPrefExistsButInvisible(PLAIN_TEXT_FOOTER_PREF_KEY)
    }

    @Test
    fun onResume_verifyTopIntroText() {
        launchFragment()

        val topIntroPref: TopIntroPreference? = fragment!!.findPreference(TOP_INTRO_PREF_KEY)
        assertThat(topIntroPref).isNotNull()
        assertThat(topIntroPref!!.isVisible).isTrue()
        assertThat(topIntroPref.title.toString()).isEqualTo(DEFAULT_INTRO)
    }

    @Test
    fun onResume_verifyIllustration() {
        launchFragment()

        val illustrationPref: IllustrationPreference? =
            fragment!!.findPreference(ILLUSTRATION_PREF_KEY)
        assertThat(illustrationPref).isNotNull()
        assertThat(illustrationPref!!.isVisible).isTrue()
        assertThat(illustrationPref.imageUri).isEqualTo(IMAGE_URI)
    }

    @Test
    fun clickOpenFeatureButton_launchFeatureActivity() {
        launchFragment()
        val buttonPref: ButtonPreference? = fragment!!.findPreference(MAIN_BUTTON_PREF_KEY)
        assertThat(buttonPref).isNotNull()
        assertThat(buttonPref!!.isVisible).isTrue()
        assertThat(buttonPref.title.toString()).isEqualTo("Open $DEFAULT_LABEL")

        val inflater = LayoutInflater.from(fragment!!.requireContext())
        val view: View = inflater.inflate(buttonPref.layoutResource, null)
        val viewHolder = PreferenceViewHolder.createInstanceForTests(view)
        buttonPref.onBindViewHolder(viewHolder)
        buttonPref.button.performClick()
        ShadowLooper.idleMainLooper()

        val intent =
            Shadows.shadowOf(fragment!!.context as ContextWrapper?).peekNextStartedActivity()
        assertThat(intent).isNotNull()
        assertThat(intent.component).isEqualTo(PLACEHOLDER_A11Y_ACTIVITY)
    }

    @Test
    fun clickSettings_launchA11yActivitySettings() {
        launchFragment()
        val preference: Preference? = fragment!!.findPreference(A11Y_ACTIVITY_SETTINGS_PREF_KEY)
        assertThat(preference).isNotNull()

        preference!!.performClick()
        ShadowLooper.idleMainLooper()

        val intent =
            Shadows.shadowOf(fragment!!.context as ContextWrapper?).peekNextStartedActivity()
        assertThat(intent).isNotNull()
        assertThat(intent.component).isEqualTo(PLACEHOLDER_A11Y_ACTIVITY_SETTINGS)
    }

    @Test
    fun onResume_verifyHtmlFooterText() {
        launchFragment()
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
            .isEqualTo("About $DEFAULT_LABEL\n\n${preference.summary}")
    }

    @Test
    fun onResume_verifyPlainTextFooter() {
        launchFragment()
        val preference: AccessibilityFooterPreference? =
            fragment!!.findPreference(PLAIN_TEXT_FOOTER_PREF_KEY)

        assertThat(preference).isNotNull()
        assertThat(preference!!.isVisible).isTrue()
        assertThat(preference.summary.toString()).isEqualTo(DEFAULT_DESCRIPTION)
        assertThat(preference.contentDescription.toString())
            .isEqualTo("About $DEFAULT_LABEL\n\n${preference.summary}")
    }

    @Test
    fun getMetricsCategory() {
        launchFragment()

        assertThat(fragment!!.metricsCategory).isEqualTo(SettingsEnums.ACCESSIBILITY_SERVICE)
    }

    private fun launchFragment(a11yShortcutInfo: AccessibilityShortcutInfo) {
        a11yManager.setInstalledAccessibilityShortcutListAsUser(listOf(a11yShortcutInfo))
        val bundle = Bundle()
        bundle.putParcelable(AccessibilitySettings.EXTRA_COMPONENT_NAME, PLACEHOLDER_A11Y_ACTIVITY)
        fragScenario =
            FragmentScenario.launch(
                    LaunchAccessibilityActivityPreferenceFragment::class.java,
                    bundle,
                    androidx.appcompat.R.style.Theme_AppCompat,
                    null as FragmentFactory?,
                )
                .moveToState(Lifecycle.State.RESUMED)
        fragScenario!!.onFragment(
            FragmentAction { frag: LaunchAccessibilityActivityPreferenceFragment? ->
                fragment = frag
            }
        )
    }

    override fun getShortcutToggle(): ShortcutPreference? {
        return fragment!!.findPreference<ShortcutPreference?>(SHORTCUT_PREF_KEY)
    }

    override fun launchFragment(): LaunchAccessibilityActivityPreferenceFragment {
        launchFragment(createMockAccessibilityShortcutInfo())
        return fragment!!
    }

    override fun getFeatureComponent(): ComponentName = PLACEHOLDER_A11Y_ACTIVITY

    private fun createMockAccessibilityShortcutInfo(): AccessibilityShortcutInfo {
        val mockInfo = mock<AccessibilityShortcutInfo>()
        val activityInfo = mock<ActivityInfo>()

        activityInfo.packageName = PLACEHOLDER_PACKAGE_NAME
        activityInfo.name = A11Y_ACTIVITY_CLASS_NAME
        activityInfo.applicationInfo = ApplicationInfo()
        whenever(mockInfo.activityInfo).thenReturn(activityInfo)
        whenever(activityInfo.loadLabel(any())).thenReturn(DEFAULT_LABEL)
        whenever(mockInfo.animatedImageRes).thenReturn(IMAGE_RES)
        whenever(mockInfo.loadSummary(any())).thenReturn(DEFAULT_SUMMARY)
        whenever(mockInfo.loadIntro(any())).thenReturn(DEFAULT_INTRO)
        whenever(mockInfo.loadHtmlDescription(any())).thenReturn(DEFAULT_HTML_DESCRIPTION)
        whenever(mockInfo.loadDescription(any())).thenReturn(DEFAULT_DESCRIPTION)
        whenever(mockInfo.componentName).thenReturn(PLACEHOLDER_A11Y_ACTIVITY)
        whenever(mockInfo.settingsActivityName).thenReturn(FAKE_A11Y_ACTIVITY_SETTINGS_CLASS_NAME)

        val settingsIntent = Intent(Intent.ACTION_MAIN, null)
        settingsIntent.setComponent(PLACEHOLDER_A11Y_ACTIVITY_SETTINGS)
        packageManager.addIntentFilterForActivity(
            settingsIntent.component,
            IntentFilter(settingsIntent.action),
        )

        val a11yActivityIntent = Intent()
        a11yActivityIntent.setComponent(PLACEHOLDER_A11Y_ACTIVITY)
        packageManager.addIntentFilterForActivity(a11yActivityIntent.component, /* filter= */ null)
        return mockInfo
    }

    private fun assertPrefExistsButInvisible(prefKey: String) {
        val preference: Preference? = fragment!!.findPreference(prefKey)
        assertThat(preference).isNotNull()
        assertThat(preference!!.isVisible).isFalse()
    }

    companion object {
        private const val TOP_INTRO_PREF_KEY = "top_intro"
        private const val ILLUSTRATION_PREF_KEY = "animated_image"
        private const val MAIN_BUTTON_PREF_KEY = "launch_preference"
        private const val SHORTCUT_PREF_KEY = "shortcut_preference_key"
        private const val A11Y_ACTIVITY_SETTINGS_PREF_KEY = "accessibility_activity_settings"
        private const val HTML_FOOTER_PERF_KEY = "html_footer_info"
        private const val PLAIN_TEXT_FOOTER_PREF_KEY = "footer_info"
        private const val DEFAULT_INTRO = "default intro"
        private const val DEFAULT_SUMMARY = "default summary"
        private const val DEFAULT_DESCRIPTION = "default description"
        private const val DEFAULT_HTML_DESCRIPTION = "<b>default html description</b><br/>"
        private const val DEFAULT_LABEL = "default label"
        private const val PLACEHOLDER_PACKAGE_NAME = "com.placeholder.example"
        private const val A11Y_ACTIVITY_CLASS_NAME = "fakeA11yActivityClass"
        private const val FAKE_A11Y_ACTIVITY_SETTINGS_CLASS_NAME = "fakeA11ySettingsClass"
        private val PLACEHOLDER_A11Y_ACTIVITY: ComponentName =
            ComponentName(PLACEHOLDER_PACKAGE_NAME, A11Y_ACTIVITY_CLASS_NAME)
        private val PLACEHOLDER_A11Y_ACTIVITY_SETTINGS: ComponentName =
            ComponentName(PLACEHOLDER_PACKAGE_NAME, FAKE_A11Y_ACTIVITY_SETTINGS_CLASS_NAME)
        private val IMAGE_RES = R.drawable.ic_accessibility_visibility
        private val IMAGE_URI = "android.resource://$PLACEHOLDER_PACKAGE_NAME/$IMAGE_RES".toUri()
    }
}
