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

import com.android.settings.R
import com.android.settingslib.widget.GroupSectionDividerMixin

import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.PointF
import android.graphics.RectF
import android.hardware.display.DisplayManager
import android.hardware.display.DisplayTopology
import android.util.DisplayMetrics
import android.view.DisplayInfo
import android.view.MotionEvent
import android.view.ViewTreeObserver
import android.widget.FrameLayout
import android.widget.TextView

import androidx.annotation.VisibleForTesting
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder

import java.util.function.Consumer

import kotlin.math.abs

/**
 * DisplayTopologyPreference allows the user to change the display topology
 * when there is one or more extended display attached.
 */
class DisplayTopologyPreference(context : Context)
        : Preference(context), ViewTreeObserver.OnGlobalLayoutListener, GroupSectionDividerMixin {
    @VisibleForTesting lateinit var mPaneContent : FrameLayout
    @VisibleForTesting lateinit var mPaneHolder : FrameLayout
    @VisibleForTesting lateinit var mTopologyHint : TextView

    @VisibleForTesting var injector : Injector

    /**
     * How many physical pixels to move in pane coordinates (Pythagorean distance) before a drag is
     * considered non-trivial and intentional.
     *
     * This value is computed on-demand so that the injector can be changed at any time.
     */
    @VisibleForTesting val accidentalDragDistancePx
        get() = DisplayTopology.dpToPx(4f, injector.densityDpi)

    /**
     * How long before until a tap is considered a drag regardless of distance moved.
     */
    @VisibleForTesting val accidentalDragTimeLimitMs = 800L

    /**
     * This is needed to prevent a repopulation of the pane causing another
     * relayout and vice-versa ad infinitum.
     */
    private var mPaneNeedsRefresh = false

    private val mTopologyListener = Consumer<DisplayTopology> { applyTopology(it) }

    init {
        layoutResource = R.layout.display_topology_preference

        // Prevent highlight when hovering with mouse.
        isSelectable = false

        isPersistent = false

        isCopyingEnabled = false

        injector = Injector(context)
    }

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)

        val newPane = holder.findViewById(R.id.display_topology_pane_content) as FrameLayout
        if (this::mPaneContent.isInitialized) {
            if (newPane == mPaneContent) {
                return
            }
            mPaneContent.viewTreeObserver.removeOnGlobalLayoutListener(this)
        }
        mPaneContent = newPane
        mPaneHolder = holder.itemView as FrameLayout
        mTopologyHint = holder.findViewById(R.id.topology_hint) as TextView
        mPaneContent.viewTreeObserver.addOnGlobalLayoutListener(this)
    }

    override fun onAttached() {
        super.onAttached()
        // We don't know if topology changes happened when we were detached, as it is impossible to
        // listen at that time (we must remove listeners when detaching). Setting this flag makes
        // the following onGlobalLayout call refresh the pane.
        mPaneNeedsRefresh = true
        injector.registerTopologyListener(mTopologyListener)
    }

    override fun onDetached() {
        super.onDetached()
        injector.unregisterTopologyListener(mTopologyListener)
    }

    override fun onGlobalLayout() {
        if (mPaneNeedsRefresh) {
            mPaneNeedsRefresh = false
            refreshPane()
        }
    }

    open class Injector(val context : Context) {
        /**
         * Lazy property for Display Manager, to prevent eagerly getting the service in unit tests.
         */
        private val displayManager : DisplayManager by lazy {
            context.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
        }

        open var displayTopology : DisplayTopology?
            get() = displayManager.displayTopology
            set(value) { displayManager.displayTopology = value }

        open val wallpaper: Bitmap?
            get() = WallpaperManager.getInstance(context).bitmap

        /**
         * This density is the density of the current display (showing the topology pane). It is
         * necessary to use this density here because the topology pane coordinates are in physical
         * pixels, and the display coordinates are in density-independent pixels.
         */
        open val densityDpi: Int by lazy {
            val info = DisplayInfo()
            if (context.display.getDisplayInfo(info)) {
                info.logicalDensityDpi
            } else {
                DisplayMetrics.DENSITY_DEFAULT
            }
        }

        open fun registerTopologyListener(listener: Consumer<DisplayTopology>) {
            displayManager.registerTopologyListener(context.mainExecutor, listener)
        }

        open fun unregisterTopologyListener(listener: Consumer<DisplayTopology>) {
            displayManager.unregisterTopologyListener(listener)
        }
    }

    /**
     * Holds information about the current system topology.
     * @param positions list of displays comprised of the display ID and position
     */
    private data class TopologyInfo(
            val topology: DisplayTopology, val scaling: TopologyScale,
            val positions: List<Pair<Int, RectF>>)

    /**
     * Holds information about the current drag operation. The initial rawX, rawY values of the
     * cursor are recorded in order to detect whether the drag was a substantial drag or likely
     * accidental.
     *
     * @param stationaryDisps ID and position of displays that are not moving
     * @param display View that is currently being dragged
     * @param displayId ID of display being dragged
     * @param displayWidth width of display being dragged in actual (not View) coordinates
     * @param displayHeight height of display being dragged in actual (not View) coordinates
     * @param initialBlockX block's X coordinate upon touch down event
     * @param initialBlockY block's Y coordinate upon touch down event
     * @param initialTouchX rawX value of the touch down event
     * @param initialTouchY rawY value of the touch down event
     * @param startTimeMs time when tap down occurred, needed to detect the user intentionally
     *                    wanted to drag rather than just click
     */
    private data class BlockDrag(
            val stationaryDisps : List<Pair<Int, RectF>>,
            val display: DisplayBlock, val displayId: Int,
            val displayWidth: Float, val displayHeight: Float,
            val initialBlockX: Float, val initialBlockY: Float,
            val initialTouchX: Float, val initialTouchY: Float,
            val startTimeMs: Long)

    private var mTopologyInfo : TopologyInfo? = null
    private var mDrag : BlockDrag? = null

    private fun sameDisplayPosition(a: RectF, b: RectF): Boolean {
        // Comparing in display coordinates, so a 1 pixel difference will be less than one dp in
        // pane coordinates. Canceling the drag and refreshing the pane will not change the apparent
        // position of displays in the pane.
        val EPSILON = 1f
        return EPSILON > abs(a.left - b.left) &&
                EPSILON > abs(a.right - b.right) &&
                EPSILON > abs(a.top - b.top) &&
                EPSILON > abs(a.bottom - b.bottom)
    }

    @VisibleForTesting fun refreshPane() {
        val topology = injector.displayTopology
        if (topology == null) {
            // This occurs when no topology is active.
            // TODO(b/352648432): show main display or mirrored displays rather than an empty pane.
            mTopologyHint.text = ""
            mPaneContent.removeAllViews()
            mTopologyInfo = null
            return
        }

        applyTopology(topology)
    }

    @VisibleForTesting var mTimesRefreshedBlocks = 0

    private fun applyTopology(topology: DisplayTopology) {
        mTopologyHint.text = context.getString(R.string.external_display_topology_hint)

        val oldBounds = mTopologyInfo?.positions
        val newBounds = buildList {
            val bounds = topology.absoluteBounds
            (0..bounds.size()-1).forEach {
                add(Pair(bounds.keyAt(it), bounds.valueAt(it)))
            }
        }

        if (oldBounds != null && oldBounds.size == newBounds.size &&
                oldBounds.zip(newBounds).all { (old, new) ->
                    old.first == new.first && sameDisplayPosition(old.second, new.second)
                }) {
            return
        }

        val recycleableBlocks = ArrayDeque<DisplayBlock>()
        for (i in 0..mPaneContent.childCount-1) {
            recycleableBlocks.add(mPaneContent.getChildAt(i) as DisplayBlock)
        }

        val scaling = TopologyScale(
                mPaneContent.width,
                minEdgeLength = DisplayTopology.dpToPx(60f, injector.densityDpi),
                maxEdgeLength = DisplayTopology.dpToPx(256f, injector.densityDpi),
                newBounds.map { it.second }.toList())
        mPaneHolder.layoutParams.let {
            val newHeight = scaling.paneHeight.toInt()
            if (it.height != newHeight) {
                it.height = newHeight
                mPaneHolder.layoutParams = it
            }
        }

        var wallpaperBitmap : Bitmap? = null

        newBounds.forEach { (id, pos) ->
            val block = recycleableBlocks.removeFirstOrNull() ?: DisplayBlock(context).apply {
                if (wallpaperBitmap == null) {
                    wallpaperBitmap = injector.wallpaper
                }
                // We need a separate wallpaper Drawable for each display block, since each needs to
                // be drawn at a separate size.
                setWallpaper(wallpaperBitmap)

                mPaneContent.addView(this)
            }
            block.setHighlighted(false)

            block.placeAndSize(pos, scaling)
            block.setOnTouchListener { view, ev ->
                when (ev.actionMasked) {
                    MotionEvent.ACTION_DOWN -> onBlockTouchDown(id, pos, block, ev)
                    MotionEvent.ACTION_MOVE -> onBlockTouchMove(ev)
                    MotionEvent.ACTION_UP -> onBlockTouchUp(ev)
                    else -> false
                }
            }
        }
        mPaneContent.removeViews(newBounds.size, recycleableBlocks.size)
        mTimesRefreshedBlocks++

        mTopologyInfo = TopologyInfo(topology, scaling, newBounds)

        // Cancel the drag if one is in progress.
        mDrag = null
    }

    private fun onBlockTouchDown(
            displayId: Int, displayPos: RectF, block: DisplayBlock, ev: MotionEvent): Boolean {
        val positions = (mTopologyInfo ?: return false).positions

        // Do not allow dragging for single-display topology, since there is nothing to clamp it to.
        if (positions.size <= 1) { return false }

        val stationaryDisps = positions.filter { it.first != displayId }

        mDrag?.display?.setHighlighted(false)
        block.setHighlighted(true)

        // We have to use rawX and rawY for the coordinates since the view receiving the event is
        // also the view that is moving. We need coordinates relative to something that isn't
        // moving, and the raw coordinates are relative to the screen.
        mDrag = BlockDrag(
                stationaryDisps.toList(), block, displayId, displayPos.width(), displayPos.height(),
                initialBlockX = block.x, initialBlockY = block.y,
                initialTouchX = ev.rawX, initialTouchY = ev.rawY,
                startTimeMs = ev.eventTime,
        )

        // Prevents a container of this view from intercepting the touch events in the case the
        // pointer moves outside of the display block or the pane.
        mPaneContent.requestDisallowInterceptTouchEvent(true)
        return true
    }

    private fun onBlockTouchMove(ev: MotionEvent): Boolean {
        val drag = mDrag ?: return false
        val topology = mTopologyInfo ?: return false
        val dispDragCoor = topology.scaling.paneToDisplayCoor(
                ev.rawX - drag.initialTouchX + drag.initialBlockX,
                ev.rawY - drag.initialTouchY + drag.initialBlockY)
        val dispDragRect = RectF(
                dispDragCoor.x, dispDragCoor.y,
                dispDragCoor.x + drag.displayWidth, dispDragCoor.y + drag.displayHeight)
        val snapRect = clampPosition(drag.stationaryDisps.map { it.second }, dispDragRect)

        drag.display.place(topology.scaling.displayToPaneCoor(snapRect.left, snapRect.top))

        return true
    }

    private fun onBlockTouchUp(ev: MotionEvent): Boolean {
        val drag = mDrag ?: return false
        val topology = mTopologyInfo ?: return false
        mPaneContent.requestDisallowInterceptTouchEvent(false)
        drag.display.setHighlighted(false)

        val netPxDragged = Math.hypot(
                (drag.initialBlockX - drag.display.x).toDouble(),
                (drag.initialBlockY - drag.display.y).toDouble())
        val timeDownMs = ev.eventTime - drag.startTimeMs
        if (netPxDragged < accidentalDragDistancePx && timeDownMs < accidentalDragTimeLimitMs) {
            drag.display.x = drag.initialBlockX
            drag.display.y = drag.initialBlockY
            return true
        }

        val newCoor = topology.scaling.paneToDisplayCoor(
                drag.display.x, drag.display.y)
        val newTopology = topology.topology.copy()
        val newPositions = drag.stationaryDisps.map { (id, pos) -> id to PointF(pos.left, pos.top) }
                .plus(drag.displayId to newCoor)

        val arr = hashMapOf(*newPositions.toTypedArray())
        newTopology.rearrange(arr)

        // Setting mTopologyInfo to null forces applyTopology to skip the no-op drag check. This is
        // necessary because we don't know if newTopology.rearrange has mutated the topology away
        // from what the user has dragged into position.
        mTopologyInfo = null
        applyTopology(newTopology)

        injector.displayTopology = newTopology

        return true
    }
}
