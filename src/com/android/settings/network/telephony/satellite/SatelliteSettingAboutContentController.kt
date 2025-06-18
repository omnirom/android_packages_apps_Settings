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

package com.android.settings.network.telephony.satellite

import android.content.Context
import android.telephony.TelephonyManager
import androidx.preference.PreferenceScreen
import com.android.settings.network.telephony.TelephonyBasePreferenceController
import com.android.settingslib.widget.TopIntroPreference
import com.android.settings.R;

/** A controller to show the introduction of satellite connectivity. */
class SatelliteSettingAboutContentController(context: Context, key: String) :
    TelephonyBasePreferenceController(context, key) {
    private lateinit var simOperatorName: String

    fun init(subId: Int) {
        mSubId = subId
        simOperatorName =
            mContext.getSystemService(TelephonyManager::class.java)?.getSimOperatorName(mSubId) ?: ""
    }

    override fun displayPreference(screen: PreferenceScreen?) {
        super.displayPreference(screen)
        val preference: TopIntroPreference? =
            screen?.findPreference(PREF_KEY_ABOUT_SATELLITE_CONNECTIVITY)
        preference?.title =
            mContext.getString(R.string.description_about_satellite_setting, simOperatorName)
    }

    override fun getAvailabilityStatus(subId: Int): Int {
        return AVAILABLE
    }

    companion object {
        const val PREF_KEY_ABOUT_SATELLITE_CONNECTIVITY = "key_about_satellite_connectivity";
    }
}