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

package com.android.settings.accessibility.screenmagnification.dialogs

import android.app.Dialog
import android.app.settings.SettingsEnums
import android.content.DialogInterface
import android.os.Bundle
import android.provider.Settings
import android.provider.Settings.Secure.AccessibilityMagnificationCursorFollowingMode
import android.util.Log
import android.view.LayoutInflater
import android.widget.AdapterView
import android.widget.TextView
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.setFragmentResult
import com.android.settings.R
import com.android.settings.accessibility.AccessibilityDialogUtils
import com.android.settings.accessibility.ItemInfoArrayAdapter
import com.android.settings.core.instrumentation.InstrumentedDialogFragment

/** Displays options of how Magnification follows your cursor */
class CursorFollowingModeChooser : InstrumentedDialogFragment() {
    private lateinit var requestKey: String
    private lateinit var modeInfos: List<CursorFollowingModeInfo>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestKey = requireArguments().getString(ARG_REQUEST_KEY, "")
        modeInfos =
            listOf(
                CursorFollowingModeInfo(
                    title =
                        getText(R.string.accessibility_magnification_cursor_following_continuous),
                    mode =
                        Settings.Secure.ACCESSIBILITY_MAGNIFICATION_CURSOR_FOLLOWING_MODE_CONTINUOUS,
                ),
                CursorFollowingModeInfo(
                    title = getText(R.string.accessibility_magnification_cursor_following_center),
                    mode = Settings.Secure.ACCESSIBILITY_MAGNIFICATION_CURSOR_FOLLOWING_MODE_CENTER,
                ),
                CursorFollowingModeInfo(
                    title = getText(R.string.accessibility_magnification_cursor_following_edge),
                    mode = Settings.Secure.ACCESSIBILITY_MAGNIFICATION_CURSOR_FOLLOWING_MODE_EDGE,
                ),
            )
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val listView =
            AccessibilityDialogUtils.createSingleChoiceListView(
                requireContext(),
                modeInfos,
                /* itemListener= */ null,
            )

        val headerView =
            LayoutInflater.from(requireContext())
                .inflate(R.layout.accessibility_dialog_header, listView, /* attachToRoot= */ false)
        headerView
            .requireViewById<TextView>(R.id.accessibility_dialog_header_text_view)
            .setText(R.string.accessibility_magnification_cursor_following_header)
        listView.addHeaderView(headerView, /* data= */ null, /* isSelectable= */ false)

        if (savedInstanceState == null) {
            // Sets up initial selected item
            val selectedMode =
                Settings.Secure.getInt(
                    requireContext().contentResolver,
                    SETTING_KEY,
                    Settings.Secure.ACCESSIBILITY_MAGNIFICATION_CURSOR_FOLLOWING_MODE_CONTINUOUS,
                )
            val selectedIndex =
                listView.adapter.run {
                    for (index in 0 until count) {
                        val modeInfo = getItem(index) as? CursorFollowingModeInfo
                        if (modeInfo?.mode == selectedMode) {
                            return@run index
                        }
                    }
                    AdapterView.INVALID_POSITION
                }
            if (selectedIndex != AdapterView.INVALID_POSITION) {
                listView.setItemChecked(selectedIndex, true)
            }
        }

        return AccessibilityDialogUtils.createCustomDialog(
            requireContext(),
            getText(R.string.accessibility_magnification_cursor_following_title),
            listView,
            getText(R.string.save),
            DialogInterface.OnClickListener { _, _ ->
                val selectedModeInfo: CursorFollowingModeInfo? =
                    listView.checkedItemPosition.let {
                        if (it == AdapterView.INVALID_POSITION) {
                            null
                        } else {
                            listView.adapter.getItem(it) as? CursorFollowingModeInfo
                        }
                    }

                confirmSelection(selectedModeInfo)
            },
            getText(R.string.cancel),
            /* negativeListener= */ null,
        )
    }

    private fun confirmSelection(modeInfo: CursorFollowingModeInfo?) {
        if (modeInfo == null) {
            Log.w(TAG, "Selected positive button with INVALID_POSITION index")
            return
        }

        Settings.Secure.putInt(requireContext().contentResolver, SETTING_KEY, modeInfo.mode)
        setFragmentResult(requestKey, Bundle().apply { putInt(RESULT, modeInfo.mode) })
    }

    override fun getMetricsCategory(): Int {
        return SettingsEnums.DIALOG_MAGNIFICATION_CURSOR_FOLLOWING
    }

    companion object {
        private const val TAG = "CursorFollowingModeChooser"
        private const val SETTING_KEY =
            Settings.Secure.ACCESSIBILITY_MAGNIFICATION_CURSOR_FOLLOWING_MODE
        internal const val ARG_REQUEST_KEY = "requestKey"
        internal const val RESULT = "selectedMode"

        @JvmStatic
        fun showDialog(fragmentManager: FragmentManager, requestKey: String) {
            val bundle = Bundle().apply { putString(ARG_REQUEST_KEY, requestKey) }
            CursorFollowingModeChooser().apply {
                arguments = bundle
                show(fragmentManager, /* tag= */ CursorFollowingModeChooser::class.simpleName)
            }
        }

        @JvmStatic
        @AccessibilityMagnificationCursorFollowingMode
        fun getCheckedModeFromResult(bundle: Bundle): Int {
            return bundle.getInt(RESULT)
        }
    }
}

class CursorFollowingModeInfo(
    title: CharSequence,
    @AccessibilityMagnificationCursorFollowingMode val mode: Int,
) : ItemInfoArrayAdapter.ItemInfo(title, /* summary= */ null, /* drawableId= */ null)
