/*
 * Copyright 2018 The Android Open Source Project
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
package com.android.settings.bluetooth;

import static com.android.settingslib.Utils.isAudioModeOngoingCall;

import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.util.Log;

import androidx.preference.Preference;

import com.android.settings.connecteddevice.DevicePreferenceCallback;
import com.android.settingslib.bluetooth.BluetoothUtils;
import com.android.settingslib.bluetooth.CachedBluetoothDevice;
import com.android.settingslib.bluetooth.LocalBluetoothManager;
import com.android.settingslib.utils.ThreadUtils;

import java.util.concurrent.atomic.AtomicBoolean;

/** Controller to maintain available media Bluetooth devices */
public class AvailableMediaBluetoothDeviceUpdater extends BluetoothDeviceUpdater
        implements Preference.OnPreferenceClickListener {

    private static final String TAG = "AvailableMediaBluetoothDeviceUpdater";

    private static final String PREF_KEY_PREFIX = "available_media_bt_";
    private final LocalBluetoothManager mLocalBtManager;
    private AtomicBoolean mIsOngoingCall = new AtomicBoolean(false);

    public AvailableMediaBluetoothDeviceUpdater(
            Context context,
            DevicePreferenceCallback devicePreferenceCallback,
            int metricsCategory) {
        super(context, devicePreferenceCallback, metricsCategory);
        mLocalBtManager = Utils.getLocalBtManager(context);
        var unused =
                ThreadUtils.postOnBackgroundThread(
                        () -> mIsOngoingCall.set(isAudioModeOngoingCall(mContext)));
    }

    /**
     * Set if the device is in ongoing call mode.
     *
     * <p>This should be set whe the activity is onStart and when audio mode is changed.
     */
    public void setIsOngoingCall(boolean isOngoingCall) {
        mIsOngoingCall.set(isOngoingCall);
    }

    @Override
    public boolean isFilterMatched(CachedBluetoothDevice cachedDevice) {
        final int currentAudioProfile;

        if (mIsOngoingCall.get()) {
            // in phone call
            currentAudioProfile = BluetoothProfile.HEADSET;
        } else {
            // without phone call
            currentAudioProfile = BluetoothProfile.A2DP;
        }

        boolean isFilterMatched = false;
        if (isDeviceConnected(cachedDevice) && isDeviceInCachedDevicesList(cachedDevice)) {
            Log.d(TAG, "isFilterMatched() current audio profile : " + currentAudioProfile);
            String deviceName = cachedDevice.getName();

            // If device is LE Audio, it is compatible with HFP and A2DP.
            // It would show in Available Devices group if the audio sharing flag is disabled or
            // the device is not in the audio sharing session.
            if (cachedDevice.isConnectedLeAudioDevice()
                    || cachedDevice.hasConnectedLeAudioMemberDevice()) {
                if (BluetoothUtils.isAudioSharingUIAvailable(mContext)
                        && BluetoothUtils.hasConnectedBroadcastSource(
                                cachedDevice, mLocalBtManager)) {
                    Log.d(
                            TAG,
                            "isFilterMatched() device : "
                                    + deviceName
                                    + ", isFilterMatched : false, in audio sharing.");
                    return false;
                } else {
                    Log.d(
                            TAG,
                            "isFilterMatched() device : "
                                    + deviceName
                                    + ", isFilterMatched : true, the LEA profile is connected.");
                    return true;
                }
            }

            // If device is Hearing Aid, it is compatible with HFP and A2DP.
            // It would show in Available Devices group.
            if (cachedDevice.isConnectedAshaHearingAidDevice()) {
                Log.d(
                        TAG,
                        "isFilterMatched() device : "
                                + deviceName
                                + ", isFilterMatched : true, the HA profile is connected.");
                return true;
            }

            // According to the current audio profile type,
            // this page will show the bluetooth device that have corresponding profile.
            // For example:
            // If current audio profile is a2dp, show the bluetooth device that have a2dp profile.
            // If current audio profile is headset,
            // show the bluetooth device that have headset profile.
            switch (currentAudioProfile) {
                case BluetoothProfile.A2DP:
                    isFilterMatched = cachedDevice.isConnectedA2dpDevice();
                    break;
                case BluetoothProfile.HEADSET:
                    isFilterMatched = cachedDevice.isConnectedHfpDevice();
                    break;
            }
            Log.d(
                    TAG,
                    "isFilterMatched() device : "
                            + deviceName
                            + ", isFilterMatched : "
                            + isFilterMatched);
        }
        return isFilterMatched;
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        mMetricsFeatureProvider.logClickedPreference(preference, mMetricsCategory);
        var unused =
                ThreadUtils.postOnBackgroundThread(
                        () -> mDevicePreferenceCallback.onDeviceClick(preference));
        return true;
    }

    @Override
    protected String getPreferenceKeyPrefix() {
        return PREF_KEY_PREFIX;
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    @Override
    protected void update(CachedBluetoothDevice cachedBluetoothDevice) {
        super.update(cachedBluetoothDevice);
        Log.d(TAG, "Map : " + mPreferenceMap);
    }
}
