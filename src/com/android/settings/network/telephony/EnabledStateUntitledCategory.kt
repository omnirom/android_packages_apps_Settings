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

package com.android.settings.network.telephony

import android.content.Context
import com.android.settings.network.SubscriptionsChangeListener
import com.android.settingslib.metadata.PreferenceAvailabilityProvider
import com.android.settingslib.metadata.PreferenceGroup
import com.android.settingslib.metadata.PreferenceLifecycleContext
import com.android.settingslib.metadata.PreferenceLifecycleProvider
import com.android.settingslib.preference.PreferenceBinding
import com.android.settingslib.widget.UntitledPreferenceCategory

// LINT.IfChange
class EnabledStateUntitledCategory(val subId: Int) :
    PreferenceGroup,
    PreferenceBinding,
    PreferenceAvailabilityProvider,
    PreferenceLifecycleProvider {
    var subscriptionsChangeListener: SubscriptionsChangeListener? = null

    override val key: String
        get() = KEY

    override fun createWidget(context: Context) = UntitledPreferenceCategory(context)

    override fun isAvailable(context: Context): Boolean =
        context.subscriptionManager?.isActiveSubscriptionId(subId) == true

    override fun onStart(context: PreferenceLifecycleContext) {
        val listenerClient =
            object : SubscriptionsChangeListener.SubscriptionsChangeListenerClient {
                override fun onAirplaneModeChanged(airplaneModeEnabled: Boolean) {}

                override fun onSubscriptionsChanged() = context.notifyPreferenceChange(KEY)
            }

        subscriptionsChangeListener = SubscriptionsChangeListener(context, listenerClient)
        subscriptionsChangeListener?.start()
    }

    override fun onStop(context: PreferenceLifecycleContext) {
        subscriptionsChangeListener?.let {
            it.stop()
            subscriptionsChangeListener = null
        }
    }

    companion object {
        const val KEY = "enabled_state_container"
    }
}
// LINT.ThenChange(DisabledSubscriptionController.java)
