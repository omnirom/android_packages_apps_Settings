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

import android.annotation.SuppressLint
import android.content.Context
import android.telephony.SubscriptionManager
import android.util.Log
import com.android.settings.Utils
import com.android.settings.flags.Flags
import com.android.settings.network.SubscriptionUtil
import com.android.settings.wifi.utils.isAdminUser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class EnabledNetworkModeData(
    val isAvailable: Boolean = false,
    val summary: CharSequence? = null,
)

open class MobileNetworkData(
    val context: Context,
    val coroutineScope: CoroutineScope? = null,
    val subId: Int = SubscriptionManager.INVALID_SUBSCRIPTION_ID,
) {
    data class PhoneNumberData(
        val isAvailable: Boolean = false,
        val summary: CharSequence? = null,
        val storageValue: CharSequence? = summary,
    )

    val phoneNumberDataFlow = MutableStateFlow(PhoneNumberData())

    val enabledNetworkModeFlow = MutableStateFlow(EnabledNetworkModeData())
    val enabledNetworkModeEntriesBuilder =
        EnabledNetworkModePreferenceController.PreferenceEntriesBuilder(context, subId)

    init {
        refresh()
    }

    fun refresh() {
        coroutineScope?.launch {
            withContext(Dispatchers.Default) {
                phoneNumberDataFlow.value =
                    PhoneNumberData(
                        isAvailable = isMobileNetworkAvailable(),
                        summary = getPhoneNumber(),
                    )
                Log.d(TAG, "subId=$subId,phoneNumberData=${phoneNumberDataFlow.value}")

                enabledNetworkModeEntriesBuilder.refresh()
                enabledNetworkModeFlow.value =
                    EnabledNetworkModeData(
                        isAvailable = isEnabledNetworkModeAvailable,
                        summary = enabledNetworkModeEntriesBuilder.summary,
                    )
                Log.d(TAG, "subId=$subId,enabledNetworkMode=${enabledNetworkModeFlow.value}")
            }
        }
    }

    fun isMobileNetworkAvailable(): Boolean {
        val isAvailable =
            context.isAdminUser == true &&
                (Utils.isMobileDataCapable(context) || Utils.isVoiceCapable(context)) &&
                Flags.isDualSimOnboardingEnabled() &&
                SubscriptionManager.isValidSubscriptionId(subId)
        return isAvailable
    }

    @SuppressLint("MissingPermission")
    fun getPhoneNumber(): CharSequence {
        return context.subscriptionManager?.getActiveSubscriptionInfo(subId)?.let {
            SubscriptionUtil.getBidiFormattedPhoneNumber(context, it)
        } ?: ""
    }

    val isEnabledNetworkModeAvailable: Boolean =
        getNetworkModePreferenceType(context, subId) == NetworkModePreferenceType.EnabledNetworkMode

    companion object {
        private const val TAG = "MobileNetworkData"
    }
}
