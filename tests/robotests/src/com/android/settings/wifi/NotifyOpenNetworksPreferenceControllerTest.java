/*
 * Copyright (C) 2017 The Android Open Source Project
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

package com.android.settings.wifi;

import static android.provider.Settings.Global.WIFI_NETWORKS_AVAILABLE_NOTIFICATION_ON;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.net.wifi.WifiManager;
import android.platform.test.annotations.DisableFlags;
import android.platform.test.annotations.EnableFlags;
import android.platform.test.flag.junit.SetFlagsRule;
import android.provider.Settings;

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;
import androidx.preference.SwitchPreference;

import com.google.common.util.concurrent.MoreExecutors;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.function.Consumer;

@RunWith(RobolectricTestRunner.class)
public class NotifyOpenNetworksPreferenceControllerTest {

    @Rule
    public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();
    @Mock
    private WifiManager mWifiManager;
    @Mock
    private PreferenceScreen mPreferenceScreen;
    @Mock
    private Preference mPreference;
    private Context mContext;
    private NotifyOpenNetworksPreferenceController mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.getApplication();
        mController = new NotifyOpenNetworksPreferenceController(mContext);
        mController.mWifiManager = mWifiManager;
        // Mock the executors as direct executors so that scheduled tasks will be dispatched
        // immediately to enabled the verifications of those tasks.
        mController.mBackgroundExecutor = MoreExecutors.newDirectExecutorService();
        mController.mUiExecutor = MoreExecutors.directExecutor();
        when(mPreferenceScreen.findPreference("notify_open_networks")).thenReturn(mPreference);
    }

    @Test
    public void testIsAvailable_shouldAlwaysReturnTrue() {
        assertThat(mController.isAvailable()).isTrue();
    }

    @Test
    @DisableFlags({com.android.settings.connectivity.Flags.FLAG_WIFI_MULTIUSER,
            com.android.wifi.flags.Flags.FLAG_MULTI_USER_WIFI_ENHANCEMENT})
    public void setChecked_withTrue_shouldUpdateSetting() {
        Settings.Global.putInt(mContext.getContentResolver(),
                WIFI_NETWORKS_AVAILABLE_NOTIFICATION_ON, 0);

        mController.setChecked(true);

        assertThat(Settings.Global.getInt(mContext.getContentResolver(),
                WIFI_NETWORKS_AVAILABLE_NOTIFICATION_ON, 0))
                .isEqualTo(1);
    }

    @Test
    @DisableFlags({com.android.settings.connectivity.Flags.FLAG_WIFI_MULTIUSER,
            com.android.wifi.flags.Flags.FLAG_MULTI_USER_WIFI_ENHANCEMENT})
    public void setChecked_withFalse_shouldUpdateSetting() {
        Settings.Global.putInt(mContext.getContentResolver(),
                WIFI_NETWORKS_AVAILABLE_NOTIFICATION_ON, 1);

        mController.setChecked(false);

        assertThat(Settings.Global.getInt(mContext.getContentResolver(),
                WIFI_NETWORKS_AVAILABLE_NOTIFICATION_ON, 0))
                .isEqualTo(0);
    }

    @Test
    @EnableFlags({com.android.settings.connectivity.Flags.FLAG_WIFI_MULTIUSER,
            com.android.wifi.flags.Flags.FLAG_MULTI_USER_WIFI_ENHANCEMENT})
    public void testSetChecked_withMultiUserWifiSupport_shouldUpdateThroughWifiManager() {
        mController.setChecked(true);
        verify(mWifiManager).setOpenNetworkNotifierEnabled(true);

        mController.setChecked(false);
        verify(mWifiManager).setOpenNetworkNotifierEnabled(false);
    }

    @Test
    @EnableFlags({com.android.settings.connectivity.Flags.FLAG_WIFI_MULTIUSER,
            com.android.wifi.flags.Flags.FLAG_MULTI_USER_WIFI_ENHANCEMENT})
    public void testDisplayPreference_withMultiUserWifiSupport_shouldFetchThroughWifiManager() {
        ArgumentCaptor<Consumer> resultCallbackCaptor = ArgumentCaptor.forClass(Consumer.class);
        mController.displayPreference(mPreferenceScreen);
        verify(mWifiManager).isOpenNetworkNotifierEnabled(any(), resultCallbackCaptor.capture());
        Consumer<Boolean> resultCallback = resultCallbackCaptor.getValue();

        // Before the callback from WifiManager finishes, isChecked should return false by default.
        assertFalse(mController.isChecked());

        resultCallback.accept(true);
        assertTrue(mController.isChecked());

        resultCallback.accept(false);
        assertFalse(mController.isChecked());
    }

    @Test
    @EnableFlags({com.android.settings.connectivity.Flags.FLAG_WIFI_MULTIUSER,
            com.android.wifi.flags.Flags.FLAG_MULTI_USER_WIFI_ENHANCEMENT})
    public void testOnResume_withMultiUserWifiSupport_shouldFetchThroughWifiManager() {
        mController.onResume();
        verify(mWifiManager).isOpenNetworkNotifierEnabled(any(), any());
    }

    @Test
    @DisableFlags({com.android.settings.connectivity.Flags.FLAG_WIFI_MULTIUSER,
            com.android.wifi.flags.Flags.FLAG_MULTI_USER_WIFI_ENHANCEMENT})
    public void updateState_preferenceSetCheckedWhenSettingsAreEnabled() {
        final SwitchPreference preference = mock(SwitchPreference.class);
        Settings.Global.putInt(mContext.getContentResolver(),
                WIFI_NETWORKS_AVAILABLE_NOTIFICATION_ON, 1);

        mController.updateState(preference);

        verify(preference).setChecked(true);
    }

    @Test
    @DisableFlags({com.android.settings.connectivity.Flags.FLAG_WIFI_MULTIUSER,
            com.android.wifi.flags.Flags.FLAG_MULTI_USER_WIFI_ENHANCEMENT})
    public void updateState_preferenceSetCheckedWhenSettingsAreDisabled() {
        final SwitchPreference preference = mock(SwitchPreference.class);
        Settings.Global.putInt(mContext.getContentResolver(),
                WIFI_NETWORKS_AVAILABLE_NOTIFICATION_ON, 0);

        mController.updateState(preference);

        verify(preference).setChecked(false);
    }
}
