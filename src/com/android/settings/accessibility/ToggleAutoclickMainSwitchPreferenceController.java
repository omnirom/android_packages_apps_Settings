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

package com.android.settings.accessibility;

import static android.provider.Settings.Secure.NAVIGATION_MODE;

import static com.android.settings.accessibility.AccessibilityUtil.State.OFF;
import static com.android.settings.accessibility.AccessibilityUtil.State.ON;

import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.server.accessibility.Flags;
import com.android.settings.R;
import com.android.settings.core.TogglePreferenceController;

/** The controller to handle main switch to turn on or turn off accessibility autoclick. */
public class ToggleAutoclickMainSwitchPreferenceController
        extends TogglePreferenceController implements DefaultLifecycleObserver {

    static final Uri ACCESSIBILITY_AUTOCLICK_ENABLED_URI =
            Settings.Secure.getUriFor(Settings.Secure.ACCESSIBILITY_AUTOCLICK_ENABLED);
    private final ContentResolver mContentResolver;
    private @Nullable Preference mPreference;
    private @Nullable FragmentManager mFragmentManager;

    @VisibleForTesting
    final ContentObserver mSettingsObserver =
            new ContentObserver(new Handler(Looper.getMainLooper())) {
                @Override
                public void onChange(boolean selfChange, @Nullable Uri uri) {
                    if (mPreference == null || uri == null) {
                        return;
                    }
                    updateState(mPreference);
                }
            };

    public ToggleAutoclickMainSwitchPreferenceController(
            @NonNull Context context, @NonNull String preferenceKey) {
        super(context, preferenceKey);
        mContentResolver = context.getContentResolver();
    }

    public void setFragment(@NonNull Fragment fragment) {
        mFragmentManager = fragment.getChildFragmentManager();
    }

    @Override
    public int getAvailabilityStatus() {
        return Flags.enableAutoclickIndicator() ? AVAILABLE : CONDITIONALLY_UNAVAILABLE;
    }

    @Override
    public void displayPreference(@NonNull PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreference = screen.findPreference(getPreferenceKey());
    }

    @Override
    public boolean isChecked() {
        return Settings.Secure.getInt(mContentResolver,
                Settings.Secure.ACCESSIBILITY_AUTOCLICK_ENABLED, OFF) == ON;
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        Settings.Secure.putInt(mContentResolver,
                Settings.Secure.ACCESSIBILITY_AUTOCLICK_ENABLED,
                isChecked ? ON : OFF);

        // Show navigation suggestion dialog when enabling.
        if (isChecked && isGestureNavigationEnabled() && !isLaptopDevice()) {
            showNavigationSuggestion();
        }

        return true;
    }

    @Override
    public void onStart(@NonNull LifecycleOwner owner) {
        mContentResolver.registerContentObserver(
                ACCESSIBILITY_AUTOCLICK_ENABLED_URI,
                /* notifyForDescendants= */ false,
                mSettingsObserver);
    }

    @Override
    public void onStop(@NonNull LifecycleOwner owner) {
        mContentResolver.unregisterContentObserver(mSettingsObserver);
    }

    @Override
    public int getSliceHighlightMenuRes() {
        return R.string.menu_key_system;
    }

    /**
     * Shows navigation suggestion dialog when autoclick is enabled.
     */
    private void showNavigationSuggestion() {
        if (mFragmentManager == null) {
            return;
        }

        // Show the navigation suggestion dialog.
        AutoclickNavigationSuggestionDialogFragment.newInstance()
                .show(mFragmentManager,
                        AutoclickNavigationSuggestionDialogFragment.class.getName());
    }

    /*
     * Navigation bar modes:
     *  0 - 3-button navigation
     *  1 - 2-button navigation (deprecated on newer Android versions)
     *  2 - Gesture navigation (no visible buttons)
     *
     * Returns true if the current navigation mode is gesture-based.
     */
    private boolean isGestureNavigationEnabled() {
        int navigationMode = Settings.Secure.getInt(mContentResolver, NAVIGATION_MODE, -1);
        return navigationMode == 2;
    }

    /**
     * Checks if the current device is a laptop device.
     */
    private boolean isLaptopDevice() {
        return mContext.getPackageManager().hasSystemFeature(PackageManager.FEATURE_PC);
    }
}
