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

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.PowerManager
import androidx.annotation.VisibleForTesting
import com.android.settingslib.datastore.AbstractKeyedDataObservable
import com.android.settingslib.datastore.DataChangeReason

/** A shared observable to monitor power save mode change. */
class PowerSaveModeObservable private constructor(private val context: Context) :
    AbstractKeyedDataObservable<String>() {

    private val broadcastReceiver =
        object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                notifyChange(DataChangeReason.UPDATE)
            }
        }

    override fun onFirstObserverAdded() {
        context.registerReceiver(
            broadcastReceiver,
            IntentFilter(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED),
        )
    }

    override fun onLastObserverRemoved() {
        context.unregisterReceiver(broadcastReceiver)
    }

    companion object {
        @Volatile private var instance: PowerSaveModeObservable? = null

        @JvmStatic
        fun get(context: Context): PowerSaveModeObservable =
            instance
                ?: synchronized(this) {
                    instance
                        ?: PowerSaveModeObservable(context.applicationContext).also {
                            instance = it
                        }
                }

        @VisibleForTesting
        fun resetInstance() {
            instance = null
        }
    }
}
