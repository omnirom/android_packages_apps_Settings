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

package com.android.settings.gestures;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnResume;
import com.android.settingslib.core.lifecycle.events.OnStart;
import com.android.settingslib.core.lifecycle.events.OnStop;
import com.android.settingslib.widget.CardPreference;

import kotlin.Unit;

public class DoubleTapPowerLockscreenTipPreferenceController extends BasePreferenceController
        implements LifecycleObserver, OnStart, OnStop, OnResume {

    private static final String TAG = "DoubleTapPowerLockscreenTip";

    /** URI to query current Keyguard Quick Affordance selections. */
    static final Uri KEYGUARD_QUICK_AFFORDANCE_SELECTIONS_URI = new Uri.Builder()
            .scheme(ContentResolver.SCHEME_CONTENT)
            .authority("com.android.systemui.customization")
            .appendPath("lockscreen_quickaffordance")
            .appendPath("selections")
            .build();

    /** Name of Cursor column containing Keyguard Quick Affordance selections. */
    static final String AFFORDANCE_NAME_COLUMN = "affordance_name";
    static final String CAMERA_KEYGUARD_QUICK_AFFORDANCE_NAME = "Camera";
    static final String WALLET_KEYGUARD_QUICK_AFFORDANCE_NAME = "Wallet";
    static final String SLOT_ID_COLUMN = "slot_id";

    @Nullable private CardPreference mPreference;
    private boolean mHasBeenDismissed = false;

    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private final ContentObserver mSettingsObserver =
            new ContentObserver(mHandler) {
                @Override
                public void onChange(boolean selfChange, @Nullable Uri uri) {
                    if (mPreference == null || uri == null) {
                        return;
                    }
                    if (uri.equals(
                            DoubleTapPowerSettingsUtils
                                    .DOUBLE_TAP_POWER_BUTTON_GESTURE_ENABLED_URI)) {
                        if (DoubleTapPowerSettingsUtils.isDoubleTapPowerButtonGestureEnabled(
                                mContext)) {
                            // If the gesture is enabled, check whether tip needs to shown
                            updateState(mPreference);
                        } else {
                            // If the gesture is disabled, hide tip
                            mPreference.setVisible(false);
                        }
                    } else if (uri.equals(
                            DoubleTapPowerSettingsUtils
                                    .DOUBLE_TAP_POWER_BUTTON_GESTURE_TARGET_ACTION_URI)) {
                        updateState(mPreference);
                    }
                }
            };

    public DoubleTapPowerLockscreenTipPreferenceController(
            @NonNull Context context,
            @NonNull String preferenceKey) {
        super(context, preferenceKey);
    }

    @Override
    public void onStart() {
        DoubleTapPowerSettingsUtils.registerObserver(mContext, mSettingsObserver);
    }

    @Override
    public void onStop() {
        DoubleTapPowerSettingsUtils.unregisterObserver(mContext, mSettingsObserver);
    }

    @Override
    public void onResume() {
        if (mPreference != null) {
            updateState(mPreference);
        }
    }


    @Override
    public int getAvailabilityStatus() {
        return DoubleTapPowerSettingsUtils
                .isMultiTargetDoubleTapPowerButtonGestureAvailable(mContext)
                ? AVAILABLE_UNSEARCHABLE
                : UNSUPPORTED_ON_DEVICE;
    }

    @Override
    public void displayPreference(@NonNull PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreference = screen.findPreference(getPreferenceKey());
        updatePreference();
    }

    @Override
    public void updateState(@NonNull Preference preference) {
        super.updateState(preference);

        if (!DoubleTapPowerSettingsUtils.isDoubleTapPowerButtonGestureEnabled(mContext)
                || mHasBeenDismissed) {
            preference.setVisible(false);
            return;
        }

        Pair<String, String> targetActionInLockscreenShortcutPair =
                getTargetActionIfLockScreenShortcut(mContext);
        if (targetActionInLockscreenShortcutPair == null) {
            preference.setVisible(false);
            return;
        }
        String targetActionInLockscreenShortcut = targetActionInLockscreenShortcutPair.first;
        if (preference instanceof CardPreference) {
            Log.i(TAG, "Target action is also lockscreen shorcut. Showing suggestion card.");
            preference.setSummary(mContext.getString(
                            R.string.double_tap_power_lockscreen_shortcut_tip_description,
                            targetActionInLockscreenShortcut
                    ));
            preference.setVisible(true);
        }
        updatePreference();
    }

    /**
     * Dismisses the Preference
     *
     * @param preference Preference
     */
    @VisibleForTesting
    public void onDismiss(@NonNull Preference preference) {
        preference.setVisible(false);
        mHasBeenDismissed = true;
    }

    private void updatePreference() {
        if (mPreference != null) {
            Pair<String, String> targetActionInLockscreenShortcutPair =
                    getTargetActionIfLockScreenShortcut(mContext);
            mPreference.setAdditionalAction(
                    com.android.settingslib.widget
                            .theme.R.drawable.settingslib_expressive_icon_close,
                    mContext.getString(
                            com.android.settingslib.widget
                                    .theme.R.string.settingslib_dismiss_button_content_description),
                    preference -> {
                        onDismiss(preference);
                        return Unit.INSTANCE;
                    }
            );
            mPreference.setOnPreferenceClickListener(preference -> {
                final Intent intent = new Intent(Intent.ACTION_SET_WALLPAPER);
                intent.putExtra("destination", "quick_affordances");
                // Informs destination it was launched within Settings
                intent.putExtra(
                        "com.android.wallpaper.LAUNCH_SOURCE",
                        "app_launched_settings"
                );
                if (targetActionInLockscreenShortcutPair != null) {
                    @Nullable String slotId = targetActionInLockscreenShortcutPair.second;
                    if (slotId != null) {
                        intent.putExtra(SLOT_ID_COLUMN, slotId);
                    }
                }
                final String packageName =
                        mContext.getString(R.string.config_wallpaper_picker_package);
                if (!TextUtils.isEmpty(packageName)) {
                    intent.setPackage(packageName);
                }
                mContext.startActivity(intent);
                return true;
            });
        }
    }

    @Nullable
    private static Pair<String, String> getTargetActionIfLockScreenShortcut(
            @NonNull Context context) {
        String currentTargetActionName =
                DoubleTapPowerSettingsUtils
                        .isDoubleTapPowerButtonGestureForCameraLaunchEnabled(context)
                ? CAMERA_KEYGUARD_QUICK_AFFORDANCE_NAME : WALLET_KEYGUARD_QUICK_AFFORDANCE_NAME;
        Log.i(TAG, "Current target action name " + currentTargetActionName);
        try (Cursor cursor = context.getContentResolver().query(
                KEYGUARD_QUICK_AFFORDANCE_SELECTIONS_URI,
                null,
                null,
                null)) {
            if (cursor == null) {
                Log.w(TAG, "Keyguard Quick Affordance Cursor was null!");
                return null;
            }

            final int columnIndex = cursor.getColumnIndex(AFFORDANCE_NAME_COLUMN);
            final int slotIdColumnIndex = cursor.getColumnIndex(SLOT_ID_COLUMN);
            if (columnIndex == -1 || slotIdColumnIndex == -1) {
                Log.w(TAG, "Keyguard Quick Affordance Cursor doesn't contain \""
                        + AFFORDANCE_NAME_COLUMN + "\" column!");
                return null;
            }

            while (cursor.moveToNext()) {
                final String affordanceName = cursor.getString(columnIndex);
                if (TextUtils.equals(affordanceName, currentTargetActionName)) {
                    final String slotId = cursor.getString(slotIdColumnIndex);
                    return Pair.create(affordanceName, slotId);
                }
            }
            return null;
        } catch (Exception e) {
            Log.e(TAG, "Exception while querying Keyguard Quick Affordance content provider", e);
            return null;
        }
    }
}
