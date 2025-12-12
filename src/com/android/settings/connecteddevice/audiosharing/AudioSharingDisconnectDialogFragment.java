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

import android.app.Dialog;
import android.app.settings.SettingsEnums;
import android.os.Bundle;
import android.util.Log;
import android.util.Pair;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.Lifecycle;

import com.android.settings.R;
import com.android.settings.core.instrumentation.InstrumentedDialogFragment;
import com.android.settingslib.bluetooth.BluetoothUtils;
import com.android.settingslib.bluetooth.CachedBluetoothDevice;

import com.google.common.collect.ImmutableList;

import java.util.List;

public class AudioSharingDisconnectDialogFragment extends InstrumentedDialogFragment {
    private static final String TAG = "AudioSharingDisconnectDialog";

    private static final String BUNDLE_KEY_DEVICE_TO_DISCONNECT_ITEMS =
            "bundle_key_device_to_disconnect_items";

    // The host creates an instance of this dialog fragment must implement this interface to receive
    // event callbacks.
    public interface DialogEventListener {
        /**
         * Called when users click the device item to disconnect from the audio sharing in the
         * dialog.
         *
         * @param item The device item clicked.
         */
        void onItemClick(AudioSharingDeviceItem item);
    }

    @Nullable private static DialogEventListener sListener;
    @Nullable private static CachedBluetoothDevice sNewDevice;
    private static ImmutableList<Pair<Integer, Object>> sEventData = ImmutableList.of();

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.DIALOG_AUDIO_SHARING_SWITCH_DEVICE;
    }

    /**
     * Display the {@link AudioSharingDisconnectDialogFragment} dialog.
     *
     * <p>If the dialog is showing for the same group, update the dialog event listener.
     *
     * @param host The Fragment this dialog will be hosted.
     * @param deviceItems The existing connected device items in audio sharing session.
     * @param newDevice The latest connected device triggered this dialog.
     * @param listener The callback to handle the user action on this dialog.
     * @param eventData The eventData to log with for dialog onClick events.
     * @return whether the dialog is shown
     */
    public static boolean show(
            @Nullable Fragment host,
            @NonNull List<AudioSharingDeviceItem> deviceItems,
            @NonNull CachedBluetoothDevice newDevice,
            @NonNull DialogEventListener listener,
            @NonNull ImmutableList<Pair<Integer, Object>> eventData) {
        if (host == null) {
            Log.d(TAG, "Fail to show dialog, host is null");
            return false;
        }
        if (!BluetoothUtils.isAudioSharingUIAvailable(host.getContext())) {
            Log.d(TAG, "Fail to show dialog, feature disabled");
            return false;
        }
        final FragmentManager manager;
        try {
            manager = host.getChildFragmentManager();
        } catch (IllegalStateException e) {
            Log.d(TAG, "Fail to show dialog: " + e.getMessage());
            return false;
        }
        Lifecycle.State currentState = host.getLifecycle().getCurrentState();
        if (!currentState.isAtLeast(Lifecycle.State.INITIALIZED)) {
            Log.d(TAG, "Fail to show dialog with state: " + currentState);
            return false;
        }
        sListener = listener;
        sNewDevice = newDevice;
        sEventData = eventData;
        AudioSharingUtils.postOnMainThread(
                host.getContext(),
                () -> {
                    AlertDialog dialog = AudioSharingDialogHelper.getDialogIfShowing(manager, TAG);
                    if (dialog != null) {
                        Log.d(TAG, "Dialog is showing, update the content.");
                        updateDialog(ImmutableList.copyOf(deviceItems), dialog);
                    } else {
                        Log.d(TAG, "Show up the dialog.");
                        final Bundle bundle = new Bundle();
                        bundle.putParcelableList(
                                BUNDLE_KEY_DEVICE_TO_DISCONNECT_ITEMS, deviceItems);
                        AudioSharingDisconnectDialogFragment dialogFrag =
                                new AudioSharingDisconnectDialogFragment();
                        dialogFrag.setArguments(bundle);
                        dialogFrag.show(manager, TAG);
                    }
                });
        return true;
    }

    /** Return the tag of {@link AudioSharingDisconnectDialogFragment} dialog. */
    public static @NonNull String tag() {
        return TAG;
    }

    /** Get the latest connected device which triggers the dialog. */
    public @Nullable CachedBluetoothDevice getDevice() {
        return sNewDevice;
    }

    /** Test only: get the {@link DialogEventListener} passed to the dialog. */
    @VisibleForTesting
    @Nullable
    DialogEventListener getListener() {
        return sListener;
    }

    /** Test only: get the event data passed to the dialog. */
    @VisibleForTesting
    @NonNull
    ImmutableList<Pair<Integer, Object>> getEventData() {
        return sEventData;
    }

    @Override
    @NonNull
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        Bundle arguments = requireArguments();
        List<AudioSharingDeviceItem> deviceItems =
                arguments.getParcelable(BUNDLE_KEY_DEVICE_TO_DISCONNECT_ITEMS, List.class);
        View.OnClickListener onNegativeBtnClicked =
                v -> {
                    mMetricsFeatureProvider.action(
                            getMetricsCategory(),
                            SettingsEnums.ACTION_AUDIO_SHARING_DIALOG_NEGATIVE_BTN_CLICKED,
                            getMetricsCategory(),
                            sEventData.toString(),
                            /* changedPreferenceIntValue= */ 0);
                    dismiss();
                };
        AudioSharingDialogFactory.DialogBuilder builder =
                AudioSharingDialogFactory.newBuilder(getActivity())
                        .setTitle(R.string.audio_sharing_disconnect_dialog_title)
                        .setTitleIcon(com.android.settingslib.R.drawable.ic_bt_le_audio_sharing)
                        .setIsCustomBodyEnabled(true)
                        .setCustomMessage(R.string.audio_sharing_dialog_disconnect_content)
                        .setCustomNegativeButton(
                                com.android.settings.R.string.cancel, onNegativeBtnClicked);
        if (deviceItems == null) {
            Log.d(TAG, "Create dialog error: null deviceItems");
            return builder.build();
        }
        AudioSharingDeviceAdapter.OnClickListener onClickListener =
                (AudioSharingDeviceItem item) -> {
                    if (sListener != null) {
                        sListener.onItemClick(item);
                        mMetricsFeatureProvider.action(
                                getMetricsCategory(),
                                SettingsEnums.ACTION_AUDIO_SHARING_DIALOG_POSITIVE_BTN_CLICKED,
                                getMetricsCategory(),
                                sEventData.toString(),
                                /* changedPreferenceIntValue= */ 0);
                    }
                    dismiss();
                };
        builder.setCustomDeviceActions(
                new AudioSharingDeviceAdapter(
                        getContext(),
                        deviceItems,
                        onClickListener,
                        AudioSharingDeviceAdapter.ActionType.REMOVE));
        return builder.build();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        FragmentActivity activity = getActivity();
        if (activity instanceof AudioSharingJoinHandlerActivity
                && !activity.isChangingConfigurations()
                && !activity.isFinishing()) {
            Log.d(TAG, "onDestroy, finish activity = " + activity.getClass().getName());
            activity.finish();
        }
    }

    private static void updateDialog(
            @NonNull List<AudioSharingDeviceItem> deviceItems, @NonNull AlertDialog dialog) {
        AudioSharingDialogFactory.updateCustomDeviceActions(dialog, deviceItems);
    }
}
