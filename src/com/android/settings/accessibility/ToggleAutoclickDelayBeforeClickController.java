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

package com.android.settings.accessibility;

import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.accessibility.AccessibilityManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.server.accessibility.Flags;
import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;

/** Controller class that controls accessibility autoclick delay time settings. */
public class ToggleAutoclickDelayBeforeClickController
        extends BasePreferenceController implements DefaultLifecycleObserver {

    public static final String TAG =
            ToggleAutoclickDelayBeforeClickController.class.getSimpleName();

    static final Uri ACCESSIBILITY_AUTOCLICK_DELAY_URI =
            Settings.Secure.getUriFor(Settings.Secure.ACCESSIBILITY_AUTOCLICK_DELAY);

    @Nullable private FragmentManager mFragmentManager;
    @Nullable private Preference mPreference;

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

    public ToggleAutoclickDelayBeforeClickController(@NonNull Context context,
            @NonNull String preferenceKey) {
        super(context, preferenceKey);

        if (Flags.enableAutoclickIndicator()) {
            resetAutoclickDelayValueIfNecessary();
        }
    }

    @Override
    public void displayPreference(@NonNull PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreference = screen.findPreference(getPreferenceKey());
    }

    @Override
    public void onStart(@NonNull LifecycleOwner owner) {
        mContext.getContentResolver().registerContentObserver(
                ACCESSIBILITY_AUTOCLICK_DELAY_URI,
                /* notifyForDescendants= */ false,
                mSettingsObserver);
    }

    @Override
    public void onStop(@NonNull LifecycleOwner owner) {
        mContext.getContentResolver().unregisterContentObserver(mSettingsObserver);
    }

    public void setFragment(@NonNull Fragment fragment) {
        mFragmentManager = fragment.getChildFragmentManager();
    }

    @Override
    public boolean handlePreferenceTreeClick(@NonNull Preference preference) {
        if (!TextUtils.equals(preference.getKey(), getPreferenceKey())
                || mFragmentManager == null) {
            return false;
        }

        AutoclickDelayDialogFragment.newInstance().show(mFragmentManager, TAG);
        return true;
    }

    @Override
    public int getAvailabilityStatus() {
        return Flags.enableAutoclickIndicator() ? AVAILABLE : CONDITIONALLY_UNAVAILABLE;
    }

    @Override
    public @NonNull CharSequence getSummary() {
        final int autoclickDelay = Settings.Secure.getInt(mContext.getContentResolver(),
                Settings.Secure.ACCESSIBILITY_AUTOCLICK_DELAY,
                AccessibilityManager.AUTOCLICK_DELAY_WITH_INDICATOR_DEFAULT);
        return AutoclickUtils.getAutoclickDelaySummary(
                        mContext, R.string.accessibility_autoclick_delay_unit_second,
                        autoclickDelay);
    }

    private void resetAutoclickDelayValueIfNecessary() {
        int autoclickDelay = Settings.Secure.getInt(mContext.getContentResolver(),
                Settings.Secure.ACCESSIBILITY_AUTOCLICK_DELAY,
                AccessibilityManager.AUTOCLICK_DELAY_WITH_INDICATOR_DEFAULT);

        // If the flag is just enabled, the delay settings may be 0.
        // Reset the delay value to default in this case.
        if (autoclickDelay < AutoclickUtils.MIN_AUTOCLICK_DELAY_MS) {
            autoclickDelay = AccessibilityManager.AUTOCLICK_DELAY_WITH_INDICATOR_DEFAULT;
            Settings.Secure.putInt(
                    mContext.getContentResolver(),
                    Settings.Secure.ACCESSIBILITY_AUTOCLICK_DELAY,
                    autoclickDelay);
        }
    }
}
