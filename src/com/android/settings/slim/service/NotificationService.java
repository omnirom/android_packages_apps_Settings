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

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.app.AlarmManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.content.ContentResolver;
import android.database.ContentObserver;
import android.os.IBinder;
import android.os.UserHandle;
import android.os.Handler;
import android.provider.Telephony.Sms.Intents;
import android.provider.Settings;
import android.telephony.PhoneStateListener;
import android.telephony.SmsMessage;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.preference.PreferenceManager;

import com.android.internal.util.slim.QuietHoursHelper;

import com.android.settings.R;

public class NotificationService extends Service {

    private final static String TAG = "NotificationService";

    private static final int QUIETHOUR_NOTIFICATION_ID = 5253;

    private Context mContext;

    private static boolean mPaused;
    private static boolean mSnoozed;
    private static int mSnoozeTime = 10;
    private static PendingIntent mResumeSnoozeIntent;
    private static AlarmManager mAlarmManager;
    private static NotificationManager mNotificationManager;
    private static boolean mHandleObserver = true;

    private Handler mHandler = new Handler();
    private SettingsObserver mObserver;
    private SharedPreferences mSharedPrefs;
    private OnSharedPreferenceChangeListener mSharedPrefsObserver;

    private static final String QUIETHOUR_NOTIFICATION_PAUSE_ID = "pause";
    private static final String QUIETHOUR_NOTIFICATION_RESUME_ID = "resume";
    private static final String QUIETHOUR_NOTIFICATION_STOP_ID = "stop";
    private static final String QUIETHOUR_NOTIFICATION_SNOOZE_ID = "snooze";
    private static final String QUIETHOUR_NOTIFICATION_RESUME_SNOOZE_ID = "resume_snooze";

    public static class NotificationReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            mHandleObserver = false;
            Log.d(TAG, "onReceive " + action);
            if (action.equals(QUIETHOUR_NOTIFICATION_STOP_ID)){
                mPaused = false;
                mSnoozed = false;
                if (mResumeSnoozeIntent != null){
                    mAlarmManager.cancel(mResumeSnoozeIntent);
                    mResumeSnoozeIntent = null;
                }
                Settings.System.putInt(context.getContentResolver(), Settings.System.QUIET_HOURS_PAUSED, 0);
                Settings.System.putInt(context.getContentResolver(), Settings.System.QUIET_HOURS_FORCED, 0);
                notifyQuietHour(context);
            } else if (action.equals(QUIETHOUR_NOTIFICATION_PAUSE_ID)){
                mPaused = true;
                mSnoozed = false;
                if (mResumeSnoozeIntent != null){
                    mAlarmManager.cancel(mResumeSnoozeIntent);
                    mResumeSnoozeIntent = null;
                }
                Settings.System.putInt(context.getContentResolver(), Settings.System.QUIET_HOURS_PAUSED, 1);
                notifyQuietHour(context);
            } else if (action.equals(QUIETHOUR_NOTIFICATION_RESUME_ID) ||
                    action.equals(QUIETHOUR_NOTIFICATION_RESUME_SNOOZE_ID)){
                if (mSnoozed){
                    Log.d(TAG, "stop snooze");
                } else {
                    if (mResumeSnoozeIntent != null){
                        mAlarmManager.cancel(mResumeSnoozeIntent);
                    }
                }
                mPaused = false;
                mSnoozed = false;
                mResumeSnoozeIntent = null;
                Settings.System.putInt(context.getContentResolver(), Settings.System.QUIET_HOURS_PAUSED, 0);
                notifyQuietHour(context);
            } else if (action.equals(QUIETHOUR_NOTIFICATION_SNOOZE_ID)){
                mPaused = true;
                mSnoozed = true;
                mSnoozeTime = SmsCallController.getInstance(context).returnSnoozeTime();
                Log.d(TAG, "start snooze for " + mSnoozeTime);
                Settings.System.putInt(context.getContentResolver(), Settings.System.QUIET_HOURS_PAUSED, 1);

                Intent resume = new Intent(QUIETHOUR_NOTIFICATION_RESUME_SNOOZE_ID, null,
                    context, NotificationReceiver.class);
                mResumeSnoozeIntent = PendingIntent.getBroadcast(context, 0, resume,
                    PendingIntent.FLAG_CANCEL_CURRENT);
                mAlarmManager.set(AlarmManager.RTC_WAKEUP,
                    System.currentTimeMillis() + mSnoozeTime * 60 * 1000,
                    mResumeSnoozeIntent);

                notifyQuietHour(context);
            }
        }
    }

    @Override
    public void onCreate() {
        Log.d(TAG, "onCreate");
        super.onCreate();
        mContext = this;
        mAlarmManager = (AlarmManager) this.getSystemService(Context.ALARM_SERVICE);
        mNotificationManager = (NotificationManager)
                this.getSystemService(Context.NOTIFICATION_SERVICE);

        mPaused = false;
        mSnoozed = false;
        mHandleObserver = true;
        Settings.System.putInt(this.getContentResolver(), Settings.System.QUIET_HOURS_PAUSED, 0);

        if (mResumeSnoozeIntent != null){
            mAlarmManager.cancel(mResumeSnoozeIntent);
            mResumeSnoozeIntent = null;
        }

        if (SmsCallController.getInstance(this).returnUserNotification()){
            notifyQuietHour(this);
        }

        mObserver = new SettingsObserver(mHandler);
        mObserver.observe();

        mSharedPrefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        mSharedPrefsObserver =
                new OnSharedPreferenceChangeListener() {
            public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
                if (key.equals(SmsCallController.KEY_QUIET_HOURS_NOTIFICATION)) {
                    if (!SmsCallController.getInstance(mContext).returnUserNotification()){
                        destroyQuietHour(mContext);
                    } else {
                        notifyQuietHour(mContext);
                    }
                }
            }
        };
        mSharedPrefs.registerOnSharedPreferenceChangeListener(mSharedPrefsObserver);

        Intent intent = new Intent(QuietHoursHelper.QUIET_HOURS_START);
        mContext.sendBroadcast(intent);
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy");

        mPaused = false;
        mSnoozed = false;
        mHandleObserver = false;
        Settings.System.putInt(this.getContentResolver(), Settings.System.QUIET_HOURS_PAUSED, 0);

        if (mResumeSnoozeIntent != null){
            mAlarmManager.cancel(mResumeSnoozeIntent);
            mResumeSnoozeIntent = null;
        }

        destroyQuietHour(this);

        mObserver.unObserve();
        mSharedPrefs.unregisterOnSharedPreferenceChangeListener(mSharedPrefsObserver);

        Intent intent = new Intent(QuietHoursHelper.QUIET_HOURS_STOP);
        mContext.sendBroadcast(intent);

        super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        // no body bind to here
        return null;
    }

    private static void destroyQuietHour(Context context) {
        mNotificationManager.cancelAsUser(null, QUIETHOUR_NOTIFICATION_ID, UserHandle.ALL);
    }

    private static void notifyQuietHour(Context context) {
        final Intent pause = new Intent(QUIETHOUR_NOTIFICATION_PAUSE_ID, null, context, NotificationReceiver.class);
        final Intent resume = new Intent(QUIETHOUR_NOTIFICATION_RESUME_ID, null, context, NotificationReceiver.class);
        final Intent snooze = new Intent(QUIETHOUR_NOTIFICATION_SNOOZE_ID, null, context, NotificationReceiver.class);
        final Intent stop = new Intent(QUIETHOUR_NOTIFICATION_STOP_ID, null, context, NotificationReceiver.class);
        final boolean forceStarted = Settings.System.getInt(context.getContentResolver(),
                Settings.System.QUIET_HOURS_FORCED, 0) != 0;

        Intent touch = new Intent(Intent.ACTION_MAIN);
        touch.setClassName("com.android.settings",
                            "com.android.settings.Settings$QuietHoursSettingsActivity");
        PendingIntent pi = PendingIntent.getActivity(context, 0, touch, 0);

        Resources r = context.getResources();

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

        Notification.Builder b = new Notification.Builder(context)
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
                     PendingIntent.getBroadcast(context, 0, snooze, PendingIntent.FLAG_CANCEL_CURRENT));

            b.addAction(com.android.internal.R.drawable.ic_media_pause,
                     r.getString(R.string.quiet_hours_notification_pause),
                     PendingIntent.getBroadcast(context, 0, pause, PendingIntent.FLAG_CANCEL_CURRENT));
        } else {
            b.addAction(com.android.internal.R.drawable.ic_media_play,
                     r.getString(R.string.quiet_hours_notification_resume),
                     PendingIntent.getBroadcast(context, 0, resume, PendingIntent.FLAG_CANCEL_CURRENT));
        }
        if (forceStarted){
            b.addAction(R.drawable.ic_item_delete,
                 r.getString(R.string.quiet_hours_notification_stop),
                 PendingIntent.getBroadcast(context, 0, stop, PendingIntent.FLAG_CANCEL_CURRENT));
        }

        mNotificationManager.notifyAsUser(null, QUIETHOUR_NOTIFICATION_ID, b.build(),
                    UserHandle.ALL);
    }

    private class SettingsObserver extends ContentObserver {
        SettingsObserver(Handler handler) {
            super(handler);
        }

        void observe() {
            ContentResolver resolver = mContext.getContentResolver();
            resolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.QUIET_HOURS_FORCED), false, this);
            resolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.QUIET_HOURS_PAUSED), false, this);
            update();
        }

        void unObserve(){
            mContext.getContentResolver().unregisterContentObserver(this);
        }

        @Override
        public void onChange(boolean selfChange) {
            super.onChange(selfChange);
            if (mHandleObserver){
                update();
            } else {
                mHandleObserver = true;
            }
        }

        public void update() {
            if (!SmsCallController.getInstance(mContext).returnUserNotification()){
                return;
            }

            // for external pause
            final boolean paused = Settings.System.getInt(mContext.getContentResolver(),
                    Settings.System.QUIET_HOURS_PAUSED, 0) != 0;
            if (paused){
                mPaused = true;
                mSnoozed = false;
                if (mResumeSnoozeIntent != null){
                    mAlarmManager.cancel(mResumeSnoozeIntent);
                    mResumeSnoozeIntent = null;
                }
            } else {
                if (mSnoozed){
                    if (mResumeSnoozeIntent != null){
                        mAlarmManager.cancel(mResumeSnoozeIntent);
                    }
                }
                mPaused = false;
                mSnoozed = false;
                mResumeSnoozeIntent = null;
            }
            notifyQuietHour(mContext);
        }
    }
}
