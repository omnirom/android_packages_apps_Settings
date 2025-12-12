/*
 * Copyright 2025 The Android Open Source Project
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

import android.view.LayoutInflater
import android.widget.FrameLayout
import com.android.settings.R

/** View representation to manage display topology arrangement */
open class DisplayTopologyPreferenceView(val injector: ConnectedDisplayInjector) :
    FrameLayout(injector.context!!) {

    private val controller = DisplayTopologyPreferenceController(context, injector)

    init {
        LayoutInflater.from(context)
            .inflate(R.layout.display_topology_preference, this, /* attachToRoot= */ true)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()

        controller.bindViews(
            findViewById(R.id.display_topology_pane_holder),
            findViewById(R.id.display_topology_pane_content),
            findViewById(R.id.topology_hint),
        )
        controller.attach()
        injector.displayTopology?.primaryDisplayId?.let { controller.selectDisplay(it) }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        controller.detach()
    }

    open fun setOnDisplayBlockSelectedListener(
        listener: DisplayTopologyPreferenceController.OnDisplayBlockSelectedListener
    ) {
        controller.onDisplayBlockSelectedListener = listener
    }

    open fun removeOnDisplayBlockSelectedListener() {
        controller.onDisplayBlockSelectedListener = null
    }

    open fun selectDisplay(displayId: Int) {
        controller.selectDisplay(displayId)
    }
}
