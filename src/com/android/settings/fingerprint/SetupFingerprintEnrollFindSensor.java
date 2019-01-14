/*
 * Copyright (C) 2015 The Android Open Source Project
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
 * limitations under the License
 */

package com.android.settings.fingerprint;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.FragmentManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.UserHandle;
import android.support.annotation.NonNull;

import com.android.internal.logging.nano.MetricsProto;
import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.settings.fingerprint.FingerprintEnrollFindSensor;
import com.android.settings.R;
import com.android.settings.SetupWizardUtils;
import com.android.settings.core.instrumentation.InstrumentedDialogFragment;
import com.android.settings.password.ChooseLockSettingsHelper;

public class SetupFingerprintEnrollFindSensor extends FingerprintEnrollFindSensor {
    private boolean mSensorLocation;

    @Override
    protected int getContentView() {
        if  (mSensorLocation) {
                return R.layout.fingerprint_enroll_find_sensor_underscreen;
        } else {
                return R.layout.fingerprint_enroll_find_sensor;
        }
    }

    @Override
    protected Intent getEnrollingIntent() {
        Intent intent = new Intent(this, SetupFingerprintEnrollEnrolling.class);
        intent.putExtra(ChooseLockSettingsHelper.EXTRA_KEY_CHALLENGE_TOKEN, mToken);
        if (mUserId != UserHandle.USER_NULL) {
            intent.putExtra(Intent.EXTRA_USER_ID, mUserId);
        }
        SetupWizardUtils.copySetupExtras(getIntent(), intent);
        return intent;
    }

    @Override
    protected void onSkipButtonClick() {
        new SkipFingerprintDialog().show(getFragmentManager());
    }

    @Override
    public int getMetricsCategory() {
        return MetricsEvent.FINGERPRINT_FIND_SENSOR_SETUP;
    }

    public static class SkipFingerprintDialog extends InstrumentedDialogFragment
            implements DialogInterface.OnClickListener {
        private static final String TAG_SKIP_DIALOG = "skip_dialog";

        @Override
        public int getMetricsCategory() {
            return MetricsProto.MetricsEvent.DIALOG_FINGERPRINT_SKIP_SETUP;
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            return onCreateDialogBuilder().create();
        }

        @NonNull
        public AlertDialog.Builder onCreateDialogBuilder() {
            return new AlertDialog.Builder(getContext())
                    .setTitle(R.string.setup_fingerprint_enroll_skip_title)
                    .setPositiveButton(R.string.skip_anyway_button_label, this)
                    .setNegativeButton(R.string.go_back_button_label, this)
                    .setMessage(R.string.setup_fingerprint_enroll_skip_after_adding_lock_text);
        }

        @Override
        public void onClick(DialogInterface dialog, int button) {
            switch (button) {
                case DialogInterface.BUTTON_POSITIVE:
                    Activity activity = getActivity();
                    if (activity != null) {
                        activity.setResult(RESULT_SKIP);
                        activity.finish();
                    }
                    break;
            }
        }

        public void show(FragmentManager manager) {
            show(manager, TAG_SKIP_DIALOG);
        }
    }
}
