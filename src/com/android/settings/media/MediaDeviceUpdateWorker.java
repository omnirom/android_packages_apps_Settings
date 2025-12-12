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

package com.android.settings.media;

import static android.media.AudioManager.STREAM_DEVICES_CHANGED_ACTION;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.media.MediaRouter2Manager;
import android.media.RoutingSessionInfo;
import android.net.Uri;
import android.text.TextUtils;

import androidx.annotation.VisibleForTesting;

import com.android.settings.slices.SliceBackgroundWorker;
import com.android.settingslib.Utils;
import com.android.settingslib.media.LocalMediaManager;
import com.android.settingslib.media.MediaDevice;

import java.util.List;

/**
 * SliceBackgroundWorker for get MediaDevice list and handle MediaDevice state change event.
 */
public class MediaDeviceUpdateWorker extends SliceBackgroundWorker
        implements LocalMediaManager.DeviceCallback {

    public static final String MEDIA_PACKAGE_NAME = "media_package_name";

    protected final Context mContext;
    private final DevicesChangedBroadcastReceiver mReceiver;
    private final String mPackageName;
    @VisibleForTesting
    MediaRouter2Manager mManager;

    @VisibleForTesting
    LocalMediaManager mLocalMediaManager;

    public MediaDeviceUpdateWorker(Context context, Uri uri) {
        super(context, uri);
        mContext = context;
        mPackageName = uri.getQueryParameter(MEDIA_PACKAGE_NAME);
        mReceiver = new DevicesChangedBroadcastReceiver();
    }

    @Override
    protected void onSlicePinned() {
        if (mLocalMediaManager == null || !TextUtils.equals(mPackageName,
                mLocalMediaManager.getPackageName())) {
            mLocalMediaManager = new LocalMediaManager(mContext, mPackageName);
        }

        // Delaying initialization to allow mocking in Roboelectric tests.
        if (mManager == null) {
            mManager = MediaRouter2Manager.getInstance(mContext);
        }

        mLocalMediaManager.registerCallback(this);
        final IntentFilter intentFilter = new IntentFilter(STREAM_DEVICES_CHANGED_ACTION);
        mContext.registerReceiver(mReceiver, intentFilter);
        mLocalMediaManager.startScan();
    }

    @Override
    protected void onSliceUnpinned() {
        mLocalMediaManager.unregisterCallback(this);
        mContext.unregisterReceiver(mReceiver);
        mLocalMediaManager.stopScan();
    }

    @Override
    public void close() {
        mLocalMediaManager = null;
    }

    @Override
    public void onDeviceListUpdate(List<MediaDevice> devices) {
        notifySliceChange();
    }

    @Override
    public void onSelectedDeviceStateChanged(MediaDevice device, int state) {
        notifySliceChange();
    }

    @Override
    public void onDeviceAttributesChanged() {
        notifySliceChange();
    }

    @Override
    public void onRequestFailed(int reason) {
        notifySliceChange();
    }

    void adjustSessionVolume(String sessionId, int volume) {
        mLocalMediaManager.adjustSessionVolume(sessionId, volume);
    }

    List<RoutingSessionInfo> getActiveRemoteMediaDevices() {
        return mLocalMediaManager.getRemoteRoutingSessions();
    }

    boolean shouldDisableMediaOutput(String packageName) {
        // TODO: b/291277292 - Remove references to MediaRouter2Manager and implement long-term
        //  solution in SettingsLib.
        return mManager.getTransferableRoutes(packageName).isEmpty();
    }

    boolean shouldEnableVolumeSeekBar(RoutingSessionInfo sessionInfo) {
        return mLocalMediaManager.shouldEnableVolumeSeekBar(sessionInfo);
    }

    private class DevicesChangedBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (TextUtils.equals(AudioManager.STREAM_DEVICES_CHANGED_ACTION, action)
                    && Utils.isAudioModeOngoingCall(mContext)) {
                notifySliceChange();
            }
        }
    }
}
