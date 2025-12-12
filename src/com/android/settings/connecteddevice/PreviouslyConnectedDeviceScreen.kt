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

package com.android.settings.connecteddevice

import android.app.settings.SettingsEnums
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import androidx.fragment.app.Fragment
import androidx.preference.Preference
import com.android.settings.R
import com.android.settings.Settings.PreviouslyConnectedDeviceActivity
import com.android.settings.core.PreferenceScreenMixin
import com.android.settings.flags.Flags
import com.android.settings.overlay.FeatureFactory.Companion.featureFactory
import com.android.settings.utils.makeLaunchIntent
import com.android.settingslib.metadata.PreferenceAvailabilityProvider
import com.android.settingslib.metadata.PreferenceLifecycleContext
import com.android.settingslib.metadata.PreferenceLifecycleProvider
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.metadata.PreferenceSummaryProvider
import com.android.settingslib.metadata.ProvidePreferenceScreen
import com.android.settingslib.metadata.preferenceHierarchy
import kotlinx.coroutines.CoroutineScope

/**
 * This screen (fragment) displays previously connected devices. It is associated with the
 * "previously_connected_devices" PreferenceCategory in the XML, which is dynamically managed and
 * controlled by the {@link
 * com.android.settings.connecteddevice.PreviouslyConnectedDevicePreferenceController}.
 */
// LINT.IfChange
@ProvidePreferenceScreen(PreviouslyConnectedDeviceScreen.KEY)
open class PreviouslyConnectedDeviceScreen :
    PreferenceScreenMixin,
    PreferenceAvailabilityProvider,
    PreferenceSummaryProvider,
    PreferenceLifecycleProvider,
    DevicePreferenceCallback {

    private val bluetoothAdapter: BluetoothAdapter? by lazy { BluetoothAdapter.getDefaultAdapter() }

    override val key: String
        get() = KEY

    override val title: Int
        get() = R.string.previous_connected_see_all

    override val screenTitle: Int
        get() = R.string.connected_device_saved_title

    override val icon: Int
        get() = R.drawable.ic_chevron_right_24dp

    private var bluetoothStateReceiver: BroadcastReceiver? = null

    override fun isFlagEnabled(context: Context) = Flags.deeplinkConnectedDevices25q4()

    override fun getMetricsCategory() = SettingsEnums.PREVIOUSLY_CONNECTED_DEVICES

    override fun hasCompleteHierarchy() = false

    override fun fragmentClass(): Class<out Fragment>? =
        PreviouslyConnectedDeviceDashboardFragment::class.java

    override val highlightMenuKey: Int
        get() = R.string.menu_key_connected_devices

    override fun getPreferenceHierarchy(context: Context, coroutineScope: CoroutineScope) =
        preferenceHierarchy(context) {}

    override fun getLaunchIntent(context: Context, metadata: PreferenceMetadata?) =
        makeLaunchIntent(context, PreviouslyConnectedDeviceActivity::class.java, metadata?.key)

    override fun isAvailable(context: Context) =
        context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH) ||
            featureFactory.dockUpdaterFeatureProvider.getSavedDockUpdater(context, this) != null

    override fun onStart(context: PreferenceLifecycleContext) {
        super.onStart(context)
        if (isEntryPoint(context)) {
            val filter = IntentFilter().apply { addAction(BluetoothAdapter.ACTION_STATE_CHANGED) }
            val preferenceLifecycleContext = context
            bluetoothStateReceiver =
                object : BroadcastReceiver() {
                    override fun onReceive(context: Context?, intent: Intent?) {
                        preferenceLifecycleContext.notifyPreferenceChange(KEY)
                    }
                }
            context.registerReceiver(bluetoothStateReceiver, filter, Context.RECEIVER_EXPORTED)
        } else if (isContainer(context)) {
            if (bluetoothAdapter?.isEnabled == false) {
                bluetoothAdapter?.enable()
            }
        }
    }

    override fun onStop(context: PreferenceLifecycleContext) {
        super.onStop(context)
        if (isEntryPoint(context)) {
            bluetoothStateReceiver?.let {
                context.unregisterReceiver(it)
                bluetoothStateReceiver = null
            }
        }
    }

    override fun getSummary(context: Context): CharSequence? {
        return if (
            context.getSystemService(BluetoothManager::class.java)?.adapter?.isEnabled() == true
        )
            ""
        else context.getString(R.string.connected_device_see_all_summary)
    }

    override fun onDeviceAdded(preference: Preference?) {}

    override fun onDeviceRemoved(preference: Preference?) {}

    companion object {
        const val KEY = "previously_connected_devices_see_all"
    }
}
// LINT.ThenChange(PreviouslyConnectedDeviceDashboardFragment.java, PreviouslyConnectedDevicePreferenceController.java)
