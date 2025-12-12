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

package com.android.settings

import android.app.AlertDialog
import android.content.Context
import android.os.UserManager
import android.view.View
import androidx.test.core.app.ApplicationProvider
import com.android.settings.TrustedCredentialsDialogBuilder.DelegateInterface
import com.android.settings.TrustedCredentialsFragment.CertHolder
import com.android.settings.testutils.shadow.ShadowDevicePolicyManager
import com.android.settings.testutils.shadow.ShadowLockPatternUtils
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLooper.shadowMainLooper

@RunWith(RobolectricTestRunner::class)
@Config(shadows = [ShadowLockPatternUtils::class, ShadowDevicePolicyManager::class])
class TrustedCredentialsDialogBuilderTest {
    private val PROFILE_ID = 0

    private lateinit var userManager: UserManager
    private lateinit var context: Context
    private lateinit var builder: TrustedCredentialsDialogBuilder

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()

        userManager =
            RuntimeEnvironment.getApplication().getSystemService(Context.USER_SERVICE)
                as UserManager
        // Simulate a profile with no lock screen security.
        ShadowLockPatternUtils.setIsSecure(PROFILE_ID, false)

        // Build and start the Settings.TrustedCredentialsSettingsActivity using Robolectric.
        // This simulates the activity lifecycle.
        val controller =
            Robolectric.buildActivity(Settings.TrustedCredentialsSettingsActivity::class.java)
                .create()
                .start()
                .resume()
        val activity: Settings.TrustedCredentialsSettingsActivity = controller.get()

        val certHolder = mock<CertHolder> { on { isSystemCert } doReturn false }
        val delegate = mock<DelegateInterface>()

        builder = TrustedCredentialsDialogBuilder(activity, delegate)
        builder.setCertHolder(certHolder)
    }

    /**
     * Tests the visibility of buttons in the TrustedCredentialsDialogBuilder. This test verifies
     * that the user has the option to uninstall user-provided custom CA certificates.
     */
    @Test
    fun testRemoveCertsButtonVisible() {
        val dialog = builder.create()
        // Show the dialog. This will trigger its layout and button initialization.
        dialog.show()
        // Advance the Robolectric main looper to process any pending UI events,
        // such as layout passes or button visibility updates.
        shadowMainLooper().idle()

        // Assert that the button that allows users to uninstall CA certificates is visible.
        assertThat(dialog.getButton(AlertDialog.BUTTON_NEGATIVE).visibility).isEqualTo(View.VISIBLE)
    }

    /**
     * This test verifies that the user does not have the option to uninstall user-provided custom
     * CA certificates if prohibited by the restriction DISALLOW_CONFIG_CREDENTIALS.
     */
    @Test
    fun testRemoveCertsButtonHidden() {
        // Set the user restriction that disallows credential configuration.
        userManager.setUserRestriction(UserManager.DISALLOW_CONFIG_CREDENTIALS, true)
        val dialog = builder.create()
        dialog.show()

        // Advance the Robolectric main looper to process any pending UI events,
        // such as layout passes or button visibility updates.
        shadowMainLooper().idle()

        // Assert that the button that allows users to uninstall CA certificates is not visible.
        assertThat(dialog.getButton(AlertDialog.BUTTON_NEGATIVE).visibility).isEqualTo(View.GONE)
    }
}
