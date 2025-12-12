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

import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.R
import com.google.android.setupcompat.template.FooterBarMixin
import com.google.android.setupdesign.GlifLayout
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SupervisionErrorActivityTest {

    @Test
    fun primaryButton_isConfiguredAndFinishesActivityOnClick() {
        ActivityScenario.launch(SupervisionErrorActivity::class.java).use { scenario ->
            scenario.moveToState(Lifecycle.State.RESUMED)

            scenario.onActivity { activity ->
                val layout =
                    activity.findViewById<GlifLayout>(R.id.supervision_generic_error_page_layout)
                val mixin = layout.getMixin(FooterBarMixin::class.java)

                val primaryButton = mixin.primaryButtonView
                assertThat(primaryButton).isNotNull()
                assertThat(primaryButton.text.toString())
                    .isEqualTo(
                        activity.getString(R.string.supervision_generic_error_page_button_label)
                    )

                primaryButton.performClick()
                assertThat(activity.isFinishing).isTrue()
            }
        }
    }
}
