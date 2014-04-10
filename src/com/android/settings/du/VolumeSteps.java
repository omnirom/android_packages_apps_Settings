/*
 * Copyright (C) 2014 Dirty Unicorns
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

package com.android.settings.du;

import android.content.ContentResolver;
import android.content.Context;
import android.media.AudioManager;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;

public class VolumeSteps extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener  {

    private static final String TAG = "VolumeSteps";
    private static final String KEY_VOLUME_STEPS_ALARM = "volume_steps_alarm";
    private static final String KEY_VOLUME_STEPS_DTMF = "volume_steps_dtmf";
    private static final String KEY_VOLUME_STEPS_MUSIC = "volume_steps_music";
    private static final String KEY_VOLUME_STEPS_NOTIFICATION = "volume_steps_notification";
    private static final String KEY_VOLUME_STEPS_RING = "volume_steps_ring";
    private static final String KEY_VOLUME_STEPS_SYSTEM = "volume_steps_system";
    private static final String KEY_VOLUME_STEPS_VOICE_CALL = "volume_steps_voice_call";

    private ListPreference mVolumeStepsAlarm;
    private ListPreference mVolumeStepsDTMF;
    private ListPreference mVolumeStepsMusic;
    private ListPreference mVolumeStepsNotification;
    private ListPreference mVolumeStepsRing;
    private ListPreference mVolumeStepsSystem;
    private ListPreference mVolumeStepsVoiceCall;

    private AudioManager mAudioManager;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getPreferenceManager() != null) {
            addPreferencesFromResource(R.xml.volume_steps_settings);

            PreferenceScreen prefSet = getPreferenceScreen();

            int activePhoneType = TelephonyManager.getDefault().getCurrentPhoneType();

            mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

            boolean isPhone = activePhoneType != TelephonyManager.PHONE_TYPE_NONE;
            PreferenceCategory audioCat = (PreferenceCategory) getPreferenceScreen().findPreference("category_volume");

            // Load the preferences
            mVolumeStepsAlarm =
                (ListPreference) prefSet.findPreference(KEY_VOLUME_STEPS_ALARM);
            mVolumeStepsDTMF =
                (ListPreference) prefSet.findPreference(KEY_VOLUME_STEPS_DTMF);
            mVolumeStepsMusic =
                (ListPreference) prefSet.findPreference(KEY_VOLUME_STEPS_MUSIC);
            mVolumeStepsNotification =
                (ListPreference) prefSet.findPreference(KEY_VOLUME_STEPS_NOTIFICATION);
            mVolumeStepsRing =
                (ListPreference) prefSet.findPreference(KEY_VOLUME_STEPS_RING);
            mVolumeStepsSystem =
                (ListPreference) prefSet.findPreference(KEY_VOLUME_STEPS_SYSTEM);
            mVolumeStepsVoiceCall =
                (ListPreference) prefSet.findPreference(KEY_VOLUME_STEPS_VOICE_CALL);

            // Update volume steps
            updateVolumeSteps(mVolumeStepsAlarm.getKey(),mAudioManager.getStreamMaxVolume(mAudioManager.STREAM_ALARM));
            mVolumeStepsAlarm.setOnPreferenceChangeListener(this);

            if (isPhone){
                updateVolumeSteps(mVolumeStepsDTMF.getKey(),mAudioManager.getStreamMaxVolume(mAudioManager.STREAM_DTMF));
                mVolumeStepsDTMF.setOnPreferenceChangeListener(this);
            }
            else
                audioCat.removePreference(mVolumeStepsDTMF);

            updateVolumeSteps(mVolumeStepsMusic.getKey(),mAudioManager.getStreamMaxVolume(mAudioManager.STREAM_MUSIC));
            mVolumeStepsMusic.setOnPreferenceChangeListener(this);

            updateVolumeSteps(mVolumeStepsNotification.getKey(),mAudioManager.getStreamMaxVolume(mAudioManager.STREAM_NOTIFICATION));
            mVolumeStepsNotification .setOnPreferenceChangeListener(this);

            if (isPhone){
                updateVolumeSteps(mVolumeStepsRing.getKey(),mAudioManager.getStreamMaxVolume(mAudioManager.STREAM_RING));
                mVolumeStepsRing .setOnPreferenceChangeListener(this);
            }
            else
                audioCat.removePreference(mVolumeStepsRing);

            updateVolumeSteps(mVolumeStepsSystem.getKey(),mAudioManager.getStreamMaxVolume(mAudioManager.STREAM_SYSTEM));
            mVolumeStepsSystem .setOnPreferenceChangeListener(this);

            if (isPhone){
                updateVolumeSteps(mVolumeStepsVoiceCall.getKey(),mAudioManager.getStreamMaxVolume(mAudioManager.STREAM_VOICE_CALL));
                mVolumeStepsVoiceCall .setOnPreferenceChangeListener(this);
            }
            else
                audioCat.removePreference(mVolumeStepsVoiceCall);

        }
    }

    public boolean onPreferenceChange(Preference preference, Object objValue) {
        if (preference == mVolumeStepsAlarm) {
            updateVolumeSteps(preference.getKey(),Integer.parseInt(objValue.toString()));
        } else if (preference == mVolumeStepsDTMF) {
            updateVolumeSteps(preference.getKey(),Integer.parseInt(objValue.toString()));
        } else if (preference == mVolumeStepsMusic) {
            updateVolumeSteps(preference.getKey(),Integer.parseInt(objValue.toString()));
        } else if (preference == mVolumeStepsNotification) {
            updateVolumeSteps(preference.getKey(),Integer.parseInt(objValue.toString()));
        } else if (preference == mVolumeStepsRing) {
            updateVolumeSteps(preference.getKey(),Integer.parseInt(objValue.toString()));
        } else if (preference == mVolumeStepsSystem) {
            updateVolumeSteps(preference.getKey(),Integer.parseInt(objValue.toString()));
        } else if (preference == mVolumeStepsVoiceCall) {
            updateVolumeSteps(preference.getKey(),Integer.parseInt(objValue.toString()));
        }

        return false;
    }

    private void updateVolumeSteps(int streamType, int steps) {
        //Change the setting live
        mAudioManager.setStreamMaxVolume(streamType, steps);
    }

    private void updateVolumeSteps(String settingsKey, int steps) {
        int streamType = -1;
        if (settingsKey.equals(KEY_VOLUME_STEPS_ALARM))
            streamType = mAudioManager.STREAM_ALARM;
        else if (settingsKey.equals(KEY_VOLUME_STEPS_DTMF))
            streamType = mAudioManager.STREAM_DTMF;
        else if (settingsKey.equals(KEY_VOLUME_STEPS_MUSIC))
            streamType = mAudioManager.STREAM_MUSIC;
        else if (settingsKey.equals(KEY_VOLUME_STEPS_NOTIFICATION))
            streamType = mAudioManager.STREAM_NOTIFICATION;
        else if (settingsKey.equals(KEY_VOLUME_STEPS_RING))
            streamType = mAudioManager.STREAM_RING;
        else if (settingsKey.equals(KEY_VOLUME_STEPS_SYSTEM))
            streamType = mAudioManager.STREAM_SYSTEM;
        else if (settingsKey.equals(KEY_VOLUME_STEPS_VOICE_CALL))
            streamType = mAudioManager.STREAM_VOICE_CALL;

        //Save the setting for next boot
        Settings.System.putInt(getContentResolver(), settingsKey, steps);

        ((ListPreference)findPreference(settingsKey)).setSummary(String.valueOf(steps));

        updateVolumeSteps(streamType, steps);

        Log.i(TAG, "Volume steps:" + settingsKey + "" +String.valueOf(steps));
    }

}
