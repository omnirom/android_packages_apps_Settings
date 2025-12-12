/*
 * Copyright 2025 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may not use this file except in compliance with the License.
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
import android.app.ActivityManager.LOCK_TASK_MODE_NONE
import android.app.Application
import android.app.TaskStackListener
import android.content.Context
import android.hardware.display.DisplayManager
import android.provider.Settings
import android.view.Display.DEFAULT_DISPLAY
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceScreen
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.RestrictedListPreference
import com.android.settings.connecteddevice.display.SelectedDisplayPreferenceFragment.PrefInfo
import com.android.settings.testutils.InstantTaskExecutorRule
import com.google.common.truth.Truth.assertThat
import kotlin.test.assertNull
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.Captor
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.kotlin.doReturn

/** Unit tests for [SelectedDisplayPreferenceFragment]. */
@RunWith(AndroidJUnit4::class)
class SelectedDisplayPreferenceFragmentTest : ExternalDisplayTestBase() {
    // Rule to execute LiveData operations synchronously
    @get:Rule val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var application: Application
    private lateinit var viewModel: DisplayPreferenceViewModel
    private lateinit var fragment: TestableSelectedDisplayPreferenceFragment

    @Captor private lateinit var taskStackListenerCaptor: ArgumentCaptor<TaskStackListener>

    @Before
    override fun setUp() {
        super.setUp()
        application = ApplicationProvider.getApplicationContext() as Application

        viewModel =
            DisplayPreferenceViewModel(
                application,
                mMockedInjector,
                mActivityManager,
                mActivityTaskManager,
                mDevicePolicyManager,
            )
        setMirroringMode(false)
    }

    @Test
    fun testNoDisplayAtStart_noPreferenceShown() {
        fragment = initFragment()
        // Simulate with the state of waiting for displays to be loaded
        updateDisplaysAndTopology(emptyList())
        viewModel.updateEnabledDisplays()

        val category = mPreferenceScreen.getPreference(0) as PreferenceCategory
        assertThat(category).isNotNull()
        assertThat(category.title).isEqualTo("")

        // Verify all other preferences are hidden
        for (i in 0 until category.preferenceCount) {
            val pref = category.getPreference(i)
            assertFalse(pref.isVisible)
        }
    }

    @Test
    fun testDefaultDisplaySelected_showsBuiltInPreferences() {
        fragment = initFragment()
        includeBuiltinDisplay()
        viewModel.updateEnabledDisplays()
        viewModel.updateSelectedDisplay(DEFAULT_DISPLAY)

        val category = mPreferenceScreen.getPreference(0) as PreferenceCategory
        assertVisible(category, PrefInfo.BUILTIN_DISPLAY_SUB_CATEGORY.key, true)
        val builtinDisplaySubPrefCategory =
            category.findPreference<Preference>(PrefInfo.BUILTIN_DISPLAY_SUB_CATEGORY.key)!!
                as PreferenceCategory

        assertVisible(category, PrefInfo.DISPLAY_MIRRORING.key, true)
        assertVisible(category, PrefInfo.INCLUDE_DEFAULT_DISPLAY.key, true)
        assertVisible(builtinDisplaySubPrefCategory, PrefInfo.BUILTIN_DISPLAY_DENSITY.key, true)
        assertVisible(category, PrefInfo.EXTERNAL_DISPLAY_DENSITY.key, false)
        assertVisible(category, PrefInfo.DISPLAY_RESOLUTION.key, false)
        assertVisible(category, PrefInfo.DISPLAY_ROTATION.key, false)
        assertVisible(category, PrefInfo.DISPLAY_CONNECTION_PREFERENCE.key, false)
    }

    @Test
    fun testDefaultDisplaySelected_launchingDefaultDisplaySettings() {
        fragment = initFragment()
        includeBuiltinDisplay()
        viewModel.updateEnabledDisplays()
        viewModel.updateSelectedDisplay(DEFAULT_DISPLAY)

        val category = mPreferenceScreen.getPreference(0) as PreferenceCategory
        val builtinDisplaySubPrefCategory =
            category.findPreference<Preference>(PrefInfo.BUILTIN_DISPLAY_SUB_CATEGORY.key)!!
                as PreferenceCategory

        val sizeAndTextPref =
            builtinDisplaySubPrefCategory.findPreference<Preference>(
                PrefInfo.BUILTIN_DISPLAY_DENSITY.key
            )
        sizeAndTextPref!!.onPreferenceClickListener!!.onPreferenceClick(sizeAndTextPref)

        assertTrue(fragment.isBuiltinDisplaySettingsLaunched)
    }

    @Test
    fun testDefaultDisplaySelected_mirroringPreference_updateIsCheckedState() {
        fragment = initFragment()
        includeBuiltinDisplay()
        viewModel.updateEnabledDisplays()
        viewModel.updateSelectedDisplay(DEFAULT_DISPLAY)
        setMirroringMode(true)

        val category = mPreferenceScreen.getPreference(0) as PreferenceCategory
        var preference = category.findPreference<MirrorPreference>(PrefInfo.DISPLAY_MIRRORING.key)!!
        assertTrue(preference.isEnabled)
        assertTrue(preference.isChecked)

        setMirroringMode(false)
        preference = category.findPreference(PrefInfo.DISPLAY_MIRRORING.key)!!
        assertTrue(preference.isEnabled)
        assertFalse(preference.isChecked)
    }

    @Test
    fun testDefaultDisplaySelected_notProjectedMode_hideIncludeDefaultDisplayInTopology() {
        fragment = initFragment()
        doReturn(false).`when`(mMockedInjector).isProjectedModeEnabled()
        includeBuiltinDisplay()
        viewModel.updateEnabledDisplays()
        viewModel.updateSelectedDisplay(DEFAULT_DISPLAY)
        setMirroringMode(false)

        val category = mPreferenceScreen.getPreference(0) as PreferenceCategory
        assertVisible(category, PrefInfo.INCLUDE_DEFAULT_DISPLAY.key, false)
    }

    @Test
    fun testDefaultDisplaySelected_isMirroring_hideIncludeDefaultDisplayInTopology() {
        fragment = initFragment()
        includeBuiltinDisplay()
        viewModel.updateEnabledDisplays()
        viewModel.updateSelectedDisplay(DEFAULT_DISPLAY)
        setMirroringMode(true)

        val category = mPreferenceScreen.getPreference(0) as PreferenceCategory
        assertVisible(category, PrefInfo.INCLUDE_DEFAULT_DISPLAY.key, false)
    }

    @Test
    fun testDefaultDisplaySelected_lockTaskLocked_setMirroringToggleAsChecked() {
        fragment = initFragment()
        verify(mActivityTaskManager).registerTaskStackListener(taskStackListenerCaptor.capture())
        includeBuiltinDisplay()
        viewModel.updateEnabledDisplays()
        viewModel.updateSelectedDisplay(DEFAULT_DISPLAY)
        taskStackListenerCaptor.value.onLockTaskModeChanged(LOCK_TASK_MODE_LOCKED)
        mHandler.flush()

        val category = mPreferenceScreen.getPreference(0) as PreferenceCategory
        val preference = category.findPreference<MirrorPreference>(PrefInfo.DISPLAY_MIRRORING.key)!!
        assertTrue(preference.isChecked)
        assertFalse(preference.isEnabled)
    }

    @Test
    fun testDefaultDisplaySelected_lockTaskLocked_hideIncludeDefaultDisplayInTopology() {
        fragment = initFragment()
        verify(mActivityTaskManager).registerTaskStackListener(taskStackListenerCaptor.capture())
        includeBuiltinDisplay()
        viewModel.updateEnabledDisplays()
        viewModel.updateSelectedDisplay(DEFAULT_DISPLAY)
        taskStackListenerCaptor.value.onLockTaskModeChanged(LOCK_TASK_MODE_LOCKED)
        mHandler.flush()

        val category = mPreferenceScreen.getPreference(0) as PreferenceCategory
        assertVisible(category, PrefInfo.INCLUDE_DEFAULT_DISPLAY.key, false)
    }

    @Test
    fun testExternalDisplaySelected_showsExternalPreferences() {
        fragment = initFragment()
        val display = mDisplays.first { it.id == EXTERNAL_DISPLAY_ID }

        viewModel.updateSelectedDisplay(display.id)

        val category = mPreferenceScreen.getPreference(0) as PreferenceCategory
        assertVisible(category, PrefInfo.DISPLAY_MIRRORING.key, false)
        assertVisible(category, PrefInfo.INCLUDE_DEFAULT_DISPLAY.key, false)
        assertVisible(category, PrefInfo.BUILTIN_DISPLAY_SUB_CATEGORY.key, false)
        assertVisible(category, PrefInfo.EXTERNAL_DISPLAY_DENSITY.key, true)
        assertVisible(category, PrefInfo.DISPLAY_RESOLUTION.key, true)
        assertVisible(category, PrefInfo.DISPLAY_ROTATION.key, true)
        assertVisible(category, PrefInfo.DISPLAY_CONNECTION_PREFERENCE.key, true)
    }

    @Test
    fun testExternalDisplaySelected_isMirroring_hideDisplayDensityPreference() {
        fragment = initFragment()
        val display = mDisplays.first { it.id == EXTERNAL_DISPLAY_ID }

        setMirroringMode(true)
        viewModel.updateSelectedDisplay(display.id)

        val category = mPreferenceScreen.getPreference(0) as PreferenceCategory
        assertVisible(category, PrefInfo.DISPLAY_MIRRORING.key, false)
        assertVisible(category, PrefInfo.INCLUDE_DEFAULT_DISPLAY.key, false)
        assertVisible(category, PrefInfo.BUILTIN_DISPLAY_SUB_CATEGORY.key, false)
        assertVisible(category, PrefInfo.EXTERNAL_DISPLAY_DENSITY.key, false)
        assertVisible(category, PrefInfo.DISPLAY_RESOLUTION.key, true)
        assertVisible(category, PrefInfo.DISPLAY_ROTATION.key, true)
        assertVisible(category, PrefInfo.DISPLAY_CONNECTION_PREFERENCE.key, true)
    }

    @Test
    fun testExternalDisplaySelected_lockTaskLocked_hideIncludeDefaultDisplayInTopology() {
        fragment = initFragment()
        verify(mActivityTaskManager).registerTaskStackListener(taskStackListenerCaptor.capture())
        val display = mDisplays.first { it.id == EXTERNAL_DISPLAY_ID }

        viewModel.updateSelectedDisplay(display.id)
        taskStackListenerCaptor.value.onLockTaskModeChanged(LOCK_TASK_MODE_LOCKED)
        mHandler.flush()

        val category = mPreferenceScreen.getPreference(0) as PreferenceCategory
        assertVisible(category, PrefInfo.EXTERNAL_DISPLAY_DENSITY.key, false)
    }

    @Test
    fun testExternalDisplaySelected_launchingResolutionSelector() {
        fragment = initFragment()
        val display = mDisplays.first { it.id == EXTERNAL_DISPLAY_ID }

        viewModel.updateSelectedDisplay(display.id)
        val category = mPreferenceScreen.getPreference(0) as PreferenceCategory
        val resolutionPref = category.findPreference<Preference>(PrefInfo.DISPLAY_RESOLUTION.key)!!
        resolutionPref.onPreferenceClickListener!!.onPreferenceClick(resolutionPref)

        assertThat(resolutionPref.summary.toString())
            .isEqualTo("${display.mode?.physicalWidth} x ${display.mode?.physicalHeight}")
        assertThat(fragment.writtenMetricsPreference).isEqualTo(resolutionPref)
        assertThat(fragment.resolutionSelectorLaunchDisplayId).isEqualTo(EXTERNAL_DISPLAY_ID)
    }

    @Test
    fun testExternalDisplaySelected_updatesRotationPreference() {
        fragment = initFragment()
        val display = mDisplays.first { it.id == EXTERNAL_DISPLAY_ID }
        doReturn(true).`when`(mMockedInjector).freezeDisplayRotation(display.id, 1)

        viewModel.updateSelectedDisplay(display.id)
        val category = mPreferenceScreen.getPreference(0) as PreferenceCategory
        val rotationPref = category.findPreference<ListPreference>(PrefInfo.DISPLAY_ROTATION.key)!!
        val initialValue = rotationPref.value
        rotationPref.onPreferenceChangeListener!!.onPreferenceChange(rotationPref, "1")

        assertThat(initialValue).isEqualTo("0")
        assertThat(fragment.writtenMetricsPreference).isEqualTo(rotationPref)
        verify(mMockedInjector).freezeDisplayRotation(display.id, 1)
        assertThat(rotationPref.value).isEqualTo("1")
    }

    @Test
    fun testExternalDisplaySelected_notProjectedMode_hideDisplayConnectionPreference() {
        doReturn(false).`when`(mMockedInjector).isProjectedModeEnabled()
        fragment = initFragment()
        val display = mDisplays.first { it.id == EXTERNAL_DISPLAY_ID }

        viewModel.updateSelectedDisplay(display.id)

        val category = mPreferenceScreen.getPreference(0) as PreferenceCategory
        assertNull(category.findPreference(PrefInfo.DISPLAY_CONNECTION_PREFERENCE.key))
    }

    @Test
    fun testExternalDisplaySelected_updatesConnectionPreference() {
        fragment = initFragment()
        val display = mDisplays.first { it.id == EXTERNAL_DISPLAY_ID }

        viewModel.updateSelectedDisplay(display.id)
        val category = mPreferenceScreen.getPreference(0) as PreferenceCategory
        val connectionPref =
            category.findPreference<RestrictedListPreference>(
                PrefInfo.DISPLAY_CONNECTION_PREFERENCE.key
            )!!

        assertThat(connectionPref.value).isEqualTo("0")

        connectionPref.onPreferenceChangeListener!!.onPreferenceChange(connectionPref, "1")

        assertThat(fragment.writtenMetricsPreference).isEqualTo(connectionPref)
        verify(mMockedInjector).updateDisplayConnectionPreference(display.uniqueId, 1)
        assertThat(connectionPref.value).isEqualTo("1")
    }

    @Test
    fun testExternalDisplaySelected_lockTaskLocked_disableConnectionPreference() {
        fragment = initFragment()
        verify(mActivityTaskManager).registerTaskStackListener(taskStackListenerCaptor.capture())
        val display = mDisplays.first { it.id == EXTERNAL_DISPLAY_ID }

        viewModel.updateSelectedDisplay(display.id)
        taskStackListenerCaptor.value.onLockTaskModeChanged(LOCK_TASK_MODE_LOCKED)
        mHandler.flush()

        val category = mPreferenceScreen.getPreference(0) as PreferenceCategory
        var connectionPref =
            category.findPreference<RestrictedListPreference>(
                PrefInfo.DISPLAY_CONNECTION_PREFERENCE.key
            )!!
        assertThat(connectionPref.isEnabled).isEqualTo(false)
        assertThat(connectionPref.value)
            .isEqualTo(DisplayManager.EXTERNAL_DISPLAY_CONNECTION_PREFERENCE_MIRROR.toString())

        taskStackListenerCaptor.value.onLockTaskModeChanged(LOCK_TASK_MODE_NONE)
        mHandler.flush()
        connectionPref = category.findPreference(PrefInfo.DISPLAY_CONNECTION_PREFERENCE.key)!!
        assertThat(connectionPref.isEnabled).isEqualTo(true)
        assertThat(connectionPref.value).isEqualTo("0")
    }

    private fun setMirroringMode(enable: Boolean) {
        Settings.Secure.putInt(
            application.contentResolver,
            Settings.Secure.MIRROR_BUILT_IN_DISPLAY,
            if (enable) 1 else 0,
        )
        viewModel.mirrorModeObserver.onChange(/* selfChange= */ false)
    }

    private fun assertVisible(category: PreferenceCategory, key: String, isVisible: Boolean) {
        assertThat(category.findPreference<Preference>(key)!!.isVisible).isEqualTo(isVisible)
    }

    private fun initFragment(): TestableSelectedDisplayPreferenceFragment {
        val fragment =
            TestableSelectedDisplayPreferenceFragment(mContext, mPreferenceScreen, viewModel)
        fragment.onCreateCallback(null)
        fragment.onActivityCreatedCallback(null)
        fragment.onStartCallback()
        return fragment
    }

    class TestableSelectedDisplayPreferenceFragment(
        private val context: Context,
        private val preferenceScreen: PreferenceScreen,
        viewModel: DisplayPreferenceViewModel,
    ) : SelectedDisplayPreferenceFragment(viewModel) {
        var resolutionSelectorLaunchDisplayId = -123
        var isBuiltinDisplaySettingsLaunched = false
        var writtenMetricsPreference: Preference? = null
        private val mockViewLifecycleOwner = mock(LifecycleOwner::class.java)

        val lifecycleRegistry = LifecycleRegistry(this)

        init {
            doReturn(lifecycleRegistry).`when`(mockViewLifecycleOwner).lifecycle
            // Required to allow observer to start observing data
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
        }

        override fun getContext(): Context {
            return context
        }

        override fun getViewLifecycleOwner(): LifecycleOwner {
            return mockViewLifecycleOwner
        }

        override fun addPreferencesFromResource(preferencesResId: Int) {
            // No-op
        }

        override fun getPreferenceScreen(): PreferenceScreen {
            return preferenceScreen
        }

        override fun writePreferenceClickMetric(preference: Preference?) {
            writtenMetricsPreference = preference
        }

        override fun launchResolutionSelector(displayId: Int) {
            resolutionSelectorLaunchDisplayId = displayId
        }

        override fun launchBuiltinDisplaySettings() {
            isBuiltinDisplaySettingsLaunched = true
        }
    }
}
