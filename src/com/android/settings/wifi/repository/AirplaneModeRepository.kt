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

package com.android.settings.wifi.repository

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.provider.Settings
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn

class AirplaneModeRepository(private val context: Context) {
    /**
     * Flow for airplane mode state.
     *
     * @return true if airplane mode is enabled.
     */
    fun isAirplaneModeFlow(): Flow<Boolean> =
        callbackFlow {
                val isEnabled = getAirplaneModeState()
                trySend(isEnabled)

                val receiver =
                    object : android.content.BroadcastReceiver() {
                        override fun onReceive(context: Context, intent: Intent) {
                            if (intent.action == Intent.ACTION_AIRPLANE_MODE_CHANGED) {
                                val isApmOn = intent.getBooleanExtra("state", false)
                                Log.d(TAG, "isApmOn=$isApmOn")
                                trySend(isApmOn)
                            }
                        }
                    }
                val intentFilter = IntentFilter(Intent.ACTION_AIRPLANE_MODE_CHANGED)
                context.registerReceiver(receiver, intentFilter)

                awaitClose { context.unregisterReceiver(receiver) }
            }
            .distinctUntilChanged()
            .conflate()
            .flowOn(Dispatchers.Default)

    /**
     * Set the airplane mode state.
     *
     * @param enable true to enable airplane mode, false to disable.
     */
    fun setAirplaneMode(enable: Boolean) {
        Settings.Global.putInt(
            context.contentResolver,
            Settings.Global.AIRPLANE_MODE_ON,
            if (enable) 1 else 0,
        )

        val intent = Intent(Intent.ACTION_AIRPLANE_MODE_CHANGED)
        intent.putExtra("state", enable)
        context.sendBroadcast(intent)
    }

    private fun getAirplaneModeState(): Boolean {
        return Settings.Global.getInt(
            context.contentResolver,
            Settings.Global.AIRPLANE_MODE_ON,
            0,
        ) != 0
    }

    private companion object {
        private const val TAG = "AirplaneModeRepository"
    }
}
