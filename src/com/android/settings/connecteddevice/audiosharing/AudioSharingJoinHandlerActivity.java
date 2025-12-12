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

import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.Log;

import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settingslib.bluetooth.BluetoothUtils;
import com.android.settingslib.widget.SettingsThemeHelper;

public class AudioSharingJoinHandlerActivity extends SettingsActivity {
    private static final String TAG = "AudioSharingJoinHandlerActivity";

    @Override
    protected void onCreate(Bundle savedState) {
        super.onCreate(savedState);
        if (!BluetoothUtils.isAudioSharingUIAvailable(this)) {
            finish();
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (!BluetoothUtils.isAudioSharingUIAvailable(this)) {
            finish();
        }
        if (intent != null) {
            Log.d(TAG, "onNewIntent = " + intent);
            getSupportFragmentManager().getFragments().stream().filter(
                            frag -> frag instanceof AudioSharingJoinHandlerDashboardFragment)
                    .findFirst().ifPresent(
                            frag -> ((AudioSharingJoinHandlerDashboardFragment) frag)
                                    .handleDeviceConnectedFromIntent(intent));
        }
    }

    @Override
    public Resources.Theme getTheme() {
        var theme = super.getTheme();
        theme.applyStyle(
                SettingsThemeHelper.isExpressiveTheme(this)
                        ? R.style.Transparent_Expressive : R.style.Transparent, true);
        return theme;
    }

    @Override
    protected boolean isToolbarEnabled() {
        return false;
    }

    @Override
    protected boolean isValidFragment(String fragmentName) {
        return AudioSharingJoinHandlerDashboardFragment.class.getName().equals(fragmentName);
    }
}
