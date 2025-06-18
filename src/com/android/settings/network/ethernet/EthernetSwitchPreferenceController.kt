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

package com.android.settings.network.ethernet

import android.content.Context
import android.net.EthernetManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.preference.Preference
import androidx.preference.PreferenceScreen
import com.android.settings.connectivity.Flags
import com.android.settingslib.RestrictedSwitchPreference
import com.android.settingslib.core.AbstractPreferenceController
import com.google.common.annotations.VisibleForTesting
import java.util.concurrent.Executor

class EthernetSwitchPreferenceController(context: Context, private val lifecycle: Lifecycle) :
    AbstractPreferenceController(context),
    LifecycleEventObserver,
    EthernetTracker.EthernetInterfaceTrackerListener {

    private val ethernetManager: EthernetManager? =
        context.getSystemService(EthernetManager::class.java)
    private var preference: RestrictedSwitchPreference? = null
    private val executor = ContextCompat.getMainExecutor(context)
    private val ethernetTracker =
        EthernetTrackerImpl.getInstance(context)

    init {
        lifecycle.addObserver(this)
    }

    override fun getPreferenceKey(): String {
        return KEY
    }

    override fun isAvailable(): Boolean {
        return (Flags.ethernetSettings() && ethernetTracker.availableInterfaces.size > 0)
    }

    override fun displayPreference(screen: PreferenceScreen) {
        super.displayPreference(screen)
        preference = screen.findPreference(KEY)
        preference?.setOnPreferenceChangeListener(this::onPreferenceChange)
    }

    override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
        when (event) {
            Lifecycle.Event.ON_START -> {
                ethernetManager?.addEthernetStateListener(executor, this::onEthernetStateChanged)
                ethernetTracker.registerInterfaceListener(this)
            }

            Lifecycle.Event.ON_STOP -> {
                ethernetManager?.removeEthernetStateListener(this::onEthernetStateChanged)
                ethernetTracker.unregisterInterfaceListener(this)
            }

            else -> {}
        }
    }

    fun onPreferenceChange(preference: Preference, newValue: Any): Boolean {
        val isChecked = newValue as Boolean
        ethernetManager?.setEthernetEnabled(isChecked)
        return true
    }

    @VisibleForTesting
    fun onEthernetStateChanged(state: Int) {
        preference?.setChecked(state == EthernetManager.ETHERNET_STATE_ENABLED)
    }

    override fun onInterfaceListChanged(ethernetInterfaces: List<EthernetInterface>) {
        preference?.setVisible(ethernetInterfaces.size > 0)
    }

    companion object {
        private val KEY = "main_toggle_ethernet"
    }
}
