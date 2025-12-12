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

package com.android.settings.spa.app.specialaccess

import android.Manifest
import android.app.admin.DevicePolicyManager
import android.app.admin.DevicePolicyResourcesManager
import android.content.Context
import android.content.pm.ApplicationInfo
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settingslib.spaprivileged.model.app.AppOpsPermissionController
import com.android.settingslib.spaprivileged.model.app.IPackageManagers
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when` as whenever
import org.mockito.MockitoAnnotations
import org.mockito.Spy
import org.mockito.kotlin.any
import org.mockito.kotlin.doNothing
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.spy

@RunWith(AndroidJUnit4::class)
class UsageDataAppListTest {
    @Spy private val context: Context = ApplicationProvider.getApplicationContext()

    @Mock private lateinit var packageManagers: IPackageManagers

    @Mock private lateinit var dpm: DevicePolicyManager

    @Mock private lateinit var resourcesManager: DevicePolicyResourcesManager

    private lateinit var listModel: UsageDataAppListModel

    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)
        val realListModel = UsageDataAppListModel(context, packageManagers, dpm)
        listModel = spy(realListModel)
        doNothing().`when`(listModel).showDisableUsageAccessWarningDialog()
        whenever(dpm.resources).thenReturn(resourcesManager)
        whenever(resourcesManager.getString(any(), any())).thenReturn("some string")
    }

    /** Tests that the transformItem method correctly wraps the given ApplicationInfo. */
    @Test
    fun transformItem_recordHasCorrectApp() {
        val record = listModel.transformItem(APP)

        assertThat(record.app).isSameInstanceAs(APP)
    }

    /**
     * Tests that an app is correctly marked as not changeable if it has not requested the
     * PACKAGE_USAGE_STATS permission.
     */
    @Test
    fun transformItem_whenNotRequestPermission_isNotChangeable() {
        with(packageManagers) {
            whenever(APP.hasRequestPermission(Manifest.permission.PACKAGE_USAGE_STATS))
                .thenReturn(false)
        }

        val record = listModel.transformItem(APP)

        assertThat(record.isChangeable).isFalse()
    }

    /**
     * Tests that an app is correctly marked as changeable if it has requested the
     * PACKAGE_USAGE_STATS permission.
     */
    @Test
    fun transformItem_whenRequestPermission_isChangeable() {
        with(packageManagers) {
            whenever(APP.hasRequestPermission(Manifest.permission.PACKAGE_USAGE_STATS))
                .thenReturn(true)
        }

        val record = listModel.transformItem(APP)

        assertThat(record.isChangeable).isTrue()
    }

    /**
     * Tests that the warning dialog is shown when disabling usage access for a profile owner app.
     */
    @Test
    fun setAllowed_whenSetToFalseAndIsProfileOwner_dialogShown() {
        whenever(dpm.isProfileOwnerApp(PACKAGE_NAME)).thenReturn(true)
        val controller = mock<AppOpsPermissionController>()
        val record = UsageDataAppRecord(app = APP, isChangeable = true, controller = controller)

        listModel.setAllowed(record, false)

        verify(listModel).showDisableUsageAccessWarningDialog()
        verify(controller).setAllowed(false)
    }

    /** Tests that the warning dialog is not shown when enabling usage access for any app. */
    @Test
    fun setAllowed_whenSetToTrue_dialogNotShown() {
        val controller = mock<AppOpsPermissionController>()
        val record = UsageDataAppRecord(app = APP, isChangeable = true, controller = controller)

        listModel.setAllowed(record, true)

        verify(listModel, never()).showDisableUsageAccessWarningDialog()
        verify(controller).setAllowed(true)
    }

    /**
     * Tests that the warning dialog is not shown when disabling usage access for a
     * non-profile-owner app.
     */
    @Test
    fun setAllowed_whenSetToFalseAndIsNotProfileOwner_dialogNotShown() {
        whenever(dpm.isProfileOwnerApp(PACKAGE_NAME)).thenReturn(false)
        val controller = mock<AppOpsPermissionController>()
        val record = UsageDataAppRecord(app = APP, isChangeable = true, controller = controller)

        listModel.setAllowed(record, false)

        verify(listModel, never()).showDisableUsageAccessWarningDialog()
        verify(controller).setAllowed(false)
    }

    /** Verifies that the underlying permission controller's setAllowed method is called. */
    @Test
    fun setAllowed_controllerSetAllowedCalled() {
        val controller = mock<AppOpsPermissionController>()
        val record = UsageDataAppRecord(app = APP, isChangeable = true, controller = controller)

        listModel.setAllowed(record, true)

        verify(controller).setAllowed(true)
    }

    /**
     * Tests that the transform method correctly builds a list of records, marking them as
     * changeable based on whether they have requested the permission.
     */
    @Test
    fun transform_appListIsTransformed() = runTest {
        val appList = listOf(APP, APP_NO_PERMISSION)
        whenever(packageManagers.getAppOpPermissionPackages(USER_ID, PERMISSION))
            .thenReturn(setOf(PACKAGE_NAME))

        val recordListFlow = listModel.transform(flowOf(USER_ID), flowOf(appList))

        val recordList = recordListFlow.first()
        assertThat(recordList).hasSize(2)
        assertThat(recordList[0].app).isSameInstanceAs(APP)
        assertThat(recordList[0].isChangeable).isTrue()
        assertThat(recordList[1].app).isSameInstanceAs(APP_NO_PERMISSION)
        assertThat(recordList[1].isChangeable).isFalse()
    }

    /** Tests that the filter method correctly removes records that are not changeable. */
    @Test
    fun filter_unchangeableRecordsAreFiltered() = runTest {
        val recordList =
            listOf(
                UsageDataAppRecord(app = APP, isChangeable = true, controller = mock()),
                UsageDataAppRecord(
                    app = APP_NO_PERMISSION,
                    isChangeable = false,
                    controller = mock(),
                ),
            )

        val filteredListFlow = listModel.filter(flowOf(USER_ID), flowOf(recordList))

        val filteredList = filteredListFlow.first()
        assertThat(filteredList).hasSize(1)
        assertThat(filteredList[0].app).isSameInstanceAs(APP)
    }

    private companion object {
        const val USER_ID = 0
        const val PACKAGE_NAME = "package.name"
        const val PACKAGE_NAME_NO_PERMISSION = "package.name.no_permission"
        const val PERMISSION = Manifest.permission.PACKAGE_USAGE_STATS

        val APP = ApplicationInfo().apply { packageName = PACKAGE_NAME }
        val APP_NO_PERMISSION = ApplicationInfo().apply { packageName = PACKAGE_NAME_NO_PERMISSION }
    }
}
