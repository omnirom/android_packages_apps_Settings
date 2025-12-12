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

package com.android.settings.safetycenter.ui.model

import android.Manifest
import android.app.Application
import android.safetycenter.SafetyCenterData
import android.safetycenter.SafetyCenterErrorDetails
import android.safetycenter.SafetyCenterIssue
import android.safetycenter.SafetyCenterManager
import androidx.annotation.RequiresPermission
import androidx.core.content.ContextCompat.getMainExecutor
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.map
import androidx.lifecycle.viewmodel.CreationExtras
import kotlin.reflect.KClass

/**
 * A [SafetyCenterViewModel] that provides real-time data from the backing [SafetyCenterManager]
 * service.
 *
 * @param app The application context, used to retrieve system services.
 */
class LiveSafetyCenterViewModel(app: Application) : SafetyCenterViewModel(app) {

    override val statusUiLiveData: LiveData<StatusUiData>
        get() = safetyCenterUiLiveData.map { StatusUiData(it) }

    override val safetyCenterUiLiveData: LiveData<SafetyCenterData> by this::_safetyCenterLiveData
    override val errorLiveData: LiveData<SafetyCenterErrorDetails> by this::_errorLiveData

    private val _safetyCenterLiveData = SafetyCenterLiveData()
    private val _errorLiveData = MutableLiveData<SafetyCenterErrorDetails>()

    private val safetyCenterManager = app.getSystemService(SafetyCenterManager::class.java)!!

    /**
     * A [MutableLiveData] that listens for changes from the [SafetyCenterManager].
     *
     * It automatically registers and unregisters the listener when it becomes active or inactive.
     */
    private inner class SafetyCenterLiveData :
        MutableLiveData<SafetyCenterData>(), SafetyCenterManager.OnSafetyCenterDataChangedListener {
        @RequiresPermission(Manifest.permission.MANAGE_SAFETY_CENTER)
        override fun onActive() {
            safetyCenterManager.addOnSafetyCenterDataChangedListener(
                getMainExecutor(app.applicationContext),
                this,
            )
            super.onActive()
        }

        @RequiresPermission(Manifest.permission.MANAGE_SAFETY_CENTER)
        override fun onInactive() {
            safetyCenterManager.removeOnSafetyCenterDataChangedListener(this)
            super.onInactive()
        }

        override fun onSafetyCenterDataChanged(data: SafetyCenterData) {
            value = data
        }

        override fun onError(errorDetails: SafetyCenterErrorDetails) {
            _errorLiveData.value = errorDetails
        }
    }

    @RequiresPermission(Manifest.permission.MANAGE_SAFETY_CENTER)
    override fun getCurrentSafetyCenterDataAsUiData(): SafetyCenterData =
        safetyCenterManager.safetyCenterData

    @RequiresPermission(Manifest.permission.MANAGE_SAFETY_CENTER)
    override fun rescan() {
        safetyCenterManager.refreshSafetySources(
            SafetyCenterManager.REFRESH_REASON_RESCAN_BUTTON_CLICK
        )
    }

    override fun clearError() {
        _errorLiveData.value = null
    }

    @RequiresPermission(Manifest.permission.MANAGE_SAFETY_CENTER)
    override fun dismissIssue(issue: SafetyCenterIssue) {
        safetyCenterManager.dismissSafetyCenterIssue(issue.id)
    }
}

/**
 * A [ViewModelProvider.Factory] for creating instances of [LiveSafetyCenterViewModel].
 *
 * @param app The application context to be provided to the ViewModel.
 */
class LiveSafetyCenterViewModelFactory(private val app: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: KClass<T>, extras: CreationExtras): T {
        @Suppress("UNCHECKED_CAST")
        return LiveSafetyCenterViewModel(app) as T
    }
}
