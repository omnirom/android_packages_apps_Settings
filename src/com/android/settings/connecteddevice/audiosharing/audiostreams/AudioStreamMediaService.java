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

import static com.android.settingslib.bluetooth.LocalBluetoothLeBroadcast.EXTRA_PRIVATE_BROADCAST_RECEIVE_DATA;
import static com.android.settingslib.bluetooth.LocalBluetoothLeBroadcastAssistant.LocalBluetoothLeBroadcastSourceState.DECRYPTION_FAILED;
import static com.android.settingslib.bluetooth.LocalBluetoothLeBroadcastAssistant.LocalBluetoothLeBroadcastSourceState.PAUSED;
import static com.android.settingslib.bluetooth.LocalBluetoothLeBroadcastAssistant.LocalBluetoothLeBroadcastSourceState.STREAMING;

import static java.util.Collections.emptyList;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.app.settings.SettingsEnums;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothLeBroadcastReceiveState;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.BluetoothVolumeControl;
import android.content.Intent;
import android.media.MediaMetadata;
import android.media.session.MediaSession;
import android.media.session.PlaybackState;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Process;
import android.util.Log;
import android.view.KeyEvent;

import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import com.android.settings.R;
import com.android.settings.bluetooth.Utils;
import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.bluetooth.BluetoothCallback;
import com.android.settingslib.bluetooth.BluetoothUtils;
import com.android.settingslib.bluetooth.CachedBluetoothDevice;
import com.android.settingslib.bluetooth.CachedBluetoothDeviceManager;
import com.android.settingslib.bluetooth.LocalBluetoothLeBroadcastAssistant;
import com.android.settingslib.bluetooth.LocalBluetoothLeBroadcastAssistant.LocalBluetoothLeBroadcastSourceState;
import com.android.settingslib.bluetooth.LocalBluetoothManager;
import com.android.settingslib.bluetooth.PrivateBroadcastReceiveData;
import com.android.settingslib.bluetooth.VolumeControlProfile;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;
import com.android.settingslib.flags.Flags;
import com.android.settingslib.utils.ThreadUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AudioStreamMediaService extends Service {
    static final String BROADCAST_ID = "audio_stream_media_service_broadcast_id";
    static final String BROADCAST_TITLE = "audio_stream_media_service_broadcast_title";
    static final String DEVICES = "audio_stream_media_service_devices";
    private static final String TAG = "AudioStreamMediaService";
    private static final int NOTIFICATION_ID = R.string.audio_streams_title;
    private static final int BROADCAST_LISTENING_NOW_TEXT = R.string.audio_streams_listening_now;
    private static final int BROADCAST_STREAM_PAUSED_TEXT = R.string.audio_streams_present_now;
    @VisibleForTesting static final String LEAVE_BROADCAST_ACTION = "leave_broadcast_action";
    private static final String LEAVE_BROADCAST_TEXT = "Leave Broadcast";
    private static final String CHANNEL_ID = "bluetooth_notification_channel";
    private static final String DEFAULT_DEVICE_NAME = "";
    private static final int STATIC_PLAYBACK_DURATION = 100;
    private static final int STATIC_PLAYBACK_POSITION = 30;
    private static final int ZERO_PLAYBACK_SPEED = 0;
    private final PlaybackState.Builder mPlayStatePlayingBuilder =
            new PlaybackState.Builder()
                    .setActions(PlaybackState.ACTION_PLAY_PAUSE | PlaybackState.ACTION_SEEK_TO)
                    .setState(
                            PlaybackState.STATE_PLAYING,
                            STATIC_PLAYBACK_POSITION,
                            ZERO_PLAYBACK_SPEED)
                    .addCustomAction(
                            LEAVE_BROADCAST_ACTION,
                            LEAVE_BROADCAST_TEXT,
                            com.android.settings.R.drawable.ic_clear);
    private final PlaybackState.Builder mPlayStatePausingBuilder =
            new PlaybackState.Builder()
                    .setActions(PlaybackState.ACTION_PLAY_PAUSE | PlaybackState.ACTION_SEEK_TO)
                    .setState(
                            PlaybackState.STATE_PAUSED,
                            STATIC_PLAYBACK_POSITION,
                            ZERO_PLAYBACK_SPEED)
                    .addCustomAction(
                            LEAVE_BROADCAST_ACTION,
                            LEAVE_BROADCAST_TEXT,
                            com.android.settings.R.drawable.ic_clear);
    private final PlaybackState.Builder mPlayStateHysteresisBuilder =
            new PlaybackState.Builder()
                    .setState(
                            PlaybackState.STATE_PAUSED,
                            STATIC_PLAYBACK_POSITION,
                            ZERO_PLAYBACK_SPEED)
                    .addCustomAction(
                            LEAVE_BROADCAST_ACTION,
                            LEAVE_BROADCAST_TEXT,
                            com.android.settings.R.drawable.ic_clear);

    private final MetricsFeatureProvider mMetricsFeatureProvider =
            FeatureFactory.getFeatureFactory().getMetricsFeatureProvider();
    private final HandlerThread mHandlerThread = new HandlerThread(TAG,
            Process.THREAD_PRIORITY_BACKGROUND);
    private boolean mIsMuted = false;
    // Set 25 as default as the volume range from `VolumeControlProfile` is from 0 to 255.
    // If the initial volume from `onDeviceVolumeChanged` is larger than zero (not muted), we will
    // override this value. Otherwise, we raise the volume to 25 when the play button is clicked.
    private int mLatestPositiveVolume = 25;
    private boolean mHysteresisModeFixAvailable;
    private int mBroadcastId;
    @VisibleForTesting
    @Nullable
    Map<BluetoothDevice, LocalBluetoothLeBroadcastSourceState> mStateByDevice;
    @Nullable private LocalBluetoothManager mLocalBtManager;
    @Nullable private AudioStreamsHelper mAudioStreamsHelper;
    @Nullable private LocalBluetoothLeBroadcastAssistant mLeBroadcastAssistant;
    @Nullable private VolumeControlProfile mVolumeControl;
    @Nullable private NotificationManager mNotificationManager;
    @Nullable private MediaSession mLocalSession;
    @VisibleForTesting @Nullable AudioStreamsBroadcastAssistantCallback mBroadcastAssistantCallback;
    @VisibleForTesting @Nullable BluetoothCallback mBluetoothCallback;
    @VisibleForTesting @Nullable BluetoothVolumeControl.Callback mVolumeControlCallback;
    @VisibleForTesting @Nullable MediaSession.Callback mMediaSessionCallback;

    @Override
    public void onCreate() {
        if (!BluetoothUtils.isAudioSharingUIAvailable(this)) {
            return;
        }
        Log.d(TAG, "onCreate()");
        super.onCreate();
        mLocalBtManager = Utils.getLocalBtManager(this);
        if (mLocalBtManager == null) {
            Log.w(TAG, "onCreate() : mLocalBtManager is null!");
            return;
        }

        mAudioStreamsHelper = new AudioStreamsHelper(mLocalBtManager);
        mLeBroadcastAssistant = mAudioStreamsHelper.getLeBroadcastAssistant();
        if (mLeBroadcastAssistant == null) {
            Log.w(TAG, "onCreate() : mLeBroadcastAssistant is null!");
            return;
        }

        mNotificationManager = getSystemService(NotificationManager.class);
        if (mNotificationManager == null) {
            Log.w(TAG, "onCreate() : notificationManager is null!");
            return;
        }

        mHandlerThread.start();
        getHandler().post(
                () -> {
                    if (mLocalBtManager == null
                            || mLeBroadcastAssistant == null
                            || mNotificationManager == null) {
                        return;
                    }
                    if (mNotificationManager.getNotificationChannel(CHANNEL_ID) == null) {
                        NotificationChannel notificationChannel =
                                new NotificationChannel(
                                        CHANNEL_ID,
                                        getString(com.android.settings.R.string.bluetooth),
                                        NotificationManager.IMPORTANCE_HIGH);
                        mNotificationManager.createNotificationChannel(notificationChannel);
                    }

                    mBluetoothCallback = new BtCallback();
                    mLocalBtManager.getEventManager().registerCallback(mBluetoothCallback);

                    mVolumeControl = mLocalBtManager.getProfileManager().getVolumeControlProfile();
                    if (mVolumeControl != null) {
                        mVolumeControlCallback = new VolumeControlCallback();
                        mVolumeControl.registerCallback(getHandler()::post, mVolumeControlCallback);
                    }

                    mBroadcastAssistantCallback = new AssistantCallback();
                    mLeBroadcastAssistant.registerServiceCallBack(
                            getHandler()::post, mBroadcastAssistantCallback);

                    mHysteresisModeFixAvailable =
                            BluetoothUtils.isAudioSharingHysteresisModeFixAvailable(this);
                });
    }

    @VisibleForTesting
    Handler getHandler() {
        return mHandlerThread.getThreadHandler();
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy()");
        getHandler().post(
                () -> {
                    if (mStateByDevice != null) {
                        mStateByDevice.clear();
                        mStateByDevice = null;
                    }
                    if (mLocalSession != null) {
                        mLocalSession.release();
                        mLocalSession = null;
                    }
                    if (mLocalBtManager != null) {
                        mLocalBtManager.getEventManager().unregisterCallback(
                                mBluetoothCallback);
                    }
                    if (mLeBroadcastAssistant != null && mBroadcastAssistantCallback != null) {
                        mLeBroadcastAssistant.unregisterServiceCallBack(
                                mBroadcastAssistantCallback);
                    }
                    if (mVolumeControl != null && mVolumeControlCallback != null) {
                        mVolumeControl.unregisterCallback(mVolumeControlCallback);
                    }
                });
        mHandlerThread.quitSafely();
    }

    @Override
    public int onStartCommand(@Nullable Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand()");
        if (intent == null) {
            Log.w(TAG, "Intent is null. Service will not start.");
            stopSelf();
            return START_NOT_STICKY;
        }
        // TODO(b/398700619): Remove hasExtra check when feasible.
        if (Flags.audioStreamMediaServiceByReceiveState() && intent.hasExtra(
                EXTRA_PRIVATE_BROADCAST_RECEIVE_DATA)) {
            PrivateBroadcastReceiveData data = intent.getParcelableExtra(
                    EXTRA_PRIVATE_BROADCAST_RECEIVE_DATA, PrivateBroadcastReceiveData.class);
            if (data == null || !PrivateBroadcastReceiveData.Companion.isValid(data)) {
                Log.w(TAG, "Data is null or invalid. Service will not start.");
                stopSelf();
                return START_NOT_STICKY;
            }
            getHandler().post(() -> handleIntentData(data));
            return START_NOT_STICKY;
        }
        getHandler().post(() -> {
            mBroadcastId = intent.getIntExtra(BROADCAST_ID, -1);
            if (mBroadcastId == -1) {
                Log.w(TAG, "Invalid broadcast ID. Service will not start.");
                stopSelf();
                return;
            }
            var devices = intent.getParcelableArrayListExtra(DEVICES, BluetoothDevice.class);
            if (devices == null || devices.isEmpty()) {
                Log.w(TAG, "No device. Service will not start.");
                stopSelf();
            } else {
                mStateByDevice = new HashMap<>();
                devices.forEach(d -> mStateByDevice.put(d, STREAMING));
                MediaSession.Token token =
                        getOrCreateLocalMediaSession(intent.getStringExtra(BROADCAST_TITLE));
                startForeground(NOTIFICATION_ID, buildNotification(token));
            }
        });
        return START_NOT_STICKY;
    }

    private void handleIntentData(PrivateBroadcastReceiveData data) {
        int broadcastId = data.getBroadcastId();
        BluetoothDevice device = data.getSink();
        int sourceId = data.getSourceId();
        var state = data.getState();
        String programInfo = data.getProgramInfo();

        // Service not running yet.
        if (mBroadcastId == 0) {
            Log.d(TAG, "handleIntentData(): sending " + data + " to handleInitialSetup()");
            handleInitialSetup(broadcastId, device, state, sourceId, programInfo);
            return;
        }

        // Service running with a different broadcast id, most likely staled. We have a new
        // broadcast Id to handle.
        if (mBroadcastId != broadcastId) {
            Log.d(TAG, "handleIntentData(): sending " + data + " to handleNewBroadcastId()");
            handleNewBroadcastId(broadcastId, device, state, sourceId, programInfo);
            return;
        }

        // Service running with the same broadcast Id, we have new device joining or a state update.
        if (mStateByDevice != null && (!mStateByDevice.containsKey(device) || mStateByDevice.get(
                device) != state)) {
            Log.d(TAG, "handleIntentData(): sending " + data + " to handleNewDeviceOrState()");
            handleNewDeviceOrState(device, state, sourceId, programInfo);
        }

        Log.d(TAG, "handleIntentData(): nothing to update.");
    }

    private void handleInitialSetup(int broadcastId, BluetoothDevice device,
            LocalBluetoothLeBroadcastSourceState state, int sourceId, String programInfo) {
        if (state == DECRYPTION_FAILED) {
            Log.d(TAG, "handleInitialSetup() : decryption failed. Service will not start.");
            stopSelf();
            return;
        }
        mBroadcastId = broadcastId;
        mStateByDevice = new HashMap<>();
        mStateByDevice.put(device, state);
        MediaSession.Token token = getOrCreateLocalMediaSession(
                getBroadcastName(device, sourceId, programInfo));
        startForeground(NOTIFICATION_ID, buildNotification(token));
    }

    private void handleNewBroadcastId(int broadcastId, BluetoothDevice device,
            LocalBluetoothLeBroadcastSourceState state, int sourceId, String programInfo) {
        if (state == DECRYPTION_FAILED) {
            Log.d(TAG, "handleNewBroadcastId() : decryption failed. Ignore.");
            return;
        }
        mBroadcastId = broadcastId;
        mStateByDevice = new HashMap<>();
        mStateByDevice.put(device, state);
        updateMediaSessionAndNotify(device, sourceId, programInfo);
    }

    private void handleNewDeviceOrState(BluetoothDevice device,
            LocalBluetoothLeBroadcastSourceState state, int sourceId, String programInfo) {
        if (mStateByDevice != null) {
            mStateByDevice.put(device, state);
        }
        if (getDeviceInValidState().isEmpty()) {
            Log.d(TAG, "handleNewDeviceOrState() : no device is in valid state. Stop service.");
            stopSelf();
            return;
        }
        updateMediaSessionAndNotify(device, sourceId, programInfo);
    }

    private MediaSession.Token getOrCreateLocalMediaSession(String title) {
        if (mLocalSession != null) {
            return mLocalSession.getSessionToken();
        }
        mLocalSession = new MediaSession(this, TAG);
        mLocalSession.setMetadata(
                new MediaMetadata.Builder()
                        .putString(MediaMetadata.METADATA_KEY_TITLE, title)
                        .putLong(MediaMetadata.METADATA_KEY_DURATION, STATIC_PLAYBACK_DURATION)
                        .build());
        mLocalSession.setActive(true);
        mLocalSession.setPlaybackState(getPlaybackState());
        mMediaSessionCallback = new MediaSessionCallback();
        mLocalSession.setCallback(mMediaSessionCallback, getHandler());
        return mLocalSession.getSessionToken();
    }

    private PlaybackState getPlaybackState() {
        if (isAllDeviceHysteresis()) {
            return mPlayStateHysteresisBuilder.build();
        }
        return mIsMuted ? mPlayStatePausingBuilder.build() : mPlayStatePlayingBuilder.build();
    }

    private boolean isAllDeviceHysteresis() {
        return mHysteresisModeFixAvailable && mStateByDevice != null
                && mStateByDevice.values().stream().allMatch(v -> v == PAUSED);
    }

    private String getDeviceName() {
        List<BluetoothDevice> validDevices = getDeviceInValidState();
        if (validDevices.isEmpty() || mLocalBtManager == null) {
            return DEFAULT_DEVICE_NAME;
        }

        CachedBluetoothDeviceManager manager = mLocalBtManager.getCachedDeviceManager();
        if (manager == null) {
            return DEFAULT_DEVICE_NAME;
        }

        CachedBluetoothDevice device = manager.findDevice(validDevices.getFirst());
        return device != null ? device.getName() : DEFAULT_DEVICE_NAME;
    }

    private Notification buildNotification(MediaSession.Token token) {
        String deviceName = getDeviceName();
        Notification.MediaStyle mediaStyle = new Notification.MediaStyle().setMediaSession(token);
        if (deviceName != null && !deviceName.isEmpty()) {
            mediaStyle.setRemotePlaybackInfo(
                    deviceName, com.android.settingslib.R.drawable.ic_bt_le_audio, null);
        }
        Notification.Builder notificationBuilder =
                new Notification.Builder(this, CHANNEL_ID)
                        .setSmallIcon(com.android.settingslib.R.drawable.ic_bt_le_audio_sharing)
                        .setStyle(mediaStyle)
                        .setContentText(getString(
                                isAllDeviceHysteresis() ? BROADCAST_STREAM_PAUSED_TEXT :
                                        BROADCAST_LISTENING_NOW_TEXT))
                        .setSilent(true);
        return notificationBuilder.build();
    }

    private void updateMediaSessionAndNotify(BluetoothDevice device, int sourceId,
            String programInfo) {
        if (mNotificationManager == null || mLocalSession == null) {
            Log.w(TAG, "mNotificationManager or mLocalSession is null, ignore update.");
            return;
        }
        mLocalSession.setMetadata(new MediaMetadata.Builder().putString(
                MediaMetadata.METADATA_KEY_TITLE,
                getBroadcastName(device, sourceId, programInfo)).putLong(
                MediaMetadata.METADATA_KEY_DURATION, STATIC_PLAYBACK_DURATION).build());
        mLocalSession.setPlaybackState(getPlaybackState());
        mNotificationManager.notify(NOTIFICATION_ID,
                buildNotification(mLocalSession.getSessionToken()));
    }

    private String getBroadcastName(BluetoothDevice sink, int sourceId, String programInfo) {
        if (mLeBroadcastAssistant == null || sink == null) {
            return programInfo;
        }
        var metadata = mLeBroadcastAssistant.getSourceMetadata(sink, sourceId);
        if (metadata == null || metadata.getBroadcastId() != mBroadcastId
                || metadata.getBroadcastName() == null || metadata.getBroadcastName().isEmpty()) {
            Log.d(TAG, "getBroadcastName(): source metadata not found, using programInfo: "
                    + programInfo);
            return programInfo;
        }
        return metadata.getBroadcastName();
    }

    private List<BluetoothDevice> getDeviceInValidState() {
        if (mStateByDevice == null || mStateByDevice.isEmpty()) {
            Log.w(TAG, "getDeviceInValidState() : mStateByDevice is null or empty!");
            return emptyList();
        }
        if (Flags.audioStreamMediaServiceByReceiveState()) {
            return mStateByDevice.entrySet().stream().filter(
                    entry -> entry.getValue() != DECRYPTION_FAILED).map(Map.Entry::getKey).toList();
        }
        return mStateByDevice.keySet().stream().toList();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private class AssistantCallback extends AudioStreamsBroadcastAssistantCallback {
        @Override
        public void onSourceLost(int broadcastId) {
            super.onSourceLost(broadcastId);
            handleRemoveSource();
        }

        @Override
        public void onSourceRemoved(BluetoothDevice sink, int sourceId, int reason) {
            super.onSourceRemoved(sink, sourceId, reason);
            handleRemoveSource();
        }

        @Override
        public void onReceiveStateChanged(
                BluetoothDevice sink, int sourceId, BluetoothLeBroadcastReceiveState state) {
            if (Flags.audioStreamMediaServiceByReceiveState()) {
                return;
            }
            super.onReceiveStateChanged(sink, sourceId, state);
            if (!mHysteresisModeFixAvailable || mStateByDevice == null
                    || !mStateByDevice.containsKey(sink)) {
                return;
            }
            var sourceState = LocalBluetoothLeBroadcastAssistant.getLocalSourceState(state);
            boolean streaming = sourceState == STREAMING;
            boolean paused = sourceState == PAUSED;
            // Exit early if the state is neither streaming nor paused
            if (!streaming && !paused) {
                return;
            }
            boolean shouldUpdate = mStateByDevice.get(sink) != sourceState;
            if (shouldUpdate) {
                mStateByDevice.put(sink, sourceState);
                if (mLocalSession != null) {
                    mLocalSession.setPlaybackState(getPlaybackState());
                    if (mNotificationManager != null) {
                        mNotificationManager.notify(
                                NOTIFICATION_ID,
                                buildNotification(mLocalSession.getSessionToken())
                        );
                    }
                    Log.d(TAG, "updating source state to : " + sourceState);
                }
            }
        }

        private void handleRemoveSource() {
            if (mAudioStreamsHelper != null
                    && !mAudioStreamsHelper.getConnectedBroadcastIdAndState(
                            mHysteresisModeFixAvailable).containsKey(mBroadcastId)) {
                stopSelf();
            }
        }
    }

    private class VolumeControlCallback implements BluetoothVolumeControl.Callback {
        @Override
        public void onDeviceVolumeChanged(
                @NonNull BluetoothDevice device, @IntRange(from = -255, to = 255) int volume) {
            if (!getDeviceInValidState().contains(device)) {
                Log.w(TAG, "onDeviceVolumeChanged() : device not in valid state list");
                return;
            }
            Log.d(
                    TAG,
                    "onDeviceVolumeChanged() bluetoothDevice : " + device + " volume: " + volume);
            if (volume == 0) {
                mIsMuted = true;
            } else {
                mIsMuted = false;
                mLatestPositiveVolume = volume;
            }
            if (mLocalSession != null) {
                mLocalSession.setPlaybackState(getPlaybackState());
            }
        }
    }

    private class BtCallback implements BluetoothCallback {
        @Override
        public void onBluetoothStateChanged(int bluetoothState) {
            getHandler().post(() -> {
                if (BluetoothAdapter.STATE_OFF == bluetoothState) {
                    Log.d(TAG, "onBluetoothStateChanged() : stopSelf");
                    stopSelf();
                }
            });
        }

        @Override
        public void onProfileConnectionStateChanged(
                @NonNull CachedBluetoothDevice cachedDevice,
                @ConnectionState int state,
                int bluetoothProfile) {
            getHandler().post(() -> {
                if (state == BluetoothAdapter.STATE_DISCONNECTED
                        && bluetoothProfile == BluetoothProfile.LE_AUDIO_BROADCAST_ASSISTANT
                        && mStateByDevice != null) {
                    mStateByDevice.remove(cachedDevice.getDevice());
                }
                if (getDeviceInValidState().isEmpty()) {
                    Log.d(TAG, "onProfileConnectionStateChanged() : stopSelf");
                    stopSelf();
                }
            });
        }
    }

    private class MediaSessionCallback extends MediaSession.Callback {
        @Override
        public boolean onMediaButtonEvent(@NonNull Intent mediaButtonIntent) {
            KeyEvent keyEvent = mediaButtonIntent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
            if (keyEvent != null) {
                Log.d(TAG, "onMediaButtonEvent(): triggered by MediaSessionCallback");
                switch (keyEvent.getKeyCode()) {
                    case KeyEvent.KEYCODE_MEDIA_PLAY:
                        handleOnPlay();
                        break;
                    case KeyEvent.KEYCODE_MEDIA_PAUSE:
                        handleOnPause();
                        break;
                    default: // fall out
                }
            }
            return super.onMediaButtonEvent(mediaButtonIntent);
        }

        @Override
        public void onSeekTo(long pos) {
            Log.d(TAG, "onSeekTo: " + pos);
            if (mLocalSession != null) {
                mLocalSession.setPlaybackState(getPlaybackState());
            }
        }

        @Override
        public void onPause() {
            handleOnPause();
        }

        @Override
        public void onPlay() {
            handleOnPlay();
        }

        @Override
        public void onCustomAction(@NonNull String action, Bundle extras) {
            Log.d(TAG, "onCustomAction: " + action);
            if (action.equals(LEAVE_BROADCAST_ACTION) && mAudioStreamsHelper != null) {
                mAudioStreamsHelper.removeSource(mBroadcastId);
                mMetricsFeatureProvider.action(
                        getApplicationContext(),
                        SettingsEnums.ACTION_AUDIO_STREAM_NOTIFICATION_LEAVE_BUTTON_CLICK);
            }
        }
    }

    private void handleOnPlay() {
        getDeviceInValidState().forEach(device -> {
            Log.d(TAG, "onPlay() setting volume for device : " + device + " volume: "
                    + mLatestPositiveVolume);
            setDeviceVolume(device, mLatestPositiveVolume);
        });
    }

    private void handleOnPause() {
        getDeviceInValidState().forEach(device -> {
            Log.d(TAG, "onPause() setting volume for device : " + device + " volume: " + 0);
            setDeviceVolume(device, /* volume= */ 0);
        });
    }

    private void setDeviceVolume(BluetoothDevice device, int volume) {
        int event = SettingsEnums.ACTION_AUDIO_STREAM_NOTIFICATION_MUTE_BUTTON_CLICK;
        var unused =
                ThreadUtils.postOnBackgroundThread(
                        () -> {
                            if (mVolumeControl != null) {
                                mVolumeControl.setDeviceVolume(device, volume, false);
                                mMetricsFeatureProvider.action(
                                        getApplicationContext(), event, volume == 0 ? 1 : 0);
                            }
                        });
    }
}
