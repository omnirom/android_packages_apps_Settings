/*
 * Copyright (C) 2012 The CyanogenMod Project
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

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.os.UserHandle;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceGroup;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.preference.RingtonePreference;
import android.preference.SwitchPreference;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.text.InputFilter;
import android.text.TextUtils;
import android.widget.EditText;
import android.util.Log;

import com.android.internal.util.slim.QuietHoursHelper;

import com.android.settings.R;
import com.android.settings.slim.service.QuietHoursController;
import com.android.settings.SettingsPreferenceFragment;
import org.omnirom.omnigears.preference.SystemCheckBoxPreference;

public class QuietHours extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener  {

    private static final String TAG = "QuietHours";
    private static final String KEY_QUIET_HOURS_ENABLED = "quiet_hours_enabled";
    private static final CharSequence KEY_QUIET_HOURS_RINGER = "quiet_hours_ringer";
    private static final String KEY_QUIET_HOURS_DIM = "quiet_hours_dim";
    private static final String KEY_QUIET_HOURS_TIMERANGE = "quiet_hours_timerange";
    private static final String KEY_LOOP_BYPASS_RINGTONE = "quiet_hours_alarm_loop";
    private static final String KEY_AUTO_SMS = "auto_sms";
    private static final String KEY_AUTO_SMS_CALL = "auto_sms_call";
    private static final String KEY_AUTO_SMS_MESSAGE = "auto_sms_message";
    private static final String KEY_CALL_BYPASS = "call_bypass";
    private static final String KEY_SMS_BYPASS = "sms_bypass";
    private static final String KEY_REQUIRED_CALLS = "required_calls";
    private static final String KEY_SMS_BYPASS_CODE = "sms_bypass_code";
    private static final String KEY_BYPASS_RINGTONE = "bypass_ringtone";
    private static final String KEY_QUIET_HOURS_NOTIFICATION = "quiet_hours_enable_notification";
    private static final String KEY_SNOOZE_TIME = "snooze_time";
    private static final String KEY_QUIET_HOURS_START = "quiet_hours_start";

    private static final int DLG_AUTO_SMS_MESSAGE = 0;
    private static final int DLG_SMS_BYPASS_CODE = 1;
    private static final int MIN_CALL_NUMBER = 1;

    private SwitchPreference mQuietHoursEnabled;
    private ListPreference mQuietHoursRinger;
    private SystemCheckBoxPreference mQuietHoursDim;
    private SystemCheckBoxPreference mRingtoneLoop;
    private ListPreference mAutoSms;
    private ListPreference mAutoSmsCall;
    private ListPreference mSmsBypass;
    private ListPreference mCallBypass;
    private ListPreference mCallBypassNumber;
    private Preference mSmsBypassCode;
    private Preference mAutoSmsMessage;
    private QuietHoursAlarmRingtonePreference mBypassRingtone;
    private TimeRangePreference mQuietHoursTimeRange;
    private ListPreference mSnoozeTime;
    private SwitchPreference mQuietHoursStart;

    private ContentResolver mResolver;
    private Context mContext;
    private Resources mResources;

    private int mSmsPref;
    private int mCallPref;
    private int mSmsBypassPref;
    private int mCallBypassPref;

    private SharedPreferences mPrefs;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getPreferenceManager() != null) {
            addPreferencesFromResource(R.xml.quiet_hours_settings);

            final boolean isSecondaryUser = UserHandle.myUserId() != UserHandle.USER_OWNER;

            mContext = getActivity().getApplicationContext();
            mResolver = mContext.getContentResolver();
            mResources = mContext.getResources();
            PreferenceScreen prefSet = getPreferenceScreen();

            mPrefs = PreferenceManager.getDefaultSharedPreferences(mContext);
            QuietHoursController.getInstance(mContext);

            // Load the preferences
            mQuietHoursEnabled =
                (SwitchPreference) prefSet.findPreference(KEY_QUIET_HOURS_ENABLED);
            mQuietHoursTimeRange =
                (TimeRangePreference) prefSet.findPreference(KEY_QUIET_HOURS_TIMERANGE);
            mQuietHoursRinger =
                (ListPreference) prefSet.findPreference(KEY_QUIET_HOURS_RINGER);
            mQuietHoursDim =
                (SystemCheckBoxPreference) prefSet.findPreference(KEY_QUIET_HOURS_DIM);
            mRingtoneLoop =
                (SystemCheckBoxPreference) prefSet.findPreference(KEY_LOOP_BYPASS_RINGTONE);
            mAutoSms =
                (ListPreference) prefSet.findPreference(KEY_AUTO_SMS);
            mAutoSmsCall =
                (ListPreference) prefSet.findPreference(KEY_AUTO_SMS_CALL);
            mAutoSmsMessage =
                (Preference) prefSet.findPreference(KEY_AUTO_SMS_MESSAGE);
            mSmsBypass =
                (ListPreference) prefSet.findPreference(KEY_SMS_BYPASS);
            mCallBypass =
                (ListPreference) prefSet.findPreference(KEY_CALL_BYPASS);
            mCallBypassNumber =
                (ListPreference) prefSet.findPreference(KEY_REQUIRED_CALLS);
            mSmsBypassCode =
                (Preference) prefSet.findPreference(KEY_SMS_BYPASS_CODE);
            mBypassRingtone =
                (QuietHoursAlarmRingtonePreference) prefSet.findPreference(KEY_BYPASS_RINGTONE);
            mSnoozeTime =
                (ListPreference) prefSet.findPreference(KEY_SNOOZE_TIME);
            mQuietHoursStart =
                (SwitchPreference) prefSet.findPreference(KEY_QUIET_HOURS_START);

            // Set the preference state and listeners where applicable
            mQuietHoursEnabled.setChecked(
                    Settings.System.getInt(mResolver, Settings.System.QUIET_HOURS_ENABLED, 0) != 0);
            mQuietHoursEnabled.setOnPreferenceChangeListener(this);
            mQuietHoursTimeRange.setTimeRange(
                    Settings.System.getInt(mResolver, Settings.System.QUIET_HOURS_START, 0),
                    Settings.System.getInt(mResolver, Settings.System.QUIET_HOURS_END, 0));
            mQuietHoursTimeRange.setOnPreferenceChangeListener(this);
            mRingtoneLoop.setOnPreferenceChangeListener(this);

            mAutoSms.setValue(String.valueOf(Settings.System.getInt(mResolver, Settings.System.QUIET_HOURS_AUTO_SMS, 0)));
            mAutoSms.setOnPreferenceChangeListener(this);

            mAutoSmsCall.setValue(String.valueOf(Settings.System.getInt(mResolver, Settings.System.QUIET_HOURS_AUTO_CALL, 0)));
            mAutoSmsCall.setOnPreferenceChangeListener(this);

            mSmsBypass.setValue(String.valueOf(Settings.System.getInt(mResolver, Settings.System.QUIET_HOURS_SMS_BYPASS, 0)));
            mSmsBypass.setOnPreferenceChangeListener(this);

            mCallBypass.setValue(String.valueOf(Settings.System.getInt(mResolver, Settings.System.QUIET_HOURS_CALL_BYPASS, 0)));
            mCallBypass.setOnPreferenceChangeListener(this);

            mCallBypassNumber.setValue(String.valueOf(Settings.System.getInt(mResolver, Settings.System.QUIET_HOURS_CALL_BYPASS_COUNT, 2)));
            mCallBypassNumber.setOnPreferenceChangeListener(this);

            mBypassRingtone.setOnPreferenceChangeListener(this);
            mSnoozeTime.setValue(mPrefs.getString(KEY_SNOOZE_TIME, "10"));
            mSnoozeTime.setOnPreferenceChangeListener(this);
            updateStartSwitch();
            mQuietHoursStart.setOnPreferenceChangeListener(this);

            TelephonyManager telephonyManager =
                    (TelephonyManager) mContext.getSystemService(Context.TELEPHONY_SERVICE);
            if (isSecondaryUser || telephonyManager.getPhoneType() == TelephonyManager.PHONE_TYPE_NONE) {
                prefSet.removePreference(mQuietHoursRinger);
                prefSet.removePreference((PreferenceGroup) findPreference("sms_respond"));
                prefSet.removePreference((PreferenceGroup) findPreference("quiethours_bypass"));
            } else {
                int muteType = Settings.System.getInt(mResolver,
                    Settings.System.QUIET_HOURS_RINGER, 0);
                mQuietHoursRinger.setValue(String.valueOf(muteType));
                mQuietHoursRinger.setSummary(mQuietHoursRinger.getEntry());
                mQuietHoursRinger.setOnPreferenceChangeListener(this);
                int callBypassNumber = Settings.System.getInt(mResolver,
                        Settings.System.QUIET_HOURS_CALL_BYPASS_COUNT, 2);
                boolean loopRingtone = Settings.System.getInt(mResolver,
                        Settings.System.QUIET_HOURS_ALARM_LOOP, 0) != 0;
                mSmsBypassPref = Settings.System.getInt(mResolver,
                        Settings.System.QUIET_HOURS_SMS_BYPASS, 0);
                mSmsPref = Settings.System.getInt(mResolver,
                        Settings.System.QUIET_HOURS_AUTO_SMS, 0);
                mCallPref = Settings.System.getInt(mResolver,
                        Settings.System.QUIET_HOURS_AUTO_CALL, 0);
                mCallBypassPref = Settings.System.getInt(mResolver,
                        Settings.System.QUIET_HOURS_CALL_BYPASS, 0);
                Uri alertSoundUri = returnUserRingtone();
                Ringtone ringtoneAlarm = RingtoneManager.getRingtone(mContext, alertSoundUri);
                mBypassRingtone.setSummary(ringtoneAlarm.getTitle(mContext));
                mRingtoneLoop.setChecked(loopRingtone);
                mRingtoneLoop.setSummary(loopRingtone
                        ? R.string.quiet_hours_bypass_ringtone_loop_summary_on
                        : R.string.quiet_hours_bypass_ringtone_loop_summary_off);
                mSmsBypass.setSummary(mSmsBypass.getEntries()[mSmsBypassPref]);
                mCallBypass.setSummary(mCallBypass.getEntries()[mCallBypassPref]);
                mCallBypassNumber.setSummary(mCallBypassNumber.getEntries()[callBypassNumber-MIN_CALL_NUMBER]
                        + mResources.getString(R.string.quiet_hours_calls_required_summary));
                mAutoSms.setSummary(mAutoSms.getEntries()[mSmsPref]);
                mAutoSmsCall.setSummary(mAutoSmsCall.getEntries()[mCallPref]);
                mCallBypassNumber.setEnabled(mCallBypassPref != 0);
                mSmsBypassCode.setEnabled(mSmsBypassPref != 0);

                int idx = mSnoozeTime.findIndexOfValue(mSnoozeTime.getValue());
                mSnoozeTime.setSummary(mSnoozeTime.getEntries()[idx]);

                shouldDisplayTextPref();
                setSmsBypassCodeSummary();
            }

            // Remove the notification light setting if the device does not support it
            if (mQuietHoursDim != null && !mResources.getBoolean(
                        com.android.internal.R.bool.config_intrusiveNotificationLed)) {
                prefSet.removePreference(mQuietHoursDim);
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        updateStartSwitch();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        if (preference == mAutoSmsMessage) {
            showDialogInner(DLG_AUTO_SMS_MESSAGE);
            return true;
        } else if (preference == mSmsBypassCode) {
            showDialogInner(DLG_SMS_BYPASS_CODE);
            return true;
        }
        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mQuietHoursTimeRange) {
            Settings.System.putInt(mResolver, Settings.System.QUIET_HOURS_START,
                    mQuietHoursTimeRange.getStartTime());
            Settings.System.putInt(mResolver, Settings.System.QUIET_HOURS_END,
                    mQuietHoursTimeRange.getEndTime());
            return true;
        } else if (preference == mQuietHoursEnabled) {
            boolean enabled = (Boolean) newValue;
            Settings.System.putInt(mResolver, Settings.System.QUIET_HOURS_ENABLED,
                    enabled ? 1 : 0);
            if (!enabled){
                Settings.System.putInt(mResolver, Settings.System.QUIET_HOURS_FORCED, 0);
            }
            updateStartSwitch();
            return true;
        } else if (preference == mQuietHoursRinger) {
            int ringerMuteType = Integer.valueOf((String) newValue);
            int index = mQuietHoursRinger.findIndexOfValue((String) newValue);
            Settings.System.putInt(mResolver, Settings.System.QUIET_HOURS_RINGER, ringerMuteType);
            mQuietHoursRinger.setSummary(mQuietHoursRinger.getEntries()[index]);
            return true;
        } else if (preference == mRingtoneLoop) {
            mRingtoneLoop.setSummary((Boolean) newValue
                    ? R.string.quiet_hours_bypass_ringtone_loop_summary_on
                    : R.string.quiet_hours_bypass_ringtone_loop_summary_off);
            return true;
        } else if (preference == mAutoSms) {
            mSmsPref = Integer.parseInt((String) newValue);
            mAutoSms.setSummary(mAutoSms.getEntries()[mSmsPref]);
            Settings.System.putInt(mResolver, Settings.System.QUIET_HOURS_AUTO_SMS, mSmsPref);
            shouldDisplayTextPref();
            return true;
        } else if (preference == mAutoSmsCall) {
            mCallPref = Integer.parseInt((String) newValue);
            mAutoSmsCall.setSummary(mAutoSmsCall.getEntries()[mCallPref]);
            Settings.System.putInt(mResolver, Settings.System.QUIET_HOURS_AUTO_CALL, mCallPref);
            shouldDisplayTextPref();
            return true;
        } else if (preference == mSmsBypass) {
            mSmsBypassPref = Integer.parseInt((String) newValue);
            mSmsBypass.setSummary(mSmsBypass.getEntries()[mSmsBypassPref]);
            Settings.System.putInt(mResolver, Settings.System.QUIET_HOURS_SMS_BYPASS, mSmsBypassPref);
            mSmsBypassCode.setEnabled(mSmsBypassPref != 0);
            return true;
        } else if (preference == mCallBypass) {
            mCallBypassPref = Integer.parseInt((String) newValue);
            mCallBypass.setSummary(mCallBypass.getEntries()[mCallBypassPref]);
            Settings.System.putInt(mResolver, Settings.System.QUIET_HOURS_CALL_BYPASS, mCallBypassPref);
            mCallBypassNumber.setEnabled(mCallBypassPref != 0);
            return true;
        } else if (preference == mCallBypassNumber) {
            int val = Integer.parseInt((String) newValue);
            mCallBypassNumber.setSummary(mCallBypassNumber.getEntries()[val-MIN_CALL_NUMBER]
                    + mResources.getString(R.string.quiet_hours_calls_required_summary));
            Settings.System.putInt(mResolver, Settings.System.QUIET_HOURS_CALL_BYPASS_COUNT, val);
            return true;
        } else if (preference == mBypassRingtone) {
            Uri val = Uri.parse((String) newValue);
            Ringtone ringtone = RingtoneManager.getRingtone(mContext, val);
            if (ringtone != null) {
                Settings.System.putString(mResolver, Settings.System.QUIET_HOURS_ALARM_TONE, val.toString());
                mBypassRingtone.setSummary(ringtone.getTitle(mContext));
            } else {
                // No silent option, won't reach here
                Settings.System.putString(mResolver, Settings.System.QUIET_HOURS_ALARM_TONE, null);
            }
            return true;
        } else if (preference == mSnoozeTime){
            int idx = mSnoozeTime.findIndexOfValue((String) newValue);
            mSnoozeTime.setSummary(mSnoozeTime.getEntries()[idx]);
            return true;
        } else if (preference == mQuietHoursStart){
            boolean start = (Boolean) newValue;
            Settings.System.putInt(mResolver, Settings.System.QUIET_HOURS_FORCED, start ? 1 : 0);
            mQuietHoursTimeRange.setEnabled(!start);
            return true;
        }
        return false;
    }

    private void shouldDisplayTextPref() {
        mAutoSmsMessage.setEnabled(mSmsPref != 0 || mCallPref != 0);
    }

    private void setSmsBypassCodeSummary() {
        final String defaultCode = mResources.getString(R.string.quiet_hours_sms_code_null);
        String code = Settings.System.getString(mResolver,
                Settings.System.QUIET_HOURS_SMS_BYPASS_CODE);
        if (code == null){
            code = defaultCode;
        }
        mSmsBypassCode.setSummary(code);
    }

    private void showDialogInner(int id) {
        DialogFragment newFragment = MyAlertDialogFragment.newInstance(id);
        newFragment.setTargetFragment(this, 0);
        newFragment.show(getFragmentManager(), "dialog " + id);
    }

    public static class MyAlertDialogFragment extends DialogFragment {

        public static MyAlertDialogFragment newInstance(int id) {
            MyAlertDialogFragment frag = new MyAlertDialogFragment();
            Bundle args = new Bundle();
            args.putInt("id", id);
            frag.setArguments(args);
            return frag;
        }

        QuietHours getOwner() {
            return (QuietHours) getTargetFragment();
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            int id = getArguments().getInt("id");
            switch (id) {
                case DLG_AUTO_SMS_MESSAGE:
                    final String defaultText =
                        getActivity().getResources().getString(R.string.quiet_hours_auto_sms_null);
                    String autoText = Settings.System.getString(getOwner().mResolver,
                        Settings.System.QUIET_HOURS_AUTO_SMS_TEXT);
                    if (autoText == null){
                        autoText = defaultText;
                    }

                    final EditText input = new EditText(getActivity());
                    InputFilter[] filter = new InputFilter[1];
                    // No multi/split messages for ease of compatibility
                    filter[0] = new InputFilter.LengthFilter(160);
                    input.append(autoText);
                    input.setFilters(filter);

                    return new AlertDialog.Builder(getActivity())
                    .setTitle(R.string.quiet_hours_auto_string_title)
                    .setMessage(R.string.quiet_hours_auto_string_explain)
                    .setView(input)
                    .setNegativeButton(R.string.cancel, null)
                    .setPositiveButton(R.string.dlg_ok,
                        new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            String value = input.getText().toString();
                            if (TextUtils.isEmpty(value)) {
                                value = defaultText;
                            }
                            Settings.System.putString(getOwner().mResolver,
                                    Settings.System.QUIET_HOURS_AUTO_SMS_TEXT, value);
                        }
                    })
                    .create();
                case DLG_SMS_BYPASS_CODE:
                    final String defaultCode =
                        getActivity().getResources().getString(R.string.quiet_hours_sms_code_null);
                    String code = Settings.System.getString(getOwner().mResolver,
                        Settings.System.QUIET_HOURS_SMS_BYPASS_CODE);
                    if (code == null){
                        code = defaultCode;
                    }

                    final EditText inputCode = new EditText(getActivity());
                    InputFilter[] filterCode = new InputFilter[1];
                    filterCode[0] = new InputFilter.LengthFilter(20);
                    inputCode.append(code);
                    inputCode.setFilters(filterCode);

                    return new AlertDialog.Builder(getActivity())
                    .setTitle(R.string.quiet_hours_sms_code_title)
                    .setMessage(R.string.quiet_hours_sms_code_explain)
                    .setView(inputCode)
                    .setNegativeButton(R.string.cancel, null)
                    .setPositiveButton(R.string.dlg_ok,
                        new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            String value = inputCode.getText().toString();
                            // allow empty string
                            //if (TextUtils.isEmpty(value)) {
                            //    value = defaultCode;
                            //}
                            Settings.System.putString(getOwner().mResolver,
                                    Settings.System.QUIET_HOURS_SMS_BYPASS_CODE, value);   
                            getOwner().setSmsBypassCodeSummary();
                        }
                    })
                    .create();
            }
            throw new IllegalArgumentException("unknown id " + id);
        }

        @Override
        public void onCancel(DialogInterface dialog) {

        }
    }

    private void updateStartSwitch(){
        final boolean forceStarted = Settings.System.getInt(mResolver,
                Settings.System.QUIET_HOURS_FORCED, 0) != 0;
        mQuietHoursStart.setChecked(forceStarted);
        mQuietHoursTimeRange.setEnabled(!forceStarted);
    }

    private Uri returnUserRingtone() {
        String ringtoneString = Settings.System.getString(mResolver, Settings.System.QUIET_HOURS_ALARM_TONE);
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
}
