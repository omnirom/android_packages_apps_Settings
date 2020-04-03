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

package com.android.settings.development;

import android.content.Context;
import android.os.SystemProperties;

import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;
import androidx.preference.SwitchPreference;

import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.development.DeveloperOptionsPreferenceController;

public class UsbDiagPreferenceController extends
        DeveloperOptionsPreferenceController implements Preference.OnPreferenceChangeListener,
        PreferenceControllerMixin {

    private static final String USB_ENABLE_DIAGNOSTICS_KEY =
            "usb_enable_diagnostics";
    @VisibleForTesting
    static final String USB_DIAGNOSTICS_PROPERTY =
            "sys.usb.config";

    public UsbDiagPreferenceController(Context context) {
        super(context);
    }

    @Override
    public String getPreferenceKey() {
        return USB_ENABLE_DIAGNOSTICS_KEY;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        final boolean isEnabled = (boolean) newValue;
        SystemProperties.set(USB_DIAGNOSTICS_PROPERTY,
               isEnabled ? "diag,serial_cdev,rmnet,adb" : "");
        return true;
    }

    @Override
    public void updateState(Preference preference) {
        final String propVal = SystemProperties.get(USB_DIAGNOSTICS_PROPERTY);
        final boolean isEnabled = Boolean.valueOf(
                propVal.equals("diag,serial_cdev,rmnet,adb"));
        ((SwitchPreference) mPreference).setChecked(isEnabled);
    }

    @Override
    protected void onDeveloperOptionsSwitchDisabled() {
        super.onDeveloperOptionsSwitchDisabled();
        SystemProperties.set(USB_DIAGNOSTICS_PROPERTY, "adb");
        ((SwitchPreference) mPreference).setChecked(false);
    }
}
