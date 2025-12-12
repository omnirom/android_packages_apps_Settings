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

package com.android.settings.accessibility.hearingdevices.ui

import android.content.Context
import android.provider.Settings
import android.telephony.TelephonyManager
import android.text.TextUtils
import androidx.fragment.app.FragmentManager
import androidx.preference.Preference
import com.android.settings.R
import com.android.settings.accessibility.HearingAidCompatibilityPreferenceController.HacDisclaimerDialog
import com.android.settings.accessibility.hearingdevices.data.HearingAidCompatibilityDataStore
import com.android.settingslib.datastore.KeyValueStore
import com.android.settingslib.datastore.SettingsSystemStore
import com.android.settingslib.metadata.PreferenceAvailabilityProvider
import com.android.settingslib.metadata.PreferenceLifecycleContext
import com.android.settingslib.metadata.PreferenceLifecycleProvider
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.metadata.ReadWritePermit
import com.android.settingslib.metadata.SensitivityLevel
import com.android.settingslib.metadata.SwitchPreference
import com.android.settingslib.preference.SwitchPreferenceBinding

class HearingAidCompatibilitySwitchPreference(private val context: Context) :
    SwitchPreference(
        KEY,
        R.string.accessibility_hac_mode_title,
        R.string.accessibility_hac_mode_summary,
    ),
    SwitchPreferenceBinding,
    PreferenceAvailabilityProvider,
    Preference.OnPreferenceChangeListener,
    PreferenceLifecycleProvider {
    private var telephonyManager: TelephonyManager =
        context.getSystemService(TelephonyManager::class.java)!!
    private lateinit var fragmentManager: FragmentManager

    override fun onCreate(context: PreferenceLifecycleContext) {
        super.onCreate(context)
        fragmentManager = context.fragmentManager
    }

    override fun storage(context: Context): KeyValueStore =
        HearingAidCompatibilityDataStore(context)

    override fun getReadPermissions(context: Context) = SettingsSystemStore.getReadPermissions()

    override fun getWritePermissions(context: Context) = SettingsSystemStore.getWritePermissions()

    override fun getReadPermit(context: Context, callingPid: Int, callingUid: Int) =
        ReadWritePermit.ALLOW

    override fun getWritePermit(
        context: Context,
        value: Boolean?,
        callingPid: Int,
        callingUid: Int,
    ) = ReadWritePermit.ALLOW

    override val sensitivityLevel
        get() = SensitivityLevel.NO_SENSITIVITY

    override fun bind(preference: Preference, metadata: PreferenceMetadata) {
        super.bind(preference, metadata)
        preference.onPreferenceChangeListener = this
    }

    override fun isAvailable(context: Context): Boolean =
        telephonyManager.isHearingAidCompatibilitySupported()

    override fun onPreferenceChange(preference: Preference, newValue: Any?): Boolean {
        val isChecked = newValue as Boolean
        if (isChecked && shouldShowDisclaimer()) {
            HacDisclaimerDialog.newInstance().show(fragmentManager, KEY)
        }
        return true
    }

    private fun shouldShowDisclaimer(): Boolean {
        return !TextUtils.isEmpty(context.getText(R.string.hac_disclaimer_message))
    }

    companion object {
        const val KEY = Settings.System.HEARING_AID_COMPATIBILITY
    }
}
