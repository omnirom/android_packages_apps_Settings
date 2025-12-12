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

import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/** A until for carrier satellite setting. */
public class SatelliteCarrierSettingUtils {
    private static final String TAG = "SatelliteCarrierSettingUtils";

    /**
     * {@link android.telephony.satellite.SatelliteAccessConfiguration} is used to store satellite
     * access configuration that will be applied to the satellite communication at the corresponding
     * region. 1001 is one of the Tag ID, and is pointed out current region is supported by carrier
     * satellite service.
     */
    public static final int SATELLITE_REGION_TAG_ID = 1001;

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
     * @return Integer of {@code CarrierConfigManager#SATELLITE_DATA_SUPPORT_MODE}
     */
    public static int getSatelliteDataMode(Context context, int subId) {
        SatelliteManagerWrapper wrapper =
                sSatelliteManagerWrapper == null ? new SatelliteManagerWrapper(context)
                        : sSatelliteManagerWrapper;
        return wrapper.getSatelliteDataSupportMode(subId);
    }

    /**
     * Check if current carrier is supported in this region.
     */
    public static boolean isCarrierSatelliteRegionSupported(Context context,
            @Nullable List<Integer> tagIds, int carrierId) {
        if (tagIds == null) {
            return true;
        }

        int[] carrierIds = context.getResources().getIntArray(
                com.android.settings.R.array.config_carrier_id_list_for_satellite_geo_fence_check);
        Log.d(TAG, "tagIds is " + tagIds + " / carrier id : " + carrierId);
        boolean isCarrierNeedToCheckRegion = Arrays.stream(carrierIds).anyMatch(
                it -> it == carrierId);
        if (!isCarrierNeedToCheckRegion) {
            return true;
        }
        return tagIds.stream().anyMatch(tagId -> tagId == SATELLITE_REGION_TAG_ID);
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
                Log.d(TAG, "getAttachRestrictionReasonsForCarrier : " + restrictionReason);
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
