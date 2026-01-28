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

import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;

import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnPause;
import com.android.settingslib.core.lifecycle.events.OnResume;

import org.omnirom.omnilib.utils.OmniSettings;

public class GestureSmallPreferenceController extends BasePreferenceController implements
        LifecycleObserver, OnResume, OnPause {

    private ContentObserver mObserver =
            new ContentObserver(new Handler(Looper.getMainLooper())) {
                @Override
                public void onChange(boolean selfChange) {
                    updateState(mPreference);
                }
            };
    private Preference mPreference;

    public GestureSmallPreferenceController(Context context, String key) {
        super(context, key);
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreference = screen.findPreference(getPreferenceKey());
    }

    private boolean hideGestureHandle() {
        return Settings.System.getInt(mContext.getContentResolver(),
                OmniSettings.OMNI_GESTURE_HANDLE_HIDE, 0) != 0;
    }

    @Override
    public void onResume() {
        mContext.getContentResolver().registerContentObserver(
                Settings.System.getUriFor(OmniSettings.OMNI_GESTURE_HANDLE_HIDE),
                false, mObserver);
    }

    @Override
    public void onPause() {
        mContext.getContentResolver().unregisterContentObserver(mObserver);
    }

    @Override
    public int getAvailabilityStatus() {
        return hideGestureHandle() ? DISABLED_DEPENDENT_SETTING : AVAILABLE;
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);
        preference.setEnabled(getAvailabilityStatus() != DISABLED_DEPENDENT_SETTING);
        refreshSummary(preference);
    }
}
