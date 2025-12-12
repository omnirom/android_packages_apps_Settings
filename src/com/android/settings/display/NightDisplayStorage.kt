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
package com.android.settings.display

import android.content.Context
import android.hardware.display.ColorDisplayManager
import android.hardware.display.NightDisplayListener
import com.android.settingslib.datastore.AbstractKeyedDataObservable
import com.android.settingslib.datastore.KeyValueStore
import com.android.settingslib.metadata.PreferenceChangeReason

/** Abstract storage for night display settings. */
@Suppress("UNCHECKED_CAST")
class NightDisplayStorage(private val context: Context) :
    AbstractKeyedDataObservable<String>(), KeyValueStore, NightDisplayListener.Callback {

    val nightDisplayListener: NightDisplayListener = NightDisplayListener(context)
    private val colorDisplayManager: ColorDisplayManager? =
        context.getSystemService(ColorDisplayManager::class.java)

    override fun contains(key: String) = (key == NightDisplayScreen.KEY)

    override fun <T : Any> getValue(key: String, valueType: Class<T>) =
        (colorDisplayManager?.isNightDisplayActivated == true) as T

    override fun <T : Any> setValue(key: String, valueType: Class<T>, value: T?) {
        colorDisplayManager?.isNightDisplayActivated = value as Boolean
    }

    override fun onActivated(activated: Boolean) {
        notifyChange(NightDisplayScreen.KEY, PreferenceChangeReason.STATE)
        super.onActivated(activated)
    }

    override fun onFirstObserverAdded() {
        nightDisplayListener.setCallback(this)
    }

    override fun onLastObserverRemoved() {
        nightDisplayListener.setCallback(null)
    }
}
