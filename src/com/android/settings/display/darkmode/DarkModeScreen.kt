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

package com.android.settings.display.darkmode

import android.app.UiModeManager
import android.app.settings.SettingsEnums
import android.app.settings.SettingsEnums.ACTION_DARK_THEME
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import androidx.fragment.app.Fragment
import androidx.preference.Preference
import com.android.settings.R
import com.android.settings.Settings.DarkThemeSettingsActivity
import com.android.settings.accessibility.FeedbackManager
import com.android.settings.accessibility.Flags
import com.android.settings.accessibility.ForceInvertSurveyButtonPreference
import com.android.settings.accessibility.shared.ui.FeedbackButtonPreference
import com.android.settings.contract.KEY_DARK_THEME
import com.android.settings.core.PreferenceScreenMixin
import com.android.settings.metrics.PreferenceActionMetricsProvider
import com.android.settings.utils.makeLaunchIntent
import com.android.settingslib.PrimarySwitchPreferenceBinding
import com.android.settingslib.datastore.HandlerExecutor
import com.android.settingslib.datastore.KeyValueStore
import com.android.settingslib.datastore.KeyedObserver
import com.android.settingslib.metadata.BooleanValuePreference
import com.android.settingslib.metadata.PreferenceCategory
import com.android.settingslib.metadata.PreferenceIndexableProvider
import com.android.settingslib.metadata.PreferenceLifecycleContext
import com.android.settingslib.metadata.PreferenceLifecycleProvider
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.metadata.PreferenceSummaryProvider
import com.android.settingslib.metadata.ProvidePreferenceScreen
import com.android.settingslib.metadata.ReadWritePermit
import com.android.settingslib.metadata.SensitivityLevel
import com.android.settingslib.metadata.preferenceHierarchy
import kotlinx.coroutines.CoroutineScope

// LINT.IfChange
abstract class BaseDarkModeScreen(context: Context) :
    PreferenceScreenMixin,
    PrimarySwitchPreferenceBinding,
    PreferenceActionMetricsProvider,
    BooleanValuePreference,
    PreferenceSummaryProvider,
    PreferenceIndexableProvider,
    PreferenceLifecycleProvider {

    private val darkModeStorage = DarkModeStorage(context)
    private var powerSaveModeObserver: KeyedObserver<String?>? = null

    override val title: Int
        get() = R.string.dark_ui_mode

    override val keywords: Int
        get() = R.string.keywords_dark_ui_mode

    override val highlightMenuKey: Int
        get() = R.string.menu_key_display

    override fun getMetricsCategory() = SettingsEnums.DARK_UI_SETTINGS

    override val preferenceActionMetrics: Int
        get() = ACTION_DARK_THEME

    override fun tags(context: Context) = arrayOf(KEY_DARK_THEME)

    override fun getReadPermissions(context: Context) = DarkModeStorage.getReadPermissions()

    override fun getWritePermissions(context: Context) = DarkModeStorage.getWritePermissions()

    override fun getReadPermit(context: Context, callingPid: Int, callingUid: Int) =
        ReadWritePermit.ALLOW

    override fun getWritePermit(context: Context, callingPid: Int, callingUid: Int) =
        ReadWritePermit.ALLOW

    override val sensitivityLevel
        get() = SensitivityLevel.NO_SENSITIVITY

    override fun isFlagEnabled(context: Context) = Flags.catalystDarkUiMode()

    override fun fragmentClass(): Class<out Fragment>? = DarkModeSettingsFragment::class.java

    override fun getLaunchIntent(context: Context, metadata: PreferenceMetadata?): Intent? =
        makeLaunchIntent(context, DarkThemeSettingsActivity::class.java, metadata?.key)

    override fun getPreferenceHierarchy(context: Context, coroutineScope: CoroutineScope) =
        preferenceHierarchy(context) {
            +DarkModeTopIntroPreference()
            +DarkModeMainSwitchPreference(darkModeStorage)
            +TwilightLocationPreference()
            if (android.view.accessibility.Flags.forceInvertColor()) {
                +PreferenceCategory("dark_theme_group", R.string.dark_theme_version_category) += {
                    val modeStorage = DarkThemeModeStorage(context)
                    +StandardDarkModeSelectorPreference(modeStorage)
                    +ExpandedDarkModeSelectorPreference(modeStorage)
                }
            }
            +PreferenceCategory("display_category", R.string.dark_theme_timing_category) += {
                val uiModeManager = context.getSystemService(UiModeManager::class.java)
                +DarkModeSchedulePreference(uiModeManager!!, BedtimeSettings(context))
                +StartTimePreference(uiModeManager)
                +EndTimePreference(uiModeManager)
            }
            +DarkModePendingLocationFooterPreference()
            +DarkModeExpandedFooterPreference()
            +DarkModeCustomModesFooterPreference()
            +FeedbackButtonPreference { FeedbackManager(context, metricsCategory) }
            +ForceInvertSurveyButtonPreference(metricsCategory)
        }

    override fun storage(context: Context): KeyValueStore = darkModeStorage

    override fun bind(preference: Preference, metadata: PreferenceMetadata) {
        super.bind(preference, metadata)
        if (preference is DarkModePreference) preference.setCatalystEnabled(true)
    }

    override fun onStart(context: PreferenceLifecycleContext) {
        if (isContainer(context)) {
            val observer = KeyedObserver<String?> { _, _ -> context.notifyPreferenceChange(key) }
            powerSaveModeObserver = observer
            PowerSaveModeObservable.get(context).addObserver(observer, HandlerExecutor.main)
        }
    }

    override fun onStop(context: PreferenceLifecycleContext) {
        if (isContainer(context)) {
            powerSaveModeObserver?.let {
                PowerSaveModeObservable.get(context).removeObserver(it)
                powerSaveModeObserver = null
            }
        }
    }

    override fun isEnabled(context: Context) = !context.isPowerSaveMode()

    override fun isIndexable(context: Context) =
        Flags.catalystDarkUiMode() && !context.isPowerSaveMode()

    override fun getSummary(context: Context): CharSequence? {
        val active = darkModeStorage.getBoolean(key) == true
        return when {
            !context.isPowerSaveMode() -> AutoDarkTheme.getStatus(context, active)
            active -> context.getString(R.string.dark_ui_mode_disabled_summary_dark_theme_on)
            else -> context.getString(R.string.dark_ui_mode_disabled_summary_dark_theme_off)
        }
    }

    companion object {
        private fun Context.isPowerSaveMode() =
            getSystemService(PowerManager::class.java)?.isPowerSaveMode == true
    }
}

// LINT.ThenChange(../DarkUIPreferenceController.java)

@ProvidePreferenceScreen(DarkModeScreen.KEY)
open class DarkModeScreen(context: Context) : BaseDarkModeScreen(context) {
    override val key
        get() = KEY

    companion object {
        const val KEY = "dark_ui_mode"
    }
}

@ProvidePreferenceScreen(DarkModeScreenOnAccessibility.KEY)
open class DarkModeScreenOnAccessibility(context: Context) : BaseDarkModeScreen(context) {
    override val key
        get() = KEY

    override val icon: Int
        get() = R.drawable.ic_dark_ui

    override fun isIndexable(context: Context) = false

    companion object {
        const val KEY = "dark_ui_mode_accessibility"
    }
}
