/*
 * Copyright (C) 2021 The Android Open Source Project
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

package com.android.settings;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Process;
import android.os.UserHandle;
import android.os.UserManager;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.android.settings.Settings.TestingSettingsActivity;


public class TestingSettingsBroadcastReceiver extends BroadcastReceiver {
    private final static String TAG = "TestingSettingsBroadcastReceiver";

    public TestingSettingsBroadcastReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent != null && intent.getAction() != null
                && intent.getAction().equals(TelephonyManager.ACTION_SECRET_CODE)
                && !isDisabled(context)) {
            UserManager userManager = context.getSystemService(UserManager.class);
            UserHandle currentUser = Process.myUserHandle();
            if (userManager != null) {
                if (userManager.getUserInfo(currentUser.hashCode()).isMain()) {
                    Intent i = new Intent(Intent.ACTION_MAIN);
                    i.setClass(context, TestingSettingsActivity.class);
                    i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(i);
                } else {
                    Log.d(TAG, "Not main user, not starting TestingSettingsActivity.");
                }
            }
        }
    }

    private boolean isDisabled(Context context) {
        return "user".equals(Build.TYPE) && context.getResources().getBoolean(
                R.bool.config_hide_testing_settings_menu_for_user_builds);
    }
}
