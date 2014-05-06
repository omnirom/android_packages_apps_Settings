/*
 *  Copyright (C) 2014 The OmniROM Project
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package com.android.settings.slim.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.provider.Settings;
import android.os.UserHandle;
import android.util.Log;

public class QuietHoursBootReceiver extends BroadcastReceiver {
    private final static boolean DEBUG = true;
    private final static String TAG = "QuietHoursBootReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (DEBUG) {
            Log.i(TAG, "onReceive action = " + action);
        }
        Settings.System.putIntForUser(context.getContentResolver(),
                Settings.System.QUIET_HOURS_ACTIVE, 0, UserHandle.USER_CURRENT);
        QuietHoursController.getInstance(context).init();
    }
}
