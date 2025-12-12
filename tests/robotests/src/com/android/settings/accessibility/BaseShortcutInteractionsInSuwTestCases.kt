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

package com.android.settings.accessibility

import android.os.Bundle
import android.view.View
import androidx.fragment.app.testing.FragmentScenario
import androidx.fragment.app.testing.FragmentScenario.FragmentAction
import androidx.lifecycle.Lifecycle
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.android.settings.R
import com.android.settings.accessibility.data.AccessibilityRepositoryProvider
import com.android.settings.testutils.AccessibilityTestUtils
import com.google.android.setupcompat.template.FooterBarMixin
import com.google.android.setupdesign.GlifPreferenceLayout
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Test

/** Base test cases for fragment shown in Setup flow that provides accessibility shortcut toggle. */
abstract class BaseShortcutInteractionsInSuwTestCases<T : PreferenceFragmentCompat> :
    BaseShortcutInteractionsTestCases<T>() {

    protected var fragmentScenario: FragmentScenario<T>? = null
    protected var fragment: T? = null

    abstract fun getSetupWizardDescription(): String

    abstract fun getFragmentClazz(): Class<T>

    abstract fun getFragmentArgs(): Bundle?

    @After
    open fun cleanUp() {
        fragmentScenario?.close()
        AccessibilityRepositoryProvider.resetInstanceForTesting()
    }

    @Test
    fun showFragment_verifyUI() {
        val fragment = launchFragment()

        val view: View? = fragment.getView()
        assertThat(view).isInstanceOf(GlifPreferenceLayout::class.java)
        val glifPreferenceLayout = view as GlifPreferenceLayout?

        assertThat(glifPreferenceLayout!!.getHeaderText().toString())
            .isEqualTo(getSetupWizardTitle())
        assertThat(glifPreferenceLayout.getDescriptionText().toString())
            .isEqualTo(getSetupWizardDescription())
        assertThat(glifPreferenceLayout.getDividerInsetStart()).isEqualTo(Int.Companion.MAX_VALUE)
        assertThat(glifPreferenceLayout.getDividerInsetEnd()).isEqualTo(0)
        val footerBarMixin = glifPreferenceLayout.getMixin(FooterBarMixin::class.java)
        assertThat(footerBarMixin.getPrimaryButton().getText().toString())
            .isEqualTo(context.getString(R.string.done))
        assertThat(fragment.findPreference<Preference?>(TOP_INTRO_PREF_KEY)!!.isVisible()).isFalse()
    }

    override fun launchFragment(): T {
        val bundle = getFragmentArgs() ?: Bundle()
        bundle.putString(AccessibilitySettings.EXTRA_TITLE, getSetupWizardTitle())

        fragmentScenario =
            AccessibilityTestUtils.launchFragmentInSetupWizardFlow<T>(getFragmentClazz(), bundle)

        fragmentScenario?.run {
            moveToState(Lifecycle.State.RESUMED)
            onFragment(
                FragmentAction { fragment: T? ->
                    this@BaseShortcutInteractionsInSuwTestCases.fragment = fragment
                }
            )
        }

        return fragment!!
    }

    open fun getSetupWizardTitle(): String {
        return TEST_TITLE
    }

    companion object {
        private const val TEST_TITLE: String = "test_title"
        private const val TOP_INTRO_PREF_KEY: String = "top_intro"
    }
}
