/*
 * Copyright (C) 2024 The Android Open Source Project
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

import android.app.settings.SettingsEnums
import android.content.ComponentName
import android.hardware.display.ColorDisplayManager
import android.platform.test.annotations.RequiresFlagsDisabled
import android.platform.test.annotations.RequiresFlagsEnabled
import android.platform.test.flag.junit.DeviceFlagsValueProvider
import android.provider.Settings
import android.text.Html
import android.widget.TextView
import androidx.core.net.toUri
import androidx.fragment.app.FragmentFactory
import androidx.fragment.app.testing.FragmentScenario
import androidx.lifecycle.Lifecycle
import androidx.preference.Preference
import androidx.preference.TwoStatePreference
import com.android.internal.accessibility.AccessibilityShortcutController.REDUCE_BRIGHT_COLORS_COMPONENT_NAME
import com.android.settings.R
import com.android.settings.accessibility.extradim.ui.ExtraDimScreen
import com.android.settings.testutils.XmlTestUtils
import com.android.settings.testutils.inflateViewHolder
import com.android.settings.testutils.shadow.SettingsShadowResources
import com.android.settingslib.testutils.shadow.ShadowColorDisplayManager
import com.android.settingslib.widget.IllustrationPreference
import com.android.settingslib.widget.SliderPreference
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadow.api.Shadow
import org.robolectric.shadows.ShadowLooper

/** Tests for [ToggleReduceBrightColorsPreferenceFragment] */
@Config(shadows = [ShadowColorDisplayManager::class, SettingsShadowResources::class])
@RunWith(RobolectricTestRunner::class)
class ToggleReduceBrightColorsPreferenceFragmentTest :
    BaseShortcutInteractionsTestCases<ToggleReduceBrightColorsPreferenceFragment>() {

    @get:Rule val checkFlagsRule = DeviceFlagsValueProvider.createCheckFlagsRule()
    private val shadowColorDisplayManager: ShadowColorDisplayManager =
        Shadow.extract(context.getSystemService(ColorDisplayManager::class.java))
    private var fragScenario: FragmentScenario<ToggleReduceBrightColorsPreferenceFragment>? = null
    private var fragment: ToggleReduceBrightColorsPreferenceFragment? = null

    @Before
    fun setup() {
        setReduceBrightColorsAvailable(true)
    }

    @After
    fun cleanUp() {
        fragScenario?.close()
    }

    @Test
    fun onResume_verifyTopIntroText() {
        launchFragment()

        val topIntroPref: Preference? = fragment!!.findPreference(TOP_INTRO_PREF_KEY)
        assertThat(topIntroPref).isNotNull()

        assertThat(topIntroPref!!.title.toString())
            .isEqualTo(context.getString(R.string.reduce_bright_colors_preference_intro_text))
    }

    @RequiresFlagsEnabled(Flags.FLAG_CATALYST_EXTRA_DIM)
    @Test
    fun onResume_verifyIllustration() {
        launchFragment()

        val illustrationPref: IllustrationPreference? =
            fragment!!.findPreference(ILLUSTRATION_PREF_KEY)
        assertThat(illustrationPref).isNotNull()

        assertThat(illustrationPref!!.isVisible).isTrue()
        assertThat(illustrationPref.lottieAnimationResId).isEqualTo(R.raw.extra_dim_banner)
    }

    @RequiresFlagsDisabled(Flags.FLAG_CATALYST_EXTRA_DIM)
    @Test
    fun onResume_flagOff_verifyIllustration() {
        launchFragment()

        val illustrationPref: IllustrationPreference? =
            fragment!!.findPreference(ILLUSTRATION_PREF_KEY)
        assertThat(illustrationPref).isNotNull()

        assertThat(illustrationPref!!.isVisible).isTrue()
        assertThat(illustrationPref.imageUri).isEqualTo(IMAGE_URI)
    }

    @Test
    fun onResume_verifyExtraDimMainSwitchText() {
        launchFragment()

        val mainSwitch = getMainSwitch()
        assertThat(mainSwitch).isNotNull()
        assertThat(mainSwitch!!.isVisible).isTrue()

        assertThat(mainSwitch.title.toString())
            .isEqualTo(context.getString(R.string.reduce_bright_colors_switch_title))
    }

    @Test
    fun onResume_verifyIntensitySliderText() {
        launchFragment()

        val sliderPref = getIntensitySlider()
        assertThat(sliderPref).isNotNull()
        assertThat(sliderPref!!.isVisible).isTrue()
        val prefViewHolder = sliderPref.inflateViewHolder()

        assertThat(sliderPref.title.toString())
            .isEqualTo(context.getString(R.string.reduce_bright_colors_intensity_preference_title))
        val textStart: TextView? = prefViewHolder.findViewById(android.R.id.text1) as TextView?
        val textEnd: TextView? = prefViewHolder.findViewById(android.R.id.text2) as TextView?
        assertThat(textStart!!.text.toString())
            .isEqualTo(context.getString(R.string.brightness_intensity_start_label))
        assertThat(textEnd!!.text.toString())
            .isEqualTo(context.getString(R.string.brightness_intensity_end_label))
    }

    @Test
    fun onResume_verifyPersistentSwitchText() {
        launchFragment()

        val persistentPref = getPersistentToggle()
        assertThat(persistentPref).isNotNull()
        assertThat(persistentPref!!.isVisible).isTrue()

        assertThat(persistentPref.title.toString())
            .isEqualTo(context.getString(R.string.reduce_bright_colors_persist_preference_title))
    }

    @Test
    fun onResume_verifyShortcutText() {
        launchFragment()

        val shortcutPref = getShortcutToggle()
        assertThat(shortcutPref).isNotNull()
        assertThat(shortcutPref!!.isVisible).isTrue()

        assertThat(shortcutPref.title.toString())
            .isEqualTo(context.getString(R.string.reduce_bright_colors_shortcut_title))
    }

    @Test
    fun onResume_verifyHtmlFooterText() {
        launchFragment()

        val footerPref = fragment!!.findPreference<AccessibilityFooterPreference?>(FOOTER_PREF)
        assertThat(footerPref).isNotNull()
        assertThat(footerPref!!.isVisible).isTrue()
        assertThat(footerPref.summary.toString())
            .isEqualTo(
                Html.fromHtml(
                        context.getString(R.string.reduce_bright_colors_preference_subtitle),
                        Html.FROM_HTML_MODE_COMPACT,
                        /* imageGetter= */ null,
                        /* tagHandler= */ null,
                    )
                    .toString()
            )
        assertThat(footerPref.contentDescription.toString())
            .isEqualTo(
                "${context.getString(R.string.reduce_bright_colors_about_title)}\n\n${footerPref.summary}"
            )
    }

    @Test
    fun onResume_extraDimOff_mainSwitchIsUnchecked() {
        shadowColorDisplayManager.setReduceBrightColorsActivated(false)

        launchFragment()

        val mainSwitch = getMainSwitch()
        assertThat(mainSwitch).isNotNull()
        assertThat(mainSwitch!!.isVisible).isTrue()
        assertThat(mainSwitch.isChecked).isFalse()
    }

    @Test
    fun onResume_extraDimOn_mainSwitchIsChecked() {
        shadowColorDisplayManager.setReduceBrightColorsActivated(true)

        launchFragment()

        val mainSwitch = getMainSwitch()
        assertThat(mainSwitch).isNotNull()
        assertThat(mainSwitch!!.isVisible).isTrue()
        assertThat(mainSwitch.isChecked).isTrue()
    }

    @Test
    fun turnOffMainSwitch_extraDimTurnedOff() {
        onResume_extraDimOn_mainSwitchIsChecked()

        val mainSwitch = getMainSwitch()
        mainSwitch!!.performClick()
        ShadowLooper.idleMainLooper()

        assertThat(mainSwitch.isChecked).isFalse()
        assertThat(shadowColorDisplayManager.isReduceBrightColorsActivated).isFalse()
    }

    @Test
    fun turnOnMainSwitch_extraDimTurnedOn() {
        onResume_extraDimOff_mainSwitchIsUnchecked()

        val mainSwitch = getMainSwitch()
        mainSwitch!!.performClick()
        ShadowLooper.idleMainLooper()

        assertThat(mainSwitch.isChecked).isTrue()
        assertThat(shadowColorDisplayManager.isReduceBrightColorsActivated).isTrue()
    }

    @Test
    fun onResume_extraDimOff_intensityAndPersistentPrefsAreDisabled() {
        shadowColorDisplayManager.setReduceBrightColorsActivated(false)

        launchFragment()

        assertThat(getIntensitySlider()!!.isEnabled).isFalse()
        assertThat(getPersistentToggle()!!.isEnabled).isFalse()
    }

    @Test
    fun onResume_extraDimOn_intensityAndPersistentPrefsAreEnabled() {
        shadowColorDisplayManager.setReduceBrightColorsActivated(true)

        launchFragment()

        assertThat(getIntensitySlider()!!.isEnabled).isTrue()
        assertThat(getPersistentToggle()!!.isEnabled).isTrue()
    }

    @Test
    fun onResume_intensityIsX_sliderIsX() {
        shadowColorDisplayManager.setReduceBrightColorsActivated(true)
        val intensitySetting = 20
        val expectedSliderPosition = 80 // Max(100) - settingsValue
        Settings.Secure.putInt(
            context.contentResolver,
            Settings.Secure.REDUCE_BRIGHT_COLORS_LEVEL,
            intensitySetting,
        )

        launchFragment()

        assertThat(getIntensitySlider()!!.value).isEqualTo(expectedSliderPosition)
    }

    @Test
    fun changeSliderValue_intensitySettingsReflectsValue() {
        shadowColorDisplayManager.setReduceBrightColorsActivated(true)
        val intensitySetting = 20
        Settings.Secure.putInt(
            context.contentResolver,
            Settings.Secure.REDUCE_BRIGHT_COLORS_LEVEL,
            intensitySetting,
        )
        val newIntensitySetting = 40
        val newSliderPosition = 60 // Max - settingsValue
        launchFragment()

        val sliderPref = getIntensitySlider()!!
        sliderPref.callChangeListener(newSliderPosition)
        sliderPref.value = newSliderPosition
        ShadowLooper.idleMainLooper()

        val settingValue =
            Settings.Secure.getInt(
                context.contentResolver,
                Settings.Secure.REDUCE_BRIGHT_COLORS_LEVEL,
                /* def= */ 0,
            )
        assertThat(settingValue).isEqualTo(newIntensitySetting)
    }

    @Test
    fun onResume_persistentOn_switchIsChecked() {
        shadowColorDisplayManager.setReduceBrightColorsActivated(true)
        Settings.Secure.putInt(
            context.contentResolver,
            Settings.Secure.REDUCE_BRIGHT_COLORS_PERSIST_ACROSS_REBOOTS,
            1,
        )

        launchFragment()
        val persistentPref = getPersistentToggle()
        assertThat(persistentPref).isNotNull()
        assertThat(persistentPref!!.isVisible).isTrue()
        assertThat(persistentPref.isChecked).isTrue()
    }

    @Test
    fun turnOffPersistent_persistentSettingsIsOff() {
        onResume_persistentOn_switchIsChecked()

        val persistentPref = getPersistentToggle()!!
        persistentPref.performClick()
        ShadowLooper.idleMainLooper()

        assertThat(persistentPref.isChecked).isFalse()
        val settingValue =
            Settings.Secure.getInt(
                context.contentResolver,
                Settings.Secure.REDUCE_BRIGHT_COLORS_PERSIST_ACROSS_REBOOTS,
                0,
            )
        assertThat(settingValue).isEqualTo(0)
    }

    @Test
    fun getFeatureName() {
        launchFragment()

        val featureName = fragment!!.getFeatureName().toString()
        assertThat(featureName)
            .isEqualTo(context.getString(R.string.reduce_bright_colors_preference_title).toString())
    }

    @Test
    fun getMetricsCategory() {
        val category = ToggleReduceBrightColorsPreferenceFragment().getMetricsCategory()
        assertThat(category).isEqualTo(SettingsEnums.REDUCE_BRIGHT_COLORS_SETTINGS)
    }

    @Test
    fun getPreferenceScreenResId() {
        assertThat(ToggleReduceBrightColorsPreferenceFragment().getPreferenceScreenResId())
            .isEqualTo(R.xml.accessibility_extra_dim_settings)
    }

    @RequiresFlagsDisabled(
        Flags.FLAG_CATALYST_EXTRA_DIM,
        com.android.settings.flags.Flags.FLAG_CATALYST_SETTINGS_SEARCH,
    )
    @Test
    fun getNonIndexableKeys_reduceBrightColorUnavailable_allElementsInXmlIsNonSearchable() {
        setReduceBrightColorsAvailable(false)
        val expectedKeys =
            XmlTestUtils.getKeysFromPreferenceXml(context, R.xml.accessibility_extra_dim_settings)

        val actualKeys =
            ToggleReduceBrightColorsPreferenceFragment.SEARCH_INDEX_DATA_PROVIDER
                .getNonIndexableKeys(context)
                .filterNotNull()

        assertThat(actualKeys).containsExactlyElementsIn(expectedKeys)
    }

    @RequiresFlagsDisabled(
        Flags.FLAG_CATALYST_EXTRA_DIM,
        com.android.settings.flags.Flags.FLAG_CATALYST_SETTINGS_SEARCH,
    )
    @Test
    fun getNonIndexableKey_reduceBrightColorAvailable_containsOnlyNonSearchablePrefKeys() {
        shadowColorDisplayManager.setReduceBrightColorsActivated(true)
        val expectedKeys =
            listOf(
                TOP_INTRO_PREF_KEY,
                ILLUSTRATION_PREF_KEY,
                PERSISTS_AFTER_RESTORE_PERF_KEY,
                FOOTER_PREF,
            )

        val actualKeys =
            ToggleReduceBrightColorsPreferenceFragment.SEARCH_INDEX_DATA_PROVIDER
                .getNonIndexableKeys(context)
                .filterNotNull()

        assertThat(actualKeys).containsExactlyElementsIn(expectedKeys)
    }

    @RequiresFlagsEnabled(
        Flags.FLAG_CATALYST_EXTRA_DIM,
        com.android.settings.flags.Flags.FLAG_CATALYST_SETTINGS_SEARCH,
    )
    @Test
    fun getXmlResourceToIndex_catalystSearch() {
        val indexableResources =
            ToggleReduceBrightColorsPreferenceFragment.SEARCH_INDEX_DATA_PROVIDER
                .getXmlResourcesToIndex(context, true)

        assertThat(indexableResources).isNull()
    }

    @Test
    fun getPreferenceScreenBindingKey_returnExtraDimScreenKey() {
        assertThat(
                ToggleReduceBrightColorsPreferenceFragment().getPreferenceScreenBindingKey(context)
            )
            .isEqualTo(ExtraDimScreen.KEY)
    }

    private fun setReduceBrightColorsAvailable(available: Boolean) {
        SettingsShadowResources.overrideResource(
            com.android.internal.R.bool.config_reduceBrightColorsAvailable,
            available,
        )
        SettingsShadowResources.overrideResource(
            com.android.internal.R.bool.config_evenDimmerEnabled,
            false,
        )
    }

    private fun getMainSwitch(): TwoStatePreference? {
        return fragment!!.findPreference(MAIN_SWITCH_PREF_KEY)
    }

    private fun getIntensitySlider(): SliderPreference? {
        return fragment!!.findPreference(INTENSITY_PREF_KEY)
    }

    private fun getPersistentToggle(): TwoStatePreference? {
        return fragment!!.findPreference(PERSISTS_AFTER_RESTORE_PERF_KEY)
    }

    override fun getShortcutToggle(): ShortcutPreference? {
        return fragment!!.findPreference(SHORTCUT_PREF_KEY)
    }

    override fun launchFragment(): ToggleReduceBrightColorsPreferenceFragment {
        fragScenario =
            FragmentScenario.launch(
                    ToggleReduceBrightColorsPreferenceFragment::class.java,
                    /* fragArgs= */ null,
                    androidx.appcompat.R.style.Theme_AppCompat,
                    null as FragmentFactory?,
                )
                .moveToState(Lifecycle.State.RESUMED)
        fragScenario!!.onFragment { frag: ToggleReduceBrightColorsPreferenceFragment? ->
            fragment = frag
        }
        return fragment!!
    }

    override fun getFeatureComponent(): ComponentName = REDUCE_BRIGHT_COLORS_COMPONENT_NAME

    companion object {
        private const val TOP_INTRO_PREF_KEY = "top_intro"
        private const val ILLUSTRATION_PREF_KEY = "animated_image"
        private const val MAIN_SWITCH_PREF_KEY = "reduce_bright_colors_switch"
        private const val INTENSITY_PREF_KEY = "reduce_bright_colors_level"
        private const val PERSISTS_AFTER_RESTORE_PERF_KEY =
            "reduce_bright_colors_persist_across_reboots"
        private const val SHORTCUT_PREF_KEY = "reduce_bright_colors_shortcut"
        private const val FOOTER_PREF = "html_description"
        private val IMAGE_RES = R.raw.extra_dim_banner
        private val IMAGE_URI = "android.resource://com.android.settings/$IMAGE_RES".toUri()
    }
}
