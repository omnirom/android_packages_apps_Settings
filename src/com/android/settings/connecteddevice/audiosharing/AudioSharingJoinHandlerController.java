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

package com.android.settings.connecteddevice.audiosharing;

import static com.android.settingslib.bluetooth.LocalBluetoothLeBroadcast.EXTRA_BLUETOOTH_DEVICE;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothLeBroadcastAssistant;
import android.bluetooth.BluetoothLeBroadcastMetadata;
import android.bluetooth.BluetoothLeBroadcastReceiveState;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.annotation.WorkerThread;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.preference.PreferenceScreen;

import com.android.settings.bluetooth.Utils;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settingslib.bluetooth.BluetoothCallback;
import com.android.settingslib.bluetooth.BluetoothEventManager;
import com.android.settingslib.bluetooth.BluetoothUtils;
import com.android.settingslib.bluetooth.CachedBluetoothDevice;
import com.android.settingslib.bluetooth.CachedBluetoothDeviceManager;
import com.android.settingslib.bluetooth.LocalBluetoothLeBroadcastAssistant;
import com.android.settingslib.bluetooth.LocalBluetoothManager;
import com.android.settingslib.utils.ThreadUtils;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class AudioSharingJoinHandlerController extends BasePreferenceController
        implements DefaultLifecycleObserver, BluetoothCallback {
    private static final String TAG = "AudioSharingJoinHandlerCtrl";
    private static final String KEY = "audio_sharing_join_handler";

    @Nullable private final LocalBluetoothManager mBtManager;
    @Nullable private final BluetoothEventManager mEventManager;
    @Nullable private final CachedBluetoothDeviceManager mDeviceManager;
    @Nullable private final LocalBluetoothLeBroadcastAssistant mAssistant;
    private final Executor mExecutor;
    @Nullable private DashboardFragment mFragment;
    @Nullable private AudioSharingDialogHandler mDialogHandler;
    @VisibleForTesting
    BluetoothLeBroadcastAssistant.Callback mAssistantCallback =
            new BluetoothLeBroadcastAssistant.Callback() {
                @Override
                public void onSearchStarted(int reason) {
                }

                @Override
                public void onSearchStartFailed(int reason) {
                }

                @Override
                public void onSearchStopped(int reason) {
                }

                @Override
                public void onSearchStopFailed(int reason) {
                }

                @Override
                public void onSourceFound(@NonNull BluetoothLeBroadcastMetadata source) {
                }

                @Override
                public void onSourceAdded(
                        @NonNull BluetoothDevice sink, int sourceId, int reason) {
                    Log.d(TAG, "onSourceAdded: dismiss stale dialog.");
                    if (mDeviceManager != null && mDialogHandler != null) {
                        CachedBluetoothDevice cachedDevice = mDeviceManager.findDevice(sink);
                        if (cachedDevice != null) {
                            mDialogHandler.closeOpeningDialogsForLeaDevice(cachedDevice);
                        }
                    }
                }

                @Override
                public void onSourceAddFailed(
                        @NonNull BluetoothDevice sink,
                        @NonNull BluetoothLeBroadcastMetadata source,
                        int reason) {
                }

                @Override
                public void onSourceModified(
                        @NonNull BluetoothDevice sink, int sourceId, int reason) {
                }

                @Override
                public void onSourceModifyFailed(
                        @NonNull BluetoothDevice sink, int sourceId, int reason) {
                }

                @Override
                public void onSourceRemoved(
                        @NonNull BluetoothDevice sink, int sourceId, int reason) {
                }

                @Override
                public void onSourceRemoveFailed(
                        @NonNull BluetoothDevice sink, int sourceId, int reason) {
                }

                @Override
                public void onReceiveStateChanged(
                        @NonNull BluetoothDevice sink,
                        int sourceId,
                        @NonNull BluetoothLeBroadcastReceiveState state) {
                }
            };

    public AudioSharingJoinHandlerController(@NonNull Context context,
            @NonNull String preferenceKey) {
        super(context, preferenceKey);
        mBtManager = Utils.getLocalBtManager(mContext);
        mEventManager = mBtManager == null ? null : mBtManager.getEventManager();
        mDeviceManager = mBtManager == null ? null : mBtManager.getCachedDeviceManager();
        mAssistant = mBtManager == null ? null
                : mBtManager.getProfileManager().getLeAudioBroadcastAssistantProfile();
        mExecutor = Executors.newSingleThreadExecutor();
    }

    /**
     * Initialize the controller.
     *
     * @param fragment The fragment to provide the context and metrics category for {@link
     *                 AudioSharingBluetoothDeviceUpdater} and provide the host for dialogs.
     */
    public void init(@NonNull DashboardFragment fragment) {
        mFragment = fragment;
        mDialogHandler = new AudioSharingDialogHandler(mContext, fragment);
    }

    @Override
    public void onStart(@NonNull LifecycleOwner owner) {
        var unused = ThreadUtils.postOnBackgroundThread(() -> {
            if (!isAvailable()) {
                Log.d(TAG, "Skip onStart(), feature is not supported.");
                return;
            }
            if (mEventManager == null || mDialogHandler == null || mAssistant == null) {
                Log.d(TAG, "Skip onStart(), profile is not ready.");
                return;
            }
            Log.d(TAG, "onStart() Register callbacks.");
            mEventManager.registerCallback(this);
            mAssistant.registerServiceCallBack(mExecutor, mAssistantCallback);
            mDialogHandler.registerCallbacks(mExecutor);
        });
    }

    @Override
    public void onStop(@NonNull LifecycleOwner owner) {
        var unused = ThreadUtils.postOnBackgroundThread(() -> {
            if (!isAvailable()) {
                Log.d(TAG, "Skip onStop(), feature is not supported.");
                return;
            }
            if (mEventManager == null || mDialogHandler == null || mAssistant == null) {
                Log.d(TAG, "Skip onStop(), profile is not ready.");
                return;
            }
            Log.d(TAG, "onStop() Unregister callbacks.");
            mEventManager.unregisterCallback(this);
            mAssistant.unregisterServiceCallBack(mAssistantCallback);
            mDialogHandler.unregisterCallbacks();
        });
    }

    @Override
    public int getAvailabilityStatus() {
        return BluetoothUtils.isAudioSharingUIAvailable(mContext)
                ? AVAILABLE_UNSEARCHABLE
                : UNSUPPORTED_ON_DEVICE;
    }

    @Override
    public String getPreferenceKey() {
        return KEY;
    }

    @Override
    public int getSliceHighlightMenuRes() {
        return 0;
    }

    @Override
    public void displayPreference(@NonNull PreferenceScreen screen) {
        super.displayPreference(screen);
        if (mFragment == null
                || mFragment.getActivity() == null
                || mFragment.getActivity().getIntent() == null) {
            Log.d(TAG, "Skip handleDeviceConnectedFromIntent, fragment intent is null");
            return;
        }
        Intent intent = mFragment.getActivity().getIntent();
        var unused =
                ThreadUtils.postOnBackgroundThread(() -> handleDeviceConnectedFromIntent(intent));
    }

    @Override
    public void onProfileConnectionStateChanged(
            @NonNull CachedBluetoothDevice cachedDevice,
            @ConnectionState int state,
            int bluetoothProfile) {
        if (mDialogHandler == null || mFragment == null) {
            Log.d(TAG, "Ignore onProfileConnectionStateChanged, not init correctly");
            return;
        }
        // Close related dialogs if the BT remote device is disconnected.
        if (state == BluetoothAdapter.STATE_DISCONNECTED) {
            boolean isLeAudioSupported = BluetoothUtils.isLeAudioSupported(cachedDevice);
            if (isLeAudioSupported
                    && bluetoothProfile == BluetoothProfile.LE_AUDIO_BROADCAST_ASSISTANT) {
                Log.d(TAG, "closeOpeningDialogsForLeaDevice");
                mDialogHandler.closeOpeningDialogsForLeaDevice(cachedDevice);
            } else if (!isLeAudioSupported && !cachedDevice.isConnected()) {
                Log.d(TAG, "closeOpeningDialogsForNonLeaDevice");
                mDialogHandler.closeOpeningDialogsForNonLeaDevice(cachedDevice);
            }
        }
    }

    @Override
    public void onBluetoothStateChanged(@AdapterState int bluetoothState) {
        if (bluetoothState == BluetoothAdapter.STATE_OFF) {
            finishActivity();
        }
    }

    /** Handle just connected device via intent. */
    @WorkerThread
    public void handleDeviceConnectedFromIntent(@NonNull Intent intent) {
        BluetoothDevice device = intent.getParcelableExtra(EXTRA_BLUETOOTH_DEVICE,
                BluetoothDevice.class);
        CachedBluetoothDevice cachedDevice =
                (device == null || mDeviceManager == null)
                        ? null
                        : mDeviceManager.findDevice(device);
        if (cachedDevice == null) {
            Log.d(TAG, "Skip handleDeviceConnectedFromIntent and finish activity, device is null");
            finishActivity();
            return;
        }
        if (mDialogHandler == null) {
            Log.d(TAG, "Skip handleDeviceConnectedFromIntent and finish activity, handler is null");
            finishActivity();
            return;
        }
        Log.d(TAG, "handleDeviceConnectedFromIntent, device = " + device.getAnonymizedAddress());
        if (!mDialogHandler.handleDeviceConnected(cachedDevice, /* userTriggered= */ false)) {
            Log.d(TAG, "handleDeviceConnectedFromIntent, finish activity");
            finishActivity();
        }
    }

    private void finishActivity() {
        AudioSharingUtils.postOnMainThread(mContext, () -> {
            if (mFragment != null && mFragment.getActivity() != null) {
                Log.d(TAG, "Finish activity");
                mFragment.getActivity().finish();
            }
        });
    }

    @VisibleForTesting
    void setDialogHandler(@Nullable AudioSharingDialogHandler dialogHandler) {
        mDialogHandler = dialogHandler;
    }
}
