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

package com.android.settings.testutils

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder

fun Preference.inflateViewHolder(): PreferenceViewHolder {
    val inflater = LayoutInflater.from(context)
    val view: View = inflater.inflate(layoutResource, null)
    val viewHolder = PreferenceViewHolder.createInstanceForTests(view)
    if (widgetLayoutResource != 0) {
        inflater.inflate(
            widgetLayoutResource,
            viewHolder.itemView.findViewById<ViewGroup?>(android.R.id.widget_frame)
        )
    }
    onBindViewHolder(viewHolder)
    return viewHolder
}
