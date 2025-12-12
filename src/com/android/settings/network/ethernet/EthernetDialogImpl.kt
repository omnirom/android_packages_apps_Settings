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

package com.android.settings.network.ethernet

import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.net.IpConfiguration
import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.annotation.StyleRes
import com.android.settings.R
import com.android.settings.network.ethernet.EthernetDialog.EthernetDialogListener

class EthernetDialogImpl(
    context: Context,
    private val listener: EthernetDialogListener,
    private val ipConfiguration: IpConfiguration,
    private val preferenceId: String,
    @StyleRes style: Int = 0,
) : AlertDialog(context, style), DialogInterface.OnClickListener, EthernetDialog {

    private lateinit var view: View
    private lateinit var controller: EthernetDialogController

    override fun onClick(dialogInterface: DialogInterface, id: Int) {
        when (id) {
            BUTTON_SUBMIT -> {
                listener.onSubmit(this)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        view = layoutInflater.inflate(R.layout.ethernet_dialog, null)
        setView(view)
        setSubmitButton(context.getString(R.string.ethernet_config_submit))
        setCancelButton(context.getString(R.string.ethernet_config_cancel))
        setTitle(context.getString(R.string.ethernet_config_title))
        controller =
            EthernetDialogControllerImpl(context, ipConfiguration, view, this, preferenceId)
        super.onCreate(savedInstanceState)
    }

    override fun getController(): EthernetDialogController {
        return controller
    }

    override fun getSubmitButton(): Button? = getButton(BUTTON_SUBMIT)

    override fun getCancelButton(): Button? = getButton(BUTTON_NEGATIVE)

    private fun setSubmitButton(text: CharSequence) {
        setButton(BUTTON_SUBMIT, text, this)
    }

    private fun setCancelButton(text: CharSequence) {
        setButton(BUTTON_NEGATIVE, text, this)
    }

    companion object {
        private const val BUTTON_SUBMIT = BUTTON_POSITIVE
    }
}
