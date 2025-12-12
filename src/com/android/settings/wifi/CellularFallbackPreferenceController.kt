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
package com.android.settings.wifi

import android.content.Context
import android.content.res.Resources
import android.provider.Settings
import android.telephony.SubscriptionManager
import android.net.ConnectivitySettingsManager
import android.net.ConnectivitySettingsManager.NETWORK_AVOID_BAD_WIFI_AVOID
import android.net.ConnectivitySettingsManager.NETWORK_AVOID_BAD_WIFI_IGNORE
import android.net.ConnectivitySettingsManager.NETWORK_AVOID_BAD_WIFI_PROMPT
import android.net.platform.flags.Flags
import android.util.Log
import com.android.settings.R
import com.android.settings.core.TogglePreferenceController

/**
 * CellularFallbackPreferenceController controls whether we should fall back to cellular when
 * wifi is bad.
 */
class CellularFallbackPreferenceController
@JvmOverloads
constructor(
    context: Context,
    key: String,
    private val getActiveDataSubscriptionId: () -> Int = {
        SubscriptionManager.getActiveDataSubscriptionId()
    },
    private val getResourcesForSubId: (Int) -> Resources = {
        SubscriptionManager.getResourcesForSubId(context, it)
    },
    private val getNetworkAvoidBadWifi: () -> Boolean = {
        ConnectivitySettingsManager.getNetworkAvoidBadWifi(
            context,
            getActiveDataSubscriptionId()
        )
    },
    private val shouldShowAvoidBadWifiToggle: () -> Boolean = {
        ConnectivitySettingsManager.shouldShowAvoidBadWifiToggle(
            context,
            getActiveDataSubscriptionId()
        )
    },
    private val setNetworkAvoidBadWifi: (Int) -> Unit = {
        if(Flags.avoidBadWifiFromCarrierConfig()) {
            try {
                ConnectivitySettingsManager.setNetworkAvoidBadWifi(
                    context,
                    getActiveDataSubscriptionId(),
                    it
                )
            } catch (e: Exception) {
                Log.w(TAG, "ConnectivitySettingsManager.setNetworkAvoidBadWifi error", e)
                ConnectivitySettingsManager.setNetworkAvoidBadWifi(context, it)
            }
        } else {
            ConnectivitySettingsManager.setNetworkAvoidBadWifi(context, it)
        }
    }
) : TogglePreferenceController(context, key) {

    override fun getAvailabilityStatus(): Int =
        if (Flags.avoidBadWifiFromCarrierConfig()) {
            if (shouldShowAvoidBadWifiToggle()) AVAILABLE else UNSUPPORTED_ON_DEVICE
        } else {
            if (avoidBadWifiConfig()) UNSUPPORTED_ON_DEVICE else AVAILABLE
        }

    override fun isChecked() = avoidBadWifiCurrentSettings()

    override fun setChecked(isChecked: Boolean): Boolean {
        // On: avoid bad wifi. Off: if flag on IGNORE else PROMPT.
        val settingValue = when {
          isChecked -> NETWORK_AVOID_BAD_WIFI_AVOID
          Flags.avoidBadWifiFromCarrierConfig() -> NETWORK_AVOID_BAD_WIFI_IGNORE
          else -> NETWORK_AVOID_BAD_WIFI_PROMPT
        }
        setNetworkAvoidBadWifi(settingValue)
        return true
    }

    override fun getSliceHighlightMenuRes(): Int = R.string.menu_key_network

    fun avoidBadWifiConfig(): Boolean {
        val activeDataSubscriptionId = getActiveDataSubscriptionId()
        // keep old behavior
        if (activeDataSubscriptionId == SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
            return true
        }
        val resources = getResourcesForSubId(activeDataSubscriptionId)
        return resources.getInteger(
            com.android.internal.R.integer.config_networkAvoidBadWifi
        ) == 1
    }

    fun avoidBadWifiCurrentSettings(): Boolean =
        if (Flags.avoidBadWifiFromCarrierConfig()) {
            getNetworkAvoidBadWifi()
        } else {
            "1" == Settings.Global.getString(
                mContext.getContentResolver(),
                Settings.Global.NETWORK_AVOID_BAD_WIFI
            )
        }
    companion object {
        const val TAG = "CellularFallbackPreferenceController"
    }
}