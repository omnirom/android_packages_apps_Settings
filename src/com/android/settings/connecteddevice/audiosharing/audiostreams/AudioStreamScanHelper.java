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

import static android.bluetooth.BluetoothStatusCodes.ERROR_ALREADY_IN_TARGET_STATE;

import static com.android.settings.connecteddevice.audiosharing.audiostreams.AudioStreamScanHelper.State.STATE_OFF;
import static com.android.settings.connecteddevice.audiosharing.audiostreams.AudioStreamScanHelper.State.STATE_ON;
import static com.android.settings.connecteddevice.audiosharing.audiostreams.AudioStreamScanHelper.State.STATE_TURNING_OFF;
import static com.android.settings.connecteddevice.audiosharing.audiostreams.AudioStreamScanHelper.State.STATE_TURNING_ON;
import static com.android.settings.connecteddevice.audiosharing.audiostreams.AudioStreamScanHelper.State.STATE_WAITING_TO_RESTART_WITH_NO_FILTER;
import static com.android.settings.connecteddevice.audiosharing.audiostreams.AudioStreamsProgressCategoryController.UNSET_BROADCAST_ID;

import static java.util.Collections.emptyList;

import android.bluetooth.BluetoothLeBroadcastMetadata;
import android.bluetooth.le.ScanFilter;
import android.os.ParcelUuid;
import android.util.Log;

import androidx.annotation.NonNull;

import com.android.settingslib.bluetooth.LocalBluetoothLeBroadcastAssistant;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Helper class for managing the scanning. It utilizes the
 * {@link LocalBluetoothLeBroadcastAssistant} to initiate and stop scanning and provides callbacks
 * to inform listeners about the scan state.
 */
public class AudioStreamScanHelper implements
        AudioStreamsProgressCategoryCallback.ScanStateListener {
    enum State {
        STATE_OFF,
        STATE_TURNING_ON,
        STATE_ON,
        STATE_TURNING_OFF,
        STATE_WAITING_TO_RESTART_WITH_NO_FILTER,
    }

    private static final String TAG = "AudioStreamScanHelper";
    private static final ParcelUuid BAAS_UUID = ParcelUuid.fromString(
            "00001852-0000-1000-8000-00805F9B34FB");
    private static final String UNSET_DEVICE_ADDR = "FF:FF:FF:FF:FF:FF";
    private final @Nullable LocalBluetoothLeBroadcastAssistant mLeBroadcastAssistant;
    private final Consumer<Boolean> mScanStateChangedListener;
    private final @NonNull Executor mExecutor;
    private State mState = STATE_OFF;

    AudioStreamScanHelper(@NonNull Executor executor,
            @Nullable LocalBluetoothLeBroadcastAssistant leBroadcastAssistant,
            @Nonnull Consumer<Boolean> scanStateChangedListener) {
        mExecutor = executor;
        mLeBroadcastAssistant = leBroadcastAssistant;
        mScanStateChangedListener = scanStateChangedListener;
    }

    /**
     * Returns if the scanning has already started or in the process of starting.
     */
    public boolean hasStartedScanning() {
        return mState == STATE_ON || mState == STATE_TURNING_ON;
    }

    /**
     * Starts the scanning process for available audio stream sources.
     * This method will do nothing if scanning is already active or in the process of starting.
     */
    public void startScanning() {
        mExecutor.execute(() -> {
            if (hasStartedScanning()) {
                Log.d(TAG, "startScanning() : do nothing, state = " + mState);
                return;
            }
            if (mLeBroadcastAssistant != null) {
                Log.d(TAG, "startScanning()");
                mLeBroadcastAssistant.startSearchingForSources(emptyList());
                setState(STATE_TURNING_ON);
            }
        });
    }

    /**
     * Starts the scanning process for one specific audio stream when info in metadata is available.
     * This method will do nothing if scanning is already started or in the process of starting.
     */
    public void startScanningWithFilter(@NonNull BluetoothLeBroadcastMetadata metadata) {
        mExecutor.execute(() -> {
            if (hasStartedScanning()) {
                Log.d(TAG, "startScanningWithFilter() : do nothing, state = " + mState);
                return;
            }
            boolean useFilter = false;
            ScanFilter.Builder builder = new ScanFilter.Builder();
            if (metadata.getBroadcastId() != UNSET_BROADCAST_ID) {
                byte[] broadcastIdBytes = createBroadcastIdBytes(metadata.getBroadcastId());
                byte[] serviceDataMask = new byte[broadcastIdBytes.length];
                Arrays.fill(serviceDataMask, (byte) 0xFF);
                builder.setServiceData(BAAS_UUID, broadcastIdBytes, serviceDataMask);
                useFilter = true;
            }
            if (metadata.getSourceDevice() != null && !Objects.equals(
                    metadata.getSourceDevice().getAddress(), UNSET_DEVICE_ADDR)) {
                builder.setDeviceAddress(metadata.getSourceDevice().getAddress());
                useFilter = true;
            }
            var filter = builder.build();
            if (mLeBroadcastAssistant != null) {
                Log.d(TAG, "startScanningWithFilter() : scanFilter applied : " + filter);
                mLeBroadcastAssistant.startSearchingForSources(
                        useFilter ? List.of(filter) : emptyList());
                setState(STATE_TURNING_ON);
            }
        });
    }

    /**
     * Restarts the scanning process without scan filter.
     * This method will do nothing if scanning is not started or in the process of starting.
     */
    public void restartScanningWithoutFilter() {
        mExecutor.execute(() -> {
            if (mState != STATE_ON && mState != STATE_TURNING_ON) {
                Log.d(TAG, "restartScanningWithoutFilter() : do nothing, state = " + mState);
                return;
            }
            if (mLeBroadcastAssistant != null) {
                Log.d(TAG, "restartScanningWithoutFilter() : stop scanning");
                mLeBroadcastAssistant.stopSearchingForSources();
                setState(STATE_WAITING_TO_RESTART_WITH_NO_FILTER);
            }
        });
    }

    private static byte[] createBroadcastIdBytes(int broadcastId) {
        byte[] byteArray = new byte[3];

        byteArray[0] = (byte) (broadcastId & 0xFF);
        byteArray[1] = (byte) ((broadcastId >> 8) & 0xFF);
        byteArray[2] = (byte) ((broadcastId >> 16) & 0xFF);

        return byteArray;
    }

    /**
     * Stops the ongoing scanning process for audio stream sources.
     * This method will do nothing if scanning is already off or in the process of stopping.
     */
    public void stopScanning() {
        mExecutor.execute(() -> {
            if (mState == STATE_OFF || mState == STATE_TURNING_OFF) {
                Log.d(TAG, "stopScanning() : do nothing, state = " + mState);
                return;
            }
            if (mLeBroadcastAssistant != null) {
                Log.d(TAG, "stopScanning()");
                mLeBroadcastAssistant.stopSearchingForSources();
                setState(STATE_TURNING_OFF);
            }
        });
    }

    @Override
    public void scanningStarted() {
        mExecutor.execute(() -> {
            Log.d(TAG, "scanningStarted()");
            setState(STATE_ON);
        });
    }

    @Override
    public void scanningStartFailed(int reason) {
        mExecutor.execute(() -> {
            Log.d(TAG, "scanningStartFailed() : reason = " + reason);
            setState(reason == ERROR_ALREADY_IN_TARGET_STATE ? STATE_ON : STATE_OFF);
        });
    }

    @Override
    public void scanningStopped() {
        mExecutor.execute(() -> {
            Log.d(TAG, "scanningStopped()");
            if (mState == STATE_WAITING_TO_RESTART_WITH_NO_FILTER) {
                startScanning();
            } else {
                setState(STATE_OFF);
            }
        });
    }

    @Override
    public void scanningStopFailed(int reason) {
        mExecutor.execute(() -> {
            Log.d(TAG, "scanningStopFailed() : reason = " + reason);
            setState(reason == ERROR_ALREADY_IN_TARGET_STATE ? STATE_OFF : STATE_ON);
        });
    }

    private void setState(State newState) {
        Log.d(TAG, "setState: from " + mState + " to " + newState);
        mState = newState;
        mScanStateChangedListener.accept(hasStartedScanning());
    }
}
