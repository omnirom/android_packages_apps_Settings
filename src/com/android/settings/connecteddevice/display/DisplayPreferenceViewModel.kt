/*
 * Copyright 2025 The Android Open Source Project
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

package com.android.settings.connecteddevice.display

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.app.ActivityManager.LOCK_TASK_MODE_LOCKED
import android.app.ActivityTaskManager
import android.app.Application
import android.app.TaskStackListener
import android.app.admin.DevicePolicyIdentifiers
import android.app.admin.DevicePolicyManager
import android.app.admin.EnforcingAdmin
import android.database.ContentObserver
import android.os.UserHandle
import android.provider.Settings
import android.provider.Settings.Secure.INCLUDE_DEFAULT_DISPLAY_IN_TOPOLOGY
import android.view.Display.DEFAULT_DISPLAY
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/** Centralized data source to provide display updates for display preference fragments */
class DisplayPreferenceViewModel
@JvmOverloads
constructor(
    application: Application,
    val injector: ConnectedDisplayInjector =
        ConnectedDisplayInjector(application.applicationContext),
    private val activityManager: ActivityManager =
        application.applicationContext.getSystemService(ActivityManager::class.java),
    private val activityTaskManager: ActivityTaskManager =
        application.applicationContext.getSystemService(ActivityTaskManager::class.java),
    private val devicePolicyManager: DevicePolicyManager =
        application.applicationContext.getSystemService(DevicePolicyManager::class.java),
) : AndroidViewModel(application) {

    data class LockTaskPolicyInfo(
        val lockTaskMode: Int = -1,
        val enforcingAdmin: EnforcingAdmin? = null,
    )

    data class DisplayUiState(
        val enabledDisplays: Map<Int, DisplayDeviceAdditionalInfo> = emptyMap(),
        val selectedDisplayId: Int = -1,
        val isMirroring: Boolean = false,
        val includeDefaultDisplayInTopology: Boolean = false,
        val showIncludeDefaultDisplayInTopologyPref: Boolean = false,
        val lockTaskPolicyInfo: LockTaskPolicyInfo = LockTaskPolicyInfo(),
    )

    private val appContext = application.applicationContext
    private val _uiState = MutableLiveData(DisplayUiState())
    val uiState: LiveData<DisplayUiState> = _uiState

    private val displayListener =
        object : ExternalDisplaySettingsConfiguration.DisplayListener() {
            override fun update(displayId: Int) {
                updateEnabledDisplays()
            }
        }

    @VisibleForTesting
    val mirrorModeObserver =
        object : ContentObserver(injector.handler) {
            override fun onChange(selfChange: Boolean) {
                updateMirroringState()
            }
        }

    @VisibleForTesting
    val includeDefaultDisplayInTopologyObserver =
        object : ContentObserver(injector.handler) {
            override fun onChange(selfChange: Boolean) {
                updateIncludeDefaultDisplayInTopology()
            }
        }

    private val lockTaskModeListener =
        object : TaskStackListener() {
            override fun onLockTaskModeChanged(mode: Int) {
                // Callback might be invoked from non-main thread
                injector.handler.post { updateLockTaskMode(mode) }
            }
        }

    init {
        injector.registerDisplayListener(displayListener)
        registerMirrorModeObserver()
        registerIncludeDefaultDisplayInTopologyObserver()
        activityTaskManager.registerTaskStackListener(lockTaskModeListener)

        // Wait synchronously for the first load
        viewModelScope.launch { updateEnabledDisplays().join() }
        updateMirroringState()
        updateIncludeDefaultDisplayInTopology()
        updateLockTaskMode(activityManager.getLockTaskModeState())
    }

    override fun onCleared() {
        super.onCleared()
        activityTaskManager.unregisterTaskStackListener(lockTaskModeListener)
        appContext.contentResolver.unregisterContentObserver(
            includeDefaultDisplayInTopologyObserver
        )
        appContext.contentResolver.unregisterContentObserver(mirrorModeObserver)
        injector.unregisterDisplayListener(displayListener)
    }

    fun updateSelectedDisplay(newDisplayId: Int) {
        if (_uiState.value?.selectedDisplayId != newDisplayId) {
            updateState { it.copy(selectedDisplayId = newDisplayId) }
        }
    }

    fun updateEnabledDisplays(): Job {
        return viewModelScope.launch {
            // getDisplaysWithAdditionalInfo() runs on bg thread as it will do multiple binder calls
            val enabledDisplaysMap =
                injector
                    .getDisplaysWithAdditionalInfo()
                    .filter {
                        it.isEnabled == DisplayIsEnabled.YES &&
                            (it.id == DEFAULT_DISPLAY || it.isConnectedDisplay)
                    }
                    .associateBy { it.id }

            updateState { currentState ->
                val selectedId =
                    if (enabledDisplaysMap.contains(currentState.selectedDisplayId)) {
                        currentState.selectedDisplayId
                    } else {
                        // If the currently selected display is no longer available, reset to
                        // default.
                        getDefaultDisplayId()
                    }
                currentState.copy(
                    enabledDisplays = enabledDisplaysMap,
                    selectedDisplayId = selectedId,
                )
            }
        }
    }

    private fun updateMirroringState() {
        // This doesn't need to trigger manual viewmodel updates for enabled displays as Display
        // callback will eventually be called following mirroring update
        updateState {
            val newMirroringState =
                isDisplayInMirroringState(
                    isDisplayInMirroringMode(appContext),
                    it.lockTaskPolicyInfo.lockTaskMode,
                )
            it.copy(
                isMirroring = newMirroringState,
                showIncludeDefaultDisplayInTopologyPref =
                    shouldShowIncludeDefaultDisplayInTopologyPref(newMirroringState),
            )
        }
    }

    private fun registerMirrorModeObserver() {
        appContext.contentResolver.registerContentObserver(
            Settings.Secure.getUriFor(Settings.Secure.MIRROR_BUILT_IN_DISPLAY),
            /* notifyForDescendants= */ false,
            mirrorModeObserver,
        )
    }

    private fun updateIncludeDefaultDisplayInTopology() {
        val newState = isIncludeDefaultDisplayInTopology()
        if (_uiState.value?.includeDefaultDisplayInTopology != newState) {
            updateState { it.copy(includeDefaultDisplayInTopology = newState) }
        }
    }

    private fun registerIncludeDefaultDisplayInTopologyObserver() {
        appContext.contentResolver.registerContentObserver(
            Settings.Secure.getUriFor(INCLUDE_DEFAULT_DISPLAY_IN_TOPOLOGY),
            /* notifyForDescendants= */ false,
            includeDefaultDisplayInTopologyObserver,
        )
    }

    @SuppressLint("MissingPermission")
    private fun updateLockTaskMode(mode: Int) {
        if (_uiState.value?.lockTaskPolicyInfo?.lockTaskMode != mode) {
            // This could be dispatched to bg thread, but given how infrequent lockTaskMode
            // change happens, it's fine to run this on main thread to not overcomplicate code
            val enforcingAdmin =
                devicePolicyManager
                    .getEnforcingAdminsForPolicy(
                        DevicePolicyIdentifiers.LOCK_TASK_POLICY,
                        UserHandle.myUserId(),
                    )
                    .getMostImportantEnforcingAdmin()
            updateState {
                val newMirroringState = isDisplayInMirroringState(it.isMirroring, mode)
                it.copy(
                    isMirroring = newMirroringState,
                    lockTaskPolicyInfo = LockTaskPolicyInfo(mode, enforcingAdmin),
                    showIncludeDefaultDisplayInTopologyPref =
                        shouldShowIncludeDefaultDisplayInTopologyPref(newMirroringState),
                )
            }
        }
    }

    private fun isIncludeDefaultDisplayInTopology() =
        Settings.Secure.getInt(
            appContext.getContentResolver(),
            INCLUDE_DEFAULT_DISPLAY_IN_TOPOLOGY,
            0,
        ) != 0

    private fun shouldShowIncludeDefaultDisplayInTopologyPref(isMirroring: Boolean) =
        !isMirroring &&
            injector.isDefaultDisplayInTopologyFlagEnabled() &&
            injector.isProjectedModeEnabled()

    /**
     * This is different from the actual [Settings.Secure.MIRROR_BUILT_IN_DISPLAY] value
     *
     * When [lockTaskMode] is set to locked, Display is set to mirroring mode, but Settings value is
     * not changed
     */
    private fun isDisplayInMirroringState(isMirroring: Boolean, lockTaskMode: Int) =
        isMirroring || lockTaskMode == LOCK_TASK_MODE_LOCKED

    private fun getDefaultDisplayId(): Int {
        return injector.displayTopology?.primaryDisplayId ?: DEFAULT_DISPLAY
    }

    private fun updateState(updateAction: (DisplayUiState) -> DisplayUiState) {
        val currentState = _uiState.value ?: DisplayUiState()
        _uiState.value = updateAction(currentState)
    }
}
