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

import android.app.ActivityManager.LOCK_TASK_MODE_LOCKED
import android.app.Application
import android.app.TaskStackListener
import android.provider.Settings
import android.view.Display
import android.view.Display.DEFAULT_DISPLAY
import androidx.lifecycle.Observer
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.testutils.InstantTaskExecutorRule
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.Captor
import org.mockito.Mock
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.verify

/** Unit test for [DisplayPreferenceViewModel] */
@RunWith(AndroidJUnit4::class)
class DisplayPreferenceViewModelTest : ExternalDisplayTestBase() {

    // Rule to execute LiveData operations synchronously
    @get:Rule val instantTaskExecutorRule = InstantTaskExecutorRule()

    @Mock private lateinit var uiStateObserver: Observer<DisplayPreferenceViewModel.DisplayUiState>

    @Captor private lateinit var taskStackListenerCaptor: ArgumentCaptor<TaskStackListener>

    private lateinit var viewModel: DisplayPreferenceViewModel
    private lateinit var application: Application

    @Before
    override fun setUp() {
        super.setUp()
        application = ApplicationProvider.getApplicationContext()
    }

    @After
    fun tearDown() {
        viewModel.uiState.removeObserver(uiStateObserver)
    }

    private fun setupViewModel() {
        viewModel =
            DisplayPreferenceViewModel(
                application,
                mMockedInjector,
                mActivityManager,
                mActivityTaskManager,
                mDevicePolicyManager,
            )
        viewModel.uiState.observeForever(uiStateObserver)
    }

    private fun setMirroringMode(enable: Boolean) {
        Settings.Secure.putInt(
            application.contentResolver,
            Settings.Secure.MIRROR_BUILT_IN_DISPLAY,
            if (enable) 1 else 0,
        )
        viewModel.mirrorModeObserver.onChange(/* selfChange= */ false)
    }

    private fun setIncludeDefaultDisplayInTopology(enable: Boolean) {
        Settings.Secure.putInt(
            application.contentResolver,
            Settings.Secure.INCLUDE_DEFAULT_DISPLAY_IN_TOPOLOGY,
            if (enable) 1 else 0,
        )
        viewModel.includeDefaultDisplayInTopologyObserver.onChange(/* selfChange= */ false)
    }

    @Test
    fun init_loadsEnabledDisplaysAndSetsDefaultDisplay() {
        setupViewModel()
        val state = viewModel.uiState.value!!

        assertThat(state.enabledDisplays).hasSize(2)
        assertThat(state.enabledDisplays.keys)
            .containsExactly(EXTERNAL_DISPLAY_ID, OVERLAY_DISPLAY_ID)

        assertThat(state.selectedDisplayId).isEqualTo(mDisplayTopology.primaryDisplayId)
        assertThat(state.isMirroring).isFalse()
    }

    @Test
    fun mirrorModeSettingChanged_updatesUiState_isMirroring() {
        setupViewModel()
        assertThat(viewModel.uiState.value!!.isMirroring).isFalse()

        setMirroringMode(true)

        assertThat(viewModel.uiState.value!!.isMirroring).isTrue()

        setMirroringMode(false)

        assertThat(viewModel.uiState.value!!.isMirroring).isFalse()
    }

    @Test
    fun mirrorModeSettingChanged_lockTaskModeLocked_isMirroringIsTrue() {
        setupViewModel()
        verify(mActivityTaskManager).registerTaskStackListener(taskStackListenerCaptor.capture())
        setMirroringMode(true)
        assertThat(viewModel.uiState.value!!.isMirroring).isTrue()

        taskStackListenerCaptor.value.onLockTaskModeChanged(LOCK_TASK_MODE_LOCKED)
        mHandler.flush()
        setMirroringMode(false)

        assertThat(viewModel.uiState.value!!.isMirroring).isTrue()
    }

    @Test
    fun mirrorModeSettingChanged_updatesUiState_showIncludeDefaultDisplayInTopologyPref() {
        setupViewModel()
        assertThat(viewModel.uiState.value!!.showIncludeDefaultDisplayInTopologyPref).isTrue()

        setMirroringMode(true)

        assertThat(viewModel.uiState.value!!.showIncludeDefaultDisplayInTopologyPref).isFalse()

        setMirroringMode(false)

        assertThat(viewModel.uiState.value!!.showIncludeDefaultDisplayInTopologyPref).isTrue()
    }

    @Test
    fun mirrorModeSettingChanged_notProjectedMode_showIncludeDefaultDisplayInTopologyPref_false() {
        setupViewModel()
        doReturn(false).`when`(mMockedInjector).isProjectedModeEnabled()
        setMirroringMode(false)

        assertThat(viewModel.uiState.value!!.showIncludeDefaultDisplayInTopologyPref).isFalse()
    }

    @Test
    fun initialLockTaskModeLockedupdatesUiState() {
        doReturn(LOCK_TASK_MODE_LOCKED).`when`(mActivityManager).lockTaskModeState
        setupViewModel()

        assertThat(viewModel.uiState.value!!.lockTaskPolicyInfo.lockTaskMode)
            .isEqualTo(LOCK_TASK_MODE_LOCKED)
        assertThat(viewModel.uiState.value!!.isMirroring).isTrue()
        assertThat(viewModel.uiState.value!!.showIncludeDefaultDisplayInTopologyPref).isFalse()
    }

    @Test
    fun lockTaskModeChanged_updatesUiState() {
        setupViewModel()
        verify(mActivityTaskManager).registerTaskStackListener(taskStackListenerCaptor.capture())
        taskStackListenerCaptor.value.onLockTaskModeChanged(LOCK_TASK_MODE_LOCKED)
        mHandler.flush()

        assertThat(viewModel.uiState.value!!.lockTaskPolicyInfo.lockTaskMode)
            .isEqualTo(LOCK_TASK_MODE_LOCKED)
        assertThat(viewModel.uiState.value!!.isMirroring).isTrue()
        assertThat(viewModel.uiState.value!!.showIncludeDefaultDisplayInTopologyPref).isFalse()
    }

    @Test
    fun includeDefaultDisplayInTopologySettingChanged_updatesUiState() {
        setupViewModel()
        assertThat(viewModel.uiState.value!!.includeDefaultDisplayInTopology).isFalse()

        setIncludeDefaultDisplayInTopology(true)

        assertThat(viewModel.uiState.value!!.includeDefaultDisplayInTopology).isTrue()

        setIncludeDefaultDisplayInTopology(false)

        assertThat(viewModel.uiState.value!!.includeDefaultDisplayInTopology).isFalse()
    }

    @Test
    fun updateSelectedDisplay_selectedDisplayUpdated() {
        setupViewModel()
        viewModel.updateSelectedDisplay(123)

        val state = viewModel.uiState.value!!
        assertThat(state.selectedDisplayId).isEqualTo(123)
    }

    @Test
    fun updateEnabledDisplays_includeBuiltinDisplay_selectedDisplayIdKept_enabledDisplaysUpdated() {
        setupViewModel()
        val primaryDisplayId = mDisplayTopology.primaryDisplayId
        includeBuiltinDisplay()

        viewModel.updateEnabledDisplays()

        val state = viewModel.uiState.value!!
        assertThat(state.enabledDisplays).hasSize(3)
        assertThat(state.enabledDisplays.keys)
            .containsExactly(DEFAULT_DISPLAY, EXTERNAL_DISPLAY_ID, OVERLAY_DISPLAY_ID)
        assertThat(state.selectedDisplayId).isEqualTo(primaryDisplayId)
    }

    @Test
    fun updateEnabledDisplays_excludeNonConnectedDisplaysAndNotDefaultDisplay() {
        setupViewModel()
        val initialState = viewModel.uiState.value!!
        assertThat(initialState.enabledDisplays).hasSize(2)

        val updatedEnabledDisplays = mDisplays.toMutableList()
        // Add non-connected display
        val mode = Display.Mode(720, 1280, 60f)
        updatedEnabledDisplays.add(
            DisplayDevice(
                123,
                "local:1111111111",
                "test",
                mode,
                listOf(mode),
                DisplayIsEnabled.YES,
                /* isConnectedDisplay= */ false,
            )
        )
        updateDisplaysAndTopology(updatedEnabledDisplays)

        viewModel.updateEnabledDisplays()

        val state = viewModel.uiState.value!!
        assertThat(state.enabledDisplays).hasSize(2)
    }

    @Test
    fun updateEnabledDisplays_excludeNonEnabledDisplaysAndNotDefaultDisplay() {
        setupViewModel()
        val initialState = viewModel.uiState.value!!
        assertThat(initialState.enabledDisplays).hasSize(2)

        val updatedEnabledDisplays = mDisplays.toMutableList()
        // Add non-enabled display
        val mode = Display.Mode(720, 1280, 60f)
        updatedEnabledDisplays.add(
            DisplayDevice(
                123,
                "local:1111111111",
                "test",
                mode,
                listOf(mode),
                DisplayIsEnabled.NO,
                /* isConnectedDisplay= */ true,
            )
        )
        updateDisplaysAndTopology(updatedEnabledDisplays)

        viewModel.updateEnabledDisplays()

        val state = viewModel.uiState.value!!
        assertThat(state.enabledDisplays).hasSize(2)
    }

    @Test
    fun updateEnabledDisplays_removeSelectedDisplay_selectedDisplayUpdated() {
        setupViewModel()
        val initialState = viewModel.uiState.value!!
        assertThat(initialState.enabledDisplays).hasSize(2)

        val updatedEnabledDisplays = mDisplays.toMutableList()
        // Remove initially selected display
        updatedEnabledDisplays.removeIf { it.id == mDisplayTopology.primaryDisplayId }
        updateDisplaysAndTopology(updatedEnabledDisplays)

        viewModel.updateEnabledDisplays()

        val state = viewModel.uiState.value!!
        assertThat(state.enabledDisplays).hasSize(1)
        assertThat(state.enabledDisplays.keys).containsExactly(updatedEnabledDisplays[0].id)
        assertThat(state.selectedDisplayId).isEqualTo(mDisplayTopology.primaryDisplayId)
    }
}
