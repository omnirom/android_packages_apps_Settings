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

package com.android.settings.wifi

import android.Manifest
import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.location.LocationManager
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.preference.Preference
import androidx.preference.SwitchPreferenceCompat
import com.android.settings.R
import com.android.settings.utils.AnnotationSpan
import com.android.settings.wifi.utils.locationManager
import com.android.settings.wifi.utils.wifiManager
import com.android.settingslib.datastore.AbstractKeyedDataObservable
import com.android.settingslib.datastore.KeyValueStore
import com.android.settingslib.datastore.Permissions
import com.android.settingslib.metadata.PreferenceChangeReason
import com.android.settingslib.metadata.PreferenceLifecycleContext
import com.android.settingslib.metadata.PreferenceLifecycleProvider
import com.android.settingslib.metadata.PreferenceSummaryProvider
import com.android.settingslib.metadata.ReadWritePermit
import com.android.settingslib.metadata.SensitivityLevel
import com.android.settingslib.metadata.SwitchPreference
import com.android.settingslib.preference.SwitchPreferenceBinding

@Suppress("DEPRECATION")
class WifiWakeupSwitchPreference :
    SwitchPreference(KEY, R.string.wifi_wakeup),
    SwitchPreferenceBinding,
    PreferenceSummaryProvider,
    PreferenceLifecycleProvider {

    override val icon: Int
        get() = R.drawable.ic_auto_wifi

    override fun getSummary(context: Context): CharSequence? =
        when {
            context.locationManager?.isLocationEnabled == false -> getNoLocationSummary(context)
            else -> context.getText(R.string.wifi_wakeup_summary)
        }

    override fun onCreate(context: PreferenceLifecycleContext) {
        context.requirePreference<SwitchPreferenceCompat>(KEY).onPreferenceChangeListener =
            Preference.OnPreferenceChangeListener { _, _ ->
                !(shouldShowLocationSettings(context) || shouldShowWifiScanSettings(context))
            }
    }

    override fun onResume(context: PreferenceLifecycleContext) {
        context.notifyPreferenceChange(KEY)
    }

    override fun onActivityResult(
        context: PreferenceLifecycleContext,
        requestCode: Int,
        resultCode: Int,
        data: Intent?,
    ): Boolean {
        if (resultCode != Activity.RESULT_OK || requestCode != REQUEST_CODE) {
            return false
        }
        context.getKeyValueStore(KEY)?.setBoolean(KEY, true)
        context.notifyPreferenceChange(KEY)
        return true
    }

    override fun getReadPermissions(context: Context) =
        Permissions.allOf(Manifest.permission.ACCESS_WIFI_STATE)

    override fun getWritePermissions(context: Context) =
        Permissions.anyOf(Manifest.permission.NETWORK_SETTINGS)

    override fun getReadPermit(context: Context, callingPid: Int, callingUid: Int) =
        ReadWritePermit.ALLOW

    override fun getWritePermit(
        context: Context,
        value: Boolean?,
        callingPid: Int,
        callingUid: Int,
    ) =
        when {
            value == true &&
                (context.wifiManager?.isScanAlwaysAvailable != true ||
                    context.locationManager?.isLocationEnabled != true) -> ReadWritePermit.DISALLOW

            else -> ReadWritePermit.ALLOW
        }

    override val sensitivityLevel
        get() = SensitivityLevel.LOW_SENSITIVITY

    override fun storage(context: Context): KeyValueStore = Storage(context)

    @Suppress("UNCHECKED_CAST")
    private class Storage(private val context: Context) :
        AbstractKeyedDataObservable<String>(), KeyValueStore {

        private var broadcastReceiver: BroadcastReceiver? = null

        override fun contains(key: String) =
            key == KEY && context.wifiManager != null && context.locationManager != null

        override fun <T : Any> getValue(key: String, valueType: Class<T>): T? {
            return (context.wifiManager?.isAutoWakeupEnabled == true &&
                context.wifiManager?.isScanAlwaysAvailable == true &&
                context.locationManager?.isLocationEnabled == true)
                as T?
        }

        override fun <T : Any> setValue(key: String, valueType: Class<T>, value: T?) {
            val isEnabled = value as? Boolean ?: return
            if (context.wifiManager?.isScanAlwaysAvailable != true) {
                Log.w(TAG, "Failed to set value because isScanAlwaysAvailable is false!")
                return
            }
            if (context.locationManager?.isLocationEnabled != true) {
                Log.w(TAG, "Failed to set value because isLocationEnabled is false!")
                return
            }

            context.wifiManager?.isAutoWakeupEnabled = isEnabled
        }

        override fun onFirstObserverAdded() {
            broadcastReceiver =
                object : BroadcastReceiver() {
                    override fun onReceive(context: Context, intent: Intent) {
                        notifyChange(KEY, PreferenceChangeReason.VALUE)
                    }
                }
            val intentFilter = IntentFilter(LocationManager.MODE_CHANGED_ACTION)
            context.registerReceiver(broadcastReceiver, intentFilter)
        }

        override fun onLastObserverRemoved() {
            broadcastReceiver?.let { context.unregisterReceiver(it) }
        }
    }

    private fun getNoLocationSummary(context: Context): CharSequence? {
        val linkInfo = AnnotationSpan.LinkInfo("link", null)
        val locationText: CharSequence = context.getText(R.string.wifi_wakeup_summary_no_location)
        return AnnotationSpan.linkify(locationText, linkInfo)
    }

    private fun shouldShowLocationSettings(context: Context): Boolean {
        if (context.locationManager?.isLocationEnabled == true) {
            return false
        }
        Log.i(TAG, "Show Location settings")
        val intent =
            Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS).setPackage(context.packageName)
        context.startActivityForResult(KEY, intent, REQUEST_CODE, null as Bundle?)
        return true
    }

    private fun shouldShowWifiScanSettings(context: PreferenceLifecycleContext): Boolean {
        if (context.wifiManager?.isScanAlwaysAvailable == true) {
            return false
        }
        Log.i(TAG, "Show Wi-Fi scanning settings")
        context.fragmentManager?.let {
            val dialogFragment = WifiScanningRequiredFragment.newInstance()
            dialogFragment.setCallback(this, context, REQUEST_CODE)
            dialogFragment.show(it, TAG)
        }
        return true
    }

    companion object {
        const val TAG = "WifiWakeupSwitchPreference"
        const val KEY = "enable_wifi_wakeup"
        private const val REQUEST_CODE = 600
    }
}
