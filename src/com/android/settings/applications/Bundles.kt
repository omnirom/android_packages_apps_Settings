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

package com.android.settings.applications

import android.os.Bundle

private const val KEY_PACKAGE_NAME = "pkg"
private const val KEY_INTENT_SOURCE = "source"

/** Saves a package name to [Bundle] with key [KEY_PACKAGE_NAME]. */
fun String.toArguments(capacity: Int = 1) =
    Bundle(capacity).also { it.putString(KEY_PACKAGE_NAME, this) }

/** Returns the package name in Bundle with key [KEY_PACKAGE_NAME]. */
val Bundle.packageName: String
    get() = getString(KEY_PACKAGE_NAME)!!

/**
 * Returns where the [Bundle] is coming from with key [KEY_INTENT_SOURCE].
 *
 * <p> Indicates where this intent is coming from.
 */
val Bundle.source: String
    get() = getString(KEY_INTENT_SOURCE, "")
