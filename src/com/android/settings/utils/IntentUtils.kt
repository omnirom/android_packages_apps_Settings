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

package com.android.settings.utils

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.android.settings.SettingsActivity.EXTRA_FRAGMENT_ARG_KEY
import com.android.settingslib.metadata.EXTRA_BINDING_SCREEN_ARGS

/**
 * Returns the [Intent] to start given settings activity and highlight a specific preference.
 *
 * @param context context
 * @param activityClass activity to start
 * @param key preference key to locate
 */
fun makeLaunchIntent(context: Context, activityClass: Class<out Activity>, key: String?) =
    createIntent(context, activityClass).apply { highlightPreference(key) }

/**
 * Returns the [Intent] to start given settings activity that is parameterized screen and then
 * highlight a specific preference.
 *
 * @param context context
 * @param activityClass activity to start
 * @param arguments arguments of the parameterized screen
 * @param key preference key to locate
 */
fun makeLaunchIntent(
    context: Context,
    activityClass: Class<out Activity>,
    arguments: Bundle,
    key: String?,
) = createIntent(context, activityClass).apply { highlightPreference(arguments, key) }

private fun createIntent(context: Context, activityClass: Class<out Activity>) =
    Intent(context, activityClass).apply {
        // MUST provide an action even no action is specified in AndroidManifest.xml, otherwise
        // SettingsIntelligence starts intent with com.android.settings.SEARCH_RESULT_TRAMPOLINE
        // action instead of given activity.
        action = Intent.ACTION_MAIN
    }

/**
 * Sets the intent extra to highlight given preference on a parameterized screen.
 *
 * @param arguments arguments of the parameterized screen
 * @param key preference to highlight
 */
fun Intent.highlightPreference(arguments: Bundle, key: String?) {
    putExtra(EXTRA_BINDING_SCREEN_ARGS, arguments)
    if (key != null) putExtra(EXTRA_FRAGMENT_ARG_KEY, key)
}

/** Sets the intent extra to highlight given preference. */
fun Intent.highlightPreference(key: String?) {
    if (key != null) putExtra(EXTRA_FRAGMENT_ARG_KEY, key)
}
