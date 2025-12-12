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

package com.android.settings.i18n

import android.app.backup.BackupHelper
import android.app.backup.BackupRestoreEventLogger
import android.content.Context

/**
 * Provider for I18N related features.
 */
interface RegionalCustomizationFeatureProvider {
    /**
     * Return a BackupHelper for backup movement related settings.
     *
     * @param context App context
     * @param logger To log B&R stats.
     */
    fun getMovementBackupHelper(context: Context, logger: BackupRestoreEventLogger): BackupHelper
}
