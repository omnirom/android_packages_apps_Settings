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

package com.android.settings.connecteddevice.display

import android.graphics.PointF
import android.view.InputDevice
import android.view.MotionEvent
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

const val A11Y_DRAG_STEPS = 10
const val A11Y_MOVE_DISTANCE_DP = 16f
val A11Y_MOVE_DURATION_MS: Duration = 200.milliseconds

enum class Direction {
    UP,
    DOWN,
    LEFT,
    RIGHT,
}

fun createMotionEvent(
    downTime: Long,
    eventTime: Long,
    action: Int,
    point: PointF,
    screenPositionOffset: PointF,
): MotionEvent {
    val properties =
        MotionEvent.PointerProperties().apply {
            id = 0
            toolType = MotionEvent.TOOL_TYPE_FINGER
        }
    val coords =
        MotionEvent.PointerCoords().apply {
            x = point.x + screenPositionOffset.x
            y = point.y + screenPositionOffset.y
            pressure = 1f
            size = 1f
        }

    return MotionEvent.obtain(
        downTime,
        eventTime,
        action,
        /* pointerCount= */ 1,
        /* pointerProperties= */ arrayOf(properties),
        /* pointerCoords= */ arrayOf(coords),
        /* metaState= */ 0,
        /* buttonState= */ 0,
        /* xPrecision= */ 1f,
        /* yPrecision= */ 1f,
        /* deviceId= */ 0,
        /* edgeFlags= */ 0,
        /* source= */ InputDevice.SOURCE_TOUCHSCREEN,
        /* flags= */ 0,
    )
}
