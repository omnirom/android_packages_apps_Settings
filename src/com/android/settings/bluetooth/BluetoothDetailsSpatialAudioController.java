/*
 * Copyright (C) 2022 The Android Open Source Project
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

import android.content.Context;
import android.media.AudioDeviceAttributes;
import android.media.AudioDeviceInfo;
import android.media.AudioManager;
import android.media.Spatializer;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceScreen;
import androidx.preference.SwitchPreference;

import com.android.settings.R;
import com.android.settingslib.bluetooth.CachedBluetoothDevice;
import com.android.settingslib.core.lifecycle.Lifecycle;

/**
 * The controller of the Spatial audio setting in the bluetooth detail settings.
 */
public class BluetoothDetailsSpatialAudioController extends BluetoothDetailsController
        implements Preference.OnPreferenceClickListener {

    private static final String TAG = "BluetoothSpatialAudioController";
    private static final String KEY_SPATIAL_AUDIO_GROUP = "spatial_audio_group";
    private static final String KEY_SPATIAL_AUDIO = "spatial_audio";
    private static final String KEY_HEAD_TRACKING = "head_tracking";

    private final Spatializer mSpatializer;

    @VisibleForTesting
    PreferenceCategory mProfilesContainer;
    @VisibleForTesting
    AudioDeviceAttributes mAudioDevice;

    private boolean mIsAvailable;

    public BluetoothDetailsSpatialAudioController(
            Context context,
            PreferenceFragmentCompat fragment,
            CachedBluetoothDevice device,
            Lifecycle lifecycle) {
        super(context, fragment, device, lifecycle);
        AudioManager audioManager = context.getSystemService(AudioManager.class);
        mSpatializer = audioManager.getSpatializer();
        getAvailableDevice();

    }

    @Override
    public boolean isAvailable() {
        return mIsAvailable;
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        SwitchPreference switchPreference = (SwitchPreference) preference;
        String key = switchPreference.getKey();
        if (TextUtils.equals(key, KEY_SPATIAL_AUDIO)) {
            if (switchPreference.isChecked()) {
                mSpatializer.addCompatibleAudioDevice(mAudioDevice);
            } else {
                mSpatializer.removeCompatibleAudioDevice(mAudioDevice);
            }
            refresh();
            return true;
        } else if (TextUtils.equals(key, KEY_HEAD_TRACKING)) {
            mSpatializer.setHeadTrackerEnabled(switchPreference.isChecked(), mAudioDevice);
            return true;
        } else {
            Log.w(TAG, "invalid key name.");
            return false;
        }
    }

    @Override
    public String getPreferenceKey() {
        return KEY_SPATIAL_AUDIO_GROUP;
    }

    @Override
    protected void init(PreferenceScreen screen) {
        mProfilesContainer = screen.findPreference(getPreferenceKey());
        mProfilesContainer.setLayoutResource(R.layout.preference_bluetooth_profile_category);
        refresh();
    }

    @Override
    protected void refresh() {
        SwitchPreference spatialAudioPref = mProfilesContainer.findPreference(KEY_SPATIAL_AUDIO);
        if (spatialAudioPref == null) {
            spatialAudioPref = createSpatialAudioPreference(mProfilesContainer.getContext());
            mProfilesContainer.addPreference(spatialAudioPref);
        }

        boolean isSpatialAudioOn = mSpatializer.getCompatibleAudioDevices().contains(mAudioDevice);
        Log.d(TAG, "refresh() isSpatialAudioOn : " + isSpatialAudioOn);
        spatialAudioPref.setChecked(isSpatialAudioOn);

        SwitchPreference headTrackingPref = mProfilesContainer.findPreference(KEY_HEAD_TRACKING);
        if (headTrackingPref == null) {
            headTrackingPref = createHeadTrackingPreference(mProfilesContainer.getContext());
            mProfilesContainer.addPreference(headTrackingPref);
        }

        boolean isHeadTrackingAvailable =
                isSpatialAudioOn && mSpatializer.hasHeadTracker(mAudioDevice);
        Log.d(TAG, "refresh() has head tracker : " + mSpatializer.hasHeadTracker(mAudioDevice));
        headTrackingPref.setVisible(isHeadTrackingAvailable);
        if (isHeadTrackingAvailable) {
            headTrackingPref.setChecked(mSpatializer.isHeadTrackerEnabled(mAudioDevice));
        }
    }

    @VisibleForTesting
    SwitchPreference createSpatialAudioPreference(Context context) {
        SwitchPreference pref = new SwitchPreference(context);
        pref.setKey(KEY_SPATIAL_AUDIO);
        pref.setTitle(context.getString(R.string.bluetooth_details_spatial_audio_title));
        pref.setSummary(context.getString(R.string.bluetooth_details_spatial_audio_summary));
        pref.setOnPreferenceClickListener(this);
        return pref;
    }

    @VisibleForTesting
    SwitchPreference createHeadTrackingPreference(Context context) {
        SwitchPreference pref = new SwitchPreference(context);
        pref.setKey(KEY_HEAD_TRACKING);
        pref.setTitle(context.getString(R.string.bluetooth_details_head_tracking_title));
        pref.setSummary(context.getString(R.string.bluetooth_details_head_tracking_summary));
        pref.setOnPreferenceClickListener(this);
        return pref;
    }

    private void getAvailableDevice() {
        AudioDeviceAttributes a2dpDevice = new AudioDeviceAttributes(
                AudioDeviceAttributes.ROLE_OUTPUT,
                AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
                mCachedDevice.getAddress());
        AudioDeviceAttributes bleHeadsetDevice = new AudioDeviceAttributes(
                AudioDeviceAttributes.ROLE_OUTPUT,
                AudioDeviceInfo.TYPE_BLE_HEADSET,
                mCachedDevice.getAddress());
        AudioDeviceAttributes bleSpeakerDevice = new AudioDeviceAttributes(
                AudioDeviceAttributes.ROLE_OUTPUT,
                AudioDeviceInfo.TYPE_BLE_SPEAKER,
                mCachedDevice.getAddress());
        AudioDeviceAttributes bleBroadcastDevice = new AudioDeviceAttributes(
                AudioDeviceAttributes.ROLE_OUTPUT,
                AudioDeviceInfo.TYPE_BLE_BROADCAST,
                mCachedDevice.getAddress());
        AudioDeviceAttributes hearingAidDevice = new AudioDeviceAttributes(
                AudioDeviceAttributes.ROLE_OUTPUT,
                AudioDeviceInfo.TYPE_HEARING_AID,
                mCachedDevice.getAddress());

        mIsAvailable = true;
        if (mSpatializer.isAvailableForDevice(bleHeadsetDevice)) {
            mAudioDevice = bleHeadsetDevice;
        } else if (mSpatializer.isAvailableForDevice(bleSpeakerDevice)) {
            mAudioDevice = bleSpeakerDevice;
        } else if (mSpatializer.isAvailableForDevice(bleBroadcastDevice)) {
            mAudioDevice = bleBroadcastDevice;
        } else if (mSpatializer.isAvailableForDevice(a2dpDevice)) {
            mAudioDevice = a2dpDevice;
        } else {
            mIsAvailable = mSpatializer.isAvailableForDevice(hearingAidDevice);
            mAudioDevice = hearingAidDevice;
        }

        Log.d(TAG, "getAvailableDevice() device : "
                + mCachedDevice.getDevice().getAnonymizedAddress()
                + ", type : " + mAudioDevice.getType()
                + ", is available : " + mIsAvailable);
    }

    @VisibleForTesting
    void setAvailableDevice(AudioDeviceAttributes audioDevice) {
        mAudioDevice = audioDevice;
        mIsAvailable = mSpatializer.isAvailableForDevice(audioDevice);
    }
}
