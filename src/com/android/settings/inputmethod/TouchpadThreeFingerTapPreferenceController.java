/*
 * Copyright 2024 The Android Open Source Project
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

import static com.android.settings.inputmethod.TouchpadThreeFingerTapUtils.SHARED_PREF_NAME;
import static com.android.settings.inputmethod.TouchpadThreeFingerTapUtils.getCurrentGestureType;
import static com.android.settings.inputmethod.TouchpadThreeFingerTapUtils.getDefaultAssistantTitle;
import static com.android.settings.inputmethod.TouchpadThreeFingerTapUtils.getLabel;
import static com.android.settings.inputmethod.TouchpadThreeFingerTapUtils.getLaunchingAppComponentName;

import android.app.settings.SettingsEnums;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.hardware.input.KeyGestureEvent;
import android.os.UserHandle;
import android.provider.Settings;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleEventObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;

/** The top-level preference controller that handles the three finger tap behaviour. */
public class TouchpadThreeFingerTapPreferenceController extends BasePreferenceController
        implements LifecycleEventObserver {

    private final MetricsFeatureProvider mMetricsFeatureProvider;
    private final ContentResolver mContentResolver;
    private final PackageManager mPackageManager;
    private final SharedPreferences mSharedPreferences;

    private @Nullable Preference mPreference;

    public TouchpadThreeFingerTapPreferenceController(@NonNull Context context,
            @NonNull String key) {
        super(context, key);
        mMetricsFeatureProvider = FeatureFactory.getFeatureFactory().getMetricsFeatureProvider();
        mContentResolver = context.getContentResolver();
        mPackageManager = context.getPackageManager();
        mSharedPreferences = context.getSharedPreferences(SHARED_PREF_NAME, Context.MODE_PRIVATE);
    }

    @Override
    public int getAvailabilityStatus() {
        return InputPeripheralsSettingsUtils.isTouchpad() ? AVAILABLE : CONDITIONALLY_UNAVAILABLE;
    }

    @Override
    public int getSliceHighlightMenuRes() {
        return R.string.menu_key_system;
    }

    @Override
    public @Nullable CharSequence getSummary() {
        int gesture = getCurrentGestureType(mContentResolver);

        return switch (gesture) {
            case KeyGestureEvent.KEY_GESTURE_TYPE_UNSPECIFIED ->
                    mContext.getString(R.string.three_finger_tap_middle_click);
            case KeyGestureEvent.KEY_GESTURE_TYPE_LAUNCH_ASSISTANT ->
                    getDefaultAssistantTitle(mContext, mPackageManager);
            case KeyGestureEvent.KEY_GESTURE_TYPE_HOME ->
                    mContext.getString(R.string.three_finger_tap_go_home);
            case KeyGestureEvent.KEY_GESTURE_TYPE_BACK ->
                    mContext.getString(R.string.three_finger_tap_go_back);
            case KeyGestureEvent.KEY_GESTURE_TYPE_RECENT_APPS ->
                    mContext.getString(R.string.three_finger_tap_recent_apps);
            case KeyGestureEvent.KEY_GESTURE_TYPE_LAUNCH_APPLICATION -> getLaunchAppSummary();
            default -> null;
        };
    }

    private CharSequence getLaunchAppSummary() {
        ComponentName componentName = getLaunchingAppComponentName(mSharedPreferences);
        CharSequence label = getLabel(mPackageManager, componentName);
        return label == null
                ? mContext.getString(R.string.three_finger_tap_launch_app_summary) : label;
    }

    @Override
    public void displayPreference(@NonNull PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreference = screen.findPreference(getPreferenceKey());
        refreshSummary(mPreference);
    }

    @Override
    public void onStateChanged(@NonNull LifecycleOwner lifecycleOwner,
            @NonNull Lifecycle.Event event) {
        refreshSummary(mPreference);
        if (event == Lifecycle.Event.ON_PAUSE) {
            int currentValue =
                    Settings.System.getIntForUser(mContext.getContentResolver(),
                            Settings.System.TOUCHPAD_THREE_FINGER_TAP_CUSTOMIZATION,
                            KeyGestureEvent.KEY_GESTURE_TYPE_UNSPECIFIED, UserHandle.USER_CURRENT);
            mMetricsFeatureProvider.action(mContext,
                    SettingsEnums.ACTION_TOUCHPAD_THREE_FINGER_TAP_CUSTOMIZATION_CHANGED,
                    currentValue);
        }
    }
}
