/*
 * Copyright 2025 The Android Open Source Project
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

package com.android.settings.inputmethod;

import static android.view.PointerIcon.POINTER_ICON_VECTOR_STYLE_STROKE_BLACK;
import static android.view.PointerIcon.POINTER_ICON_VECTOR_STYLE_STROKE_NONE;
import static android.view.PointerIcon.POINTER_ICON_VECTOR_STYLE_STROKE_WHITE;

import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.UserHandle;
import android.provider.Settings;
import android.view.flags.Flags;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleEventObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.core.BasePreferenceController;
import com.android.settingslib.widget.SelectorWithWidgetPreference;

/** Preference controller that updates the cursor stroke's color. */
public class PointerStrokePreferenceController extends BasePreferenceController
        implements LifecycleEventObserver, SelectorWithWidgetPreference.OnClickListener {

    @Nullable
    private SelectorWithWidgetPreference mPreference;

    private final Uri mUri = Settings.System.getUriFor(Settings.System.POINTER_STROKE_STYLE);

    private ContentObserver mObserver =
            new ContentObserver(new Handler(Looper.getMainLooper())) {
                @Override
                public void onChange(boolean selfChange, @Nullable Uri uri) {
                    if (mPreference == null || uri == null) {
                        return;
                    }
                    if (uri.equals(mUri)) {
                        updateState(mPreference);
                    }
                }
            };

    public PointerStrokePreferenceController(@NonNull Context context,
            @NonNull String key) {
        super(context, key);
    }

    @VisibleForTesting
    PointerStrokePreferenceController(@NonNull Context context,
            @NonNull String key,
            ContentObserver contentObserver) {
        this(context, key);
        mObserver = contentObserver;
    }

    @Override
    public int getAvailabilityStatus() {
        return Flags.enableVectorCursorA11ySettings() ? AVAILABLE
                : CONDITIONALLY_UNAVAILABLE;
    }

    @Override
    public void displayPreference(@NonNull PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreference = screen.findPreference(mPreferenceKey);
        if (mPreference != null) {
            mPreference.setOnClickListener(this);
        }
    }

    @Override
    public void onRadioButtonClicked(@NonNull SelectorWithWidgetPreference preference) {
        final int stroke = getStrokeByPrefKey(mPreferenceKey);
        setStroke(stroke);
    }

    @Override
    public void onStateChanged(@NonNull LifecycleOwner lifecycleOwner,
            @NonNull Lifecycle.Event event) {
        if (event == Lifecycle.Event.ON_START) {
            mContext.getContentResolver().registerContentObserver(
                    mUri, /* notifyForDescendants = */ true, mObserver);
        } else if (event == Lifecycle.Event.ON_STOP) {
            mContext.getContentResolver().unregisterContentObserver(mObserver);
        }
    }

    @Override
    public void updateState(@NonNull Preference preference) {
        super.updateState(preference);

        int prefValue = getStrokeByPrefKey(mPreferenceKey);
        int currentValue = getCurrentStroke();
        if (mPreference != null) {
            mPreference.setChecked(prefValue == currentValue);
        }
    }

    /**
     * Method to set stroke colour. Should only be used for testing.
     */
    @VisibleForTesting
    public void setStroke(int stroke) {
        Settings.System.putIntForUser(
                mContext.getContentResolver(),
                Settings.System.POINTER_STROKE_STYLE,
                stroke,
                UserHandle.USER_CURRENT);
    }

    /**
     * Method to get current stroke colour. Should only be used for testing.
     */
    @VisibleForTesting
    public int getCurrentStroke() {
        return Settings.System.getIntForUser(
                mContext.getContentResolver(),
                Settings.System.POINTER_STROKE_STYLE,
                /* default = */ POINTER_ICON_VECTOR_STYLE_STROKE_WHITE,
                UserHandle.USER_CURRENT);
    }

    private int getStrokeByPrefKey(@NonNull String prefKey) {
        if (prefKey.equals("stroke_style_black")) {
            return POINTER_ICON_VECTOR_STYLE_STROKE_BLACK;
        } else if (prefKey.equals("stroke_style_none")) {
            return POINTER_ICON_VECTOR_STYLE_STROKE_NONE;
        }
        // white or default
        return POINTER_ICON_VECTOR_STYLE_STROKE_WHITE;
    }
}
