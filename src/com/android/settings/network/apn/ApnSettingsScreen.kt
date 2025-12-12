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

package com.android.settings.network.apn

import android.app.settings.SettingsEnums
import android.content.Context
import android.os.Bundle
import android.os.PersistableBundle
import android.os.UserManager
import android.telephony.CarrierConfigManager
import android.telephony.SubscriptionManager.INVALID_SUBSCRIPTION_ID
import androidx.fragment.app.Fragment
import com.android.settings.R
import com.android.settings.Settings.ApnSettingsActivity
import com.android.settings.core.PreferenceScreenMixin
import com.android.settings.flags.Flags
import com.android.settings.network.CarrierConfigCache
import com.android.settings.network.SubscriptionUtil
import com.android.settings.network.telephony.MobileNetworkUtils
import com.android.settings.restriction.PreferenceRestrictionMixin
import com.android.settings.utils.makeLaunchIntent
import com.android.settingslib.RestrictedPreference
import com.android.settingslib.metadata.PreferenceAvailabilityProvider
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.metadata.ProvidePreferenceScreen
import com.android.settingslib.metadata.preferenceHierarchy
import com.android.settingslib.preference.PreferenceBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.map

// LINT.IfChange
@ProvidePreferenceScreen(ApnSettingsScreen.KEY, parameterized = true)
open class ApnSettingsScreen(override val arguments: Bundle) :
    PreferenceScreenMixin,
    PreferenceRestrictionMixin,
    PreferenceAvailabilityProvider,
    PreferenceBinding {
    private val subId = arguments.getInt(ApnSettings.SUB_ID, INVALID_SUBSCRIPTION_ID)

    override val key: String
        get() = KEY

    override val title: Int
        get() = R.string.mobile_network_apn_title

    override val screenTitle: Int
        get() = R.string.apn_settings

    override val keywords: Int
        get() = R.string.keywords_access_point_names

    override val restrictionKeys
        get() = arrayOf(UserManager.DISALLOW_CONFIG_MOBILE_NETWORKS)

    // TODO (b/441290203) - migrate ApnPreferenceController.updateState to catalyst
    override fun isEnabled(context: Context): Boolean =
        super<PreferenceRestrictionMixin>.isEnabled(context)

    override fun isFlagEnabled(context: Context) = Flags.deeplinkNetworkAndInternet25q4()

    override fun getMetricsCategory() = SettingsEnums.APN

    override fun hasCompleteHierarchy() = false

    override fun fragmentClass(): Class<out Fragment>? = ApnSettings::class.java

    override val highlightMenuKey: Int
        get() = R.string.menu_key_network

    override fun getPreferenceHierarchy(context: Context, coroutineScope: CoroutineScope) =
        preferenceHierarchy(context) {}

    override fun createWidget(context: Context) = RestrictedPreference(context)

    override fun getLaunchIntent(context: Context, metadata: PreferenceMetadata?) =
        makeLaunchIntent(context, ApnSettingsActivity::class.java, arguments, metadata?.bindingKey)

    override fun isAvailable(context: Context): Boolean {
        val carrierConfig: PersistableBundle? =
            CarrierConfigCache.getInstance(context).getConfigForSubId(subId)
        val isGsmApn =
            MobileNetworkUtils.isGsmOptions(context, subId) &&
                carrierConfig != null &&
                carrierConfig.getBoolean(CarrierConfigManager.KEY_APN_EXPAND_BOOL)
        val hideCarrierNetwork =
            carrierConfig == null ||
                carrierConfig.getBoolean(
                    CarrierConfigManager.KEY_HIDE_CARRIER_NETWORK_SETTINGS_BOOL
                )

        return !hideCarrierNetwork && isGsmApn
    }

    companion object {
        const val KEY = "telephony_apn_key"

        @JvmStatic
        fun parameters(context: Context): Flow<Bundle> {
            fun Int.toArguments() = Bundle(1).also { it.putInt(ApnSettings.SUB_ID, this) }
            return SubscriptionUtil.getSelectableSubscriptionInfoList(context).asFlow().map {
                it.subscriptionId.toArguments()
            }
        }
    }
}
// LINT.ThenChange(ApnSettings.java, ../telephony/ApnPreferenceController.java)
