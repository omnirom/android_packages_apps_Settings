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
import static android.telephony.CarrierConfigManager.KEY_SATELLITE_INFORMATION_REDIRECT_URL_STRING;

import static com.android.settings.network.telephony.satellite.SatelliteSettingFooterController.KEY_FOOTER_PREFERENCE;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
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
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
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
    @Mock
    FooterPreference mFooterPreference;

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
        when(mFooterPreference.getKey()).thenReturn(KEY_FOOTER_PREFERENCE);
        screen.addPreference(mFooterPreference);
        mController.init(TEST_SUB_ID, mPersistableBundle);

        mController.displayPreference(screen);

        ArgumentCaptor<CharSequence> summary = ArgumentCaptor.forClass(CharSequence.class);
        verify(mFooterPreference).setSummary(summary.capture());
        assertThat(
                summary.getValue().toString().contains(ResourcesUtils.getResourcesString(mContext,
                        "satellite_footer_content_section_0"))).isTrue();
        assertThat(
                summary.getValue().toString().contains(ResourcesUtils.getResourcesString(mContext,
                        "satellite_footer_content_section_1"))).isTrue();
        assertThat(
                summary.getValue().toString().contains(ResourcesUtils.getResourcesString(mContext,
                        "satellite_footer_content_section_2"))).isTrue();
        assertThat(
                summary.getValue().toString().contains(ResourcesUtils.getResourcesString(mContext,
                        "satellite_footer_content_section_3"))).isTrue();
        assertThat(
                summary.getValue().toString().contains(ResourcesUtils.getResourcesString(mContext,
                        "satellite_footer_content_section_4"))).isTrue();
    }


    @Test
    public void displayPreferenceScreen_manualTypeAndNoEntitlement() {
        mPersistableBundle.putInt(KEY_CARRIER_ROAMING_NTN_CONNECT_TYPE_INT,
                CARRIER_ROAMING_NTN_CONNECT_MANUAL);
        mPersistableBundle.putBoolean(KEY_SATELLITE_ENTITLEMENT_SUPPORTED_BOOL, false);

        PreferenceScreen screen = new PreferenceManager(mContext).createPreferenceScreen(mContext);
        when(mFooterPreference.getKey()).thenReturn(KEY_FOOTER_PREFERENCE);
        screen.addPreference(mFooterPreference);
        mController.init(TEST_SUB_ID, mPersistableBundle);

        mController.displayPreference(screen);

        ArgumentCaptor<CharSequence> summary = ArgumentCaptor.forClass(CharSequence.class);
        verify(mFooterPreference).setSummary(summary.capture());

        assertThat(
                summary.getValue().toString().contains(ResourcesUtils.getResourcesString(mContext,
                        "satellite_footer_content_section_7", TEST_OPERATOR_NAME))).isTrue();
        assertThat(
                summary.getValue().toString().contains(ResourcesUtils.getResourcesString(mContext,
                        "satellite_footer_content_section_5"))).isFalse();
        assertThat(
                summary.getValue().toString().contains(ResourcesUtils.getResourcesString(mContext,
                        "satellite_footer_content_section_6"))).isFalse();
    }

    @Test
    public void displayPreferenceScreen_autoTypeAndNoEntitlement() {
        mPersistableBundle.putInt(KEY_CARRIER_ROAMING_NTN_CONNECT_TYPE_INT,
                CARRIER_ROAMING_NTN_CONNECT_AUTOMATIC);
        mPersistableBundle.putBoolean(KEY_SATELLITE_ENTITLEMENT_SUPPORTED_BOOL, false);

        PreferenceScreen screen = new PreferenceManager(mContext).createPreferenceScreen(mContext);
        when(mFooterPreference.getKey()).thenReturn(KEY_FOOTER_PREFERENCE);
        screen.addPreference(mFooterPreference);
        mController.init(TEST_SUB_ID, mPersistableBundle);

        mController.displayPreference(screen);

        ArgumentCaptor<CharSequence> summary = ArgumentCaptor.forClass(CharSequence.class);
        verify(mFooterPreference).setSummary(summary.capture());

        assertThat(
                summary.getValue().toString().contains(ResourcesUtils.getResourcesString(mContext,
                        "satellite_footer_content_section_7", TEST_OPERATOR_NAME))).isFalse();
        assertThat(
                summary.getValue().toString().contains(ResourcesUtils.getResourcesString(mContext,
                        "satellite_footer_content_section_5"))).isTrue();
        assertThat(
                summary.getValue().toString().contains(ResourcesUtils.getResourcesString(mContext,
                        "satellite_footer_content_section_6"))).isTrue();
    }

    @Test
    public void displayPreferenceScreen_autoTypeAndHasEntitlement() {
        mPersistableBundle.putInt(KEY_CARRIER_ROAMING_NTN_CONNECT_TYPE_INT,
                CARRIER_ROAMING_NTN_CONNECT_AUTOMATIC);
        mPersistableBundle.putBoolean(KEY_SATELLITE_ENTITLEMENT_SUPPORTED_BOOL, true);

        PreferenceScreen screen = new PreferenceManager(mContext).createPreferenceScreen(mContext);
        when(mFooterPreference.getKey()).thenReturn(KEY_FOOTER_PREFERENCE);
        screen.addPreference(mFooterPreference);
        mController.init(TEST_SUB_ID, mPersistableBundle);

        mController.displayPreference(screen);

        ArgumentCaptor<CharSequence> summary = ArgumentCaptor.forClass(CharSequence.class);
        verify(mFooterPreference).setSummary(summary.capture());

        assertThat(
                summary.getValue().toString().contains(ResourcesUtils.getResourcesString(mContext,
                        "satellite_footer_content_section_5"))).isTrue();
        assertThat(
                summary.getValue().toString().contains(ResourcesUtils.getResourcesString(mContext,
                        "satellite_footer_content_section_6"))).isFalse();
        assertThat(
                summary.getValue().toString().contains(ResourcesUtils.getResourcesString(mContext,
                        "satellite_footer_content_section_7", TEST_OPERATOR_NAME))).isTrue();
    }
}
