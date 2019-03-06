/*
 * Copyright (C) 2016 The Android Open Source Project
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

package com.android.settings.notification;

import android.content.Context;
import android.media.RingtoneManager;

import android.telephony.TelephonyManager;

import androidx.preference.PreferenceScreen;

import com.android.settings.DefaultRingtonePreference;
import com.android.settings.R;

import com.android.settings.Utils;

public class PhoneRingtonePreferenceController extends RingtonePreferenceControllerBase {

    private static final int SLOT_ID = 0;
    private static final String KEY_PHONE_RINGTONE = "phone_ringtone";

    public PhoneRingtonePreferenceController(Context context) {
        super(context);
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);

        TelephonyManager telephonyManager =
                (TelephonyManager) mContext.getSystemService(Context.TELEPHONY_SERVICE);
        if (telephonyManager.isMultiSimEnabled()) {
            // For Multi SIM device, shoud show "Phone ringtone - SIM 1" for slot1 ringtone setting.
            DefaultRingtonePreference ringtonePreference =
                    (DefaultRingtonePreference) screen.findPreference(KEY_PHONE_RINGTONE);
            ringtonePreference.setTitle(mContext.getString(R.string.ringtone1_title));
            ringtonePreference.setEnabled(hasCard());
        }
    }

    @Override
    public String getPreferenceKey() {
        return KEY_PHONE_RINGTONE;
    }

    @Override
    public boolean isAvailable() {
        return Utils.isVoiceCapable(mContext);
    }

    @Override
    public int getRingtoneType() {
        return RingtoneManager.TYPE_RINGTONE;
    }

    private boolean hasCard() {
        TelephonyManager telephonyManager =
                (TelephonyManager) mContext.getSystemService(Context.TELEPHONY_SERVICE);
        return telephonyManager.hasIccCard(SLOT_ID);
    }
}
