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

package com.android.settings.deviceinfo

import android.content.Context
import android.os.Build
import android.provider.Settings.Global.DEVICE_NAME
import android.util.Log
import androidx.fragment.app.Fragment
import androidx.preference.Preference
import com.android.settings.R
import com.android.settings.deviceinfo.aboutphone.MyDeviceInfoFragment
import com.android.settings.widget.ValidatedEditTextPreference
import com.android.settings.wifi.tether.WifiDeviceNameTextValidator
import com.android.settingslib.datastore.HandlerExecutor
import com.android.settingslib.datastore.KeyedObserver
import com.android.settingslib.datastore.SettingsGlobalStore
import com.android.settingslib.metadata.PreferenceAvailabilityProvider
import com.android.settingslib.metadata.PreferenceLifecycleContext
import com.android.settingslib.metadata.PreferenceLifecycleProvider
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.metadata.PreferenceSummaryProvider
import com.android.settingslib.preference.PreferenceBinding

// LINT.IfChange
class DeviceNamePreference(val context: Context) :
    PreferenceMetadata,
    PreferenceBinding,
    PreferenceAvailabilityProvider,
    PreferenceSummaryProvider,
    PreferenceLifecycleProvider,
    Preference.OnPreferenceChangeListener {
    private lateinit var lifeCycleContext: PreferenceLifecycleContext

    private var keyedObserver: KeyedObserver<String>? = null

    override val key: String
        get() = KEY

    override val title: Int
        get() = R.string.my_device_info_device_name_preference_title

    override fun isAvailable(context: Context) =
        context.resources.getBoolean(R.bool.config_show_device_name)

    override fun getSummary(context: Context): CharSequence? =
        SettingsGlobalStore.get(context).getString(DEVICE_NAME) ?: Build.MODEL

    override fun createWidget(context: Context) = ValidatedEditTextPreference(context)

    override fun bind(preference: Preference, metadata: PreferenceMetadata) {
        super.bind(preference, metadata)
        (preference as ValidatedEditTextPreference).apply {
            isPersistent = true
            isCopyingEnabled = true
            text = summary.toString()
            onPreferenceChangeListener = this@DeviceNamePreference
            setValidator { WifiDeviceNameTextValidator().isTextValid(it) }
        }
    }

    override fun onPreferenceChange(preference: Preference, newValue: Any?): Boolean {
        val fragment: MyDeviceInfoFragment? =
            lifeCycleContext.findFragment(MY_DEVICE_FRAGMENT_NAME) as MyDeviceInfoFragment?
        Log.d(
            TAG,
            "DeviceNamePreference fragment is " +
                fragment?.javaClass?.simpleName +
                " and new name is $newValue",
        )
        fragment?.showDeviceNameWarningDialog(newValue as String)
        return true
    }

    override fun onCreate(context: PreferenceLifecycleContext) {
        lifeCycleContext = context
    }

    override fun onStart(context: PreferenceLifecycleContext) {
        val observer = KeyedObserver<String> { _, _ -> context.notifyPreferenceChange(KEY) }
        keyedObserver = observer
        SettingsGlobalStore.get(context).addObserver(DEVICE_NAME, observer, HandlerExecutor.main)
    }

    override fun onStop(context: PreferenceLifecycleContext) {
        keyedObserver?.let {
            SettingsGlobalStore.get(context).removeObserver(DEVICE_NAME, it)
            keyedObserver = null
        }
    }

    private fun PreferenceLifecycleContext.findFragment(targetFragment: String): Fragment? =
        fragmentManager.fragments.find { it -> it.javaClass.simpleName == targetFragment }

    companion object {
        const val KEY = "device_name"
        private val MY_DEVICE_FRAGMENT_NAME: String = MyDeviceInfoFragment::class.java.simpleName
        private val TAG = DeviceNamePreference::class.java.simpleName
    }
}
// LINT.ThenChange(DeviceNamePreferenceController.java)
