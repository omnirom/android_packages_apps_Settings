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

import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.provider.Settings;

import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settings.core.TogglePreferenceController;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnPause;
import com.android.settingslib.core.lifecycle.events.OnResume;
import com.android.settingslib.utils.ThreadUtils;

import com.google.common.util.concurrent.ListeningExecutorService;

import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

/**
 * {@link TogglePreferenceController} that controls whether we should notify user when open
 * network is available.
 */
public class NotifyOpenNetworksPreferenceController extends TogglePreferenceController
        implements PreferenceControllerMixin, LifecycleObserver, OnResume, OnPause {

    private static final String KEY_NOTIFY_OPEN_NETWORKS = "notify_open_networks";
    private SettingObserver mSettingObserver;
    @VisibleForTesting
    WifiManager mWifiManager;
    @VisibleForTesting
    ListeningExecutorService mBackgroundExecutor;
    @VisibleForTesting
    Executor mUiExecutor;
    private Consumer<Boolean> mResultCallback;
    private Preference mPreference;
    private boolean mNotifierEnabled = false;

    public NotifyOpenNetworksPreferenceController(Context context) {
        super(context, KEY_NOTIFY_OPEN_NETWORKS);
        if (com.android.settings.connectivity.Flags.wifiMultiuser()
                && com.android.wifi.flags.Flags.multiUserWifiEnhancement()) {
            mWifiManager = context.getSystemService(WifiManager.class);
            mBackgroundExecutor = ThreadUtils.getBackgroundExecutor();
            mUiExecutor = context.getMainExecutor();
            mResultCallback = new Consumer<Boolean>() {
                @Override
                public void accept(Boolean enabled) {
                    mNotifierEnabled = Objects.requireNonNullElse(enabled, false);
                    if (mPreference != null) {
                        updateState(mPreference);
                    }
                }
            };
        }
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        if (com.android.settings.connectivity.Flags.wifiMultiuser()
                && com.android.wifi.flags.Flags.multiUserWifiEnhancement()) {
            mPreference = screen.findPreference(getPreferenceKey());
            mBackgroundExecutor.execute(
                    () -> mWifiManager.isOpenNetworkNotifierEnabled(mUiExecutor, mResultCallback));
        } else {
            mSettingObserver = new SettingObserver(screen.findPreference(getPreferenceKey()));
        }
    }

    @Override
    public void onResume() {
        if (com.android.settings.connectivity.Flags.wifiMultiuser()
                && com.android.wifi.flags.Flags.multiUserWifiEnhancement()) {
            // On resume, fetch setting value from WifiManager once instead of registering a
            // listener on Settings.Global, since WifiManager is now the single source of value
            // storage for this setting.
            mBackgroundExecutor.execute(
                    () -> mWifiManager.isOpenNetworkNotifierEnabled(mUiExecutor, mResultCallback));
        } else if (mSettingObserver != null) {
            mSettingObserver.register(mContext.getContentResolver(), true /* register */);
        }
    }

    @Override
    public void onPause() {
        if (!(com.android.settings.connectivity.Flags.wifiMultiuser()
                && com.android.wifi.flags.Flags.multiUserWifiEnhancement())
                && mSettingObserver != null) {
            mSettingObserver.register(mContext.getContentResolver(), false /* register */);
        }
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }

    @Override
    public boolean isChecked() {
        if (com.android.settings.connectivity.Flags.wifiMultiuser()
                && com.android.wifi.flags.Flags.multiUserWifiEnhancement()) {
            return mNotifierEnabled;
        } else {
            return Settings.Global.getInt(mContext.getContentResolver(),
                    Settings.Global.WIFI_NETWORKS_AVAILABLE_NOTIFICATION_ON, 0) == 1;
        }
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        if (com.android.settings.connectivity.Flags.wifiMultiuser()
                && com.android.wifi.flags.Flags.multiUserWifiEnhancement()) {
            mBackgroundExecutor.execute(
                    () -> mWifiManager.setOpenNetworkNotifierEnabled(isChecked));
        } else {
            Settings.Global.putInt(mContext.getContentResolver(),
                    Settings.Global.WIFI_NETWORKS_AVAILABLE_NOTIFICATION_ON,
                    isChecked ? 1 : 0);
        }
        return true;
    }

    @Override
    public int getSliceHighlightMenuRes() {
        return R.string.menu_key_network;
    }

    /**
     * @deprecated Use {@link WifiManager#isOpenNetworkNotifierEnabled(Executor, Consumer)} instead
     * as the source for setting value, and there's no need to listen to Settings.Global value
     * change. This will no longer be used on devices after B and when the following two flags are
     * enabled: {@link com.android.settings.connectivity.Flags#wifiMultiuser} and
     * {@link com.android.wifi.flags.Flags#multiUserWifiEnhancement}.
     */
    @Deprecated(forRemoval = true)
    class SettingObserver extends ContentObserver {
        private final Uri NETWORKS_AVAILABLE_URI = Settings.Global.getUriFor(
                Settings.Global.WIFI_NETWORKS_AVAILABLE_NOTIFICATION_ON);

        private final Preference mPreference;

        public SettingObserver(Preference preference) {
            super(new Handler());
            mPreference = preference;
        }

        public void register(ContentResolver cr, boolean register) {
            if (register) {
                cr.registerContentObserver(NETWORKS_AVAILABLE_URI, false, this);
            } else {
                cr.unregisterContentObserver(this);
            }
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            super.onChange(selfChange, uri);
            if (NETWORKS_AVAILABLE_URI.equals(uri)) {
                updateState(mPreference);
            }
        }
    }
}
