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

package com.android.settings.display.darkmode

import android.app.settings.SettingsEnums
import android.content.Context
import android.content.Intent
import android.location.LocationManager
import androidx.preference.Preference
import com.android.settings.R
import com.android.settings.Settings.LocationSettingsActivity
import com.android.settings.overlay.FeatureFactory.Companion.featureFactory
import com.android.settingslib.metadata.PreferenceAvailabilityProvider
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.preference.PreferenceBinding
import com.android.settingslib.widget.BannerMessagePreference

// LINT.IfChange
class TwilightLocationPreference :
    PreferenceMetadata, PreferenceBinding, PreferenceAvailabilityProvider {

    override val key: String
        get() = KEY

    override val title: Int
        get() = R.string.twilight_mode_location_off_dialog_message

    override val indexable
        get() = false

    override fun isAvailable(context: Context): Boolean {
        val locationManager =
            context.getSystemService<LocationManager?>(LocationManager::class.java)
        return locationManager?.isLocationEnabled == false
    }

    override fun createWidget(context: Context) = BannerMessagePreference(context)

    override fun bind(preference: Preference, metadata: PreferenceMetadata) {
        super.bind(preference, metadata)
        preference as BannerMessagePreference
        preference.apply {
            setPositiveButtonText(R.string.twilight_mode_launch_location)
            setPositiveButtonOnClickListener { v ->
                featureFactory.metricsFeatureProvider.logClickedPreference(
                    this,
                    SettingsEnums.DARK_UI_SETTINGS,
                )

                val intent = Intent()
                intent.setClass(context, LocationSettingsActivity::class.java)
                context.startActivity(intent)
            }
        }
    }

    companion object {
        const val KEY = "dark_ui_location_off"
    }
}
// LINT.ThenChange(../TwilightLocationPreferenceController.java)
