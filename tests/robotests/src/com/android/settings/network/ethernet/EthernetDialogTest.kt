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

import android.content.Context
import android.content.DialogInterface
import android.net.IpConfiguration
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.network.ethernet.EthernetDialog.EthernetDialogListener
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock

@RunWith(AndroidJUnit4::class)
class EthernetDialogTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private var submitCount = 0

    private val listener =
        object : EthernetDialogListener {
            override fun onSubmit(dialog: EthernetDialog) {
                submitCount++
            }
        }

    @Test
    fun clickPositiveButton() {
        val ethernetDialog = EthernetDialogImpl(context, listener, IpConfiguration(), "eth0")

        ethernetDialog.onClick(mock<DialogInterface>(), DialogInterface.BUTTON_POSITIVE)

        assertEquals(submitCount, 1)
    }

    @Test
    fun clickNegativeButton() {
        val ethernetDialog = EthernetDialogImpl(context, listener, IpConfiguration(), "eth0")

        ethernetDialog.onClick(mock<DialogInterface>(), DialogInterface.BUTTON_NEGATIVE)

        assertEquals(submitCount, 0)
    }
}
