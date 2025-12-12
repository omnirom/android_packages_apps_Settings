/*
 * Copyright (C) 2025 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings.notification.modes

import android.app.settings.SettingsEnums
import android.content.Context
import android.os.UserManager.DISALLOW_ADJUST_VOLUME
import androidx.fragment.app.Fragment
import com.android.settings.R
import com.android.settings.Settings.ModesSettingsActivity
import com.android.settings.contract.TAG_DEVICE_STATE_PREFERENCE
import com.android.settings.contract.TAG_DEVICE_STATE_SCREEN
import com.android.settings.core.PreferenceScreenMixin
import com.android.settings.flags.Flags
import com.android.settings.restriction.PreferenceRestrictionMixin
import com.android.settings.utils.makeLaunchIntent
import com.android.settingslib.metadata.PreferenceIconProvider
import com.android.settingslib.metadata.PreferenceLifecycleContext
import com.android.settingslib.metadata.PreferenceLifecycleProvider
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.metadata.PreferenceSummaryProvider
import com.android.settingslib.metadata.ProvidePreferenceScreen
import com.android.settingslib.metadata.preferenceHierarchy
import com.android.settingslib.notification.modes.ZenModesBackend
import com.android.settingslib.widget.SettingsThemeHelper.isExpressiveTheme
import kotlinx.coroutines.CoroutineScope

// LINT.IfChange
@ProvidePreferenceScreen(ZenModesListScreen.KEY)
open class ZenModesListScreen :
    PreferenceScreenMixin,
    PreferenceRestrictionMixin,
    PreferenceIconProvider,
    PreferenceSummaryProvider,
    PreferenceLifecycleProvider {

    private var zenSettingsObserver: ZenSettingsObserver? = null

    override val key: String
        get() = KEY

    override val title: Int
        get() = R.string.zen_modes_list_title

    override val highlightMenuKey: Int
        get() = R.string.menu_key_priority_modes

    override fun getSummary(context: Context): CharSequence? {
        val zenModesBackend = ZenModesBackend.getInstance(context)
        val zenModesSummaryBuilder =
            ZenModeSummaryHelper(context, ZenHelperBackend.getInstance(context))
        try {
            return zenModesSummaryBuilder.getModesSummary(zenModesBackend.getModes())
        } catch (e: SecurityException) {
            return null
        }
    }

    override fun getIcon(context: Context) =
        when {
            isExpressiveTheme(context) -> R.drawable.ic_homepage_modes
            else -> com.android.internal.R.drawable.ic_zen_priority_modes
        }

    override fun isEnabled(context: Context) = super<PreferenceRestrictionMixin>.isEnabled(context)

    override val restrictionKeys: Array<String> = arrayOf(DISALLOW_ADJUST_VOLUME)

    override fun getMetricsCategory(): Int = SettingsEnums.ZEN_PRIORITY_MODES_LIST

    override fun tags(context: Context) =
        arrayOf(TAG_DEVICE_STATE_SCREEN, TAG_DEVICE_STATE_PREFERENCE)

    override fun isFlagEnabled(context: Context) = Flags.deeplinkModes25q4()

    override fun hasCompleteHierarchy() = false

    override fun getLaunchIntent(context: Context, metadata: PreferenceMetadata?) =
        makeLaunchIntent(context, ModesSettingsActivity::class.java, metadata?.key)

    override fun fragmentClass(): Class<out Fragment> = ZenModesListFragment::class.java

    override fun getPreferenceHierarchy(context: Context, coroutineScope: CoroutineScope) =
        preferenceHierarchy(context) {}

    override fun onStart(context: PreferenceLifecycleContext) {
        if (isEntryPoint(context)) {
            val observer = ZenSettingsObserver(context) { context.notifyPreferenceChange(KEY) }
            observer.register()
            zenSettingsObserver = observer
        }
    }

    override fun onStop(context: PreferenceLifecycleContext) {
        if (isEntryPoint(context)) {
            zenSettingsObserver?.unregister()
            zenSettingsObserver = null
        }
    }

    companion object {
        const val KEY = "top_level_priority_modes"
    }
}
// LINT.ThenChange(ZenModesListFragment.java)
