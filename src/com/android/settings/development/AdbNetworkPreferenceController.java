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

package com.android.settings.development;

import android.content.ContentResolver;
import android.content.Context;
import android.net.NetworkUtils;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.UserManager;
import android.provider.Settings;
import androidx.annotation.Nullable;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;
import androidx.preference.TwoStatePreference;
import android.text.TextUtils;

import com.android.settings.R;
import com.android.settingslib.core.ConfirmationDialogController;
import com.android.settingslib.development.DeveloperOptionsPreferenceController;

import java.net.InetAddress;

public class AdbNetworkPreferenceController extends DeveloperOptionsPreferenceController implements
        ConfirmationDialogController {
    private static final String KEY_ENABLE_ADB_NETWORK = "enable_adb_network";

    public static final int ADB_NETWORK_SETTING_ON = 5555;
    public static final int ADB_NETWORK_SETTING_OFF = 0;

    private final DevelopmentSettingsDashboardFragment mFragment;

    public AdbNetworkPreferenceController(Context context, DevelopmentSettingsDashboardFragment fragment) {
        super(context);
        mFragment = fragment;
    }

    @Override
    public String getPreferenceKey() {
        return KEY_ENABLE_ADB_NETWORK;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        if (isAvailable()) {
            mPreference = (TwoStatePreference) screen.findPreference(KEY_ENABLE_ADB_NETWORK);
        }
    }

    @Override
    public boolean isAvailable() {
        final UserManager um = mContext.getSystemService(UserManager.class);
        return um != null && (um.isAdminUser() || um.isDemoUser());
    }

    private boolean isAdbEnabled() {
        final ContentResolver cr = mContext.getContentResolver();
        return Settings.Global.getInt(cr, Settings.Global.OMNI_ADB_PORT, ADB_NETWORK_SETTING_OFF)
                != ADB_NETWORK_SETTING_OFF;
    }

    protected void writeAdbSetting(boolean enabled) {
        Settings.Global.putInt(mContext.getContentResolver(),
                Settings.Global.OMNI_ADB_PORT, enabled ? ADB_NETWORK_SETTING_ON : ADB_NETWORK_SETTING_OFF);
        updateState(mPreference);
    }

    @Override
    public void updateState(Preference preference) {
        ((TwoStatePreference) preference).setChecked(isAdbEnabled());
        if (isAdbEnabled()) {
            WifiManager wifiManager = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);
            WifiInfo wifiInfo = wifiManager.getConnectionInfo();
            if (wifiInfo != null) {
                // if wifiInfo is not null, set the label to "hostAddress"
                InetAddress address = NetworkUtils.intToInetAddress(wifiInfo.getIpAddress());
                ((TwoStatePreference) preference).setSummary(address.getHostAddress());
            }
        } else {
            ((TwoStatePreference) preference).setSummary(mContext.getResources().getString(R.string.adb_over_network_summary));
        }
    }

    public void onAdbDialogConfirmed() {
        writeAdbSetting(true);
    }

    public void onAdbDialogDismissed() {
        updateState(mPreference);
    }

    @Override
    public void showConfirmationDialog(@Nullable Preference preference) {
        EnableAdbNetworkWarningDialog.show(mFragment);
    }

    @Override
    public void dismissConfirmationDialog() {
        // intentional no-op
    }

    @Override
    public boolean isConfirmationDialogShowing() {
        // intentional no-op
        return false;
    }

    @Override
    protected void onDeveloperOptionsSwitchDisabled() {
        super.onDeveloperOptionsSwitchDisabled();
        writeAdbSetting(false);
    }

    @Override
    public boolean handlePreferenceTreeClick(Preference preference) {
        if (TextUtils.equals(KEY_ENABLE_ADB_NETWORK, preference.getKey())) {
            if (!isAdbEnabled()) {
                showConfirmationDialog(preference);
            } else {
                writeAdbSetting(false);
            }
            return true;
        } else {
            return false;
        }
    }
}
