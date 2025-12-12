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

import static android.content.ComponentName.unflattenFromString;
import static android.content.Context.MODE_PRIVATE;

import static com.android.settings.inputmethod.TouchpadThreeFingerTapActionPreferenceController.SET_GESTURE;
import static com.android.settings.inputmethod.TouchpadThreeFingerTapUtils.LAUNCHING_APP_KEY;
import static com.android.settings.inputmethod.TouchpadThreeFingerTapUtils.SHARED_PREF_NAME;
import static com.android.settings.inputmethod.TouchpadThreeFingerTapUtils.TARGET_ACTION_URI;
import static com.android.settings.inputmethod.TouchpadThreeFingerTapUtils.getLaunchingAppComponentName;
import static com.android.settings.inputmethod.TouchpadThreeFingerTapUtils.isGestureTypeLaunchApp;
import static com.android.settings.inputmethod.TouchpadThreeFingerTapUtils.putLaunchingApp;
import static com.android.settings.inputmethod.TouchpadThreeFingerTapUtils.setDefaultGestureType;
import static com.android.settings.inputmethod.TouchpadThreeFingerTapUtils.setLaunchAppAsGestureType;

import android.app.ActivityManager;
import android.app.settings.SettingsEnums;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.LauncherActivityInfo;
import android.content.pm.LauncherApps;
import android.database.ContentObserver;
import android.hardware.input.InputGestureData;
import android.hardware.input.InputManager;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.UserHandle;
import android.util.DisplayMetrics;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleEventObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.core.BasePreferenceController;
import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;
import com.android.settingslib.widget.SelectorWithWidgetPreference;

import java.util.Comparator;
import java.util.List;

/**
 * Controller that updates a list of installed app Preferences.
 * This screen is a sub-page of {@link TouchpadThreeFingerTapPreferenceController}) allowing three
 * finger tap gesture to open the selected app.
 */
public class TouchpadThreeFingerTapAppListController extends BasePreferenceController
        implements LifecycleEventObserver, SelectorWithWidgetPreference.OnClickListener,
        SharedPreferences.OnSharedPreferenceChangeListener {

    private final ContentResolver mContentResolver;
    private final LauncherApps mLauncherApps;
    private final InputManager mInputManager;
    private final SharedPreferences mSharedPreferences;

    @Nullable
    private ContentObserver mObserver;
    @Nullable
    private PreferenceScreen mPreferenceScreen;

    private final MetricsFeatureProvider mMetricsFeatureProvider;

    private final LauncherApps.Callback mLauncherAppsCallback = new LauncherApps.Callback() {
        @Override
        public void onPackageRemoved(@Nullable String packageName, @Nullable UserHandle user) {
            if (packageName != null && isSelectedApp(packageName)) {
                handleAppRemoval();
            }
        }

        @Override
        public void onPackageAdded(@Nullable String packageName, @Nullable UserHandle user) {}

        @Override
        public void onPackageChanged(@Nullable String packageName, @Nullable UserHandle user) {}

        @Override
        public void onPackagesAvailable(
                @Nullable String[] packageNames, @Nullable UserHandle user, boolean replacing) {}

        @Override
        public void onPackagesUnavailable(
                @Nullable String[] packageNames, @Nullable UserHandle user, boolean replacing) {
            if (packageNames == null) {
                return;
            }
            for (String packageName : packageNames) {
                if (isSelectedApp(packageName)) {
                    handleAppRemoval();
                    break;
                }
            }
        }

        @Override
        public void onPackagesUnsuspended(
                @Nullable String[] packageNames, @Nullable UserHandle user) {
            if (packageNames == null) {
                return;
            }
            for (String packageName : packageNames) {
                if (isSelectedApp(packageName)) {
                    handleAppRemoval();
                    break;
                }
            }
        }
    };

    private void handleAppRemoval() {
        if (mSharedPreferences != null) {
            putLaunchingApp(mSharedPreferences, /* launchingApp= */ null);
        }
        // Only consider fallback if the gesture is launch app
        if (isGestureTypeLaunchApp(mContentResolver)) {
            mInputManager.removeAllCustomInputGestures(InputGestureData.Filter.TOUCHPAD);
            setDefaultGestureType(mContentResolver);
        }
        repopulateApps();
    }

    private boolean isSelectedApp(@NonNull String packageName) {
        if (mSharedPreferences == null) {
            return false;
        }
        ComponentName componentName = getLaunchingAppComponentName(mSharedPreferences);
        return componentName != null && packageName.equals(componentName.getPackageName());
    }

    public TouchpadThreeFingerTapAppListController(@NonNull Context context,
            @NonNull String key) {
        this(context, key,
                /* launcherApps= */ context.getSystemService(LauncherApps.class),
                /* inputManager= */ context.getSystemService(InputManager.class),
                /* sharedPreferences= */ context.getSharedPreferences(
                        SHARED_PREF_NAME, MODE_PRIVATE));
        mObserver = new ContentObserver(new Handler(Looper.getMainLooper())) {
            @Override
            public void onChange(boolean selfChange, @Nullable Uri uri) {
                if (uri == null || mPreferenceScreen == null) {
                    return;
                }
                if (uri.equals(TARGET_ACTION_URI)) {
                    updateState(mPreferenceScreen);
                }
            }
        };
    }

    @VisibleForTesting
    TouchpadThreeFingerTapAppListController(@NonNull Context context,
            @NonNull String key,
            @NonNull LauncherApps launcherApps,
            @NonNull InputManager inputManager,
            @NonNull SharedPreferences sharedPreferences,
            @NonNull ContentObserver contentObserver) {
        this(context, key, launcherApps, inputManager, sharedPreferences);
        mObserver = contentObserver;
    }

    TouchpadThreeFingerTapAppListController(@NonNull Context context,
            @NonNull String key,
            LauncherApps launcherApps,
            InputManager inputManager,
            SharedPreferences sharedPreferences) {
        super(context, key);
        mContentResolver = context.getContentResolver();
        mLauncherApps = launcherApps;
        mInputManager = inputManager;
        mSharedPreferences = sharedPreferences;
        mLauncherApps.registerCallback(mLauncherAppsCallback);
        mMetricsFeatureProvider = FeatureFactory.getFeatureFactory().getMetricsFeatureProvider();
    }

    @Override
    public void onStateChanged(@NonNull LifecycleOwner lifecycleOwner,
            @NonNull Lifecycle.Event event) {
        if (event == Lifecycle.Event.ON_START) {
            mContentResolver.registerContentObserver(
                    TARGET_ACTION_URI, /* notifyForDescendants= */ true, mObserver);
            if (mSharedPreferences != null) {
                mSharedPreferences.registerOnSharedPreferenceChangeListener(this);
            }

        } else if (event == Lifecycle.Event.ON_STOP) {
            mContentResolver.unregisterContentObserver(mObserver);
            if (mSharedPreferences != null) {
                mSharedPreferences.unregisterOnSharedPreferenceChangeListener(this);
            }
        }
    }

    @Override
    public void onSharedPreferenceChanged(@NonNull SharedPreferences sharedPreferences,
            @Nullable String key) {
        if (LAUNCHING_APP_KEY.equals(key) && mPreferenceScreen != null) {
            updateState(mPreferenceScreen);
        }
    }

    @Override
    public void updateState(@NonNull Preference preference) {
        super.updateState(preference);
        updateAppListSelection();
    }

    private void updateAppListSelection() {
        // When the current gesture state is not app launching, the key is set to null so that no
        // app Preference will be selected
        String matchingKey = null;
        ComponentName componentName = getLaunchingAppComponentName(mSharedPreferences);
        if (componentName != null) {
            matchingKey = parsePreferenceKeyFromComponent(componentName);
        }
        updateCheckStatus(matchingKey);
    }

    private void updateCheckStatus(@Nullable String matchingKey) {
        if (mPreferenceScreen == null) {
            return;
        }
        int count = mPreferenceScreen.getPreferenceCount();
        for (int i = 0; i < count; i++) {
            Preference pref = mPreferenceScreen.getPreference(i);
            if (pref instanceof SelectorWithWidgetPreference appPreference) {
                boolean isMatched = matchingKey != null
                        && matchingKey.equals(appPreference.getKey());
                appPreference.setChecked(isMatched);
            }
        }
    }

    @NonNull
    private String parsePreferenceKeyFromComponent(@NonNull ComponentName componentName) {
        // flattenToString contains the component's package name and its class name. This way, when
        // given a Preference, we can restore its corresponding component using its key.
        // unflattenFromString is called in onRadioButtonClicked
        return componentName.flattenToString();
    }

    @Override
    public int getAvailabilityStatus() {
        return InputPeripheralsSettingsUtils.isTouchpad() ? AVAILABLE : CONDITIONALLY_UNAVAILABLE;
    }

    @Override
    public void displayPreference(@NonNull PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreferenceScreen = screen;
        repopulateApps();
    }

    private void repopulateApps() {
        if (mPreferenceScreen == null) {
            return;
        }
        mPreferenceScreen.removeAll();

        int userId = ActivityManager.getCurrentUser();
        List<LauncherActivityInfo> appInfos =
                mLauncherApps.getActivityList(/* packageName= */ null, UserHandle.of(userId));
        appInfos.sort(Comparator.comparing(appInfo -> appInfo.getLabel().toString()));

        for (LauncherActivityInfo appInfo : appInfos) {
            mPreferenceScreen.addPreference(createPreference(appInfo));
        }
    }

    @NonNull
    private SelectorWithWidgetPreference createPreference(@NonNull LauncherActivityInfo appInfo) {
        SelectorWithWidgetPreference preference = new SelectorWithWidgetPreference(mContext);
        ComponentName component = appInfo.getComponentName();
        preference.setKey(parsePreferenceKeyFromComponent(component));
        preference.setTitle(appInfo.getLabel());
        preference.setIcon(appInfo.getIcon(DisplayMetrics.DENSITY_DEVICE_STABLE));
        preference.setOnClickListener(this);
        return preference;
    }

    @Override
    public void onRadioButtonClicked(@NonNull SelectorWithWidgetPreference preference) {
        String key = preference.getKey();

        // The key stores the component's information for each Preference
        // See comments in parsePreferenceKeyFromComponent()
        ComponentName componentName = unflattenFromString(key);
        setLaunchingApp(componentName);
        updateAppListSelection();
    }

    private void setLaunchingApp(@Nullable ComponentName componentName) {
        if (mPreferenceScreen == null || mSharedPreferences == null || componentName == null) {
            return;
        }
        putLaunchingApp(mSharedPreferences, componentName);
        if (mPreferenceScreen.getExtras().getBoolean(SET_GESTURE)) {
            mMetricsFeatureProvider.action(mContext,
                    SettingsEnums.ACTION_TOUCHPAD_THREE_FINGER_TAP_LAUNCHING_APP,
                    componentName.getPackageName());
            setLaunchAppAsGestureType(mContentResolver, mInputManager, componentName);
        }
    }
}
