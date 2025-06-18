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

package com.android.settings.connecteddevice.audiosharing;

import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;

import static com.android.settingslib.bluetooth.LocalBluetoothLeBroadcast.EXTRA_BLUETOOTH_DEVICE;

import android.Manifest;
import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.settings.SettingsEnums;
import android.bluetooth.BluetoothCsipSetCoordinator;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.android.settings.R;
import com.android.settings.bluetooth.Utils;
import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.bluetooth.BluetoothUtils;
import com.android.settingslib.bluetooth.LocalBluetoothLeBroadcast;
import com.android.settingslib.bluetooth.LocalBluetoothManager;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;
import com.android.settingslib.flags.Flags;

import com.google.common.collect.ImmutableList;

import java.util.List;
import java.util.Map;

public class AudioSharingReceiver extends BroadcastReceiver {
    private static final String TAG = "AudioSharingReceiver";
    private static final String ACTION_LE_AUDIO_SHARING_SETTINGS =
            "com.android.settings.BLUETOOTH_AUDIO_SHARING_SETTINGS";
    private static final String ACTION_LE_AUDIO_SHARING_STOP =
            "com.android.settings.action.BLUETOOTH_LE_AUDIO_SHARING_STOP";
    private static final String ACTION_LE_AUDIO_SHARING_ADD_SOURCE =
            "com.android.settings.action.BLUETOOTH_LE_AUDIO_SHARING_ADD_SOURCE";
    private static final String ACTION_LE_AUDIO_SHARING_CANCEL_NOTIF =
            "com.android.settings.action.BLUETOOTH_LE_AUDIO_SHARING_CANCEL_NOTIF";
    private static final String EXTRA_NOTIF_ID = "NOTIF_ID";
    private static final String CHANNEL_ID = "bluetooth_notification_channel";
    private static final int AUDIO_SHARING_NOTIFICATION_ID =
            com.android.settingslib.R.drawable.ic_bt_le_audio_sharing;
    private static final int ADD_SOURCE_NOTIFICATION_ID = R.string.share_audio_notification_title;
    private static final int NOTIF_AUTO_DISMISS_MILLIS = 300000; //5mins

    @Override
    public void onReceive(@NonNull Context context, @NonNull Intent intent) {
        String action = intent.getAction();
        if (action == null) {
            Log.w(TAG, "Received unexpected intent with null action.");
            return;
        }
        MetricsFeatureProvider metricsFeatureProvider =
                FeatureFactory.getFeatureFactory().getMetricsFeatureProvider();
        switch (action) {
            case LocalBluetoothLeBroadcast.ACTION_LE_AUDIO_SHARING_STATE_CHANGE:
                int state =
                        intent.getIntExtra(
                                LocalBluetoothLeBroadcast.EXTRA_LE_AUDIO_SHARING_STATE, -1);
                if (state == LocalBluetoothLeBroadcast.BROADCAST_STATE_ON) {
                    if (!BluetoothUtils.isAudioSharingUIAvailable(context)) {
                        Log.w(TAG, "Skip showSharingNotification, feature disabled.");
                        return;
                    }
                    showSharingNotification(context);
                    metricsFeatureProvider.action(
                            context, SettingsEnums.ACTION_SHOW_AUDIO_SHARING_NOTIFICATION);
                } else if (state == LocalBluetoothLeBroadcast.BROADCAST_STATE_OFF) {
                    // TODO: check BluetoothUtils#isAudioSharingEnabled() till BluetoothAdapter#
                    //       isLeAudioBroadcastSourceSupported() and BluetoothAdapter#
                    //       isLeAudioBroadcastAssistantSupported() always return FEATURE_SUPPORTED
                    //       or FEATURE_NOT_SUPPORTED when BT and BLE off
                    cancelSharingNotification(context, AUDIO_SHARING_NOTIFICATION_ID);
                    metricsFeatureProvider.action(
                            context, SettingsEnums.ACTION_CANCEL_AUDIO_SHARING_NOTIFICATION,
                            LocalBluetoothLeBroadcast.ACTION_LE_AUDIO_SHARING_STATE_CHANGE);
                    cancelSharingNotification(context, ADD_SOURCE_NOTIFICATION_ID);
                    // TODO: add metric
                } else {
                    Log.w(
                            TAG,
                            "Skip handling ACTION_LE_AUDIO_SHARING_STATE_CHANGE, invalid extras.");
                }
                break;
            case ACTION_LE_AUDIO_SHARING_STOP:
                if (BluetoothUtils.isAudioSharingUIAvailable(context)) {
                    LocalBluetoothManager manager = Utils.getLocalBtManager(context);
                    if (BluetoothUtils.isBroadcasting(manager)) {
                        AudioSharingUtils.stopBroadcasting(manager);
                        metricsFeatureProvider.action(
                                context, SettingsEnums.ACTION_STOP_AUDIO_SHARING_FROM_NOTIFICATION);
                        return;
                    }
                }
                Log.w(TAG, "cancelSharingNotification, feature disabled or not in broadcast.");
                // TODO: check BluetoothUtils#isAudioSharingEnabled() till BluetoothAdapter#
                //       isLeAudioBroadcastSourceSupported() and BluetoothAdapter#
                //       isLeAudioBroadcastAssistantSupported() always return FEATURE_SUPPORTED
                //       or FEATURE_NOT_SUPPORTED when BT and BLE off
                cancelSharingNotification(context, AUDIO_SHARING_NOTIFICATION_ID);
                metricsFeatureProvider.action(
                        context, SettingsEnums.ACTION_CANCEL_AUDIO_SHARING_NOTIFICATION,
                        ACTION_LE_AUDIO_SHARING_STOP);
                cancelSharingNotification(context, ADD_SOURCE_NOTIFICATION_ID);
                break;
            case LocalBluetoothLeBroadcast.ACTION_LE_AUDIO_SHARING_DEVICE_CONNECTED:
                if (!Flags.promoteAudioSharingForSecondAutoConnectedLeaDevice()
                        || !BluetoothUtils.isAudioSharingUIAvailable(context)) {
                    Log.d(TAG, "Skip ACTION_LE_AUDIO_SHARING_DEVICE_CONNECTED, flag/feature off");
                    return;
                }
                BluetoothDevice device = intent.getParcelableExtra(EXTRA_BLUETOOTH_DEVICE,
                        BluetoothDevice.class);
                if (device == null) {
                    Log.d(TAG, "Skip ACTION_LE_AUDIO_SHARING_DEVICE_CONNECTED, null device");
                    return;
                }
                if (isAppInForeground(context)) {
                    Log.d(TAG, "App in foreground, show share audio dialog");
                    Intent dialogIntent = new Intent();
                    dialogIntent.setClass(context, AudioSharingJoinHandlerActivity.class);
                    dialogIntent.addFlags(FLAG_ACTIVITY_NEW_TASK);
                    dialogIntent.putExtra(EXTRA_BLUETOOTH_DEVICE, device);
                    context.startActivity(dialogIntent);
                } else {
                    Log.d(TAG, "App not in foreground, show share audio notification");
                    LocalBluetoothManager manager = Utils.getLocalBtManager(context);
                    if (!validToAddSource(device, action, manager).isEmpty()) {
                        showAddSourceNotification(context, device);
                    }
                }
                break;
            case ACTION_LE_AUDIO_SHARING_ADD_SOURCE:
                if (!Flags.promoteAudioSharingForSecondAutoConnectedLeaDevice()
                        || !BluetoothUtils.isAudioSharingUIAvailable(context)) {
                    Log.d(TAG, "Skip ACTION_LE_AUDIO_SHARING_ADD_SOURCE, flag/feature off");
                    cancelSharingNotification(context, ADD_SOURCE_NOTIFICATION_ID);
                    return;
                }
                BluetoothDevice sink = intent.getParcelableExtra(EXTRA_BLUETOOTH_DEVICE,
                        BluetoothDevice.class);
                LocalBluetoothManager manager = Utils.getLocalBtManager(context);
                ImmutableList<BluetoothDevice> sinksToAdd = validToAddSource(sink, action, manager);
                AudioSharingUtils.addSourceToTargetSinks(sinksToAdd, manager);
                cancelSharingNotification(context, ADD_SOURCE_NOTIFICATION_ID);
                break;
            case ACTION_LE_AUDIO_SHARING_CANCEL_NOTIF:
                int notifId = intent.getIntExtra(EXTRA_NOTIF_ID, -1);
                if (notifId != -1) {
                    cancelSharingNotification(context, notifId);
                }
                break;
            default:
                Log.w(TAG, "Received unexpected intent " + action);
        }
    }

    private ImmutableList<BluetoothDevice> validToAddSource(@Nullable BluetoothDevice sink,
            @NonNull String action, @Nullable LocalBluetoothManager btManager) {
        if (sink == null) {
            Log.d(TAG, "Skip " + action + ", null device");
            return ImmutableList.of();
        }
        boolean isBroadcasting = BluetoothUtils.isBroadcasting(btManager);
        if (!isBroadcasting) {
            Log.d(TAG, "Skip " + action + ", not broadcasting");
            return ImmutableList.of();
        }
        Map<Integer, List<BluetoothDevice>> groupedDevices =
                AudioSharingUtils.fetchConnectedDevicesByGroupId(btManager);
        int groupId = groupedDevices.entrySet().stream().filter(
                entry -> entry.getValue().contains(sink)).findFirst().map(
                Map.Entry::getKey).orElse(BluetoothCsipSetCoordinator.GROUP_ID_INVALID);
        if (groupId == BluetoothCsipSetCoordinator.GROUP_ID_INVALID) {
            Log.d(TAG, "Skip " + action + ", no valid group id");
            return ImmutableList.of();
        }
        List<BluetoothDevice> sinksToAdd = groupedDevices.getOrDefault(groupId,
                ImmutableList.of()).stream().filter(
                    d -> !BluetoothUtils.hasConnectedBroadcastSourceForBtDevice(d,
                        btManager)).toList();
        if (sinksToAdd.isEmpty()) {
            Log.d(TAG, "Skip " + action + ", already has source");
            return ImmutableList.of();
        } else if (groupedDevices.entrySet().stream().filter(
                entry -> entry.getKey() != groupId && entry.getValue().stream().anyMatch(
                        d -> BluetoothUtils.hasConnectedBroadcastSourceForBtDevice(d,
                                btManager))).toList().size() >= 2) {
            Log.d(TAG, "Skip " + action + ", already 2 sinks");
            return ImmutableList.of();
        }
        return ImmutableList.copyOf(sinksToAdd);
    }

    private void showSharingNotification(@NonNull Context context) {
        NotificationManager nm = context.getSystemService(NotificationManager.class);
        if (nm == null) return;
        createNotificationChannelIfNeeded(nm, context);
        Intent stopIntent =
                new Intent(ACTION_LE_AUDIO_SHARING_STOP).setPackage(context.getPackageName());
        PendingIntent stopPendingIntent =
                PendingIntent.getBroadcast(
                        context,
                        R.string.audio_sharing_stop_button_label,
                        stopIntent,
                        PendingIntent.FLAG_IMMUTABLE);
        Intent settingsIntent =
                new Intent(ACTION_LE_AUDIO_SHARING_SETTINGS)
                        .setPackage(context.getPackageName())
                        .putExtra(
                                MetricsFeatureProvider.EXTRA_SOURCE_METRICS_CATEGORY,
                                SettingsEnums.NOTIFICATION_AUDIO_SHARING);
        PendingIntent settingsPendingIntent =
                PendingIntent.getActivity(
                        context,
                        R.string.audio_sharing_settings_button_label,
                        settingsIntent,
                        PendingIntent.FLAG_IMMUTABLE);
        NotificationCompat.Action stopAction =
                new NotificationCompat.Action.Builder(
                        0,
                        context.getString(R.string.audio_sharing_stop_button_label),
                        stopPendingIntent)
                        .build();
        NotificationCompat.Action settingsAction =
                new NotificationCompat.Action.Builder(
                        0,
                        context.getString(R.string.audio_sharing_settings_button_label),
                        settingsPendingIntent)
                        .build();
        final Bundle extras = new Bundle();
        extras.putString(
                Notification.EXTRA_SUBSTITUTE_APP_NAME,
                context.getString(R.string.audio_sharing_title));
        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(context, CHANNEL_ID)
                        .setSmallIcon(com.android.settingslib.R.drawable.ic_bt_le_audio_sharing)
                        .setLocalOnly(true)
                        .setContentTitle(
                                context.getString(R.string.audio_sharing_notification_title))
                        .setContentText(
                                context.getString(R.string.audio_sharing_notification_content))
                        .setOngoing(true)
                        .setSilent(true)
                        .setColor(
                                context.getColor(
                                        com.android.internal.R.color
                                                .system_notification_accent_color))
                        .setContentIntent(settingsPendingIntent)
                        .addAction(stopAction)
                        .addAction(settingsAction)
                        .addExtras(extras);
        nm.notify(AUDIO_SHARING_NOTIFICATION_ID, builder.build());
    }

    private void showAddSourceNotification(@NonNull Context context,
            @NonNull BluetoothDevice device) {
        NotificationManager nm = context.getSystemService(NotificationManager.class);
        if (nm == null) return;
        createNotificationChannelIfNeeded(nm, context);
        Intent addSourceIntent =
                new Intent(ACTION_LE_AUDIO_SHARING_ADD_SOURCE).setPackage(context.getPackageName())
                        .putExtra(EXTRA_BLUETOOTH_DEVICE, device);
        // Use PendingIntent.FLAG_UPDATE_CURRENT here because intent extra (device) could be updated
        PendingIntent addSourcePendingIntent =
                PendingIntent.getBroadcast(
                        context,
                        R.string.audio_sharing_share_button_label,
                        addSourceIntent,
                        PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_UPDATE_CURRENT
                                | PendingIntent.FLAG_IMMUTABLE);
        NotificationCompat.Action addSourceAction =
                new NotificationCompat.Action.Builder(
                        0,
                        context.getString(R.string.audio_sharing_share_button_label),
                        addSourcePendingIntent)
                        .build();
        Intent cancelIntent = new Intent(ACTION_LE_AUDIO_SHARING_CANCEL_NOTIF).setPackage(
                        context.getPackageName())
                .putExtra(EXTRA_NOTIF_ID, ADD_SOURCE_NOTIFICATION_ID);
        PendingIntent cancelPendingIntent =
                PendingIntent.getBroadcast(
                        context,
                        R.string.cancel,
                        cancelIntent,
                        PendingIntent.FLAG_IMMUTABLE);
        NotificationCompat.Action cancelAction =
                new NotificationCompat.Action.Builder(
                        0,
                        context.getString(R.string.cancel),
                        cancelPendingIntent)
                        .build();
        final Bundle extras = new Bundle();
        extras.putString(
                Notification.EXTRA_SUBSTITUTE_APP_NAME,
                context.getString(R.string.audio_sharing_title));
        String deviceName = device.getAlias();
        if (TextUtils.isEmpty(deviceName)) {
            deviceName = device.getAddress();
        }
        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(context, CHANNEL_ID)
                        .setSmallIcon(com.android.settingslib.R.drawable.ic_bt_le_audio_sharing)
                        .setLocalOnly(true)
                        .setContentTitle(context.getString(R.string.share_audio_notification_title,
                                deviceName))
                        .setContentText(
                                context.getString(R.string.audio_sharing_notification_content))
                        .setOngoing(true)
                        .setSilent(true)
                        .setColor(
                                context.getColor(
                                        com.android.internal.R.color
                                                .system_notification_accent_color))
                        .addAction(addSourceAction)
                        .addAction(cancelAction)
                        .setTimeoutAfter(NOTIF_AUTO_DISMISS_MILLIS)
                        .addExtras(extras);
        nm.notify(ADD_SOURCE_NOTIFICATION_ID, builder.build());
    }

    private void cancelSharingNotification(@NonNull Context context, int notifId) {
        NotificationManager nm = context.getSystemService(NotificationManager.class);
        if (nm != null) {
            nm.cancel(notifId);
        }
    }

    private void createNotificationChannelIfNeeded(@NonNull NotificationManager nm,
            @NonNull Context context) {
        if (nm.getNotificationChannel(CHANNEL_ID) == null) {
            Log.d(TAG, "Create bluetooth notification channel");
            NotificationChannel notificationChannel =
                    new NotificationChannel(
                            CHANNEL_ID,
                            context.getString(com.android.settings.R.string.bluetooth),
                            NotificationManager.IMPORTANCE_HIGH);
            nm.createNotificationChannel(notificationChannel);
        }
    }

    private boolean isAppInForeground(@NonNull Context context) {
        try {
            ActivityManager activityManager = context.getSystemService(ActivityManager.class);
            String packageName = context.getPackageName();
            if (context.getPackageManager().checkPermission(Manifest.permission.PACKAGE_USAGE_STATS,
                    packageName) != PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "check isAppInForeground, returns false due to no permission");
                return false;
            }
            if (packageName != null && activityManager.getPackageImportance(packageName)
                    == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND) {
                Log.d(TAG, "check isAppInForeground, returns true");
                return true;
            }
        } catch (RuntimeException e) {
            Log.d(TAG, "check isAppInForeground, error = " + e.getMessage());
        }
        return false;
    }
}
