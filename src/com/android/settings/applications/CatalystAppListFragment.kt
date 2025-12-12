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
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import androidx.lifecycle.lifecycleScope
import com.android.settings.CatalystFragment
import com.android.settings.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Catalyst fragment to show app list.
 *
 * Missing pieces:
 * - empty list
 * - search app
 */
open class CatalystAppListFragment : CatalystFragment() {
    private val showSystem
        get() = preferenceHierarchyType as Boolean

    private val showSystemMenuTitle
        get() = if (showSystem) R.string.menu_hide_system else R.string.menu_show_system

    override fun onSaveHierarchyType(outState: Bundle, hierarchyType: Any) {
        outState.putBoolean(KEY_SHOW_SYSTEM, hierarchyType as Boolean)
    }

    override fun onRestoreHierarchyType(savedInstanceState: Bundle?) =
        savedInstanceState?.getBoolean(KEY_SHOW_SYSTEM) ?: DEFAULT_SHOW_SYSTEM

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        menu.add(Menu.NONE, MENU_SHOW_SYSTEM, Menu.NONE, showSystemMenuTitle)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(menuItem: MenuItem) =
        when (menuItem.itemId) {
            MENU_SHOW_SYSTEM -> toggleShowSystem(menuItem)
            else -> super.onOptionsItemSelected(menuItem)
        }

    private fun toggleShowSystem(menuItem: MenuItem): Boolean {
        switchPreferenceHierarchy(!showSystem)
        // delay to avoid menu title flicker
        lifecycleScope.launch {
            delay(100)
            menuItem.setTitle(showSystemMenuTitle)
        }
        return true
    }

    companion object {
        const val DEFAULT_SHOW_SYSTEM = false
        private const val MENU_SHOW_SYSTEM: Int = Menu.FIRST
        private const val KEY_SHOW_SYSTEM = "show_system"
    }
}
