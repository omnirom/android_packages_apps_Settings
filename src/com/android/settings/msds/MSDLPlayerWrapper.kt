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
package com.android.settings.msds

import android.content.Context
import android.os.VibratorManager
import android.util.Log
import com.google.android.msdl.data.model.MSDLToken
import com.google.android.msdl.domain.InteractionProperties
import com.google.android.msdl.domain.MSDLPlayer
import com.google.android.msdl.logging.MSDLEvent

/**
 * A wrapper to ensure that a single instance of the [MSDLPlayer] is provided all across Settings
 */
object MSDLPlayerWrapper {

    private const val TAG = "MSDLPlayerWrapper"

    private lateinit var internalPlayer: MSDLPlayer

    /**
     * Create the singleton of the [MSDLPlayer].
     *
     * This function is only meant to be called once during initialization of the client app
     *
     * @param context The context in which the player is created. Used to access system services.
     */
    fun createPlayer(context: Context) {
        if (::internalPlayer.isInitialized) return

        val vibratorManager =
            context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
        internalPlayer = MSDLPlayer.createPlayer(vibratorManager.defaultVibrator)
    }

    @JvmOverloads
    fun playToken(token: MSDLToken, properties: InteractionProperties? = null) {
        if (!::internalPlayer.isInitialized) {
            Log.e(TAG, "Cannot play $token because the MSDLPlayer has not been created")
            return
        }

        internalPlayer.playToken(token, properties)
    }

    @JvmOverloads
    fun getHistory(): List<MSDLEvent> {
        if (!::internalPlayer.isInitialized) {
            Log.e(TAG, "The MSDLPlayer has not been created. Returning an empty list of events")
            return listOf()
        }

        return internalPlayer.getHistory()
    }
}
