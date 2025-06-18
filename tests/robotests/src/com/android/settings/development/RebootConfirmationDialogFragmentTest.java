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

package com.android.settings.development;

import static android.os.Looper.getMainLooper;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.verify;
import static org.robolectric.Shadows.shadowOf;

import android.content.DialogInterface;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.ViewModelProvider;

import com.android.settings.R;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.shadows.ShadowDialog;

@RunWith(RobolectricTestRunner.class)
public class RebootConfirmationDialogFragmentTest {

    @Mock
    private RebootConfirmationDialogHost mHost;
    private FragmentActivity mActivity;
    private Fragment mFragment;
    private FragmentManager mFragmentManager;
    private RebootConfirmationDialogViewModel mViewModel;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mActivity = Robolectric.buildActivity(FragmentActivity.class).create().get();
        mFragmentManager = mActivity.getSupportFragmentManager();
        mFragment = new Fragment();
        FragmentTransaction transaction = mFragmentManager.beginTransaction();
        transaction.add(mFragment, "testFragment");
        transaction.commit();
        mFragmentManager.executePendingTransactions();
    }

    @Test
    public void show_shouldCreateAndShowDialog() {
        RebootConfirmationDialogFragment.show(mFragment,
                R.string.reboot_dialog_override_desktop_mode, mHost);
        shadowOf(getMainLooper()).idle();

        RebootConfirmationDialogFragment dialogFragment =
                (RebootConfirmationDialogFragment) mFragmentManager.findFragmentByTag(
                        RebootConfirmationDialogFragment.TAG);
        assertThat(dialogFragment).isNotNull();
        assertThat(dialogFragment.getShowsDialog()).isTrue();
    }

    @Test
    public void show_shouldStoreViewModel() {
        RebootConfirmationDialogFragment.show(mFragment,
                R.string.reboot_dialog_override_desktop_mode, R.string.reboot_dialog_reboot_later,
                mHost);
        shadowOf(getMainLooper()).idle();

        mViewModel = new ViewModelProvider(mActivity).get(RebootConfirmationDialogViewModel.class);
        assertThat(mViewModel.getHost()).isEqualTo(mHost);
        assertThat(mViewModel.getMessageId()).isEqualTo(
                R.string.reboot_dialog_override_desktop_mode);
        assertThat(mViewModel.getCancelButtonId()).isEqualTo(R.string.reboot_dialog_reboot_later);
    }

    @Test
    public void onCreateDialog_shouldCreateAlertDialogFromViewModel() {
        RebootConfirmationDialogFragment dialogFragment = new RebootConfirmationDialogFragment();
        dialogFragment.show(mFragmentManager, RebootConfirmationDialogFragment.TAG);
        shadowOf(getMainLooper()).idle();
        // Set up ViewModel
        mViewModel = new ViewModelProvider(mActivity).get(RebootConfirmationDialogViewModel.class);
        mViewModel.setMessageId(R.string.reboot_dialog_override_desktop_mode);
        mViewModel.setCancelButtonId(R.string.reboot_dialog_reboot_later);
        mViewModel.setHost(mHost);

        dialogFragment.onCreateDialog(null).show();
        shadowOf(getMainLooper()).idle();

        AlertDialog alertDialog = (AlertDialog) ShadowDialog.getLatestDialog();
        TextView messageView = alertDialog.findViewById(android.R.id.message);
        assertThat(messageView.getText().toString()).isEqualTo(
                mActivity.getString(R.string.reboot_dialog_override_desktop_mode));
        assertThat(alertDialog.getButton(
                DialogInterface.BUTTON_POSITIVE).getText().toString()).isEqualTo(
                mActivity.getString(R.string.reboot_dialog_reboot_now));
        assertThat(alertDialog.getButton(
                DialogInterface.BUTTON_NEGATIVE).getText().toString()).isEqualTo(
                mActivity.getString(R.string.reboot_dialog_reboot_later));
    }

    @Test
    public void onClick_positiveButton_shouldCallRebootConfirmed() {
        RebootConfirmationDialogFragment dialogFragment = showDialog();
        AlertDialog alertDialog = (AlertDialog) ShadowDialog.getLatestDialog();

        dialogFragment.onClick(alertDialog, DialogInterface.BUTTON_POSITIVE);
        verify(mHost).onRebootConfirmed(mActivity);
    }

    @Test
    public void onClick_negativeButton_shouldCallRebootCancelled() {
        RebootConfirmationDialogFragment dialogFragment = showDialog();
        AlertDialog alertDialog = (AlertDialog) ShadowDialog.getLatestDialog();

        dialogFragment.onClick(alertDialog, DialogInterface.BUTTON_NEGATIVE);
        verify(mHost).onRebootCancelled();
    }

    @Test
    public void onDismiss_shouldCallRebootDialogDismissed() {
        RebootConfirmationDialogFragment dialogFragment = showDialog();

        dialogFragment.onDismiss(null);
        verify(mHost).onRebootDialogDismissed();
    }

    private RebootConfirmationDialogFragment showDialog() {
        RebootConfirmationDialogFragment dialogFragment = new RebootConfirmationDialogFragment();
        dialogFragment.show(mFragmentManager, RebootConfirmationDialogFragment.TAG);
        shadowOf(getMainLooper()).idle();

        mViewModel = new ViewModelProvider(mActivity).get(RebootConfirmationDialogViewModel.class);
        mViewModel.setMessageId(R.string.reboot_dialog_override_desktop_mode);
        mViewModel.setCancelButtonId(R.string.reboot_dialog_reboot_later);
        mViewModel.setHost(mHost);

        dialogFragment.onCreateDialog(null).show();
        shadowOf(getMainLooper()).idle();
        return dialogFragment;
    }
}
