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

import static android.content.Context.MODE_PRIVATE;
import static android.view.accessibility.AccessibilityManager.AUTOCLICK_CURSOR_AREA_SIZE_MAX;
import static android.view.accessibility.AccessibilityManager.AUTOCLICK_CURSOR_AREA_SIZE_MIN;

import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.accessibility.AccessibilityManager;
import android.widget.RadioGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.server.accessibility.Flags;
import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;

import com.google.common.collect.ImmutableBiMap;

/** Controller class that controls accessibility autoclick cursor area size settings. */
public class ToggleAutoclickCursorAreaSizeController extends BasePreferenceController
        implements LifecycleObserver, SharedPreferences.OnSharedPreferenceChangeListener {

    public static final String TAG = ToggleAutoclickCursorAreaSizeController.class.getSimpleName();

    public final ImmutableBiMap<Integer, Integer> RADIO_BUTTON_ID_TO_CURSOR_SIZE =
            new ImmutableBiMap.Builder<Integer, Integer>()
                .put(R.id.autoclick_cursor_area_size_value_extra_large, 100)
                .put(R.id.autoclick_cursor_area_size_value_large, 80)
                .put(R.id.autoclick_cursor_area_size_value_default, 60)
                .put(R.id.autoclick_cursor_area_size_value_small, 40)
                .put(R.id.autoclick_cursor_area_size_value_extra_small, 20)
                .buildOrThrow();

    private final ContentResolver mContentResolver;
    private final SharedPreferences mSharedPreferences;
    private Preference mPreference;
    protected AlertDialog mAlertDialog;

    public ToggleAutoclickCursorAreaSizeController(@NonNull Context context,
            @NonNull String preferenceKey) {
        super(context, preferenceKey);
        mContentResolver = context.getContentResolver();
        mSharedPreferences = context.getSharedPreferences(context.getPackageName(), MODE_PRIVATE);
        constructDialog(context);
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    public void onStart() {
        if (mSharedPreferences != null) {
            mSharedPreferences.registerOnSharedPreferenceChangeListener(this);
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    public void onStop() {
        if (mSharedPreferences != null) {
            mSharedPreferences.unregisterOnSharedPreferenceChangeListener(this);
        }
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);

        mPreference = screen.findPreference(getPreferenceKey());
    }

    protected void constructDialog(Context context) {
        mAlertDialog = new AlertDialog.Builder(context)
                .setView(R.layout.dialog_autoclick_cursor_area_size)
                .setPositiveButton(android.R.string.ok,
                        (dialog, which) -> {
                            RadioGroup radioGroup =
                                    mAlertDialog.findViewById(
                                            R.id.autoclick_cursor_area_size_value_group);
                            int checkedRadioButtonId = radioGroup.getCheckedRadioButtonId();
                            int size = RADIO_BUTTON_ID_TO_CURSOR_SIZE.get(checkedRadioButtonId);
                            updateAutoclickCursorAreaSize(size);
                        })
                .setNegativeButton(android.R.string.cancel, (dialog, which) -> dialog.dismiss())
                .create();
        mAlertDialog.setOnShowListener(dialog -> {
            initStateBasedOnSize();
        });
    }

    private void initStateBasedOnSize() {
        RadioGroup cannedValueRadioGroup = mAlertDialog.findViewById(
                    R.id.autoclick_cursor_area_size_value_group);
        int autoclickCursordefaultSize = validateSize(Settings.Secure.getInt(mContentResolver,
                Settings.Secure.ACCESSIBILITY_AUTOCLICK_CURSOR_AREA_SIZE,
                AccessibilityManager.AUTOCLICK_CURSOR_AREA_SIZE_DEFAULT));

        int radioButtonId = RADIO_BUTTON_ID_TO_CURSOR_SIZE.inverse()
                .get(autoclickCursordefaultSize);

        cannedValueRadioGroup.check(radioButtonId);
    }

    @Override
    public boolean handlePreferenceTreeClick(@NonNull Preference preference) {
        if (!TextUtils.equals(preference.getKey(), getPreferenceKey())) {
            return false;
        }

        if (mAlertDialog != null) {
            mAlertDialog.show();
        }

        return true;
    }

    @Override
    public int getAvailabilityStatus() {
        return Flags.enableAutoclickIndicator() ? AVAILABLE : CONDITIONALLY_UNAVAILABLE;
    }

    @Override
    public void onSharedPreferenceChanged(
            @NonNull SharedPreferences sharedPreferences, @Nullable String key) {
        // TODO(b/383901288): Update slider if interested preference has changed.
    }

    @Override
    public CharSequence getSummary() {
        int autoclickCursorSize = validateSize(Settings.Secure.getInt(mContentResolver,
                Settings.Secure.ACCESSIBILITY_AUTOCLICK_CURSOR_AREA_SIZE,
                AccessibilityManager.AUTOCLICK_CURSOR_AREA_SIZE_DEFAULT));
        int summaryStringId;
        switch (autoclickCursorSize) {
            case 100 -> summaryStringId =
                    R.string.autoclick_cursor_area_size_dialog_option_extra_large;
            case 80 -> summaryStringId = R.string.autoclick_cursor_area_size_dialog_option_large;
            case 40 -> summaryStringId = R.string.autoclick_cursor_area_size_dialog_option_small;
            case 20 -> summaryStringId =
                    R.string.autoclick_cursor_area_size_dialog_option_extra_small;
            default -> summaryStringId = R.string.autoclick_cursor_area_size_dialog_option_default;
        }

        return mContext.getString(summaryStringId);
    }

    /** Updates autoclick cursor area size. */
    public void updateAutoclickCursorAreaSize(int size) {
        Settings.Secure.putInt(
                mContentResolver,
                Settings.Secure.ACCESSIBILITY_AUTOCLICK_CURSOR_AREA_SIZE,
                validateSize(size));
        refreshSummary(mPreference);
    }

    private int validateSize(int size) {
        size = Math.min(size, AUTOCLICK_CURSOR_AREA_SIZE_MAX);
        size = Math.max(size, AUTOCLICK_CURSOR_AREA_SIZE_MIN);
        return size;
    }
}
