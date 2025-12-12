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

package com.android.settings.gestures

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.preference.PreferenceViewHolder
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.R
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
class ButtonNavigationSettingsOrderRadioButtonTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    private val radioButton =
        ButtonNavigationSettingsOrderRadioButton(
            context,
            listOf(
                R.drawable.ic_sysbar_back,
                R.drawable.ic_sysbar_home,
                R.drawable.ic_sysbar_recents,
            ),
            listOf(
                R.string.navbar_back_button,
                R.string.navbar_home_button,
                R.string.navbar_recent_button,
            ),
        ) // Labels need to be valid string resources. Icons could technically be whatever

    private val inflater: LayoutInflater = LayoutInflater.from(context)
    private val view: View =
        inflater.inflate(radioButton.layoutResource, LinearLayout(context), false)
    private val viewHolder = PreferenceViewHolder.createInstanceForTests(view)
    private val startIcon = view.findViewById<ImageView>(R.id.icon_start)
    private val centerIcon = view.findViewById<ImageView>(R.id.icon_center)
    private val endIcon = view.findViewById<ImageView>(R.id.icon_end)
    private val label = view.findViewById<TextView>(R.id.order_label)

    @Test
    fun onBindViewHolder_ltr_setsIconContentDescriptions() {
        radioButton.onBindViewHolder(viewHolder)

        assertThat(startIcon.contentDescription)
            .isEqualTo(context.getString(R.string.navbar_back_button))
        assertThat(centerIcon.contentDescription)
            .isEqualTo(context.getString(R.string.navbar_home_button))
        assertThat(endIcon.contentDescription)
            .isEqualTo(context.getString(R.string.navbar_recent_button))
    }

    @Test
    @Config(qualifiers = "+ar-rXB")
    fun onBindViewHolder_rtl_setsIconContentDescriptions() {
        radioButton.onBindViewHolder(viewHolder)

        assertThat(startIcon.contentDescription)
            .isEqualTo(context.getString(R.string.navbar_recent_button))
        assertThat(centerIcon.contentDescription)
            .isEqualTo(context.getString(R.string.navbar_home_button))
        assertThat(endIcon.contentDescription)
            .isEqualTo(context.getString(R.string.navbar_back_button))
    }

    @Test
    fun onBindViewHolder_setsLabelText() {
        radioButton.onBindViewHolder(viewHolder)

        assertThat(label.text)
            .isEqualTo(
                ButtonNavigationSettingsOrderRadioButton.getCompoundLabel(
                    radioButton.labels,
                    context,
                )
            )
    }
}
