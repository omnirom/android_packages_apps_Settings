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

package com.android.settings.connecteddevice.audiosharing.audiostreams;

import static com.android.settings.connecteddevice.audiosharing.audiostreams.AudioStreamsHelper.getEnabledScreenReaderServices;
import static com.android.settings.connecteddevice.audiosharing.audiostreams.AudioStreamsHelper.setAccessibilityServiceOff;
import static com.android.settingslib.bluetooth.BluetoothUtils.isAudioSharingHysteresisModeFixAvailable;
import static com.android.settingslib.bluetooth.LocalBluetoothLeBroadcastAssistant.LocalBluetoothLeBroadcastSourceState.DECRYPTION_FAILED;
import static com.android.settingslib.bluetooth.LocalBluetoothLeBroadcastAssistant.LocalBluetoothLeBroadcastSourceState.PAUSED;
import static com.android.settingslib.bluetooth.LocalBluetoothLeBroadcastAssistant.LocalBluetoothLeBroadcastSourceState.STREAMING;
import static com.android.settingslib.bluetooth.LocalBluetoothLeBroadcastAssistant.getLocalSourceState;

import static java.util.stream.Collectors.toMap;

import android.app.settings.SettingsEnums;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothLeBroadcastMetadata;
import android.bluetooth.BluetoothLeBroadcastReceiveState;
import android.bluetooth.BluetoothProfile;
import android.content.ComponentName;
import android.content.Context;
import android.util.Log;
import android.view.accessibility.AccessibilityManager;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.preference.PreferenceScreen;

import com.android.internal.annotations.VisibleForTesting;
import com.android.settings.R;
import com.android.settings.bluetooth.Utils;
import com.android.settings.connecteddevice.ConnectedDeviceDashboardFragment;
import com.android.settings.connecteddevice.audiosharing.AudioSharingUtils;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.core.SubSettingLauncher;
import com.android.settingslib.bluetooth.BluetoothCallback;
import com.android.settingslib.bluetooth.BluetoothUtils;
import com.android.settingslib.bluetooth.CachedBluetoothDevice;
import com.android.settingslib.bluetooth.LocalBluetoothLeBroadcastAssistant;
import com.android.settingslib.bluetooth.LocalBluetoothManager;
import com.android.settingslib.utils.ThreadUtils;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import javax.annotation.Nullable;

public class AudioStreamsProgressCategoryController extends BasePreferenceController
        implements DefaultLifecycleObserver,
                AudioStreamsProgressCategoryCallback.SourceStateListener {
    private static final String TAG = "AudioStreamsProgressCategoryController";
    private static final boolean DEBUG = BluetoothUtils.D;
    static final int UNSET_BROADCAST_ID = -1;

    @VisibleForTesting
    final BluetoothCallback mBluetoothCallback =
            new BluetoothCallback() {
                @Override
                public void onBluetoothStateChanged(@AdapterState int bluetoothState) {
                    Log.d(TAG, "onBluetoothStateChanged() with bluetoothState : " + bluetoothState);
                    if (bluetoothState == BluetoothAdapter.STATE_OFF) {
                        mExecutor.execute(() -> init());
                    }
                }

                @Override
                public void onProfileConnectionStateChanged(
                        @NonNull CachedBluetoothDevice cachedDevice,
                        @ConnectionState int state,
                        int bluetoothProfile) {
                    Log.d(
                            TAG,
                            "onProfileConnectionStateChanged() with cachedDevice : "
                                    + cachedDevice.getAddress()
                                    + " with state : "
                                    + state
                                    + " on profile : "
                                    + bluetoothProfile);
                    if (bluetoothProfile == BluetoothProfile.LE_AUDIO_BROADCAST_ASSISTANT
                            && (state == BluetoothAdapter.STATE_CONNECTED
                                    || state == BluetoothAdapter.STATE_DISCONNECTED)) {
                        mExecutor.execute(() -> init());
                    }
                }
            };

    private final AccessibilityManager.AccessibilityServicesStateChangeListener
            mAccessibilityListener = manager -> init();

    private final Comparator<AudioStreamPreference> mComparator =
            Comparator.<AudioStreamPreference, Boolean>comparing(
                            p ->
                                    (p.getAudioStreamState() == AudioStreamState.SOURCE_ADDED
                                            || (isAudioSharingHysteresisModeFixAvailable(mContext)
                                                    && p.getAudioStreamState()
                                                            == AudioStreamState.SOURCE_PRESENT)))
                    .thenComparingInt(AudioStreamPreference::getAudioStreamRssi)
                    .reversed();

    public enum AudioStreamState {
        UNKNOWN, // When mSourceFromQrCode is present and this source has not been synced.
        WAIT_FOR_SYNC, // When source has been synced but not added to any sink.
        SYNCED, // When addSource is called for this source and waiting for response.
        ADD_SOURCE_WAIT_FOR_RESPONSE, // When addSource is called for this source and waiting for
        // response from a QR code scan.
        ADD_SOURCE_WAIT_FOR_RESPONSE_FROM_QR, // When addSource result in a bad code response.
        ADD_SOURCE_BAD_CODE, // When addSource result in other bad state.
        ADD_SOURCE_FAILED, // Source is present on sink.
        SOURCE_PRESENT, // Source is added to active sink.
        SOURCE_ADDED, // Source is no longer synced or wait for sync timed out
        SOURCE_LOST,
    }

    @VisibleForTesting Executor mExecutor;
    private final AudioStreamsProgressCategoryCallback mBroadcastAssistantCallback;
    private final AudioStreamsHelper mAudioStreamsHelper;
    private final MediaControlHelper mMediaControlHelper;
    private final @Nullable LocalBluetoothLeBroadcastAssistant mLeBroadcastAssistant;
    private final @Nullable LocalBluetoothManager mBluetoothManager;
    private final ConcurrentHashMap<Integer, AudioStreamPreference> mBroadcastIdToPreferenceMap =
            new ConcurrentHashMap<>();
    private final boolean mHysteresisModeFixAvailable;
    private final AudioStreamScanHelper mScanHelper;
    private @Nullable BluetoothLeBroadcastMetadata mSourceFromQrCode;
    private SourceOriginForLogging mSourceFromQrCodeOriginForLogging;
    @Nullable private AudioStreamsProgressCategoryPreference mCategoryPreference;
    @Nullable private Fragment mFragment;
    @Nullable AccessibilityManager mAccessibilityManager;

    public AudioStreamsProgressCategoryController(Context context, String preferenceKey) {
        super(context, preferenceKey);
        mExecutor = Executors.newSingleThreadExecutor();
        mBluetoothManager = Utils.getLocalBtManager(mContext);
        mAudioStreamsHelper = new AudioStreamsHelper(mBluetoothManager);
        mMediaControlHelper = new MediaControlHelper(mContext, mBluetoothManager);
        mLeBroadcastAssistant = mAudioStreamsHelper.getLeBroadcastAssistant();
        mScanHelper =
                new AudioStreamScanHelper(
                        mExecutor, mLeBroadcastAssistant, this::setScanningIconSpinning);
        mBroadcastAssistantCallback = new AudioStreamsProgressCategoryCallback();
        mHysteresisModeFixAvailable =
                BluetoothUtils.isAudioSharingHysteresisModeFixAvailable(mContext);
        mAccessibilityManager = context.getSystemService(AccessibilityManager.class);
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mCategoryPreference = screen.findPreference(getPreferenceKey());
    }

    @Override
    public void onStart(@NonNull LifecycleOwner owner) {
        if (mBluetoothManager != null) {
            mBluetoothManager.getEventManager().registerCallback(mBluetoothCallback);
        }
        if (mAccessibilityManager != null) {
            mAccessibilityManager.addAccessibilityServicesStateChangeListener(
                    mExecutor, mAccessibilityListener);
        }
        mExecutor.execute(this::init);
    }

    @Override
    public void onStop(@NonNull LifecycleOwner owner) {
        if (mBluetoothManager != null) {
            mBluetoothManager.getEventManager().unregisterCallback(mBluetoothCallback);
        }
        if (mAccessibilityManager != null) {
            mAccessibilityManager.removeAccessibilityServicesStateChangeListener(
                    mAccessibilityListener);
        }
        mExecutor.execute(this::stopScanningAndCleanUp);
    }

    void setFragment(Fragment fragment) {
        mFragment = fragment;
    }

    @Nullable
    Fragment getFragment() {
        return mFragment;
    }

    void setSourceFromQrCode(
            BluetoothLeBroadcastMetadata source, SourceOriginForLogging sourceOriginForLogging) {
        if (DEBUG) {
            Log.d(TAG, "setSourceFromQrCode(): broadcastId " + source.getBroadcastId());
        }
        mSourceFromQrCode = source;
        mSourceFromQrCodeOriginForLogging = sourceOriginForLogging;
    }

    void setScanningIconSpinning(boolean isScanning) {
        ThreadUtils.postOnMainThread(
                () -> {
                    if (mCategoryPreference != null) mCategoryPreference.setProgress(isScanning);
                });
    }

    // Find preference by scanned source and decide next state.
    // Expect one of the following:
    // 1) No preference existed, create new preference with state SYNCED
    // 2) WAIT_FOR_SYNC, move to ADD_SOURCE_WAIT_FOR_RESPONSE_FROM_QR
    // 3) SOURCE_ADDED, leave as-is
    @Override
    public void handleSourceFound(@NonNull BluetoothLeBroadcastMetadata source) {
        if (DEBUG) {
            Log.d(TAG, "handleSourceFound()");
        }
        var broadcastIdFound = source.getBroadcastId();

        if (mSourceFromQrCode != null && mSourceFromQrCode.getBroadcastId() == UNSET_BROADCAST_ID) {
            // mSourceFromQrCode could have no broadcast Id, we fill in the broadcast Id from the
            // scanned metadata.
            if (DEBUG) {
                Log.d(
                        TAG,
                        "handleSourceFound() : processing mSourceFromQrCode with broadcastId"
                                + " unset");
            }
            boolean updated =
                    maybeUpdateId(
                            AudioStreamsHelper.getBroadcastName(source), source.getBroadcastId());
            if (updated && mBroadcastIdToPreferenceMap.containsKey(UNSET_BROADCAST_ID)) {
                var preference = mBroadcastIdToPreferenceMap.remove(UNSET_BROADCAST_ID);
                mBroadcastIdToPreferenceMap.put(source.getBroadcastId(), preference);
            }
        }

        mBroadcastIdToPreferenceMap.compute(
                broadcastIdFound,
                (k, existingPreference) -> {
                    if (existingPreference == null) {
                        return addNewPreference(
                                source,
                                AudioStreamState.SYNCED,
                                SourceOriginForLogging.BROADCAST_SEARCH);
                    }
                    var fromState = existingPreference.getAudioStreamState();
                    if (fromState == AudioStreamState.WAIT_FOR_SYNC && mSourceFromQrCode != null) {
                        // A preference with source founded is existed from a QR code scan. As the
                        // source is now synced, we update the preference with source from scanning
                        // as it includes complete broadcast info.
                        existingPreference.setAudioStreamMetadata(
                                new BluetoothLeBroadcastMetadata.Builder(source)
                                        .setBroadcastCode(mSourceFromQrCode.getBroadcastCode())
                                        .build());
                        moveToState(
                                existingPreference,
                                AudioStreamState.ADD_SOURCE_WAIT_FOR_RESPONSE_FROM_QR);
                    } else {
                        // A preference with source founded existed either because it's already
                        // connected (SOURCE_ADDED) or present (SOURCE_PRESENT). Any other reason
                        // is unexpected. We update the preference with this source and won't
                        // change it's state.
                        existingPreference.setAudioStreamMetadata(source);
                        if (fromState != AudioStreamState.SOURCE_ADDED
                                && (!mHysteresisModeFixAvailable
                                        || fromState != AudioStreamState.SOURCE_PRESENT)) {
                            Log.w(
                                    TAG,
                                    "handleSourceFound(): unexpected state : "
                                            + fromState
                                            + " for broadcastId : "
                                            + broadcastIdFound);
                        }
                    }
                    return existingPreference;
                });
    }

    private boolean maybeUpdateId(String targetBroadcastName, int broadcastIdToSet) {
        if (mSourceFromQrCode == null) {
            return false;
        }
        if (targetBroadcastName.equals(AudioStreamsHelper.getBroadcastName(mSourceFromQrCode))) {
            if (DEBUG) {
                Log.d(
                        TAG,
                        "maybeUpdateId() : updating unset broadcastId for metadataFromQrCode with"
                                + " broadcastName: "
                                + AudioStreamsHelper.getBroadcastName(mSourceFromQrCode)
                                + " to broadcast Id: "
                                + broadcastIdToSet);
            }
            mSourceFromQrCode =
                    new BluetoothLeBroadcastMetadata.Builder(mSourceFromQrCode)
                            .setBroadcastId(broadcastIdToSet)
                            .build();
            return true;
        }
        return false;
    }

    // Find preference by mSourceFromQrCode and decide next state.
    // Expect no preference existed, create new preference with state WAIT_FOR_SYNC
    private void handleSourceFromQrCodeIfExists() {
        if (DEBUG) {
            Log.d(TAG, "handleSourceFromQrCodeIfExists()");
        }
        if (mSourceFromQrCode == null) {
            return;
        }
        mBroadcastIdToPreferenceMap.compute(
                mSourceFromQrCode.getBroadcastId(),
                (k, existingPreference) -> {
                    if (existingPreference == null) {
                        // No existing preference for this source from the QR code scan, add one and
                        // set initial state to WAIT_FOR_SYNC.
                        // Check nullability to bypass NullAway check.
                        if (mSourceFromQrCode != null) {
                            return addNewPreference(
                                    mSourceFromQrCode,
                                    AudioStreamState.WAIT_FOR_SYNC,
                                    mSourceFromQrCodeOriginForLogging);
                        }
                    }
                    Log.w(
                            TAG,
                            "handleSourceFromQrCodeIfExists(): unexpected state : "
                                    + existingPreference.getAudioStreamState()
                                    + " for broadcastId : "
                                    + (mSourceFromQrCode == null
                                            ? "null"
                                            : mSourceFromQrCode.getBroadcastId()));
                    return existingPreference;
                });
    }

    @Override
    public void handleSourceLost(int broadcastId) {
        if (DEBUG) {
            Log.d(TAG, "handleSourceLost()");
        }
        if (mAudioStreamsHelper
                .getConnectedBroadcastIdAndState(mHysteresisModeFixAvailable)
                .containsKey(broadcastId)) {
            Log.d(
                    TAG,
                    "handleSourceLost() : keep this preference as the source is still connected.");
            return;
        }
        mBroadcastIdToPreferenceMap.compute(
                broadcastId,
                (k, existingPreference) -> {
                    if (existingPreference != null) {
                        moveToState(existingPreference, AudioStreamState.SOURCE_LOST);
                    }
                    return null;
                });
    }

    @Override
    public void handleSourceRemoved() {
        if (DEBUG) {
            Log.d(TAG, "handleSourceRemoved()");
        }
        for (var entry : mBroadcastIdToPreferenceMap.entrySet()) {
            var preference = entry.getValue();

            // Look for preference has SOURCE_ADDED or SOURCE_PRESENT state, re-check if they are
            // still connected. If
            // not, means the source is removed from the sink, we move back the preference to SYNCED
            // state.
            if ((preference.getAudioStreamState() == AudioStreamState.SOURCE_ADDED
                            || (mHysteresisModeFixAvailable
                                    && preference.getAudioStreamState()
                                            == AudioStreamState.SOURCE_PRESENT))
                    && !mAudioStreamsHelper
                            .getConnectedBroadcastIdAndState(mHysteresisModeFixAvailable)
                            .containsKey(preference.getAudioStreamBroadcastId())) {

                ThreadUtils.postOnMainThread(
                        () -> {
                            var metadata = preference.getAudioStreamMetadata();

                            if (metadata != null) {
                                moveToState(preference, AudioStreamState.SYNCED);
                            } else {
                                handleSourceLost(preference.getAudioStreamBroadcastId());
                            }
                        });

                return;
            }
        }
    }

    // Find preference by receiveState and decide next state.
    // Expect one of the following:
    // 1) No preference existed, create new preference with state SOURCE_ADDED
    // 2) Any other state, move to SOURCE_ADDED
    @Override
    public void handleSourceStreaming(
            @NonNull BluetoothDevice device,
            @NonNull BluetoothLeBroadcastReceiveState receiveState) {
        if (DEBUG) {
            Log.d(TAG, "handleSourceStreaming()");
        }
        if (getLocalSourceState(receiveState) != STREAMING) {
            return;
        }
        var broadcastIdStreaming = receiveState.getBroadcastId();
        Optional<BluetoothLeBroadcastMetadata> metadata =
                getMetadataMatchingByBroadcastId(
                        device, receiveState.getSourceId(), broadcastIdStreaming);
        handleQrCodeWithUnsetBroadcastIdIfNeeded(metadata, receiveState);
        mBroadcastIdToPreferenceMap.compute(
                broadcastIdStreaming,
                (k, existingPreference) -> {
                    if (existingPreference == null) {
                        // No existing preference for this source even if it's already streaming,
                        // add one and set initial state to SOURCE_ADDED. This could happen because
                        // we retrieves the streaming source during onStart() from
                        // AudioStreamsHelper#getAllStreamingSources() even before the source is
                        // founded by scanning.
                        return metadata.isPresent()
                                ? addNewPreference(
                                        metadata.get(),
                                        AudioStreamState.SOURCE_ADDED,
                                        SourceOriginForLogging.UNKNOWN)
                                : addNewPreference(receiveState, AudioStreamState.SOURCE_ADDED);
                    }
                    if (existingPreference.getAudioStreamState() == AudioStreamState.WAIT_FOR_SYNC
                            && existingPreference.getAudioStreamBroadcastId() == UNSET_BROADCAST_ID
                            && mSourceFromQrCode != null) {
                        existingPreference.setAudioStreamMetadata(mSourceFromQrCode);
                    }
                    moveToState(existingPreference, AudioStreamState.SOURCE_ADDED);
                    return existingPreference;
                });
    }

    // Find preference by receiveState and decide next state.
    // Expect one preference existed, move to ADD_SOURCE_BAD_CODE
    @Override
    public void handleSourceConnectBadCode(@NonNull BluetoothLeBroadcastReceiveState receiveState) {
        if (DEBUG) {
            Log.d(TAG, "handleSourceConnectBadCode()");
        }
        if (getLocalSourceState(receiveState) != DECRYPTION_FAILED) {
            return;
        }
        mBroadcastIdToPreferenceMap.computeIfPresent(
                receiveState.getBroadcastId(),
                (k, existingPreference) -> {
                    moveToState(existingPreference, AudioStreamState.ADD_SOURCE_BAD_CODE);
                    return existingPreference;
                });
    }

    // Find preference by broadcastId and decide next state.
    // Expect one preference existed, move to ADD_SOURCE_FAILED
    @Override
    public void handleSourceFailedToConnect(int broadcastId) {
        if (DEBUG) {
            Log.d(TAG, "handleSourceFailedToConnect()");
        }
        mBroadcastIdToPreferenceMap.computeIfPresent(
                broadcastId,
                (k, existingPreference) -> {
                    moveToState(existingPreference, AudioStreamState.ADD_SOURCE_FAILED);
                    return existingPreference;
                });
    }

    // Find preference by receiveState and decide next state.
    // Expect one preference existed, move to SOURCE_PRESENT
    @Override
    public void handleSourcePaused(
            BluetoothDevice device, BluetoothLeBroadcastReceiveState receiveState) {
        if (DEBUG) {
            Log.d(TAG, "handleSourcePaused()");
        }
        if (!mHysteresisModeFixAvailable || getLocalSourceState(receiveState) != PAUSED) {
            return;
        }

        var broadcastIdPaused = receiveState.getBroadcastId();
        Optional<BluetoothLeBroadcastMetadata> metadata =
                getMetadataMatchingByBroadcastId(
                        device, receiveState.getSourceId(), broadcastIdPaused);
        handleQrCodeWithUnsetBroadcastIdIfNeeded(metadata, receiveState);
        mBroadcastIdToPreferenceMap.compute(
                broadcastIdPaused,
                (k, existingPreference) -> {
                    if (existingPreference == null) {
                        // No existing preference for this source even if it's already existed but
                        // currently paused, add one and set initial state to SOURCE_PRESENT. This
                        // could happen because we retrieves the paused source during onStart() from
                        // AudioStreamsHelper#getAllPausedSources() even before the source is
                        // founded by scanning.
                        return metadata.isPresent()
                                ? addNewPreference(
                                        metadata.get(),
                                        AudioStreamState.SOURCE_PRESENT,
                                        SourceOriginForLogging.UNKNOWN)
                                : addNewPreference(receiveState, AudioStreamState.SOURCE_PRESENT);
                    }
                    // Some LE devices might keep retrying with bad code, in this case we don't
                    // switch to this intermediate state.
                    if (existingPreference.getAudioStreamState()
                            == AudioStreamState.ADD_SOURCE_BAD_CODE) {
                        return existingPreference;
                    }
                    if (existingPreference.getAudioStreamState() == AudioStreamState.WAIT_FOR_SYNC
                            && existingPreference.getAudioStreamBroadcastId() == UNSET_BROADCAST_ID
                            && mSourceFromQrCode != null) {
                        existingPreference.setAudioStreamMetadata(mSourceFromQrCode);
                    }
                    moveToState(existingPreference, AudioStreamState.SOURCE_PRESENT);
                    return existingPreference;
                });
    }

    // Find preference by metadata and decide next state.
    // Expect one preference existed, move to ADD_SOURCE_WAIT_FOR_RESPONSE
    void handleSourceAddRequest(
            AudioStreamPreference preference, BluetoothLeBroadcastMetadata metadata) {
        if (DEBUG) {
            Log.d(TAG, "handleSourceAddRequest()");
        }
        mBroadcastIdToPreferenceMap.computeIfPresent(
                metadata.getBroadcastId(),
                (k, existingPreference) -> {
                    if (!existingPreference.equals(preference)) {
                        Log.w(TAG, "handleSourceAddedRequest(): existing preference not match");
                    }
                    existingPreference.setAudioStreamMetadata(metadata);
                    moveToState(existingPreference, AudioStreamState.ADD_SOURCE_WAIT_FOR_RESPONSE);
                    return existingPreference;
                });
    }

    void removePreference(AudioStreamPreference preference) {
        if (DEBUG) {
            Log.d(TAG, "removePreference()");
        }
        mBroadcastIdToPreferenceMap.remove(preference.getAudioStreamBroadcastId());
        ThreadUtils.postOnMainThread(
                () -> {
                    if (mCategoryPreference != null) {
                        mCategoryPreference.removePreference(preference);
                    }
                });
    }

    void showToast(String msg) {
        AudioSharingUtils.toastMessage(mContext, msg);
    }

    private void init() {
        boolean hasConnected =
                AudioStreamsHelper.getCachedBluetoothDeviceInSharingOrLeConnected(mBluetoothManager)
                        .isPresent();
        Set<ComponentName> screenReaderServices = getEnabledScreenReaderServices(mContext);
        if (hasConnected && screenReaderServices.isEmpty()) {
            startScanningIfNeeded();
            AudioSharingUtils.postOnMainThread(
                    mContext,
                    () -> {
                        if (mCategoryPreference != null) {
                            mCategoryPreference.setVisible(true);
                        }
                        AudioStreamsDialogFragment.dismissAll(mFragment);
                    });
        } else {
            stopScanningAndCleanUp();
            if (!hasConnected) {
                AudioSharingUtils.postOnMainThread(
                        mContext,
                        () ->
                                AudioStreamsDialogFragment.show(
                                        mFragment,
                                        getNoLeDeviceDialog(),
                                        SettingsEnums.DIALOG_AUDIO_STREAM_MAIN_NO_LE_DEVICE));
            } else if (!screenReaderServices.isEmpty()) {
                AudioSharingUtils.postOnMainThread(
                        mContext,
                        () ->
                                AudioStreamsDialogFragment.show(
                                        mFragment,
                                        getTurnOffTalkbackDialog(screenReaderServices),
                                        SettingsEnums.DIALOG_AUDIO_STREAM_MAIN_TURN_OFF_TALKBACK));
            }
        }
    }

    private void startScanningIfNeeded() {
        if (mLeBroadcastAssistant == null) {
            Log.w(TAG, "startScanningIfNeeded(): LeBroadcastAssistant is null!");
            return;
        }
        mLeBroadcastAssistant.registerServiceCallBack(mExecutor, mBroadcastAssistantCallback);
        mBroadcastAssistantCallback.setSourceStateListener(this);
        mBroadcastAssistantCallback.setScanStateListener(mScanHelper);
        Map<BluetoothDevice, List<BluetoothLeBroadcastReceiveState>> sources =
                mAudioStreamsHelper.getAllSourcesByDevice();
        // Handle currently paused sources on connected devices
        if (mHysteresisModeFixAvailable) {
            getPausedSourcesByDevice(sources)
                    .forEach(
                            (device, stateList) ->
                                    stateList.forEach(state -> handleSourcePaused(device, state)));
        }
        // Handle currently streaming sources on connected devices, if a source is streaming in one
        // device and paused in another, we handle the source as it's streaming
        getStreamSourcesByDevice(sources)
                .forEach(
                        (device, stateList) ->
                                stateList.forEach(state -> handleSourceStreaming(device, state)));
        // In handleSourceFromQrCodeIfExists(), we might start scanning with scan filter targeting
        // the specific source from the QR code. That's why we keep this function after the handling
        // of existed paused and streaming sources, so that we can skip the targeted scanning if the
        // source from the QR code is already present on the connected devices.
        handleSourceFromQrCodeIfExists();
        if (!mScanHelper.hasStartedScanning()) {
            mScanHelper.startScanning();
        }
        mMediaControlHelper.start();
    }

    private Map<BluetoothDevice, List<BluetoothLeBroadcastReceiveState>> getStreamSourcesByDevice(
            Map<BluetoothDevice, List<BluetoothLeBroadcastReceiveState>> sources) {
        return sources.entrySet().stream()
                .filter(
                        entry ->
                                entry.getValue().stream()
                                        .anyMatch(state -> getLocalSourceState(state) == STREAMING))
                .collect(toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private Map<BluetoothDevice, List<BluetoothLeBroadcastReceiveState>> getPausedSourcesByDevice(
            Map<BluetoothDevice, List<BluetoothLeBroadcastReceiveState>> sources) {
        return sources.entrySet().stream()
                .filter(
                        entry ->
                                entry.getValue().stream()
                                        .anyMatch(state -> getLocalSourceState(state) == PAUSED))
                .collect(toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private Optional<BluetoothLeBroadcastMetadata> getMetadataMatchingByBroadcastId(
            BluetoothDevice device, int sourceId, int broadcastId) {
        return Optional.ofNullable(
                        mLeBroadcastAssistant != null
                                ? mLeBroadcastAssistant.getSourceMetadata(device, sourceId)
                                : null)
                .filter(m -> m.getBroadcastId() == broadcastId);
    }

    private void handleQrCodeWithUnsetBroadcastIdIfNeeded(
            Optional<BluetoothLeBroadcastMetadata> metadata,
            BluetoothLeBroadcastReceiveState receiveState) {
        if (mSourceFromQrCode != null && mSourceFromQrCode.getBroadcastId() == UNSET_BROADCAST_ID) {
            if (DEBUG) {
                Log.d(TAG, "Processing mSourceFromQrCode with unset broadcastId");
            }
            boolean updated =
                    maybeUpdateId(
                            metadata.isPresent()
                                    ? AudioStreamsHelper.getBroadcastName(metadata.get())
                                    : AudioStreamsHelper.getBroadcastName(receiveState),
                            receiveState.getBroadcastId());
            if (updated && mBroadcastIdToPreferenceMap.containsKey(UNSET_BROADCAST_ID)) {
                var preference = mBroadcastIdToPreferenceMap.remove(UNSET_BROADCAST_ID);
                mBroadcastIdToPreferenceMap.put(receiveState.getBroadcastId(), preference);
            }
        }
    }

    private void stopScanningAndCleanUp() {
        mScanHelper.stopScanning();
        if (mLeBroadcastAssistant == null) {
            Log.w(TAG, "stopScanningAndCleanUp(): LeBroadcastAssistant is null!");
            return;
        }
        mLeBroadcastAssistant.unregisterServiceCallBack(mBroadcastAssistantCallback);
        mMediaControlHelper.stop();
        mSourceFromQrCode = null;
        mBroadcastIdToPreferenceMap.clear();
        AudioSharingUtils.postOnMainThread(
                mContext,
                () -> {
                    if (mCategoryPreference != null) {
                        mCategoryPreference.removeAudioStreamPreferences();
                        mCategoryPreference.setVisible(false);
                    }
                });
    }

    private AudioStreamPreference addNewPreference(
            BluetoothLeBroadcastReceiveState receiveState, AudioStreamState state) {
        var preference = AudioStreamPreference.fromReceiveState(mContext, receiveState);
        moveToState(preference, state);
        return preference;
    }

    private AudioStreamPreference addNewPreference(
            BluetoothLeBroadcastMetadata metadata,
            AudioStreamState state,
            SourceOriginForLogging sourceOriginForLogging) {
        var preference =
                AudioStreamPreference.fromMetadata(mContext, metadata, sourceOriginForLogging);
        moveToState(preference, state);
        return preference;
    }

    @VisibleForTesting
    void moveToState(AudioStreamPreference preference, AudioStreamState state) {
        AudioStreamStateHandler stateHandler = getStateHandler(state);
        AudioStreamStateHandler prevStateHandler =
                getStateHandler(preference.getAudioStreamState());

        if (stateHandler != null) {
            stateHandler.handleStateChange(
                    prevStateHandler, preference, this, mAudioStreamsHelper, mScanHelper);
        }

        // Update UI with the updated preference
        AudioSharingUtils.postOnMainThread(
                mContext,
                () -> {
                    if (mCategoryPreference != null) {
                        mCategoryPreference.addAudioStreamPreference(preference, mComparator);
                    }
                });
    }

    @VisibleForTesting
    static @Nullable AudioStreamStateHandler getStateHandler(AudioStreamState stateEnum) {
        var state =
                switch (stateEnum) {
                    case SYNCED -> SyncedState.getInstance();
                    case WAIT_FOR_SYNC -> WaitForSyncState.getInstance();
                    case ADD_SOURCE_WAIT_FOR_RESPONSE ->
                            AddSourceWaitForResponseState.getInstance();
                    case ADD_SOURCE_WAIT_FOR_RESPONSE_FROM_QR ->
                            AddSourceWaitForResponseFromQrState.getInstance();
                    case ADD_SOURCE_BAD_CODE -> AddSourceBadCodeState.getInstance();
                    case ADD_SOURCE_FAILED -> AddSourceFailedState.getInstance();
                    case SOURCE_PRESENT -> SourcePresentState.getInstance();
                    case SOURCE_ADDED -> SourceAddedState.getInstance();
                    case SOURCE_LOST -> SourceLostState.getInstance();
                    default -> null;
                };
        if (state == null) {
            Log.d(TAG, "Unsupported state:" + stateEnum);
        }
        return state;
    }

    private AudioStreamsDialogFragment.DialogBuilder getNoLeDeviceDialog() {
        return new AudioStreamsDialogFragment.DialogBuilder(mContext)
                .setTitle(mContext.getString(R.string.audio_streams_dialog_no_le_device_title))
                .setSubTitle2(
                        mContext.getString(R.string.audio_streams_dialog_no_le_device_subtitle))
                .setLeftButtonText(mContext.getString(R.string.audio_streams_dialog_close))
                .setLeftButtonOnClickListener(AlertDialog::dismiss)
                .setRightButtonText(
                        mContext.getString(R.string.audio_streams_dialog_no_le_device_button))
                .setRightButtonOnClickListener(
                        dialog -> {
                            new SubSettingLauncher(mContext)
                                    .setDestination(
                                            ConnectedDeviceDashboardFragment.class.getName())
                                    .setSourceMetricsCategory(
                                            SettingsEnums.DIALOG_AUDIO_STREAM_MAIN_NO_LE_DEVICE)
                                    .launch();
                            dialog.dismiss();
                        });
    }

    private AudioStreamsDialogFragment.DialogBuilder getTurnOffTalkbackDialog(
            Set<ComponentName> enabledScreenReader) {
        return new AudioStreamsDialogFragment.DialogBuilder(mContext)
                .setTitle(mContext.getString(R.string.audio_streams_dialog_turn_off_talkback_title))
                .setSubTitle2(
                        mContext.getString(
                                R.string.audio_streams_dialog_turn_off_talkback_subtitle))
                .setLeftButtonText(mContext.getString(R.string.cancel))
                .setLeftButtonOnClickListener(
                        dialog -> {
                            dialog.dismiss();
                            if (mFragment != null && mFragment.getActivity() != null) {
                                // Navigate back
                                mFragment.getActivity().finish();
                            }
                        })
                .setRightButtonText(
                        mContext.getString(R.string.audio_streams_dialog_turn_off_talkback_button))
                .setRightButtonOnClickListener(
                        dialog -> {
                            ThreadUtils.postOnBackgroundThread(
                                    () -> {
                                        if (!enabledScreenReader.isEmpty()) {
                                            setAccessibilityServiceOff(
                                                    mContext, enabledScreenReader);
                                        }
                                    });
                            dialog.dismiss();
                        });
    }
}
