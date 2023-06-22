/*
 * Copyright 2021 The Android Open Source Project
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

package com.android.settings.biometrics.fingerprint;

import static com.android.settings.biometrics.BiometricEnrollBase.RESULT_FINISHED;
import static com.android.settings.biometrics.BiometricEnrollBase.RESULT_TIMEOUT;
import static com.android.settings.biometrics.fingerprint.FingerprintEnrollEnrolling.KEY_STATE_CANCELED;

import android.app.Activity;
import android.app.Dialog;
import android.app.settings.SettingsEnums;
import android.content.DialogInterface;
import android.content.Intent;
import android.hardware.biometrics.BiometricConstants;
import android.hardware.fingerprint.FingerprintManager;
import android.os.Bundle;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.FragmentManager;

import com.android.settings.R;
import com.android.settings.biometrics.BiometricEnrollBase;
import com.android.settings.core.instrumentation.InstrumentedDialogFragment;

/** Fingerprint error dialog, will be shown when an error occurs during fingerprint enrollment. */
public class FingerprintErrorDialog extends InstrumentedDialogFragment {

    public static final String KEY_ERROR_MSG = "error_msg";
    public static final String KEY_ERROR_TITLE = "error_title";
    public static final String KEY_ERROR_ID = "error_id";
    public static final String KEY_UDFPS = "is_udfps";

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        final CharSequence errorString = getArguments().getCharSequence(KEY_ERROR_MSG);
        final CharSequence errorTitle = getArguments().getCharSequence(KEY_ERROR_TITLE);
        final int errMsgId = getArguments().getInt(KEY_ERROR_ID);
        final boolean canAssumeUdfps = getArguments().getBoolean(KEY_UDFPS, false);
        final boolean wasTimeout = errMsgId == BiometricConstants.BIOMETRIC_ERROR_TIMEOUT;

        builder.setTitle(errorTitle)
                .setMessage(errorString)
                .setCancelable(false)
                .setPositiveButton(
                        R.string.security_settings_fingerprint_enroll_dialog_ok,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                                final Activity activity = getActivity();
                                if (wasTimeout && !canAssumeUdfps) {
                                    activity.setResult(RESULT_TIMEOUT);
                                } else {
                                    activity.setResult(RESULT_FINISHED);
                                }
                                activity.finish();
                            }
                        });
        if (wasTimeout && canAssumeUdfps) {
            builder.setPositiveButton(
                            R.string.security_settings_fingerprint_enroll_dialog_try_again,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                    final Activity activity = getActivity();
                                    final Intent intent = activity.getIntent();
                                    intent.addFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT);
                                    intent.putExtra(KEY_STATE_CANCELED, false);
                                    activity.startActivity(intent);
                                    activity.finish();
                                }
                            })
                    .setNegativeButton(
                            R.string.security_settings_fingerprint_enroll_dialog_ok,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                    final Activity activity = getActivity();
                                    activity.setResult(RESULT_TIMEOUT);
                                    activity.finish();
                                }
                            });
        }
        final AlertDialog dialog = builder.create();
        dialog.setCanceledOnTouchOutside(false);
        return dialog;
    }

    public static void showErrorDialog(BiometricEnrollBase host, int errMsgId,
            boolean canAssumeUdfps) {
        if (host.isFinishing()) {
            return;
        }
        final FragmentManager fragmentManager = host.getSupportFragmentManager();
        if (fragmentManager.isDestroyed() || fragmentManager.isStateSaved()) {
            return;
        }
        CharSequence errMsg = host.getText(getErrorMessage(errMsgId));
        final CharSequence errTitle = host.getText(getErrorTitle(errMsgId));
        if (!canAssumeUdfps
                && errMsgId == BiometricConstants.BIOMETRIC_ERROR_TIMEOUT) {
            errMsg = host.getText(getErrorMessage(BiometricConstants.BIOMETRIC_ERROR_CANCELED));
        }
        final FingerprintErrorDialog dialog = newInstance(errMsg, errTitle,
                errMsgId, canAssumeUdfps);
        dialog.show(fragmentManager, FingerprintErrorDialog.class.getName());
    }

    private static int getErrorMessage(int errMsgId) {
        switch (errMsgId) {
            case FingerprintManager.FINGERPRINT_ERROR_TIMEOUT:
                // This message happens when the underlying crypto layer decides to revoke
                // the enrollment auth token.
                return R.string.security_settings_fingerprint_enroll_error_timeout_dialog_message;
            case FingerprintManager.FINGERPRINT_ERROR_BAD_CALIBRATION:
                return R.string.security_settings_fingerprint_bad_calibration;
            default:
                // There's nothing specific to tell the user about. Ask them to try again.
                return R.string.security_settings_fingerprint_enroll_error_generic_dialog_message;
        }
    }

    private static int getErrorTitle(int errMsgId) {
        switch (errMsgId) {
            case FingerprintManager.FINGERPRINT_ERROR_UNABLE_TO_PROCESS:
                return R.string
                        .security_settings_fingerprint_enroll_error_unable_to_process_dialog_title;
            default:
                return R.string.security_settings_fingerprint_enroll_error_dialog_title;
        }
    }

    private static FingerprintErrorDialog newInstance(CharSequence msg, CharSequence title,
            int msgId, boolean canAssumeUdfps) {
        final FingerprintErrorDialog dialog = new FingerprintErrorDialog();
        final Bundle args = new Bundle();
        args.putCharSequence(KEY_ERROR_MSG, msg);
        args.putCharSequence(KEY_ERROR_TITLE, title);
        args.putInt(KEY_ERROR_ID, msgId);
        args.putBoolean(KEY_UDFPS, canAssumeUdfps);
        dialog.setArguments(args);
        return dialog;
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.DIALOG_FINGERPINT_ERROR;
    }
}
