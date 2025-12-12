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

package com.android.settings.accessibility;

import android.app.Dialog;
import android.app.settings.SettingsEnums;
import android.content.Context;
import android.media.AudioManager;
import android.os.Bundle;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.FragmentManager;

import com.android.internal.annotations.VisibleForTesting;
import com.android.settings.R;
import com.android.settings.core.TogglePreferenceController;
import com.android.settings.core.instrumentation.InstrumentedDialogFragment;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.overlay.FeatureFactory;

/** Preference controller for Hearing Aid Compatibility (HAC) settings */
public class HearingAidCompatibilityPreferenceController extends TogglePreferenceController {

    private static final String TAG =
            HearingAidCompatibilityPreferenceController.class.getSimpleName();

    // Hearing Aid Compatibility settings values
    static final String HAC_KEY = "HACSetting";
    static final String HAC_VAL_ON = "ON";
    static final String HAC_VAL_OFF = "OFF";
    @VisibleForTesting
    static final int HAC_DISABLED = 0;
    @VisibleForTesting
    static final int HAC_ENABLED = 1;

    private final TelephonyManager mTelephonyManager;
    private final AudioManager mAudioManager;
    private FragmentManager mFragmentManager;

    public HearingAidCompatibilityPreferenceController(Context context,
            String preferenceKey) {
        super(context, preferenceKey);
        mTelephonyManager = context.getSystemService(TelephonyManager.class);
        mAudioManager = context.getSystemService(AudioManager.class);
    }

    void init(DashboardFragment fragment) {
        mFragmentManager = fragment.getParentFragmentManager();
    }

    @Override
    public int getAvailabilityStatus() {
        try {
            return mTelephonyManager.isHearingAidCompatibilitySupported() ? AVAILABLE
                    : UNSUPPORTED_ON_DEVICE;
        } catch (UnsupportedOperationException e) {
            // Device doesn't support FEATURE_TELEPHONY_CALLING
            return UNSUPPORTED_ON_DEVICE;
        }
    }

    @Override
    public boolean isChecked() {
        final int hac = Settings.System.getInt(mContext.getContentResolver(),
                Settings.System.HEARING_AID_COMPATIBILITY, HAC_DISABLED);
        return hac == HAC_ENABLED;
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        if (isChecked && shouldShowDisclaimer()) {
            HacDisclaimerDialog.newInstance().show(mFragmentManager, TAG);
        }
        FeatureFactory.getFeatureFactory().getMetricsFeatureProvider().changed(
                getMetricsCategory(), getPreferenceKey(), isChecked ? 1 : 0);
        setAudioParameterHacEnabled(isChecked);
        return Settings.System.putInt(mContext.getContentResolver(),
                Settings.System.HEARING_AID_COMPATIBILITY,
                (isChecked ? HAC_ENABLED : HAC_DISABLED));
    }

    @Override
    public int getSliceHighlightMenuRes() {
        return R.string.menu_key_accessibility;
    }

    private void setAudioParameterHacEnabled(boolean enabled) {
        mAudioManager.setParameters(HAC_KEY + "=" + (enabled ? HAC_VAL_ON : HAC_VAL_OFF) + ";");
    }

    private boolean shouldShowDisclaimer() {
        return !TextUtils.isEmpty(mContext.getText(R.string.hac_disclaimer_message));
    }

    /** Dialog to tell user about the disclaimer to turn on HAC */
    public static class HacDisclaimerDialog extends InstrumentedDialogFragment {

        /**
         * Creates a new instance of the HAC (Hearing Aid Compatibility) disclaimer dialog.
         *
         * @return A new instance of {@link HacDisclaimerDialog}.
         */
        public static HacDisclaimerDialog newInstance() {
            return new HacDisclaimerDialog();
        }

        @NonNull
        @Override
        public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
            return new AlertDialog.Builder(getActivity())
                    .setMessage(R.string.hac_disclaimer_message)
                    .setPositiveButton(android.R.string.ok, null)
                    .create();
        }

        @Override
        public int getMetricsCategory() {
            return SettingsEnums.DIALOG_HAC_DISCLAIMER;
        }
    }
}
