/*
 * Copyright (C) 2014 The Android Open Source Project
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
import android.content.Intent;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.UserHandle;
import android.preference.SeekBarVolumizer;
import android.provider.SearchIndexableResource;
import android.provider.Settings;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.Preference.OnPreferenceChangeListener;
import android.support.v7.preference.TwoStatePreference;
import android.text.TextUtils;

import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.settings.R;
import com.android.settings.RingtonePreference;
import com.android.settings.Utils;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.core.lifecycle.Lifecycle;

import org.omnirom.omnigears.preference.AppMultiSelectListPreference;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

public class SoundSettings extends DashboardFragment implements OnPreferenceChangeListener {
    private static final String TAG = "SoundSettings";

    private static final String KEY_CELL_BROADCAST_SETTINGS = "cell_broadcast_settings";
    private static final String SELECTED_PREFERENCE_KEY = "selected_preference";
    private static final String KEY_VOLUME_LINK_NOTIFICATION = "volume_link_notification";
    private static final String KEY_RAMP_UP_TIME = "increasing_ring_ramp_up";
    private static final String KEY_HEADSET_PLUG_ACTION = "headset_plug_action";
    private static final String KEY_HEADSET_PLUG_APP_LIST = "headset_plug_app_list";
    private static final String KEY_HEADSET_PLUG_APP_RUNNING = "headset_plug_app_running";
    private static final String VALUE_APP_LIST = "APP_LIST";
    private static final int REQUEST_CODE = 200;

    private static final int SAMPLE_CUTOFF = 2000;  // manually cap sample playback at 2 seconds

    private final VolumePreferenceCallback mVolumeCallback = new VolumePreferenceCallback();
    private final H mHandler = new H();

    private RingtonePreference mRequestPreference;
    private TwoStatePreference mVolumeLinkNotification;
    private ListPreference mRampUpTime;
    private ListPreference mHeadsetPlugAction;
    private ListPreference mHeadsetAppRunning;
    private AppMultiSelectListPreference mHeadsetPlugAppList;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mProgressiveDisclosureMixin.setTileLimit(1);
    }

    @Override
    public int getMetricsCategory() {
        return MetricsEvent.SOUND;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            String selectedPreference = savedInstanceState.getString(SELECTED_PREFERENCE_KEY, null);
            if (!TextUtils.isEmpty(selectedPreference)) {
                mRequestPreference = (RingtonePreference) findPreference(selectedPreference);
            }
        }
        if (Utils.isVoiceCapable(getContext())) {
            mVolumeLinkNotification = (TwoStatePreference) findPreference(KEY_VOLUME_LINK_NOTIFICATION);
            initVolumeLinkNotification();
            updateVolumeLinkNotification();
        } else {
            removePreference(KEY_VOLUME_LINK_NOTIFICATION);
        }

        mRampUpTime = (ListPreference) findPreference(KEY_RAMP_UP_TIME);
        int ramUpTime = Settings.System.getInt(getContentResolver(),
                Settings.System.INCREASING_RING_RAMP_UP_TIME, 10);

        mRampUpTime.setValue(Integer.toString(ramUpTime));
        mRampUpTime.setSummary(mRampUpTime.getEntry());
        mRampUpTime.setOnPreferenceChangeListener(this);

        mHeadsetPlugAction = (ListPreference) findPreference(KEY_HEADSET_PLUG_ACTION);
        String headset_action = Settings.System.getStringForUser(getContentResolver(),
                Settings.System.HEADSET_PLUG_ACTION, UserHandle.USER_CURRENT);
        mHeadsetPlugAction.setValue(headset_action);
        mHeadsetPlugAction.setSummary(mHeadsetPlugAction.getEntry());
        mHeadsetPlugAction.setOnPreferenceChangeListener(this);

        mHeadsetPlugAppList = (AppMultiSelectListPreference) findPreference(KEY_HEADSET_PLUG_APP_LIST);
        mHeadsetPlugAppList.setEnabled(
                headset_action.equals(VALUE_APP_LIST));
        mHeadsetPlugAppList.setOnPreferenceChangeListener(this);

        mHeadsetAppRunning = (ListPreference) findPreference(KEY_HEADSET_PLUG_APP_RUNNING);
        mHeadsetAppRunning.setValue(Integer.toString(Settings.System.getInt(
                getContentResolver(), Settings.System.HEADSET_PLUG_APP_RUNNING, 0)));
        mHeadsetAppRunning.setSummary(mHeadsetAppRunning.getEntry());
        mHeadsetAppRunning.setOnPreferenceChangeListener(this);
    }

    @Override
    protected int getHelpResource() {
        return R.string.help_url_sound;
    }

    @Override
    public void onPause() {
        super.onPause();
        mVolumeCallback.stopSample();
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
        if (preference instanceof RingtonePreference) {
            mRequestPreference = (RingtonePreference) preference;
            mRequestPreference.onPrepareRingtonePickerIntent(mRequestPreference.getIntent());
            startActivityForResultAsUser(
                    mRequestPreference.getIntent(),
                    REQUEST_CODE,
                    null,
                    UserHandle.of(mRequestPreference.getUserId()));
            return true;
        }
        return super.onPreferenceTreeClick(preference);
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.sound_settings;
    }

    @Override
    protected List<AbstractPreferenceController> getPreferenceControllers(Context context) {
        return buildPreferenceControllers(context, this, mVolumeCallback, getLifecycle());
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (mRequestPreference != null) {
            mRequestPreference.onActivityResult(requestCode, resultCode, data);
            mRequestPreference = null;
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mRequestPreference != null) {
            outState.putString(SELECTED_PREFERENCE_KEY, mRequestPreference.getKey());
        }
    }

    // === Volumes ===

    final class VolumePreferenceCallback implements VolumeSeekBarPreference.Callback {
        private SeekBarVolumizer mCurrent;

        @Override
        public void onSampleStarting(SeekBarVolumizer sbv) {
            if (mCurrent != null && mCurrent != sbv) {
                mCurrent.stopSample();
            }
            mCurrent = sbv;
            if (mCurrent != null) {
                mHandler.removeMessages(H.STOP_SAMPLE);
                mHandler.sendEmptyMessageDelayed(H.STOP_SAMPLE, SAMPLE_CUTOFF);
            }
        }

        @Override
        public void onStreamValueChanged(int stream, int progress) {
            // noop
        }

        public void stopSample() {
            if (mCurrent != null) {
                mCurrent.stopSample();
            }
        }
        @Override
        public void onMuted(int stream, boolean muted, boolean zenMuted) {
            // noop
        }
    }

    // === Callbacks ===


    private final class H extends Handler {
        private static final int STOP_SAMPLE = 1;

        private H() {
            super(Looper.getMainLooper());
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case STOP_SAMPLE:
                    mVolumeCallback.stopSample();
                    break;
            }
        }
    }

    private static List<AbstractPreferenceController> buildPreferenceControllers(Context context,
            SoundSettings fragment, VolumeSeekBarPreference.Callback callback,
            Lifecycle lifecycle) {
        final List<AbstractPreferenceController> controllers = new ArrayList<>();
        controllers.add(new ZenModePreferenceController(context));
        controllers.add(new EmergencyBroadcastPreferenceController(
                context, KEY_CELL_BROADCAST_SETTINGS));
        controllers.add(new VibrateWhenRingPreferenceController(context));

        // === Volumes ===
        controllers.add(new AlarmVolumePreferenceController(context, callback, lifecycle));
        controllers.add(new MediaVolumePreferenceController(context, callback, lifecycle));
        controllers.add(new NotificationVolumePreferenceController(context, callback, lifecycle));
        controllers.add(new RingVolumePreferenceController(context, callback, lifecycle));

        // === Phone & notification ringtone ===
        controllers.add(new PhoneRingtonePreferenceController(context));
        controllers.add(new AlarmRingtonePreferenceController(context));
        controllers.add(new NotificationRingtonePreferenceController(context));

        // === Work Sound Settings ===
        controllers.add(new WorkSoundPreferenceController(context, fragment, lifecycle));

        // === Other Sound Settings ===
        controllers.add(new DialPadTonePreferenceController(context, fragment, lifecycle));
        controllers.add(new ScreenLockSoundPreferenceController(context, fragment, lifecycle));
        controllers.add(new ChargingSoundPreferenceController(context, fragment, lifecycle));
        controllers.add(new DockingSoundPreferenceController(context, fragment, lifecycle));
        controllers.add(new TouchSoundPreferenceController(context, fragment, lifecycle));
        controllers.add(new VibrateOnTouchPreferenceController(context, fragment, lifecycle));
        controllers.add(new DockAudioMediaPreferenceController(context, fragment, lifecycle));
        controllers.add(new BootSoundPreferenceController(context));
        controllers.add(new EmergencyTonePreferenceController(context, fragment, lifecycle));
        controllers.add(new ScreenshotSoundPreferenceController(context, fragment, lifecycle));

        return controllers;
    }

    private void initVolumeLinkNotification() {
        if (mVolumeLinkNotification != null) {
            mVolumeLinkNotification.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    final boolean val = (Boolean)newValue;
                    if (val) {
                        // set to same volume as ringer by default if link is enabled
                        // otherwise notification volume will only change after next
                        // change of ringer volume
                        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
                        final int ringerVolume = audioManager.getStreamVolume(AudioManager.STREAM_RING);
                        audioManager.setStreamVolume(AudioManager.STREAM_NOTIFICATION, ringerVolume, 0);
                    }
                    Settings.System.putInt(getContentResolver(),
                            Settings.System.VOLUME_LINK_NOTIFICATION, val ? 1 : 0);
                    NotificationVolumePreferenceController notificationVolumeController = 
                            getPreferenceController(NotificationVolumePreferenceController.class);
                    if (notificationVolumeController != null) {
                        notificationVolumeController.getPreference().setVisible(!val);
                    }
                    return true;
                }
            });
        }
    }

    private void updateVolumeLinkNotification() {
        if (mVolumeLinkNotification != null) {
            final boolean linkEnabled = Settings.System.getInt(getContentResolver(),
                    Settings.System.VOLUME_LINK_NOTIFICATION, 1) == 1;
            mVolumeLinkNotification.setChecked(linkEnabled);
            NotificationVolumePreferenceController notificationVolumeController = 
                    getPreferenceController(NotificationVolumePreferenceController.class);
            if (notificationVolumeController != null) {
                notificationVolumeController.getPreference().setVisible(!linkEnabled);
            }
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mRampUpTime) {
            int value = Integer.valueOf((String) newValue);
            int index = mRampUpTime.findIndexOfValue((String) newValue);
            mRampUpTime.setSummary(mRampUpTime.getEntries()[index]);
            Settings.System.putInt(getContentResolver(), Settings.System.INCREASING_RING_RAMP_UP_TIME, value);
            return true;
        } else if (preference == mHeadsetPlugAction){
            String value = (String) newValue;
            int index = mHeadsetPlugAction.findIndexOfValue(value);
            mHeadsetPlugAction.setSummary(mHeadsetPlugAction.getEntries()[index]);
            Settings.System.putStringForUser(getContentResolver(),
                    Settings.System.HEADSET_PLUG_ACTION, value, UserHandle.USER_CURRENT);
            mHeadsetPlugAppList.setEnabled(
                    value.equals(VALUE_APP_LIST));
            return true;
        } else if (preference == mHeadsetPlugAppList) {
            StringBuilder builder = new StringBuilder();
            String delimiter = "";

            for (String value : (Set<String>) newValue) {
                builder.append(delimiter);
                builder.append(value);
                delimiter = "|";
            }

            Settings.System.putStringForUser(getContentResolver(),
                    Settings.System.HEADSET_PLUG_APP_LIST, builder.toString(), UserHandle.USER_CURRENT);
            return true;
        } else if (preference == mHeadsetAppRunning) {
            String value = (String) newValue;
            int val = Integer.parseInt(value);
            Settings.System.putIntForUser(getContentResolver(),
                    Settings.System.HEADSET_PLUG_APP_RUNNING, val, UserHandle.USER_CURRENT);
            int index = mHeadsetAppRunning.findIndexOfValue(value);
            mHeadsetAppRunning.setSummary(mHeadsetAppRunning.getEntries()[index]);
            return true;
        }

        return false;
    }

    // === Indexing ===

    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider() {

                public List<SearchIndexableResource> getXmlResourcesToIndex(
                        Context context, boolean enabled) {
                    final SearchIndexableResource sir = new SearchIndexableResource(context);
                    sir.xmlResId = R.xml.sound_settings;
                    return Arrays.asList(sir);
                }

                @Override
                public List<AbstractPreferenceController> getPreferenceControllers(Context context) {
                    return buildPreferenceControllers(context, null /* fragment */,
                            null /* callback */, null /* lifecycle */);
                }

                @Override
                public List<String> getNonIndexableKeys(Context context) {
                    List<String> keys = super.getNonIndexableKeys(context);
                    // Duplicate results
                    keys.add((new ZenModePreferenceController(context)).getPreferenceKey());
                    keys.add(ZenModeSettings.KEY_VISUAL_SETTINGS);
                    keys.add(KEY_CELL_BROADCAST_SETTINGS);
                    return keys;
                }
            };

    // === Work Sound Settings ===

    void enableWorkSync() {
        final WorkSoundPreferenceController workSoundController =
                getPreferenceController(WorkSoundPreferenceController.class);
        if (workSoundController != null) {
            workSoundController.enableWorkSync();
        }
    }
}
