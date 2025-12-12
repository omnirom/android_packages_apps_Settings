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

import android.os.Bundle
import android.view.InputDevice
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.android.internal.annotations.VisibleForTesting
import com.android.settings.R
import com.android.settings.core.SettingsBaseActivity
import com.android.settingslib.collapsingtoolbar.widget.ScrollableToolbarItemLayout
import com.google.common.collect.HashBiMap
import kotlin.math.min

/**
 * The main fragment that holds both the DisplayTopologyPreferenceView and the
 * SelectedDisplayPreferenceFragment, isolating them from each other to prevent redraw issues.
 */
open class TabbedDisplayPreferenceFragment(
    private val testViewModel: DisplayPreferenceViewModel? = null
) : Fragment() {

    @VisibleForTesting internal lateinit var appBarLayout: View

    @VisibleForTesting
    internal lateinit var displayTopologyPreferenceView: DisplayTopologyPreferenceView

    @VisibleForTesting internal lateinit var selectedDisplayPrefContainer: View

    @VisibleForTesting internal lateinit var noDisplayConnectedLayout: View

    private lateinit var viewModel: DisplayPreferenceViewModel

    // Map toolbar indices to display IDs
    private val toolbarIdxToDisplayIdMapping: HashBiMap<Int, Int> = HashBiMap.create()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (testViewModel != null) {
            // Test-only path
            viewModel = testViewModel
        } else {
            viewModel = ViewModelProvider(this).get(DisplayPreferenceViewModel::class.java)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        return inflater.inflate(R.layout.tabbed_display_settings_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val activity = getCurrentActivity() ?: return
        activity.setTitle(R.string.external_display_settings_title)
        appBarLayout = view.findViewById(R.id.app_bar_layout)
        selectedDisplayPrefContainer = view.findViewById(R.id.selected_display_preference_container)
        noDisplayConnectedLayout = view.findViewById(R.id.no_display_connected_layout)

        setupAppBarLayout()
        setupDisplayTopologyPreferenceView(view)
        setupSelectedDisplayPreferenceFragment(savedInstanceState)
        startListeningForUpdates()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        if (::displayTopologyPreferenceView.isInitialized) {
            displayTopologyPreferenceView.removeOnDisplayBlockSelectedListener()
        }
        if (::appBarLayout.isInitialized) {
            appBarLayout.setOnGenericMotionListener(null)
        }
        getCurrentActivity()?.removeOnItemSelectedListener()
    }

    @VisibleForTesting
    internal open fun createDisplayTopologyPreferenceView(): DisplayTopologyPreferenceView {
        return DisplayTopologyPreferenceView(viewModel.injector)
    }

    @VisibleForTesting
    internal open fun getCurrentActivity(): SettingsBaseActivity? {
        return getActivity() as? SettingsBaseActivity
    }

    private fun startListeningForUpdates() {
        displayTopologyPreferenceView.setOnDisplayBlockSelectedListener(
            object : DisplayTopologyPreferenceController.OnDisplayBlockSelectedListener {
                override fun onSelected(displayId: Int) {
                    viewModel.updateSelectedDisplay(displayId)
                }
            }
        )
        viewModel.uiState.observe(viewLifecycleOwner) { state ->
            val enabledDisplays = state.enabledDisplays
            val selectedDisplayId = state.selectedDisplayId
            val isMirroring = state.isMirroring

            // Determine if there are any connected external displays to show the main UI
            val hasConnectedExternalDisplay = enabledDisplays.values.any { it.isConnectedDisplay }

            if (hasConnectedExternalDisplay) {
                showMainViews()
                val highlightedDisplayId = if (isMirroring) -1 else selectedDisplayId
                displayTopologyPreferenceView.selectDisplay(highlightedDisplayId)
                updateToolbarDisplays(enabledDisplays, selectedDisplayId)
            } else {
                hideMainViews()
                clearToolbar()
            }
        }
    }

    private fun showMainViews() {
        appBarLayout.visibility = View.VISIBLE
        selectedDisplayPrefContainer.visibility = View.VISIBLE
        noDisplayConnectedLayout.visibility = View.GONE
    }

    private fun hideMainViews() {
        appBarLayout.visibility = View.GONE
        selectedDisplayPrefContainer.visibility = View.GONE
        noDisplayConnectedLayout.visibility = View.VISIBLE
    }

    private fun setupAppBarLayout() {
        // TODO(b/428853467): AppBarLayout doesn't support mouse scrolling
        appBarLayout.setOnGenericMotionListener { view, event ->
            if (
                event.action != MotionEvent.ACTION_SCROLL ||
                    !event.isFromSource(InputDevice.SOURCE_MOUSE)
            ) {
                return@setOnGenericMotionListener false
            }
            // Propagate scroll events to selectedDisplayPrefContainer to match the
            // `appbar_scrolling_view_behavior` property
            val newEvent = MotionEvent.obtain(event)
            // Limit the Y position as any yCoord >= target.height will be ignored
            val maxY = min(newEvent.y, (selectedDisplayPrefContainer.height - 1).toFloat())
            newEvent.offsetLocation(/* deltaX= */ 0f, maxY - newEvent.y)
            selectedDisplayPrefContainer.dispatchGenericMotionEvent(newEvent)
            newEvent.recycle()
            return@setOnGenericMotionListener true
        }
    }

    private fun setupDisplayTopologyPreferenceView(layoutView: View) {
        val topologyContainer =
            layoutView.findViewById<ViewGroup>(R.id.display_topology_preference_container)
        displayTopologyPreferenceView = createDisplayTopologyPreferenceView()
        topologyContainer.addView(displayTopologyPreferenceView)
    }

    private fun setupSelectedDisplayPreferenceFragment(savedInstanceState: Bundle?) {
        if (savedInstanceState != null) {
            // Fragment is restored from fragment manager
            return
        }
        childFragmentManager
            .beginTransaction()
            .replace(
                R.id.selected_display_preference_container,
                SelectedDisplayPreferenceFragment(viewModel),
            )
            .commit()
    }

    private fun clearToolbar() {
        val activity = getCurrentActivity() ?: return
        // Remove existing listener to not get updated when toolbar items are re-added
        activity.removeOnItemSelectedListener()
        toolbarIdxToDisplayIdMapping.clear()
        activity.setFloatingToolbarVisibility(false)
        activity.setToolbarItems(emptyList())
    }

    private fun updateToolbarDisplays(
        enabledDisplays: Map<Int, DisplayDevice>,
        selectedDisplayId: Int,
    ) {
        val activity = getCurrentActivity() ?: return
        clearToolbar()

        val toolbarItems = mutableListOf<ScrollableToolbarItemLayout.ToolbarItem>()
        enabledDisplays.values.forEachIndexed { i, display ->
            val displayId = display.id
            if (!display.isConnectedDisplay) {
                toolbarItems.add(
                    ScrollableToolbarItemLayout.ToolbarItem(
                        R.drawable.display_category_builtin_display_icon,
                        requireContext().getString(R.string.builtin_display_settings_category),
                    )
                )
            } else {
                toolbarItems.add(
                    ScrollableToolbarItemLayout.ToolbarItem(
                        R.drawable.display_category_connected_display_icon,
                        display.name,
                    )
                )
            }
            toolbarIdxToDisplayIdMapping[i] = displayId
        }

        activity.setToolbarItems(toolbarItems)
        activity.setFloatingToolbarVisibility(true)
        activity.setOnItemSelectedListener(
            object : ScrollableToolbarItemLayout.OnItemSelectedListener {
                override fun onItemSelected(
                    position: Int,
                    toolbarItem: ScrollableToolbarItemLayout.ToolbarItem,
                ) {
                    toolbarIdxToDisplayIdMapping[position]?.let { displayId ->
                        viewModel.updateSelectedDisplay(displayId)
                    }
                }
            }
        )
        toolbarIdxToDisplayIdMapping.inverse().get(selectedDisplayId)?.let { toolbarPosition ->
            activity.setToolbarSelectedItem(toolbarPosition)
        }
    }
}
