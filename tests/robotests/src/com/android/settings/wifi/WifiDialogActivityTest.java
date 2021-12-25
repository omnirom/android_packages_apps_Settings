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

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.doReturn;

import android.content.Intent;
import android.net.wifi.WifiConfiguration;

import androidx.lifecycle.Lifecycle.State;
import androidx.test.core.app.ActivityScenario;

import com.android.settings.R;
import com.android.settings.testutils.shadow.ShadowAlertDialogCompat;
import com.android.settings.testutils.shadow.ShadowConnectivityManager;
import com.android.settings.testutils.shadow.ShadowNetworkDetailsTracker;
import com.android.settings.testutils.shadow.ShadowWifiManager;

import com.google.android.setupcompat.util.WizardManagerHelper;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.util.ReflectionHelpers;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {
        ShadowAlertDialogCompat.class,
        ShadowConnectivityManager.class,
        ShadowNetworkDetailsTracker.class,
        ShadowWifiManager.class
})
public class WifiDialogActivityTest {

    private static final String AP1_SSID = "\"ap1\"";
    @Mock
    private WifiConfigController mController;
    @Mock
    private WifiConfigController2 mController2;

    private ActivityScenario<WifiDialogActivity> mWifiDialogActivity;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        WifiConfiguration wifiConfig = new WifiConfiguration();
        wifiConfig.SSID = AP1_SSID;
        doReturn(wifiConfig).when(mController).getConfig();
        doReturn(wifiConfig).when(mController2).getConfig();
    }

    @After
    public void cleanUp() {
        if (mWifiDialogActivity != null) {
            mWifiDialogActivity.close();
        }
    }

    private ActivityScenario<WifiDialogActivity> createTargetActivity(Intent activityIntent) {
        return ActivityScenario.launch(activityIntent);
    }

    @Test
    public void onSubmit_shouldConnectToNetwork() {
        WifiDialogActivity activity = Robolectric.setupActivity(WifiDialogActivity.class);
        WifiDialog dialog = (WifiDialog) ShadowAlertDialogCompat.getLatestAlertDialog();
        assertThat(dialog).isNotNull();

        ReflectionHelpers.setField(dialog, "mController", mController);

        activity.onSubmit(dialog);

        assertThat(ShadowWifiManager.get().savedWifiConfig.SSID).isEqualTo(AP1_SSID);
    }

    @Test
    @Ignore
    public void onSubmit2_whenConnectForCallerIsTrue_shouldConnectToNetwork() {
        final Intent intent = new Intent("com.android.settings.WIFI_DIALOG");
        intent.putExtra(WifiDialogActivity.KEY_CHOSEN_WIFIENTRY_KEY, "FAKE_KEY");
        intent.putExtra(WifiDialogActivity.KEY_CONNECT_FOR_CALLER, true);
        mWifiDialogActivity = createTargetActivity(intent);

        mWifiDialogActivity.moveToState(State.CREATED);
        mWifiDialogActivity.moveToState(State.STARTED);

        WifiDialog2 dialog = (WifiDialog2) ShadowAlertDialogCompat.getLatestAlertDialog();
        assertThat(dialog).isNotNull();

        ReflectionHelpers.setField(dialog, "mController", mController2);

        mWifiDialogActivity.onActivity(activity -> {
            activity.onSubmit(dialog);
            assertThat(ShadowWifiManager.get().savedWifiConfig.SSID).isEqualTo(AP1_SSID);
        });
    }

    @Test
    public void onSubmit_whenConnectForCallerIsFalse_shouldNotConnectToNetwork() {
        WifiDialogActivity activity =
                Robolectric.buildActivity(
                        WifiDialogActivity.class,
                        new Intent().putExtra(WifiDialogActivity.KEY_CONNECT_FOR_CALLER, false))
                        .setup().get();
        WifiDialog dialog = (WifiDialog) ShadowAlertDialogCompat.getLatestAlertDialog();

        assertThat(dialog).isNotNull();

        ReflectionHelpers.setField(dialog, "mController", mController);

        activity.onSubmit(dialog);

        assertThat(ShadowWifiManager.get().savedWifiConfig).isNull();
    }

    @Test
    @Ignore
    public void onSubmit2_whenConnectForCallerIsFalse_shouldNotConnectToNetwork() {
        final Intent intent = new Intent("com.android.settings.WIFI_DIALOG");
        intent.putExtra(WifiDialogActivity.KEY_CHOSEN_WIFIENTRY_KEY, "FAKE_KEY");
        intent.putExtra(WifiDialogActivity.KEY_CONNECT_FOR_CALLER, false);
        mWifiDialogActivity = createTargetActivity(intent);

        mWifiDialogActivity.moveToState(State.CREATED);
        mWifiDialogActivity.moveToState(State.STARTED);

        WifiDialog2 dialog = (WifiDialog2) ShadowAlertDialogCompat.getLatestAlertDialog();
        assertThat(dialog).isNotNull();

        ReflectionHelpers.setField(dialog, "mController", mController2);

        mWifiDialogActivity.onActivity(activity -> {
            activity.onSubmit(dialog);
            assertThat(ShadowWifiManager.get().savedWifiConfig).isEqualTo(null);
        });
    }

    @Test
    public void onSubmit_whenLaunchInSetupFlow_shouldBeLightThemeForWifiDialog() {
        WifiDialogActivity activity =
                Robolectric.buildActivity(
                        WifiDialogActivity.class,
                        new Intent()
                                .putExtra(WifiDialogActivity.KEY_CONNECT_FOR_CALLER, false)
                                .putExtra(WizardManagerHelper.EXTRA_IS_FIRST_RUN, true)
                                .putExtra(WizardManagerHelper.EXTRA_IS_SETUP_FLOW, true))
                        .setup().get();
        WifiDialog dialog = (WifiDialog) ShadowAlertDialogCompat.getLatestAlertDialog();

        assertThat(dialog).isNotNull();

        activity.onSubmit(dialog);

        assertThat(dialog.getContext().getThemeResId())
                .isEqualTo(R.style.SuwAlertDialogThemeCompat_Light);
    }
}
