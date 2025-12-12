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

package com.android.settings.notification.modes.devicestate

import android.app.settings.SettingsEnums
import android.content.Context
import android.content.Intent
import com.android.settings.R
import com.android.settings.Settings.DndDisplaySettingsActivity
import com.android.settings.contract.TAG_DEVICE_STATE_SCREEN
import com.android.settings.core.PreferenceScreenMixin
import com.android.settings.notification.modes.ZenHelperBackend
import com.android.settings.notification.modes.ZenModeSummaryHelper
import com.android.settingslib.metadata.PreferenceAvailabilityProvider
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.metadata.PreferenceSummaryProvider
import com.android.settingslib.metadata.preferenceHierarchy
import kotlinx.coroutines.CoroutineScope

// LINT.IfChange
//@ProvidePreferenceScreen(ZenModeDndDisplayScreen.KEY)
open class ZenModeDndDisplayScreen :
    PreferenceScreenMixin, PreferenceAvailabilityProvider, PreferenceSummaryProvider {
    override val key: String
        get() = KEY

    override val title: Int
        get() = R.string.mode_display_settings_title

    override val icon: Int
        get() = R.drawable.ic_zen_mode_category_display

    override fun getMetricsCategory() =
        SettingsEnums.PAGE_UNKNOWN // TODO: correct page id in future for non-virtual migration.

    override val highlightMenuKey: Int
        get() = R.string.menu_key_priority_modes

    override fun tags(context: Context) = arrayOf(TAG_DEVICE_STATE_SCREEN)

    override fun isFlagEnabled(context: Context) = false

    override fun isAvailable(context: Context) = context.hasDndMode()

    override fun getSummary(context: Context): CharSequence? {
        val summaryHelper = ZenModeSummaryHelper(context, ZenHelperBackend.getInstance(context))
        return summaryHelper.getDisplayEffectsSummary(context.getDndMode()!!)
    }

    override fun getLaunchIntent(context: Context, metadata: PreferenceMetadata?): Intent? =
        Intent(context, DndDisplaySettingsActivity::class.java)

    override fun getPreferenceHierarchy(context: Context, coroutineScope: CoroutineScope) =
        preferenceHierarchy(context) {}

    companion object {
        const val KEY = "device_state_dnd_mode_display_settings" // only for device state.
    }
}
// LINT.ThenChange(
//     ../ZenModeDisplayFragment.java,
//     ../ZenModeDisplayLinkPreferenceController.java,
// )
