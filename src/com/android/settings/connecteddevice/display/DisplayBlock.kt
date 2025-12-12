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

import android.graphics.Outline
import android.graphics.PointF
import android.graphics.Rect
import android.os.Bundle
import android.os.Trace
import android.util.Log
import android.util.Size
import android.view.SurfaceControl
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import android.view.ViewOutlineProvider
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityNodeInfo.AccessibilityAction
import android.widget.FrameLayout
import androidx.annotation.VisibleForTesting
import com.android.settings.R
import com.android.settingslib.utils.ThreadUtils
import kotlin.time.Duration.Companion.milliseconds

/** Represents a draggable block in the topology pane. */
class DisplayBlock(val injector: ConnectedDisplayInjector) : FrameLayout(injector.context!!) {
    @VisibleForTesting
    internal val highlightPx =
        context.resources.getDimensionPixelSize(R.dimen.display_block_highlight_width)
    @VisibleForTesting
    internal val cornerRadiusPx =
        context.resources.getDimensionPixelSize(R.dimen.display_block_corner_radius)

    // Id of the logical display this DisplayBlock represents
    var logicalDisplayId: Int = -1
        private set

    // This doesn't necessarily refer to the actual display this block represents. In case of
    // mirroring, it will be the id of the mirrored display
    private var displayIdToShowWallpaper: Int? = null

    /** Scale of the mirrored wallpaper to the actual wallpaper size. */
    @VisibleForTesting
    internal var surfaceScale: Float? = null
        private set

    // Size of the surface of the display that would be projected on this block
    @VisibleForTesting
    internal var surfaceSize: Size? = null
        private set

    // These are surfaces which must be removed from the display block hierarchy and released once
    // the new surface is put in place. This list can have more than one item because we may get
    // two reset calls before we get a single surfaceChange callback.
    private val oldSurfaces = mutableListOf<SurfaceControl>()
    private var letterboxBackgroundSurface: SurfaceControl? = null
    private var wallpaperSurface: SurfaceControl? = null

    private val updateSurfaceView = Runnable { updateSurfaceView() }

    private val holderCallback =
        object : SurfaceHolder.Callback {
            override fun surfaceCreated(h: SurfaceHolder) {}

            override fun surfaceChanged(
                h: SurfaceHolder,
                format: Int,
                newWidth: Int,
                newHeight: Int,
            ) {
                updateSurfaceView()
            }

            override fun surfaceDestroyed(h: SurfaceHolder) {}
        }

    val wallpaperView = SurfaceView(context)
    private val backgroundView =
        View(context).apply {
            background = context.getDrawable(R.drawable.display_block_background)
        }

    @VisibleForTesting
    val selectionMarkerView =
        View(context).apply {
            background = context.getDrawable(R.drawable.display_block_selection_marker_background)
        }

    /**
     * The coordinates of the upper-left corner of the block in pane coordinates, not including the
     * highlight border.
     */
    var positionInPane: PointF
        get() = PointF(x + highlightPx, y + highlightPx)
        set(value: PointF) {
            x = value.x - highlightPx
            y = value.y - highlightPx
        }

    var onA11yMoveListener: ((direction: Direction) -> Unit)? = null

    init {
        isScrollContainer = false
        isVerticalScrollBarEnabled = false
        isHorizontalScrollBarEnabled = false

        isFocusable = true
        isScreenReaderFocusable = true

        // Prevents shadow from appearing around edge of button.
        stateListAnimator = null

        addView(wallpaperView)
        addView(backgroundView)
        addView(selectionMarkerView)

        wallpaperView.holder.addCallback(holderCallback)
    }

    @VisibleForTesting
    fun updateSurfaceView() {
        val displayId = displayIdToShowWallpaper ?: return

        if (parent == null) {
            Log.i(TAG, "View for display $displayId has no parent - cancelling update")
            return
        }

        val currentSurface = wallpaperSurface
        if (currentSurface != null) {
            renderSurfaceView(currentSurface)
            return
        }
        ThreadUtils.postOnBackgroundThread {
            Trace.beginSection("Settings Wallpaper fetchSurfaceView $logicalDisplayId")
            val fetchedSurface = injector.wallpaper(displayId)
            Trace.endSection()
            if (fetchedSurface == null) {
                // Fetch failed, schedule a retry
                injector.handler.postDelayed(
                    ::updateSurfaceView,
                    REFETCH_WALLPAPER_DELAY.inWholeMilliseconds,
                )
            } else {
                injector.handler.post { handleFetchedWallpaperSurface(displayId, fetchedSurface) }
            }
        }
    }

    private fun handleFetchedWallpaperSurface(
        fetchedForDisplayId: Int,
        fetchedSurface: SurfaceControl,
    ) {
        // If the view is no longer attached or the target display has changed, ignore the result
        if (parent == null || fetchedForDisplayId != displayIdToShowWallpaper) {
            Log.i(TAG, "Wallpaper display changed or view detached, ignoring stale surface.")
            return
        }
        wallpaperSurface = fetchedSurface
        renderSurfaceView(fetchedSurface)
    }

    /**
     * Reparents surface to the SurfaceControl of wallpaperView, so that view will render `surface`.
     * Any surfaces which may be parented to wallpaperView already should be passed in oldSurfaces
     * and they will be removed from the wallpaperView's hierarchy and released.
     */
    private fun renderSurfaceView(surface: SurfaceControl) {
        val surfaceScale = surfaceScale ?: return
        val surfaceSize = surfaceSize ?: return
        val isMirroringOtherDisplay = logicalDisplayId != displayIdToShowWallpaper

        Trace.beginSection("Settings Wallpaper renderSurfaceView display#$displayIdToShowWallpaper")

        val t = injector.createSurfaceTransaction()
        t.reparent(surface, wallpaperView.surfaceControl)

        // Calculate the final (scaled) dimensions of the wallpaper content
        val scaledSurfaceWidth = surfaceSize.width * surfaceScale
        val scaledSurfaceHeight = surfaceSize.height * surfaceScale
        // Calculate the top-left position needed to center the surface within the wallpaperView
        val positionX = (wallpaperView.width - scaledSurfaceWidth) / 2f
        val positionY = (wallpaperView.height - scaledSurfaceHeight) / 2f
        val destinationCrop = Rect(0, 0, surfaceSize.width, surfaceSize.height)

        t.setScale(surface, surfaceScale, surfaceScale)
        t.setPosition(surface, positionX, positionY)
        t.setCrop(surface, destinationCrop)
        if (isMirroringOtherDisplay) {
            // In mirroring mode, area outside the letterbox will be translucent, causing display
            // behind to be spilling over. The workaround here is to add another black background
            // surface behind the original wallpaper surface
            val backgroundSurface =
                injector
                    .getSurfaceControlBuilder()
                    .setName("Letterbox background display#$logicalDisplayId")
                    .setColorLayer()
                    .build()
            letterboxBackgroundSurface = backgroundSurface

            t.reparent(backgroundSurface, wallpaperView.surfaceControl)
            // Set main wallpaper surface to be on top of the background
            t.setLayer(surface, 1)

            t.setAlpha(backgroundSurface, 0.5f)
            t.show(backgroundSurface)
        } else {
            // Corner radius should only be applied when display is not letterboxed, and has to be
            // set on the original surface size, so apply a inverse scale here.
            t.setCornerRadius(surface, cornerRadiusPx.toFloat() / surfaceScale)
            letterboxBackgroundSurface = null
        }

        oldSurfaces.forEach { t.remove(it) }
        t.apply()
        oldSurfaces.forEach { it.release() }
        oldSurfaces.clear()

        Trace.endSection()
    }

    fun setHighlighted(value: Boolean) {
        selectionMarkerView.visibility = if (value) VISIBLE else INVISIBLE

        // The highlighted block must be draw last so that its highlight shows over the borders of
        // other displays.
        z = if (value) 2f else 1f
    }

    /**
     * Sets position and size of the block given coordinates in pane space.
     *
     * @param logicalDisplayId ID of the logical display this DisplayBlock represents
     * @param displayIdToShowWallpaper ID of the display whose wallpaper would be projected on this
     *   display block.
     * @param topLeft coordinates of top left corner of the block, not including highlight border
     * @param bottomRight coordinates of bottom right corner of the block, not including highlight
     *   border
     * @param surfaceScale scale in pixels of the size of the wallpaper mirror to the actual
     *   wallpaper on the screen - should be less than one to indicate scaling to smaller size
     * @param surfaceSize the size of the surface of the display that would be projected on the
     *   block
     */
    fun reset(
        logicalDisplayId: Int,
        displayIdToShowWallpaper: Int,
        topLeft: PointF,
        bottomRight: PointF,
        surfaceScale: Float,
        surfaceSize: Size,
    ) {
        wallpaperSurface?.let { oldSurfaces.add(it) }
        letterboxBackgroundSurface?.let { oldSurfaces.add(it) }
        injector.handler.removeCallbacks(updateSurfaceView)
        wallpaperSurface = null
        letterboxBackgroundSurface = null
        positionInPane = topLeft

        this.logicalDisplayId = logicalDisplayId
        this.displayIdToShowWallpaper = displayIdToShowWallpaper
        this.surfaceScale = surfaceScale
        this.surfaceSize = surfaceSize

        val displayDevice = injector.getDisplay(logicalDisplayId)
        contentDescription =
            context.getString(
                R.string.external_display_topology_display_block_content_description,
                displayDevice?.name ?: "Display $logicalDisplayId",
            )

        val newWidth = (bottomRight.x - topLeft.x).toInt()
        val newHeight = (bottomRight.y - topLeft.y).toInt()

        val paddedWidth = newWidth + 2 * highlightPx
        val paddedHeight = newHeight + 2 * highlightPx

        if (width == paddedWidth && height == paddedHeight) {
            // Will not receive a surfaceChanged callback, so in case the wallpaper is different,
            // apply it.
            updateSurfaceView()
            return
        }

        layoutParams.let {
            it.width = paddedWidth
            it.height = paddedHeight
            layoutParams = it
        }

        // The highlight is the outermost border. The highlight is shown outside of the parent
        // FrameLayout so that it consumes the padding between the blocks.
        wallpaperView.layoutParams.let {
            it.width = newWidth
            it.height = newHeight
            if (it is MarginLayoutParams) {
                it.leftMargin = highlightPx
                it.topMargin = highlightPx
                it.bottomMargin = highlightPx
                it.topMargin = highlightPx
            }
            wallpaperView.layoutParams = it
        }

        wallpaperView.outlineProvider =
            object : ViewOutlineProvider() {
                override fun getOutline(view: View, outline: Outline) {
                    outline.setRoundRect(0, 0, view.width, view.height, cornerRadiusPx.toFloat())
                }
            }
        wallpaperView.clipToOutline = true

        // The other two child views are MATCH_PARENT by default so will resize to fill up the
        // FrameLayout.
    }

    override fun onInitializeAccessibilityNodeInfo(info: AccessibilityNodeInfo) {
        super.onInitializeAccessibilityNodeInfo(info)
        // Add custom actions for moving the display block
        info.addAction(
            AccessibilityAction(
                R.id.action_move_display_block_up,
                context.getString(R.string.external_display_topology_a11y_action_move_up),
            )
        )
        info.addAction(
            AccessibilityAction(
                R.id.action_move_display_block_down,
                context.getString(R.string.external_display_topology_a11y_action_move_down),
            )
        )
        info.addAction(
            AccessibilityAction(
                R.id.action_move_display_block_left,
                context.getString(R.string.external_display_topology_a11y_action_move_left),
            )
        )
        info.addAction(
            AccessibilityAction(
                R.id.action_move_display_block_right,
                context.getString(R.string.external_display_topology_a11y_action_move_right),
            )
        )
    }

    override fun performAccessibilityAction(action: Int, arguments: Bundle?): Boolean {
        if (onA11yMoveListener == null) {
            return super.performAccessibilityAction(action, arguments)
        }

        when (action) {
            R.id.action_move_display_block_up -> {
                onA11yMoveListener?.invoke(Direction.UP)
                return true
            }

            R.id.action_move_display_block_down -> {
                onA11yMoveListener?.invoke(Direction.DOWN)
                return true
            }

            R.id.action_move_display_block_left -> {
                onA11yMoveListener?.invoke(Direction.LEFT)
                return true
            }

            R.id.action_move_display_block_right -> {
                onA11yMoveListener?.invoke(Direction.RIGHT)
                return true
            }

            else -> return super.performAccessibilityAction(action, arguments)
        }
    }

    private companion object {
        private val REFETCH_WALLPAPER_DELAY = 500.milliseconds
        private const val TAG = "DisplayBlock"
    }
}
