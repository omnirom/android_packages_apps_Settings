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

package com.android.settings.network

import android.app.Activity
import android.app.settings.SettingsEnums.ACTION_AIRPLANE_TOGGLE
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.UserHandle
import android.os.UserManager
import android.provider.Settings
import android.telephony.TelephonyManager
import androidx.annotation.DrawableRes
import androidx.preference.Preference
import com.android.settings.AirplaneModeEnabler
import com.android.settings.R
import com.android.settings.Utils
import com.android.settings.contract.KEY_AIRPLANE_MODE
import com.android.settings.metrics.PreferenceActionMetricsProvider
import com.android.settings.network.SatelliteRepository.Companion.isSatelliteOn
import com.android.settings.restriction.PreferenceRestrictionMixin
import com.android.settingslib.RestrictedSwitchPreference
import com.android.settingslib.datastore.KeyValueStore
import com.android.settingslib.datastore.KeyValueStoreDelegate
import com.android.settingslib.datastore.SettingsGlobalStore
import com.android.settingslib.metadata.PreferenceAvailabilityProvider
import com.android.settingslib.metadata.PreferenceLifecycleContext
import com.android.settingslib.metadata.PreferenceLifecycleProvider
import com.android.settingslib.metadata.ReadWritePermit
import com.android.settingslib.metadata.SensitivityLevel
import com.android.settingslib.metadata.SwitchPreference

// LINT.IfChange
class AirplaneModePreference :
    SwitchPreference(KEY, R.string.airplane_mode),
    PreferenceActionMetricsProvider,
    PreferenceAvailabilityProvider,
    PreferenceLifecycleProvider,
    PreferenceRestrictionMixin {

    override val icon: Int
        @DrawableRes get() = R.drawable.ic_airplanemode_active

    override fun tags(context: Context) = arrayOf(KEY_AIRPLANE_MODE)

    override fun isAvailable(context: Context) =
        (context.resources.getBoolean(R.bool.config_show_toggle_airplane) &&
            !context.packageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK))

    override fun isEnabled(context: Context) = super<PreferenceRestrictionMixin>.isEnabled(context)

    override val restrictionKeys
        get() = arrayOf(UserManager.DISALLOW_AIRPLANE_MODE)

    override fun getReadPermissions(context: Context) = SettingsGlobalStore.getReadPermissions()

    override fun getWritePermissions(context: Context) = SettingsGlobalStore.getWritePermissions()

    override fun getReadPermit(context: Context, callingPid: Int, callingUid: Int) =
        ReadWritePermit.ALLOW

    override fun getWritePermit(context: Context, callingPid: Int, callingUid: Int) =
        when {
            isSatelliteOn(context) || isInEcmMode(context) -> ReadWritePermit.DISALLOW
            else -> ReadWritePermit.ALLOW
        }

    override val sensitivityLevel
        get() = SensitivityLevel.HIGH_SENSITIVITY

    override val preferenceActionMetrics: Int
        get() = ACTION_AIRPLANE_TOGGLE

    override fun storage(context: Context) = createDataStore(context)

    override fun onCreate(context: PreferenceLifecycleContext) {
        context.requirePreference<RestrictedSwitchPreference>(KEY).onPreferenceChangeListener =
            Preference.OnPreferenceChangeListener { _: Preference, _: Any ->
                if (isInEcmMode(context)) {
                    showEcmDialog(context)
                    return@OnPreferenceChangeListener false
                }
                if (isSatelliteOn(context)) {
                    showSatelliteDialog(context)
                    return@OnPreferenceChangeListener false
                }
                return@OnPreferenceChangeListener true
            }
    }

    override fun onActivityResult(
        context: PreferenceLifecycleContext,
        requestCode: Int,
        resultCode: Int,
        data: Intent?,
    ): Boolean {
        if (requestCode == REQUEST_CODE_EXIT_ECM && resultCode == Activity.RESULT_OK) {
            context.getKeyValueStore(KEY)?.setBoolean(KEY, true)
        }
        return true
    }

    private fun isInEcmMode(context: Context) =
        AirplaneModeEnabler.isInEcmMode(
            context,
            context.getSystemService(TelephonyManager::class.java),
        )

    private fun showEcmDialog(context: PreferenceLifecycleContext) {
        val intent =
            Intent(TelephonyManager.ACTION_SHOW_NOTICE_ECM_BLOCK_OTHERS, null)
                .setPackage(Utils.PHONE_PACKAGE_NAME)
        context.startActivityForResult(intent, REQUEST_CODE_EXIT_ECM, null)
    }

    private fun showSatelliteDialog(context: PreferenceLifecycleContext) {
        val intent =
            Intent(context, SatelliteWarningDialogActivity::class.java)
                .putExtra(
                    SatelliteWarningDialogActivity.EXTRA_TYPE_OF_SATELLITE_WARNING_DIALOG,
                    SatelliteWarningDialogActivity.TYPE_IS_AIRPLANE_MODE,
                )
        context.startActivity(intent)
    }

    companion object {
        const val KEY = Settings.Global.AIRPLANE_MODE_ON
        const val DEFAULT_VALUE = false
        const val REQUEST_CODE_EXIT_ECM = 1

        fun createDataStore(context: Context): KeyValueStore = AirplaneModeStorage(context)

        @Suppress("UNCHECKED_CAST")
        private class AirplaneModeStorage(private val context: Context) : KeyValueStoreDelegate {

            private val settingsStore =
                SettingsGlobalStore.get(context).apply { setDefaultValue(KEY, DEFAULT_VALUE) }

            override val keyValueStoreDelegate
                get() = settingsStore

            override fun <T : Any> setValue(key: String, valueType: Class<T>, value: T?) {
                settingsStore.setValue(key, valueType, value)

                val intent = Intent(Intent.ACTION_AIRPLANE_MODE_CHANGED)
                intent.putExtra("state", getBoolean(KEY)!!)
                context.sendBroadcastAsUser(intent, UserHandle.ALL)
            }
        }
    }
}
// LINT.ThenChange(AirplaneModePreferenceController.java)
