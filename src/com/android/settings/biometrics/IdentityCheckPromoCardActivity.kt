/*
 * Copyright (C) 2025 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings.biometrics

import android.os.Bundle
import android.provider.Settings
import androidx.fragment.app.FragmentActivity

/** Activity that shows the Identity Check promo card using [IdentityCheckPromoCardFragment]. */
class IdentityCheckPromoCardActivity : FragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Settings.Secure.putInt(contentResolver, Settings.Secure.IDENTITY_CHECK_PROMO_CARD_SHOWN, 1)

        val bottomSheetFragment =
            supportFragmentManager.findFragmentByTag(BOTTOM_SHEET_FRAGMENT_TAG)
                as? IdentityCheckPromoCardFragment

        if (bottomSheetFragment == null) {
            showBottomSheetFragment()
        } else {
            bottomSheetFragment.setOnDismissListener { onDismiss() }
        }
    }

    private fun onDismiss() {
        if (!isChangingConfigurations) {
            finish()
        }
    }

    private fun showBottomSheetFragment() {
        val bottomSheetFragment = IdentityCheckPromoCardFragment()
        val bundle = Bundle()
        bundle.putString(IdentityCheckPromoCardFragment.KEY_INTENT_ACTION, intent.action)
        bottomSheetFragment.arguments = bundle
        bottomSheetFragment.setOnDismissListener { onDismiss() }
        bottomSheetFragment.show(supportFragmentManager, BOTTOM_SHEET_FRAGMENT_TAG)
    }

    companion object {
        private const val BOTTOM_SHEET_FRAGMENT_TAG =
            "identity_check_promo_card_bottom_sheet_fragment"
    }
}
