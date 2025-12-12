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
package com.android.settings.display

import android.app.settings.SettingsEnums.ACTION_AMBIENT_DISPLAY_ALWAYS_ON
import android.content.Context
import android.hardware.display.AmbientDisplayConfiguration
import android.os.SystemProperties
import android.os.UserHandle
import android.os.UserManager
import com.android.settings.R
import com.android.settings.contract.KEY_AMBIENT_DISPLAY_ALWAYS_ON
import com.android.settings.display.AmbientDisplayAlwaysOnPreferenceController.isAodSuppressedByBedtime
import com.android.settings.display.ambient.AmbientDisplayStorage
import com.android.settings.metrics.PreferenceActionMetricsProvider
import com.android.settings.restriction.PreferenceRestrictionMixin
import com.android.settingslib.datastore.KeyValueStore
import com.android.settingslib.datastore.SettingsSecureStore
import com.android.settingslib.metadata.PreferenceAvailabilityProvider
import com.android.settingslib.metadata.PreferenceSummaryProvider
import com.android.settingslib.metadata.ReadWritePermit
import com.android.settingslib.metadata.SensitivityLevel
import com.android.settingslib.metadata.SwitchPreference

// LINT.IfChange
/**
 * Contains the SwitchPreference for use on the Lock screen page. It is being migrated to
 * [AmbientDisplayAlwaysOnPreferenceScreen].
 */
class AmbientDisplayAlwaysOnPreference :
    SwitchPreference(KEY, R.string.doze_always_on_title, R.string.doze_always_on_summary),
    PreferenceActionMetricsProvider,
    PreferenceAvailabilityProvider,
    PreferenceSummaryProvider,
    PreferenceRestrictionMixin {

    override val keywords: Int
        get() = R.string.keywords_always_show_time_info

    override val preferenceActionMetrics: Int
        get() = ACTION_AMBIENT_DISPLAY_ALWAYS_ON

    override fun tags(context: Context) = arrayOf(KEY_AMBIENT_DISPLAY_ALWAYS_ON)

    override val restrictionKeys: Array<String>
        get() = arrayOf(UserManager.DISALLOW_AMBIENT_DISPLAY)

    override fun isEnabled(context: Context) = super<PreferenceRestrictionMixin>.isEnabled(context)

    override fun isAvailable(context: Context) =
        !SystemProperties.getBoolean(PROP_AWARE_AVAILABLE, false) &&
            AmbientDisplayConfiguration(context).alwaysOnAvailableForUser(UserHandle.myUserId())

    override fun getSummary(context: Context): CharSequence? =
        context.getText(
            when {
                isAodSuppressedByBedtime(context) -> R.string.aware_summary_when_bedtime_on
                else -> R.string.doze_always_on_summary
            }
        )

    override fun storage(context: Context): KeyValueStore = AmbientDisplayStorage(context)

    override fun getReadPermissions(context: Context) = SettingsSecureStore.getReadPermissions()

    override fun getWritePermissions(context: Context) = SettingsSecureStore.getWritePermissions()

    override fun getReadPermit(context: Context, callingPid: Int, callingUid: Int) =
        ReadWritePermit.ALLOW

    override fun getWritePermit(context: Context, callingPid: Int, callingUid: Int) =
        ReadWritePermit.ALLOW

    override val sensitivityLevel
        get() = SensitivityLevel.NO_SENSITIVITY

    companion object {
        const val KEY = KEY_AMBIENT_DISPLAY_ALWAYS_ON
        const val PROP_AWARE_AVAILABLE = "ro.vendor.aware_available"
    }
}

// LINT.ThenChange(AmbientDisplayAlwaysOnPreferenceController.java)
