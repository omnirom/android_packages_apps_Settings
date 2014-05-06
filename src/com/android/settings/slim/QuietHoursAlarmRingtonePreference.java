/*
 * Copyright (C) 2007 The Android Open Source Project
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

package com.android.settings.slim;

import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.preference.RingtonePreference;
import android.util.AttributeSet;
import android.util.Log;
import android.provider.Settings;
import android.content.ContentResolver;

public class QuietHoursAlarmRingtonePreference extends RingtonePreference {
    private static final String TAG = "QuietHoursAlarmRingtonePreference";

    public QuietHoursAlarmRingtonePreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected Uri onRestoreRingtone() {
        return returnUserRingtone();
    }

    private Uri returnUserRingtone() {
        String ringtoneString = Settings.System.getString(getContext().getContentResolver(),
                Settings.System.QUIET_HOURS_ALARM_TONE);
        if (ringtoneString == null) {
            // Value not set, defaults to Default Ringtone
            Uri alertSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
            return alertSoundUri;
        } else {
            Uri ringtoneUri = Uri.parse(ringtoneString);
            return ringtoneUri;
        }
    }
}
