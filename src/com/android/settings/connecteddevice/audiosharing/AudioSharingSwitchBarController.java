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

import static com.android.settingslib.bluetooth.LocalBluetoothLeBroadcast.EXTRA_START_LE_AUDIO_SHARING;

import android.app.settings.SettingsEnums;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothLeBroadcast;
import android.bluetooth.BluetoothLeBroadcastAssistant;
import android.bluetooth.BluetoothLeBroadcastMetadata;
import android.bluetooth.BluetoothLeBroadcastReceiveState;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.FeatureFlagUtils;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityEvent;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.UiThread;
import androidx.annotation.VisibleForTesting;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;

import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.bluetooth.Utils;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.overlay.FeatureFactory;
import com.android.settings.widget.SettingsMainSwitchBar;
import com.android.settingslib.bluetooth.BluetoothCallback;
import com.android.settingslib.bluetooth.BluetoothEventManager;
import com.android.settingslib.bluetooth.BluetoothUtils;
import com.android.settingslib.bluetooth.CachedBluetoothDevice;
import com.android.settingslib.bluetooth.CachedBluetoothDeviceManager;
import com.android.settingslib.bluetooth.LocalBluetoothLeBroadcast;
import com.android.settingslib.bluetooth.LocalBluetoothLeBroadcastAssistant;
import com.android.settingslib.bluetooth.LocalBluetoothManager;
import com.android.settingslib.bluetooth.LocalBluetoothProfileManager;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;
import com.android.settingslib.utils.ThreadUtils;

import com.google.common.collect.ImmutableList;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class AudioSharingSwitchBarController extends BasePreferenceController
        implements DefaultLifecycleObserver,
                OnCheckedChangeListener,
                LocalBluetoothProfileManager.ServiceListener,
                BluetoothCallback {
    private static final String TAG = "AudioSharingSwitchCtlr";
    private static final String PREF_KEY = "audio_sharing_main_switch";
    private static final String EXTRA_SOURCE_METRICS = ":settings:source_metrics";

    interface OnAudioSharingStateChangedListener {
        /**
         * The callback which will be triggered when:
         *
         * <p>1. Bluetooth on/off state changes. 2. Broadcast and assistant profile
         * connect/disconnect state changes. 3. Audio sharing start/stop state changes.
         */
        void onAudioSharingStateChanged();

        /**
         * The callback which will be triggered when:
         *
         * <p>Broadcast and assistant profile connected.
         */
        void onAudioSharingProfilesConnected();
    }

    private final SettingsMainSwitchBar mSwitchBar;
    private final BluetoothAdapter mBluetoothAdapter;
    @Nullable private final LocalBluetoothManager mBtManager;
    @Nullable private final BluetoothEventManager mEventManager;
    @Nullable private final LocalBluetoothProfileManager mProfileManager;
    @Nullable private final LocalBluetoothLeBroadcast mBroadcast;
    @Nullable private final LocalBluetoothLeBroadcastAssistant mAssistant;
    @Nullable private Fragment mFragment;
    private final Executor mExecutor;
    private final MetricsFeatureProvider mMetricsFeatureProvider;
    private final OnAudioSharingStateChangedListener mListener;
    @VisibleForTesting IntentFilter mIntentFilter;
    private Map<Integer, List<BluetoothDevice>> mGroupedConnectedDevices = new HashMap<>();
    @Nullable private AudioSharingDeviceItem mTargetActiveItem;
    private List<AudioSharingDeviceItem> mDeviceItemsForSharing = new ArrayList<>();
    private final AtomicBoolean mCallbacksRegistered = new AtomicBoolean(false);
    private AtomicInteger mIntentHandleStage =
            new AtomicInteger(StartIntentHandleStage.TO_HANDLE.getId());
    // The sinks in adding source process. We show the progress dialog based on this list.
    private CopyOnWriteArrayList<BluetoothDevice> mSinksInAdding = new CopyOnWriteArrayList<>();
    // The primary/active sinks in adding source process.
    // To avoid users advance to share then pair flow before the primary/active sinks successfully
    // join the audio sharing, we will wait for the process complete for this list of sinks and then
    // popup audio sharing dialog with options to pair new device.
    private CopyOnWriteArrayList<BluetoothDevice> mSinksToWaitFor = new CopyOnWriteArrayList<>();
    private AtomicBoolean mStartingSharing = new AtomicBoolean(false);
    private AtomicBoolean mStoppingSharing = new AtomicBoolean(false);

    @VisibleForTesting
    BroadcastReceiver mReceiver =
            new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    if (intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
                                    == BluetoothAdapter.STATE_ON
                            && !AudioSharingUtils.isAudioSharingProfileReady(mProfileManager)) {
                        if (mProfileManager != null) {
                            mProfileManager.addServiceListener(
                                    AudioSharingSwitchBarController.this);
                        }
                    } else {
                        updateSwitch();
                        mListener.onAudioSharingStateChanged();
                    }
                }
            };

    @VisibleForTesting
    final BluetoothLeBroadcast.Callback mBroadcastCallback =
            new BluetoothLeBroadcast.Callback() {
                @Override
                public void onBroadcastStarted(int reason, int broadcastId) {
                    Log.d(
                            TAG,
                            "onBroadcastStarted(), reason = "
                                    + reason
                                    + ", broadcastId = "
                                    + broadcastId);
                    updateSwitch();
                    AudioSharingUtils.toastMessage(
                            mContext, mContext.getString(R.string.audio_sharing_sharing_label));
                    mListener.onAudioSharingStateChanged();
                }

                @Override
                public void onBroadcastStartFailed(int reason) {
                    Log.d(TAG, "onBroadcastStartFailed(), reason = " + reason);
                    mStartingSharing.compareAndSet(true, false);
                    updateSwitch();
                    showErrorDialog();
                    mMetricsFeatureProvider.action(
                            mContext,
                            SettingsEnums.ACTION_AUDIO_SHARING_START_FAILED,
                            SettingsEnums.AUDIO_SHARING_SETTINGS);
                }

                @Override
                public void onBroadcastMetadataChanged(
                        int broadcastId, @NonNull BluetoothLeBroadcastMetadata metadata) {
                    Log.d(
                            TAG,
                            "onBroadcastMetadataChanged(), broadcastId = "
                                    + broadcastId
                                    + ", metadata = "
                                    + metadata.getBroadcastName());
                    if (!mStartingSharing.compareAndSet(true, false)) {
                        Log.d(TAG, "Skip handleOnBroadcastReady, not in starting process");
                        return;
                    }
                    handleOnBroadcastReady(metadata);
                }

                @Override
                public void onBroadcastStopped(int reason, int broadcastId) {
                    Log.d(
                            TAG,
                            "onBroadcastStopped(), reason = "
                                    + reason
                                    + ", broadcastId = "
                                    + broadcastId);
                    mStoppingSharing.compareAndSet(true, false);
                    updateSwitch();
                    AudioSharingUtils.postOnMainThread(
                            mContext, () -> dismissStaleDialogsOtherThanErrorDialog());
                    AudioSharingUtils.toastMessage(
                            mContext,
                            mContext.getString(R.string.audio_sharing_sharing_stopped_label));
                    mListener.onAudioSharingStateChanged();
                }

                @Override
                public void onBroadcastStopFailed(int reason) {
                    Log.d(TAG, "onBroadcastStopFailed(), reason = " + reason);
                    mStoppingSharing.compareAndSet(true, false);
                    updateSwitch();
                    mMetricsFeatureProvider.action(
                            mContext,
                            SettingsEnums.ACTION_AUDIO_SHARING_STOP_FAILED,
                            SettingsEnums.AUDIO_SHARING_SETTINGS);
                }

                @Override
                public void onBroadcastUpdated(int reason, int broadcastId) {}

                @Override
                public void onBroadcastUpdateFailed(int reason, int broadcastId) {}

                @Override
                public void onPlaybackStarted(int reason, int broadcastId) {
                    Log.d(
                            TAG,
                            "onPlaybackStarted(), reason = "
                                    + reason
                                    + ", broadcastId = "
                                    + broadcastId);
                }

                @Override
                public void onPlaybackStopped(int reason, int broadcastId) {}
            };

    @VisibleForTesting
    final BluetoothLeBroadcastAssistant.Callback mBroadcastAssistantCallback =
            new BluetoothLeBroadcastAssistant.Callback() {
                @Override
                public void onSearchStarted(int reason) {}

                @Override
                public void onSearchStartFailed(int reason) {}

                @Override
                public void onSearchStopped(int reason) {}

                @Override
                public void onSearchStopFailed(int reason) {}

                @Override
                public void onSourceFound(@NonNull BluetoothLeBroadcastMetadata source) {}

                @Override
                public void onSourceAdded(@NonNull BluetoothDevice sink, int sourceId, int reason) {
                    if (mSinksInAdding.contains(sink)) {
                        mSinksInAdding.remove(sink);
                    }
                    dismissProgressDialogIfNeeded();
                    Log.d(
                            TAG,
                            "onSourceAdded(), sink = "
                                    + sink
                                    + ", remaining sinks = "
                                    + mSinksInAdding);
                    if (mSinksToWaitFor.contains(sink)) {
                        mSinksToWaitFor.remove(sink);
                        if (mSinksToWaitFor.isEmpty() && mBroadcast != null) {
                            // To avoid users advance to share then pair flow before the
                            // primary/active sinks successfully join the audio sharing,
                            // popup dialog till adding source complete for mSinksToWaitFor.
                            ImmutableList<Pair<Integer, Object>> eventData =
                                    AudioSharingUtils.buildAudioSharingDialogEventData(
                                            SettingsEnums.AUDIO_SHARING_SETTINGS,
                                            SettingsEnums.DIALOG_AUDIO_SHARING_MAIN,
                                            /* userTriggered= */ false,
                                            /* deviceCountInSharing= */ 1,
                                            /* candidateDeviceCount= */ 0);
                            showJoinAudioSharingDialog(
                                    eventData, mBroadcast.getLatestBluetoothLeBroadcastMetadata());
                        }
                    }
                }

                @Override
                public void onSourceAddFailed(
                        @NonNull BluetoothDevice sink,
                        @NonNull BluetoothLeBroadcastMetadata source,
                        int reason) {
                    Log.d(
                            TAG,
                            "onSourceAddFailed(), sink = "
                                    + sink
                                    + ", source = "
                                    + source
                                    + ", reason = "
                                    + reason);
                    if (mSinksInAdding.contains(sink)) {
                        stopAudioSharing();
                        showErrorDialog();
                        mMetricsFeatureProvider.action(
                                mContext,
                                SettingsEnums.ACTION_AUDIO_SHARING_JOIN_FAILED,
                                SettingsEnums.AUDIO_SHARING_SETTINGS);
                    }
                }

                @Override
                public void onSourceModified(
                        @NonNull BluetoothDevice sink, int sourceId, int reason) {}

                @Override
                public void onSourceModifyFailed(
                        @NonNull BluetoothDevice sink, int sourceId, int reason) {}

                @Override
                public void onSourceRemoved(
                        @NonNull BluetoothDevice sink, int sourceId, int reason) {}

                @Override
                public void onSourceRemoveFailed(
                        @NonNull BluetoothDevice sink, int sourceId, int reason) {}

                @Override
                public void onReceiveStateChanged(
                        @NonNull BluetoothDevice sink,
                        int sourceId,
                        @NonNull BluetoothLeBroadcastReceiveState state) {
                    Log.d(
                            TAG,
                            "onReceiveStateChanged(), sink = "
                                    + sink
                                    + ", sourceId = "
                                    + sourceId
                                    + ", state = "
                                    + state);
                }
            };

    AudioSharingSwitchBarController(
            Context context,
            SettingsMainSwitchBar switchBar,
            OnAudioSharingStateChangedListener listener) {
        super(context, PREF_KEY);
        mSwitchBar = switchBar;
        mListener = listener;
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        mIntentFilter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        mBtManager = Utils.getLocalBtManager(context);
        mEventManager = mBtManager == null ? null : mBtManager.getEventManager();
        mProfileManager = mBtManager == null ? null : mBtManager.getProfileManager();
        mBroadcast = mProfileManager == null ? null : mProfileManager.getLeAudioBroadcastProfile();
        mAssistant =
                mProfileManager == null
                        ? null
                        : mProfileManager.getLeAudioBroadcastAssistantProfile();
        mExecutor = Executors.newSingleThreadExecutor();
        mMetricsFeatureProvider = FeatureFactory.getFeatureFactory().getMetricsFeatureProvider();
        mSwitchBar.getRootView().setAccessibilityDelegate(new MainSwitchAccessibilityDelegate());
    }

    @Override
    public void onStart(@NonNull LifecycleOwner owner) {
        if (!isAvailable()) {
            Log.d(TAG, "Skip register callbacks. Feature is not available.");
            return;
        }
        mContext.registerReceiver(mReceiver, mIntentFilter, Context.RECEIVER_EXPORTED_UNAUDITED);
        updateSwitch();
        registerCallbacks();
        if (!AudioSharingUtils.isAudioSharingProfileReady(mProfileManager)) {
            if (mProfileManager != null) {
                mProfileManager.addServiceListener(this);
            }
            Log.d(TAG, "Skip handleStartAudioSharingFromIntent. Profile is not ready.");
            return;
        }
        if (mIntentHandleStage.compareAndSet(
                StartIntentHandleStage.TO_HANDLE.getId(),
                StartIntentHandleStage.HANDLE_AUTO_ADD.getId())) {
            Log.d(TAG, "onStart: handleStartAudioSharingFromIntent");
            handleStartAudioSharingFromIntent();
        }
    }

    @Override
    public void onStop(@NonNull LifecycleOwner owner) {
        if (!isAvailable()) {
            Log.d(TAG, "Skip unregister callbacks. Feature is not available.");
            return;
        }
        mContext.unregisterReceiver(mReceiver);
        if (mProfileManager != null) {
            mProfileManager.removeServiceListener(this);
        }
        unregisterCallbacks();
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        // Filter out unnecessary callbacks when switch is disabled.
        if (!buttonView.isEnabled()) return;
        if (mBroadcast == null || mAssistant == null) {
            mSwitchBar.setChecked(false);
            Log.d(TAG, "Skip onCheckedChanged, profile not support.");
            return;
        }
        mSwitchBar.setEnabled(false);
        boolean isBroadcasting = BluetoothUtils.isBroadcasting(mBtManager);
        if (isChecked) {
            if (isBroadcasting) {
                Log.d(TAG, "Skip startAudioSharing, already broadcasting.");
                mSwitchBar.setEnabled(true);
                return;
            }
            // FeatureFlagUtils.SETTINGS_NEED_CONNECTED_BLE_DEVICE_FOR_BROADCAST is always true in
            // prod. We can turn off the flag for debug purpose.
            if (FeatureFlagUtils.isEnabled(
                            mContext,
                            FeatureFlagUtils.SETTINGS_NEED_CONNECTED_BLE_DEVICE_FOR_BROADCAST)
                    && hasEmptyConnectedSink()) {
                // Pop up dialog to ask users to connect at least one lea buds before audio sharing.
                AudioSharingUtils.postOnMainThread(
                        mContext,
                        () -> {
                            mSwitchBar.setEnabled(true);
                            mSwitchBar.setChecked(false);
                            AudioSharingConfirmDialogFragment.show(mFragment);
                        });
                return;
            }
            startAudioSharing();
        } else {
            if (!isBroadcasting) {
                Log.d(TAG, "Skip stopAudioSharing, already not broadcasting.");
                mSwitchBar.setEnabled(true);
                return;
            }
            stopAudioSharing();
            mMetricsFeatureProvider.action(
                    mContext, SettingsEnums.ACTION_AUDIO_SHARING_MAIN_SWITCH_OFF);
        }
    }

    @Override
    public int getAvailabilityStatus() {
        return BluetoothUtils.isAudioSharingUIAvailable(mContext)
                ? AVAILABLE
                : UNSUPPORTED_ON_DEVICE;
    }

    @Override
    public void onServiceConnected() {
        Log.d(TAG, "onServiceConnected()");
        if (AudioSharingUtils.isAudioSharingProfileReady(mProfileManager)) {
            updateSwitch();
            mListener.onAudioSharingProfilesConnected();
            mListener.onAudioSharingStateChanged();
            if (mProfileManager != null) {
                mProfileManager.removeServiceListener(this);
            }
            if (mIntentHandleStage.compareAndSet(
                    StartIntentHandleStage.TO_HANDLE.getId(),
                    StartIntentHandleStage.HANDLE_AUTO_ADD.getId())) {
                Log.d(TAG, "onServiceConnected: handleStartAudioSharingFromIntent");
                handleStartAudioSharingFromIntent();
            }
        }
    }

    @Override
    public void onServiceDisconnected() {
        Log.d(TAG, "onServiceDisconnected()");
        // Do nothing.
    }

    @Override
    public void onActiveDeviceChanged(
            @Nullable CachedBluetoothDevice activeDevice, int bluetoothProfile) {
        if (activeDevice != null) {
            Log.d(
                    TAG,
                    "onActiveDeviceChanged: device = "
                            + activeDevice.getDevice().getAnonymizedAddress()
                            + ", profile = "
                            + bluetoothProfile);
            updateSwitch();
        }
    }

    /**
     * Initialize the controller.
     *
     * @param fragment The fragment to host the {@link AudioSharingSwitchBarController} dialog.
     * @param needHandleIntent Indicates if need to handle intent to start sharing.
     *                         When screen rotates, the controller will be re-created, this param
     *                         is set false to avoid the intent being re-handled unexpectedly.
     */
    public void init(@NonNull Fragment fragment, boolean needHandleIntent) {
        this.mFragment = fragment;
        if (!needHandleIntent) {
            mIntentHandleStage.set(StartIntentHandleStage.UNSUPPORTED.getId());
        }
    }

    /** Handle auto add source to the just paired device in share then pair flow. */
    public void handleAutoAddSourceAfterPair(@NonNull BluetoothDevice device) {
        CachedBluetoothDeviceManager deviceManager =
                mBtManager == null ? null : mBtManager.getCachedDeviceManager();
        CachedBluetoothDevice cachedDevice =
                deviceManager == null ? null : deviceManager.findDevice(device);
        if (cachedDevice != null && mBroadcast != null) {
            Log.d(TAG, "handleAutoAddSourceAfterPair, device = " + device.getAnonymizedAddress());
            addSourceToTargetSinks(
                    ImmutableList.of(device),
                    cachedDevice.getName(),
                    mBroadcast.getLatestBluetoothLeBroadcastMetadata(),
                    AudioSharingUtils.buildAddSourceEventData(
                            SettingsEnums.BLUETOOTH_PAIRING, /* userTriggered= */ true));
        }
    }

    /** Test only: set callback registration status in tests. */
    @VisibleForTesting
    void setCallbacksRegistered(boolean registered) {
        mCallbacksRegistered.set(registered);
    }

    private void registerCallbacks() {
        if (!isAvailable()) {
            Log.d(TAG, "Skip registerCallbacks(). Feature is not available.");
            return;
        }
        if (mBroadcast == null || mAssistant == null || mEventManager == null) {
            Log.d(TAG, "Skip registerCallbacks(). Profile not support on this device.");
            return;
        }
        if (!mCallbacksRegistered.get()) {
            Log.d(TAG, "registerCallbacks()");
            mSwitchBar.addOnSwitchChangeListener(this);
            mBroadcast.registerServiceCallBack(mExecutor, mBroadcastCallback);
            mAssistant.registerServiceCallBack(mExecutor, mBroadcastAssistantCallback);
            mEventManager.registerCallback(this);
            mCallbacksRegistered.set(true);
        }
    }

    private void unregisterCallbacks() {
        if (!isAvailable()) {
            Log.d(TAG, "Skip unregisterCallbacks(). Feature is not available.");
            return;
        }
        if (mBroadcast == null || mAssistant == null || mEventManager == null) {
            Log.d(TAG, "Skip unregisterCallbacks(). Profile not support on this device.");
            return;
        }
        if (mCallbacksRegistered.get()) {
            Log.d(TAG, "unregisterCallbacks()");
            mSwitchBar.removeOnSwitchChangeListener(this);
            mBroadcast.unregisterServiceCallBack(mBroadcastCallback);
            mAssistant.unregisterServiceCallBack(mBroadcastAssistantCallback);
            mEventManager.unregisterCallback(this);
            mCallbacksRegistered.set(false);
        }
    }

    private void startAudioSharing() {
        // Compute the device connection state before start audio sharing since the devices will
        // be set to inactive after the broadcast started.
        mGroupedConnectedDevices = AudioSharingUtils.fetchConnectedDevicesByGroupId(mBtManager);
        List<AudioSharingDeviceItem> deviceItems =
                AudioSharingUtils.buildOrderedConnectedLeadAudioSharingDeviceItem(
                        mBtManager, mGroupedConnectedDevices, /* filterByInSharing= */ false);
        // deviceItems is ordered. The active device is the first place if exits.
        mDeviceItemsForSharing = new ArrayList<>(deviceItems);
        mTargetActiveItem = null;
        if (!deviceItems.isEmpty() && deviceItems.get(0).isActive()) {
            // If active device exists for audio sharing, share to it
            // automatically once the broadcast is started.
            mTargetActiveItem = deviceItems.get(0);
            mDeviceItemsForSharing.remove(0);
        }
        if (mBroadcast != null) {
            mStartingSharing.set(true);
            mBroadcast.startPrivateBroadcast();
            mSinksInAdding.clear();
            AudioSharingUtils.postOnMainThread(mContext,
                    () -> AudioSharingProgressDialogFragment.show(mFragment,
                            mContext.getString(
                                    R.string.audio_sharing_progress_dialog_start_stream_content)));
            logStartBroadcast(deviceItems.size());
        }
    }

    private void stopAudioSharing() {
        if (mBroadcast != null) {
            int broadcastId = mBroadcast.getLatestBroadcastId();
            if (broadcastId != -1) {
                mBroadcast.stopBroadcast(broadcastId);
                mStoppingSharing.set(true);
                mSinksInAdding.clear();
                mSinksToWaitFor.clear();
            }
            cleanUpStatesForStartSharing();
        }
    }

    private void updateSwitch() {
        var unused =
                ThreadUtils.postOnBackgroundThread(
                        () -> {
                            boolean isBroadcasting = BluetoothUtils.isBroadcasting(mBtManager);
                            boolean hasActiveDevice =
                                    AudioSharingUtils.hasActiveConnectedLeadDevice(mBtManager);
                            boolean hasEmptyConnectedDevice = hasEmptyConnectedSink();
                            boolean isStateReady =
                                    isBluetoothOn()
                                            && AudioSharingUtils.isAudioSharingProfileReady(
                                                    mProfileManager)
                                            && (isBroadcasting
                                                    // Always enable toggle when no connected sink.
                                                    // We have dialog to guide users to connect
                                                    // compatible devices for audio sharing.
                                                    || hasEmptyConnectedDevice
                                                    // Disable toggle till device gets active after
                                                    // broadcast ends.
                                                    || hasActiveDevice);
                            AudioSharingUtils.postOnMainThread(
                                    mContext,
                                    () -> {
                                        if (mSwitchBar.isChecked() != isBroadcasting) {
                                            mSwitchBar.setChecked(isBroadcasting);
                                        }
                                        if (mSwitchBar.isEnabled() != isStateReady) {
                                            mSwitchBar.setEnabled(isStateReady);
                                        }
                                        Log.d(
                                                TAG,
                                                "updateSwitch, checked = "
                                                        + isBroadcasting
                                                        + ", enabled = "
                                                        + isStateReady);
                                    });
                        });
    }

    private boolean isBluetoothOn() {
        return mBluetoothAdapter != null && mBluetoothAdapter.isEnabled();
    }

    private boolean hasEmptyConnectedSink() {
        return mAssistant != null && mAssistant.getAllConnectedDevices().isEmpty();
    }

    private void handleOnBroadcastReady(@NonNull BluetoothLeBroadcastMetadata metadata) {
        List<BluetoothDevice> targetActiveSinks =
                mTargetActiveItem == null
                        ? ImmutableList.of()
                        : mGroupedConnectedDevices.getOrDefault(
                                mTargetActiveItem.getGroupId(), ImmutableList.of());
        ImmutableList<Pair<Integer, Object>> eventData =
                AudioSharingUtils.buildAudioSharingDialogEventData(
                        SettingsEnums.AUDIO_SHARING_SETTINGS,
                        SettingsEnums.DIALOG_AUDIO_SHARING_MAIN,
                        /* userTriggered= */ false,
                        /* deviceCountInSharing= */ targetActiveSinks.isEmpty() ? 0 : 1,
                        /* candidateDeviceCount= */ mDeviceItemsForSharing.size());
        // Auto add primary/active sinks w/o user interactions.
        if (!targetActiveSinks.isEmpty() && mTargetActiveItem != null) {
            Log.d(TAG, "handleOnBroadcastReady: automatically add source to active sinks.");
            addSourceToTargetSinks(
                    targetActiveSinks,
                    mTargetActiveItem.getName(),
                    metadata,
                    AudioSharingUtils.buildAddSourceEventData(
                            SettingsEnums.ACTION_AUTO_JOIN_AUDIO_SHARING,
                            /* userTriggered= */ false));
            // To avoid users advance to share then pair flow before the primary/active sinks
            // successfully join the audio sharing, save the primary/active sinks in mSinksToWaitFor
            // and popup dialog till adding source complete for these sinks.
            if (mDeviceItemsForSharing.isEmpty()) {
                mSinksToWaitFor.clear();
                mSinksToWaitFor.addAll(targetActiveSinks);
            }
            mTargetActiveItem = null;
            // When audio sharing page is brought up by intent with EXTRA_START_LE_AUDIO_SHARING
            // == true, plus there is one active lea headset and one connected lea headset, we
            // should auto add these sinks without user interactions.
            if (mIntentHandleStage.compareAndSet(
                            StartIntentHandleStage.HANDLE_AUTO_ADD.getId(),
                            StartIntentHandleStage.HANDLED.getId())
                    && mDeviceItemsForSharing.size() == 1) {
                Log.d(TAG, "handleOnBroadcastReady: auto add source to the second device");
                AudioSharingDeviceItem target = mDeviceItemsForSharing.get(0);
                List<BluetoothDevice> targetSinks =
                        mGroupedConnectedDevices.getOrDefault(
                                target.getGroupId(), ImmutableList.of());
                addSourceToTargetSinks(
                        targetSinks,
                        target.getName(),
                        metadata,
                        AudioSharingUtils.buildAddSourceEventData(
                                SettingsEnums.ACTION_AUTO_JOIN_AUDIO_SHARING,
                                /* userTriggered= */ true));
                cleanUpStatesForStartSharing();
                return;
            }
        }
        // Still mark intent as handled if early returned due to preconditions not met
        mIntentHandleStage.compareAndSet(
                StartIntentHandleStage.HANDLE_AUTO_ADD.getId(),
                StartIntentHandleStage.HANDLED.getId());
        if (mFragment == null) {
            Log.d(TAG, "handleOnBroadcastReady: dialog fail to show due to null fragment.");
            // Clean up states before early return.
            dismissProgressDialogIfNeeded();
            cleanUpStatesForStartSharing();
            return;
        }
        // To avoid users advance to share then pair flow before the primary/active sinks
        // successfully join the audio sharing, popup dialog till adding source complete for
        // mSinksToWaitFor.
        if (mSinksToWaitFor.isEmpty() && !mStoppingSharing.get()) {
            showJoinAudioSharingDialog(eventData, metadata);
        }
    }

    private void showJoinAudioSharingDialog(
            ImmutableList<Pair<Integer, Object>> eventData,
            @Nullable BluetoothLeBroadcastMetadata metadata) {
        if (!BluetoothUtils.isBroadcasting(mBtManager)) {
            Log.d(TAG, "Skip showJoinAudioSharingDialog, broadcast is stopped");
            return;
        }
        AudioSharingDialogFragment.DialogEventListener listener =
                new AudioSharingDialogFragment.DialogEventListener() {
                    @Override
                    public void onPositiveClick() {
                        // Could go to other pages (pair new device), dismiss the progress dialog.
                        dismissProgressDialogIfNeeded();
                        cleanUpStatesForStartSharing();
                    }

                    @Override
                    public void onItemClick(@NonNull AudioSharingDeviceItem item) {
                        List<BluetoothDevice> targetSinks =
                                mGroupedConnectedDevices.getOrDefault(
                                        item.getGroupId(), ImmutableList.of());
                        addSourceToTargetSinks(
                                targetSinks,
                                item.getName(),
                                metadata,
                                AudioSharingUtils.buildAddSourceEventData(
                                        SettingsEnums.DIALOG_AUDIO_SHARING_MAIN,
                                        /* userTriggered= */ true));
                        cleanUpStatesForStartSharing();
                    }

                    @Override
                    public void onCancelClick() {
                        // Could go to other pages (show qr code), dismiss the progress dialog.
                        dismissProgressDialogIfNeeded();
                        cleanUpStatesForStartSharing();
                    }
                };
        AudioSharingUtils.postOnMainThread(
                mContext,
                () ->
                        AudioSharingDialogFragment.show(
                                mFragment, mDeviceItemsForSharing, metadata, listener, eventData));
    }

    private void showErrorDialog() {
        AudioSharingUtils.postOnMainThread(
                mContext,
                () -> {
                    // Remove all stale dialogs before showing error dialog
                    dismissStaleDialogsOtherThanErrorDialog();
                    AudioSharingErrorDialogFragment.show(mFragment);
                });
    }

    @UiThread
    private void dismissStaleDialogsOtherThanErrorDialog() {
        List<Fragment> fragments = new ArrayList<Fragment>();
        try {
            if (mFragment != null) {
                fragments = mFragment.getChildFragmentManager().getFragments();
            }
        } catch (Exception e) {
            Log.e(TAG, "Fail to dismiss stale dialogs: " + e.getMessage());
        }
        for (Fragment fragment : fragments) {
            if (fragment != null
                    && fragment instanceof DialogFragment
                    && !(fragment instanceof AudioSharingErrorDialogFragment)
                    && ((DialogFragment) fragment).getDialog() != null) {
                Log.d(TAG, "Remove stale dialog = " + fragment.getTag());
                ((DialogFragment) fragment).dismissAllowingStateLoss();
            }
        }
    }

    private static final class MainSwitchAccessibilityDelegate extends View.AccessibilityDelegate {
        @Override
        public boolean onRequestSendAccessibilityEvent(
                @NonNull ViewGroup host, @NonNull View view, @NonNull AccessibilityEvent event) {
            if (event.getEventType() == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
                    && (event.getContentChangeTypes()
                                    & AccessibilityEvent.CONTENT_CHANGE_TYPE_ENABLED)
                            != 0) {
                Log.d(TAG, "Skip accessibility event for CONTENT_CHANGE_TYPE_ENABLED");
                return false;
            }
            return super.onRequestSendAccessibilityEvent(host, view, event);
        }
    }

    private void handleStartAudioSharingFromIntent() {
        var unused =
                ThreadUtils.postOnBackgroundThread(
                        () -> {
                            if (mFragment == null
                                    || mFragment.getActivity() == null
                                    || mFragment.getActivity().getIntent() == null) {
                                Log.d(
                                        TAG,
                                        "Skip handleStartAudioSharingFromIntent, "
                                                + "fragment intent is null");
                                return;
                            }
                            Intent intent = mFragment.getActivity().getIntent();
                            Bundle args =
                                    intent.getBundleExtra(
                                            SettingsActivity.EXTRA_SHOW_FRAGMENT_ARGUMENTS);
                            Boolean shouldStart =
                                    args != null
                                            && args.getBoolean(EXTRA_START_LE_AUDIO_SHARING, false);
                            if (!shouldStart) {
                                Log.d(TAG, "Skip handleStartAudioSharingFromIntent, arg false");
                                mIntentHandleStage.compareAndSet(
                                        StartIntentHandleStage.HANDLE_AUTO_ADD.getId(),
                                        StartIntentHandleStage.HANDLED.getId());
                                return;
                            }
                            if (BluetoothUtils.isBroadcasting(mBtManager)) {
                                Log.d(TAG, "Skip handleStartAudioSharingFromIntent, in broadcast");
                                mIntentHandleStage.compareAndSet(
                                        StartIntentHandleStage.HANDLE_AUTO_ADD.getId(),
                                        StartIntentHandleStage.HANDLED.getId());
                                return;
                            }
                            Log.d(TAG, "HandleStartAudioSharingFromIntent, start broadcast");
                            //
                            AudioSharingUtils.postOnMainThread(
                                    mContext, () -> mSwitchBar.setChecked(true));
                        });
    }

    private void addSourceToTargetSinks(
            List<BluetoothDevice> targetGroupedSinks,
            @NonNull String targetSinkName,
            @Nullable BluetoothLeBroadcastMetadata metadata,
            ImmutableList<Pair<Integer, Object>> eventData) {
        if (targetGroupedSinks.isEmpty()) {
            Log.d(TAG, "Skip addSourceToTargetSinks, no sinks.");
            return;
        }
        if (metadata == null) {
            Log.d(TAG, "Skip addSourceToTargetSinks, metadata is null");
            return;
        }
        if (mAssistant == null) {
            Log.d(TAG, "skip addSourceToTargetDevices, assistant profile is null.");
            return;
        }
        mSinksInAdding.addAll(targetGroupedSinks);
        String progressMessage =
                mContext.getString(
                        R.string.audio_sharing_progress_dialog_add_source_content, targetSinkName);
        showProgressDialog(progressMessage);
        for (BluetoothDevice sink : targetGroupedSinks) {
            mAssistant.addSource(sink, metadata, /* isGroupOp= */ false);
        }
        mMetricsFeatureProvider.action(
                SettingsEnums.AUDIO_SHARING_SETTINGS,
                SettingsEnums.ACTION_AUDIO_SHARING_ADD_SOURCE,
                SettingsEnums.AUDIO_SHARING_SETTINGS,
                eventData.toString(),
                /* changedPreferenceIntValue= */ 0);
    }

    private void showProgressDialog(@NonNull String progressMessage) {
        AudioSharingUtils.postOnMainThread(
                mContext,
                () -> AudioSharingProgressDialogFragment.show(mFragment, progressMessage));
    }

    private void dismissProgressDialogIfNeeded() {
        if (mSinksInAdding.isEmpty()) {
            AudioSharingUtils.postOnMainThread(
                    mContext, () -> AudioSharingProgressDialogFragment.dismiss(mFragment));
        }
    }

    private void cleanUpStatesForStartSharing() {
        mGroupedConnectedDevices.clear();
        mDeviceItemsForSharing.clear();
    }

    private void logStartBroadcast(int candidateDeviceCount) {
        int sourceMetric = SettingsEnums.PAGE_UNKNOWN;
        String callingPackage = "";
        if (mIntentHandleStage.get() != StartIntentHandleStage.HANDLE_AUTO_ADD.getId()) {
            // Sharing is started by toggle
            sourceMetric = SettingsEnums.AUDIO_SHARING_SETTINGS;
            callingPackage = mContext.getPackageName();
        } else if (mFragment != null && mFragment.getActivity() != null) {
            // Sharing is started by intent
            FragmentActivity activity = mFragment.getActivity();
            Intent intent = activity.getIntent();
            if (intent != null) {
                sourceMetric = intent.getIntExtra(EXTRA_SOURCE_METRICS, SettingsEnums.PAGE_UNKNOWN);
            }
            if (activity instanceof SettingsActivity settingsActivity) {
                callingPackage = settingsActivity.getInitialCallingPackage();
            }
        }
        ImmutableList<Pair<Integer, Object>> eventData =
                ImmutableList.of(
                        Pair.create(
                                AudioSharingUtils.MetricKey.METRIC_KEY_SOURCE_PAGE_ID.getId(),
                                sourceMetric),
                        Pair.create(
                                AudioSharingUtils.MetricKey.METRIC_KEY_SOURCE_PACKAGE_NAME.getId(),
                                callingPackage),
                        Pair.create(
                                AudioSharingUtils.MetricKey.METRIC_KEY_CANDIDATE_DEVICE_COUNT
                                        .getId(),
                                candidateDeviceCount));
        mMetricsFeatureProvider.action(
                SettingsEnums.AUDIO_SHARING_SETTINGS,
                SettingsEnums.ACTION_AUDIO_SHARING_MAIN_SWITCH_ON,
                SettingsEnums.AUDIO_SHARING_SETTINGS,
                eventData.toString(),
                /* changedPreferenceIntValue= */ 0);
    }

    private enum StartIntentHandleStage {
        TO_HANDLE(0),
        HANDLE_AUTO_ADD(1),
        HANDLED(2),
        UNSUPPORTED(3);

        private final int mId;

        StartIntentHandleStage(int id) {
            this.mId = id;
        }

        public int getId() {
            return mId;
        }
    }
}
