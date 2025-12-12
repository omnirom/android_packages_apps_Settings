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

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.app.backup.BackupRestoreEventLogger;
import android.app.backup.BlobBackupHelper;
import android.content.Context;
import android.content.pm.IPackageManager;
import android.os.HandlerThread;
import android.os.Process;
import android.util.Slog;

import java.time.Clock;

/** A {@link BlobBackupHelper} that handles backup and restore of user aspect ratio settings.*/
public class UserAspectRatioBackupHelper extends BlobBackupHelper {
    private static final String TAG = "UsrAspRatioBackupHlp"; // Must be < 23 characters.
    private static final boolean DEBUG = false;

    @BackupRestoreEventLogger.BackupRestoreDataType
    private static final String DATA_TYPE_USER_ASPECT_RATIO = "user_aspect_ratio:user_aspect_ratio";
    @BackupRestoreEventLogger.BackupRestoreError
    private static final String ERROR_UNEXPECTED_KEY = "unexpected_key";

    // Key under which the payload blob is stored
    public static final String KEY_USER_ASPECT_RATIO = "user_aspect_ratio";

    // Current version of the blob schema
    private static final int BLOB_VERSION = 1;

    private final UserAspectRatioBackupManager mUserAspectRatioBackupManager;

    public UserAspectRatioBackupHelper(@NonNull Context context,
            @NonNull IPackageManager packageManager, @NonNull BackupRestoreEventLogger logger) {
        super(BLOB_VERSION, KEY_USER_ASPECT_RATIO);
        setLogger(logger);

        // Handler for package updates.
        HandlerThread broadcastHandlerThread = new HandlerThread(TAG,
                Process.THREAD_PRIORITY_BACKGROUND);
        broadcastHandlerThread.start();

        mUserAspectRatioBackupManager = new UserAspectRatioBackupManager(context, packageManager,
                context.getPackageManager(), getLogger(), broadcastHandlerThread.getThreadHandler(),
                Clock.systemUTC());
    }

    @Override
    @Nullable
    protected byte[] getBackupPayload(@NonNull String key) {
        if (DEBUG) {
            Slog.d(TAG, "Handling backup of " + key);
        }
        byte[] newPayload = null;
        if (KEY_USER_ASPECT_RATIO.equals(key)) {
            newPayload = mUserAspectRatioBackupManager.getBackupPayload();
            getLogger().logItemsBackedUp(DATA_TYPE_USER_ASPECT_RATIO, /* count= */ 1);
        } else {
            Slog.w(TAG, "Unexpected backup key " + key);
            getLogger().logItemsBackupFailed(DATA_TYPE_USER_ASPECT_RATIO, /* count= */ 1,
                    ERROR_UNEXPECTED_KEY);
        }
        return newPayload;
    }

    @Override
    protected void applyRestoredPayload(@NonNull String key, @Nullable byte[] payload) {
        if (DEBUG) {
            Slog.d(TAG, "Handling restore of " + key);
        }
        if (KEY_USER_ASPECT_RATIO.equals(key)) {
            if (payload == null) {
                return;
            }
            try {
                mUserAspectRatioBackupManager.stageAndApplyRestoredPayload(payload);
                getLogger().logItemsRestored(DATA_TYPE_USER_ASPECT_RATIO, /* count= */ 1);
            } catch (Exception e) {
                Slog.e(TAG, "Error restoring user aspect ratio ", e);
            }
        } else {
            Slog.w(TAG, "Unexpected restore key " + key);
            getLogger().logItemsRestoreFailed(DATA_TYPE_USER_ASPECT_RATIO, /* count= */ 1,
                    ERROR_UNEXPECTED_KEY);
        }
    }
}
