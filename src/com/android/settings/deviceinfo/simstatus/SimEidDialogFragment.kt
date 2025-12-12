/*
 * Copyright (C) 2017 The Android Open Source Project
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
package com.android.settings.deviceinfo.simstatus

import android.R
import android.app.Dialog
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.FragmentManager
import com.android.settings.core.instrumentation.InstrumentedDialogFragment
import com.android.settings.deviceinfo.PhoneNumberUtil
import com.android.settingslib.qrcode.QrCodeGenerator

class SimEidDialogFragment : InstrumentedDialogFragment() {
    private var mRootView: View? = null

    override fun getMetricsCategory(): Int {
        return 0
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val bundle = requireArguments()
        val dialogTitle = bundle.getString(DIALOG_TITLE_BUNDLE_KEY)
        val eid = bundle.getString(SIM_EID_BUNDLE_KEY)!!
        val builder = AlertDialog.Builder(requireActivity())
            .setTitle(dialogTitle)
            .setPositiveButton(R.string.ok, null /* onClickListener */)
        mRootView = LayoutInflater.from(builder.context)
            .inflate(com.android.settings.R.layout.dialog_eid_status, null /* parent */)

        val dialog = builder.setView(mRootView).create()
        dialog.window!!.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )
        dialog.setCanceledOnTouchOutside(false)
        val textView = mRootView!!.requireViewById<TextView>(com.android.settings.R.id.esim_id_value)
        textView.text = PhoneNumberUtil.expandByTts(eid)

        val qrCodeView = mRootView!!.requireViewById<ImageView>(com.android.settings.R.id.esim_id_qrcode)
        qrCodeView.setImageBitmap(getEidQrCode(eid))

        return dialog
    }

    companion object {
        private const val SIM_EID_BUNDLE_KEY = "arg_key_sim_eid"
        private const val DIALOG_TITLE_BUNDLE_KEY = "arg_key_dialog_title"

        private const val TAG = "SimEidDialogFragment"
        private const val QR_CODE_SIZE = 600

        fun show(manager: FragmentManager, title: String, eid: String) {
            if (manager.findFragmentByTag(TAG) == null) {
                val bundle = Bundle()
                bundle.putString(SIM_EID_BUNDLE_KEY, eid)
                bundle.putString(DIALOG_TITLE_BUNDLE_KEY, title)
                val dialog = SimEidDialogFragment()
                dialog.setArguments(bundle)
                dialog.show(manager, TAG)
            }
        }

        /**
         * Gets the QR code for EID
         * @param eid is the EID string
         * @return a Bitmap of QR code
         */
        private fun getEidQrCode(eid: String): Bitmap? {
            return try {
                QrCodeGenerator.encodeQrCode(contents = eid, size = QR_CODE_SIZE)
            } catch (exception: Exception) {
                Log.w(TAG, "Error when creating QR code width $QR_CODE_SIZE", exception)
                null
            }
        }
    }
}
