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

import static com.android.settingslib.bluetooth.LocalBluetoothLeBroadcastAssistant.getLocalSourceState;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothLeBroadcastMetadata;
import android.bluetooth.BluetoothLeBroadcastReceiveState;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class AudioStreamsProgressCategoryCallback extends AudioStreamsBroadcastAssistantCallback {
    @Nullable private SourceStateListener mSourceStateListener = null;
    @Nullable private ScanStateListener mScanStateListener = null;

    void setSourceStateListener(SourceStateListener listener) {
        mSourceStateListener = listener;
    }

    void setScanStateListener(ScanStateListener listener) {
        mScanStateListener = listener;
    }

    @Override
    public void onReceiveStateChanged(
            BluetoothDevice sink, int sourceId, BluetoothLeBroadcastReceiveState state) {
        super.onReceiveStateChanged(sink, sourceId, state);
        if (mSourceStateListener != null) {
            var sourceState = getLocalSourceState(state);
            switch (sourceState) {
                case STREAMING -> mSourceStateListener.handleSourceStreaming(sink, state);
                case DECRYPTION_FAILED -> mSourceStateListener.handleSourceConnectBadCode(state);
                case PAUSED -> mSourceStateListener.handleSourcePaused(sink, state);
                default -> {
                    // Do nothing
                }
            }
        }
    }

    @Override
    public void onSearchStartFailed(int reason) {
        super.onSearchStartFailed(reason);
        if (mScanStateListener != null) {
            mScanStateListener.scanningStartFailed(reason);
        }
    }

    @Override
    public void onSearchStarted(int reason) {
        super.onSearchStarted(reason);
        if (mScanStateListener != null) {
            mScanStateListener.scanningStarted();
        }
    }

    @Override
    public void onSearchStopFailed(int reason) {
        super.onSearchStopFailed(reason);
        if (mScanStateListener != null) {
            mScanStateListener.scanningStopFailed(reason);
        }
    }

    @Override
    public void onSearchStopped(int reason) {
        super.onSearchStopped(reason);
        if (mScanStateListener != null) {
            mScanStateListener.scanningStopped();
        }
    }

    @Override
    public void onSourceAddFailed(
            BluetoothDevice sink, BluetoothLeBroadcastMetadata source, int reason) {
        super.onSourceAddFailed(sink, source, reason);
        if (mSourceStateListener != null) {
            mSourceStateListener.handleSourceFailedToConnect(source.getBroadcastId());
        }
    }

    @Override
    public void onSourceFound(BluetoothLeBroadcastMetadata source) {
        super.onSourceFound(source);
        if (mSourceStateListener != null) {
            mSourceStateListener.handleSourceFound(source);
        }
    }

    @Override
    public void onSourceLost(int broadcastId) {
        super.onSourceLost(broadcastId);
        if (mSourceStateListener != null) {
            mSourceStateListener.handleSourceLost(broadcastId);
        }
    }

    @Override
    public void onSourceRemoved(BluetoothDevice sink, int sourceId, int reason) {
        super.onSourceRemoved(sink, sourceId, reason);
        if (mSourceStateListener != null) {
            mSourceStateListener.handleSourceRemoved();
        }
    }

    interface ScanStateListener {
        void scanningStarted();
        void scanningStartFailed(int reason);
        void scanningStopped();
        void scanningStopFailed(int reason);
    }

    interface SourceStateListener {
        void handleSourceStreaming(@NonNull BluetoothDevice device,
                @NonNull BluetoothLeBroadcastReceiveState receiveState);
        void handleSourceConnectBadCode(@NonNull BluetoothLeBroadcastReceiveState receiveState);
        void handleSourcePaused(@NonNull BluetoothDevice device,
                @NonNull BluetoothLeBroadcastReceiveState receiveState);
        void handleSourceFailedToConnect(int broadcastId);
        void handleSourceFound(@NonNull BluetoothLeBroadcastMetadata source);
        void handleSourceLost(int broadcastId);
        void handleSourceRemoved();
    }
}
