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
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.util.Pair;

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
import com.google.common.collect.Iterables;

import java.util.List;

public class AudioSharingStopDialogFragment extends InstrumentedDialogFragment {
    private static final String TAG = "AudioSharingStopDialog";

    private static final String BUNDLE_KEY_DEVICE_TO_DISCONNECT_ITEMS =
            "bundle_key_device_to_disconnect_items";
    private static final String BUNDLE_KEY_NEW_DEVICE_NAME = "bundle_key_new_device_name";

    // The host creates an instance of this dialog fragment must implement this interface to receive
    // event callbacks.
    public interface DialogEventListener {
        /** Called when users click the stop sharing button in the dialog. */
        void onStopSharingClick();
    }

    @Nullable private static DialogEventListener sListener;
    @Nullable private static CachedBluetoothDevice sCachedDevice;
    private static ImmutableList<Pair<Integer, Object>> sEventData = ImmutableList.of();

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.DIALOG_STOP_AUDIO_SHARING;
    }

    /**
     * Display the {@link AudioSharingStopDialogFragment} dialog.
     *
     * <p>If the dialog is showing, update the dialog message and event listener.
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
        sCachedDevice = newDevice;
        sEventData = eventData;
        AudioSharingUtils.postOnMainThread(
                host.getContext(),
                () -> {
                    AlertDialog dialog = AudioSharingDialogHelper.getDialogIfShowing(manager, TAG);
                    if (dialog != null) {
                        Log.d(TAG, "Dialog is showing, update the content.");
                        updateDialog(host.getContext(), deviceItems, newDevice.getName(), dialog);
                    } else {
                        Log.d(TAG, "Show up the dialog.");
                        final Bundle bundle = new Bundle();
                        bundle.putParcelableList(
                                BUNDLE_KEY_DEVICE_TO_DISCONNECT_ITEMS, deviceItems);
                        bundle.putString(BUNDLE_KEY_NEW_DEVICE_NAME, newDevice.getName());
                        AudioSharingStopDialogFragment dialogFrag =
                                new AudioSharingStopDialogFragment();
                        dialogFrag.setArguments(bundle);
                        dialogFrag.show(manager, TAG);
                    }
                });
        return true;
    }

    /** Return the tag of {@link AudioSharingStopDialogFragment} dialog. */
    public static @NonNull String tag() {
        return TAG;
    }

    /** Get the latest connected device which triggers the dialog. */
    public @Nullable CachedBluetoothDevice getDevice() {
        return sCachedDevice;
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
        String newDeviceName = arguments.getString(BUNDLE_KEY_NEW_DEVICE_NAME);
        DialogInterface.OnClickListener onPositiveBtnClicked =
                (dlg, which) -> {
                    if (sListener != null) {
                        sListener.onStopSharingClick();
                        mMetricsFeatureProvider.action(
                                getMetricsCategory(),
                                SettingsEnums.ACTION_AUDIO_SHARING_DIALOG_POSITIVE_BTN_CLICKED,
                                getMetricsCategory(),
                                sEventData.toString(),
                                /* changedPreferenceIntValue= */ 0);
                    }
                };
        DialogInterface.OnClickListener onNegativeBtnClicked =
                (dlg, which) ->
                        mMetricsFeatureProvider.action(
                                getMetricsCategory(),
                                SettingsEnums.ACTION_AUDIO_SHARING_DIALOG_NEGATIVE_BTN_CLICKED,
                                getMetricsCategory(),
                                sEventData.toString(),
                                /* changedPreferenceIntValue= */ 0);
        AlertDialog dialog =
                AudioSharingDialogFactory.newBuilder(getActivity())
                        .setTitleIcon(com.android.settings.R.drawable.ic_warning_24dp)
                        .setIsCustomBodyEnabled(true)
                        .setPositiveButton(
                                R.string.audio_sharing_connect_button_label, onPositiveBtnClicked)
                        .setNegativeButton(
                                com.android.settings.R.string.cancel, onNegativeBtnClicked)
                        .build();
        dialog.show();
        updateDialog(getContext(), deviceItems, newDeviceName, dialog);
        return dialog;
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
            @NonNull Context context,
            @Nullable List<AudioSharingDeviceItem> deviceItems,
            String newDeviceName,
            @NonNull AlertDialog dialog) {
        String title = context.getString(R.string.audio_sharing_stop_dialog_title, newDeviceName);
        String customMessage = "";
        if (deviceItems != null) {
            if (deviceItems.size() == 1) {
                customMessage =
                        context.getString(
                                R.string.audio_sharing_stop_dialog_content,
                                Iterables.getOnlyElement(deviceItems).getName());
            } else if (deviceItems.size() == 2) {
                customMessage =
                        context.getString(
                                R.string.audio_sharing_stop_dialog_with_two_content,
                                deviceItems.get(0).getName(),
                                deviceItems.get(1).getName());
            } else {
                customMessage =
                        context.getString(R.string.audio_sharing_stop_dialog_with_more_content);
            }
        }
        AudioSharingDialogFactory.updateTitle(dialog, title);
        AudioSharingDialogFactory.updateCustomMessage(dialog, customMessage);
    }
}
