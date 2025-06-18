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

package com.android.settings.wifi.utils

import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.EditText
import com.google.android.material.textfield.TextInputLayout

/** A widget that wraps the relationship work between a TextInputLayout and an EditText. */
open class TextInputGroup(
    val view: View,
    private val layoutId: Int,
    private val editTextId: Int,
    private val errorMessageId: Int,
) {

    val layout: TextInputLayout
        get() = view.requireViewById(layoutId)

    val editText: EditText
        get() = view.requireViewById(editTextId)

    val errorMessage: String
        get() = view.context.getString(errorMessageId)

    private val textWatcher =
        object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                layout.isErrorEnabled = false
            }
        }

    init {
        addTextChangedListener(textWatcher)
    }

    fun addTextChangedListener(watcher: TextWatcher) {
        editText.addTextChangedListener(watcher)
    }

    var label: String
        get() = layout.hint?.toString() ?: ""
        set(value) {
            layout.setHint(value)
        }

    var text: String
        get() = editText.text?.toString() ?: ""
        set(value) {
            editText.setText(value)
        }

    var helperText: String
        get() = layout.helperText?.toString() ?: ""
        set(value) {
            layout.setHelperText(value)
            if (value.isEmpty()) layout.isHelperTextEnabled = false
        }

    var error: String
        get() = layout.error?.toString() ?: ""
        set(value) {
            layout.setError(value)
            if (value.isEmpty()) layout.isErrorEnabled = false
        }

    open fun validate(): Boolean {
        if (!editText.isShown) return true

        val isValid = text.isNotEmpty()
        if (!isValid) {
            Log.w(TAG, "validate failed in ${layout.hint ?: "unknown"}")
            error = errorMessage
        }
        return isValid
    }

    companion object {
        const val TAG = "TextInputGroup"
    }
}
