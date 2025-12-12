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

import android.app.settings.SettingsEnums
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.UserManager
import android.provider.Settings
import android.telephony.SubscriptionManager
import androidx.fragment.app.Fragment
import com.android.settings.R
import com.android.settings.Settings.MobileNetworkActivity
import com.android.settings.core.PreferenceScreenMixin
import com.android.settings.datausage.DataUsageListScreen
import com.android.settings.flags.Flags
import com.android.settings.network.SubscriptionUtil
import com.android.settings.network.apn.ApnSettings
import com.android.settings.network.apn.ApnSettingsScreen
import com.android.settings.restriction.PreferenceRestrictionMixin
import com.android.settings.utils.makeLaunchIntent
import com.android.settingslib.metadata.PreferenceAvailabilityProvider
import com.android.settingslib.metadata.PreferenceIndexableProvider
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.metadata.PreferenceTitleProvider
import com.android.settingslib.metadata.ProvidePreferenceScreen
import com.android.settingslib.metadata.preferenceHierarchy
import com.android.settingslib.widget.UntitledPreferenceCategoryMetadata
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.map

/** Preference screen for Network & Internet > SIMs > [SIM] */
// LINT.IfChange
@ProvidePreferenceScreen(MobileNetworkScreen.KEY, parameterized = true)
open class MobileNetworkScreen(override val arguments: Bundle) :
    PreferenceScreenMixin,
    PreferenceAvailabilityProvider,
    PreferenceTitleProvider,
    PreferenceIndexableProvider,
    PreferenceRestrictionMixin {

    private val subId =
        arguments.getInt(Settings.EXTRA_SUB_ID, SubscriptionManager.INVALID_SUBSCRIPTION_ID)

    override val key: String
        get() = KEY

    override val bindingKey
        get() = "$KEY-$subId"

    override fun getTitle(context: Context): CharSequence? {
        val info = SubscriptionUtil.getSubscriptionOrDefault(context, subId)
        return SubscriptionUtil.getUniqueSubscriptionDisplayName(info, context)
    }

    override val highlightMenuKey: Int
        get() = R.string.menu_key_network

    override fun getMetricsCategory() = SettingsEnums.MOBILE_NETWORK

    override fun isAvailable(context: Context): Boolean =
        SubscriptionManager.isValidSubscriptionId(subId)

    override fun isFlagEnabled(context: Context): Boolean = Flags.deeplinkNetworkAndInternet25q4()

    override fun fragmentClass(): Class<out Fragment>? = MobileNetworkSettings::class.java

    override fun hasCompleteHierarchy() = false

    override fun getPreferenceHierarchy(context: Context, coroutineScope: CoroutineScope) =
        preferenceHierarchy(context) {
            if (Flags.deeplinkNetworkAndInternet25q4()) {
                +MobileNetworkMainSwitchPreference(context, subId) order +0
                val data = MobileNetworkData(context, coroutineScope, subId)
                +EnabledStateUntitledCategory(subId) += {
                    +MobileNetworkDataUsagePreference(context, coroutineScope, subId)
                    +MobileNetworkSpnPreference(context, subId)
                    +MobileNetworkPhoneNumberPreference(data)
                    +EnabledNetworkModePreference(data)
                    +MobileNetworkImeiPreference(context, subId)
                    +(DataUsageListScreen.KEY args arguments)
                    +UntitledPreferenceCategoryMetadata("apn_and_protection_container") += {
                        val bundle = Bundle(1).also { it.putInt(ApnSettings.SUB_ID, subId) }
                        +(ApnSettingsScreen.KEY args bundle)
                    }
                }
            }
        }

    override fun getLaunchIntent(context: Context, metadata: PreferenceMetadata?): Intent? {
        val intent =
            makeLaunchIntent(
                context,
                MobileNetworkActivity::class.java,
                arguments,
                metadata?.bindingKey,
            )
        intent.putExtra(Settings.EXTRA_SUB_ID, subId)
        return intent
    }

    override val restrictionKeys
        get() = arrayOf(UserManager.DISALLOW_CONFIG_MOBILE_NETWORKS)

    override val useAdminDisabledSummary
        get() = true

    override fun isEnabled(context: Context) = super<PreferenceRestrictionMixin>.isEnabled(context)

    override fun isIndexable(context: Context) =
        // Shoudln't be indexable on ARC (Android on Chrome OS) and this seems to be the way to
        // detect that. See https://stackoverflow.com/a/44868935 or isArc() implementation in
        // cts/common/device-side/util-axt/src/com/android/compatibility/common/util/FeatureUtil.java
        !(context.packageManager.hasSystemFeature("org.chromium.arc") ||
            context.packageManager.hasSystemFeature("org.chromium.arc.device_management"))

    companion object {
        const val KEY = "mobile_network_pref_screen"

        @JvmStatic
        fun parameters(context: Context): Flow<Bundle> {
            fun Int.toArguments() = Bundle(1).also { it.putInt(Settings.EXTRA_SUB_ID, this) }
            return SubscriptionUtil.getSelectableSubscriptionInfoList(context).asFlow().map {
                it.subscriptionId.toArguments()
            }
        }
    }
}
// LINT.ThenChange(MobileNetworkSettings.java)
