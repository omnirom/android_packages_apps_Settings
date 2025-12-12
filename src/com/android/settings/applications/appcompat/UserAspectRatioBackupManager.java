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

package com.android.settings.applications.appcompat;

import static android.content.pm.PackageManager.USER_MIN_ASPECT_RATIO_16_9;
import static android.content.pm.PackageManager.USER_MIN_ASPECT_RATIO_3_2;
import static android.content.pm.PackageManager.USER_MIN_ASPECT_RATIO_4_3;
import static android.content.pm.PackageManager.USER_MIN_ASPECT_RATIO_APP_DEFAULT;
import static android.content.pm.PackageManager.USER_MIN_ASPECT_RATIO_FULLSCREEN;
import static android.content.pm.PackageManager.USER_MIN_ASPECT_RATIO_SPLIT_SCREEN;
import static android.content.pm.PackageManager.USER_MIN_ASPECT_RATIO_UNSET;
import static android.hardware.display.DisplayManager.DISPLAY_CATEGORY_ALL_INCLUDING_DISABLED;

import static com.android.settings.applications.appcompat.UserAspectRatioBackupHelper.KEY_USER_ASPECT_RATIO;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.annotation.UserIdInt;
import android.app.backup.BackupRestoreEventLogger;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.IPackageManager;
import android.content.pm.PackageManager;
import android.graphics.Rect;
import android.hardware.display.DisplayManager;
import android.os.Handler;
import android.os.RemoteException;
import android.os.UserHandle;
import android.util.Slog;
import android.view.Display;
import android.view.DisplayInfo;

import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.content.PackageMonitor;
import com.android.settings.R;
import com.android.window.flags.Flags;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.InstantSource;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/** Manager class for performing Backup & Restore for per-app user aspect ratio override. */
class UserAspectRatioBackupManager {
    private static final String TAG = "UserAspRatioBackupMngr";   // must be < 23 chars
    private static final boolean DEBUG = false;

    @BackupRestoreEventLogger.BackupRestoreError
    private static final String ERROR_SERIALIZE_FAILED = "serialize_failed";
    @BackupRestoreEventLogger.BackupRestoreError
    private static final String ERROR_QUERY_ASPECT_RATIO_FAILED =
            "backup_query_packages_failed";
    @BackupRestoreEventLogger.BackupRestoreError
    private static final String ERROR_DESERIALIZE_FAILED = "deserialize_failed";
    @BackupRestoreEventLogger.BackupRestoreError
    private static final String ERROR_TYPE_CAST_FAILED = "type_cast_failed";
    @BackupRestoreEventLogger.BackupRestoreError
    private static final String ERROR_QUERY_PACKAGE_FAILED = "query_package_failed";
    @BackupRestoreEventLogger.BackupRestoreError
    private static final String ERROR_ASPECT_RATIO_FAILED = "aspect_ratio_failed";

    @NonNull
    private final Context mContext;
    @NonNull
    private final IPackageManager mIPackageManager;
    @NonNull
    private final PackageManager mPackageManager;
    @VisibleForTesting
    @NonNull
    private Set<Integer> mAvailableUserMinAspectRatioSet;
    @NonNull
    private final UserAspectRatioRestoreStorage mStorage;

    @UserIdInt
    private final int mUserId;
    @NonNull
    private final BackupRestoreEventLogger mLogger;

    private final Map<Integer, Float> mUserAspectRatiosPerSettingMap = new HashMap<>();

    /**
     * Helper to monitor package states for the purpose of restoring user aspect ratios.
     *
     * <p>This package monitor also keeps the backed-up and restored data up-to-date when a package
     * is removed.
     */
    @VisibleForTesting
    final PackageMonitor mPackageMonitor = new PackageMonitor() {
        @Override
        public void onPackageAdded(String packageName, int uid) {
            final int aspectRatio = mStorage.getAndRemoveUserAspectRatioForPackage(packageName);
            if (aspectRatio != USER_MIN_ASPECT_RATIO_UNSET) {
                checkExistingAspectRatioAndApplyRestore(packageName, aspectRatio);
            }
        }

        @Override
        public void onPackageRemoved(@NonNull String packageName, int uid) {
            // Just in case, but the user aspect ratio should be restored and removed as soon as an
            // app is installed. Therefore there should not be anything to clean up here.
            mStorage.getAndRemoveUserAspectRatioForPackage(packageName);
        }
    };

    public UserAspectRatioBackupManager(@NonNull Context context,
            @NonNull IPackageManager iPackageManager, @NonNull PackageManager packageManager,
            @NonNull BackupRestoreEventLogger logger, @NonNull Handler handler,
            @NonNull InstantSource instantSource) {
        mContext = context;
        mIPackageManager = iPackageManager;
        mPackageManager = packageManager;
        mUserId = mPackageManager.getUserId();
        mStorage = new UserAspectRatioRestoreStorage(context, mUserId, instantSource);
        mLogger = logger;

        if (!Flags.restoreUserAspectRatioSettingsUsingService()) {
            mPackageMonitor.register(context, UserHandle.of(UserHandle.USER_ALL), handler);
        }

        populateAvailableUserAspectRatioSettingOptions(mContext.getResources().getIntArray(
                R.array.config_userAspectRatioOverrideValues));
    }

    @VisibleForTesting
    void populateAvailableUserAspectRatioSettingOptions(
            @NonNull int[] userAspectRatioResourceValues) {
        mAvailableUserMinAspectRatioSet = Arrays.stream(userAspectRatioResourceValues).boxed()
                .collect(Collectors.toSet());
        // App Default is not in the set above, but is always offered. Users can also specify this
        // value, for example if there is an OEM override to some other value.
        mAvailableUserMinAspectRatioSet.add(USER_MIN_ASPECT_RATIO_APP_DEFAULT);

        final Rect displaySize = getSizeOfLargestDisplay();
        if (displaySize == null || displaySize.isEmpty()) {
            return;
        }
        // Always >= 1.
        final float currentLargestInternalDisplayAspectRatio = getAspectRatioForRect(displaySize);

        mUserAspectRatiosPerSettingMap.clear();
        for (int aspectRatioEnum : userAspectRatioResourceValues) {
            float aspectRatio = getAspectRatioForSetting(aspectRatioEnum,
                    currentLargestInternalDisplayAspectRatio);
            // Values that cannot be used as target aspect ratios will return -1.
            if (aspectRatio > 0) {
                mUserAspectRatiosPerSettingMap.put(aspectRatioEnum, aspectRatio);
            }
        }
    }

    /**
     * Returns the real aspect ratio that the given setting represents.
     *
     * <p>Possible values:
     * <ul>
     *    <li> Concrete user aspect ratios (e.g. 16/9): value >= 1.
     *    <li> Split screen aspect ratio: displayAspectRatio / 2, also >= 1.
     *    <li> Fullscreen: 0.5
     *    <li> Settings that will not be chosen: -1.
     * </ul>
     */
    private float getAspectRatioForSetting(int userAspectRatioSetting, float displayAspectRatio) {
        return switch (userAspectRatioSetting) {
            case USER_MIN_ASPECT_RATIO_3_2 ->  3 / 2f;
            case USER_MIN_ASPECT_RATIO_4_3 -> 4 / 3f;
            case USER_MIN_ASPECT_RATIO_16_9 -> 16 / 9f;
            case USER_MIN_ASPECT_RATIO_SPLIT_SCREEN -> makeFloatHigherOrEqualTo1(
                    displayAspectRatio / 2);
            // Fullscreen should always result in the largest area, this means the smallest aspect
            // ratio. Special value of 0.5f is used, to allow user aspect ratio setting of 1 in the
            // future.
            case USER_MIN_ASPECT_RATIO_FULLSCREEN -> 0.5f;
            // Other values, like APP_DEFAULT, will not be chosen as "closest larger".
            default -> -1;
        };
    }

    private static float makeFloatHigherOrEqualTo1(float number) {
        return number < 1 ? 1f / number : number;
    }

    /**
     * Returns the per-app user aspect ratio settings to be backed up as a data-blob.
     */
    @Nullable
    public byte[] getBackupPayload() {
        final Map<String, Integer> aspectRatioStates = getAllUserAspectRatios(mLogger);

        if (DEBUG) {
            Slog.d(TAG, "User aspect ratio states to backup =" + aspectRatioStates);
        }

        try (ByteArrayOutputStream bos = new ByteArrayOutputStream(); ObjectOutputStream oos =
                new ObjectOutputStream(bos)) {
            oos.writeObject(aspectRatioStates);
            return bos.toByteArray();
        } catch (Exception e) {
            mLogger.logItemsBackupFailed(KEY_USER_ASPECT_RATIO, /* count= */ 1,
                    ERROR_SERIALIZE_FAILED);
            Slog.e(TAG, "Could not serialize payload.", e);
            return null;
        }
    }

    @NonNull
    private Map<String, Integer> getAllUserAspectRatios(@NonNull BackupRestoreEventLogger logger) {
        final List<ApplicationInfo> appList = mPackageManager.getInstalledApplications(
                PackageManager.MATCH_DISABLED_COMPONENTS
                        | PackageManager.MATCH_DISABLED_UNTIL_USED_COMPONENTS);

        final HashMap<String, Integer> aspectRatioStates = new HashMap<>();
        for (ApplicationInfo app : appList) {
            try {
                final int aspectRatio = mIPackageManager.getUserMinAspectRatio(app.packageName,
                        mUserId);
                if (aspectRatio != USER_MIN_ASPECT_RATIO_UNSET) {
                    aspectRatioStates.put(app.packageName, aspectRatio);
                }
            } catch (RemoteException e) {
                logger.logItemsBackupFailed(KEY_USER_ASPECT_RATIO, /* count= */ 1,
                        ERROR_QUERY_ASPECT_RATIO_FAILED);
                Slog.e(TAG, "Could not get user aspect ratio for " + app.packageName + ".", e);
            }
        }
        return aspectRatioStates;
    }

    @Nullable
    private Map<String, Integer> readFromByteArray(@NonNull byte[] payload) {
        Object readObject = null;
        try (ByteArrayInputStream bis = new ByteArrayInputStream(payload); ObjectInputStream ois =
                new ObjectInputStream(bis)) {
            readObject = ois.readObject();
            // Cannot check generic type, therefore ClassCastException will be thrown and logged if
            // type is incorrect.
            return (Map<String, Integer>) readObject;
        } catch (ClassCastException e) {
            mLogger.logItemsRestoreFailed(KEY_USER_ASPECT_RATIO, /* count= */ 1,
                    ERROR_TYPE_CAST_FAILED);
            Slog.e(TAG, "Could not cast to Map<String, Integer>: " + readObject, e);
            return null;
        } catch (IOException | ClassNotFoundException e) {
            mLogger.logItemsRestoreFailed(KEY_USER_ASPECT_RATIO, /* count= */ 1,
                    ERROR_DESERIALIZE_FAILED);
            Slog.e(TAG, "Could not read payload for backup", e);
            return null;
        }
    }

    /**
     * Restores the per-app user aspect ratio settings that were previously backed up.
     *
     * <p>This method will parse the input data blob and restore the aspect ratio settings for apps
     * which are present on the device. It will stage the aspect ratio data for the apps which are
     * not installed at the time this is called, to be referenced later when the app is installed.
     */
    public void stageAndApplyRestoredPayload(@NonNull byte[] payload) {
        final Map<String, Integer> userAspectRatioStates = readFromByteArray(payload);
        if (userAspectRatioStates == null || userAspectRatioStates.isEmpty()) {
            Slog.d(TAG, "StageAndApplyRestoredPayload: payload is empty.");
            return;
        }
        if (DEBUG) {
            Slog.d(TAG, "StageAndApplyRestoredPayload user=" + mUserId + " payload="
                    + new String(payload, StandardCharsets.UTF_8));
        }

        for (String pkgName : userAspectRatioStates.keySet()) {
            final Integer aspectRatio = userAspectRatioStates.get(pkgName);
            if (aspectRatio == null) {
                Slog.d(TAG, "StageAndApplyRestoredPayload null aspect ratio for package: "
                        + pkgName);
                continue;
            }
            // If restoring via service is enabled, try to set the user aspect ratio even if the
            // package is not installed - this would be restored later by a system service.
            if (isPackageInstalled(pkgName) || Flags.restoreUserAspectRatioSettingsUsingService()) {
                Slog.d(TAG, "StageAndApplyRestoredPayload Found package: " + pkgName);
                checkExistingAspectRatioAndApplyRestore(pkgName, aspectRatio);
            } else {
                Slog.d(TAG, "StageAndApplyRestoredPayload package not installed: " + pkgName);
                mStorage.storePackageAndUserAspectRatio(pkgName, aspectRatio);
            }
        }
        mStorage.restoreCompleted();
    }

    private boolean isPackageInstalled(@NonNull String packageName) {
        try {
            return mPackageManager.getPackageInfo(packageName, /* flags= */ 0) != null;
        } catch (PackageManager.NameNotFoundException e) {
            // It is common that the package is not installed during setup. Restore will be retried
            // when the package is installed.
            if (DEBUG) {
                Slog.d(TAG, "Could not get package info for " + packageName);
            }
            return false;
        } catch (Exception e) {
            mLogger.logItemsRestoreFailed(KEY_USER_ASPECT_RATIO, /* count= */ 1,
                    ERROR_QUERY_PACKAGE_FAILED);
            if (DEBUG) {
                Slog.e(TAG, "Could not get package info for " + packageName, e);
            }
            return false;
        }
    }

    /** Applies the restore for per-app user set min aspect ratio. */
    private void checkExistingAspectRatioAndApplyRestore(@NonNull String pkgName,
            @PackageManager.UserMinAspectRatio int aspectRatio) {
        try {
            final int existingUserAspectRatio = mIPackageManager.getUserMinAspectRatio(pkgName,
                    mUserId);
            // Don't apply the restore if the aspect ratio have already been set for the app.
            if (existingUserAspectRatio != USER_MIN_ASPECT_RATIO_UNSET) {
                Slog.d(TAG, "Not restoring user aspect ratio=" + aspectRatio + " for package="
                        + pkgName + " as it is already set to " + existingUserAspectRatio + ".");
                return;
            }

            final int mostSuitableAspectRatio = getSameOrClosestBiggerAspectRatio(aspectRatio);
            if (mostSuitableAspectRatio != USER_MIN_ASPECT_RATIO_UNSET) {
                mIPackageManager.setUserMinAspectRatio(pkgName, mUserId, mostSuitableAspectRatio);
                if (DEBUG) {
                    Slog.d(TAG, "Restored user aspect ratio=" + mostSuitableAspectRatio
                            + " for package=" + pkgName + " . Backed-up aspect ratio was "
                            + aspectRatio);
                }
            }
        } catch (RemoteException | IllegalArgumentException e) {
            mLogger.logItemsRestoreFailed(KEY_USER_ASPECT_RATIO, /* count= */ 1,
                    ERROR_ASPECT_RATIO_FAILED);
            Slog.e(TAG, "Could not restore user aspect ratio for package " + pkgName, e);
        }
    }

    private float getAspectRatioForRect(@NonNull Rect rect) {
        return makeFloatHigherOrEqualTo1((float) rect.width() / rect.height());
    }

    /**
     * Returns the most fitting aspect ratio available on this device.
     *
     * <p> If the exact aspect ratio is not available, choose the next closest one that produces a
     * bigger app surface. The goal is to produce similar UX, but also to favor bigger app area.
     * Users have already changed the user aspect ratio on their previous device, so they should be
     * able to change it again if the new aspect ratio is not ideal.
     *
     * <p>Aspect ratio is chosen in this order of priority, if available:
     * <ol>
     *     <li>Same aspect ratio setting,
     *     <li>The closest bigger available setting,
     *     <li>{@link PackageManager#USER_MIN_ASPECT_RATIO_FULLSCREEN},
     *     <li>{@link PackageManager#USER_MIN_ASPECT_RATIO_UNSET}.
     * </ol>
     */
    private int getSameOrClosestBiggerAspectRatio(int aspectRatioSetting) {
        if (mAvailableUserMinAspectRatioSet.contains(aspectRatioSetting)) {
            return aspectRatioSetting;
        }

        // TODO(b/413007174): this assumes that the size of the backed up display is similar to this
        //  device. Backup device dimensions too to calculate the exact aspect ratio that the user
        //  has set.
        final Rect displaySize = getSizeOfLargestDisplay();
        if (displaySize == null || displaySize.isEmpty()) {
            return USER_MIN_ASPECT_RATIO_UNSET;
        }
        float realRestoredAspectRatio = getAspectRatioForSetting(aspectRatioSetting,
                getAspectRatioForRect(displaySize));

        // Bigger aspect ratio means smaller area, so the goal is to find the closest aspect ratio
        // float value that is less than the restored aspect ratio. Fullscreen has the aspect ratio
        // equal to 0.5 (smallest value), so the result will gravitate towards fullscreen.
        int bestSetting = 0;
        float closestSmallerAspectRatioValue = 0;
        for (Integer setting : mUserAspectRatiosPerSettingMap.keySet()) {
            float aspectRatio = mUserAspectRatiosPerSettingMap.get(setting);
            // Bigger area (goal) results in a smaller aspect ratio (closer to 1), so bigger value
            // does not work. Fullscreen has a special value of 0.5f, to allow min aspect ratio
            // setting of 1 in the future.
            // Values < 0 are not applicable for this comparison.
            if (aspectRatio > realRestoredAspectRatio || aspectRatio < 0) {
                continue;
            }
            // If current value is smaller than the restored, make sure it is the closest so far.
            if (aspectRatio > closestSmallerAspectRatioValue) {
                closestSmallerAspectRatioValue = aspectRatio;
                bestSetting = setting;
            }
        }
        if (bestSetting == 0) {
            // This device is smaller than the real aspect ratio of the new device. Choose
            // fullscreen if possible.
            if (mAvailableUserMinAspectRatioSet.contains(USER_MIN_ASPECT_RATIO_FULLSCREEN)) {
                return USER_MIN_ASPECT_RATIO_FULLSCREEN;
            } else {
                Slog.w(TAG, "Unable to find a suitable aspect ratio for restored: "
                        + aspectRatioSetting);
                return USER_MIN_ASPECT_RATIO_UNSET;
            }
        }

        return bestSetting;
    }

    // Visible for testing because DisplayManager cannot be mocked, so using the real implementation
    // in the test and making sure it is well tested.

    /**
     * Returns the dimensions of the largest built-in display, even if inactive or disabled.
     *
     * <p>This method assumes that the largest display will be the one to measure aspect ratio by,
     * as it is unable to check whether {@code ignoreOrientationRequest == true}.
     */
    @VisibleForTesting
    @Nullable
    Rect getSizeOfLargestDisplay() {
        final DisplayManager displayManager = mContext.getSystemService(DisplayManager.class);
        if (displayManager == null) {
            return null;
        }
        // Query all internal displays - including disabled - to include inner screens for foldables
        // when the device is folded.
        final Display[] displays = displayManager.getDisplays(
                /* flags= */ DISPLAY_CATEGORY_ALL_INCLUDING_DISABLED);
        final Rect maxDimensions = new Rect();
        for (Display display: displays) {
            final DisplayInfo outDisplayInfo = new DisplayInfo();
            display.getDisplayInfo(outDisplayInfo);
            if (display.getType() != Display.TYPE_INTERNAL) {
                // Some aspect ratios, like 'half-screen', are relative to the biggest internal
                // display. Ignore other types of displays.
                continue;
            }
            final int width = outDisplayInfo.getNaturalWidth();
            final int height = outDisplayInfo.getNaturalHeight();
            if (width * height > maxDimensions.width() * maxDimensions.height())  {
                maxDimensions.set(0 , 0, width, height);
            }
        }
        return maxDimensions;
    }
}
