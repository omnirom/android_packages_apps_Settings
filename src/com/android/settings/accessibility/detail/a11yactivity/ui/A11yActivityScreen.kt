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

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.provider.Settings
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.preference.Preference
import com.android.settings.R
import com.android.settings.Utils
import com.android.settings.accessibility.AccessibilitySettings
import com.android.settings.accessibility.Flags
import com.android.settings.accessibility.LaunchAccessibilityActivityPreferenceFragment
import com.android.settings.accessibility.data.AccessibilityRepositoryProvider
import com.android.settings.accessibility.detail.a11yactivity.ui.A11yActivityFooterPreference.Companion.FOOTER_KEY
import com.android.settings.accessibility.detail.a11yactivity.ui.A11yActivityFooterPreference.Companion.HTML_FOOTER_KEY
import com.android.settings.accessibility.shared.ui.LaunchAppInfoPreference
import com.android.settings.core.PreferenceScreenMixin
import com.android.settings.overlay.FeatureFactory.Companion.featureFactory
import com.android.settings.utils.highlightPreference
import com.android.settingslib.metadata.PreferenceCategory
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.metadata.PreferenceSummaryProvider
import com.android.settingslib.metadata.PreferenceTitleProvider
import com.android.settingslib.metadata.ProvidePreferenceScreen
import com.android.settingslib.metadata.preferenceHierarchy
import com.android.settingslib.preference.PreferenceBinding
import com.android.settingslib.widget.TwoTargetPreference
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow

@ProvidePreferenceScreen(A11yActivityScreen.KEY, parameterized = true)
open class A11yActivityScreen(context: Context, override val arguments: Bundle) :
    PreferenceScreenMixin, PreferenceSummaryProvider, PreferenceTitleProvider, PreferenceBinding {
    private val packageManager = context.packageManager
    private val featureComponentName: ComponentName =
        requireNotNull(
            arguments.getParcelable(
                AccessibilitySettings.EXTRA_COMPONENT_NAME,
                ComponentName::class.java,
            )
        )
    private val accessibilityShortcutInfo =
        AccessibilityRepositoryProvider.get(context)
            .getAccessibilityShortcutInfo(featureComponentName)

    override fun isFlagEnabled(context: Context): Boolean {
        return Flags.catalystA11yActivityDetail()
    }

    override val highlightMenuKey: Int
        get() = R.string.menu_key_accessibility

    override val key: String
        get() = KEY

    override fun getMetricsCategory(): Int {
        return featureFactory.accessibilityPageIdFeatureProvider.getCategory(featureComponentName)
    }

    override fun getSummary(context: Context): CharSequence? {
        return accessibilityShortcutInfo?.loadSummary(packageManager)
    }

    override fun getTitle(context: Context): CharSequence? {
        return accessibilityShortcutInfo?.activityInfo?.loadLabel(packageManager)
    }

    override fun createWidget(context: Context): Preference {
        return TwoTargetPreference(context).apply {
            // setIconSize is the only reason why we need TwoTargetPreference
            setIconSize(TwoTargetPreference.ICON_SIZE_MEDIUM)
            isIconSpaceReserved = true
        }
    }

    override fun bind(preference: Preference, metadata: PreferenceMetadata) {
        super.bind(preference, metadata)
        val twoTargetPreference = preference as? TwoTargetPreference
        twoTargetPreference?.icon = getIcon(preference.context)
    }

    private fun getIcon(context: Context): Drawable {
        val icon =
            accessibilityShortcutInfo?.activityInfo?.run {
                if (iconResource != 0) loadIcon(packageManager) else null
            } ?: ContextCompat.getDrawable(context, R.drawable.ic_accessibility_generic)

        return Utils.getAdaptiveIcon(context, icon, Color.WHITE)
    }

    override fun fragmentClass(): Class<out Fragment>? {
        return LaunchAccessibilityActivityPreferenceFragment::class.java
    }

    override val bindingKey: String
        get() = featureComponentName.flattenToString()

    override fun getPreferenceHierarchy(context: Context, coroutineScope: CoroutineScope) =
        preferenceHierarchy(context) {
            val shortcutInfo = accessibilityShortcutInfo
            if (shortcutInfo != null) {
                +IntroPreference(shortcutInfo)
                +A11yActivityIllustrationPreference(shortcutInfo)
                +LaunchA11yActivityPreference(shortcutInfo)
                +PreferenceCategory(
                    key = "general_categories",
                    title = R.string.accessibility_screen_option,
                ) +=
                    {
                        +ShortcutPreference(context, shortcutInfo, metricsCategory)
                        +A11yActivitySettingPreference(shortcutInfo)
                        +LaunchAppInfoPreference(
                            key = "accessibility_activity_app_info",
                            packageName = shortcutInfo.componentName.packageName,
                        )
                    }
                +A11yActivityFooterPreference(HTML_FOOTER_KEY, shortcutInfo, loadHtmlFooter = true)
                +A11yActivityFooterPreference(FOOTER_KEY, shortcutInfo, loadHtmlFooter = false)
            }
        }

    override fun getLaunchIntent(context: Context, metadata: PreferenceMetadata?) =
        Intent(Settings.ACTION_ACCESSIBILITY_DETAILS_SETTINGS).apply {
            highlightPreference(arguments, metadata?.key)
            putExtra(Intent.EXTRA_COMPONENT_NAME, featureComponentName.flattenToString())
        }

    companion object {
        const val KEY = "a11y_activity_detail_screen"

        @OptIn(ExperimentalCoroutinesApi::class)
        @JvmStatic
        fun parameters(context: Context): Flow<Bundle> {

            return flow {
                AccessibilityRepositoryProvider.get(context)
                    .accessibilityShortcutInfos
                    .first()
                    .forEach { a11yShortcutInfo ->
                        emit(
                            Bundle(1).apply {
                                putParcelable(
                                    AccessibilitySettings.EXTRA_COMPONENT_NAME,
                                    a11yShortcutInfo.componentName,
                                )
                            }
                        )
                    }
            }
        }
    }
}
