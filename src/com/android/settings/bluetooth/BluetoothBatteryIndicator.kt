/*
 * Copyright (C) 2025 The Android Open Source Project
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
package com.android.settings.bluetooth

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Path.Direction
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.View
import androidx.core.graphics.PathParser
import com.android.settings.R
import com.android.settingslib.graph.ThemedBatteryDrawable
import kotlin.math.min
import androidx.core.graphics.createBitmap

/** View for bluetooth battery indicator. */
class BluetoothBatteryIndicator @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val ringBackgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }

    private val iconBackgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        strokeCap = Paint.Cap.ROUND
    }

    private val chargingIconPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        strokeCap = Paint.Cap.ROUND
    }

    private val progressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }

    private lateinit var batteryMeterDrawable: ThemedBatteryDrawable

    private var strokeWidth = 0f
    private var deviceIconSize = 0f
    private var batteryIconBackgroundSize = 0f
    private var batteryIconWidth = 0f
    private var batteryIconHeight = 0f
    private var chargingIconWidth = 0f
    private var chargingIconHeight = 0f
    private var strokePadding = 0f
    private var backgroundColor = Color.GRAY
    private var batteryStatusColor = Color.GREEN
    private var batteryIconColor = Color.GREEN

    private val ringRect = RectF()
    private val deviceIconRect = RectF()
    private val deviceIconBackgroundRect = RectF()
    private val batteryIconBackgroundRect = RectF()
    private lateinit var batteryMeterColorFilter: ColorFilter
    private val batteryIconRect = RectF()
    private val chargingIconRect = RectF()
    private val boltPath = Path()
    private val boltPathBound = RectF()

    var batteryLevel: Int = 0
        set(value) {
            field = min(value, MAX_BATTERY)
            updateColors()
            invalidate()
        }

    var charging = false
        set(value) {
            field = value
            updateColors()
            invalidate()
        }

    var deviceIcon: Drawable? = null

    init {
        strokeWidth =
            context.resources.getDimensionPixelSize(R.dimen.bluetooth_battery_indicator_stroke_width)
                .toFloat()
        deviceIconSize =
            context.resources.getDimensionPixelSize(R.dimen.bluetooth_battery_indicator_device_icon_size)
                .toFloat()
        batteryIconBackgroundSize =
            context.resources.getDimensionPixelSize(R.dimen.bluetooth_battery_indicator_battery_background_size)
                .toFloat()
        batteryIconWidth =
            context.resources.getDimensionPixelSize(R.dimen.bluetooth_battery_indicator_battery_meter_width)
                .toFloat()
        batteryIconHeight =
            context.resources.getDimensionPixelSize(R.dimen.bluetooth_battery_indicator_battery_meter_height)
                .toFloat()
        chargingIconWidth =
            context.resources.getDimensionPixelSize(R.dimen.bluetooth_battery_indicator_charging_icon_width)
                .toFloat()
        chargingIconHeight =
            context.resources.getDimensionPixelSize(R.dimen.bluetooth_battery_indicator_charging_icon_height)
                .toFloat()
        strokePadding =
            context.resources.getDimensionPixelSize(R.dimen.bluetooth_battery_indicator_stroke_padding)
                .toFloat()
        backgroundColor =
            context.getColor(com.android.settingslib.widget.theme.R.color.settingslib_materialColorSurfaceBright)

        val boltPathString = context.resources.getString(
            com.android.internal.R.string.config_batterymeterBoltPath
        )
        boltPath.set(PathParser.createPathFromPathData(boltPathString))
        batteryMeterDrawable = ThemedBatteryDrawable(context, 0)

        ringBackgroundPaint.strokeWidth = strokeWidth
        progressPaint.strokeWidth = strokeWidth
        ringBackgroundPaint.color = backgroundColor
        progressPaint.color = batteryStatusColor
        iconBackgroundPaint.color = backgroundColor
    }

    private fun updateColors() {
        batteryStatusColor = getBatteryStatusColor()
        batteryIconColor = getBatteryIconColor()
        batteryMeterColorFilter = PorterDuffColorFilter(
            batteryIconColor, PorterDuff.Mode.SRC
        )
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        val offset = (batteryIconBackgroundSize - strokeWidth) / 2
        val ringRadius = min(w.toFloat(), h - offset) / 2 - strokeWidth / 2
        val ringCenterW = w / 2
        val ringCenterH = (h + offset) / 2
        ringRect.set(
            ringCenterW - ringRadius,
            ringCenterH - ringRadius,
            ringCenterW + ringRadius,
            ringCenterH + ringRadius
        )
        deviceIconBackgroundRect.set(
            ringRect.left + strokeWidth / 2 + strokePadding,
            ringRect.top + strokeWidth / 2 + strokePadding,
            ringRect.right - strokeWidth / 2 - strokePadding,
            ringRect.bottom - strokeWidth / 2 - strokePadding
        )
        deviceIconRect.set(
            deviceIconBackgroundRect.centerX() - deviceIconSize / 2,
            deviceIconBackgroundRect.centerY() - deviceIconSize / 2,
            deviceIconBackgroundRect.centerX() + deviceIconSize / 2,
            deviceIconBackgroundRect.centerY() + deviceIconSize / 2
        )
        batteryIconBackgroundRect.set(
            ringCenterW - batteryIconBackgroundSize / 2,
            ringCenterH - ringRadius - batteryIconBackgroundSize / 2,
            ringCenterW + batteryIconBackgroundSize / 2,
            ringCenterH - ringRadius + batteryIconBackgroundSize / 2
        )
        batteryIconRect.set(
            batteryIconBackgroundRect.centerX() - batteryIconWidth / 2,
            batteryIconBackgroundRect.centerY() - batteryIconHeight / 2,
            batteryIconBackgroundRect.centerX() + batteryIconWidth / 2,
            batteryIconBackgroundRect.centerY() + batteryIconHeight / 2
        )
        chargingIconRect.set(
            batteryIconBackgroundRect.centerX() - chargingIconWidth / 2,
            batteryIconBackgroundRect.centerY() - chargingIconHeight / 2,
            batteryIconBackgroundRect.centerX() + chargingIconWidth / 2,
            batteryIconBackgroundRect.centerY() + chargingIconHeight / 2
        )
        boltPath.computeBounds(boltPathBound)
        val transform = Matrix()
        transform.setRectToRect(boltPathBound, chargingIconRect, Matrix.ScaleToFit.FILL)
        boltPath.transform(transform)
        updateColors()
    }

    private fun getBatteryStatusColor(): Int {
        if (charging) {
            return context.getColor(com.android.settingslib.widget.theme.R.color.settingslib_colorBackgroundLevel_low)
        }
        return when (batteryLevel) {
            in 51..100 -> context.getColor(com.android.settingslib.widget.theme.R.color.settingslib_colorBackgroundLevel_low)
            in 20..50 -> context.getColor(com.android.settingslib.widget.theme.R.color.settingslib_colorBackgroundLevel_medium)
            in 1..19 -> context.getColor(com.android.settingslib.widget.theme.R.color.settingslib_colorBackgroundLevel_high)
            else -> backgroundColor
        }
    }

    private fun getBatteryIconColor(): Int {
        if (charging) {
            return context.getColor(com.android.settingslib.widget.theme.R.color.settingslib_colorContentLevel_low)
        }
        return when (batteryLevel) {
            in 51..100 -> context.getColor(com.android.settingslib.widget.theme.R.color.settingslib_colorContentLevel_low)
            in 20..50 -> context.getColor(com.android.settingslib.widget.theme.R.color.settingslib_colorContentLevel_medium)
            in 1..19 -> context.getColor(com.android.settingslib.widget.theme.R.color.settingslib_colorContentLevel_high)
            else -> context.getColor(com.android.settingslib.widget.theme.R.color.settingslib_materialColorOnSurface)
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        progressPaint.color = batteryStatusColor
        chargingIconPaint.color = batteryIconColor

        if (batteryLevel >= 0) {
            clipOutBatteryIconSurroundingArea(canvas)
            val startAngle = (360f - RING_MAX_ANGLE) / 2 + 270f
            canvas.drawArc(ringRect, startAngle, RING_MAX_ANGLE, false, ringBackgroundPaint)
            val sweepAngle = (batteryLevel.toFloat() / MAX_BATTERY) * RING_MAX_ANGLE
            canvas.drawArc(ringRect, startAngle, sweepAngle, false, progressPaint)
        }

        iconBackgroundPaint.color = backgroundColor
        deviceIcon?.let {
            canvas.drawCircle(
                deviceIconBackgroundRect.centerX(),
                deviceIconBackgroundRect.centerY(),
                deviceIconBackgroundRect.width() / 2,
                iconBackgroundPaint
            )
            canvas.drawBitmap(it.toBitmap(), null, deviceIconRect, null)
        }

        if (batteryLevel >= 0) {
            iconBackgroundPaint.color = batteryStatusColor
            canvas.drawCircle(
                batteryIconBackgroundRect.centerX(),
                batteryIconBackgroundRect.centerY(),
                batteryIconBackgroundRect.width() / 2,
                iconBackgroundPaint
            )
            if (charging) {
                canvas.drawPath(boltPath, chargingIconPaint)
            } else {
                batteryMeterDrawable.setBatteryLevel(batteryLevel)
                batteryMeterDrawable.setColorFilter(batteryMeterColorFilter)
                batteryMeterDrawable.charging = false
                canvas.drawBitmap(
                    batteryMeterDrawable.toBitmap(), null, batteryIconRect, null
                )
            }
        }
    }

    private fun clipOutBatteryIconSurroundingArea(canvas: Canvas) {
        val largePath = Path()
        largePath.addCircle(
            batteryIconBackgroundRect.centerX(),
            batteryIconBackgroundRect.centerY(),
            batteryIconBackgroundSize / 2 + strokePadding,
            Direction.CW
        )
        val smallPath = Path()
        smallPath.addCircle(
            batteryIconBackgroundRect.centerX(),
            batteryIconBackgroundRect.centerY(),
            batteryIconBackgroundSize / 2,
            Direction.CW
        )
        largePath.op(smallPath, Path.Op.DIFFERENCE)
        canvas.clipOutPath(largePath)
    }

    fun Drawable.toBitmap(): Bitmap {
        val bitmap = createBitmap(intrinsicWidth, intrinsicHeight)
        val canvas = Canvas(bitmap)
        setBounds(0, 0, canvas.width, canvas.height)
        draw(canvas)
        return bitmap
    }

    private companion object {
        const val MAX_BATTERY = 100
        const val RING_MAX_ANGLE = 300f
    }
}
