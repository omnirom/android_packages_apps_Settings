/*
 * Copyright (C) 2011 The Android Open Source Project
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

package com.android.settings.tts;

import static android.provider.Settings.Secure.TTS_DEFAULT_RATE;
import static android.provider.Settings.Secure.TTS_DEFAULT_SYNTH;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.tts.TtsEnginePreference.RadioButtonGroupState;

import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.ContentResolver;
import android.content.Intent;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceDrawerActivity;
import android.preference.PreferenceCategory;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.speech.tts.TextToSpeech.EngineInfo;
import android.speech.tts.TtsEngines;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Checkable;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class TextToSpeechSettings extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener, Preference.OnPreferenceClickListener,
        RadioButtonGroupState {

    private static final String TAG = "TextToSpeechSettings";
    private static final boolean DBG = false;

    /** Preference key for the "play TTS example" preference. */
    private static final String KEY_PLAY_EXAMPLE = "tts_play_example";

    /** Preference key for the TTS rate selection dialog. */
    private static final String KEY_DEFAULT_RATE = "tts_default_rate";

    /**
     * Preference key for the engine selection preference.
     */
    private static final String KEY_ENGINE_PREFERENCE_SECTION =
            "tts_engine_preference_section";

    /**
     * These look like birth years, but they aren't mine. I'm much younger than this.
     */
    private static final int GET_SAMPLE_TEXT = 1983;
    private static final int VOICE_DATA_INTEGRITY_CHECK = 1977;

    private PreferenceCategory mEnginePreferenceCategory;
    private ListPreference mDefaultRatePref;
    private Preference mPlayExample;

    private int mDefaultRate = TextToSpeech.Engine.DEFAULT_RATE;

    /**
     * The currently selected engine.
     */
    private String mCurrentEngine;

    /**
     * The engine checkbox that is currently checked. Saves us a bit of effort
     * in deducing the right one from the currently selected engine.
     */
    private Checkable mCurrentChecked;

    /**
     * The previously selected TTS engine. Useful for rollbacks if the users
     * choice is not loaded or fails a voice integrity check.
     */
    private String mPreviousEngine;

    private TextToSpeech mTts = null;
    private TtsEngines mEnginesHelper = null;

    /**
     * The initialization listener used when we are initalizing the settings
     * screen for the first time (as opposed to when a user changes his choice
     * of engine).
     */
    private final TextToSpeech.OnInitListener mInitListener = new TextToSpeech.OnInitListener() {
        @Override
        public void onInit(int status) {
            onInitEngine(status);
        }
    };

    /**
     * The initialization listener used when the user changes his choice of
     * engine (as opposed to when then screen is being initialized for the first
     * time).
     */
    private final TextToSpeech.OnInitListener mUpdateListener = new TextToSpeech.OnInitListener() {
        @Override
        public void onInit(int status) {
            onUpdateEngine(status);
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.tts_settings);

        getActivity().setVolumeControlStream(TextToSpeech.Engine.DEFAULT_STREAM);

        mPlayExample = findPreference(KEY_PLAY_EXAMPLE);
        mPlayExample.setOnPreferenceClickListener(this);

        mEnginePreferenceCategory = (PreferenceCategory) findPreference(
                KEY_ENGINE_PREFERENCE_SECTION);
        mDefaultRatePref = (ListPreference) findPreference(KEY_DEFAULT_RATE);

        mTts = new TextToSpeech(getActivity().getApplicationContext(), mInitListener);
        mEnginesHelper = new TtsEngines(getActivity().getApplicationContext());

        setTtsUtteranceProgressListener();
        initSettings();
    }

    private void setTtsUtteranceProgressListener() {
        if (mTts == null) {
            return;
        }
        mTts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
            @Override
            public void onStart(String utteranceId) {}

            @Override
            public void onDone(String utteranceId) {}

            @Override
            public void onError(String utteranceId) {
                Log.e(TAG, "Error while trying to synthesize sample text");
            }
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mTts != null) {
            mTts.shutdown();
            mTts = null;
        }
    }

    private void initSettings() {
        final ContentResolver resolver = getContentResolver();

        // Set up the default rate.
        try {
            mDefaultRate = Settings.Secure.getInt(resolver, TTS_DEFAULT_RATE);
        } catch (SettingNotFoundException e) {
            // Default rate setting not found, initialize it
            mDefaultRate = TextToSpeech.Engine.DEFAULT_RATE;
        }
        mDefaultRatePref.setValue(String.valueOf(mDefaultRate));
        mDefaultRatePref.setOnPreferenceChangeListener(this);

        mCurrentEngine = mTts.getCurrentEngine();

        PreferenceDrawerActivity preferenceActivity = null;
        if (getActivity() instanceof PreferenceDrawerActivity) {
            preferenceActivity = (PreferenceDrawerActivity) getActivity();
        } else {
            throw new IllegalStateException("TextToSpeechSettings used outside a " +
                    "PreferenceDrawerActivity");
        }

        mEnginePreferenceCategory.removeAll();

        List<EngineInfo> engines = mEnginesHelper.getEngines();
        for (EngineInfo engine : engines) {
            TtsEnginePreference enginePref = new TtsEnginePreference(getActivity(), engine,
                    this, preferenceActivity);
            mEnginePreferenceCategory.addPreference(enginePref);
        }

        checkVoiceData(mCurrentEngine);
    }

    /**
     * Ask the current default engine to return a string of sample text to be
     * spoken to the user.
     */
    private void getSampleText() {
        String currentEngine = mTts.getCurrentEngine();

        if (TextUtils.isEmpty(currentEngine)) currentEngine = mTts.getDefaultEngine();


        Locale defaultLocale = mTts.getDefaultLanguage();
        if (defaultLocale == null) {
            Log.e(TAG, "Failed to get default language from engine " + currentEngine);
            return;
        }
        mTts.setLanguage(defaultLocale);

        // TODO: This is currently a hidden private API. The intent extras
        // and the intent action should be made public if we intend to make this
        // a public API. We fall back to using a canned set of strings if this
        // doesn't work.
        Intent intent = new Intent(TextToSpeech.Engine.ACTION_GET_SAMPLE_TEXT);

        intent.putExtra("language", defaultLocale.getLanguage());
        intent.putExtra("country", defaultLocale.getCountry());
        intent.putExtra("variant", defaultLocale.getVariant());
        intent.setPackage(currentEngine);

        try {
            if (DBG) Log.d(TAG, "Getting sample text: " + intent.toUri(0));
            startActivityForResult(intent, GET_SAMPLE_TEXT);
        } catch (ActivityNotFoundException ex) {
            Log.e(TAG, "Failed to get sample text, no activity found for " + intent + ")");
        }
    }

    /**
     * Called when the TTS engine is initialized.
     */
    public void onInitEngine(int status) {
        if (status == TextToSpeech.SUCCESS) {
            updateWidgetState(true);
            if (DBG) Log.d(TAG, "TTS engine for settings screen initialized.");
        } else {
            if (DBG) Log.d(TAG, "TTS engine for settings screen failed to initialize successfully.");
            updateWidgetState(false);
        }
    }

    /**
     * Called when voice data integrity check returns
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == GET_SAMPLE_TEXT) {
            onSampleTextReceived(resultCode, data);
        } else if (requestCode == VOICE_DATA_INTEGRITY_CHECK) {
            onVoiceDataIntegrityCheckDone(data);
        }
    }

    private String getDefaultSampleString() {
        if (mTts != null && mTts.getLanguage() != null) {
            final String currentLang = mTts.getLanguage().getISO3Language();
            String[] strings = getActivity().getResources().getStringArray(
                    R.array.tts_demo_strings);
            String[] langs = getActivity().getResources().getStringArray(
                    R.array.tts_demo_string_langs);

            for (int i = 0; i < strings.length; ++i) {
                if (langs[i].equals(currentLang)) {
                    return strings[i];
                }
            }
        }
        return null;
    }

    private boolean isNetworkRequiredForSynthesis() {
        Set<String> features = mTts.getFeatures(mTts.getLanguage());
        return features.contains(TextToSpeech.Engine.KEY_FEATURE_NETWORK_SYNTHESIS) &&
                !features.contains(TextToSpeech.Engine.KEY_FEATURE_EMBEDDED_SYNTHESIS);
    }

    private void onSampleTextReceived(int resultCode, Intent data) {
        String sample = getDefaultSampleString();

        if (resultCode == TextToSpeech.LANG_AVAILABLE && data != null) {
            if (data != null && data.getStringExtra("sampleText") != null) {
                sample = data.getStringExtra("sampleText");
            }
            if (DBG) Log.d(TAG, "Got sample text: " + sample);
        } else {
            if (DBG) Log.d(TAG, "Using default sample text :" + sample);
        }

        if (sample != null && mTts != null) {
            // The engine is guaranteed to have been initialized here
            // because this preference is not enabled otherwise.

            final boolean networkRequired = isNetworkRequiredForSynthesis();
            if (!networkRequired || networkRequired &&
                    (mTts.isLanguageAvailable(mTts.getLanguage()) >= TextToSpeech.LANG_AVAILABLE)) {
                HashMap<String, String> params = new HashMap<String, String>();
                params.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "Sample");

                mTts.speak(sample, TextToSpeech.QUEUE_FLUSH, params);
            } else {
                Log.w(TAG, "Network required for sample synthesis for requested language");
                displayNetworkAlert();
            }
        } else {
            // TODO: Display an error here to the user.
            Log.e(TAG, "Did not have a sample string for the requested language");
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object objValue) {
        if (KEY_DEFAULT_RATE.equals(preference.getKey())) {
            // Default rate
            mDefaultRate = Integer.parseInt((String) objValue);
            try {
                Settings.Secure.putInt(getContentResolver(), TTS_DEFAULT_RATE, mDefaultRate);
                if (mTts != null) {
                    mTts.setSpeechRate(mDefaultRate / 100.0f);
                }
                if (DBG) Log.d(TAG, "TTS default rate changed, now " + mDefaultRate);
            } catch (NumberFormatException e) {
                Log.e(TAG, "could not persist default TTS rate setting", e);
            }
        }

        return true;
    }

    /**
     * Called when mPlayExample is clicked
     */
    @Override
    public boolean onPreferenceClick(Preference preference) {
        if (preference == mPlayExample) {
            // Get the sample text from the TTS engine; onActivityResult will do
            // the actual speaking
            getSampleText();
            return true;
        }

        return false;
    }

    private void updateWidgetState(boolean enable) {
        mPlayExample.setEnabled(enable);
        mDefaultRatePref.setEnabled(enable);
    }

    private void displayNetworkAlert() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(android.R.string.dialog_alert_title);
        builder.setIconAttribute(android.R.attr.alertDialogIcon);
        builder.setMessage(getActivity().getString(R.string.tts_engine_network_required));
        builder.setCancelable(false);
        builder.setPositiveButton(android.R.string.ok, null);

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void updateDefaultEngine(String engine) {
        if (DBG) Log.d(TAG, "Updating default synth to : " + engine);

        // Disable the "play sample text" preference and the speech
        // rate preference while the engine is being swapped.
        updateWidgetState(false);

        // Keep track of the previous engine that was being used. So that
        // we can reuse the previous engine.
        //
        // Note that if TextToSpeech#getCurrentEngine is not null, it means at
        // the very least that we successfully bound to the engine service.
        mPreviousEngine = mTts.getCurrentEngine();

        // Step 1: Shut down the existing TTS engine.
        if (mTts != null) {
            try {
                mTts.shutdown();
                mTts = null;
            } catch (Exception e) {
                Log.e(TAG, "Error shutting down TTS engine" + e);
            }
        }

        // Step 2: Connect to the new TTS engine.
        // Step 3 is continued on #onUpdateEngine (below) which is called when
        // the app binds successfully to the engine.
        if (DBG) Log.d(TAG, "Updating engine : Attempting to connect to engine: " + engine);
        mTts = new TextToSpeech(getActivity().getApplicationContext(), mUpdateListener, engine);
        setTtsUtteranceProgressListener();
    }

    /*
     * Step 3: We have now bound to the TTS engine the user requested. We will
     * attempt to check voice data for the engine if we successfully bound to it,
     * or revert to the previous engine if we didn't.
     */
    public void onUpdateEngine(int status) {
        if (status == TextToSpeech.SUCCESS) {
            if (DBG) {
                Log.d(TAG, "Updating engine: Successfully bound to the engine: " +
                        mTts.getCurrentEngine());
            }
            checkVoiceData(mTts.getCurrentEngine());
        } else {
            if (DBG) Log.d(TAG, "Updating engine: Failed to bind to engine, reverting.");
            if (mPreviousEngine != null) {
                // This is guaranteed to at least bind, since mPreviousEngine would be
                // null if the previous bind to this engine failed.
                mTts = new TextToSpeech(getActivity().getApplicationContext(), mInitListener,
                        mPreviousEngine);
                setTtsUtteranceProgressListener();
            }
            mPreviousEngine = null;
        }
    }

    /*
     * Step 4: Check whether the voice data for the engine is ok.
     */
    private void checkVoiceData(String engine) {
        Intent intent = new Intent(TextToSpeech.Engine.ACTION_CHECK_TTS_DATA);
        intent.setPackage(engine);
        try {
            if (DBG) Log.d(TAG, "Updating engine: Checking voice data: " + intent.toUri(0));
            startActivityForResult(intent, VOICE_DATA_INTEGRITY_CHECK);
        } catch (ActivityNotFoundException ex) {
            Log.e(TAG, "Failed to check TTS data, no activity found for " + intent + ")");
        }
    }

    /*
     * Step 5: The voice data check is complete.
     */
    private void onVoiceDataIntegrityCheckDone(Intent data) {
        final String engine = mTts.getCurrentEngine();

        if (engine == null) {
            Log.e(TAG, "Voice data check complete, but no engine bound");
            return;
        }

        if (data == null){
            Log.e(TAG, "Engine failed voice data integrity check (null return)" +
                    mTts.getCurrentEngine());
            return;
        }

        Settings.Secure.putString(getContentResolver(), TTS_DEFAULT_SYNTH, engine);

        final int engineCount = mEnginePreferenceCategory.getPreferenceCount();
        for (int i = 0; i < engineCount; ++i) {
            final Preference p = mEnginePreferenceCategory.getPreference(i);
            if (p instanceof TtsEnginePreference) {
                TtsEnginePreference enginePref = (TtsEnginePreference) p;
                if (enginePref.getKey().equals(engine)) {
                    enginePref.setVoiceDataDetails(data);
                    break;
                }
            }
        }

        updateWidgetState(true);
    }

    @Override
    public Checkable getCurrentChecked() {
        return mCurrentChecked;
    }

    @Override
    public String getCurrentKey() {
        return mCurrentEngine;
    }

    @Override
    public void setCurrentChecked(Checkable current) {
        mCurrentChecked = current;
    }

    @Override
    public void setCurrentKey(String key) {
        mCurrentEngine = key;
        updateDefaultEngine(mCurrentEngine);
    }

}
