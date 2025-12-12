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

package com.android.settings.connecteddevice.audiosharing;

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import com.android.settings.R;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settingslib.utils.ThreadUtils;

public class AudioSharingJoinHandlerDashboardFragment extends DashboardFragment {
    private static final String TAG = "AudioSharingJoinHandlerFrag";

    @Nullable private AudioSharingJoinHandlerController mController;

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.AUDIO_SHARING_JOIN_HANDLER;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.bluetooth_le_audio_sharing_join_handler;
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mController = use(AudioSharingJoinHandlerController.class);
        if (mController != null) {
            Log.d(TAG, "onAttach, init controller");
            mController.init(this);
        }
    }

    /** Handle just connected device via intent. */
    public void handleDeviceConnectedFromIntent(@NonNull Intent intent) {
        var unused =
                ThreadUtils.postOnBackgroundThread(
                        () -> {
                            if (mController != null) {
                                Log.d(TAG, "handleDeviceConnectedFromIntent");
                                mController.handleDeviceConnectedFromIntent(intent);
                            }
                        });
    }

    /** Test only: set mock controllers for the {@link AudioSharingJoinHandlerDashboardFragment} */
    @VisibleForTesting
    void setController(AudioSharingJoinHandlerController controller) {
        mController = controller;
    }
}
