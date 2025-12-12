/*
 * Copyright (C) 2023 The Android Open Source Project
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

package com.android.settings.connecteddevice.audiosharing;

import static com.android.settings.connecteddevice.audiosharing.AudioSharingUtils.MetricKey.METRIC_KEY_DEVICE_IS_PRIMARY;

import android.app.settings.SettingsEnums;
import android.bluetooth.BluetoothCsipSetCoordinator;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.media.AudioManager;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.settings.R;
import com.android.settings.bluetooth.Utils;
import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.bluetooth.BluetoothUtils;
import com.android.settingslib.bluetooth.CachedBluetoothDevice;
import com.android.settingslib.bluetooth.LocalBluetoothManager;
import com.android.settingslib.bluetooth.VolumeControlProfile;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;
import com.android.settingslib.utils.ThreadUtils;
import com.android.settingslib.widget.SliderPreference;

public class AudioSharingDeviceVolumeSliderPreference extends SliderPreference {
    private static final String TAG = "AudioSharingVolPref";

    public static final int MIN_VOLUME = 0;
    public static final int MAX_VOLUME = 255;

    private final Context mContext;
    private final CachedBluetoothDevice mCachedDevice;
    @Nullable private final LocalBluetoothManager mBtManager;
    private MetricsFeatureProvider mMetricsFeatureProvider =
            FeatureFactory.getFeatureFactory().getMetricsFeatureProvider();

    public AudioSharingDeviceVolumeSliderPreference(
            Context context, @NonNull CachedBluetoothDevice device) {
        super(context);
        mContext = context;
        mCachedDevice = device;
        mBtManager = Utils.getLocalBtManager(mContext);
    }

    @NonNull
    public CachedBluetoothDevice getCachedDevice() {
        return mCachedDevice;
    }

    /**
     * Initialize {@link AudioSharingDeviceVolumeSliderPreference}.
     *
     * <p>Need to be called after creating the preference.
     */
    public void initialize() {
        setMax(MAX_VOLUME);
        setMin(MIN_VOLUME);
        setUpdatesContinuously(false);
        setOnPreferenceChangeListener(
                (pref, value) -> {
                    handleProgressChange((int) value);
                    return true;
                });
        refreshPreference();
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if ((o == null) || !(o instanceof AudioSharingDeviceVolumeSliderPreference)) {
            return false;
        }
        return mCachedDevice.equals(((AudioSharingDeviceVolumeSliderPreference) o).mCachedDevice);
    }

    @Override
    public int hashCode() {
        return mCachedDevice.hashCode();
    }

    @Override
    @NonNull
    public String toString() {
        StringBuilder builder = new StringBuilder("Preference{");
        builder.append("preference=").append(super.toString());
        if (mCachedDevice.getDevice() != null) {
            builder.append(", device=").append(mCachedDevice.getDevice().getAnonymizedAddress());
        }
        builder.append("}");
        return builder.toString();
    }

    void onPreferenceAttributesChanged() {
        refreshPreference();
    }

    private void refreshPreference() {
        var unused = ThreadUtils.postOnBackgroundThread(() -> {
            String name = mCachedDevice.getName();
            AudioSharingUtils.postOnMainThread(mContext, () -> {
                setTitle(name);
                setSliderContentDescription(
                        mContext.getString(R.string.audio_sharing_device_volume_description, name));
            });
        });
    }

    private void handleProgressChange(int progress) {
        var unused =
                ThreadUtils.postOnBackgroundThread(
                        () -> {
                            int groupId = BluetoothUtils.getGroupId(mCachedDevice);
                            if (groupId != BluetoothCsipSetCoordinator.GROUP_ID_INVALID
                                    && groupId
                                            == BluetoothUtils.getPrimaryGroupIdForBroadcast(
                                                    mContext.getContentResolver(), mBtManager)) {
                                // Set media stream volume for primary buds, audio manager will
                                // update all buds volume in the audio sharing.
                                setAudioManagerStreamVolume(progress);
                            } else {
                                // Set buds volume for other buds.
                                setDeviceVolume(mCachedDevice.getDevice(), progress);
                            }
                        });
    }

    private void setDeviceVolume(@Nullable BluetoothDevice device, int progress) {
        if (device == null) {
            Log.d(TAG, "Skip set device volume, device is null");
            return;
        }
        VolumeControlProfile vc =
                mBtManager == null
                        ? null
                        : mBtManager.getProfileManager().getVolumeControlProfile();
        if (vc != null) {
            vc.setDeviceVolume(device, progress, /* isGroupOp= */ true);
            mMetricsFeatureProvider.action(
                    SettingsEnums.PAGE_UNKNOWN,
                    SettingsEnums.ACTION_AUDIO_SHARING_CHANGE_MEDIA_DEVICE_VOLUME,
                    SettingsEnums.PAGE_UNKNOWN,
                    String.valueOf(METRIC_KEY_DEVICE_IS_PRIMARY.getId()),
                    /* isPrimary= */ 0);
            Log.d(
                    TAG,
                    "set device volume, device = "
                            + device.getAnonymizedAddress()
                            + " volume = "
                            + progress);
        }
    }

    private void setAudioManagerStreamVolume(int progress) {
        int seekbarRange =
                AudioSharingDeviceVolumeSliderPreference.MAX_VOLUME
                        - AudioSharingDeviceVolumeSliderPreference.MIN_VOLUME;
        try {
            AudioManager audioManager = mContext.getSystemService(AudioManager.class);
            int streamVolumeRange =
                    audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                            - audioManager.getStreamMinVolume(AudioManager.STREAM_MUSIC);
            int volume = Math.round((float) progress * streamVolumeRange / seekbarRange);
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, volume, 0);
            mMetricsFeatureProvider.action(
                    SettingsEnums.PAGE_UNKNOWN,
                    SettingsEnums.ACTION_AUDIO_SHARING_CHANGE_MEDIA_DEVICE_VOLUME,
                    SettingsEnums.PAGE_UNKNOWN,
                    String.valueOf(METRIC_KEY_DEVICE_IS_PRIMARY.getId()),
                    /* isPrimary= */ 1);
            Log.d(TAG, "set music stream volume, volume = " + progress);
        } catch (RuntimeException e) {
            Log.e(TAG, "Fail to setAudioManagerStreamVolumeForFallbackDevice, error = " + e);
        }
    }
}
