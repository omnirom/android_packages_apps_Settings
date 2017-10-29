/*
 * Copyright (C) 2017 The Android Open Source Project
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
package com.android.settings.deviceinfo;

import android.app.Fragment;
import android.content.Context;
import android.os.Build;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import android.text.TextUtils;

import com.android.settings.core.PreferenceController;
import com.android.settingslib.DeviceInfoUtils;

public class DeviceModelPreferenceController extends PreferenceController {

    private static final String KEY_DEVICE_MODEL = "device_model";

    private final Fragment mHost;

    public DeviceModelPreferenceController(Context context, Fragment host) {
        super(context);
        mHost = host;
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        final Preference pref = screen.findPreference(KEY_DEVICE_MODEL);
        if (pref != null) {
            pref.setSummary(getDeviceModel());
        }
    }

    @Override
    public String getPreferenceKey() {
        return KEY_DEVICE_MODEL;
    }

    @Override
    public boolean handlePreferenceTreeClick(Preference preference) {
        if (!TextUtils.equals(preference.getKey(), KEY_DEVICE_MODEL)) {
            return false;
        }
        //final HardwareInfoDialogFragment fragment = HardwareInfoDialogFragment.newInstance();
        //fragment.show(mHost.getFragmentManager(), HardwareInfoDialogFragment.TAG);
        return true;
    }

    public static String getDeviceModel() {
        return Build.MODEL + DeviceInfoUtils.getMsvSuffix();
    }
}
