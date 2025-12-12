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

package com.android.settings.restriction

import android.content.Context
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceFragmentCompat
import com.android.settingslib.datastore.HandlerExecutor
import com.android.settingslib.datastore.KeyedObserver
import com.android.settingslib.metadata.PreferenceChangeReason
import com.android.settingslib.metadata.PreferenceHierarchyNode
import com.android.settingslib.preference.PreferenceScreenBindingHelper

/** Helper to rebind preference immediately when user restriction is changed. */
class UserRestrictionBindingHelper(
    fragment: PreferenceFragmentCompat,
    private val screenBindingHelper: PreferenceScreenBindingHelper,
) : KeyedObserver<String>, AutoCloseable {
    private val context: Context = fragment.requireContext()
    private val restrictionKeysToPreferenceKeys = mutableMapOf<String, MutableSet<String>>()

    init {
        screenBindingHelper.forEachAsyncRecursively(::addNode, fragment.lifecycleScope) { _, node ->
            // node is added to hierarchy in async manner
            addNode(node)
        }
    }

    private fun addNode(node: PreferenceHierarchyNode) {
        val metadata = node.metadata
        val restrictionKeys =
            (metadata as? PreferenceRestrictionMixin)?.restrictionKeys ?: emptyArray()
        if (restrictionKeys.isEmpty()) return
        val userRestrictions = UserRestrictions.get(context)
        val executor = HandlerExecutor.main
        fun addObserver(restrictionKey: String) =
            userRestrictions.addObserver(
                restrictionKey,
                this@UserRestrictionBindingHelper,
                executor,
            )
        val key = metadata.key
        for (restrictionKey in restrictionKeys) {
            restrictionKeysToPreferenceKeys
                .getOrPut(restrictionKey) {
                    addObserver(restrictionKey)
                    mutableSetOf()
                }
                .add(key)
        }
    }

    override fun onKeyChanged(restrictionKey: String, reason: Int) {
        val keys = restrictionKeysToPreferenceKeys[restrictionKey] ?: return
        for (key in keys) screenBindingHelper.notifyChange(key, PreferenceChangeReason.STATE)
    }

    override fun close() {
        val restrictionKeys = restrictionKeysToPreferenceKeys.keys
        if (restrictionKeys.isNotEmpty()) {
            val userRestrictions = UserRestrictions.get(context)
            for (restrictionKey in restrictionKeys) {
                userRestrictions.removeObserver(restrictionKey, this)
            }
        }
    }
}
