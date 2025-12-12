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

import static android.telephony.CarrierConfigManager.SATELLITE_DATA_SUPPORT_ALL;
import static android.telephony.CarrierConfigManager.SATELLITE_DATA_SUPPORT_ONLY_RESTRICTED;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.spy;

import android.content.Context;
import android.os.Looper;
import android.telephony.satellite.SatelliteManager;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class SatelliteCarrierSettingUtilsTest {
    private static final int TEST_SUB_ID = 0;

    @Rule
    public final MockitoRule mMockitoRule = MockitoJUnit.rule();

    private Context mContext;

    @Before
    public void setUp() {
        if (Looper.myLooper() == null) {
            Looper.prepare();
        }
        mContext = spy(ApplicationProvider.getApplicationContext());
    }

    @Test
    public void isSatelliteAccountEligible_noRestrictedReason_returnTrue() {
        SatelliteCarrierSettingUtils.SatelliteManagerWrapper wrapper =
                new SatelliteCarrierSettingUtils.SatelliteManagerWrapper(mContext) {
                    @Override
                    public Set<Integer> getAttachRestrictionReasonsForCarrier(int subId) {
                        return Collections.emptySet();
                    }
                };
        SatelliteCarrierSettingUtils.sSatelliteManagerWrapper = wrapper;

        boolean result = SatelliteCarrierSettingUtils.isSatelliteAccountEligible(mContext,
                TEST_SUB_ID);

        assertThat(result).isTrue();
    }

    @Test
    public void isSatelliteAccountEligible_hasRestrictedReason_returnFalse() throws Exception {
        SatelliteCarrierSettingUtils.sSatelliteManagerWrapper =
                new SatelliteCarrierSettingUtils.SatelliteManagerWrapper(mContext) {
                    @Override
                    public Set<Integer> getAttachRestrictionReasonsForCarrier(int subId) {
                        Set<Integer> set = new HashSet<>();
                        set.add(SatelliteManager
                                .SATELLITE_COMMUNICATION_RESTRICTION_REASON_ENTITLEMENT);
                        return set;
                    }
                };

        boolean result = SatelliteCarrierSettingUtils.isSatelliteAccountEligible(mContext,
                TEST_SUB_ID);

        assertThat(result).isFalse();
    }

    @Test
    public void isSatelliteDataRestricted_unlimitedDataMode_returnFalse() throws Exception {
        SatelliteCarrierSettingUtils.sSatelliteManagerWrapper =
                new SatelliteCarrierSettingUtils.SatelliteManagerWrapper(mContext) {
                    @Override
                    public int getSatelliteDataSupportMode(int subId) {
                        return SATELLITE_DATA_SUPPORT_ALL;
                    }
                };

        int result = SatelliteCarrierSettingUtils.getSatelliteDataMode(mContext,
                TEST_SUB_ID);

        assertThat(result).isEqualTo(SATELLITE_DATA_SUPPORT_ALL);
    }

    @Test
    public void isSatelliteDataRestricted_restrictedDataMode_returnTrue() throws Exception {
        SatelliteCarrierSettingUtils.sSatelliteManagerWrapper =
                new SatelliteCarrierSettingUtils.SatelliteManagerWrapper(mContext) {
                    @Override
                    public int getSatelliteDataSupportMode(int subId) {
                        return SATELLITE_DATA_SUPPORT_ONLY_RESTRICTED;
                    }
                };

        int result = SatelliteCarrierSettingUtils.getSatelliteDataMode(mContext,
                TEST_SUB_ID);

        assertThat(result).isEqualTo(SATELLITE_DATA_SUPPORT_ONLY_RESTRICTED);
    }
}
