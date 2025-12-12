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

import android.content.Context
import android.content.pm.ApplicationInfo
import android.icu.text.CollationKey
import android.icu.text.Collator
import java.util.IdentityHashMap

/** Returns [Comparator] to sort [ApplicationInfo]. */
val Context.applicationInfoComparator: Comparator<ApplicationInfo>
    get() {
        val packageManager = packageManager
        val collator = Collator.getInstance().freeze()
        val cachedLabels = IdentityHashMap<ApplicationInfo, CollationKey>()
        return compareBy(
            {
                cachedLabels.getOrPut(it) {
                    collator.getCollationKey(it.loadLabel(packageManager).toString())
                }
            },
            { it.packageName },
            { it.uid },
        )
    }
