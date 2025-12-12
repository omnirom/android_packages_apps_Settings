/*
 * Copyright (C) 2025 The Android Open Source Project
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

import static android.content.ComponentName.unflattenFromString;
import static android.hardware.input.AppLaunchData.createLaunchDataForComponent;
import static android.hardware.input.InputGestureData.TOUCHPAD_GESTURE_TYPE_THREE_FINGER_TAP;
import static android.hardware.input.InputGestureData.createTouchpadTrigger;

import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.hardware.input.AppLaunchData;
import android.hardware.input.InputGestureData;
import android.hardware.input.InputManager;
import android.hardware.input.KeyGestureEvent;
import android.net.Uri;
import android.provider.Settings;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.settings.R;

import java.util.Map;

/**
 * Utility class for retrieving 3 Finger Tap related values in touchpad settings.
 */
public final class TouchpadThreeFingerTapUtils {
    static final String TAG = "TouchpadThreeFingerTap";

    static final String TARGET_ACTION =
            Settings.System.TOUCHPAD_THREE_FINGER_TAP_CUSTOMIZATION;
    static final Uri TARGET_ACTION_URI =
            Settings.System.getUriFor(TARGET_ACTION);

    static final String SHARED_PREF_NAME = "three_finger_tap";
    static final String LAUNCHING_APP_KEY = "launching_app";

    static final InputGestureData.Trigger TRIGGER =
            createTouchpadTrigger(TOUCHPAD_GESTURE_TYPE_THREE_FINGER_TAP);

    // Note that KEY_GESTURE_TYPE_UNSPECIFIED is the "mouse mid click" action
    static final Integer DEFAULT_GESTURE_TYPE =
            KeyGestureEvent.KEY_GESTURE_TYPE_UNSPECIFIED;

    private static final Map<String, Integer> PREF_KEY_TO_GESTURE_TYPE = Map.ofEntries(
            Map.entry("launch_gemini", KeyGestureEvent.KEY_GESTURE_TYPE_LAUNCH_ASSISTANT),
            Map.entry("go_home", KeyGestureEvent.KEY_GESTURE_TYPE_HOME),
            Map.entry("go_back", KeyGestureEvent.KEY_GESTURE_TYPE_BACK),
            Map.entry("recent_apps", KeyGestureEvent.KEY_GESTURE_TYPE_RECENT_APPS),
            Map.entry("launch_app", KeyGestureEvent.KEY_GESTURE_TYPE_LAUNCH_APPLICATION),
            Map.entry("middle_click", KeyGestureEvent.KEY_GESTURE_TYPE_UNSPECIFIED));

    /**
     * Returns the file name that should be backed up and restored by SharedPreferencesBackupHelper
     * infrastructure.
     *
     * @return the shared preference's name
     */
    public static String getFileToBackup() {
        // only a singleton
        return SHARED_PREF_NAME;
    }

    /**
     * @param resolver ContentResolver
     * @return the current KeyGestureEvent set for three finger tap
     */
    public static int getCurrentGestureType(@NonNull ContentResolver resolver) {
        return Settings.System.getIntForUser(
                resolver,
                TARGET_ACTION,
                KeyGestureEvent.KEY_GESTURE_TYPE_UNSPECIFIED,
                ActivityManager.getCurrentUser());
    }

    /**
     * Return if KEY_GESTURE_TYPE_LAUNCH_APPLICATION is the current the gesture type
     *
     * @param resolver ContentResolver
     */
    public static boolean isGestureTypeLaunchApp(@NonNull ContentResolver resolver) {
        return getCurrentGestureType(resolver)
                == KeyGestureEvent.KEY_GESTURE_TYPE_LAUNCH_APPLICATION;
    }

    /**
     * @param prefKey Preference key for input_touchpad_three_finger_tap_action
     * @return the corresponding KeyGestureEvent of the given key, or KEY_GESTURE_TYPE_UNSPECIFIED
     * if the key does not exist
     */
    public static int getGestureTypeByPrefKey(@NonNull String prefKey) {
        return PREF_KEY_TO_GESTURE_TYPE.getOrDefault(
                prefKey, KeyGestureEvent.KEY_GESTURE_TYPE_UNSPECIFIED);
    }

    /**
     * @param resolver ContentResolver
     * @param gestureType the three finger tap KeyGestureEvent
     */
    public static void setGestureType(@NonNull ContentResolver resolver, int gestureType) {
        Settings.System.putIntForUser(
                resolver, TARGET_ACTION, gestureType, ActivityManager.getCurrentUser());
    }

    /**
     * Set KeyGestureEvent.KEY_GESTURE_TYPE_UNSPECIFIED as the gesture type
     *
     * @param resolver ContentResolver
     */
    public static void setDefaultGestureType(@NonNull ContentResolver resolver) {
        Settings.System.putIntForUser(
                resolver, TARGET_ACTION, DEFAULT_GESTURE_TYPE, ActivityManager.getCurrentUser());
    }

    /**
     * Set KeyGestureEvent.KEY_GESTURE_TYPE_LAUNCH_APPLICATION as the gesture type and update
     * InputManager to trigger the chosen app
     *
     * @param resolver ContentResolver
     * @param inputManager InputManager
     * @param componentName ComponentName of the launching app
     */
    public static void setLaunchAppAsGestureType(
            @NonNull ContentResolver resolver, @NonNull InputManager inputManager,
            @NonNull ComponentName componentName) {
        AppLaunchData appLaunchData = createLaunchDataForComponent(
                componentName.getPackageName(), componentName.getClassName());
        InputGestureData gestureData =
                new InputGestureData.Builder()
                        .setTrigger(TouchpadThreeFingerTapUtils.TRIGGER)
                        .setAppLaunchData(appLaunchData)
                        .build();
        inputManager.removeAllCustomInputGestures(InputGestureData.Filter.TOUCHPAD);
        inputManager.addCustomInputGesture(gestureData);

        setGestureType(
                resolver, /* gestureType= */ KeyGestureEvent.KEY_GESTURE_TYPE_LAUNCH_APPLICATION);
    }

    /**
     * @param sharedPreferences SharedPreferences
     * @param launchingApp the ComponentName of the launching app
     */
    public static void putLaunchingApp(
            @NonNull SharedPreferences sharedPreferences, @Nullable ComponentName launchingApp) {
        String launchingAppFlattened = launchingApp == null ? null : launchingApp.flattenToString();
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(LAUNCHING_APP_KEY, launchingAppFlattened);
        editor.apply();
    }

    /**
     * @param sharedPreferences SharedPreferences
     * @return the ComponentName of launching app
     */
    @Nullable
    public static ComponentName getLaunchingAppComponentName(
            @Nullable SharedPreferences sharedPreferences) {
        if (sharedPreferences == null) {
            return null;
        }
        String launchingApp = sharedPreferences.getString(LAUNCHING_APP_KEY, null);
        return launchingApp == null ? null : unflattenFromString(launchingApp);
    }

    /**
     * Get the label of the default assistant
     *
     * @param resolver ContentResolver
     * @param packageManager PackageManager
     * @return the label of the default assistant
     */
    @Nullable
    public static CharSequence getAssistantName(
            @NonNull ContentResolver resolver, @NonNull PackageManager packageManager) {
        String flattened = Settings.Secure.getString(resolver, Settings.Secure.ASSISTANT);
        if (flattened != null) {
            ComponentName componentName = ComponentName.unflattenFromString(flattened);
            return getLabel(packageManager, componentName);
        }
        return null;
    }

    /**
     * Get the action title with the default assistant's name if applicable
     *
     * @param context Context
     * @param packageManager PackageManager
     * @return the action title with the default assistant's name
     */
    public static CharSequence getDefaultAssistantTitle(
            @NonNull Context context, @NonNull PackageManager packageManager) {
        CharSequence assistantName = getAssistantName(context.getContentResolver(), packageManager);
        // Use the generic name if we can't get the default assistant's label
        if (assistantName == null) {
            assistantName =
                    context.getString(R.string.three_finger_tap_launch_generic_assistant_name);
        }
        return context.getString(
                R.string.three_finger_tap_launch_default_assistant, assistantName);
    }

    /**
     * Get the label of a given app
     *
     * @param packageManager ContentResolver
     * @param componentName the ComponentName of the app
     * @return the label of the app
     */
    @Nullable
    public static CharSequence getLabel(
            @NonNull PackageManager packageManager, @Nullable ComponentName componentName) {
        if (componentName != null) {
            try {
                ApplicationInfo appInfo = packageManager.getApplicationInfo(
                        componentName.getPackageName(), /* flags= */ 0);
                return appInfo.loadLabel(packageManager);
            } catch (PackageManager.NameNotFoundException e) {
                Log.e(TAG, "Package not found: " + componentName.getPackageName(), e);
            }
        }
        return null;
    }
}
