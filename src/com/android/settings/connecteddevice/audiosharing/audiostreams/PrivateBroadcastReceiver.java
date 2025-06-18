/*
 * Copyright (C) 2025 The Android Open Source Project
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

import static com.android.settingslib.bluetooth.LocalBluetoothLeBroadcast.ACTION_LE_AUDIO_PRIVATE_BROADCAST_RECEIVED;
import static com.android.settingslib.bluetooth.LocalBluetoothLeBroadcast.EXTRA_PRIVATE_BROADCAST_RECEIVE_DATA;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.android.settingslib.bluetooth.BluetoothUtils;
import com.android.settingslib.bluetooth.PrivateBroadcastReceiveData;
import com.android.settingslib.flags.Flags;

/**
 * A {@link BroadcastReceiver} that listens for a private broadcast state.
 * Upon receiving the broadcast, it extracts data and starts an {@link AudioStreamMediaService}
 */
public class PrivateBroadcastReceiver extends BroadcastReceiver {
    private static final String TAG = "PrivBroadcastReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (action == null || !action.equals(ACTION_LE_AUDIO_PRIVATE_BROADCAST_RECEIVED)) {
            Log.w(TAG, "Received unexpected intent action.");
            return;
        }
        if (!Flags.audioStreamMediaServiceByReceiveState()
                || !BluetoothUtils.isAudioSharingUIAvailable(context)) {
            Log.d(TAG, "Skip, flag/feature off");
            return;
        }
        PrivateBroadcastReceiveData data = intent.getParcelableExtra(
                EXTRA_PRIVATE_BROADCAST_RECEIVE_DATA, PrivateBroadcastReceiveData.class);
        if (data == null || !PrivateBroadcastReceiveData.Companion.isValid(data)) {
            Log.w(TAG, "PrivateBroadcastReceiveData is null or invalid.");
            return;
        }
        startOrUpdateService(context, data);
    }

    private static void startOrUpdateService(Context context, PrivateBroadcastReceiveData data) {
        Log.d(TAG, "startOrUpdateService() with data:" + data);
        Intent intent = new Intent(context, AudioStreamMediaService.class);
        intent.putExtra(EXTRA_PRIVATE_BROADCAST_RECEIVE_DATA, data);
        context.startService(intent);
    }
}
