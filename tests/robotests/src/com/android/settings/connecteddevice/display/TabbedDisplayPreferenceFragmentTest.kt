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

import android.app.Application
import android.os.SystemClock
import android.view.InputDevice
import android.view.MotionEvent
import android.view.View
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.Settings
import com.android.settings.core.SettingsBaseActivity
import com.android.settings.testutils.InstantTaskExecutorRule
import com.android.settingslib.collapsingtoolbar.widget.ScrollableToolbarItemLayout
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.Captor
import org.mockito.kotlin.any
import org.mockito.kotlin.atLeastOnce
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.reset
import org.mockito.kotlin.spy
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

/** Unit tests for [TabbedDisplayPreferenceFragment]. */
@RunWith(AndroidJUnit4::class)
class TabbedDisplayPreferenceFragmentTest : ExternalDisplayTestBase() {
    @get:Rule val instantTaskExecutorRule = InstantTaskExecutorRule()

    @get:Rule
    val activityScenarioRule = ActivityScenarioRule(Settings.TestingSettingsActivity::class.java)

    @Captor
    private lateinit var toolbarListener:
        ArgumentCaptor<ScrollableToolbarItemLayout.OnItemSelectedListener>
    @Captor
    private lateinit var toolbarItemsCaptor:
        ArgumentCaptor<List<ScrollableToolbarItemLayout.ToolbarItem>>
    @Captor private lateinit var motionEventCaptor: ArgumentCaptor<MotionEvent>

    private lateinit var viewModel: DisplayPreferenceViewModel
    private lateinit var fragment: TestableTabbedDisplayPreferenceFragment
    private lateinit var settingsActivity: SettingsBaseActivity
    private lateinit var topologyView: FakeDisplayTopologyPreferenceView

    @Before
    override fun setUp() {
        super.setUp()
        val application = ApplicationProvider.getApplicationContext() as Application

        includeBuiltinDisplay()

        viewModel =
            DisplayPreferenceViewModel(
                application,
                mMockedInjector,
                mActivityManager,
                mActivityTaskManager,
                mDevicePolicyManager,
            )
        topologyView = FakeDisplayTopologyPreferenceView(mMockedInjector)
        initFragment()

        // Spy on the container to verify propagated events
        fragment.selectedDisplayPrefContainer = spy(fragment.selectedDisplayPrefContainer)

        verify(settingsActivity, atLeastOnce()).setOnItemSelectedListener(toolbarListener.capture())
        reset(settingsActivity)
    }

    @Test
    fun noExternalDisplay_showsNoDisplayConnected() {
        // Remove other external displays
        mDisplays = emptyList()
        includeBuiltinDisplay()
        updateDisplaysAndTopology(mDisplays)

        viewModel.updateEnabledDisplays()

        assertThat(fragment.appBarLayout.visibility).isEqualTo(View.GONE)
        assertThat(fragment.selectedDisplayPrefContainer.visibility).isEqualTo(View.GONE)
        assertThat(fragment.noDisplayConnectedLayout.visibility).isEqualTo(View.VISIBLE)
        verify(settingsActivity).setFloatingToolbarVisibility(false)
        verify(settingsActivity).removeOnItemSelectedListener()
        verify(settingsActivity).setToolbarItems(toolbarItemsCaptor.capture())
        assertThat(toolbarItemsCaptor.value.size).isEqualTo(0)
    }

    @Test
    fun withExternalDisplay_showsMainContent() {
        viewModel.updateEnabledDisplays()

        assertThat(fragment.appBarLayout.visibility).isEqualTo(View.VISIBLE)
        assertThat(fragment.selectedDisplayPrefContainer.visibility).isEqualTo(View.VISIBLE)
        assertThat(fragment.noDisplayConnectedLayout.visibility).isEqualTo(View.GONE)
        verify(settingsActivity).setToolbarSelectedItem(/* position= */ 0)
        verify(settingsActivity).setFloatingToolbarVisibility(true)
        verify(settingsActivity, atLeastOnce()).setToolbarItems(toolbarItemsCaptor.capture())
        assertThat(toolbarItemsCaptor.value.size).isEqualTo(3)
    }

    @Test
    fun onDisplayBlockSelected_updatesViewModel() {
        val selectedDisplayId = mDisplays.first { it.isConnectedDisplay }.id
        val listener = topologyView.selectedListener

        listener?.onSelected(selectedDisplayId)

        val updatedState = viewModel.uiState.value
        assertThat(updatedState?.selectedDisplayId).isEqualTo(selectedDisplayId)
    }

    @Test
    fun onToolbarItemSelected_updatesViewModel() {
        val listener = toolbarListener.value
        val secondDisplayId = mDisplays[1].id

        listener.onItemSelected(
            /* position= */ 1,
            ScrollableToolbarItemLayout.ToolbarItem(/* iconResId= */ null, ""),
        )

        val updatedState = viewModel.uiState.value
        assertThat(updatedState?.selectedDisplayId).isEqualTo(secondDisplayId)
    }

    @Test
    fun onStateUpdate_selectsCorrectDisplayBlockAndToolbarItem() {
        val secondDisplayId = mDisplays[1].id

        viewModel.updateSelectedDisplay(secondDisplayId)

        assertThat(topologyView.selectedDisplay).isEqualTo(secondDisplayId)
        verify(settingsActivity).setToolbarSelectedItem(/* position= */ 1)
    }

    @Test
    fun inMirroringMode_selectsNoDisplayInTopologyView() {
        android.provider.Settings.Secure.putInt(
            (ApplicationProvider.getApplicationContext() as Application).contentResolver,
            android.provider.Settings.Secure.MIRROR_BUILT_IN_DISPLAY,
            1,
        )
        viewModel.mirrorModeObserver.onChange(/* selfChange= */ false)

        assertThat(topologyView.selectedDisplay).isEqualTo(-1)
    }

    @Test
    fun setupAppBarLayout_onMouseScroll_propagatesEventWithClampedY() {
        // Verifies that a mouse scroll event is propagated to the container below the app bar.
        // Also verifies that the Y coordinate of the event is clamped to the container's height.
        val containerSpy = fragment.selectedDisplayPrefContainer
        val containerHeight = 100
        whenever(containerSpy.height).thenReturn(containerHeight)
        val uptime = SystemClock.uptimeMillis()
        val motionEvent =
            MotionEvent.obtain(
                uptime,
                uptime,
                MotionEvent.ACTION_SCROLL,
                /* x= */ 50f,
                /* y= */ 150f,
                /* metaState= */ 0)
        motionEvent.source = InputDevice.SOURCE_MOUSE

        val result = fragment.appBarLayout.dispatchGenericMotionEvent(motionEvent)

        assertThat(result).isTrue()
        verify(containerSpy).dispatchGenericMotionEvent(motionEventCaptor.capture())
        val capturedEvent = motionEventCaptor.value
        // Expected Y is min(150f, 100 - 1) = 99f. The event is offset to have this new Y.
        assertThat(capturedEvent.y).isEqualTo(99f)
        motionEvent.recycle()
    }

    @Test
    fun setupAppBarLayout_onNonMouseScrollAction_doesNotPropagateEvent() {
        // Verifies that a non-scroll motion event is not propagated.
        val containerSpy = fragment.selectedDisplayPrefContainer
        val uptime = SystemClock.uptimeMillis()
        val motionEvent =
            MotionEvent.obtain(
                uptime,
                uptime,
                MotionEvent.ACTION_MOVE, // Not a scroll action
                /* x= */50f,
                /* y= */50f,
                /* metaState= */ 0
            )
        motionEvent.source = InputDevice.SOURCE_MOUSE

        val result = fragment.appBarLayout.dispatchGenericMotionEvent(motionEvent)

        assertThat(result).isFalse()
        verify(containerSpy, never()).dispatchGenericMotionEvent(any())
        motionEvent.recycle()
    }

    @Test
    fun setupAppBarLayout_onNonMouseScrollSource_doesNotPropagateEvent() {
        // Verifies that a scroll event from a non-mouse source is not propagated.
        val containerSpy = fragment.selectedDisplayPrefContainer
        val uptime = SystemClock.uptimeMillis()
        val motionEvent =
            MotionEvent.obtain(
                uptime,
                uptime,
                MotionEvent.ACTION_SCROLL,
                /* x= */ 50f,
                /* y= */ 50f,
                /* metaState= */ 0)
        motionEvent.source = InputDevice.SOURCE_TOUCHSCREEN // Not a mouse source

        val result = fragment.appBarLayout.dispatchGenericMotionEvent(motionEvent)

        assertThat(result).isFalse()
        verify(containerSpy, never()).dispatchGenericMotionEvent(any())
        motionEvent.recycle()
    }

    @Test
    fun onDestroyView_removesGenericMotionListener() {
        // Verifies that the motion listener on the app bar is removed when the view is destroyed.
        val appBarLayoutSpy = spy(fragment.appBarLayout)
        fragment.appBarLayout = appBarLayoutSpy

        fragment.onDestroyView()

        verify(appBarLayoutSpy).setOnGenericMotionListener(null)
    }

    private fun initFragment(): TestableTabbedDisplayPreferenceFragment {
        activityScenarioRule.scenario.onActivity { activity ->
            settingsActivity = spy(activity)
            fragment =
                TestableTabbedDisplayPreferenceFragment(topologyView, settingsActivity, viewModel)
            activity.supportFragmentManager.beginTransaction().add(fragment, "testTag").commitNow()
        }
        return fragment
    }

    class TestableTabbedDisplayPreferenceFragment(
        val topologyView: DisplayTopologyPreferenceView,
        val settingsActivity: SettingsBaseActivity,
        viewModel: DisplayPreferenceViewModel,
    ) : TabbedDisplayPreferenceFragment(viewModel) {

        private val mockViewLifecycleOwner: LifecycleOwner = mock()
        val lifecycleRegistry = LifecycleRegistry(this)

        init {
            whenever(mockViewLifecycleOwner.lifecycle).thenReturn(lifecycleRegistry)
            // Required to allow observer to start observing data
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
        }

        override fun createDisplayTopologyPreferenceView(): DisplayTopologyPreferenceView {
            return topologyView
        }

        override fun getCurrentActivity(): SettingsBaseActivity? {
            return settingsActivity
        }

        override fun getViewLifecycleOwner(): LifecycleOwner {
            return mockViewLifecycleOwner
        }
    }

    class FakeDisplayTopologyPreferenceView(injector: ConnectedDisplayInjector) :
        DisplayTopologyPreferenceView(injector) {

        var selectedListener: DisplayTopologyPreferenceController.OnDisplayBlockSelectedListener? =
            null
        var selectedDisplay: Int = -1

        override fun onAttachedToWindow() {
            super.onAttachedToWindow()
        }

        override fun onDetachedFromWindow() {
            super.onDetachedFromWindow()
        }

        override fun setOnDisplayBlockSelectedListener(
            listener: DisplayTopologyPreferenceController.OnDisplayBlockSelectedListener
        ) {
            super.setOnDisplayBlockSelectedListener(listener)
            selectedListener = listener
        }

        override fun removeOnDisplayBlockSelectedListener() {
            super.removeOnDisplayBlockSelectedListener()
            selectedListener = null
        }

        override fun selectDisplay(displayId: Int) {
            super.selectDisplay(displayId)
            selectedDisplay = displayId
        }
    }
}
