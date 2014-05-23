/*
 * Copyright (C) 2014 SlimRoms project
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

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.NotificationManager;
import android.app.Notification;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.database.Cursor;
import android.media.RingtoneManager;
import android.os.Handler;
import android.net.Uri;
import android.os.UserHandle;
import android.preference.PreferenceManager;
import android.provider.ContactsContract.PhoneLookup;
import android.provider.Settings;
import android.telephony.SmsManager;
import android.util.Log;

import com.android.internal.util.slim.QuietHoursHelper;

import java.util.Calendar;
import java.text.DateFormat;

import com.android.settings.R;

public class SmsCallController {

    private final static String TAG = "SmsCallController";

    private static final String KEY_AUTO_SMS = "auto_sms";
    private static final String KEY_AUTO_SMS_CALL = "auto_sms_call";
    private static final String KEY_AUTO_SMS_MESSAGE = "auto_sms_message";
    private static final String KEY_LOOP_BYPASS_RINGTONE = "loop_bypass_ringtone";
    private static final String KEY_BYPASS_RINGTONE = "bypass_ringtone";
    private static final String KEY_CALL_BYPASS = "call_bypass";
    private static final String KEY_SMS_BYPASS = "sms_bypass";
    private static final String KEY_REQUIRED_CALLS = "required_calls";
    private static final String KEY_SMS_BYPASS_CODE = "sms_bypass_code";
    private static final String KEY_QUIET_HOURS_NOTIFICATION = "quiet_hours_enable_notification";
    private static final String KEY_SNOOZE_TIME = "snooze_time";

    public static final String SCHEDULE_SMSCALL_SERVICE_COMMAND =
            "com.android.settings.slim.service.SCHEDULE_SMSCALL_SERVICE_COMMAND";
    public static final String START_NOTIFICATION_COMMAND =
            "com.android.settings.slim.service.START_NOTIFICATION_COMMAND";
    public static final String STOP_NOTIFICATION_COMMAND =
            "com.android.settings.slim.service.STOP_NOTIFICATION_COMMAND";

    private static final String NOTIFICATION_PAUSE_ID = "pause";
    private static final String NOTIFICATION_RESUME_ID = "resume";
    private static final String NOTIFICATION_STOP_ID = "stop";
    private static final String NOTIFICATION_SNOOZE_ID = "snooze";
    private static final String NOTIFICATION_RESUME_SNOOZE_ID = "resume_snooze";

    private static final int FULL_DAY = 1440; // 1440 minutes in a day
    private static final int TIME_LIMIT = 30; // 30 minute bypass limit
    public static final int DEFAULT_DISABLED = 0;
    public static final int ALL_NUMBERS = 1;
    public static final int CONTACTS_ONLY = 2;
    public static final int STARRED_ONLY = 3;
    public static final int DEFAULT_TWO = 2;
    private static final int NOTIFICATION_ID = 5253;

    private Context mContext;
    private SharedPreferences mSharedPrefs;
    private OnSharedPreferenceChangeListener mSharedPrefsObserver;
    private static AlarmManager mAlarmManager;
    private NotificationManager mNotificationManager;

    private Intent mServiceTriggerIntent;
    private PendingIntent mStartIntent;
    private PendingIntent mStopIntent;

    private PendingIntent mStartNotificationIntent;
    private PendingIntent mStopNotificationIntent;

    private boolean mQuietHoursEnabled;
    private int mQuietHoursStart;
    private int mQuietHoursEnd;

    private int mSmsBypass;
    private int mCallBypass;
    private int mAutoCall;
    private int mAutoText;
    private boolean mForceStarted;
    private boolean mPaused;

    private static boolean mSnoozed;
    private static int mSnoozeTime = 10;
    private static PendingIntent mResumeSnoozeIntent;

    private Handler mHandler = new Handler();

    public static class NotificationIntentReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            Log.d("NotificationIntentReceiver", "onReceive action = " + action);

            if (action.equals(NOTIFICATION_STOP_ID)){
                mSnoozed = false;
                if (mResumeSnoozeIntent != null){
                    mAlarmManager.cancel(mResumeSnoozeIntent);
                    mResumeSnoozeIntent = null;
                }
                Settings.System.putIntForUser(context.getContentResolver(),
                        Settings.System.QUIET_HOURS_PAUSED, 0, UserHandle.USER_CURRENT);
                Settings.System.putIntForUser(context.getContentResolver(),
                        Settings.System.QUIET_HOURS_FORCED, 0, UserHandle.USER_CURRENT);
            } else if (action.equals(NOTIFICATION_PAUSE_ID)){
                SmsCallController.getInstance(context).pauseQuietHours();
            } else if (action.equals(NOTIFICATION_RESUME_ID) ||
                    action.equals(NOTIFICATION_RESUME_SNOOZE_ID)){
                SmsCallController.getInstance(context).resumeQuietHours();
            } else if (action.equals(NOTIFICATION_SNOOZE_ID)){
                mSnoozed = true;
                mSnoozeTime = SmsCallController.getInstance(context).returnSnoozeTime();
                Log.d(TAG, "start snooze for " + mSnoozeTime);
                Settings.System.putIntForUser(context.getContentResolver(),
                        Settings.System.QUIET_HOURS_PAUSED, 1, UserHandle.USER_CURRENT);

                Intent resume = new Intent(NOTIFICATION_RESUME_SNOOZE_ID, null,
                    context, NotificationIntentReceiver.class);
                mResumeSnoozeIntent = PendingIntent.getBroadcast(context, 0, resume,
                    PendingIntent.FLAG_CANCEL_CURRENT);
                mAlarmManager.set(AlarmManager.RTC_WAKEUP,
                    System.currentTimeMillis() + mSnoozeTime * 60 * 1000,
                    mResumeSnoozeIntent);
            }
        }
    }

    /**
     * Singleton.
     */
    private static SmsCallController sInstance;

    /**
     * Get the instance.
     */
    public static SmsCallController getInstance(Context context) {
        if (sInstance != null) {
            return sInstance;
        } else {
            return sInstance = new SmsCallController(context);
        }
    }

    /**
     * Constructor.
     */
    private SmsCallController(Context context) {
        mContext = context;

        mSharedPrefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        mSharedPrefsObserver =
                new OnSharedPreferenceChangeListener() {
            public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
                if (key.equals(KEY_SMS_BYPASS)
                        || key.equals(KEY_CALL_BYPASS)
                        || key.equals(KEY_AUTO_SMS_CALL)
                        || key.equals(KEY_AUTO_SMS)) {
                    updateSharedPreferences();
                    scheduleService();
                } else if (key.equals(KEY_QUIET_HOURS_NOTIFICATION)) {
                    updateNotification();
                }
            }
        };
        mSharedPrefs.registerOnSharedPreferenceChangeListener(mSharedPrefsObserver);
        updateSharedPreferences();

        mAlarmManager = (AlarmManager) mContext.getSystemService(Context.ALARM_SERVICE);
        mNotificationManager = (NotificationManager)
                mContext.getSystemService(Context.NOTIFICATION_SERVICE);

        mServiceTriggerIntent = new Intent(mContext, SmsCallService.class);
        Intent start = new Intent(mContext, SmsCallService.class);
        mStartIntent = PendingIntent.getService(
                mContext, 0, start, PendingIntent.FLAG_CANCEL_CURRENT);

        Intent startNotification = new Intent(mContext, AlarmReceiver.class);
        startNotification.setAction(START_NOTIFICATION_COMMAND);
        mStartNotificationIntent = PendingIntent.getBroadcast(
                mContext, 1, startNotification, PendingIntent.FLAG_CANCEL_CURRENT);

        Intent stopSmsCallService = new Intent(mContext, AlarmReceiver.class);
        stopSmsCallService.setAction(SCHEDULE_SMSCALL_SERVICE_COMMAND);
        mStopIntent = PendingIntent.getBroadcast(
                mContext, 2, stopSmsCallService, PendingIntent.FLAG_CANCEL_CURRENT);

        Intent stopNotification = new Intent(mContext, AlarmReceiver.class);
        stopNotification.setAction(STOP_NOTIFICATION_COMMAND);
        mStopNotificationIntent= PendingIntent.getBroadcast(
                mContext, 3, stopNotification, PendingIntent.FLAG_CANCEL_CURRENT);

        // Settings observer
        SettingsObserver observer = new SettingsObserver(mHandler);
        observer.observe();
    }

    private void updateSharedPreferences() {
        mSmsBypass = Integer.parseInt(mSharedPrefs.getString(
                KEY_SMS_BYPASS, String.valueOf(DEFAULT_DISABLED)));
        mCallBypass = Integer.parseInt(mSharedPrefs.getString(
                KEY_CALL_BYPASS, String.valueOf(DEFAULT_DISABLED)));
        mAutoCall = Integer.parseInt(mSharedPrefs.getString(
                KEY_AUTO_SMS_CALL, String.valueOf(DEFAULT_DISABLED)));
        mAutoText = Integer.parseInt(mSharedPrefs.getString(
                KEY_AUTO_SMS, String.valueOf(DEFAULT_DISABLED)));
    }

    // Return the current time
    protected int returnTimeInMinutes() {
        Calendar calendar = Calendar.getInstance();
        return calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE);
    }

    // Return current day of month
    protected int returnDayOfMonth() {
        Calendar calendar = Calendar.getInstance();
        return calendar.get(Calendar.DAY_OF_MONTH);
    }

    // Return if last call versus current call less than 30 minute apart
    protected boolean returnTimeConstraintMet(int firstCallTime, int dayOfFirstCall) {
        final int currentMinutes = returnTimeInMinutes();
        final int dayOfMonth = returnDayOfMonth();
        // New Day, start at zero
        if (dayOfMonth != dayOfFirstCall) {
            // Less or Equal to 30 minutes until midnight
            if (firstCallTime >= (FULL_DAY - TIME_LIMIT)) {
                if ((currentMinutes >= 0) && (currentMinutes <= TIME_LIMIT)) {
                    int remainderDayOne = FULL_DAY - firstCallTime;
                    if ((remainderDayOne + currentMinutes) <= TIME_LIMIT) {
                        return true;
                    } else {
                        return false;
                    }
                } else {
                    return false;
                }
            } else {
                // new day and prior call happened with more than
                // 30 minutes remaining in day
                return false;
            }
        } else {
            // Same day - simple subtraction: or you need to get out more
            // and it's been a month since your last call, reboot, or reschedule
            if ((currentMinutes - firstCallTime) <= TIME_LIMIT) {
                return true;
            } else {
                return false;
            }
        }
    }

    /* True: Ringtone loops until alert dismissed
     * False: Ringtone plays only once
     */
    protected boolean returnUserRingtoneLoop() {
        return mSharedPrefs.getBoolean(KEY_LOOP_BYPASS_RINGTONE, true);
    }

    protected boolean returnUserNotification() {
        return mSharedPrefs.getBoolean(KEY_QUIET_HOURS_NOTIFICATION, true);
    }

    /* Returns user-selected alert Ringtone
     * Parsed from saved string or default ringtone
     */
    public Uri returnUserRingtone() {
        String ringtoneString = mSharedPrefs.getString(KEY_BYPASS_RINGTONE, null);
        if (ringtoneString == null) {
            // Value not set, defaults to Default Ringtone
            Uri alertSoundUri = RingtoneManager.getDefaultUri(
                    RingtoneManager.TYPE_RINGTONE);
            return alertSoundUri;
        } else {
            Uri ringtoneUri = Uri.parse(ringtoneString);
            return ringtoneUri;
        }
    }

    // Code sender can deliver to start an alert
    protected String returnUserTextBypassCode() {
        String defaultCode = mContext.getResources().getString(
                R.string.quiet_hours_sms_code_null);
        return mSharedPrefs.getString(KEY_SMS_BYPASS_CODE, defaultCode);
    }

    // Number of missed calls within time constraint to start alert
    protected int returnUserCallBypassCount() {
        return Integer.parseInt(mSharedPrefs.getString(
                KEY_REQUIRED_CALLS, String.valueOf(DEFAULT_TWO)));
    }

    /* Default: Off
     * All Numbers
     * Contacts Only
     */
    protected int returnUserTextBypass() {
        return mSmsBypass;
    }

    /* Default: Off
     * All Numbers
     * Contacts Only
     */
    protected int returnUserCallBypass() {
        return mCallBypass;
    }

    /* Default: Off
     * All Numbers
     * Contacts Only
     */
    protected int returnUserAutoCall() {
        return mAutoCall;
    }

    /* Default: Off
     * All Numbers
     * Contacts Only
     */
    protected int returnUserAutoText() {
        return mAutoText;
    }

    // Code sender can deliver to start an alert
    protected int returnSnoozeTime() {
        return Integer.parseInt(mSharedPrefs.getString(
                KEY_SNOOZE_TIME, String.valueOf("10")));
    }

    // Pull current settings and send message if applicable
    protected void checkSmsQualifiers(String incomingNumber,
            int userAutoSms, boolean isContact) {
        String message = null;
        String defaultSms = mContext.getResources().getString(
                R.string.quiet_hours_auto_sms_null);
        message = mSharedPrefs.getString(KEY_AUTO_SMS_MESSAGE, defaultSms);
        switch (userAutoSms) {
            case ALL_NUMBERS:
                sendAutoReply(message, incomingNumber);
                break;
            case CONTACTS_ONLY:
                if (isContact) {
                    sendAutoReply(message, incomingNumber);
                }
                break;
            case STARRED_ONLY:
                if (isContact && isStarred(incomingNumber)) {
                    sendAutoReply(message, incomingNumber);
                }
                break;
        }
    }

    /* True: Contact
     * False: Not a contact
     */
    protected boolean isContact(String phoneNumber) {
        boolean isContact = false;
        Uri lookupUri = Uri.withAppendedPath(
                PhoneLookup.CONTENT_FILTER_URI, Uri.encode(phoneNumber));
        String[] numberProject = {
                PhoneLookup._ID,
                PhoneLookup.NUMBER,
                PhoneLookup.DISPLAY_NAME };
        Cursor c = mContext.getContentResolver().query(
                lookupUri, numberProject, null, null, null);
        try {
            if (c.moveToFirst()) {
                isContact = true;
            }
        } finally {
            if (c != null) {
               c.close();
            }
        }
        return isContact;
    }

    /* True: Starred contact
     * False: Not starred
     */
    protected boolean isStarred(String phoneNumber) {
        boolean isStarred = false;
        Uri lookupUri = Uri.withAppendedPath(
                PhoneLookup.CONTENT_FILTER_URI, Uri.encode(phoneNumber));
        String[] numberProject = { PhoneLookup.STARRED };
        Cursor c = mContext.getContentResolver().query(
                lookupUri, numberProject, null, null, null);
        try {
            if (c.moveToFirst()) {
                if (c.getInt(c.getColumnIndex(PhoneLookup.STARRED)) == 1) {
                    isStarred = true;
                }
            }
        } finally {
            if (c != null) {
               c.close();
            }
        }
        return isStarred;
    }

    // Returns the contact name or number
    protected String returnContactName(String phoneNumber) {
        String contactName = null;
        Uri lookupUri = Uri.withAppendedPath(
                PhoneLookup.CONTENT_FILTER_URI, Uri.encode(phoneNumber));
        String[] numberProject = { PhoneLookup.DISPLAY_NAME };
        Cursor c = mContext.getContentResolver().query(
            lookupUri, numberProject, null, null, null);

        try {
            if (c.moveToFirst()) {
                contactName = c.getString(c.getColumnIndex(PhoneLookup.DISPLAY_NAME));
            } else {
                // Not in contacts, return number again
                contactName = phoneNumber;
            }
        } finally {
            if (c != null) {
               c.close();
            }
        }

        return contactName;
    }

    // Send the message
    protected void sendAutoReply(String message, String phoneNumber) {
        SmsManager sms = SmsManager.getDefault();
        try {
            sms.sendTextMessage(phoneNumber, null, message, null, null);
        } catch (IllegalArgumentException e) {
        }
    }

    /*
     * Called when:
     * QuietHours Toggled
     * QuietHours TimeChanged
     * AutoSMS Preferences Changed
     * At Boot
     * Time manually adjusted or Timezone Changed
     * AutoSMS service Stopped - Schedule again for next day
     */
    public void scheduleService() {
        scheduleNotification();
        scheduleSmsCallService();
    }

    public void scheduleNotification() {
        mAlarmManager.cancel(mStartNotificationIntent);
        mAlarmManager.cancel(mStopNotificationIntent);

        stopNotification();
        if (!mQuietHoursEnabled) {
            return;
        }

        if (mForceStarted) {
            startNotification();
            return;
        }

        if (mQuietHoursStart == mQuietHoursEnd) {
            // 24 hours, start without stop
            startNotification();
            return;
        }

        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.SECOND, 0);
        final int currentMinutes =
                calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE);

        boolean inQuietHours = false;
        // time from now on (in minutes) when the service start/stop should be scheduled
        int serviceStartMinutes = -1, serviceStopMinutes = -1;

        if (mQuietHoursEnd < mQuietHoursStart) {
            // Starts at night, ends in the morning.
            if (currentMinutes >= mQuietHoursStart) {
                // In QuietHours - quietHoursEnd in new day
                inQuietHours = true;
                serviceStopMinutes = FULL_DAY - currentMinutes + mQuietHoursEnd;
            } else if (currentMinutes < mQuietHoursEnd) {
                // In QuietHours - quietHoursEnd in same day
                inQuietHours = true;
                serviceStopMinutes = mQuietHoursEnd - currentMinutes;
            } else {
                // Out of QuietHours
                // Current time less than quietHoursStart, greater than quietHoursEnd
                inQuietHours = false;
                serviceStartMinutes = mQuietHoursStart - currentMinutes;
                serviceStopMinutes = FULL_DAY - currentMinutes + mQuietHoursEnd;
            }
        } else {
            // Starts in the morning, ends at night.
            if (currentMinutes >= mQuietHoursStart && currentMinutes < mQuietHoursEnd) {
                // In QuietHours
                inQuietHours = true;
                serviceStopMinutes = mQuietHoursEnd - currentMinutes;
            } else {
                // Out of QuietHours
                inQuietHours = false;
                if (currentMinutes < mQuietHoursStart) {
                    serviceStartMinutes = mQuietHoursStart - currentMinutes;
                    serviceStopMinutes = mQuietHoursEnd - currentMinutes;
                } else {
                    // Current Time greater than quietHoursEnd
                    serviceStartMinutes = FULL_DAY - currentMinutes + mQuietHoursStart;
                    serviceStopMinutes = FULL_DAY - currentMinutes + mQuietHoursEnd;
                }
            }
        }

        if (inQuietHours) {
            startNotification();
        } else {
            stopNotification();
        }

        if (serviceStartMinutes >= 0) {
            calendar.add(Calendar.MINUTE, serviceStartMinutes);
            Log.d(TAG, "QuietHours schedule start at " + DateFormat.getDateTimeInstance().format(calendar.getTime()));
            mAlarmManager.set(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), mStartNotificationIntent);
            calendar.add(Calendar.MINUTE, -serviceStartMinutes);
        }

        if (serviceStopMinutes >= 0) {
            calendar.add(Calendar.MINUTE, serviceStopMinutes);
            Log.d(TAG, "QuietHours schedule stop at " + DateFormat.getDateTimeInstance().format(calendar.getTime()));
            mAlarmManager.set(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), mStopNotificationIntent);
            calendar.add(Calendar.MINUTE, -serviceStopMinutes);
        }
    }

    public void scheduleSmsCallService() {
        mAlarmManager.cancel(mStartIntent);
        mAlarmManager.cancel(mStopIntent);

        // always stop to make sure :)
        mContext.stopServiceAsUser(mServiceTriggerIntent,
                android.os.Process.myUserHandle());

        if (!mQuietHoursEnabled
                || (mAutoCall == DEFAULT_DISABLED
                && mAutoText == DEFAULT_DISABLED
                && mCallBypass == DEFAULT_DISABLED
                && mSmsBypass == DEFAULT_DISABLED)) {
            return;
        }

        if (mForceStarted) {
            mContext.startServiceAsUser(mServiceTriggerIntent,
                    android.os.Process.myUserHandle());
            return;
        }

        if (mQuietHoursStart == mQuietHoursEnd) {
            // 24 hours, start without stop
            mContext.startServiceAsUser(mServiceTriggerIntent,
                    android.os.Process.myUserHandle());
            return;
        }

        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.SECOND, 0);
        final int currentMinutes =
                calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE);

        boolean inQuietHours = false;
        // time from now on (in minutes) when the service start/stop should be scheduled
        int serviceStartMinutes = -1, serviceStopMinutes = -1;

        if (mQuietHoursEnd < mQuietHoursStart) {
            // Starts at night, ends in the morning.
            if (currentMinutes >= mQuietHoursStart) {
                // In QuietHours - quietHoursEnd in new day
                inQuietHours = true;
                serviceStopMinutes = FULL_DAY - currentMinutes + mQuietHoursEnd;
            } else if (currentMinutes < mQuietHoursEnd) {
                // In QuietHours - quietHoursEnd in same day
                inQuietHours = true;
                serviceStopMinutes = mQuietHoursEnd - currentMinutes;
            } else {
                // Out of QuietHours
                // Current time less than quietHoursStart, greater than quietHoursEnd
                inQuietHours = false;
                serviceStartMinutes = mQuietHoursStart - currentMinutes;
                serviceStopMinutes = FULL_DAY - currentMinutes + mQuietHoursEnd;
            }
        } else {
            // Starts in the morning, ends at night.
            if (currentMinutes >= mQuietHoursStart && currentMinutes < mQuietHoursEnd) {
                // In QuietHours
                inQuietHours = true;
                serviceStopMinutes = mQuietHoursEnd - currentMinutes;
            } else {
                // Out of QuietHours
                inQuietHours = false;
                if (currentMinutes < mQuietHoursStart) {
                    serviceStartMinutes = mQuietHoursStart - currentMinutes;
                    serviceStopMinutes = mQuietHoursEnd - currentMinutes;
                } else {
                    // Current Time greater than quietHoursEnd
                    serviceStartMinutes = FULL_DAY - currentMinutes + mQuietHoursStart;
                    serviceStopMinutes = FULL_DAY - currentMinutes + mQuietHoursEnd;
                }
            }
        }

        if (inQuietHours) {
            mContext.startServiceAsUser(mServiceTriggerIntent,
                    android.os.Process.myUserHandle());
        } else {
            mContext.stopServiceAsUser(mServiceTriggerIntent,
                    android.os.Process.myUserHandle());
        }

        if (serviceStartMinutes >= 0) {
            // Start service a minute early
            serviceStartMinutes--;
            calendar.add(Calendar.MINUTE, serviceStartMinutes);
            Log.d(TAG, "SmsCallService schedule start at " + DateFormat.getDateTimeInstance().format(calendar.getTime()));
            mAlarmManager.set(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), mStartIntent);
            calendar.add(Calendar.MINUTE, -serviceStartMinutes);
        }

        if (serviceStopMinutes >= 0) {
            // Stop service a minute late
            serviceStopMinutes++;
            calendar.add(Calendar.MINUTE, serviceStopMinutes);
            Log.d(TAG, "SmsCallService schedule stop at " + DateFormat.getDateTimeInstance().format(calendar.getTime()));
            mAlarmManager.set(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), mStopIntent);
            calendar.add(Calendar.MINUTE, -serviceStopMinutes);
        }
    }

    public void stopSmsCallService() {
        scheduleSmsCallService();
    }

    public void stopNotification() {
        Log.d(TAG, "stopNotification");
        if (mPaused){
            mSnoozed = false;
            Settings.System.putIntForUser(mContext.getContentResolver(),
                    Settings.System.QUIET_HOURS_PAUSED, 0, UserHandle.USER_CURRENT);

            if (mResumeSnoozeIntent != null){
                mAlarmManager.cancel(mResumeSnoozeIntent);
                mResumeSnoozeIntent = null;
            }
        }

        Intent intent = new Intent(QuietHoursHelper.QUIET_HOURS_STOP);
        mContext.sendBroadcastAsUser(intent, UserHandle.CURRENT);

        destroyQuietHour();
    }

    public void startNotification() {
        Log.d(TAG, "startNotification");
        if (mPaused){
            mSnoozed = false;
            Settings.System.putIntForUser(mContext.getContentResolver(),
                    Settings.System.QUIET_HOURS_PAUSED, 0, UserHandle.USER_CURRENT);

            if (mResumeSnoozeIntent != null){
                mAlarmManager.cancel(mResumeSnoozeIntent);
                mResumeSnoozeIntent = null;
            }
        }

        Intent intent = new Intent(QuietHoursHelper.QUIET_HOURS_START);
        mContext.sendBroadcastAsUser(intent, UserHandle.CURRENT);

        if (returnUserNotification()){
            notifyQuietHour();
        }
    }

    public void updateNotification() {
        Log.d(TAG, "updateNotification");
        if (returnUserNotification()){
            notifyQuietHour();
        } else {
            destroyQuietHour();
        }
    }

    private void destroyQuietHour() {
        mNotificationManager.cancel(NOTIFICATION_ID);
    }

    public void pauseQuietHours() {
        Log.d(TAG, "pauseQuietHours");
        mSnoozed = false;
        if (mResumeSnoozeIntent != null){
            mAlarmManager.cancel(mResumeSnoozeIntent);
            mResumeSnoozeIntent = null;
        }
        Settings.System.putIntForUser(mContext.getContentResolver(),
                Settings.System.QUIET_HOURS_PAUSED, 1, UserHandle.USER_CURRENT);
    }

    public void resumeQuietHours() {
        Log.d(TAG, "resumeQuietHours");
        if (mSnoozed){
            Log.d(TAG, "stop snooze");
        } else {
            if (mResumeSnoozeIntent != null){
                mAlarmManager.cancel(mResumeSnoozeIntent);
            }
        }
        mSnoozed = false;
        mResumeSnoozeIntent = null;
        Settings.System.putIntForUser(mContext.getContentResolver(),
                Settings.System.QUIET_HOURS_PAUSED, 0, UserHandle.USER_CURRENT);
    }

    private void notifyQuietHour() {
        final Intent pause = new Intent(NOTIFICATION_PAUSE_ID, null, mContext,
                NotificationIntentReceiver.class);
        final Intent resume = new Intent(NOTIFICATION_RESUME_ID, null, mContext,
                NotificationIntentReceiver.class);
        final Intent snooze = new Intent(NOTIFICATION_SNOOZE_ID, null, mContext,
                NotificationIntentReceiver.class);
        final Intent stop = new Intent(NOTIFICATION_STOP_ID, null, mContext,
                NotificationIntentReceiver.class);
        final boolean forceStarted = Settings.System.getIntForUser(mContext.getContentResolver(),
                Settings.System.QUIET_HOURS_FORCED, 0, UserHandle.USER_CURRENT) != 0;

        Intent touch = new Intent(Intent.ACTION_MAIN);
        touch.setClassName("com.android.settings",
                            "com.android.settings.Settings$QuietHoursSettingsActivity");
        PendingIntent pi = PendingIntent.getActivity(mContext, 0, touch, 0);

        Resources r = mContext.getResources();

        String title = r.getString(R.string.quiet_hours_enable_title);
        if (mPaused){
            if (mSnoozed){
                title = r.getString(R.string.quiet_hours_snoozed_title, mSnoozeTime);
            } else {
                title = r.getString(R.string.quiet_hours_paused_title);
            }
        }

        int iconId = R.drawable.ic_qs_quiet_hours_on;
        if (mPaused){
            iconId = R.drawable.ic_qs_quiet_hours_off;
        }

        Notification.Builder b = new Notification.Builder(mContext)
            .setTicker(r.getString(R.string.quiet_hours_enable_title))
            .setContentTitle(title)
            .setContentText(r.getString(R.string.quiet_hours_notification))
            .setSmallIcon(iconId)
            .setWhen(System.currentTimeMillis())
            .setContentIntent(pi)
            .setAutoCancel(false)
            .setOngoing(true);

        if (!mPaused){
            b.addAction(R.drawable.ic_snooze,
                     r.getString(R.string.quiet_hours_notification_snooze),
                     PendingIntent.getBroadcast(mContext, 0, snooze, PendingIntent.FLAG_CANCEL_CURRENT));

            b.addAction(com.android.internal.R.drawable.ic_media_pause,
                     r.getString(R.string.quiet_hours_notification_pause),
                     PendingIntent.getBroadcast(mContext, 0, pause, PendingIntent.FLAG_CANCEL_CURRENT));
        } else {
            b.addAction(com.android.internal.R.drawable.ic_media_play,
                     r.getString(R.string.quiet_hours_notification_resume),
                     PendingIntent.getBroadcast(mContext, 0, resume, PendingIntent.FLAG_CANCEL_CURRENT));
        }
        if (forceStarted){
            b.addAction(R.drawable.ic_item_delete,
                 r.getString(R.string.quiet_hours_notification_stop),
                 PendingIntent.getBroadcast(mContext, 0, stop, PendingIntent.FLAG_CANCEL_CURRENT));
        }

        mNotificationManager.notify(NOTIFICATION_ID, b.build());
    }

    /**
     * Settingsobserver to take care of the user settings.
     */
    private class SettingsObserver extends ContentObserver {
        SettingsObserver(Handler handler) {
            super(handler);
        }

        void observe() {
            ContentResolver resolver = mContext.getContentResolver();
            resolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.QUIET_HOURS_ENABLED),
                    false, this, UserHandle.USER_ALL);
            resolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.QUIET_HOURS_START),
                    false, this, UserHandle.USER_ALL);
            resolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.QUIET_HOURS_END),
                    false, this, UserHandle.USER_ALL);
            resolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.QUIET_HOURS_FORCED),
                    false, this, UserHandle.USER_ALL);
            resolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.QUIET_HOURS_PAUSED),
                    false, this, UserHandle.USER_ALL);

            update(null);
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            super.onChange(selfChange);
            update(uri);
        }

        public void update(Uri uri) {
            mQuietHoursEnabled = Settings.System.getIntForUser(mContext.getContentResolver(),
                    Settings.System.QUIET_HOURS_ENABLED, 0,
                    UserHandle.USER_CURRENT) != 0;
            mQuietHoursStart = Settings.System.getIntForUser(mContext.getContentResolver(),
                    Settings.System.QUIET_HOURS_START, 0,
                    UserHandle.USER_CURRENT);
            mQuietHoursEnd = Settings.System.getIntForUser(mContext.getContentResolver(),
                    Settings.System.QUIET_HOURS_END, 0,
                    UserHandle.USER_CURRENT);
            mForceStarted = Settings.System.getIntForUser(mContext.getContentResolver(),
                    Settings.System.QUIET_HOURS_FORCED, 0,
                    UserHandle.USER_CURRENT) != 0;
            mPaused = Settings.System.getIntForUser(mContext.getContentResolver(),
                    Settings.System.QUIET_HOURS_PAUSED, 0,
                    UserHandle.USER_CURRENT) != 0;

            if (uri != null && uri.equals(Settings.System.getUriFor(Settings.System.QUIET_HOURS_PAUSED))){
                updateNotification();
            } else {
                scheduleService();
            }
        }
    }
}
