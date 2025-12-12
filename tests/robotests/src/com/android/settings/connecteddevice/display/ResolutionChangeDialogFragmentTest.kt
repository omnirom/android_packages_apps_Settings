/*
 * Copyright 2025 The Android Open Source Project
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

package com.android.settings.connecteddevice.display

import android.content.DialogInterface
import android.os.Bundle
import android.os.Looper
import android.view.Display.Mode
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.testutils.shadow.ShadowAlertDialogCompat
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.TimeUnit
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.androidx.fragment.FragmentController

/** Unit tests for {@link ResolutionChangeDialogFragment}. */
@RunWith(AndroidJUnit4::class)
@Config(shadows = [ShadowAlertDialogCompat::class])
class ResolutionChangeDialogFragmentTest {

    private lateinit var newMode: Mode
    private lateinit var existingMode: Mode
    private lateinit var dialogFragment: ResolutionChangeDialogFragment
    private lateinit var dialog: AlertDialog
    private var receivedBundle: Bundle? = null

    @Before
    fun setUp() {
        newMode = Mode(1, 1920, 1080, 60f)
        existingMode = Mode(2, 1280, 720, 60f)

        ShadowAlertDialogCompat.reset()
        val parentFragment = Fragment()
        FragmentController.setupFragment(
            parentFragment,
            FragmentActivity::class.java,
            /* containerViewId= */ 0,
            /* bundle= */ null,
        )

        dialogFragment = ResolutionChangeDialogFragment.newInstance(newMode, existingMode)
        dialogFragment.show(parentFragment.parentFragmentManager, "dialog_tag")
        shadowOf(Looper.getMainLooper()).idle()
        dialog = dialogFragment.dialog as AlertDialog

        parentFragment.parentFragmentManager.setFragmentResultListener(
            ResolutionChangeDialogFragment.KEY_RESULT,
            parentFragment,
        ) { _, bundle ->
            receivedBundle = bundle
        }
    }

    @After
    fun tearDown() {
        ShadowAlertDialogCompat.reset()
    }

    @Test
    fun positiveButtonClick_sendsConfirmedResultWithNewMode() {
        dialog.getButton(DialogInterface.BUTTON_POSITIVE).performClick()
        shadowOf(Looper.getMainLooper()).idle()

        assertThat(dialog.isShowing).isFalse()
        assertModeChanged()
    }

    @Test
    fun negativeButtonClick_sendsCancelledResultWithExistingMode() {
        dialog.getButton(DialogInterface.BUTTON_NEGATIVE).performClick()
        shadowOf(Looper.getMainLooper()).idle()

        assertThat(dialog.isShowing).isFalse()
        assertModeNotChanged()
    }

    @Test
    fun timerFinish_dismissesDialogAndSendsCancelledResult() {
        shadowOf(Looper.getMainLooper()).idleFor(20, TimeUnit.SECONDS)

        assertThat(dialog.isShowing).isFalse()
        assertModeNotChanged()
    }

    @Test
    fun onStop_dismissesDialogAndSendsCancelledResult() {
        dialogFragment.onStop()
        shadowOf(Looper.getMainLooper()).idle()

        assertThat(dialog.isShowing).isFalse()
        assertModeNotChanged()
    }

    private fun assertModeChanged() {
        assertThat(receivedBundle).isNotNull()
        val bundle = receivedBundle!!
        assertThat(getIsConfirmed(bundle)).isTrue()
        assertThat(getNewMode(bundle)).isEqualTo(newMode)
    }

    private fun assertModeNotChanged() {
        assertThat(receivedBundle).isNotNull()
        val bundle = receivedBundle!!
        assertThat(getIsConfirmed(bundle)).isFalse()
        assertThat(getExistingMode(bundle)).isEqualTo(existingMode)
    }

    private fun getIsConfirmed(bundle: Bundle): Boolean {
        return bundle.getBoolean(ResolutionChangeDialogFragment.KEY_CONFIRMED)
    }

    private fun getNewMode(bundle: Bundle): Mode {
        return bundle.getParcelable(ResolutionChangeDialogFragment.KEY_NEW_MODE, Mode::class.java)!!
    }

    private fun getExistingMode(bundle: Bundle): Mode {
        return bundle.getParcelable(
            ResolutionChangeDialogFragment.KEY_EXISTING_MODE,
            Mode::class.java,
        )!!
    }
}
