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

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.PointF
import android.graphics.RectF
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.LayerDrawable
import android.widget.Button

import androidx.annotation.VisibleForTesting

/** Represents a draggable block in the topology pane. */
class DisplayBlock(context : Context) : Button(context) {
    @VisibleForTesting var mSelectedImage: Drawable = ColorDrawable(Color.BLACK)
    @VisibleForTesting var mUnselectedImage: Drawable = ColorDrawable(Color.BLACK)

    private val mSelectedBg = context.getDrawable(
            R.drawable.display_block_selection_marker_background)!!
    private val mUnselectedBg = context.getDrawable(
            R.drawable.display_block_unselected_background)!!
    private val mInsetPx = context.resources.getDimensionPixelSize(R.dimen.display_block_padding)

    init {
        isScrollContainer = false
        isVerticalScrollBarEnabled = false
        isHorizontalScrollBarEnabled = false

        // Prevents shadow from appearing around edge of button.
        stateListAnimator = null
    }

    /** Sets position of the block given unpadded coordinates. */
    fun place(topLeft: PointF) {
        x = topLeft.x
        y = topLeft.y
    }

    fun setWallpaper(wallpaper: Bitmap?) {
        val wallpaperDrawable = BitmapDrawable(context.resources, wallpaper ?: return)

        fun framedBy(bg: Drawable): Drawable =
            LayerDrawable(arrayOf(wallpaperDrawable, bg)).apply {
                setLayerInsetRelative(0, mInsetPx, mInsetPx, mInsetPx, mInsetPx)
            }
        mSelectedImage = framedBy(mSelectedBg)
        mUnselectedImage = framedBy(mUnselectedBg)
    }

    fun setHighlighted(value: Boolean) {
        background = if (value) mSelectedImage else mUnselectedImage
    }

    /** Sets position and size of the block given unpadded bounds. */
    fun placeAndSize(bounds : RectF, scale : TopologyScale) {
        val topLeft = scale.displayToPaneCoor(bounds.left, bounds.top)
        val bottomRight = scale.displayToPaneCoor(bounds.right, bounds.bottom)
        val layout = layoutParams
        layout.width = (bottomRight.x - topLeft.x).toInt()
        layout.height = (bottomRight.y - topLeft.y).toInt()
        layoutParams = layout
        place(topLeft)
    }
}
