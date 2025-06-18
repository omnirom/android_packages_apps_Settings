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
import static android.telephony.CarrierConfigManager.KEY_SATELLITE_ENTITLEMENT_SUPPORTED_BOOL;
import static android.telephony.NetworkRegistrationInfo.SERVICE_TYPE_SMS;

import static com.android.settings.core.BasePreferenceController.AVAILABLE;
import static com.android.settings.core.BasePreferenceController.CONDITIONALLY_UNAVAILABLE;
import static com.android.settings.core.BasePreferenceController.UNSUPPORTED_ON_DEVICE;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.os.Looper;
import android.os.PersistableBundle;
import android.platform.test.annotations.EnableFlags;
import android.telephony.CarrierConfigManager;
import android.telephony.TelephonyManager;
import android.telephony.satellite.SatelliteManager;

import androidx.preference.Preference;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;
import androidx.test.annotation.UiThreadTest;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.settings.network.CarrierConfigCache;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

@RunWith(AndroidJUnit4.class)
@UiThreadTest
public class SatelliteSettingPreferenceControllerTest {
    private static final String KEY = "SatelliteSettingsPreferenceControllerTest";
    private static final int TEST_SUB_ID = 5;

    @Rule
    public final MockitoRule mMockitoRule = MockitoJUnit.rule();

    @Mock
    private CarrierConfigCache mCarrierConfigCache;
    @Mock
    private TelephonyManager mTelephonyManager;

    private Context mContext;
    private SatelliteSettingPreferenceController mController;
    private final PersistableBundle mCarrierConfig = new PersistableBundle();

    @Before
    public void setUp() {
        if (Looper.myLooper() == null) {
            Looper.prepare();
        }
        mContext = spy(ApplicationProvider.getApplicationContext());
        SatelliteManager satelliteManager = new SatelliteManager(mContext);
        CarrierConfigCache.setTestInstance(mContext, mCarrierConfigCache);
        when(mContext.getSystemService(SatelliteManager.class)).thenReturn(satelliteManager);
        when(mContext.getSystemService(TelephonyManager.class)).thenReturn(mTelephonyManager);
        when(mTelephonyManager.createForSubscriptionId(TEST_SUB_ID)).thenReturn(mTelephonyManager);
        when(mCarrierConfigCache.getConfigForSubId(TEST_SUB_ID)).thenReturn(mCarrierConfig);
        mController = new SatelliteSettingPreferenceController(mContext, KEY);
    }

    @Test
    public void getAvailabilityStatus_noSatellite_returnUnsupported() {
        when(mContext.getSystemService(SatelliteManager.class)).thenReturn(null);
        mController = new SatelliteSettingPreferenceController(mContext, KEY);
        mController.initialize(TEST_SUB_ID);

        int result = mController.getAvailabilityStatus(TEST_SUB_ID);

        assertThat(result).isEqualTo(UNSUPPORTED_ON_DEVICE);
    }

    @Test
    public void getAvailabilityStatus_carrierIsNotSupport_returnUnavailable() {
        mCarrierConfig.putBoolean(
                CarrierConfigManager.KEY_SATELLITE_ATTACH_SUPPORTED_BOOL,
                false);
        mController.initialize(TEST_SUB_ID);

        int result = mController.getAvailabilityStatus(TEST_SUB_ID);

        assertThat(result).isEqualTo(CONDITIONALLY_UNAVAILABLE);
    }

    @Test
    public void getAvailabilityStatus_connectTypeIsManualButUnavailable_returnUnavailable() {
        mCarrierConfig.putBoolean(
                CarrierConfigManager.KEY_SATELLITE_ATTACH_SUPPORTED_BOOL,
                true);
        mCarrierConfig.putInt(
                CarrierConfigManager.KEY_CARRIER_ROAMING_NTN_CONNECT_TYPE_INT,
                CARRIER_ROAMING_NTN_CONNECT_MANUAL);
        mController.initialize(TEST_SUB_ID);
        mController.mCarrierRoamingNtnModeCallback.onCarrierRoamingNtnAvailableServicesChanged(
                new int[]{});

        int result = mController.getAvailabilityStatus(TEST_SUB_ID);

        assertThat(result).isEqualTo(CONDITIONALLY_UNAVAILABLE);
    }

    @Test
    public void getAvailabilityStatus_connectTypeIsManualAndAvailable_returnAvailable() {
        mCarrierConfig.putBoolean(
                CarrierConfigManager.KEY_SATELLITE_ATTACH_SUPPORTED_BOOL,
                true);
        mCarrierConfig.putInt(
                CarrierConfigManager.KEY_CARRIER_ROAMING_NTN_CONNECT_TYPE_INT,
                CARRIER_ROAMING_NTN_CONNECT_MANUAL);
        mController.initialize(TEST_SUB_ID);
        mController.mCarrierRoamingNtnModeCallback.onCarrierRoamingNtnAvailableServicesChanged(
                new int[]{SERVICE_TYPE_SMS});

        int result = mController.getAvailabilityStatus(TEST_SUB_ID);

        assertThat(result).isEqualTo(AVAILABLE);
    }

    @Test
    public void getAvailabilityStatus_connectTypeIsAuto_returnAvailable() {
        mCarrierConfig.putBoolean(
                CarrierConfigManager.KEY_SATELLITE_ATTACH_SUPPORTED_BOOL,
                true);
        mCarrierConfig.putInt(
                CarrierConfigManager.KEY_CARRIER_ROAMING_NTN_CONNECT_TYPE_INT,
                CARRIER_ROAMING_NTN_CONNECT_AUTOMATIC);
        mController.initialize(TEST_SUB_ID);

        int result = mController.getAvailabilityStatus(TEST_SUB_ID);

        assertThat(result).isEqualTo(AVAILABLE);
    }

    @Test
    @EnableFlags(com.android.settings.flags.Flags.FLAG_SATELLITE_OEM_SETTINGS_UX_MIGRATION)
    public void onResume_registerTelephonyCallback_success() {
        mController.initialize(TEST_SUB_ID);
        mController.onResume(null);

        verify(mTelephonyManager).registerTelephonyCallback(any(), any());
    }

    @Test
    @EnableFlags(com.android.settings.flags.Flags.FLAG_SATELLITE_OEM_SETTINGS_UX_MIGRATION)
    public void getAvailabilityStatus_unregisterTelephonyCallback_success() {
        mController.initialize(TEST_SUB_ID);
        mController.onPause(null);

        verify(mTelephonyManager).unregisterTelephonyCallback(any());
    }

    @Test
    @EnableFlags(com.android.settings.flags.Flags.FLAG_SATELLITE_OEM_SETTINGS_UX_MIGRATION)
    public void summary_noEntitlementAndTypeIsAuto_showSummaryWithoutEntitlement() {
        mCarrierConfig.putInt(
                CarrierConfigManager.KEY_CARRIER_ROAMING_NTN_CONNECT_TYPE_INT,
                CARRIER_ROAMING_NTN_CONNECT_AUTOMATIC);
        mCarrierConfig.putBoolean(
                KEY_SATELLITE_ENTITLEMENT_SUPPORTED_BOOL,
                false);
        mController.initialize(TEST_SUB_ID);
        PreferenceManager preferenceManager = new PreferenceManager(mContext);
        PreferenceScreen preferenceScreen = preferenceManager.createPreferenceScreen(mContext);
        Preference preference = new Preference(mContext);
        preference.setKey(KEY);
        preference.setTitle("test title");
        preferenceScreen.addPreference(preference);
        mController.displayPreference(preferenceScreen);

        assertThat(preference.getSummary()).isEqualTo(
                "Send and receive text messages by satellite. Contact your carrier for details.");
    }

    @Test
    @EnableFlags(com.android.settings.flags.Flags.FLAG_SATELLITE_OEM_SETTINGS_UX_MIGRATION)
    public void summary_smsAvailableForManualType_showSummaryWithAccount() {
        mCarrierConfig.putInt(
                CarrierConfigManager.KEY_CARRIER_ROAMING_NTN_CONNECT_TYPE_INT,
                CARRIER_ROAMING_NTN_CONNECT_MANUAL);
        mController.initialize(TEST_SUB_ID);
        PreferenceManager preferenceManager = new PreferenceManager(mContext);
        PreferenceScreen preferenceScreen = preferenceManager.createPreferenceScreen(mContext);
        Preference preference = new Preference(mContext);
        preference.setKey(KEY);
        preference.setTitle("test title");
        preferenceScreen.addPreference(preference);
        mController.displayPreference(preferenceScreen);
        mController.mCarrierRoamingNtnModeCallback.onCarrierRoamingNtnAvailableServicesChanged(
                new int[]{SERVICE_TYPE_SMS});

        assertThat(preference.getSummary()).isEqualTo(
                "Send and receive text messages by satellite. Included with your account.");
    }

    @Test
    @EnableFlags(com.android.settings.flags.Flags.FLAG_SATELLITE_OEM_SETTINGS_UX_MIGRATION)
    public void getAvailabilityStatus_smsAvailableForAutoType_showSummaryWithoutAccount() {
        mCarrierConfig.putBoolean(
                KEY_SATELLITE_ENTITLEMENT_SUPPORTED_BOOL,
                true);
        mCarrierConfig.putInt(
                CarrierConfigManager.KEY_CARRIER_ROAMING_NTN_CONNECT_TYPE_INT,
                CARRIER_ROAMING_NTN_CONNECT_MANUAL);
        mController.initialize(TEST_SUB_ID);
        PreferenceManager preferenceManager = new PreferenceManager(mContext);
        PreferenceScreen preferenceScreen = preferenceManager.createPreferenceScreen(mContext);
        Preference preference = new Preference(mContext);
        preference.setKey(KEY);
        preference.setTitle("test title");
        preferenceScreen.addPreference(preference);
        mController.displayPreference(preferenceScreen);
        mController.mCarrierRoamingNtnModeCallback.onCarrierRoamingNtnAvailableServicesChanged(
                new int[]{});

        assertThat(preference.getSummary()).isEqualTo(
                "Send and receive text messages by satellite. Not included with your account.");
    }
}
