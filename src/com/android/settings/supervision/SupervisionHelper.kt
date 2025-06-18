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
package com.android.settings.supervision

import android.app.KeyguardManager
import android.content.Context
import android.os.UserHandle
import android.os.UserManager
import android.os.UserManager.USER_TYPE_PROFILE_SUPERVISING
import androidx.annotation.VisibleForTesting

/** Convenience methods for interacting with the supervising user profile. */
open class SupervisionHelper private constructor(context: Context) {
    private val mUserManager = context.getSystemService(UserManager::class.java)
    private val mKeyguardManager = context.getSystemService(KeyguardManager::class.java)

    fun getSupervisingUserHandle(): UserHandle? {
        for (user in (mUserManager?.users ?: emptyList())) {
            if (user.userType.equals(USER_TYPE_PROFILE_SUPERVISING)) {
                return user.userHandle
            }
        }
        return null
    }

    fun isSupervisingCredentialSet(): Boolean {
        val supervisingUserId = getSupervisingUserHandle()?.identifier ?: return false
        return mKeyguardManager?.isDeviceSecure(supervisingUserId) ?: false
    }

    companion object {
        @Volatile @VisibleForTesting var sInstance: SupervisionHelper? = null

        fun getInstance(context: Context): SupervisionHelper {
            return sInstance
                ?: synchronized(this) {
                    sInstance ?: SupervisionHelper(context).also { sInstance = it }
                }
        }
    }
}
