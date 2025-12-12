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

import static com.android.settings.flags.Flags.threeFingerTapAppLaunch;
import static com.android.settings.flags.Flags.touchpadSettingsDesignUpdate;
import static com.android.settings.inputmethod.TouchpadThreeFingerTapUtils.SHARED_PREF_NAME;
import static com.android.settings.inputmethod.TouchpadThreeFingerTapUtils.TARGET_ACTION_URI;
import static com.android.settings.inputmethod.TouchpadThreeFingerTapUtils.TRIGGER;
import static com.android.settings.inputmethod.TouchpadThreeFingerTapUtils.getCurrentGestureType;
import static com.android.settings.inputmethod.TouchpadThreeFingerTapUtils.getDefaultAssistantTitle;
import static com.android.settings.inputmethod.TouchpadThreeFingerTapUtils.getGestureTypeByPrefKey;
import static com.android.settings.inputmethod.TouchpadThreeFingerTapUtils.getLabel;
import static com.android.settings.inputmethod.TouchpadThreeFingerTapUtils.getLaunchingAppComponentName;
import static com.android.settings.inputmethod.TouchpadThreeFingerTapUtils.isGestureTypeLaunchApp;
import static com.android.settings.inputmethod.TouchpadThreeFingerTapUtils.setGestureType;
import static com.android.settings.inputmethod.TouchpadThreeFingerTapUtils.setLaunchAppAsGestureType;

import android.app.settings.SettingsEnums;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.ContentObserver;
import android.hardware.input.InputGestureData;
import android.hardware.input.InputManager;
import android.hardware.input.KeyGestureEvent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleEventObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.core.SubSettingLauncher;
import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;
import com.android.settingslib.widget.SelectorWithWidgetPreference;

/**
 * Preference controller that updates different the three finger tap action.
 * When clicking on the top level Three Tinger Tap Preference (handled by
 * {@link TouchpadThreeFingerTapPreferenceController}) on the Touchpad page, it loads a
 * page of action Preferences.
 */
public class TouchpadThreeFingerTapActionPreferenceController extends BasePreferenceController
        implements LifecycleEventObserver, SelectorWithWidgetPreference.OnClickListener {

    public static final String SET_GESTURE = "set_gesture_to_launch_app";

    private static final String ASSISTANT_KEY = "launch_gemini";
    private static final String APP_KEY = "launch_app";

    private final InputManager mInputManager;
    private final ContentResolver mContentResolver;
    private final PackageManager mPackageManager;

    @Nullable
    private SelectorWithWidgetPreference mPreference;

    @Nullable
    private final SharedPreferences mSharedPreferences;

    @Nullable
    private ContentObserver mObserver;

    private final MetricsFeatureProvider mMetricsFeatureProvider;

    public TouchpadThreeFingerTapActionPreferenceController(@NonNull Context context,
            @NonNull String key) {
        this(context, key, /* inputManager= */ context.getSystemService(InputManager.class));
        mObserver = new ContentObserver(new Handler(Looper.getMainLooper())) {
            @Override
            public void onChange(boolean selfChange, @Nullable Uri uri) {
                if (mPreference == null || uri == null) {
                    return;
                }
                if (uri.equals(TARGET_ACTION_URI)) {
                    updateState(mPreference);
                }
            }
        };
    }

    @VisibleForTesting
    TouchpadThreeFingerTapActionPreferenceController(@NonNull Context context,
            @NonNull String key,
            ContentObserver contentObserver,
            InputManager inputManager) {
        this(context, key, inputManager);
        mObserver = contentObserver;
    }

    TouchpadThreeFingerTapActionPreferenceController(@NonNull Context context,
            @NonNull String key,
            InputManager inputManager) {
        super(context, key);
        mInputManager = inputManager;
        mContentResolver = context.getContentResolver();
        mPackageManager = context.getPackageManager();
        mSharedPreferences = context.getSharedPreferences(SHARED_PREF_NAME, Context.MODE_PRIVATE);
        mMetricsFeatureProvider = FeatureFactory.getFeatureFactory().getMetricsFeatureProvider();
    }

    @Override
    public int getAvailabilityStatus() {
        if (!InputPeripheralsSettingsUtils.isTouchpad()) {
            return CONDITIONALLY_UNAVAILABLE;
        }
        return (mPreferenceKey.equals(APP_KEY) && !threeFingerTapAppLaunch())
                ? CONDITIONALLY_UNAVAILABLE : AVAILABLE;
    }

    @Override
    public void displayPreference(@NonNull PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreference = screen.findPreference(mPreferenceKey);

        if (mPreference != null) {
            if (mPreferenceKey.equals(APP_KEY)) {
                mPreference.setExtraWidgetOnClickListener(
                        v -> appSelectionLauncher(/* isRadioClicked= */ false).launch());
            }
            mPreference.setOnClickListener(this);
            if (touchpadSettingsDesignUpdate() && mPreferenceKey.equals(ASSISTANT_KEY)) {
                mPreference.setTitle(getDefaultAssistantTitle(mContext, mPackageManager));
            }
        }
    }

    private SubSettingLauncher appSelectionLauncher(boolean isRadioClicked) {
        SubSettingLauncher subSettingLauncher =
                new SubSettingLauncher(mContext)
                        .setDestination(TouchpadThreeFingerTapAppSelectionFragment.class.getName())
                        .setSourceMetricsCategory(SettingsEnums.TOUCHPAD_THREE_FINGER_TAP);
        // The gesture has to be set right away if the action is launch app
        if (isRadioClicked || isGestureTypeLaunchApp(mContentResolver)) {
            Bundle args = new Bundle();
            args.putBoolean(SET_GESTURE, true);
            subSettingLauncher.setArguments(args);
        }
        return subSettingLauncher;
    }

    @Override
    public void onRadioButtonClicked(@NonNull SelectorWithWidgetPreference preference) {
        final int gestureType = getGestureTypeByPrefKey(mPreferenceKey);
        setGesture(gestureType);
    }

    private void setGesture(int customGestureType) {
        mInputManager.removeAllCustomInputGestures(InputGestureData.Filter.TOUCHPAD);
        if (customGestureType != KeyGestureEvent.KEY_GESTURE_TYPE_UNSPECIFIED) {
            if (customGestureType == KeyGestureEvent.KEY_GESTURE_TYPE_LAUNCH_APPLICATION) {
                // If there's no app selected (either the first time or the user has uninstalled the
                // app chosen), we launch the app selection page to pick an app to launch
                ComponentName launchingApp = getLaunchingAppComponentName(mSharedPreferences);
                if (launchingApp == null) {
                    appSelectionLauncher(/* isRadioClicked= */ true).launch();
                } else {
                    mMetricsFeatureProvider.action(mContext,
                            SettingsEnums.ACTION_TOUCHPAD_THREE_FINGER_TAP_LAUNCHING_APP,
                            launchingApp.getPackageName());
                    setLaunchAppAsGestureType(mContentResolver, mInputManager, launchingApp);
                }
            } else {
                InputGestureData gestureData = new InputGestureData.Builder()
                        .setTrigger(TRIGGER)
                        .setKeyGestureType(customGestureType)
                        .build();
                mInputManager.addCustomInputGesture(gestureData);
            }

        }
        setGestureType(mContentResolver, customGestureType);
    }

    @Override
    public void onStateChanged(@NonNull LifecycleOwner lifecycleOwner,
            @NonNull Lifecycle.Event event) {
        if (event == Lifecycle.Event.ON_START) {
            mContentResolver.registerContentObserver(
                    TARGET_ACTION_URI,
                    /* notifyForDescendants= */ true, mObserver);
        } else if (event == Lifecycle.Event.ON_STOP) {
            mContentResolver.unregisterContentObserver(mObserver);
        }
    }

    @Override
    public void updateState(@NonNull Preference preference) {
        super.updateState(preference);

        int prefValue = getGestureTypeByPrefKey(mPreferenceKey);
        int currentValue = getCurrentGestureType(mContentResolver);
        if (mPreference != null) {
            mPreference.setChecked(prefValue == currentValue);
        }
    }

    @Override
    public @Nullable CharSequence getSummary() {
        // We only show the summary for app selection
        if (mPreferenceKey.equals(APP_KEY)) {
            ComponentName componentName = getLaunchingAppComponentName(mSharedPreferences);
            CharSequence label = getLabel(mPackageManager, componentName);
            // Show instruction when no chosen app
            return label == null
                    ? mContext.getString(R.string.three_finger_tap_launch_app_summary) : label;
        }
        return null;
    }
}
