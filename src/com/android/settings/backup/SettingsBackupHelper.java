/**
 * Copyright (C) 2019 The Android Open Source Project
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

package com.android.settings.backup;

import android.app.AppGlobals;
import android.app.backup.BackupAgentHelper;
import android.util.Log;

import com.android.settings.applications.appcompat.UserAspectRatioBackupHelper;
import com.android.settings.applications.appcompat.UserAspectRatioManager;
import com.android.settings.flags.Flags;
import com.android.settings.i18n.RegionalCustomizationFeatureProvider;
import com.android.settings.inputmethod.TouchpadThreeFingerTapLaunchingAppBackupHelper;
import com.android.settings.inputmethod.TouchpadThreeFingerTapUtils;
import com.android.settings.onboarding.OnboardingFeatureProvider;
import com.android.settings.overlay.FeatureFactory;
import com.android.settings.shortcut.ShortcutsUpdater;
import com.android.settingslib.datastore.BackupRestoreStorageManager;

/** Backup agent for Settings APK */
public class SettingsBackupHelper extends BackupAgentHelper {
    private static final String TAG = "SettingsBackupHelper";

    public static final String SOUND_BACKUP_HELPER = "SoundSettingsBackup";
    public static final String ACCESSIBILITY_APPEARANCE_BACKUP_HELPER =
            "AccessibilityAppearanceSettingsBackup";
    public static final String MOVEMENT_BACKUP_HELPER = "MovementSettingsBackup";
    private static final String USER_ASPECT_RATIO_BACKUP_HELPER = "UserAspectRatioSettingsBackup";
    private static final String TOUCHPAD_THREE_FINGER_TAP_LAUNCHING_APP_BACKUP_HELPER =
            "TouchpadThreeFingerTapLaunchingAppBackup";

    @Override
    public void onCreate() {
        super.onCreate();
        BackupRestoreStorageManager.getInstance(this).addBackupAgentHelpers(this);
        OnboardingFeatureProvider onboardingFeatureProvider =
                FeatureFactory.getFeatureFactory().getOnboardingFeatureProvider();
        RegionalCustomizationFeatureProvider regionalCustomizationFeatureProvider =
                FeatureFactory.getFeatureFactory().getRegionalCustomizationFeatureProvider();

        if (Flags.enableSoundBackup()) {
            if (onboardingFeatureProvider != null) {
                addHelper(SOUND_BACKUP_HELPER, onboardingFeatureProvider.
                        getSoundBackupHelper(this, this.getBackupRestoreEventLogger()));
            }
        }

        if (Flags.accessibilityAppearanceSettingsBackupEnabled()) {
            if (onboardingFeatureProvider != null) {
                addHelper(ACCESSIBILITY_APPEARANCE_BACKUP_HELPER,
                        onboardingFeatureProvider.getAccessibilityAppearanceBackupHelper(
                            this, this.getBackupRestoreEventLogger()));
            }
        }

        if (Flags.movementSettingsBackupEnabled()) {
            if (regionalCustomizationFeatureProvider != null) {
                addHelper(MOVEMENT_BACKUP_HELPER,
                        regionalCustomizationFeatureProvider.getMovementBackupHelper(
                            this, this.getBackupRestoreEventLogger()));
            }
        }

        if (UserAspectRatioManager.isFeatureEnabled(getApplicationContext())) {
            // Since the aconfig flag below is read-only, this class would not compile, and tests
            // would fail to find the class, even if they are testing only code beyond the
            // flag-guarded code.
            final UserAspectRatioBackupHelper userAspectRatioBackupHelper =
                    new UserAspectRatioBackupHelper(this, AppGlobals.getPackageManager(),
                            getBackupRestoreEventLogger());
            if (com.android.window.flags.Flags.backupAndRestoreForUserAspectRatioSettings()) {
                addHelper(USER_ASPECT_RATIO_BACKUP_HELPER, userAspectRatioBackupHelper);
            }
        }

        if (Flags.threeFingerTapAppLaunch()) {
            String key = TouchpadThreeFingerTapUtils.getFileToBackup();
            final TouchpadThreeFingerTapLaunchingAppBackupHelper launchingAppBackupHelper =
                    new TouchpadThreeFingerTapLaunchingAppBackupHelper(this, key);
            addHelper(
                    TOUCHPAD_THREE_FINGER_TAP_LAUNCHING_APP_BACKUP_HELPER,
                    launchingAppBackupHelper);
        }
    }

    @Override
    public void onRestoreFinished() {
        super.onRestoreFinished();
        BackupRestoreStorageManager.getInstance(this).onRestoreFinished();
        try {
            ShortcutsUpdater.updatePinnedShortcuts(this);
        } catch (Exception e) {
            Log.e(TAG, "Error updating shortcuts after restoring backup", e);
        }
    }
}
