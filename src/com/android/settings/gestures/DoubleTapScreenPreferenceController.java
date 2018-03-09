/*
 * Copyright (C) 2016 The Android Open Source Project
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

package com.android.settings.gestures;

import android.annotation.UserIdInt;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.provider.Settings;
import android.support.v7.preference.Preference;

import com.android.internal.hardware.AmbientDisplayConfiguration;
import com.android.settings.R;
import com.android.settings.search.DatabaseIndexingUtils;
import com.android.settings.search.InlineSwitchPayload;
import com.android.settings.search.ResultPayload;
import com.android.settingslib.core.lifecycle.Lifecycle;

import static android.provider.Settings.Secure.DOZE_PULSE_ON_DOUBLE_TAP;

public class DoubleTapScreenPreferenceController extends GesturePreferenceController {

    private final int ON = 1;
    private final int OFF = 0;

    private static final String PREF_KEY_VIDEO = "gesture_double_tap_screen_video";
    private final String mDoubleTapScreenPrefKey;

    private final String SECURE_KEY = DOZE_PULSE_ON_DOUBLE_TAP;

    private final AmbientDisplayConfiguration mAmbientConfig;
    @UserIdInt
    private final int mUserId;

    public DoubleTapScreenPreferenceController(Context context, Lifecycle lifecycle,
            AmbientDisplayConfiguration config, @UserIdInt int userId, String key) {
        super(context, lifecycle);
        mAmbientConfig = config;
        mUserId = userId;
        mDoubleTapScreenPrefKey = key;
    }

    public static boolean isSuggestionComplete(Context context, SharedPreferences prefs) {
        AmbientDisplayConfiguration ambientConfig = new AmbientDisplayConfiguration(context);
        return !ambientConfig.pulseOnDoubleTapAvailable()
                || prefs.getBoolean(DoubleTapScreenSettings.PREF_KEY_SUGGESTION_COMPLETE, false);
    }

    @Override
    public boolean isAvailable() {
        return mAmbientConfig.pulseOnDoubleTapAvailable();
    }

    @Override
    public String getPreferenceKey() {
        return mDoubleTapScreenPrefKey;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        final boolean enabled = (boolean) newValue;
        Settings.Secure.putInt(mContext.getContentResolver(), SECURE_KEY, enabled ? ON : OFF);
        return true;
    }

    @Override
    protected String getVideoPrefKey() {
        return PREF_KEY_VIDEO;
    }

    @Override
    protected boolean isSwitchPrefEnabled() {
        return mAmbientConfig.pulseOnDoubleTapEnabled(mUserId);
    }

    @Override
    public ResultPayload getResultPayload() {
        final Intent intent = DatabaseIndexingUtils.buildSubsettingIntent(mContext,
                DoubleTapScreenSettings.class.getName(), mDoubleTapScreenPrefKey,
                mContext.getString(R.string.display_settings));

        return new InlineSwitchPayload(SECURE_KEY, ResultPayload.SettingsSource.SECURE,
                ON /* onValue */, intent, isAvailable(), ON /* defaultValue */);
    }
}
