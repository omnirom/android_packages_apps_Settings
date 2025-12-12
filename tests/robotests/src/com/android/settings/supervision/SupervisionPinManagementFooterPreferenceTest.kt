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
package com.android.settings.supervision

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import androidx.preference.PreferenceViewHolder
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.R
import com.android.settingslib.preference.createAndBindWidget
import com.android.settingslib.widget.FooterPreference
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SupervisionPinManagementFooterPreferenceTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val footerPreference = SupervisionPinManagementFooterPreference()

    @Test
    fun getTitle() {
        assertThat(footerPreference.title)
            .isEqualTo(R.string.device_supervision_pin_management_footer)
    }

    @Test
    fun learnMoreTextExists() {
        footerPreference.createAndBindWidget<FooterPreference>(context).also {
            val holder =
                PreferenceViewHolder.createInstanceForTests(
                    LayoutInflater.from(context)
                        .inflate(R.layout.preference_footer, /* root= */ null)
                )
            it.onBindViewHolder(holder)
            val learnMoreView = holder.itemView.findViewById<TextView?>(R.id.settingslib_learn_more)
            assertThat(learnMoreView).isNotNull()
            assertThat(learnMoreView?.visibility).isEqualTo(View.VISIBLE)
            assertThat(learnMoreView?.text.toString())
                .isEqualTo(context.getString(R.string.settingslib_learn_more_text))
        }
    }
}
