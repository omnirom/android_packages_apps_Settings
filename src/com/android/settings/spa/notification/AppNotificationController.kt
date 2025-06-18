/*
 * Copyright (C) 2022 The Android Open Source Project
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

package com.android.settings.spa.notification

import android.content.pm.ApplicationInfo
import android.service.notification.Adjustment
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.android.settings.spa.app.storage.StorageAppListModel
import com.android.settings.spa.app.storage.StorageType

class AppNotificationController(
    private val repository: AppNotificationRepository,
    private val app: ApplicationInfo,
    private val listType: ListType,
) {
    val isEnabled: LiveData<Boolean>
        get() = _isEnabled

    fun getEnabled() = _isEnabled.get()

    fun setEnabled(enabled: Boolean) {
        if (repository.setEnabled(app, enabled)) {
            _isEnabled.postValue(enabled)
        }
    }

    private val _isEnabled = object : MutableLiveData<Boolean>() {
        override fun onActive() {
            postValue(repository.isEnabled(app))
        }

        override fun onInactive() {
        }

        fun get(): Boolean = value ?: repository.isEnabled(app).also {
            postValue(it)
        }
    }

    val isAllowed: LiveData<Boolean>
        get() = _isAllowed

    fun getAllowed() = _isAllowed.get()

    fun setAllowed(enabled: Boolean) {
        when (listType) {
            ListType.ExcludeSummarization -> {
                if (repository.setAdjustmentSupportedForPackage(
                        app, Adjustment.KEY_SUMMARIZATION, enabled)) {
                    _isAllowed.postValue(enabled)
                }
            }
            ListType.ExcludeClassification -> {
                if (repository.setAdjustmentSupportedForPackage(
                        app, Adjustment.KEY_TYPE, enabled)) {
                    _isAllowed.postValue(enabled)
                }
            }
            else -> {}
        }
    }

    private val _isAllowed = object : MutableLiveData<Boolean>() {
        override fun onActive() {
            when (listType) {
                ListType.ExcludeSummarization -> {
                    postValue(repository.isAdjustmentSupportedForPackage(
                            app, Adjustment.KEY_SUMMARIZATION))
                }
                ListType.ExcludeClassification -> {
                    postValue(repository.isAdjustmentSupportedForPackage(
                        app, Adjustment.KEY_TYPE))
                }
                else -> {}
            }
        }

        override fun onInactive() {
        }

        fun get(): Boolean = when (listType) {
            ListType.ExcludeSummarization -> {
                value ?: repository.isAdjustmentSupportedForPackage(
                    app, Adjustment.KEY_SUMMARIZATION).also {
                        postValue(it)
                }
            }
            ListType.ExcludeClassification -> {
                value ?: repository.isAdjustmentSupportedForPackage(
                    app, Adjustment.KEY_TYPE).also {
                    postValue(it)
                }
            }
            else -> false
        }
    }
}
