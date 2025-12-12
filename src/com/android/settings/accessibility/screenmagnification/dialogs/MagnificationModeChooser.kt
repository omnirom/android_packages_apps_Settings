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
import android.util.Log
import android.view.LayoutInflater
import android.widget.AdapterView
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.setFragmentResult
import com.android.settings.R
import com.android.settings.accessibility.AccessibilityDialogUtils
import com.android.settings.accessibility.AccessibilityUtil
import com.android.settings.accessibility.ItemInfoArrayAdapter
import com.android.settings.accessibility.MagnificationCapabilities
import com.android.settings.accessibility.MagnificationCapabilities.MagnificationMode
import com.android.settings.core.instrumentation.InstrumentedDialogFragment

/** Displays magnification mode options in a dialog */
class MagnificationModeChooser : InstrumentedDialogFragment() {
    private lateinit var requestKey: String
    private lateinit var modeInfos: List<MagnificationModeInfo>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestKey = requireArguments().getString(ARG_REQUEST_KEY, "")
        modeInfos =
            listOf(
                MagnificationModeInfo(
                    title =
                        getText(
                            R.string.accessibility_magnification_mode_dialog_option_full_screen
                        ),
                    summary = null,
                    drawableId = R.drawable.accessibility_magnification_mode_fullscreen,
                    mode = MagnificationMode.FULLSCREEN,
                ),
                MagnificationModeInfo(
                    title = getText(R.string.accessibility_magnification_mode_dialog_option_window),
                    summary = null,
                    drawableId = R.drawable.accessibility_magnification_mode_window,
                    mode = MagnificationMode.WINDOW,
                ),
                MagnificationModeInfo(
                    title = getText(R.string.accessibility_magnification_mode_dialog_option_switch),
                    summary =
                        getText(
                            R.string.accessibility_magnification_area_settings_mode_switch_summary
                        ),
                    drawableId = R.drawable.accessibility_magnification_mode_switch,
                    mode = MagnificationMode.ALL,
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
            .setText(R.string.accessibility_magnification_area_settings_message)
        listView.addHeaderView(headerView, /* data= */ null, /* isSelectable= */ false)

        if (savedInstanceState == null) {
            // Sets up initial selected item
            val selectedMode = MagnificationCapabilities.getCapabilities(requireContext())
            val selectedIndex =
                listView.adapter.run {
                    for (index in 0 until count) {
                        val modeInfo = getItem(index) as? MagnificationModeInfo
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
            getText(R.string.accessibility_magnification_mode_dialog_title),
            listView,
            getText(R.string.save),
            DialogInterface.OnClickListener { _, _ ->
                val selectedModeInfo: MagnificationModeInfo? =
                    listView.checkedItemPosition.let {
                        if (it == AdapterView.INVALID_POSITION) {
                            null
                        } else {
                            listView.adapter.getItem(it) as? MagnificationModeInfo
                        }
                    }

                confirmSelection(selectedModeInfo)
            },
            getText(R.string.cancel),
            /* negativeListener= */ null,
        )
    }

    private fun confirmSelection(modeInfo: MagnificationModeInfo?) {
        if (modeInfo == null) {
            Log.w(TAG, "Selected positive button with INVALID_POSITION index")
            return
        }
        val mode = modeInfo.mode

        val isTripleTapEnabled =
            Settings.Secure.getInt(
                requireContext().contentResolver,
                Settings.Secure.ACCESSIBILITY_DISPLAY_MAGNIFICATION_ENABLED,
                AccessibilityUtil.State.OFF,
            ) == AccessibilityUtil.State.ON

        if (isTripleTapEnabled && mode != MagnificationMode.FULLSCREEN) {
            TripleTapWarningDialog.showDialog(parentFragmentManager, requestKey, mode)
        } else {
            MagnificationCapabilities.setCapabilities(requireContext(), mode)
            setFragmentResult(requestKey, Bundle().apply { putInt(RESULT, mode) })
        }
    }

    override fun getMetricsCategory(): Int {
        return SettingsEnums.DIALOG_MAGNIFICATION_CAPABILITY
    }

    companion object {
        private const val TAG = "MagnificationModeChooser"
        internal const val ARG_REQUEST_KEY = "requestKey"
        internal const val RESULT = "selectedMode"

        @JvmStatic
        fun showDialog(fragmentManager: FragmentManager, requestKey: String) {
            val bundle = Bundle().apply { putString(ARG_REQUEST_KEY, requestKey) }
            MagnificationModeChooser().apply {
                arguments = bundle
                show(fragmentManager, /* tag= */ MagnificationModeChooser::class.simpleName)
            }
        }

        @JvmStatic
        @MagnificationMode
        fun getCheckedModeFromResult(bundle: Bundle): Int {
            return bundle.getInt(RESULT)
        }
    }
}

class MagnificationModeInfo(
    title: CharSequence,
    summary: CharSequence?,
    @DrawableRes drawableId: Int,
    @MagnificationMode val mode: Int,
) : ItemInfoArrayAdapter.ItemInfo(title, summary, drawableId)
