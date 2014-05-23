/*
 * Copyright (C) 2013 Android Open Kang Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings.slim.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.android.internal.util.slim.QuietHoursHelper;

public class AlarmReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        Log.d("AlarmReceiver", "onReceive action = " + action);
        if (action.equals(SmsCallController.QUIET_HOURS_STOP_SMSCALL_COMMAND)){
            SmsCallController.getInstance(context).stopSmsCallService();
        } else if (action.equals(SmsCallController.QUIET_HOURS_START_COMMAND)){
            SmsCallController.getInstance(context).startQuietHours();
        } else if (action.equals(SmsCallController.QUIET_HOURS_STOP_COMMAND)){
            SmsCallController.getInstance(context).stopQuietHours();
        } else if (action.equals(QuietHoursHelper.QUIET_HOURS_SCHEDULE_COMMAND)){
            SmsCallController.getInstance(context).scheduleService();
        } else if (action.equals(QuietHoursHelper.QUIET_HOURS_PAUSE_COMMAND)){
            SmsCallController.getInstance(context).pauseQuietHours();
        } else if (action.equals(QuietHoursHelper.QUIET_HOURS_RESUME_COMMAND)){
            SmsCallController.getInstance(context).resumeQuietHours();
        } else if (action.equals(QuietHoursHelper.QUIET_HOURS_INIT_COMMAND)){
            SmsCallController.getInstance(context).init();
        }
    }
}
