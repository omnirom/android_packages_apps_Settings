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

import static com.android.settings.gestures.DoubleTapPowerSettingsUtils.DOUBLE_TAP_POWER_BUTTON_GESTURE_ENABLED_URI;

import android.app.role.OnRoleHoldersChangedListener;
import android.app.role.RoleManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.UserHandle;
import android.service.quickaccesswallet.QuickAccessWalletClient;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.core.BasePreferenceController;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnStart;
import com.android.settingslib.core.lifecycle.events.OnStop;
import com.android.settingslib.widget.FooterPreference;

public class DoubleTapPowerWalletFooterPreferenceController extends BasePreferenceController
        implements LifecycleObserver, OnStart, OnStop {

    private final Handler mHandler = new Handler(Looper.getMainLooper());

    @Nullable
    private final RoleManager mRoleManager;
    @NonNull private QuickAccessWalletClient mQuickAccessWalletClient;
    @Nullable private FooterPreference mPreference;
    private final ContentObserver mSettingsObserver =
            new ContentObserver(mHandler) {
                @Override
                public void onChange(boolean selfChange, @Nullable Uri uri) {
                    if (mPreference != null) {
                        mPreference.setEnabled(
                                DoubleTapPowerSettingsUtils.isDoubleTapPowerButtonGestureEnabled(
                                        mContext)
                        );
                    }
                }
            };
    private final OnRoleHoldersChangedListener mOnRoleHoldersChangedListener = (roleName, user) -> {
        if (!roleName.equals(RoleManager.ROLE_WALLET) || mPreference == null
                || user.getIdentifier() != UserHandle.myUserId()) {
            return;
        }
        mQuickAccessWalletClient = QuickAccessWalletClient.create(mContext);
        mPreference.setVisible(!mQuickAccessWalletClient.isWalletServiceAvailable());
    };

    public DoubleTapPowerWalletFooterPreferenceController(
            @NonNull Context context, @NonNull String preferenceKey) {
        super(context, preferenceKey);
        mRoleManager = mContext.getSystemService(RoleManager.class);
        mQuickAccessWalletClient = QuickAccessWalletClient.create(context);
    }

    @VisibleForTesting
    public DoubleTapPowerWalletFooterPreferenceController(
            @NonNull Context context, @NonNull String preferenceKey,
            @NonNull QuickAccessWalletClient quickAccessWalletClient) {
        super(context, preferenceKey);
        mRoleManager = mContext.getSystemService(RoleManager.class);
        mQuickAccessWalletClient = quickAccessWalletClient;
    }

    @Override
    public int getAvailabilityStatus() {
        if (!DoubleTapPowerSettingsUtils
                .isMultiTargetDoubleTapPowerButtonGestureAvailable(mContext)) {
            return UNSUPPORTED_ON_DEVICE;
        }
        return DoubleTapPowerSettingsUtils.isDoubleTapPowerButtonGestureEnabled(mContext)
                ? AVAILABLE_UNSEARCHABLE
                : DISABLED_DEPENDENT_SETTING;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreference = screen.findPreference(getPreferenceKey());
        if (mPreference != null) {
            mPreference.setLearnMoreText(mContext.getString(
                    com.android.settings.R.string.double_tap_power_wallet_footer_learn_more_text));
            mPreference.setLearnMoreAction(v -> {
                final Intent intent = new Intent(Intent.ACTION_MANAGE_DEFAULT_APP);
                intent.putExtra(Intent.EXTRA_ROLE_NAME, RoleManager.ROLE_WALLET);
                mContext.startActivity(intent);
            });
        }
    }

    @Override
    public void updateState(@NonNull Preference preference) {
        super.updateState(preference);
        preference.setVisible(!mQuickAccessWalletClient.isWalletServiceAvailable());
        preference.setEnabled(
                DoubleTapPowerSettingsUtils.isDoubleTapPowerButtonGestureEnabled(mContext)
        );
    }

    @Override
    public void onStart() {
        // Update to the latest wallet client when the controller starts again in case the wallet
        // role holder might change between onStop() and onStart() time period.
        mQuickAccessWalletClient = QuickAccessWalletClient.create(mContext);
        final ContentResolver resolver = mContext.getContentResolver();
        resolver.registerContentObserver(
                DOUBLE_TAP_POWER_BUTTON_GESTURE_ENABLED_URI, true, mSettingsObserver);
        if (mRoleManager != null) {
            mRoleManager.addOnRoleHoldersChangedListenerAsUser(mContext.getMainExecutor(),
                    mOnRoleHoldersChangedListener, UserHandle.of(UserHandle.myUserId()));
        }
    }

    @Override
    public void onStop() {
        DoubleTapPowerSettingsUtils.unregisterObserver(mContext, mSettingsObserver);
        if (mRoleManager != null) {
            mRoleManager.removeOnRoleHoldersChangedListenerAsUser(mOnRoleHoldersChangedListener,
                    UserHandle.of(UserHandle.myUserId()));
        }
    }
}
