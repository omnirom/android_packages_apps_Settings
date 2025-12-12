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

package com.android.settings.network.tether

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothPan
import android.bluetooth.BluetoothProfile
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.TetheringManager
import android.os.Handler
import android.os.Looper
import com.android.settings.R
import com.android.settings.datausage.DataSaverBackend
import com.android.settingslib.datastore.AbstractKeyedDataObservable
import com.android.settingslib.datastore.KeyValueStore
import com.android.settingslib.datastore.Permissions
import com.android.settingslib.metadata.PreferenceAvailabilityProvider
import com.android.settingslib.metadata.PreferenceChangeReason
import com.android.settingslib.metadata.ReadWritePermit
import com.android.settingslib.metadata.SensitivityLevel
import com.android.settingslib.metadata.SwitchPreference
import java.util.concurrent.atomic.AtomicReference

// LINT.IfChange
@Suppress("DEPRECATION")
class BluetoothTetherSwitchPreference(
    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
) : SwitchPreference(KEY, R.string.bluetooth_tether_checkbox_text), PreferenceAvailabilityProvider {

    override val summary: Int
        get() = R.string.bluetooth_tethering_subtext

    override val keywords: Int
        get() = R.string.keywords_hotspot_tethering

    override fun storage(context: Context): KeyValueStore =
        BluetoothTetherStore(context, bluetoothAdapter)

    override fun isAvailable(context: Context): Boolean {
        bluetoothAdapter ?: return false
        val tetheringManager = context.getSystemService(TetheringManager::class.java)
        val bluetoothRegexs = tetheringManager?.tetherableBluetoothRegexs
        return bluetoothRegexs?.isNotEmpty() == true
    }

    override fun isEnabled(context: Context): Boolean {
        bluetoothAdapter ?: return false
        val btState = bluetoothAdapter.state
        /* TODO: when bluetooth is off, btstate will be `state_turning_on` -> `state_off` ->
        `state_turning_on` -> `state_on`, causing preference enable status incorrect. */
        when (btState) {
            BluetoothAdapter.STATE_TURNING_OFF,
            BluetoothAdapter.STATE_TURNING_ON -> return false
            else -> {}
        }
        val dataSaverBackend = DataSaverBackend(context)
        return !dataSaverBackend.isDataSaverEnabled
    }

    override fun getReadPermissions(context: Context) =
        Permissions.allOf(Manifest.permission.BLUETOOTH_CONNECT)

    override fun getReadPermit(context: Context, callingPid: Int, callingUid: Int) =
        ReadWritePermit.ALLOW

    override fun getWritePermissions(context: Context) =
        Permissions.allOf(
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.TETHER_PRIVILEGED,
        )

    override fun getWritePermit(context: Context, callingPid: Int, callingUid: Int) =
        ReadWritePermit.ALLOW

    override val sensitivityLevel: Int
        get() = SensitivityLevel.LOW_SENSITIVITY

    @Suppress("UNCHECKED_CAST")
    private class BluetoothTetherStore(
        private val context: Context,
        private val adapter: BluetoothAdapter?,
    ) : AbstractKeyedDataObservable<String>(), KeyValueStore {

        private val bluetoothPan = AtomicReference<BluetoothPan>()
        private var tetherChangeReceiver: TetherChangeReceiver? = null

        override fun contains(key: String) = key == KEY

        override fun onFirstObserverAdded() {
            if (bluetoothPan.get() == null) {
                val profileServiceListener: BluetoothProfile.ServiceListener =
                    object : BluetoothProfile.ServiceListener {
                        override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {
                            if (bluetoothPan.get() == null) {
                                bluetoothPan.set(proxy as BluetoothPan)
                                notifyChange(KEY, PreferenceChangeReason.VALUE)
                            }
                        }

                        override fun onServiceDisconnected(profile: Int) {
                            /* Do nothing */
                        }
                    }
                adapter?.getProfileProxy(
                    context.applicationContext,
                    profileServiceListener,
                    BluetoothProfile.PAN,
                )
            }
            registerTetherChangeReceiver()
        }

        override fun onLastObserverRemoved() {
            unregisterTetherChangeReceiver()
            bluetoothPan.getAndSet(null)?.let {
                adapter?.closeProfileProxy(BluetoothProfile.PAN, it)
            }
        }

        override fun <T : Any> getValue(key: String, valueType: Class<T>): T? =
            (bluetoothPan.get()?.isTetheringOn == true) as T

        override fun <T : Any> setValue(key: String, valueType: Class<T>, value: T?) {
            if (value !is Boolean || adapter == null) return
            val connectivityManager =
                context.getSystemService(ConnectivityManager::class.java) ?: return
            if (value) {
                val handler by lazy { Handler(Looper.getMainLooper()) }
                val startTetheringCallback = OnStartTetheringCallback()
                fun startTethering() {
                    connectivityManager.startTethering(
                        ConnectivityManager.TETHERING_BLUETOOTH,
                        true,
                        startTetheringCallback,
                        handler,
                    )
                }

                if (adapter.state == BluetoothAdapter.STATE_OFF) {
                    // Turn on Bluetooth first.
                    adapter.enable()
                    val filter = IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
                    val bluetoothStateReceiver =
                        object : BroadcastReceiver() {
                            override fun onReceive(context: Context, intent: Intent) {
                                if (
                                    intent.getIntExtra(
                                        BluetoothAdapter.EXTRA_STATE,
                                        BluetoothAdapter.ERROR,
                                    ) == BluetoothAdapter.STATE_ON
                                ) {
                                    startTethering()
                                    context.unregisterReceiver(this)
                                }
                            }
                        }
                    val intent = context.registerReceiver(bluetoothStateReceiver, filter)
                    if (intent != null) bluetoothStateReceiver.onReceive(context, intent)
                } else {
                    startTethering()
                }
            } else {
                connectivityManager.stopTethering(ConnectivityManager.TETHERING_BLUETOOTH)
            }
        }

        private fun registerTetherChangeReceiver() {
            tetherChangeReceiver = TetherChangeReceiver()
            var filter = IntentFilter(TetheringManager.ACTION_TETHER_STATE_CHANGED)
            val intent = context.registerReceiver(tetherChangeReceiver, filter)

            filter =
                IntentFilter().apply {
                    addAction(Intent.ACTION_MEDIA_SHARED)
                    addAction(Intent.ACTION_MEDIA_UNSHARED)
                    addDataScheme("file")
                }
            context.registerReceiver(tetherChangeReceiver, filter)

            filter =
                IntentFilter().apply {
                    addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
                    addAction(BluetoothPan.ACTION_TETHERING_STATE_CHANGED)
                }
            context.registerReceiver(tetherChangeReceiver, filter)

            if (intent != null) tetherChangeReceiver!!.onReceive(context, intent)
        }

        private fun unregisterTetherChangeReceiver() {
            tetherChangeReceiver?.let {
                context.unregisterReceiver(it)
                tetherChangeReceiver = null
            }
        }

        private inner class OnStartTetheringCallback :
            ConnectivityManager.OnStartTetheringCallback() {
            override fun onTetheringStarted() {
                notifyChange(KEY, PreferenceChangeReason.VALUE)
            }

            override fun onTetheringFailed() {
                notifyChange(KEY, PreferenceChangeReason.VALUE)
            }
        }

        private inner class TetherChangeReceiver : BroadcastReceiver() {
            override fun onReceive(content: Context, intent: Intent) {
                when (intent.action) {
                    TetheringManager.ACTION_TETHER_STATE_CHANGED,
                    Intent.ACTION_MEDIA_SHARED,
                    Intent.ACTION_MEDIA_UNSHARED,
                    BluetoothAdapter.ACTION_STATE_CHANGED,
                    BluetoothPan.ACTION_TETHERING_STATE_CHANGED ->
                        notifyChange(KEY, PreferenceChangeReason.STATE)
                }
            }
        }
    }

    companion object {
        const val KEY = "enable_bluetooth_tethering"
    }
}
// LINT.ThenChange(TetherSettings.java)
