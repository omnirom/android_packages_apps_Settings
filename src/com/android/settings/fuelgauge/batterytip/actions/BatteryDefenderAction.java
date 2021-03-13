/*
 * Copyright (C) 2020 The Android Open Source Project
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

package com.android.settings.fuelgauge.batterytip.actions;

import android.content.Intent;

import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settingslib.HelpUtils;

/**
 * Action to open the Support Center article
 */
public class BatteryDefenderAction extends BatteryTipAction {
    private SettingsActivity mSettingsActivity;

    public BatteryDefenderAction(SettingsActivity settingsActivity) {
        super(settingsActivity.getApplicationContext());
        mSettingsActivity = settingsActivity;
    }

    /**
     * Handle the action when user clicks positive button
     */
    @Override
    public void handlePositiveAction(int metricsKey) {
        final Intent intent = HelpUtils.getHelpIntent(
                mContext,
                mContext.getString(R.string.help_url_battery_defender),
                getClass().getName());
        if (intent != null) {
            mSettingsActivity.startActivityForResult(intent, 0);
        }
        // TODO(b/173985153): Add logging enums for Battery Defender.
    }
}
