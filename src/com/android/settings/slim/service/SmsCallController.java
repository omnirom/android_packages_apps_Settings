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
import android.media.RingtoneManager;
import android.os.Handler;
import android.net.Uri;
import android.os.UserHandle;
import android.preference.PreferenceManager;
import android.provider.Settings;
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
    private static final String KEY_BYPASS_RINGTONE = "bypass_ringtone";
    private static final String KEY_CALL_BYPASS = "call_bypass";
    private static final String KEY_SMS_BYPASS = "sms_bypass";
    private static final String KEY_REQUIRED_CALLS = "required_calls";
    private static final String KEY_SMS_BYPASS_CODE = "sms_bypass_code";
    private static final String KEY_QUIET_HOURS_NOTIFICATION = "quiet_hours_enable_notification";
    private static final String KEY_SNOOZE_TIME = "snooze_time";

    public static final String QUIET_HOURS_START_COMMAND =
            "com.android.settings.slim.service.QUIET_HOURS_START_COMMAND";
    public static final String QUIET_HOURS_STOP_COMMAND =
            "com.android.settings.slim.service.QUIET_HOURS_STOP_COMMAND";

    private static final String NOTIFICATION_PAUSE_ID = "pause";
    private static final String NOTIFICATION_RESUME_ID = "resume";
    private static final String NOTIFICATION_STOP_ID = "stop";
    private static final String NOTIFICATION_SNOOZE_ID = "snooze";
    private static final String NOTIFICATION_RESUME_SNOOZE_ID = "resume_snooze";

    private static final int FULL_DAY = 1440; // 1440 minutes in a day
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
    private SettingsObserver mSettingsObserver;

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
    private boolean mInQuietHours;

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

        Log.d(TAG, "SmsCallController");
        mSharedPrefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        mSharedPrefsObserver =
                new OnSharedPreferenceChangeListener() {
            public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
                if (key.equals(KEY_QUIET_HOURS_NOTIFICATION)) {
                    updateNotification();
                }
            }
        };
        mSharedPrefs.registerOnSharedPreferenceChangeListener(mSharedPrefsObserver);

        mAlarmManager = (AlarmManager) mContext.getSystemService(Context.ALARM_SERVICE);
        mNotificationManager = (NotificationManager)
                mContext.getSystemService(Context.NOTIFICATION_SERVICE);

        Intent startNotification = new Intent(mContext, AlarmReceiver.class);
        startNotification.setAction(QUIET_HOURS_START_COMMAND);
        mStartNotificationIntent = PendingIntent.getBroadcast(
                mContext, 1, startNotification, PendingIntent.FLAG_CANCEL_CURRENT);

        Intent stopNotification = new Intent(mContext, AlarmReceiver.class);
        stopNotification.setAction(QUIET_HOURS_STOP_COMMAND);
        mStopNotificationIntent= PendingIntent.getBroadcast(
                mContext, 2, stopNotification, PendingIntent.FLAG_CANCEL_CURRENT);

        // Settings observer
        mSettingsObserver = new SettingsObserver(mHandler);
        mSettingsObserver.observe();
    }

    public void init() {
        // dummy
    }

    protected boolean returnUserNotification() {
        return mSharedPrefs.getBoolean(KEY_QUIET_HOURS_NOTIFICATION, true);
    }

    // Code sender can deliver to start an alert
    protected int returnSnoozeTime() {
        return Integer.parseInt(mSharedPrefs.getString(
                KEY_SNOOZE_TIME, String.valueOf("10")));
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
        Log.d(TAG, "scheduleService");
        scheduleNotification();
    }

    public void scheduleNotification() {
        mAlarmManager.cancel(mStartNotificationIntent);
        mAlarmManager.cancel(mStopNotificationIntent);

        stopQuietHours();
        if (!mQuietHoursEnabled) {
            return;
        }

        if (mForceStarted) {
            startQuietHours();
            return;
        }

        if (mQuietHoursStart == mQuietHoursEnd) {
            // 24 hours, start without stop
            startQuietHours();
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
            startQuietHours();
        } else {
            stopQuietHours();
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

    public void stopQuietHours() {
        Log.d(TAG, "stopQuietHours");
        mInQuietHours = false;

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

    public void startQuietHours() {
        Log.d(TAG, "startQuietHours");
        mInQuietHours = true;

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
        if (mInQuietHours){
            Log.d(TAG, "updateNotification");
            if (returnUserNotification()){
                notifyQuietHour();
            } else {
                destroyQuietHour();
            }
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
                    false, this);
            resolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.QUIET_HOURS_START),
                    false, this);
            resolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.QUIET_HOURS_END),
                    false, this);
            resolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.QUIET_HOURS_FORCED),
                    false, this);
            resolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.QUIET_HOURS_PAUSED),
                    false, this);
            resolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.QUIET_HOURS_WHITELIST),
                    false, this, UserHandle.USER_ALL);
            resolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.QUIET_HOURS_CALL_BYPASS),
                    false, this);
            resolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.QUIET_HOURS_SMS_BYPASS),
                    false, this);
            resolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.QUIET_HOURS_AUTO_CALL),
                    false, this);
            resolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.QUIET_HOURS_AUTO_SMS),
                    false, this);

            update(null);
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            super.onChange(selfChange);
            update(uri);
        }

        public void update(Uri uri) {
            Log.d(TAG, "SettingsObserver " + uri);
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
            mSmsBypass = Settings.System.getIntForUser(mContext.getContentResolver(),
                    Settings.System.QUIET_HOURS_SMS_BYPASS, 0,
                    UserHandle.USER_CURRENT);
            mCallBypass = Settings.System.getIntForUser(mContext.getContentResolver(),
                    Settings.System.QUIET_HOURS_CALL_BYPASS, 0,
                    UserHandle.USER_CURRENT);
            mAutoCall = Settings.System.getIntForUser(mContext.getContentResolver(),
                    Settings.System.QUIET_HOURS_AUTO_CALL, 0,
                    UserHandle.USER_CURRENT);
            mAutoText = Settings.System.getIntForUser(mContext.getContentResolver(),
                    Settings.System.QUIET_HOURS_AUTO_SMS, 0,
                    UserHandle.USER_CURRENT);

            if (uri != null && uri.equals(Settings.System.getUriFor(Settings.System.QUIET_HOURS_PAUSED))){
                updateNotification();
            } else {
                scheduleService();
            }
        }
    }
}
