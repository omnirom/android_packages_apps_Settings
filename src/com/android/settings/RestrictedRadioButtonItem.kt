/*
 * Copyright (C) 2016 The Android Open Source Project
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
 * limitations under the License
 */

package com.android.settings

import android.app.admin.EnforcingAdmin
import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.widget.CompoundButton
import com.android.settingslib.RestrictedLockUtils.EnforcedAdmin
import com.google.android.setupdesign.items.RadioButtonItem
import com.google.android.setupdesign.view.RichTextView

class RestrictedRadioButtonItem : RadioButtonItem {
    private var _disabledByAdmin: Boolean = false
    public val disabledByAdmin: Boolean
        get() = _disabledByAdmin

    private var _enforcedAdmin: EnforcedAdmin? = null
    public val enforcedAdmin: EnforcedAdmin?
        get() = _enforcedAdmin

    private var _enforcingAdmin: EnforcingAdmin? = null
    public val enforcingAdmin: EnforcingAdmin?
        get() = _enforcingAdmin

    private var _restriction: String? = null
    public val restriction: String?
        get() = _restriction

    constructor() : super()

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)

    @Deprecated("Use setDisabledByAdmin(EnforcingAdmin, String) instead.")
    fun setDisabledByAdmin(admin: EnforcedAdmin?) {
        val disabled = (admin != null)
        _enforcedAdmin = admin
        if (_disabledByAdmin != disabled) {
            _disabledByAdmin = disabled
            Log.i(TAG, "item $id is disabledByAdmin=$disabledByAdmin")
        }
    }

    fun setDisabledByAdmin(admin: EnforcingAdmin?) {
        val disabled = (admin != null)
        _enforcingAdmin = admin
        _restriction = restriction
        if (_disabledByAdmin != disabled) {
            _disabledByAdmin = disabled
            Log.i(TAG, "item $id is disabledByAdmin=$disabledByAdmin")
        }
    }

    fun setRestriction(restriction: String) {
        _restriction = restriction
    }

    override fun onBindView(view: View) {
        super.onBindView(view)

        val optionText: RichTextView = view.findViewById(R.id.sud_items_title)
        if (disabledByAdmin) {
            val iconContainer: View = view.findViewById(R.id.sud_items_icon_container)
            iconContainer.setVisibility(View.VISIBLE)
            optionText.setAlpha(DISABLED_ITEM_OPACITY)
        } else {
            optionText.setAlpha(ENABLED_ITEM_OPACITY)
        }
        Log.d(
            TAG,
            "onBindView: item $id is disabledByAdmin=$disabledByAdmin, text alpha=${optionText.alpha}",
        )
    }

    override fun onCheckedChanged(buttonView: CompoundButton, isChecked: Boolean) {
        // do nothing, avoid the listener being triggered twice.
    }

    override fun getDefaultLayoutResource(): Int = R.layout.sud_items_radio_button_restricted

    private companion object {
        const val TAG = "RestrictedRadioButtonItem"
        const val DISABLED_ITEM_OPACITY = 0.5f
        const val ENABLED_ITEM_OPACITY = 1.0f
    }
}
