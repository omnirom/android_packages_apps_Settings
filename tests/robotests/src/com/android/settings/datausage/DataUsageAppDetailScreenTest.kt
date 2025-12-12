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
package com.android.settings.datausage

import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.os.Bundle
import androidx.core.net.toUri
import com.android.settings.flags.Flags
import com.android.settings.testutils2.SettingsCatalystTestCase
import com.android.settingslib.metadata.PreferenceMetadata
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.robolectric.Shadows.shadowOf

class DataUsageAppDetailScreenTest : SettingsCatalystTestCase() {
    init {
        val shadowPackageManager = shadowOf(appContext.packageManager)
        val packageInfo =
            PackageInfo().apply {
                packageName = TEST_PACKAGE_NAME
                applicationInfo =
                    ApplicationInfo().apply {
                        packageName = TEST_PACKAGE_NAME
                        nonLocalizedLabel = TEST_APP_LABEL
                    }
            }
        shadowPackageManager.installPackage(packageInfo)
    }

    private val preferenceScreenCreatorWithInvalidPackage =
        DataUsageAppDetailScreen(
            appContext,
            Bundle().apply { putString("app", INVALID_PACKAGE_NAME) },
        )

    override val preferenceScreenCreator =
        DataUsageAppDetailScreen(
            appContext,
            Bundle().apply { putString("app", TEST_PACKAGE_NAME) },
        )

    override val flagName: String
        get() = Flags.FLAG_DEEPLINK_APPS_25Q4

    // TODO(b/427787504): enable when AppDataUsage fragment doesn't crash in a Robolectric test.
    override fun migration() {}

    @Test
    fun key() {
        assertThat(preferenceScreenCreator.key).isEqualTo(DataUsageAppDetailScreen.KEY)
    }

    @Test
    fun getTitle_whenPackageIsValid_returnsAppLabel() {
        // Act: Get the title from the screen creator for a valid package.
        val title = preferenceScreenCreator.getTitle(appContext)

        // Assert: The title should match the label set in the test setup.
        assertThat(title).isEqualTo(TEST_APP_LABEL)
    }

    @Test
    fun getTitle_whenPackageIsInvalid_returnsNull() {
        // Act: Get the title from the screen creator for an invalid package.
        val title = preferenceScreenCreatorWithInvalidPackage.getTitle(appContext)

        // Assert: The title should be null as the app info cannot be found.
        assertThat(title).isNull()
    }

    @Test
    fun getLaunchIntent_correctActivity() {
        val underTest = preferenceScreenCreator.getLaunchIntent(appContext, null)

        assertThat(underTest.getComponent()?.getClassName())
            .isEqualTo(AppDataUsageActivity::class.java.getName())
        assertThat(underTest.data).isEqualTo("package:$TEST_PACKAGE_NAME".toUri())
    }

    @Test
    fun getLaunchIntent_correctActivityWithExtras() {
        val underTest =
            preferenceScreenCreator.getLaunchIntent(appContext, TestMetadata("testBindingKey"))

        assertThat(underTest.getBundleExtra("settingslib:binding_screen_args")?.getString("app"))
            .isEqualTo(TEST_PACKAGE_NAME)
        assertThat(underTest.getStringExtra(":settings:fragment_args_key"))
            .isEqualTo("testBindingKey")
    }

    @Test
    fun getLaunchIntent_whenPackageIsInvalid_dataIsCorrect() {
        // Act: Get the launch intent with an invalid package name.
        val underTest = preferenceScreenCreatorWithInvalidPackage.getLaunchIntent(appContext, null)

        // Assert: The intent component and data URI should still be correctly formed.
        assertThat(underTest.getComponent()?.getClassName())
            .isEqualTo(AppDataUsageActivity::class.java.getName())
        assertThat(underTest.data).isEqualTo("package:$INVALID_PACKAGE_NAME".toUri())
    }

    @Test
    fun isAvailable_whenPackageIsValid_isTrue() {
        assertThat(preferenceScreenCreator.isAvailable(appContext)).isTrue()
    }

    @Test
    fun isAvailable_whenPackageIsInvalid_isFalse() {
        assertThat(preferenceScreenCreatorWithInvalidPackage.isAvailable(appContext)).isFalse()
    }

    companion object {
        private const val TEST_PACKAGE_NAME = "com.test.package"
        private const val TEST_APP_LABEL = "Test App"
        private const val INVALID_PACKAGE_NAME = "com.invalid.package"
    }
}

private data class TestMetadata(
    override val bindingKey: String,
    override val key: String = "testKey",
) : PreferenceMetadata
