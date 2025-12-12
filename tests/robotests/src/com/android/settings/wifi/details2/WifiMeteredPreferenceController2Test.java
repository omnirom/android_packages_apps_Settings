/*
 * Copyright (C) 2019 The Android Open Source Project
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
package com.android.settings.wifi.details2;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.net.wifi.WifiConfiguration;
import android.os.UserManager;
import android.platform.test.annotations.DisableFlags;
import android.platform.test.annotations.EnableFlags;
import android.platform.test.flag.junit.SetFlagsRule;

import androidx.preference.DropDownPreference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.connectivity.Flags;
import com.android.wifitrackerlib.WifiEntry;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class WifiMeteredPreferenceController2Test {

    private static final int METERED_OVERRIDE_NONE = 0;
    private static final int METERED_OVERRIDE_METERED = 1;
    private static final int METERED_OVERRIDE_NOT_METERED = 2;

    @Rule
    public final MockitoRule mMockitoRule = MockitoJUnit.rule();
    @Rule
    public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();

    @Mock
    private UserManager mUserManager;
    @Mock
    private WifiEntry mWifiEntry;
    @Mock
    private WifiConfiguration mWifiConfiguration;
    @Mock
    private PreferenceScreen mScreen;
    @Mock
    private DropDownPreference mPreference;

    private WifiMeteredPreferenceController2 mPreferenceController;
    private Context mContext;
    private DropDownPreference mDropDownPreference;

    @Before
    public void setUp() {
        mContext = spy(RuntimeEnvironment.application);

        when(mContext.getSystemService(UserManager.class)).thenReturn(mUserManager);
        mPreferenceController = spy(
                new WifiMeteredPreferenceController2(mContext, mWifiEntry));
        mDropDownPreference = new DropDownPreference(mContext);
        mDropDownPreference.setEntries(R.array.wifi_metered_entries);
        mDropDownPreference.setEntryValues(R.array.wifi_metered_values);
    }

    @Test
    public void testUpdateState_wifiMetered_setCorrectValue() {
        doReturn(METERED_OVERRIDE_METERED).when(mPreferenceController).getMeteredOverride();

        mPreferenceController.updateState(mDropDownPreference);

        assertThat(mDropDownPreference.getEntry()).isEqualTo("Treat as metered");
    }

    @Test
    public void testUpdateState_wifiNotMetered_setCorrectValue() {
        doReturn(METERED_OVERRIDE_NOT_METERED).when(mPreferenceController).getMeteredOverride();

        mPreferenceController.updateState(mDropDownPreference);

        assertThat(mDropDownPreference.getEntry()).isEqualTo("Treat as unmetered");
    }

    @Test
    public void testUpdateState_wifiAuto_setCorrectValue() {
        doReturn(METERED_OVERRIDE_NONE).when(mPreferenceController).getMeteredOverride();

        mPreferenceController.updateState(mDropDownPreference);

        assertThat(mDropDownPreference.getEntry()).isEqualTo("Detect automatically");
    }

    @Test
    @DisableFlags(Flags.FLAG_WIFI_MULTIUSER)
    public void displayPreference_flagDisabled() {
        mPreferenceController.updateState(mDropDownPreference);

        assertThat(mDropDownPreference.isEnabled()).isTrue();
    }

    @Test
    @EnableFlags(Flags.FLAG_WIFI_MULTIUSER)
    public void displayPreference_networkOwned() {
        when(mUserManager.getUserCount()).thenReturn(3);
        when(mWifiEntry.getWifiConfiguration()).thenReturn(mWifiConfiguration);
        mWifiConfiguration.creatorUid = 1;

        mPreferenceController.updateState(mDropDownPreference);

        assertThat(mDropDownPreference.isEnabled()).isTrue();
    }

    @Test
    @EnableFlags(Flags.FLAG_WIFI_MULTIUSER)
    public void displayPreference_networkNotOwned_singleUser() {
        when(mUserManager.getUserCount()).thenReturn(1);
        when(mWifiEntry.getWifiConfiguration()).thenReturn(mWifiConfiguration);
        mWifiConfiguration.creatorUid = Integer.MAX_VALUE;

        mPreferenceController.updateState(mDropDownPreference);

        assertThat(mDropDownPreference.isEnabled()).isTrue();
    }

    @Test
    @EnableFlags(Flags.FLAG_WIFI_MULTIUSER)
    public void displayPreference_networkNotOwned() {
        when(mUserManager.getUserCount()).thenReturn(3);
        when(mWifiEntry.getWifiConfiguration()).thenReturn(mWifiConfiguration);
        mWifiConfiguration.creatorUid = Integer.MAX_VALUE;

        mPreferenceController.updateState(mDropDownPreference);

        assertThat(mDropDownPreference.isEnabled()).isFalse();
    }
}
