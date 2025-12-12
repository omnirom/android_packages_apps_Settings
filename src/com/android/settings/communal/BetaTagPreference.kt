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

package com.android.settings.communal

import android.content.Context
import android.util.AttributeSet
import androidx.preference.Preference
import com.android.settings.R
import com.android.settingslib.widget.GroupSectionDividerMixin
import com.android.settingslib.widget.OnScreenWidgetMixin

/** Preference that displays the "beta" tag on the "widgets on lock screen" settings screen. */
class BetaTagPreference
@JvmOverloads
constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0,
) :
    Preference(context, attrs, defStyleAttr, defStyleRes),
    OnScreenWidgetMixin,
    GroupSectionDividerMixin {

    init {
        layoutResource = R.layout.widgets_beta_tag

        isSelectable = false
        isPersistent = false
    }
}
