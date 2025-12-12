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

import android.app.settings.SettingsEnums
import android.app.settings.SettingsEnums.ACTION_SUPERVISION_TRY_AGAIN
import android.os.Bundle
import com.android.settings.R
import com.android.settings.core.InstrumentedActivity
import com.android.settings.overlay.FeatureFactory
import com.google.android.setupcompat.template.FooterBarMixin
import com.google.android.setupcompat.template.FooterButton
import com.google.android.setupdesign.GlifLayout
import com.google.android.setupdesign.util.ThemeHelper

class SupervisionErrorActivity : InstrumentedActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val metricsFeatureProvider = FeatureFactory.featureFactory.metricsFeatureProvider

        ThemeHelper.trySetSuwTheme(this)

        setContentView(R.layout.supervision_generic_error_screen)

        val layout = findViewById<GlifLayout?>(R.id.supervision_generic_error_page_layout)

        val mixin = layout?.getMixin(FooterBarMixin::class.java)
        mixin?.setPrimaryButton(
            FooterButton.Builder(this)
                .setText(R.string.supervision_generic_error_page_button_label)
                .setListener {
                    metricsFeatureProvider.action(this, ACTION_SUPERVISION_TRY_AGAIN)
                    finish()
                }
                .setButtonType(FooterButton.ButtonType.DONE)
                .build()
        )
    }

    override fun getMetricsCategory(): Int {
        return SettingsEnums.DIALOG_SUPERVISION_GENERIC_ERROR
    }
}
