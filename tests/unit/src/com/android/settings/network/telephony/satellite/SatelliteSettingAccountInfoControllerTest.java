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
import static android.telephony.CarrierConfigManager.KEY_SATELLITE_ENTITLEMENT_SUPPORTED_BOOL;

import static com.android.settings.core.BasePreferenceController.AVAILABLE;
import static com.android.settings.core.BasePreferenceController.CONDITIONALLY_UNAVAILABLE;
import static com.android.settings.network.telephony.satellite.SatelliteSettingAccountInfoController.PREF_KEY_CATEGORY_YOUR_SATELLITE_PLAN;
import static com.android.settings.network.telephony.satellite.SatelliteSettingAccountInfoController.PREF_KEY_YOUR_SATELLITE_DATA_PLAN;
import static com.android.settings.network.telephony.satellite.SatelliteSettingAccountInfoController.PREF_KEY_YOUR_SATELLITE_PLAN;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.os.Looper;
import android.os.PersistableBundle;
import android.telephony.TelephonyManager;
import android.telephony.satellite.SatelliteManager;

import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.settings.testutils.ResourcesUtils;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

@RunWith(AndroidJUnit4.class)
public class SatelliteSettingAccountInfoControllerTest {
    private static final int TEST_SUB_ID = 5;
    private static final String TEST_OPERATOR_NAME = "test_operator_name";

    @Rule
    public final MockitoRule mMockitoRule = MockitoJUnit.rule();

    @Mock
    private TelephonyManager mTelephonyManager;

    private Context mContext;
    private SatelliteSettingAccountInfoController mController;
    private final PersistableBundle mPersistableBundle = new PersistableBundle();

    @Before
    public void setUp() {
        if (Looper.myLooper() == null) {
            Looper.prepare();
        }
        mContext = spy(ApplicationProvider.getApplicationContext());
        mController = new SatelliteSettingAccountInfoController(mContext,
                PREF_KEY_CATEGORY_YOUR_SATELLITE_PLAN);
        when(mContext.getSystemService(TelephonyManager.class)).thenReturn(mTelephonyManager);
        when(mTelephonyManager.getSimOperatorName(TEST_SUB_ID)).thenReturn(TEST_OPERATOR_NAME);
    }

    @Test
    public void getAvailabilityStatus_entitlementNotSupport_returnConditionalUnavailable() {
        mPersistableBundle.putInt(KEY_CARRIER_ROAMING_NTN_CONNECT_TYPE_INT,
                CARRIER_ROAMING_NTN_CONNECT_AUTOMATIC);
        when(mContext.getSystemService(SatelliteManager.class)).thenReturn(null);
        mController.init(TEST_SUB_ID, mPersistableBundle, false, false);

        int result = mController.getAvailabilityStatus(TEST_SUB_ID);

        assertThat(result).isEqualTo(CONDITIONALLY_UNAVAILABLE);
    }

    @Test
    public void getAvailabilityStatus_entitlementIsSupported_returnConditionalUnavailable() {
        mPersistableBundle.putInt(KEY_CARRIER_ROAMING_NTN_CONNECT_TYPE_INT,
                CARRIER_ROAMING_NTN_CONNECT_AUTOMATIC);
        mPersistableBundle.putBoolean(KEY_SATELLITE_ENTITLEMENT_SUPPORTED_BOOL, true);
        mController.init(TEST_SUB_ID, mPersistableBundle, false, false);

        int result = mController.getAvailabilityStatus(TEST_SUB_ID);

        assertThat(result).isEqualTo(AVAILABLE);
    }

    @Test
    public void getAvailabilityStatus_connectionTypeISManual_returnAvailable() {
        mPersistableBundle.putInt(KEY_CARRIER_ROAMING_NTN_CONNECT_TYPE_INT,
                CARRIER_ROAMING_NTN_CONNECT_MANUAL);
        mController.init(TEST_SUB_ID, mPersistableBundle, false, false);

        int result = mController.getAvailabilityStatus(TEST_SUB_ID);

        assertThat(result).isEqualTo(AVAILABLE);
    }

    @Test
    public void displayPreference_showCategoryTitle_correctOperatorName() {
        mPersistableBundle.putBoolean(KEY_SATELLITE_ENTITLEMENT_SUPPORTED_BOOL, true);
        when(mContext.getSystemService(SatelliteManager.class)).thenReturn(null);
        mController.init(TEST_SUB_ID, mPersistableBundle, false, false);

        PreferenceScreen screen = new PreferenceManager(mContext).createPreferenceScreen(mContext);
        PreferenceCategory preferenceCategory = new PreferenceCategory(mContext);
        preferenceCategory.setKey(PREF_KEY_CATEGORY_YOUR_SATELLITE_PLAN);
        Preference preference = new Preference(mContext);
        preference.setKey(PREF_KEY_YOUR_SATELLITE_PLAN);
        screen.addPreference(preferenceCategory);
        screen.addPreference(preference);

        mController.displayPreference(screen);

        assertThat(preferenceCategory.getTitle().toString()).isEqualTo(
                ResourcesUtils.getResourcesString(mContext, "category_title_your_satellite_plan",
                        TEST_OPERATOR_NAME));
    }

    @Test
    public void displayPreference_showEligibleUiButDataUnavailable_showSmsEligibleAccountState() {
        mPersistableBundle.putBoolean(KEY_SATELLITE_ENTITLEMENT_SUPPORTED_BOOL, true);
        when(mContext.getSystemService(SatelliteManager.class)).thenReturn(null);
        mController = new SatelliteSettingAccountInfoController(mContext,
                PREF_KEY_CATEGORY_YOUR_SATELLITE_PLAN) {
            @Override
            protected boolean isSatelliteEligible() {
                return true;
            }
        };
        mController.init(TEST_SUB_ID, mPersistableBundle, true, false);
        PreferenceScreen screen = new PreferenceManager(mContext).createPreferenceScreen(mContext);
        PreferenceCategory preferenceCategory = new PreferenceCategory(mContext);
        preferenceCategory.setKey(PREF_KEY_CATEGORY_YOUR_SATELLITE_PLAN);
        Preference preference = new Preference(mContext);
        preference.setKey(PREF_KEY_YOUR_SATELLITE_PLAN);
        Preference preferenceData = new Preference(mContext);
        preferenceData.setKey(PREF_KEY_YOUR_SATELLITE_DATA_PLAN);
        screen.addPreference(preferenceCategory);
        screen.addPreference(preference);
        screen.addPreference(preferenceData);

        mController.displayPreference(screen);

        assertThat(preference.getTitle().toString()).isEqualTo(
                ResourcesUtils.getResourcesString(mContext, "title_have_satellite_plan"));
        assertThat(preferenceData.getTitle()).isEqualTo(null);
    }

    @Test
    public void
            displayPreference_showEligibleUiAndDataAvailable_showSmsAndDataEligibleAccountState() {
        mPersistableBundle.putBoolean(KEY_SATELLITE_ENTITLEMENT_SUPPORTED_BOOL, true);
        when(mContext.getSystemService(SatelliteManager.class)).thenReturn(null);
        mController = new SatelliteSettingAccountInfoController(mContext,
                PREF_KEY_CATEGORY_YOUR_SATELLITE_PLAN) {
            @Override
            protected boolean isSatelliteEligible() {
                return true;
            }
        };
        mController.init(TEST_SUB_ID, mPersistableBundle, true, true);
        PreferenceScreen screen = new PreferenceManager(mContext).createPreferenceScreen(mContext);
        PreferenceCategory preferenceCategory = new PreferenceCategory(mContext);
        preferenceCategory.setKey(PREF_KEY_CATEGORY_YOUR_SATELLITE_PLAN);
        Preference preference = new Preference(mContext);
        preference.setKey(PREF_KEY_YOUR_SATELLITE_PLAN);
        Preference preferenceData = new Preference(mContext);
        preferenceData.setKey(PREF_KEY_YOUR_SATELLITE_DATA_PLAN);
        screen.addPreference(preferenceCategory);
        screen.addPreference(preference);
        screen.addPreference(preferenceData);

        mController.displayPreference(screen);

        assertThat(preference.getTitle().toString()).isEqualTo(
                ResourcesUtils.getResourcesString(mContext, "title_have_satellite_plan"));
        assertThat(preferenceData.getTitle().toString()).isEqualTo(
                ResourcesUtils.getResourcesString(mContext, "title_have_satellite_data_plan"));
    }

    @Test
    public void displayPreference_showIneligibleUi_showSmsAccountState() {
        mPersistableBundle.putBoolean(KEY_SATELLITE_ENTITLEMENT_SUPPORTED_BOOL, true);
        when(mContext.getSystemService(SatelliteManager.class)).thenReturn(null);
        mController = new SatelliteSettingAccountInfoController(mContext,
                PREF_KEY_CATEGORY_YOUR_SATELLITE_PLAN) {
            @Override
            protected boolean isSatelliteEligible() {
                return false;
            }
        };
        mController.init(TEST_SUB_ID, mPersistableBundle, false, false);
        PreferenceScreen screen = new PreferenceManager(mContext).createPreferenceScreen(mContext);
        PreferenceCategory preferenceCategory = new PreferenceCategory(mContext);
        preferenceCategory.setKey(PREF_KEY_CATEGORY_YOUR_SATELLITE_PLAN);
        Preference preference = new Preference(mContext);
        preference.setKey(PREF_KEY_YOUR_SATELLITE_PLAN);
        screen.addPreference(preferenceCategory);
        screen.addPreference(preference);

        mController.displayPreference(screen);

        assertThat(preference.getTitle().toString()).isEqualTo(
                ResourcesUtils.getResourcesString(mContext, "title_no_satellite_plan"));
    }
}
