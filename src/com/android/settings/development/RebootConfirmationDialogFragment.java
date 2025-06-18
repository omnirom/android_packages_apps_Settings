/*
 * Copyright (C) 2022 The Android Open Source Project
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

package com.android.settings.development;

import android.app.Dialog;
import android.app.settings.SettingsEnums;
import android.content.DialogInterface;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.ViewModelProvider;

import com.android.settings.R;
import com.android.settings.core.instrumentation.InstrumentedDialogFragment;

/** Dialog fragment for reboot confirmation when enabling certain features. */
public class RebootConfirmationDialogFragment extends InstrumentedDialogFragment
        implements DialogInterface.OnClickListener, DialogInterface.OnDismissListener {

    @VisibleForTesting
    static final String TAG = "DevOptionRebootDlg";

    private RebootConfirmationDialogViewModel mViewModel;

    /** Show an instance of this dialog. */
    public static void show(Fragment fragment, int messageId, RebootConfirmationDialogHost host) {
        show(fragment, messageId, R.string.reboot_dialog_reboot_later, host);
    }

    /** Show an instance of this dialog with cancel button string set as cancelButtonId */
    public static void show(
            Fragment fragment,
            int messageId,
            int cancelButtonId,
            RebootConfirmationDialogHost host) {
        final FragmentManager manager = fragment.requireActivity().getSupportFragmentManager();
        if (manager.findFragmentByTag(TAG) == null) {
            final RebootConfirmationDialogFragment dialog =
                    new RebootConfirmationDialogFragment();
            RebootConfirmationDialogViewModel mViewModel = new ViewModelProvider(
                    fragment.requireActivity()).get(
                    RebootConfirmationDialogViewModel.class);
            mViewModel.setMessageId(messageId);
            mViewModel.setCancelButtonId(cancelButtonId);
            mViewModel.setHost(host);
            dialog.show(manager, TAG);
        }
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.REBOOT_CONFIRMATION_DIALOG;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mViewModel = new ViewModelProvider(requireActivity()).get(
                RebootConfirmationDialogViewModel.class);
    }

    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstances) {
        int messageId = mViewModel.getMessageId();
        int cancelButtonId = mViewModel.getCancelButtonId();
        return new AlertDialog.Builder(requireActivity())
                .setMessage(messageId)
                .setPositiveButton(R.string.reboot_dialog_reboot_now, this)
                .setNegativeButton(cancelButtonId, this)
                .create();
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        RebootConfirmationDialogHost host = mViewModel.getHost();
        if (host == null) return;
        if (which == DialogInterface.BUTTON_POSITIVE) {
            host.onRebootConfirmed(requireContext());
        } else {
            host.onRebootCancelled();
        }
    }

    @Override
    public void onDismiss(@NonNull DialogInterface dialog) {
        super.onDismiss(dialog);
        RebootConfirmationDialogHost host = mViewModel.getHost();
        if (host != null) {
            host.onRebootDialogDismissed();
        }
    }
}
