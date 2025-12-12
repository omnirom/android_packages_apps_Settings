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

package com.android.settings.accessibility.textreading.data

/** Data class for storing the configurations related to the display size. */
data class DisplaySize(val currentIndex: Int, val values: IntArray, val defaultValue: Int) {

    // Auto generated equals.
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as DisplaySize

        if (currentIndex != other.currentIndex) return false
        if (defaultValue != other.defaultValue) return false
        if (!values.contentEquals(other.values)) return false

        return true
    }

    // Auto generated hashCode
    override fun hashCode(): Int {
        var result = currentIndex
        result = 31 * result + defaultValue
        result = 31 * result + values.contentHashCode()
        return result
    }
}
