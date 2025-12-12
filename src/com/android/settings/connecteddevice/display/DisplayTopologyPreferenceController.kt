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

import android.content.Context
import android.graphics.PointF
import android.graphics.RectF
import android.hardware.display.DisplayTopology
import android.hardware.display.DisplayTopology.TreeNode
import android.os.SystemClock
import android.util.Log
import android.util.Size
import android.view.Display.DEFAULT_DISPLAY
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import androidx.annotation.VisibleForTesting
import com.android.settings.R
import com.android.settings.core.instrumentation.SettingsStatsLog
import java.util.function.Consumer
import kotlin.math.abs
import kotlin.math.min

/**
 * Controller class containing the shared logic for displaying and managing a display topology UI.
 * This class is framework-agnostic and can be used by both a Preference and a custom View.
 */
class DisplayTopologyPreferenceController(
    private val context: Context,
    private val injector: ConnectedDisplayInjector,
) {
    @VisibleForTesting lateinit var paneContent: FrameLayout
    @VisibleForTesting lateinit var paneHolder: FrameLayout
    @VisibleForTesting lateinit var topologyHint: TextView

    /**
     * How many physical pixels to move in pane coordinates (Pythagorean distance) before a drag is
     * considered non-trivial and intentional.
     *
     * This value is computed on-demand so that the injector can be changed at any time.
     */
    @VisibleForTesting
    val accidentalDragDistancePx
        get() = DisplayTopology.dpToPx(4f, injector.densityDpi)

    /** How long before until a tap is considered a drag regardless of distance moved. */
    @VisibleForTesting val accidentalDragTimeLimitMs = 800L
    @VisibleForTesting var timesRefreshedBlocks = 0

    private val topologyListener = Consumer<DisplayTopology> { applyTopology(it) }
    private val displayListener =
        object : ExternalDisplaySettingsConfiguration.DisplayListener() {
            override fun update(displayId: Int) {
                applyDisplayUpdateInMirroringMode()
            }
        }

    private val paneContentLayoutListener =
        View.OnLayoutChangeListener { v, newLeft, _, newRight, _, oldLeft, _, oldRight, _ ->
            val oldWidth = oldRight - oldLeft
            val newWidth = newRight - newLeft
            // The width will change e.g. when first displaying the topology UI (oldWidth is 0)
            // or when the window is resized. We ignore when the height is changed because we don't
            // specify the height in terms of layout, but specify it algorithmically via
            // TopologyScale, which uses the width and the topology to calculate a height.
            if (oldWidth != newWidth) {
                Log.i(TAG, "Width changed from $oldWidth to $newWidth - refresh pane")
                refreshPane()
            }
        }

    private var topologyInfo: TopologyInfo? = null
    private var blockDrag: BlockDrag? = null
    private var revealedWallpapers: List<RevealedWallpaper> = emptyList()
    private var selectedDisplayId: Int = -1

    var onDisplayBlockSelectedListener: OnDisplayBlockSelectedListener? = null

    interface OnDisplayBlockSelectedListener {
        fun onSelected(displayId: Int)
    }

    /**
     * Holds information about the current system topology.
     *
     * @param positions list of displays comprised of the display ID and position
     */
    private data class TopologyInfo(
        val topology: DisplayTopology,
        val scaling: TopologyScale,
        val positions: List<Pair<Int, RectF>>,
    )

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
     *   wanted to drag rather than just click
     */
    private data class BlockDrag(
        val stationaryDisps: List<Pair<Int, RectF>>,
        val display: DisplayBlock,
        val displayId: Int,
        val displayWidth: Float,
        val displayHeight: Float,
        val initialBlockX: Float,
        val initialBlockY: Float,
        val initialTouchX: Float,
        val initialTouchY: Float,
        val startTimeMs: Long,
    )

    /** Binds the views from the concrete implementation (Preference or View). */
    fun bindViews(holder: FrameLayout, content: FrameLayout, hint: TextView) {
        if (this::paneContent.isInitialized && this.paneContent != content) {
            this.paneContent.removeOnLayoutChangeListener(paneContentLayoutListener)
        }
        paneHolder = holder
        paneContent = content
        topologyHint = hint
        paneContent.addOnLayoutChangeListener(paneContentLayoutListener)
    }

    /** Called by the host when it is attached to the window/screen. */
    fun attach() {
        injector.registerTopologyListener(topologyListener)
        injector.registerDisplayListener(displayListener)
    }

    /** Called by the host when it is detached from the window/screen. */
    fun detach() {
        if (this::paneContent.isInitialized) {
            paneContent.removeOnLayoutChangeListener(paneContentLayoutListener)
        }
        // No longer need to reveal wallpapers since the blocks are not visible; these will be
        // revealed again upon invocation of refreshPane.
        revealedWallpapers.forEach { it.viewManager.removeView(it.revealer) }
        revealedWallpapers = listOf()
        injector.unregisterTopologyListener(topologyListener)
        injector.unregisterDisplayListener(displayListener)
    }

    fun selectDisplay(displayId: Int) {
        selectedDisplayId = displayId
        val displayTopology = injector.displayTopology
        if (displayTopology == null || !displayTopology.allNodesIdMap().containsKey(displayId)) {
            displayBlocks().forEach { it.setHighlighted(false) }
            return
        }
        displayBlocks().forEach { it.setHighlighted(it.logicalDisplayId == displayId) }
    }

    @VisibleForTesting
    fun refreshPane() {
        if (!this::paneContent.isInitialized) {
            return
        }
        val topology = injector.displayTopology
        if (topology == null) {
            // This occurs when no topology is active.
            // TODO(b/352648432): show main display or mirrored displays rather than an empty pane.
            topologyHint.text = ""
            paneContent.removeAllViews()
            topologyInfo = null
            return
        }
        applyTopology(topology)
        applyDisplayUpdateInMirroringMode()
    }

    /**
     * Updating DisplayTopology pane consists of multiple steps:
     * 1. Update hint text
     * 2. Prepare display blocks positioning
     * 3. Adjust display blocks bounds and scale within the pane
     * 4. Ensure wallpapers are revealed
     */
    private fun applyTopology(topology: DisplayTopology) {
        // If stacked mirroring display is turned on, updates will come from DisplayListener since
        // there's no more topology update when display is added / removed
        if (!this::paneContent.isInitialized || showStackedMirroringDisplay()) {
            return
        }
        val topologyBounds = topology.absoluteBounds
        // Step 1
        topologyHint.text =
            if (topologyBounds.size() > 1) {
                context.getString(R.string.external_display_topology_hint)
            } else {
                ""
            }
        // Step 2
        val oldBounds = topologyInfo?.positions
        val newBounds = buildList {
            (0..topologyBounds.size() - 1).forEach {
                add(Pair(topologyBounds.keyAt(it), topologyBounds.valueAt(it)))
            }
        }
        if (
            oldBounds != null &&
                oldBounds.size == newBounds.size &&
                oldBounds.zip(newBounds).all { (old, new) ->
                    old.first == new.first && sameDisplayPosition(old.second, new.second)
                }
        ) {
            return
        }
        // Step 3
        val idToNode = topology.allNodesIdMap()
        val logicalDisplaySizeFetcher = LogicalDisplaySizeFetcher(injector, idToNode)
        val scaling =
            TopologyScale(
                paneContent.width,
                minEdgeLength = DisplayTopology.dpToPx(MIN_EDGE_LENGTH_DP, injector.densityDpi),
                maxEdgeLength = DisplayTopology.dpToPx(MAX_EDGE_LENGTH_DP, injector.densityDpi),
                newBounds.map { it.second },
            )
        setupDisplayPaneAndBlocks(
            scaling,
            newBounds,
            logicalDisplaySizeFetcher,
            /* isMirroring= */ false,
        )
        topologyInfo = TopologyInfo(topology, scaling, newBounds)
        // Step 4
        revealWallpapers(idToNode.keys.toSet())
    }

    /**
     * Updating DisplayTopology pane consists of multiple steps:
     * 1. Remove hint text
     * 2. Prepare display blocks positioning
     * 3. Adjust display blocks bounds and scale within the pane
     * 4. Ensure wallpapers are revealed for mirrored display and removed for other displays
     */
    private fun applyDisplayUpdateInMirroringMode() {
        // If stacked mirroring display is turned off, update will be handled by topology update
        if (!this::paneContent.isInitialized || !showStackedMirroringDisplay()) {
            return
        }
        // Step 1
        topologyHint.text = ""
        // Step 2
        val logicalDisplaySizeFetcher = LogicalDisplaySizeFetcher(injector, emptyMap())
        val newBounds = processDisplayBoundsMirroringMode(logicalDisplaySizeFetcher)
        // Step 3
        val scaling =
            TopologyScale(
                paneContent.width,
                minEdgeLength = DisplayTopology.dpToPx(MIN_EDGE_LENGTH_DP, injector.densityDpi),
                maxEdgeLength = DisplayTopology.dpToPx(MAX_EDGE_LENGTH_DP, injector.densityDpi),
                newBounds.map { it.second },
            )
        setupDisplayPaneAndBlocks(
            scaling,
            newBounds,
            logicalDisplaySizeFetcher,
            /* isMirroring= */ true,
        )
        topologyInfo = null
        // Step 4
        revealWallpapers(setOf(DEFAULT_DISPLAY))
    }

    private fun revealWallpapers(displayIdsToRevealWallpaper: Set<Int>) {
        // Construct a map containing revealers that we want to keep (keepRevealing). Then create a
        // list comprised of the values of that map as well as new revealers (revealedWallpapers).
        val keepRevealing =
            buildMap<Int, RevealedWallpaper> {
                revealedWallpapers.forEach { r ->
                    if (displayIdsToRevealWallpaper.contains(r.displayId)) {
                        put(r.displayId, r)
                    } else {
                        r.viewManager.removeView(r.revealer)
                    }
                }
            }
        revealedWallpapers =
            displayIdsToRevealWallpaper
                .map { keepRevealing.get(it) ?: injector.revealWallpaper(it) }
                .filterNotNull()
                .toList()
    }

    private fun processDisplayBoundsMirroringMode(
        logicalDisplaySizeFetcher: LogicalDisplaySizeFetcher
    ): List<Pair<Int, RectF>> {
        val displayIds =
            injector
                .getDisplays()
                .filter { it.isEnabled == DisplayIsEnabled.YES }
                .map { it.id }
                .sortedBy { it }

        val bounds = mutableListOf<Pair<Int, RectF>>()
        val mirroringDiagonalStackOffsetPx =
            DisplayTopology.dpToPx(MIRRORING_DIAGONAL_STACK_OFFSET_DP, injector.densityDpi)

        // Displays are arranged 45 degrees diagonally, with DEFAULT_DISPLAY on the front and
        // leftmost, and other displays on the back, top-right of the display on the front.
        for (i in 0..displayIds.size - 1) {
            val displayId = displayIds[i]
            val offsetPx = mirroringDiagonalStackOffsetPx * i
            logicalDisplaySizeFetcher.get(displayId)?.let {
                bounds.add(
                    Pair(
                        displayId,
                        RectF(offsetPx, -offsetPx, it.width + offsetPx, it.height - offsetPx),
                    )
                )
            }
        }
        // Reverse the z-order to make the first added display (DEFAULT_DISPLAY) on the front.
        return bounds.reversed()
    }

    private fun setupDisplayPaneAndBlocks(
        scaling: TopologyScale,
        newBounds: List<Pair<Int, RectF>>,
        logicalDisplaySizeFetcher: LogicalDisplaySizeFetcher,
        isMirroring: Boolean,
    ) {
        // Resize pane holder
        paneHolder.layoutParams.let {
            val newHeight = scaling.paneHeight.toInt()
            if (it.height != newHeight) {
                it.height = newHeight
                paneHolder.layoutParams = it
            }
        }

        // Setup display blocks. This recreates objects, but using a recycled View
        val displayBlocks = displayBlocks()
        newBounds.forEach { (id, pos) ->
            val block =
                displayBlocks.removeFirstOrNull()
                    ?: DisplayBlock(injector).apply { paneContent.addView(this) }

            // Mirroring is only supported for DEFAULT_DISPLAY for now
            val displayIdToShowWallpaper = if (isMirroring) DEFAULT_DISPLAY else id
            val displaySize = logicalDisplaySizeFetcher.get(displayIdToShowWallpaper)
            if (displaySize == null) {
                // Should not happen, just a safety-null check
                block.visibility = View.GONE
                return@forEach
            }
            val topLeft = scaling.displayToPaneCoor(pos.left, pos.top)
            val bottomRight = scaling.displayToPaneCoor(pos.right, pos.bottom)
            // In non-mirroring, both X and Y scale will be the same, in mirroring mode however,
            // it could be different and needs to apply letterboxing strategy
            val scaleX = (bottomRight.x - topLeft.x) / displaySize.width
            val scaleY = (bottomRight.y - topLeft.y) / displaySize.height
            // Letterboxing strategy: Use the smaller scale to ensure the whole display fits the
            // displayBlock view
            val displaySurfaceToBlockScale = min(scaleX, scaleY)
            block.reset(
                id,
                displayIdToShowWallpaper,
                topLeft,
                bottomRight,
                displaySurfaceToBlockScale,
                displaySize,
            )

            block.onA11yMoveListener = { direction -> simulateA11yDrag(id, pos, block, direction) }

            // This is needed to ensure block is highlighted from the start if it's selected.
            // Example scenario would be when Display#2 is selected from the tab, and there's
            // another display added, Display#2 should still be highlighted.
            block.setHighlighted(id == selectedDisplayId)

            if (isMirroring) {
                block.setOnTouchListener(null)
            } else {
                block.setOnTouchListener { view, ev ->
                    if (ev.isSynthesizedTouchpadGesture()) {
                        return@setOnTouchListener false
                    }
                    when (ev.actionMasked) {
                        MotionEvent.ACTION_DOWN -> onBlockTouchDown(id, pos, block, ev)
                        MotionEvent.ACTION_MOVE -> onBlockTouchMove(ev)
                        MotionEvent.ACTION_UP -> onBlockTouchUp(ev)
                        else -> false
                    }
                }
            }
        }
        paneContent.removeViews(newBounds.size, displayBlocks.size)
        timesRefreshedBlocks++
        // Cancel the drag if one is in progress.
        blockDrag = null
    }

    private fun simulateA11yDrag(
        displayId: Int,
        displayPos: RectF,
        block: DisplayBlock,
        direction: Direction,
    ) {
        val moveDistancePx = DisplayTopology.dpToPx(A11Y_MOVE_DISTANCE_DP, injector.densityDpi)

        val startPoint = PointF(block.x + block.width / 2, block.y + block.height / 2)
        val endPoint =
            when (direction) {
                Direction.UP -> PointF(startPoint.x, startPoint.y - moveDistancePx)
                Direction.DOWN -> PointF(startPoint.x, startPoint.y + moveDistancePx)
                Direction.LEFT -> PointF(startPoint.x - moveDistancePx, startPoint.y)
                Direction.RIGHT -> PointF(startPoint.x + moveDistancePx, startPoint.y)
            }

        val downTime = SystemClock.uptimeMillis()

        val screenPos = IntArray(2)
        paneContent.getLocationOnScreen(screenPos)
        val offset = PointF(screenPos[0].toFloat(), screenPos[1].toFloat())

        // 1. ACTION_DOWN
        val downEvent =
            createMotionEvent(downTime, downTime, MotionEvent.ACTION_DOWN, startPoint, offset)
        onBlockTouchDown(displayId, displayPos, block, downEvent)
        downEvent.recycle()

        // 2. ACTION_MOVE
        for (i in 1..A11Y_DRAG_STEPS) {
            val progress = i.toFloat() / A11Y_DRAG_STEPS
            val currentX = startPoint.x + (endPoint.x - startPoint.x) * progress
            val currentY = startPoint.y + (endPoint.y - startPoint.y) * progress
            val eventTime = SystemClock.uptimeMillis()

            val moveEvent =
                createMotionEvent(
                    downTime,
                    eventTime,
                    MotionEvent.ACTION_MOVE,
                    PointF(currentX, currentY),
                    offset,
                )

            onBlockTouchMove(moveEvent)
            moveEvent.recycle()
        }

        // 3. ACTION_UP
        val upEventTime = SystemClock.uptimeMillis()
        val upEvent =
            createMotionEvent(downTime, upEventTime, MotionEvent.ACTION_UP, endPoint, offset)

        onBlockTouchUp(upEvent)
        upEvent.recycle()
    }

    private fun onBlockTouchDown(
        displayId: Int,
        displayPos: RectF,
        block: DisplayBlock,
        ev: MotionEvent,
    ): Boolean {
        val positions = (topologyInfo ?: return false).positions

        // Do not allow dragging for single-display topology, since there is nothing to clamp it to.
        if (positions.size <= 1) {
            return false
        }

        val stationaryDisps = positions.filter { it.first != displayId }

        selectDisplay(displayId)

        // We have to use rawX and rawY for the coordinates since the view receiving the event is
        // also the view that is moving. We need coordinates relative to something that isn't
        // moving, and the raw coordinates are relative to the screen.
        val initialTopLeft = block.positionInPane
        blockDrag =
            BlockDrag(
                stationaryDisps.toList(),
                block,
                displayId,
                displayPos.width(),
                displayPos.height(),
                initialBlockX = initialTopLeft.x,
                initialBlockY = initialTopLeft.y,
                initialTouchX = ev.rawX,
                initialTouchY = ev.rawY,
                startTimeMs = ev.eventTime,
            )

        // Prevents a container of this view from intercepting the touch events in the case the
        // pointer moves outside of the display block or the pane.
        paneContent.requestDisallowInterceptTouchEvent(true)
        return true
    }

    private fun onBlockTouchMove(ev: MotionEvent): Boolean {
        val drag = blockDrag ?: return false
        val topology = topologyInfo ?: return false
        val dispDragCoor =
            topology.scaling.paneToDisplayCoor(
                ev.rawX - drag.initialTouchX + drag.initialBlockX,
                ev.rawY - drag.initialTouchY + drag.initialBlockY,
            )
        val dispDragRect =
            RectF(
                dispDragCoor.x,
                dispDragCoor.y,
                dispDragCoor.x + drag.displayWidth,
                dispDragCoor.y + drag.displayHeight,
            )
        val snapRect = clampPosition(drag.stationaryDisps.map { it.second }, dispDragRect)

        drag.display.positionInPane =
            topology.scaling.displayToPaneCoor(snapRect.left, snapRect.top)

        return true
    }

    private fun onBlockTouchUp(ev: MotionEvent): Boolean {
        val drag = blockDrag ?: return false
        val topology = topologyInfo ?: return false
        paneContent.requestDisallowInterceptTouchEvent(false)
        if (!injector.flags.showTabbedConnectedDisplaySetting()) {
            // Highlight must be removed after drag finished in non-tabbed setting
            selectDisplay(-1)
        }
        // DisplayBlock has been highlighted on touch down, touch up should only notify the listener
        onDisplayBlockSelectedListener?.onSelected(drag.displayId)

        val dropTopLeft = drag.display.positionInPane
        val netPxDragged =
            Math.hypot(
                (drag.initialBlockX - dropTopLeft.x).toDouble(),
                (drag.initialBlockY - dropTopLeft.y).toDouble(),
            )
        val timeDownMs = ev.eventTime - drag.startTimeMs
        if (netPxDragged < accidentalDragDistancePx && timeDownMs < accidentalDragTimeLimitMs) {
            drag.display.positionInPane = PointF(drag.initialBlockX, drag.initialBlockY)
            return true
        }

        val newCoor = topology.scaling.paneToDisplayCoor(dropTopLeft.x, dropTopLeft.y)
        val newTopology = topology.topology.copy()
        val newPositions =
            drag.stationaryDisps
                .map { (id, pos) -> id to PointF(pos.left, pos.top) }
                .plus(drag.displayId to newCoor)

        val arr = hashMapOf(*newPositions.toTypedArray())
        newTopology.rearrange(arr)

        // Setting topologyInfo to null forces applyTopology to skip the no-op drag check. This is
        // necessary because we don't know if newTopology.rearrange has mutated the topology away
        // from what the user has dragged into position.
        topologyInfo = null
        applyTopology(newTopology)

        injector.displayTopology = newTopology
        val logger = ExternalDisplaySettingsLoggerStore.getLogger(drag.displayId)
        logger.log(SettingsStatsLog.EXTERNAL_DISPLAY_SETTINGS_CHANGED__SETTING__TOPOLOGY)

        return true
    }

    private fun showStackedMirroringDisplay() =
        isDisplayInMirroringMode(context) &&
            injector.flags.showStackedMirroringDisplayConnectedDisplaySetting()

    private fun displayBlocks(): ArrayDeque<DisplayBlock> {
        val blocks = ArrayDeque<DisplayBlock>()
        if (this::paneContent.isInitialized) {
            for (i in 0..paneContent.childCount - 1) {
                // Recycle existing views
                val view = paneContent.getChildAt(i)
                if (view is DisplayBlock) {
                    blocks.add(view)
                }
            }
        }
        return blocks
    }

    private fun sameDisplayPosition(a: RectF, b: RectF): Boolean {
        // Comparing in display coordinates, so a 1 pixel difference will be less than one dp in
        // pane coordinates. Canceling the drag and refreshing the pane will not change the apparent
        // position of displays in the pane.
        val EPSILON = 1f
        return abs(a.left - b.left) < EPSILON &&
            abs(a.right - b.right) < EPSILON &&
            abs(a.top - b.top) < EPSILON &&
            abs(a.bottom - b.bottom) < EPSILON
    }

    /**
     * A simple wrapper class to fetch logical display size from either DisplayTopology or directly
     * from DisplayManager. This should used as a temporary variable only for the current
     * DisplayTopology update.
     */
    private class LogicalDisplaySizeFetcher(
        val injector: ConnectedDisplayInjector,
        idToNode: Map<Int?, TreeNode?>,
    ) {
        private val topologyLogicalDisplaySize =
            idToNode
                .filter { it.key != null && it.value != null }
                .map { it.key!! to Size(it.value!!.logicalWidth, it.value!!.logicalHeight) }
                .toMap()

        fun get(id: Int): Size? {
            // First check from DisplayTopology for quick lookup on logical display size. If display
            // is not in topology, then query from DisplayInfo.
            return topologyLogicalDisplaySize.get(id) ?: injector.getLogicalSize(id)
        }
    }

    private companion object {
        private const val MIN_EDGE_LENGTH_DP = 60f
        private const val MAX_EDGE_LENGTH_DP = 256f
        private const val MIRRORING_DIAGONAL_STACK_OFFSET_DP = 120f
        private const val TAG = "DisplayTopologyPreferenceController"
    }
}
