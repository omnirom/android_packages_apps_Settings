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

import android.graphics.PointF
import android.graphics.RectF

import java.util.Locale

import kotlin.math.max
import kotlin.math.min

// These extension methods make calls to min and max chainable.
fun Float.atMost(n: Number): Float = min(this, n.toFloat())
fun Float.atLeast(n: Number): Float = max(this, n.toFloat())

/**
 * Contains the parameters needed for transforming global display coordinates to and from topology
 * pane coordinates. This is necessary for implementing an interactive display topology pane. The
 * pane allows dragging and dropping display blocks into place to define the topology. Conversion to
 * pane coordinates is necessary when rendering the original topology. Conversion in the other
 * direction, to display coordinates, is necessary for resolve a drag position to display space.
 *
 * The topology pane coordinates are physical pixels and represent the relative position from the
 * upper-left corner of the pane. It uses a scale optimized for showing all displays with minimal
 * or no scrolling. The display coordinates are floating point and the origin can be in any
 * position. In practice the origin will be the upper-left coordinate of the primary display.
 *
 * @param paneWidth width of the pane in view coordinates
 * @param minEdgeLength the smallest length permitted of a display block. This should be set based
 *                      on accessibility requirements, but also accounting for padding that appears
 *                      around each button.
 * @param maxEdgeLength the longest width or height permitted of a display block. This will limit
 *                      the amount of dragging and scrolling the user will need to do to set the
 *                      arrangement.
 * @param displaysPos the absolute topology coordinates for each display in the topology.
 */
class TopologyScale(
        paneWidth: Int, minEdgeLength: Float, maxEdgeLength: Float,
        displaysPos: Collection<RectF>) {
    /** Scale of block sizes to real-world display sizes. Should be less than 1. */
    val blockRatio: Float

    /** Height of topology pane needed to allow all display blocks to appear with some padding. */
    val paneHeight: Float

    /** Pane's X view coordinate that corresponds with topology's X=0 coordinate. */
    val originPaneX: Float

    /** Pane's Y view coordinate that corresponds with topology's Y=0 coordinate. */
    val originPaneY: Float

    init {
        val displayBounds = RectF(
                Float.MAX_VALUE, Float.MAX_VALUE, Float.MIN_VALUE, Float.MIN_VALUE)
        var smallestDisplayDim = Float.MAX_VALUE
        var biggestDisplayDim = Float.MIN_VALUE

        // displayBounds is the smallest rect encompassing all displays, in display space.
        // smallestDisplayDim is the size of the smallest display edge, in display space.
        for (pos in displaysPos) {
            displayBounds.union(pos)
            smallestDisplayDim = minOf(smallestDisplayDim, pos.height(), pos.width())
            biggestDisplayDim = maxOf(biggestDisplayDim, pos.height(), pos.width())
        }

        // Initialize blockRatio such that there is 20% padding on left and right sides of the
        // display bounds.
        blockRatio = (paneWidth * 0.6 / displayBounds.width()).toFloat()
                // If the `ratio` is set too high because one of the displays will have an edge
                // greater than maxEdgeLength(px) long, decrease it such that the largest edge is
                // that long.
                .atMost(maxEdgeLength / biggestDisplayDim)
                // Also do the opposite of the above, this latter step taking precedence for a11y
                // requirements.
                .atLeast(minEdgeLength / smallestDisplayDim)

        // A tall pane is likely to result in more scrolling. So we
        // prevent the height from growing too large here, by limiting vertical padding to
        // 1.5x of the minEdgeLength on each side. This keeps a comfortable amount of
        // padding without it resulting in too much deadspace.
        paneHeight = blockRatio * displayBounds.height() + minEdgeLength * 3f

        // Set originPaneXY (the location of 0,0 in display space in the pane's coordinate system)
        // such that the display bounds rect is centered in the pane.
        // It is unlikely that either of these coordinates will be negative since blockRatio has
        // been chosen to allow 20% padding around each side of the display blocks. However, the
        // a11y requirement applied above (minEdgeLength / smallestDisplayDim) may cause the blocks
        // to not fit. This should be rare in practice, and can be worked around by moving the
        // settings UI to a larger display.
        val blockMostLeft = (paneWidth - displayBounds.width() * blockRatio) / 2
        val blockMostTop = (paneHeight - displayBounds.height() * blockRatio) / 2

        originPaneX = blockMostLeft - displayBounds.left * blockRatio
        originPaneY = blockMostTop - displayBounds.top * blockRatio
    }

    /** Transforms coordinates in view pane space to display space. */
    fun paneToDisplayCoor(paneX: Float, paneY: Float): PointF {
        return PointF((paneX - originPaneX) / blockRatio, (paneY - originPaneY) / blockRatio)
    }

    /** Transforms coordinates in display space to view pane space. */
    fun displayToPaneCoor(displayX: Float, displayY: Float): PointF {
        return PointF(displayX * blockRatio + originPaneX, displayY * blockRatio + originPaneY)
    }

    override fun toString() : String {
        return String.format(
                Locale.ROOT,
                "{TopologyScale blockRatio=%f originPaneXY=%.1f,%.1f paneHeight=%.1f}",
                blockRatio, originPaneX, originPaneY, paneHeight)
    }
}
