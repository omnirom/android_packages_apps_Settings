/*
 * Copyright (C) 2024 The Android Open Source Project
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

package com.android.settings.connecteddevice.display

import android.widget.FrameLayout
import android.widget.TextView
import androidx.annotation.VisibleForTesting
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder
import com.android.settings.R
import com.android.settingslib.widget.GroupSectionDividerMixin

/**
 * DisplayTopologyPreference allows the user to change the display topology when there is one or
 * more extended display attached.
 */
class DisplayTopologyPreference(val injector: ConnectedDisplayInjector) :
    Preference(injector.context!!), GroupSectionDividerMixin {

    @VisibleForTesting val controller = DisplayTopologyPreferenceController(context, injector)

    init {
        layoutResource = R.layout.display_topology_preference

        // Prevent highlight when hovering with mouse.
        isSelectable = false

        isPersistent = false

        isCopyingEnabled = false
    }

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)

        val paneHolder = holder.itemView as FrameLayout
        val paneContent = holder.findViewById(R.id.display_topology_pane_content) as FrameLayout
        val topologyHint = holder.findViewById(R.id.topology_hint) as TextView
        controller.bindViews(paneHolder, paneContent, topologyHint)
    }

    override fun onAttached() {
        super.onAttached()
        controller.attach()
    }

    override fun onDetached() {
        super.onDetached()
        controller.detach()
    }
}
