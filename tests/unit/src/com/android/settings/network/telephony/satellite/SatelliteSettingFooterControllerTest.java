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

import static android.telephony.CarrierConfigManager.KEY_EMERGENCY_MESSAGING_SUPPORTED_BOOL;
import static android.telephony.CarrierConfigManager.KEY_SATELLITE_ENTITLEMENT_SUPPORTED_BOOL;
import static android.telephony.CarrierConfigManager.KEY_SATELLITE_INFORMATION_REDIRECT_URL_STRING;

import static com.android.settings.network.telephony.satellite.SatelliteSettingFooterController.KEY_FOOTER_PREFERENCE;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.os.Looper;
import android.os.PersistableBundle;
import android.telephony.TelephonyManager;

import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;
import androidx.test.core.app.ApplicationProvider;

import com.android.settings.testutils.ResourcesUtils;
import com.android.settingslib.widget.FooterPreference;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

public class SatelliteSettingFooterControllerTest {
    private static final int TEST_SUB_ID = 5;
    private static final String TEST_OPERATOR_NAME = "test_operator_name";

    @Rule
    public final MockitoRule mMockitoRule = MockitoJUnit.rule();

    @Mock
    private TelephonyManager mTelephonyManager;

    private Context mContext;
    private SatelliteSettingFooterController mController;
    private final PersistableBundle mPersistableBundle = new PersistableBundle();

    @Before
    public void setUp() {
        if (Looper.myLooper() == null) {
            Looper.prepare();
        }
        mContext = spy(ApplicationProvider.getApplicationContext());
        mController = new SatelliteSettingFooterController(mContext,
                KEY_FOOTER_PREFERENCE);
        when(mContext.getSystemService(TelephonyManager.class)).thenReturn(mTelephonyManager);
        when(mTelephonyManager.getSimOperatorName(TEST_SUB_ID)).thenReturn(TEST_OPERATOR_NAME);
        mPersistableBundle.putString(KEY_SATELLITE_INFORMATION_REDIRECT_URL_STRING, "");
    }

    @Test
    public void displayPreferenceScreen_updateContent_hasBasicContent() {
        PreferenceScreen screen = new PreferenceManager(mContext).createPreferenceScreen(mContext);
        FooterPreference preference = new FooterPreference(mContext);
        preference.setKey(KEY_FOOTER_PREFERENCE);
        screen.addPreference(preference);
        mController.init(TEST_SUB_ID, mPersistableBundle);

        mController.displayPreference(screen);
        String summary = preference.getSummary().toString();

        assertThat(summary.contains(ResourcesUtils.getResourcesString(mContext,
                "satellite_footer_content_section_0"))).isTrue();
        assertThat(summary.contains(ResourcesUtils.getResourcesString(mContext,
                "satellite_footer_content_section_1"))).isTrue();
        assertThat(summary.contains(ResourcesUtils.getResourcesString(mContext,
                "satellite_footer_content_section_2"))).isTrue();
        assertThat(summary.contains(ResourcesUtils.getResourcesString(mContext,
                "satellite_footer_content_section_3"))).isTrue();
        assertThat(summary.contains(ResourcesUtils.getResourcesString(mContext,
                "satellite_footer_content_section_4"))).isTrue();
        assertThat(summary.contains(ResourcesUtils.getResourcesString(mContext,
                "satellite_footer_content_section_5"))).isTrue();
    }

    @Test
    public void displayPreferenceScreen_noEmergencyMsgSupport_hasEmergencyContent() {
        mPersistableBundle.putBoolean(KEY_EMERGENCY_MESSAGING_SUPPORTED_BOOL, false);
        PreferenceScreen screen = new PreferenceManager(mContext).createPreferenceScreen(mContext);
        FooterPreference preference = new FooterPreference(mContext);
        preference.setKey(KEY_FOOTER_PREFERENCE);
        screen.addPreference(preference);
        mController.init(TEST_SUB_ID, mPersistableBundle);

        mController.displayPreference(screen);
        String summary = preference.getSummary().toString();

        assertThat(summary.contains(ResourcesUtils.getResourcesString(mContext,
                "satellite_footer_content_section_6"))).isTrue();
    }

    @Test
    @Ignore("b/405279842")
    public void displayPreferenceScreen_emergencyMsgSupport_noEmergencyContent() {
        mPersistableBundle.putBoolean(KEY_EMERGENCY_MESSAGING_SUPPORTED_BOOL, true);
        PreferenceScreen screen = new PreferenceManager(mContext).createPreferenceScreen(mContext);
        FooterPreference preference = new FooterPreference(mContext);
        preference.setKey(KEY_FOOTER_PREFERENCE);
        screen.addPreference(preference);
        mController.init(TEST_SUB_ID, mPersistableBundle);

        mController.displayPreference(screen);
        String summary = preference.getSummary().toString();

        assertThat(summary.contains(ResourcesUtils.getResourcesString(mContext,
                "satellite_footer_content_section_6"))).isFalse();
    }

    @Test
    public void displayPreferenceScreen_entitlementSupport_hasEntitlementContent() {
        mPersistableBundle.putBoolean(KEY_SATELLITE_ENTITLEMENT_SUPPORTED_BOOL, true);
        PreferenceScreen screen = new PreferenceManager(mContext).createPreferenceScreen(mContext);
        FooterPreference preference = new FooterPreference(mContext);
        preference.setKey(KEY_FOOTER_PREFERENCE);
        screen.addPreference(preference);
        mController.init(TEST_SUB_ID, mPersistableBundle);

        mController.displayPreference(screen);
        String summary = preference.getSummary().toString();

        assertThat(summary.contains(TEST_OPERATOR_NAME)).isTrue();
    }

    @Test
    public void displayPreferenceScreen_entitlementNotSupport_noEntitlementContent() {
        mPersistableBundle.putBoolean(KEY_SATELLITE_ENTITLEMENT_SUPPORTED_BOOL, false);
        PreferenceScreen screen = new PreferenceManager(mContext).createPreferenceScreen(mContext);
        FooterPreference preference = new FooterPreference(mContext);
        preference.setKey(KEY_FOOTER_PREFERENCE);
        screen.addPreference(preference);
        mController.init(TEST_SUB_ID, mPersistableBundle);

        mController.displayPreference(screen);
        String summary = preference.getSummary().toString();

        assertThat(summary.contains(TEST_OPERATOR_NAME)).isFalse();
    }
}
