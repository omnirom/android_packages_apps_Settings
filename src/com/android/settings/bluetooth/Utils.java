/*
 * Copyright (C) 2011 The Android Open Source Project
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

package com.android.settings.bluetooth;

import static android.os.Process.BLUETOOTH_UID;

import android.app.settings.SettingsEnums;
import android.bluetooth.BluetoothCsipSetCoordinator;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.appcompat.app.AlertDialog;

import com.android.settings.R;
import com.android.settings.flags.Flags;
import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.bluetooth.BluetoothUtils;
import com.android.settingslib.bluetooth.BluetoothUtils.ErrorListener;
import com.android.settingslib.bluetooth.CachedBluetoothDevice;
import com.android.settingslib.bluetooth.CachedBluetoothDeviceManager;
import com.android.settingslib.bluetooth.HearingAidProfile;
import com.android.settingslib.bluetooth.LeAudioProfile;
import com.android.settingslib.bluetooth.LocalBluetoothLeBroadcast;
import com.android.settingslib.bluetooth.LocalBluetoothLeBroadcastAssistant;
import com.android.settingslib.bluetooth.LocalBluetoothManager;
import com.android.settingslib.bluetooth.LocalBluetoothManager.BluetoothManagerCallback;
import com.android.settingslib.bluetooth.LocalBluetoothProfile;
import com.android.settingslib.bluetooth.LocalBluetoothProfileManager;
import com.android.settingslib.utils.ThreadUtils;

import com.google.common.base.Supplier;
import com.google.common.collect.ImmutableList;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.stream.Collectors;

/**
 * Utils is a helper class that contains constants for various
 * Android resource IDs, debug logging flags, and static methods
 * for creating dialogs.
 */
public final class Utils {

    private static final String TAG = "BluetoothUtils";
    private static final String ENABLE_DUAL_MODE_AUDIO = "persist.bluetooth.enable_dual_mode_audio";

    static final boolean V = BluetoothUtils.V; // verbose logging
    static final boolean D = BluetoothUtils.D;  // regular logging

    private Utils() {
    }

    public static int getConnectionStateSummary(int connectionState) {
        switch (connectionState) {
            case BluetoothProfile.STATE_CONNECTED:
                return com.android.settingslib.R.string.bluetooth_connected;
            case BluetoothProfile.STATE_CONNECTING:
                return com.android.settingslib.R.string.bluetooth_connecting;
            case BluetoothProfile.STATE_DISCONNECTED:
                return com.android.settingslib.R.string.bluetooth_disconnected;
            case BluetoothProfile.STATE_DISCONNECTING:
                return com.android.settingslib.R.string.bluetooth_disconnecting;
            default:
                return 0;
        }
    }

    // Create (or recycle existing) and show disconnect dialog.
    static AlertDialog showDisconnectDialog(Context context,
            AlertDialog dialog,
            DialogInterface.OnClickListener disconnectListener,
            CharSequence title, CharSequence message) {
        if (dialog == null) {
            dialog = new AlertDialog.Builder(context)
                    .setPositiveButton(android.R.string.ok, disconnectListener)
                    .setNegativeButton(android.R.string.cancel, null)
                    .create();
        } else {
            if (dialog.isShowing()) {
                dialog.dismiss();
            }
            // use disconnectListener for the correct profile(s)
            CharSequence okText = context.getText(android.R.string.ok);
            dialog.setButton(DialogInterface.BUTTON_POSITIVE,
                    okText, disconnectListener);
        }
        dialog.setTitle(title);
        dialog.setMessage(message);
        dialog.show();
        return dialog;
    }

    @VisibleForTesting
    static void showConnectingError(Context context, String name, LocalBluetoothManager manager) {
        FeatureFactory.getFeatureFactory().getMetricsFeatureProvider().visible(context,
                SettingsEnums.PAGE_UNKNOWN, SettingsEnums.ACTION_SETTINGS_BLUETOOTH_CONNECT_ERROR,
                0);
        showError(context, name, R.string.bluetooth_connecting_error_message, manager);
    }

    static void showError(Context context, String name, int messageResId) {
        showError(context, name, messageResId, getLocalBtManager(context));
    }

    private static void showError(Context context, String name, int messageResId,
            LocalBluetoothManager manager) {
        String message = context.getString(messageResId, name);
        Context activity = manager.getForegroundActivity();
        if (manager.isForegroundActivity()) {
            try {
                new AlertDialog.Builder(activity)
                        .setTitle(R.string.bluetooth_error_title)
                        .setMessage(message)
                        .setPositiveButton(android.R.string.ok, null)
                        .show();
            } catch (Exception e) {
                Log.e(TAG, "Cannot show error dialog.", e);
            }
        } else {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
        }
    }

    public static LocalBluetoothManager getLocalBtManager(Context context) {
        return LocalBluetoothManager.getInstance(context, mOnInitCallback);
    }

    /**
     * Obtains a {@link LocalBluetoothManager}.
     *
     * To avoid StrictMode ThreadPolicy violation, will get it in another thread.
     */
    public static LocalBluetoothManager getLocalBluetoothManager(Context context) {
        final FutureTask<LocalBluetoothManager> localBtManagerFutureTask = new FutureTask<>(
                // Avoid StrictMode ThreadPolicy violation
                () -> getLocalBtManager(context));
        try {
            localBtManagerFutureTask.run();
            return localBtManagerFutureTask.get();
        } catch (InterruptedException | ExecutionException e) {
            Log.w(TAG, "Error getting LocalBluetoothManager.", e);
            return null;
        }
    }

    public static String createRemoteName(Context context, BluetoothDevice device) {
        String mRemoteName = device != null ? device.getAlias() : null;

        if (mRemoteName == null) {
            mRemoteName = context.getString(R.string.unknown);
        }
        return mRemoteName;
    }

    private static final ErrorListener mErrorListener = new ErrorListener() {
        @Override
        public void onShowError(Context context, String name, int messageResId) {
            showError(context, name, messageResId);
        }
    };

    private static final BluetoothManagerCallback mOnInitCallback = new BluetoothManagerCallback() {
        @Override
        public void onBluetoothManagerInitialized(Context appContext,
                LocalBluetoothManager bluetoothManager) {
            BluetoothUtils.setErrorListener(mErrorListener);
        }
    };

    public static boolean isBluetoothScanningEnabled(Context context) {
        return Settings.Global.getInt(context.getContentResolver(),
                Settings.Global.BLE_SCAN_ALWAYS_AVAILABLE, 0) == 1;
    }

    /**
     * Returns the Bluetooth Package name
     */
    public static String findBluetoothPackageName(Context context)
            throws NameNotFoundException {
        // this activity will always be in the package where the rest of Bluetooth lives
        final String sentinelActivity = "com.android.bluetooth.opp.BluetoothOppLauncherActivity";
        PackageManager packageManager = context.createContextAsUser(UserHandle.SYSTEM, 0)
                .getPackageManager();
        String[] allPackages = packageManager.getPackagesForUid(BLUETOOTH_UID);
        String matchedPackage = null;
        for (String candidatePackage : allPackages) {
            PackageInfo packageInfo;
            try {
                packageInfo =
                        packageManager.getPackageInfo(
                                candidatePackage,
                                PackageManager.GET_ACTIVITIES
                                        | PackageManager.MATCH_ANY_USER
                                        | PackageManager.MATCH_UNINSTALLED_PACKAGES
                                        | PackageManager.MATCH_DISABLED_COMPONENTS);
            } catch (NameNotFoundException e) {
                // rethrow
                throw e;
            }
            if (packageInfo.activities == null) {
                continue;
            }
            for (ActivityInfo activity : packageInfo.activities) {
                if (sentinelActivity.equals(activity.name)) {
                    if (matchedPackage == null) {
                        matchedPackage = candidatePackage;
                    } else {
                        throw new NameNotFoundException("multiple main bluetooth packages found");
                    }
                }
            }
        }
        if (matchedPackage != null) {
            return matchedPackage;
        }
        throw new NameNotFoundException("Could not find main bluetooth package");
    }

    /**
     * Returns all cachedBluetoothDevices with the same groupId.
     * @param cachedBluetoothDevice The main cachedBluetoothDevice.
     * @return all cachedBluetoothDevices with the same groupId.
     */
    public static Set<CachedBluetoothDevice> findAllCachedBluetoothDevicesByGroupId(
            LocalBluetoothManager localBtMgr,
            CachedBluetoothDevice cachedBluetoothDevice) {
        Set<CachedBluetoothDevice> cachedBluetoothDevices = new HashSet<>();
        if (cachedBluetoothDevice == null) {
            Log.e(TAG, "findAllCachedBluetoothDevicesByGroupId: no cachedBluetoothDevice");
            return cachedBluetoothDevices;
        }
        int deviceGroupId = cachedBluetoothDevice.getGroupId();
        if (deviceGroupId == BluetoothCsipSetCoordinator.GROUP_ID_INVALID) {
            cachedBluetoothDevices.add(cachedBluetoothDevice);
            return cachedBluetoothDevices;
        }

        if (localBtMgr == null) {
            Log.e(TAG, "findAllCachedBluetoothDevicesByGroupId: no LocalBluetoothManager");
            return cachedBluetoothDevices;
        }
        CachedBluetoothDevice mainDevice =
                localBtMgr.getCachedDeviceManager().getCachedDevicesCopy().stream()
                        .filter(cachedDevice -> cachedDevice.getGroupId() == deviceGroupId)
                        .findFirst().orElse(null);
        if (mainDevice == null) {
            Log.e(TAG, "findAllCachedBluetoothDevicesByGroupId: groupId = " + deviceGroupId
                    + ", no main device.");
            return cachedBluetoothDevices;
        }
        cachedBluetoothDevice = mainDevice;
        cachedBluetoothDevices.add(cachedBluetoothDevice);
        cachedBluetoothDevices.addAll(cachedBluetoothDevice.getMemberDevice());
        Log.d(TAG, "findAllCachedBluetoothDevicesByGroupId: groupId = " + deviceGroupId
                + " , cachedBluetoothDevice = " + cachedBluetoothDevice
                + " , deviceList = " + cachedBluetoothDevices);
        return cachedBluetoothDevices;
    }

    /**
     * Preloads the values and run the Runnable afterwards.
     * @param suppliers the value supplier, should be a memoized supplier
     * @param runnable the runnable to be run after value is preloaded
     */
    public static void preloadAndRun(List<Supplier<?>> suppliers, Runnable runnable) {
        if (!Flags.enableOffloadBluetoothOperationsToBackgroundThread()) {
            runnable.run();
            return;
        }
        ThreadUtils.postOnBackgroundThread(() -> {
            for (Supplier<?> supplier : suppliers) {
                Object unused = supplier.get();
            }
            ThreadUtils.postOnMainThread(runnable);
        });
    }

    /**
     * Check if need to block pairing during audio sharing
     *
     * @param localBtManager {@link LocalBluetoothManager}
     * @return if need to block pairing during audio sharing
     */
    public static boolean shouldBlockPairingInAudioSharing(
            @NonNull LocalBluetoothManager localBtManager) {
        if (!BluetoothUtils.isBroadcasting(localBtManager)) return false;
        LocalBluetoothLeBroadcastAssistant assistant =
                localBtManager.getProfileManager().getLeAudioBroadcastAssistantProfile();
        CachedBluetoothDeviceManager deviceManager = localBtManager.getCachedDeviceManager();
        List<BluetoothDevice> connectedDevices =
                assistant == null ? ImmutableList.of() : assistant.getAllConnectedDevices();
        Collection<CachedBluetoothDevice> bondedDevices =
                deviceManager == null ? ImmutableList.of() : deviceManager.getCachedDevicesCopy();
        // Block the pairing if there is ongoing audio sharing session and
        // a) there is already one temp bond sink bonded
        // or b) there are already two sinks joining the audio sharing
        return assistant != null && deviceManager != null
                && (bondedDevices.stream().anyMatch(
                        d -> BluetoothUtils.isTemporaryBondDevice(d.getDevice())
                                && d.getBondState() == BluetoothDevice.BOND_BONDED)
                || connectedDevices.stream().filter(
                        d -> BluetoothUtils.hasActiveLocalBroadcastSourceForBtDevice(d,
                                localBtManager))
                .map(d -> BluetoothUtils.getGroupId(deviceManager.findDevice(d))).collect(
                        Collectors.toSet()).size() >= 2);
    }

    /**
     * Show block pairing dialog during audio sharing
     * @param context The dialog context
     * @param dialog The dialog if already exists
     * @param localBtManager {@link LocalBluetoothManager}
     * @return The block pairing dialog
     */
    @Nullable
    static AlertDialog showBlockPairingDialog(@NonNull Context context,
            @Nullable AlertDialog dialog, @Nullable LocalBluetoothManager localBtManager) {
        if (!com.android.settingslib.flags.Flags.enableTemporaryBondDevicesUi()) return null;
        if (dialog != null && dialog.isShowing()) return dialog;
        if (dialog == null) {
            AlertDialog.Builder builder = new AlertDialog.Builder(context)
                    .setNegativeButton(android.R.string.cancel, null)
                    .setTitle(R.string.audio_sharing_block_pairing_dialog_title)
                    .setMessage(R.string.audio_sharing_block_pairing_dialog_content);
            LocalBluetoothLeBroadcast broadcast = localBtManager == null ? null :
                    localBtManager.getProfileManager().getLeAudioBroadcastProfile();
            if (broadcast != null) {
                builder.setPositiveButton(R.string.audio_sharing_turn_off_button_label,
                        (dlg, which) -> broadcast.stopLatestBroadcast());
            }
            dialog = builder.create();
        }
        dialog.show();
        return dialog;
    }

    /** Enables/disables LE Audio profile for the device. */
    public static void setLeAudioEnabled(
            @NonNull LocalBluetoothManager manager,
            @NonNull CachedBluetoothDevice cachedDevice,
            boolean enable) {
        List<CachedBluetoothDevice> devices =
                List.copyOf(findAllCachedBluetoothDevicesByGroupId(manager, cachedDevice));
        setLeAudioEnabled(manager, devices, enable);
    }

    /** Enables/disables LE Audio profile for the devices in the same csip group. */
    public static void setLeAudioEnabled(
            @NonNull LocalBluetoothManager manager,
            @NonNull List<CachedBluetoothDevice> devicesWithSameGroupId,
            boolean enable) {
        LocalBluetoothProfileManager profileManager = manager.getProfileManager();
        LeAudioProfile leAudioProfile = profileManager.getLeAudioProfile();
        List<CachedBluetoothDevice> leAudioDevices =
                getDevicesWithProfile(devicesWithSameGroupId, leAudioProfile);
        if (leAudioDevices.isEmpty()) {
            Log.i(TAG, "Fail to setLeAudioEnabled, no LE Audio profile found.");
        }
        boolean dualModeEnabled = SystemProperties.getBoolean(ENABLE_DUAL_MODE_AUDIO, false);

        if (enable && !dualModeEnabled) {
            Log.i(TAG, "Disabling classic audio profiles because dual mode is disabled");
            setProfileEnabledWhenChangingLeAudio(
                    devicesWithSameGroupId, profileManager.getA2dpProfile(), false);
            setProfileEnabledWhenChangingLeAudio(
                    devicesWithSameGroupId, profileManager.getHeadsetProfile(), false);
        }

        HearingAidProfile asha = profileManager.getHearingAidProfile();
        LocalBluetoothLeBroadcastAssistant broadcastAssistant =
                profileManager.getLeAudioBroadcastAssistantProfile();

        for (CachedBluetoothDevice leAudioDevice : leAudioDevices) {
            Log.d(
                    TAG,
                    "device:"
                            + leAudioDevice.getDevice().getAnonymizedAddress()
                            + " set LE profile enabled: "
                            + enable);
            leAudioProfile.setEnabled(leAudioDevice.getDevice(), enable);
            if (asha != null) {
                asha.setEnabled(leAudioDevice.getDevice(), !enable);
            }
            if (broadcastAssistant != null) {
                Log.d(
                        TAG,
                        "device:"
                                + leAudioDevice.getDevice().getAnonymizedAddress()
                                + " enable LE broadcast assistant profile: "
                                + enable);
                broadcastAssistant.setEnabled(leAudioDevice.getDevice(), enable);
            }
        }

        if (!enable && !dualModeEnabled) {
            Log.i(TAG, "Enabling classic audio profiles because dual mode is disabled");
            setProfileEnabledWhenChangingLeAudio(
                    devicesWithSameGroupId, profileManager.getA2dpProfile(), true);
            setProfileEnabledWhenChangingLeAudio(
                    devicesWithSameGroupId, profileManager.getHeadsetProfile(), true);
        }
    }

    private static List<CachedBluetoothDevice> getDevicesWithProfile(
            List<CachedBluetoothDevice> devices, LocalBluetoothProfile profile) {
        List<CachedBluetoothDevice> devicesWithProfile = new ArrayList<>();
        for (CachedBluetoothDevice device : devices) {
            for (LocalBluetoothProfile currentProfile : device.getProfiles()) {
                if (currentProfile.toString().equals(profile.toString())) {
                    devicesWithProfile.add(device);
                }
            }
        }
        return devicesWithProfile;
    }

    private static void setProfileEnabledWhenChangingLeAudio(
            List<CachedBluetoothDevice> devices,
            @Nullable LocalBluetoothProfile profile,
            boolean enable) {
        if (profile == null) {
            Log.i(TAG, "profile is null");
            return;
        }
        List<CachedBluetoothDevice> deviceWithProfile = getDevicesWithProfile(devices, profile);
        Log.d(TAG, "Set " + profile + " enabled:" + enable + " when switching LE Audio");
        for (CachedBluetoothDevice profileDevice : deviceWithProfile) {
            if (profile.isEnabled(profileDevice.getDevice()) != enable) {
                Log.d(
                        TAG,
                        "The "
                                + profileDevice.getDevice().getAnonymizedAddress()
                                + ":"
                                + profile
                                + " set to "
                                + enable);
                profile.setEnabled(profileDevice.getDevice(), enable);
            } else {
                Log.d(
                        TAG,
                        "The "
                                + profileDevice.getDevice().getAnonymizedAddress()
                                + ":"
                                + profile
                                + " profile is already "
                                + enable
                                + ". Do nothing.");
            }
        }
    }
}
