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
package com.android.settings.accessibility.shared

import android.content.Context
import android.content.Intent
import androidx.preference.Preference
import androidx.preference.PreferenceScreen
import com.android.settings.core.BasePreferenceController

/** PreferenceController that will start an intent */
open class LaunchIntentPreferenceController(context: Context, prefKey: String) :
    BasePreferenceController(context, prefKey) {
    private var intent: Intent? = null

    fun setIntent(intent: Intent) {
        this.intent = intent
    }

    override fun getAvailabilityStatus(): Int {
        return intent?.run { AVAILABLE } ?: CONDITIONALLY_UNAVAILABLE
    }

    override fun displayPreference(screen: PreferenceScreen?) {
        super.displayPreference(screen)
        val preference: Preference? = screen?.findPreference(preferenceKey)
        preference?.intent = intent
    }
}