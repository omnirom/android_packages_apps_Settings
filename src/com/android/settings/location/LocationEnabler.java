/*
 * Copyright (C) 2013 The CyanogenMod Project
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

package com.android.settings.location;

import android.content.Context;
import android.provider.Settings;
import android.util.Log;
import android.widget.CompoundButton;
import android.widget.Switch;

public class LocationEnabler implements CompoundButton.OnCheckedChangeListener  {
    private static final String TAG = "LocationEnabler";
    private final Context mContext;
    private Switch mSwitch;
    private boolean mStateMachineEvent;

    public LocationEnabler(Context context, Switch onOffSwitch) {
        mContext = context;
        mSwitch = onOffSwitch;
    }

    public void resume() {
        mSwitch.setOnCheckedChangeListener(this);
        setSwitchState();
    }

    public void pause() {
        mSwitch.setOnCheckedChangeListener(null);
    }

    public void setSwitch(Switch onOffSwitch) {
        if (mSwitch == onOffSwitch) {
            return;
        }
        mSwitch.setOnCheckedChangeListener(null);
        mSwitch = onOffSwitch;
        mSwitch.setOnCheckedChangeListener(this);
        setSwitchState();
    }

    private void setSwitchState() {
        int mode = Settings.Secure.getInt(mContext.getContentResolver(),
                Settings.Secure.LOCATION_MODE, Settings.Secure.LOCATION_MODE_OFF);
        mStateMachineEvent = true;
        mSwitch.setChecked(mode != Settings.Secure.LOCATION_MODE_OFF);
        mStateMachineEvent = false;
    }

    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        if (mStateMachineEvent) {
            return;
        }
        // Handle a switch change
        if (LocationSettingsBase.isRestricted(mContext)) {
            // Location toggling disabled by user restriction. Read the current location mode to
            // update the location master switch.
            if (Log.isLoggable(TAG, Log.INFO)) {
                Log.i(TAG, "Restricted user, not setting location mode");
            }
            return;
        }

        int currentMode = Settings.Secure.getInt(mContext.getContentResolver(),
                Settings.Secure.LOCATION_MODE, Settings.Secure.LOCATION_MODE_OFF);
        int lastMode = Settings.Secure.getInt(mContext.getContentResolver(),
                Settings.Secure.LOCATION_LAST_MODE, Settings.Secure.LOCATION_MODE_HIGH_ACCURACY);
        int newMode = isChecked ? lastMode : Settings.Secure.LOCATION_MODE_OFF;
        LocationSettingsBase.sendModeChangingIntent(mContext, currentMode, newMode);
    }

}
