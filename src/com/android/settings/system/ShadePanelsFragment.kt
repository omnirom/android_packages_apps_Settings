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

package com.android.settings.system

import android.app.settings.SettingsEnums
import android.content.Context
import android.view.View.LAYOUT_DIRECTION_RTL
import androidx.annotation.VisibleForTesting
import androidx.preference.PreferenceScreen
import com.android.settings.R
import com.android.settings.Utils.isDeviceFoldable
import com.android.settings.search.BaseSearchIndexProvider
import com.android.settings.support.actionbar.HelpResourceProvider
import com.android.settings.system.ShadePanelsPreferenceController.Companion.isDualShadeEnabled
import com.android.settings.system.ShadePanelsPreferenceController.Companion.setDualShadeEnabled
import com.android.settings.utils.CandidateInfoExtra
import com.android.settings.widget.RadioButtonPickerFragment
import com.android.settingslib.search.SearchIndexable
import com.android.settingslib.search.SearchIndexableRaw
import com.android.settingslib.widget.CandidateInfo
import com.android.settingslib.widget.FooterPreference
import com.android.settingslib.widget.SelectorWithWidgetPreference

/**
 * The preference fragment for the Settings page controlling Notifications & Quick Settings panels,
 * allowing the user to switch between "Dual Shade" and "Single Shade".
 */
@SearchIndexable
class ShadePanelsFragment : RadioButtonPickerFragment(), HelpResourceProvider {

    override fun getPreferenceScreenResId(): Int = R.xml.shade_panels_settings

    // TODO(b/409228328): Add a dedicated Settings enum for Shade Panels in
    //  `stats/enums/app/settings_enums.proto`, and refer to it here.
    override fun getMetricsCategory(): Int = SettingsEnums.PAGE_VISIBLE

    override fun onAttach(context: Context) {
        super.onAttach(context)

        setCategory(R.string.shade_panels_category)
        setIllustrationForSelection(getDefaultKey())
    }

    override fun getCandidates(): List<CandidateInfo> {
        val context = requireContext()
        return listOf(
            // Separate panels option (aka Dual Shade)
            CandidateInfoExtra(
                context.getText(R.string.shade_panels_separate_title),
                context.getText(R.string.shade_panels_separate_summary),
                KEY_DUAL_SHADE_PREFERENCE,
                true
            ),
            // Combined panels option (aka Single Shade)
            CandidateInfoExtra(
                context.getText(R.string.shade_panels_combined_title),
                context.getText(R.string.shade_panels_combined_summary),
                KEY_SINGLE_SHADE_PREFERENCE,
                true
            )
        )
    }

    override fun addStaticPreferences(screen: PreferenceScreen) {
        val context = requireContext()
        if (isDeviceFoldable(context)) {
            screen.addPreference(
                FooterPreference(context).apply {
                    title = context.getText(R.string.shade_panels_foldables_footer_message)
                }
            )
        }
    }

    override fun bindPreferenceExtra(
        pref: SelectorWithWidgetPreference,
        key: String,
        info: CandidateInfo?,
        defaultKey: String,
        systemDefaultKey: String?,
    ) {
        // Bind the summary of each radio button, as it's not included in the basic `CandidateInfo`.
        if (info is CandidateInfoExtra) {
            pref.setSummary(info.loadSummary())
        }
    }

    /** Retrieve the persisted value. */
    override fun getDefaultKey(): String {
        val contentResolver = requireContext().contentResolver
        return if (contentResolver.isDualShadeEnabled()) {
            KEY_DUAL_SHADE_PREFERENCE
        } else {
            KEY_SINGLE_SHADE_PREFERENCE
        }
    }

    /** Persist the selected value. */
    override fun setDefaultKey(key: String): Boolean {
        val enableDualShade = key == KEY_DUAL_SHADE_PREFERENCE
        return requireContext().contentResolver.setDualShadeEnabled(enableDualShade)
    }

    override fun onSelectionPerformed(success: Boolean) {
        if (success) {
            setIllustrationForSelection(getDefaultKey())
            updateCandidates()
        }
    }

    private fun setIllustrationForSelection(selectedKey: String) {
        val configuration = requireContext().getResources().getConfiguration()
        val isRtl = configuration.getLayoutDirection() == LAYOUT_DIRECTION_RTL
        setIllustration(
            when (selectedKey) {
                KEY_DUAL_SHADE_PREFERENCE ->
                    if (isRtl) R.raw.lottie_shade_panels_separate_rtl
                    else R.raw.lottie_shade_panels_separate_ltr
                KEY_SINGLE_SHADE_PREFERENCE ->
                    if (isRtl) R.raw.lottie_shade_panels_combined_rtl
                    else R.raw.lottie_shade_panels_combined_ltr
                else -> 0 // Indicate no specific animation for unknown keys
            },
            IllustrationType.LOTTIE_ANIMATION
        )
    }

    companion object {
        @VisibleForTesting
        const val KEY_DUAL_SHADE_PREFERENCE = "dual_shade"
        @VisibleForTesting
        const val KEY_SINGLE_SHADE_PREFERENCE = "single_shade"

        // Expose as a static field for the @SearchIndexable annotation processor.
        @JvmField
        val SEARCH_INDEX_DATA_PROVIDER: BaseSearchIndexProvider =
            object : BaseSearchIndexProvider(R.xml.shade_panels_settings) {

                override fun isPageSearchEnabled(context: Context): Boolean =
                    ShadePanelsPreferenceController.isDualShadeAvailable(context)

                override fun getRawDataToIndex(context: Context, enabled: Boolean): List<SearchIndexableRaw> {
                    return listOf(
                        SearchIndexableRaw(context).apply {
                            title = context.getString(R.string.shade_panels_separate_title)
                            summaryOn = context.getString(R.string.shade_panels_separate_summary)
                            key = KEY_DUAL_SHADE_PREFERENCE
                        },
                        SearchIndexableRaw(context).apply {
                            title = context.getString(R.string.shade_panels_combined_title)
                            summaryOn = context.getString(R.string.shade_panels_combined_summary)
                            key = KEY_SINGLE_SHADE_PREFERENCE
                        },
                    )
                }
            }
    }
}
