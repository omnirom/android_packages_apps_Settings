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
package com.android.settings.supervision.ipc

import android.os.Bundle

/**
 * Data class representing the information of a supported app, used by content filters.
 *
 * This class encapsulates data such as title and package name. It provides constructors for
 * creating instances from a Bundle and for converting instances back to a Bundle.
 *
 * @property title Optional title text for the app.
 * @property packageName Optional package name for the app used to retrieve package information.
 */
data class SupportedApp(
    /** Optional title text for the supported app. */
    var title: CharSequence? = null,
    /** Optional summary text for the supported app. */
    var summary: CharSequence? = null,
    /** Optional package name for the supported app. */
    var packageName: String? = null,
) {
    constructor(
        bundle: Bundle
    ) : this(
        title = bundle.getCharSequence(TITLE),
        summary = bundle.getCharSequence(SUMMARY),
        packageName = bundle.getString(PACKAGE_NAME),
    )

    fun toBundle(): Bundle {
        return Bundle().apply {
            title?.let { putCharSequence(TITLE, it) }
            summary?.let { putCharSequence(SUMMARY, it) }

            packageName?.let { putString(PACKAGE_NAME, it) }
        }
    }

    private companion object {
        const val TITLE = "title"
        const val SUMMARY = "summary"
        const val PACKAGE_NAME = "package_name"
    }
}
