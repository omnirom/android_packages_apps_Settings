/*
 * Copyright (C) 2024 The Android Open Source Project
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

package com.android.settings.connecteddevice.audiosharing.audiostreams;

import static com.android.settings.connecteddevice.audiosharing.audiostreams.AudioStreamsDashboardFragment.KEY_BROADCAST_METADATA;
import static com.android.settingslib.bluetooth.BluetoothBroadcastUtils.SCHEME_BT_BROADCAST_METADATA;

import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.bluetooth.Utils;
import com.android.settings.connecteddevice.audiosharing.AudioSharingUtils;
import com.android.settingslib.bluetooth.BluetoothUtils;
import com.android.settingslib.bluetooth.LocalBluetoothProfileManager;
import com.android.settingslib.widget.SettingsThemeHelper;

public class AudioStreamConfirmDialogActivity extends SettingsActivity
        implements LocalBluetoothProfileManager.ServiceListener {
    private static final String TAG = "AudioStreamConfirmDialogActivity";
    @Nullable private LocalBluetoothProfileManager mProfileManager;
    @Nullable private Bundle mSavedState;
    @Nullable private Intent mIntent;

    @Override
    protected boolean isToolbarEnabled() {
        return false;
    }

    @Override
    protected void onCreate(Bundle savedState) {
        if (!isBroadcastScheme(getIntent())) {
            Log.d(TAG, "onCreate() not broadcast scheme, ignore");
            finish();
        }
        var localBluetoothManager = Utils.getLocalBluetoothManager(this);
        mProfileManager =
                localBluetoothManager == null ? null : localBluetoothManager.getProfileManager();
        super.onCreate(savedState);
    }

    @VisibleForTesting
    static boolean isBroadcastScheme(@Nullable Intent intent) {
        if (intent == null) {
            return false;
        }
        String metadata = intent.getStringExtra(KEY_BROADCAST_METADATA);
        if (metadata != null && !metadata.isEmpty()) {
            return metadata.startsWith(SCHEME_BT_BROADCAST_METADATA);
        }
        String genericData = intent.getDataString();
        if (genericData != null && !genericData.isEmpty()) {
            return genericData.toUpperCase().startsWith(SCHEME_BT_BROADCAST_METADATA);
        }
        return false;
    }

    @Override
    public Resources.Theme getTheme() {
        var theme = super.getTheme();
        theme.applyStyle(
                SettingsThemeHelper.isExpressiveTheme(this)
                        ? R.style.Transparent_Expressive
                        : R.style.Transparent,
                true);
        return theme;
    }

    @Override
    protected void createUiFromIntent(@Nullable Bundle savedState, Intent intent) {
        if (BluetoothUtils.isAudioSharingUIAvailable(this)
                && !isBluetoothUnsupportedOrOff()
                && !AudioSharingUtils.isAudioSharingProfileReady(mProfileManager)) {
            Log.d(TAG, "createUiFromIntent() : supported but not ready, skip createUiFromIntent");
            mSavedState = savedState;
            mIntent = intent;
            return;
        }

        Log.d(
                TAG,
                "createUiFromIntent() : not supported or already connected, starting"
                        + " createUiFromIntent");
        super.createUiFromIntent(savedState, intent);
    }

    @Override
    public void onStart() {
        if (BluetoothUtils.isAudioSharingUIAvailable(this)
                && !isBluetoothUnsupportedOrOff()
                && !AudioSharingUtils.isAudioSharingProfileReady(mProfileManager)) {
            Log.d(TAG, "onStart() : supported but not ready, listen to service ready");
            if (mProfileManager != null) {
                mProfileManager.addServiceListener(this);
            }
        }
        super.onStart();
    }

    @Override
    public void onStop() {
        if (mProfileManager != null) {
            mProfileManager.removeServiceListener(this);
        }
        super.onStop();
    }

    @Override
    public void onServiceConnected() {
        if (BluetoothUtils.isAudioSharingUIAvailable(this)
                && !isBluetoothUnsupportedOrOff()
                && AudioSharingUtils.isAudioSharingProfileReady(mProfileManager)) {
            if (mProfileManager != null) {
                mProfileManager.removeServiceListener(this);
            }
            if (mIntent != null) {
                Log.d(TAG, "onServiceConnected() : service ready, starting createUiFromIntent");
                super.createUiFromIntent(mSavedState, mIntent);
            }
        }
    }

    @Override
    public void onServiceDisconnected() {}

    @Override
    protected boolean isValidFragment(String fragmentName) {
        return AudioStreamConfirmDialog.class.getName().equals(fragmentName);
    }

    private static boolean isBluetoothUnsupportedOrOff() {
        var adapter = BluetoothAdapter.getDefaultAdapter();
        return adapter == null || !adapter.isEnabled();
    }
}
