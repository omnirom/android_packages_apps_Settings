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
package com.android.settings.gestures

import android.app.settings.SettingsEnums
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.view.WindowManagerPolicyConstants.NAV_BAR_MODE_2BUTTON
import android.view.WindowManagerPolicyConstants.NAV_BAR_MODE_GESTURAL
import com.android.internal.R as InternalR
import com.android.settings.R
import com.android.settings.Settings.NavigationModeSettingsActivity
import com.android.settings.core.PreferenceScreenMixin
import com.android.settings.flags.Flags
import com.android.settings.utils.makeLaunchIntent
import com.android.settingslib.metadata.PreferenceAvailabilityProvider
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.metadata.PreferenceSummaryProvider
import com.android.settingslib.metadata.ProvidePreferenceScreen
import com.android.settingslib.metadata.preferenceHierarchy
import kotlinx.coroutines.CoroutineScope

// LINT.IfChange
@ProvidePreferenceScreen(SystemNavigationGestureScreen.KEY)
class SystemNavigationGestureScreen :
    PreferenceScreenMixin, PreferenceAvailabilityProvider, PreferenceSummaryProvider {

    override val key: String
        get() = KEY

    override val title: Int
        get() = R.string.system_navigation_title

    override val highlightMenuKey: Int
        get() = R.string.menu_key_system

    override fun getMetricsCategory() = SettingsEnums.SETTINGS_GESTURE_SWIPE_UP

    override fun isFlagEnabled(context: Context) = Flags.deeplinkSystem25q4()

    override fun fragmentClass() = SystemNavigationGestureSettings::class.java

    override fun getPreferenceHierarchy(context: Context, coroutineScope: CoroutineScope) =
        preferenceHierarchy(context) {}

    override fun hasCompleteHierarchy() = false

    override val keywords: Int
        get() = R.string.keywords_system_navigation

    override fun isAvailable(context: Context) = context.isGestureAvailable()

    override fun getLaunchIntent(context: Context, metadata: PreferenceMetadata?) =
        makeLaunchIntent(context, NavigationModeSettingsActivity::class.java, metadata?.key)

    override fun getSummary(context: Context): CharSequence? =
        when {
            context.isGestureNavigationEnabled() ->
                context.getText(R.string.edge_to_edge_navigation_title)
            context.is2ButtonNavigationEnabled() ->
                context.getText(R.string.swipe_up_to_switch_apps_title)
            else -> context.getText(R.string.legacy_navigation_title)
        }

    fun Context.isGestureAvailable(): Boolean {
        // Skip if the swipe up settings are not available
        if (!resources.getBoolean(InternalR.bool.config_swipe_up_gesture_setting_available)) {
            return false
        }

        // Skip if the recents component is not defined
        val recentsComponentName =
            ComponentName.unflattenFromString(
                getString(InternalR.string.config_recentsComponentName)
            ) ?: return false

        // Skip if the overview proxy service exists
        val quickStepIntent = Intent(ACTION_QUICKSTEP).setPackage(recentsComponentName.packageName)
        return packageManager.resolveService(quickStepIntent, PackageManager.MATCH_SYSTEM_ONLY) !=
            null
    }

    fun Context.isGestureNavigationEnabled(): Boolean =
        NAV_BAR_MODE_GESTURAL ==
            resources.getInteger(InternalR.integer.config_navBarInteractionMode)

    fun Context.is2ButtonNavigationEnabled(): Boolean =
        NAV_BAR_MODE_2BUTTON == resources.getInteger(InternalR.integer.config_navBarInteractionMode)

    companion object {
        const val KEY = "gesture_system_navigation_input_summary"

        private const val ACTION_QUICKSTEP = "android.intent.action.QUICKSTEP_SERVICE"
    }
}
// LINT.ThenChange(SystemNavigationGestureSettings.java, SystemNavigationPreferenceController.java)
