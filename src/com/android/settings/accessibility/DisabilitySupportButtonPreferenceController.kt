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

package com.android.settings.accessibility

import android.content.Context
import android.content.Intent
import androidx.preference.PreferenceScreen
import com.android.settings.R
import com.android.settings.accessibility.Flags.enableDisabilitySupport
import com.android.settings.core.BasePreferenceController
import com.android.settingslib.HelpUtils
import com.android.settingslib.widget.ButtonPreference

/** Controller for managing the display and behavior of a disability support button preference. */
class DisabilitySupportButtonPreferenceController(context: Context, prefKey: String) :
    BasePreferenceController(context, prefKey) {

    private val helpIntent: Intent? =
        HelpUtils.getHelpIntent(
            context,
            context.getString(R.string.help_url_accessibility_disability_support),
            context::class.java.name,
        )

    override fun getAvailabilityStatus(): Int =
        if (enableDisabilitySupport() && helpIntent != null) AVAILABLE
        else CONDITIONALLY_UNAVAILABLE

    override fun displayPreference(screen: PreferenceScreen) {
        super.displayPreference(screen)
        val buttonPreference: ButtonPreference? = screen.findPreference(mPreferenceKey)
        buttonPreference?.setOnClickListener { view ->
            helpIntent?.let { view.context.startActivity(it) }
        }
    }
}
