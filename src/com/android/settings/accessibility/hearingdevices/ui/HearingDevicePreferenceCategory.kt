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
import android.content.pm.PackageManager
import androidx.preference.Preference
import com.android.settings.bluetooth.BluetoothDeviceUpdater
import com.android.settings.connecteddevice.DevicePreferenceCallback
import com.android.settingslib.metadata.PreferenceAvailabilityProvider
import com.android.settingslib.metadata.PreferenceCategory
import com.android.settingslib.metadata.PreferenceLifecycleContext
import com.android.settingslib.metadata.PreferenceLifecycleProvider
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.preference.PreferenceCategoryBinding

abstract class HearingDevicePreferenceCategory(key: String, title: Int) :
    PreferenceCategory(key, title),
    PreferenceCategoryBinding,
    PreferenceAvailabilityProvider,
    PreferenceLifecycleProvider,
    DevicePreferenceCallback {

    var deviceUpdater: BluetoothDeviceUpdater? = null
    var category: androidx.preference.PreferenceCategory? = null

    override fun createWidget(context: Context): androidx.preference.PreferenceCategory {
        return super.createWidget(context).apply { isVisible = false }
    }

    abstract fun createDeviceUpdater(context: Context): BluetoothDeviceUpdater?

    override fun bind(preference: Preference, metadata: PreferenceMetadata) {
        super.bind(preference, metadata)
        if (preference is androidx.preference.PreferenceCategory) {
            category = preference
        }
    }

    override fun onCreate(context: PreferenceLifecycleContext) {
        super.onCreate(context)
        if (isAvailable(context) && deviceUpdater == null) {
            deviceUpdater =
                createDeviceUpdater(context)?.apply {
                    setPrefContext(context)
                    forceUpdate()
                }
        }
    }

    override fun onStart(context: PreferenceLifecycleContext) {
        super.onStart(context)
        deviceUpdater?.apply {
            registerCallback()
            refreshPreference()
        }
    }

    override fun onStop(context: PreferenceLifecycleContext) {
        super.onStop(context)
        deviceUpdater?.apply { unregisterCallback() }
    }

    override fun isAvailable(context: Context): Boolean =
        context.packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH)

    override fun onDeviceAdded(preference: Preference) {
        category?.apply {
            if (preferenceCount == 0) {
                isVisible = true
            }
            addPreference(preference)
        }
    }

    override fun onDeviceRemoved(preference: Preference) {
        category?.apply {
            removePreference(preference)
            if (preferenceCount == 0) {
                isVisible = false
            }
        }
    }
}
