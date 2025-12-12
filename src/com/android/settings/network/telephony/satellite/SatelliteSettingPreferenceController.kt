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
package com.android.settings.network.telephony.satellite

import android.content.Context
import android.content.Intent
import android.os.PersistableBundle
import android.provider.Settings
import android.telephony.CarrierConfigManager.CARRIER_ROAMING_NTN_CONNECT_AUTOMATIC
import android.telephony.CarrierConfigManager.CARRIER_ROAMING_NTN_CONNECT_HYBRID
import android.telephony.CarrierConfigManager.CARRIER_ROAMING_NTN_CONNECT_MANUAL
import android.telephony.CarrierConfigManager.KEY_CARRIER_ROAMING_NTN_CONNECT_TYPE_INT
import android.telephony.CarrierConfigManager.KEY_SATELLITE_ATTACH_SUPPORTED_BOOL
import android.telephony.CarrierConfigManager.KEY_SATELLITE_ENTITLEMENT_SUPPORTED_BOOL
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import android.util.Log
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.preference.Preference
import androidx.preference.PreferenceScreen
import com.android.settings.R
import com.android.settings.SettingsActivity
import com.android.settings.network.CarrierConfigCache
import com.android.settings.network.SatelliteRepository
import com.android.settings.network.telephony.MobileNetworkSettingsSearchIndex.MobileNetworkSettingsSearchItem
import com.android.settings.network.telephony.MobileNetworkSettingsSearchIndex.MobileNetworkSettingsSearchResult
import com.android.settings.network.telephony.TelephonyBasePreferenceController
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

/** Preference controller for "Satellite connectivity" */
class SatelliteSettingPreferenceController
@JvmOverloads
constructor(
    context: Context,
    key: String,
    private val carrierConfigCache: CarrierConfigCache = CarrierConfigCache.getInstance(context),
    private val satelliteRepository: SatelliteRepository = SatelliteRepository(context),
) : TelephonyBasePreferenceController(context, key) {

    private lateinit var preference: Preference
    private var mCarrierConfigs: PersistableBundle = PersistableBundle.EMPTY
    private val carrierId =
        context.getSystemService(TelephonyManager::class.java)!!.simSpecificCarrierId

    /**
     * Set subId for Satellite Settings page.
     *
     * @param subId subscription ID.
     */
    fun initialize(subId: Int) {
        Log.d(TAG, "initialize(), subId= $subId")
        this.mSubId = subId
        mCarrierConfigs = getCarrierConfigs(subId, carrierConfigCache)
    }

    override fun onViewCreated(viewLifecycleOwner: LifecycleOwner) {
        if (!SubscriptionManager.isValidSubscriptionId(mSubId)) {
            return
        }

        viewLifecycleOwner.lifecycleScope.launch {
            if (!mCarrierConfigs.getBoolean(KEY_SATELLITE_ATTACH_SUPPORTED_BOOL, false)) {
                preference.isVisible = false
                return@launch
            }

            preference.isEnabled =
                satelliteRepository
                    .isSatelliteAccessConfigurationForCurrentLocationFlow(mSubId)
                    .first()

            val carrierRoamingNtnConnectedType =
                mCarrierConfigs.getInt(
                    KEY_CARRIER_ROAMING_NTN_CONNECT_TYPE_INT,
                    CARRIER_ROAMING_NTN_CONNECT_AUTOMATIC,
                )

            val isSatelliteEntitlementSupported =
                mCarrierConfigs.getBoolean(KEY_SATELLITE_ENTITLEMENT_SUPPORTED_BOOL, false)

            when (carrierRoamingNtnConnectedType) {
                CARRIER_ROAMING_NTN_CONNECT_MANUAL -> {
                    preference.isVisible = actionManual()
                }

                CARRIER_ROAMING_NTN_CONNECT_AUTOMATIC -> {
                    preference.isVisible = true
                    if (!isSatelliteEntitlementSupported) {
                        preference.setSummary(
                            R.string.satellite_setting_summary_without_entitlement
                        )
                        return@launch
                    }
                    val isSatelliteEligible =
                        SatelliteCarrierSettingUtils.isSatelliteAccountEligible(mContext, mSubId)

                    preference.setSummary(
                        if (isSatelliteEligible) R.string.satellite_setting_enabled_summary
                        else R.string.satellite_setting_disabled_summary
                    )
                }

                CARRIER_ROAMING_NTN_CONNECT_HYBRID -> {
                    if (
                        isSatelliteEntitlementSupported &&
                            SatelliteCarrierSettingUtils.isSatelliteAccountEligible(
                                mContext,
                                mSubId,
                            )
                    ) {
                        preference.setSummary(R.string.satellite_setting_enabled_summary)
                    } else {
                        preference.isVisible = actionManual()
                    }
                }
            }
        }
    }

    private suspend fun actionManual(): Boolean {
        if (
            satelliteRepository.requestIsSupportedFlow().first() &&
                satelliteRepository.carrierRoamingNtnAvailableServicesChangedFlow(mSubId).first()
        ) {
            preference.setSummary(R.string.satellite_setting_enabled_summary)
            return true
        }
        return false
    }

    override fun getAvailabilityStatus(subId: Int): Int {
        return if (SubscriptionManager.isValidSubscriptionId(subId)) AVAILABLE
        else CONDITIONALLY_UNAVAILABLE
    }

    override fun displayPreference(screen: PreferenceScreen) {
        preference = screen.findPreference(preferenceKey)!!
    }

    override fun handlePreferenceTreeClick(preference: Preference): Boolean {
        if (preferenceKey == preference.key) {
            // This activity runs in phone process, we must use intent to start
            val intent =
                Intent(Settings.ACTION_SATELLITE_SETTING).setPackage(mContext.getPackageName())
            // This will setup the Home and Search affordance
            intent.putExtra(SettingsActivity.EXTRA_SHOW_FRAGMENT_AS_SUBSETTING, true)
            intent.putExtra(SatelliteSetting.SUB_ID, mSubId)
            mContext.startActivity(intent)
            return true
        }

        return false
    }

    companion object {
        const val TAG = "SatelliteSettingPreferenceController"

        class SatelliteConnectivitySearchItem(private val context: Context) :
            MobileNetworkSettingsSearchItem {
            private fun isAvailable(subId: Int): Boolean = runBlocking {
                val carrierConfigCache = CarrierConfigCache.getInstance(context)
                val carrierConfigs = getCarrierConfigs(subId, carrierConfigCache)
                if (!carrierConfigs.getBoolean(KEY_SATELLITE_ATTACH_SUPPORTED_BOOL, false)) {
                    return@runBlocking false
                }

                val carrierRoamingNtnConnectedType =
                    carrierConfigs.getInt(
                        KEY_CARRIER_ROAMING_NTN_CONNECT_TYPE_INT,
                        CARRIER_ROAMING_NTN_CONNECT_AUTOMATIC,
                    )
                var available = false
                when (carrierRoamingNtnConnectedType) {
                    CARRIER_ROAMING_NTN_CONNECT_MANUAL -> {
                        val satelliteRepository = SatelliteRepository(context)
                        available =
                            satelliteRepository.requestIsSupportedFlow().first() &&
                                satelliteRepository
                                    .carrierRoamingNtnAvailableServicesChangedFlow(subId)
                                    .first()
                    }

                    CARRIER_ROAMING_NTN_CONNECT_AUTOMATIC -> {
                        available = true
                    }

                    CARRIER_ROAMING_NTN_CONNECT_HYBRID -> {
                        if (
                            carrierConfigs.getBoolean(
                                KEY_SATELLITE_ENTITLEMENT_SUPPORTED_BOOL,
                                false,
                            ) &&
                                SatelliteCarrierSettingUtils.isSatelliteAccountEligible(
                                    context,
                                    subId,
                                )
                        ) {
                            available = true
                        } else {
                            val satelliteRepository = SatelliteRepository(context)
                            available =
                                satelliteRepository.requestIsSupportedFlow().first() &&
                                    satelliteRepository
                                        .carrierRoamingNtnAvailableServicesChangedFlow(subId)
                                        .first()
                        }
                    }
                }
                available
            }

            override fun getSearchResult(subId: Int): MobileNetworkSettingsSearchResult? {
                if (!isAvailable(subId)) return null
                return MobileNetworkSettingsSearchResult(
                    key = "telephony_satellite_setting_key",
                    title = context.getString(R.string.title_satellite_setting_connectivity),
                )
            }
        }

        fun getCarrierConfigs(
            subId: Int,
            carrierConfigCache: CarrierConfigCache,
        ): PersistableBundle {
            return carrierConfigCache.getSpecificConfigsForSubId(
                subId,
                KEY_SATELLITE_ATTACH_SUPPORTED_BOOL,
                KEY_SATELLITE_ENTITLEMENT_SUPPORTED_BOOL,
                KEY_CARRIER_ROAMING_NTN_CONNECT_TYPE_INT,
            )
        }
    }
}
