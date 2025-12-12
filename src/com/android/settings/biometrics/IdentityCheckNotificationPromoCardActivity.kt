/*
 * Copyright (C) 2025 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings.biometrics

import android.app.Activity
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.os.Bundle
import android.provider.Settings
import android.safetycenter.SafetyEvent
import android.safetycenter.SafetyEvent.SAFETY_EVENT_TYPE_SOURCE_STATE_CHANGED
import com.android.settings.safetycenter.IdentityCheckSafetySource

class IdentityCheckNotificationPromoCardActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Settings.Secure.putInt(
            contentResolver,
            Settings.Secure.IDENTITY_CHECK_NOTIFICATION_VIEW_DETAILS_CLICKED,
            1,
        )
        IdentityCheckSafetySource.setSafetySourceData(
            this,
            SafetyEvent.Builder(SAFETY_EVENT_TYPE_SOURCE_STATE_CHANGED).build(),
        )
        startActivityIfNeeded(getSafetyCenterIntent(), 0)
        finish()
    }

    private fun getSafetyCenterIntent(): Intent {
        // TODO: b/430093308 - Update to open safety center and promo card
        return Intent(Intent.ACTION_SAFETY_CENTER).apply { addFlags(FLAG_ACTIVITY_NEW_TASK) }
    }
}
