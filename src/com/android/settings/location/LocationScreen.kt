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
package com.android.settings.location

import android.app.settings.SettingsEnums
import android.content.Context
import android.content.Intent
import android.location.LocationManager
import androidx.fragment.app.Fragment
import com.android.settings.R
import com.android.settings.core.PreferenceScreenMixin
import com.android.settings.flags.Flags
import com.android.settingslib.datastore.KeyValueStore
import com.android.settingslib.datastore.NoOpKeyedObservable
import com.android.settingslib.metadata.BooleanValuePreference
import com.android.settingslib.metadata.PreferenceIconProvider
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.metadata.PreferenceSummaryProvider
import com.android.settingslib.metadata.ProvidePreferenceScreen
import com.android.settingslib.metadata.ReadWritePermit
import com.android.settingslib.metadata.SensitivityLevel
import com.android.settingslib.metadata.preferenceHierarchy
import com.android.settingslib.widget.MainSwitchPreferenceBinding
import com.android.settingslib.widget.SettingsThemeHelper.isExpressiveTheme
import kotlinx.coroutines.CoroutineScope

@ProvidePreferenceScreen(LocationScreen.KEY)
open class LocationScreen :
    PreferenceScreenMixin, PreferenceSummaryProvider, PreferenceIconProvider {

    override val key: String
        get() = KEY

    override val title: Int
        get() = R.string.location_settings_title

    override val keywords: Int
        get() = R.string.keywords_location

    override val highlightMenuKey: Int
        get() = R.string.menu_key_location

    override fun getMetricsCategory() = SettingsEnums.LOCATION

    override fun getLaunchIntent(context: Context, metadata: PreferenceMetadata?): Intent? {
        return Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS)
    }

    override fun getSummary(context: Context): CharSequence? {
        var locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return if (locationManager.isLocationEnabled) {
            context.getString(R.string.location_settings_loading_app_permission_stats)
        } else {
            context.getString(R.string.location_settings_summary_location_off)
        }
    }

    override fun getIcon(context: Context) =
        when {
            isExpressiveTheme(context) -> R.drawable.ic_homepage_location
            else -> R.drawable.ic_settings_location_filled
        }

    override fun isFlagEnabled(context: Context) = Flags.catalystLocationSettings()

    override fun hasCompleteHierarchy() = false

    override fun fragmentClass(): Class<out Fragment>? = LocationSettings::class.java

    override fun getPreferenceHierarchy(context: Context, coroutineScope: CoroutineScope) =
        preferenceHierarchy(context) {
            +LocationMainSwitch()
            if (Flags.catalystLocationSettings()) +RecentLocationAccessScreen.KEY
        }

    companion object {
        const val KEY = "location_settings"
    }
}

private class LocationMainSwitch : BooleanValuePreference, MainSwitchPreferenceBinding {

    override val key: String
        get() = KEY

    override val title: Int
        get() = R.string.location_settings_primary_switch_title

    override val sensitivityLevel = SensitivityLevel.HIGH_SENSITIVITY

    override fun getWritePermit(context: Context, callingPid: Int, callingUid: Int) =
        ReadWritePermit.DISALLOW

    override fun storage(context: Context) = LocationStorage(context)

    companion object {
        const val KEY = "location_main_switch"
    }
}

private class LocationStorage(context: Context) : NoOpKeyedObservable<String>(), KeyValueStore {

    private val locationManager = context.getSystemService(LocationManager::class.java)

    override fun contains(key: String): Boolean = true

    override fun <T : Any> getValue(key: String, valueType: Class<T>): T? {
        return locationManager.isLocationEnabled as T
    }

    override fun <T : Any> setValue(key: String, valueType: Class<T>, value: T?) {
        // Not implemented. This is a very sensitive setting which needs to be thoughtfully handled
        // if ever exposed to set through this API.
    }
}
