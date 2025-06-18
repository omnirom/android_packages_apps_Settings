/*
 * Copyright (C) 2025 The Android Open Source Project
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

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;

import com.google.common.annotations.VisibleForTesting;

public class HearingDevicePairingIntroPreferenceController extends BasePreferenceController {
    private final HearingAidHelper mHelper;

    public HearingDevicePairingIntroPreferenceController(
            @NonNull Context context,
            @NonNull String preferenceKey) {
        super(context, preferenceKey);
        mHelper = new HearingAidHelper(context);
    }

    @VisibleForTesting
    public HearingDevicePairingIntroPreferenceController(
            @NonNull Context context,
            @NonNull String preferenceKey,
            @NonNull HearingAidHelper hearingAidHelper) {
        super(context, preferenceKey);
        mHelper = hearingAidHelper;
    }

    @Override
    public int getAvailabilityStatus() {
        return mHelper.isHearingAidSupported() ? AVAILABLE : UNSUPPORTED_ON_DEVICE;
    }

    @Override
    public void displayPreference(@NonNull PreferenceScreen screen) {
        super.displayPreference(screen);

        final Preference pairingIntroPreference = screen.findPreference(getPreferenceKey());
        final boolean isAshaProfileSupported = mHelper.isAshaProfileSupported();
        final boolean isHapClientProfileSupported = mHelper.isHapClientProfileSupported();
        if (isAshaProfileSupported && isHapClientProfileSupported) {
            pairingIntroPreference.setTitle(
                    mContext.getString(R.string.accessibility_hearing_device_pairing_intro));
        } else if (isAshaProfileSupported) {
            pairingIntroPreference.setTitle(
                    mContext.getString(
                            R.string.accessibility_hearing_device_pairing_asha_only_intro));
        } else if (isHapClientProfileSupported) {
            pairingIntroPreference.setTitle(
                    mContext.getString(
                            R.string.accessibility_hearing_device_pairing_hap_only_intro));
        } else {
            // Intentionally blank, getAvailabilityStatus() should handle visibility for
            // none-supported case.
        }
    }
}
