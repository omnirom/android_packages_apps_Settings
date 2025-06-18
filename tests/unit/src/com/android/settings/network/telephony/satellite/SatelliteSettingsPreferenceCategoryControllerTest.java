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

import static android.telephony.CarrierConfigManager.CARRIER_ROAMING_NTN_CONNECT_AUTOMATIC;
import static android.telephony.CarrierConfigManager.CARRIER_ROAMING_NTN_CONNECT_MANUAL;
import static android.telephony.CarrierConfigManager.KEY_CARRIER_ROAMING_NTN_CONNECT_TYPE_INT;
import static android.telephony.CarrierConfigManager.KEY_SATELLITE_ATTACH_SUPPORTED_BOOL;
import static android.telephony.CarrierConfigManager.KEY_SATELLITE_ESOS_SUPPORTED_BOOL;
import static android.telephony.NetworkRegistrationInfo.SERVICE_TYPE_SMS;

import static com.android.settings.core.BasePreferenceController.AVAILABLE_UNSEARCHABLE;
import static com.android.settings.core.BasePreferenceController.CONDITIONALLY_UNAVAILABLE;
import static com.android.settings.core.BasePreferenceController.UNSUPPORTED_ON_DEVICE;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.os.Looper;
import android.os.PersistableBundle;
import android.platform.test.annotations.EnableFlags;
import android.telephony.satellite.SatelliteManager;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.internal.telephony.flags.Flags;
import com.android.settings.network.CarrierConfigCache;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

@RunWith(AndroidJUnit4.class)
public class SatelliteSettingsPreferenceCategoryControllerTest {
    private static final String KEY = "telephony_satellite_settings_category_key";
    private static final int TEST_SUB_ID = 0;

    @Rule
    public final MockitoRule mMockitoRule = MockitoJUnit.rule();

    @Mock
    private CarrierConfigCache mCarrierConfigCache;

    private Context mContext = null;
    private SatelliteSettingsPreferenceCategoryController mController = null;
    private PersistableBundle mPersistableBundle = new PersistableBundle();

    @Before
    public void setUp() {
        if (Looper.myLooper() == null) {
            Looper.prepare();
        }
        mContext = spy(ApplicationProvider.getApplicationContext());
        SatelliteManager satelliteManager = new SatelliteManager(mContext);
        CarrierConfigCache.setTestInstance(mContext, mCarrierConfigCache);
        mController = new SatelliteSettingsPreferenceCategoryController(mContext, KEY);
        when(mCarrierConfigCache.getConfigForSubId(TEST_SUB_ID)).thenReturn(mPersistableBundle);
        mPersistableBundle.putInt(KEY_CARRIER_ROAMING_NTN_CONNECT_TYPE_INT,
                CARRIER_ROAMING_NTN_CONNECT_AUTOMATIC);
        when(mContext.getSystemService(SatelliteManager.class)).thenReturn(satelliteManager);
        mController.mIsSatelliteSupported.set(true);
    }

    @Test
    @EnableFlags(Flags.FLAG_CARRIER_ENABLED_SATELLITE_FLAG)
    public void getAvailabilityStatus_noSatelliteManager_returnUnsupported() {
        when(mContext.getSystemService(SatelliteManager.class)).thenReturn(null);
        mController.init(TEST_SUB_ID);

        int result = mController.getAvailabilityStatus(TEST_SUB_ID);

        assertThat(result).isEqualTo(UNSUPPORTED_ON_DEVICE);
    }

    @Test
    @EnableFlags(Flags.FLAG_CARRIER_ENABLED_SATELLITE_FLAG)
    public void getAvailabilityStatus_deviceUnsupported_returnUnsupported() {
        mPersistableBundle.putInt(KEY_CARRIER_ROAMING_NTN_CONNECT_TYPE_INT,
                CARRIER_ROAMING_NTN_CONNECT_MANUAL);
        mController.mIsSatelliteSupported.set(false);
        mController.init(TEST_SUB_ID);

        int result = mController.getAvailabilityStatus(TEST_SUB_ID);

        assertThat(result).isEqualTo(UNSUPPORTED_ON_DEVICE);
    }

    @Test
    @EnableFlags(Flags.FLAG_CARRIER_ENABLED_SATELLITE_FLAG)
    public void getAvailabilityStatus_carrierNotSupport_returnUnsupported() {
        mPersistableBundle.putBoolean(KEY_SATELLITE_ATTACH_SUPPORTED_BOOL, false);
        mController.init(TEST_SUB_ID);

        int result = mController.getAvailabilityStatus(TEST_SUB_ID);

        assertThat(result).isEqualTo(UNSUPPORTED_ON_DEVICE);
    }

    @Test
    @EnableFlags({
            Flags.FLAG_CARRIER_ENABLED_SATELLITE_FLAG,
            com.android.settings.flags.Flags.FLAG_SATELLITE_OEM_SETTINGS_UX_MIGRATION
    })
    public void getAvailabilityStatus_sosSupported_returnAvailable() {
        mPersistableBundle.putBoolean(KEY_SATELLITE_ESOS_SUPPORTED_BOOL, true);
        mPersistableBundle.putBoolean(KEY_SATELLITE_ATTACH_SUPPORTED_BOOL, true);
        mController.init(TEST_SUB_ID);

        int result = mController.getAvailabilityStatus(TEST_SUB_ID);

        assertThat(result).isEqualTo(AVAILABLE_UNSEARCHABLE);
    }

    @Test
    @EnableFlags({
            Flags.FLAG_CARRIER_ENABLED_SATELLITE_FLAG,
            com.android.settings.flags.Flags.FLAG_SATELLITE_OEM_SETTINGS_UX_MIGRATION
    })
    public void getAvailabilityStatus_connectTypeAuto_returnAvailable() {
        mPersistableBundle.putBoolean(KEY_SATELLITE_ESOS_SUPPORTED_BOOL, false);
        mPersistableBundle.putBoolean(KEY_SATELLITE_ATTACH_SUPPORTED_BOOL, true);
        mPersistableBundle.putInt(KEY_CARRIER_ROAMING_NTN_CONNECT_TYPE_INT,
                CARRIER_ROAMING_NTN_CONNECT_AUTOMATIC);
        mController.init(TEST_SUB_ID);

        int result = mController.getAvailabilityStatus(TEST_SUB_ID);

        assertThat(result).isEqualTo(AVAILABLE_UNSEARCHABLE);
    }

    @Test
    @EnableFlags({
            Flags.FLAG_CARRIER_ENABLED_SATELLITE_FLAG,
            com.android.settings.flags.Flags.FLAG_SATELLITE_OEM_SETTINGS_UX_MIGRATION
    })
    public void getAvailabilityStatus_connectTypeManualAndAvailable_returnAvailable() {
        mPersistableBundle.putBoolean(KEY_SATELLITE_ESOS_SUPPORTED_BOOL, false);
        mPersistableBundle.putBoolean(KEY_SATELLITE_ATTACH_SUPPORTED_BOOL, true);
        mPersistableBundle.putInt(KEY_CARRIER_ROAMING_NTN_CONNECT_TYPE_INT,
                CARRIER_ROAMING_NTN_CONNECT_AUTOMATIC);
        mController.init(TEST_SUB_ID);
        mController.mCarrierRoamingNtnModeCallback.onCarrierRoamingNtnAvailableServicesChanged(
                new int[]{SERVICE_TYPE_SMS});

        int result = mController.getAvailabilityStatus(TEST_SUB_ID);

        assertThat(result).isEqualTo(AVAILABLE_UNSEARCHABLE);
    }

    @Test
    @EnableFlags({
            Flags.FLAG_CARRIER_ENABLED_SATELLITE_FLAG,
            com.android.settings.flags.Flags.FLAG_SATELLITE_OEM_SETTINGS_UX_MIGRATION
    })
    public void getAvailabilityStatus_connectTypeManualAndUnavailable_returnUnavailable() {
        mPersistableBundle.putBoolean(KEY_SATELLITE_ESOS_SUPPORTED_BOOL, false);
        mPersistableBundle.putBoolean(KEY_SATELLITE_ATTACH_SUPPORTED_BOOL, true);
        mPersistableBundle.putInt(KEY_CARRIER_ROAMING_NTN_CONNECT_TYPE_INT,
                CARRIER_ROAMING_NTN_CONNECT_MANUAL);
        mController.init(TEST_SUB_ID);
        mController.mCarrierRoamingNtnModeCallback.onCarrierRoamingNtnAvailableServicesChanged(
                new int[]{});

        int result = mController.getAvailabilityStatus(TEST_SUB_ID);

        assertThat(result).isEqualTo(CONDITIONALLY_UNAVAILABLE);
    }
}
