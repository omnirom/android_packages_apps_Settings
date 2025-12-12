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
import android.provider.Settings.EXTRA_AUTOMATIC_ZEN_RULE_ID
import com.android.settings.R
import com.android.settings.Settings.ModeSettingsActivity
import com.android.settings.contract.TAG_DEVICE_STATE_SCREEN
import com.android.settings.core.PreferenceScreenMixin
import com.android.settingslib.metadata.PreferenceAvailabilityProvider
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.metadata.PreferenceSummaryProvider
import com.android.settingslib.metadata.PreferenceTitleProvider
import com.android.settingslib.metadata.preferenceHierarchy
import kotlinx.coroutines.CoroutineScope

// LINT.IfChange
//@ProvidePreferenceScreen(ZenModeBedtimeScreen.KEY)
open class ZenModeBedtimeScreen :
    PreferenceScreenMixin,
    PreferenceAvailabilityProvider,
    PreferenceTitleProvider,
    PreferenceSummaryProvider {

    override val key: String
        get() = KEY

    override fun getMetricsCategory() =
        SettingsEnums.PAGE_UNKNOWN // TODO: correct page id in future for non-virtual migration.

    override val highlightMenuKey: Int
        get() = R.string.menu_key_priority_modes

    override fun tags(context: Context) = arrayOf(TAG_DEVICE_STATE_SCREEN)

    override fun isFlagEnabled(context: Context) = false

    override fun isAvailable(context: Context) = context.hasBedtimeMode()

    override fun getTitle(context: Context): CharSequence? = context.getBedtimeMode()?.name

    override fun getSummary(context: Context): CharSequence? =
        context.getZenModeScreenSummary(context.getBedtimeMode())

    override fun getLaunchIntent(context: Context, metadata: PreferenceMetadata?): Intent? =
        Intent(context, ModeSettingsActivity::class.java)
            .putExtra(EXTRA_AUTOMATIC_ZEN_RULE_ID, context.getBedtimeMode()?.id)

    override fun getPreferenceHierarchy(context: Context, coroutineScope: CoroutineScope) =
        preferenceHierarchy(context) { +ZenModeButtonPreference(context.getBedtimeMode()!!) }

    companion object {
        const val KEY = "device_state_bedtime_mode_screen" // only for device state.
    }
}
// LINT.ThenChange(
//     ../ZenModeFragment.java,
//     ../ZenModesListPreferenceController.java,
//     ../ZenModesListItemPreference.java,
// )
