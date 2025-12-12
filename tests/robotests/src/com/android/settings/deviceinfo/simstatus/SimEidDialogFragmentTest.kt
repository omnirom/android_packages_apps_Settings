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

package com.android.settings.deviceinfo.simstatus

import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import com.android.settings.testutils.shadow.ShadowAlertDialogCompat
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnit
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLooper.shadowMainLooper
import org.robolectric.shadows.androidx.fragment.FragmentController

@RunWith(RobolectricTestRunner::class)
@Config(shadows = [ShadowAlertDialogCompat::class])
class SimEidDialogFragmentTest {
    @get:Rule
    val mockitoRule = MockitoJUnit.rule()

    private lateinit var mParent: Fragment

    @Before
    fun setUp() {
        ShadowAlertDialogCompat.reset()
        mParent = Fragment()
        FragmentController.setupFragment(
            mParent, FragmentActivity::class.java, /* containerViewId= */ 0, /* bundle= */ null)
    }

    @Test
    fun show_titleIsCorrect() {
        SimEidDialogFragment.show(mParent.childFragmentManager, TITLE, EID)
        shadowMainLooper().idle()
        val dialog = ShadowAlertDialogCompat.getLatestAlertDialog()
        assertNotNull(dialog)
        val view = dialog!!.findViewById<TextView>(com.android.settings.R.id.esim_id_value)

        assertNotNull(view)
        assertThat(view!!.getText().toString()).isEqualTo(EID)
    }

    private companion object {
        const val TITLE = "EID"
        const val EID = "123456"
    }
}