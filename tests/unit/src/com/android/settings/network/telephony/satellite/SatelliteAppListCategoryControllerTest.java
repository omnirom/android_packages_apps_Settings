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

import static android.telephony.CarrierConfigManager.CARRIER_ROAMING_NTN_CONNECT_MANUAL;
import static android.telephony.CarrierConfigManager.KEY_CARRIER_ROAMING_NTN_CONNECT_TYPE_INT;
import static android.telephony.CarrierConfigManager.KEY_SATELLITE_ENTITLEMENT_SUPPORTED_BOOL;

import static com.android.settings.core.BasePreferenceController.AVAILABLE_UNSEARCHABLE;
import static com.android.settings.core.BasePreferenceController.CONDITIONALLY_UNAVAILABLE;
import static com.android.settings.network.telephony.satellite.SatelliteAppListCategoryController.MAXIMUM_OF_PREFERENCE_AMOUNT;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Looper;
import android.os.PersistableBundle;
import android.platform.test.annotations.EnableFlags;

import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;
import androidx.test.core.app.ApplicationProvider;

import com.android.internal.telephony.flags.Flags;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.List;

public class SatelliteAppListCategoryControllerTest {
    @Rule
    public final MockitoRule mMockitoRule = MockitoJUnit.rule();

    private static final int TEST_SUB_ID = 0;
    private static final List<String> PACKAGE_NAMES = List.of("com.android.settings",
            "com.android.apps.messaging", "com.android.dialer", "com.android.systemui");
    private static final String KEY = "SatelliteAppListCategoryControllerTest";

    @Mock
    private PackageManager mPackageManager;

    private Context mContext;
    private SatelliteAppListCategoryController mController;
    private PersistableBundle mPersistableBundle = new PersistableBundle();

    @Before
    public void setUp() {
        if (Looper.myLooper() == null) {
            Looper.prepare();
        }
        mContext = spy(ApplicationProvider.getApplicationContext());
        when(mContext.getPackageManager()).thenReturn(mPackageManager);
        mPersistableBundle.putInt(KEY_CARRIER_ROAMING_NTN_CONNECT_TYPE_INT,
                CARRIER_ROAMING_NTN_CONNECT_MANUAL);
        mPersistableBundle.putBoolean(KEY_SATELLITE_ENTITLEMENT_SUPPORTED_BOOL, false);
    }

    @Test
    @EnableFlags(Flags.FLAG_SATELLITE_25Q4_APIS)
    public void displayPreference_has4SatSupportedApps_showMaxPreference() throws Exception {
        when(mPackageManager.getApplicationInfoAsUser(any(), anyInt(), anyInt())).thenReturn(
                new ApplicationInfo());
        mController = new SatelliteAppListCategoryController(mContext, KEY) {
            @Override
            protected boolean isSatelliteEligible() {
                return true;
            }

            @Override
            protected List<String> getSatelliteDataOptimizedApps() {
                return PACKAGE_NAMES;
            }
        };
        mController.init(TEST_SUB_ID, mPersistableBundle, true, true);
        PreferenceManager preferenceManager = new PreferenceManager(mContext);
        PreferenceScreen preferenceScreen = preferenceManager.createPreferenceScreen(mContext);
        PreferenceCategory category = new PreferenceCategory(mContext);
        category.setKey(mController.getPreferenceKey());
        preferenceScreen.addPreference(category);

        mController.displayPreference(preferenceScreen);

        assertThat(category.getPreferenceCount() == MAXIMUM_OF_PREFERENCE_AMOUNT).isTrue();
    }


    @Test
    @EnableFlags(Flags.FLAG_SATELLITE_25Q4_APIS)
    public void getAvailabilityStatus_hasSatSupportedApps_returnAvailable() {
        mController = new SatelliteAppListCategoryController(mContext, KEY) {
            @Override
            protected boolean isSatelliteEligible() {
                return true;
            }

            @Override
            protected List<String> getSatelliteDataOptimizedApps() {
                return PACKAGE_NAMES;
            }
        };
        mController.init(TEST_SUB_ID, mPersistableBundle, true, true);

        int result = mController.getAvailabilityStatus(TEST_SUB_ID);

        assertThat(result).isEqualTo(AVAILABLE_UNSEARCHABLE);
    }

    @Test
    @EnableFlags(Flags.FLAG_SATELLITE_25Q4_APIS)
    public void getAvailabilityStatus_noSatSupportedApps_returnUnavailable() {
        mController = new SatelliteAppListCategoryController(mContext, KEY) {
            @Override
            protected boolean isSatelliteEligible() {
                return true;
            }

            @Override
            protected List<String> getSatelliteDataOptimizedApps() {
                return List.of();
            }
        };
        mController.init(TEST_SUB_ID, mPersistableBundle, true, true);

        int result = mController.getAvailabilityStatus(TEST_SUB_ID);

        assertThat(result).isEqualTo(CONDITIONALLY_UNAVAILABLE);
    }

    @Test
    @EnableFlags(Flags.FLAG_SATELLITE_25Q4_APIS)
    public void getAvailabilityStatus_dataUnavailable_returnUnavailable() {
        mController = new SatelliteAppListCategoryController(mContext, KEY) {
            @Override
            protected boolean isSatelliteEligible() {
                return true;
            }

            @Override
            protected List<String> getSatelliteDataOptimizedApps() {
                return PACKAGE_NAMES;
            }
        };
        mController.init(TEST_SUB_ID, mPersistableBundle, true, false);

        int result = mController.getAvailabilityStatus(TEST_SUB_ID);

        assertThat(result).isEqualTo(CONDITIONALLY_UNAVAILABLE);
    }

    @Test
    @EnableFlags(Flags.FLAG_SATELLITE_25Q4_APIS)
    public void getAvailabilityStatus_entitlementSupportedButAccountIneligible_returnUnavailable() {
        mPersistableBundle.putBoolean(KEY_SATELLITE_ENTITLEMENT_SUPPORTED_BOOL, true);
        mController = new SatelliteAppListCategoryController(mContext, KEY) {
            @Override
            protected boolean isSatelliteEligible() {
                return false;
            }

            @Override
            protected List<String> getSatelliteDataOptimizedApps() {
                return PACKAGE_NAMES;
            }
        };
        mController.init(TEST_SUB_ID, mPersistableBundle, true, true);

        int result = mController.getAvailabilityStatus(TEST_SUB_ID);

        assertThat(result).isEqualTo(CONDITIONALLY_UNAVAILABLE);
    }

    @Test
    @EnableFlags(Flags.FLAG_SATELLITE_25Q4_APIS)
    public void getAvailabilityStatus_entitlementSupportedAndAccountEligible_returnAvailable() {
        mPersistableBundle.putBoolean(KEY_SATELLITE_ENTITLEMENT_SUPPORTED_BOOL, true);
        mController = new SatelliteAppListCategoryController(mContext, KEY) {
            @Override
            protected boolean isSatelliteEligible() {
                return true;
            }

            @Override
            protected List<String> getSatelliteDataOptimizedApps() {
                return PACKAGE_NAMES;
            }
        };
        mController.init(TEST_SUB_ID, mPersistableBundle, true, true);

        int result = mController.getAvailabilityStatus(TEST_SUB_ID);

        assertThat(result).isEqualTo(AVAILABLE_UNSEARCHABLE);
    }
}
