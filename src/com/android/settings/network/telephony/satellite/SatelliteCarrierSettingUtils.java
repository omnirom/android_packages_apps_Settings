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

package com.android.settings.network.telephony.satellite;

import static android.telephony.CarrierConfigManager.SATELLITE_DATA_SUPPORT_ONLY_RESTRICTED;

import android.content.Context;
import android.telephony.satellite.SatelliteManager;
import android.util.Log;

import androidx.annotation.VisibleForTesting;

import java.util.Collections;
import java.util.Set;

/** A until for carrier satellite setting. */
public class SatelliteCarrierSettingUtils {
    private static final String TAG = "SatelliteCarrierSettingUtils";

    @VisibleForTesting
    static SatelliteManagerWrapper sSatelliteManagerWrapper;

    /**
     * Checks account is eligible.
     *
     * @return true if there is no restriction reason returned.
     */
    public static boolean isSatelliteAccountEligible(Context context, int subId) {
        SatelliteManagerWrapper wrapper =
                sSatelliteManagerWrapper == null ? new SatelliteManagerWrapper(context)
                        : sSatelliteManagerWrapper;

        Set<Integer> restrictionReason = wrapper.getAttachRestrictionReasonsForCarrier(subId);
        return !restrictionReason.contains(
                SatelliteManager.SATELLITE_COMMUNICATION_RESTRICTION_REASON_ENTITLEMENT);
    }

    /**
     * Use getSatelliteDataSupportMode to check data mode is restricted.
     *
     * @return true if data mode is restricted.
     */
    public static boolean isSatelliteDataRestricted(Context context, int subId) {
        SatelliteManagerWrapper wrapper =
                sSatelliteManagerWrapper == null ? new SatelliteManagerWrapper(context)
                        : sSatelliteManagerWrapper;
        return wrapper.getSatelliteDataSupportMode(subId) <= SATELLITE_DATA_SUPPORT_ONLY_RESTRICTED;
    }


    @VisibleForTesting
    static class SatelliteManagerWrapper {
        private final SatelliteManager mSatelliteManager;

        SatelliteManagerWrapper(Context context) {
            mSatelliteManager = context.getSystemService(SatelliteManager.class);
        }

        public Set<Integer> getAttachRestrictionReasonsForCarrier(int subId) {
            if (mSatelliteManager == null) {
                Log.d(TAG, "SatelliteManager is null.");
                return Collections.emptySet();
            }
            try {
                Set<Integer> restrictionReason =
                        mSatelliteManager.getAttachRestrictionReasonsForCarrier(subId);
                Log.d(TAG, "Error to getAttachRestrictionReasonsForCarrier : " + restrictionReason);
                return restrictionReason;
            } catch (SecurityException | IllegalStateException | IllegalArgumentException e) {
                Log.d(TAG, "Error to getAttachRestrictionReasonsForCarrier : " + e);
            }
            return Collections.emptySet();
        }

        public int getSatelliteDataSupportMode(int subId) {
            if (mSatelliteManager == null) {
                Log.d(TAG, "SatelliteManager is null.");
                return SATELLITE_DATA_SUPPORT_ONLY_RESTRICTED;
            }

            var dataMode = SATELLITE_DATA_SUPPORT_ONLY_RESTRICTED;
            try {
                dataMode = mSatelliteManager.getSatelliteDataSupportMode(subId);
                Log.d(TAG, "Data mode : " + dataMode);
            } catch (IllegalStateException e) {
                Log.d(TAG, "Failed to get data mode : " + e);
            }
            return dataMode;
        }
    }
}
