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

import android.app.ActivityManager
import android.app.role.RoleManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import java.time.Duration
import kotlin.test.Test
import org.junit.Before
import org.junit.runner.RunWith
import org.mockito.Mockito.`when` as whenever
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify
import org.robolectric.shadows.ShadowLooper
import org.robolectric.shadows.ShadowSystemClock

@RunWith(AndroidJUnit4::class)
class SupervisionAuthControllerTest {

    private val mockActivityManager = mock<ActivityManager>()
    private val mockRoleManager = mock<RoleManager>()
    private var mockContext = mock<Context>()

    @Before
    fun setUp() {
        SupervisionAuthController.sInstance = null
        whenever(mockContext.getSystemService(ActivityManager::class.java))
            .thenReturn(mockActivityManager)
        whenever(mockContext.getSystemService(RoleManager::class.java)).thenReturn(mockRoleManager)
        whenever(mockContext.applicationContext).thenReturn(mockContext)
        mockRoleManager.stub {
            on { getRoleHolders(RoleManager.ROLE_SYSTEM_SUPERVISION) } doReturn
                listOf(SUPERVISION_PACKAGE_NAME)
        }
        ShadowLooper.idleMainLooper()
    }

    @Test
    fun initially_sessionIsNotActive() {
        val mockTask =
            mock<ActivityManager.AppTask>().stub {
                on { taskInfo } doReturn FOCUSED_SUPERVISION_DASHBOARD_TASK_INFO
            }
        mockActivityManager.stub { on { appTasks } doReturn listOf(mockTask) }

        val authController = SupervisionAuthController.getInstance(mockContext)
        assertThat(authController.isSessionActive(TASK_ID)).isFalse()
    }

    @Test
    fun startSession_sessionIsActive() {
        val mockTask =
            mock<ActivityManager.AppTask>().stub {
                on { taskInfo } doReturn FOCUSED_SUPERVISION_DASHBOARD_TASK_INFO
            }
        mockActivityManager.stub { on { appTasks } doReturn listOf(mockTask) }

        val authController = SupervisionAuthController.getInstance(mockContext)
        authController.startSession(TASK_ID)
        ShadowLooper.idleMainLooper()
        assertThat(authController.isSessionActive(TASK_ID)).isTrue()
    }

    @Test
    fun taskLosesFocus_sessionInvalidated() {
        val mockTask =
            mock<ActivityManager.AppTask>().stub {
                on { taskInfo } doReturn FOCUSED_SUPERVISION_DASHBOARD_TASK_INFO
            }
        mockActivityManager.stub { on { appTasks } doReturn listOf(mockTask) }

        val authController = SupervisionAuthController.getInstance(mockContext)
        authController.startSession(TASK_ID)
        authController.mTaskStackListener.onTaskStackChanged()
        ShadowLooper.idleMainLooper()
        assertThat(authController.isSessionActive(TASK_ID)).isTrue()

        mockTask.stub { on { taskInfo } doReturn NOT_FOCUSED_SUPERVISION_DASHBOARD_TASK_INFO }
        authController.mTaskStackListener.onTaskStackChanged()
        ShadowLooper.idleMainLooper()
        assertThat(authController.isSessionActive(TASK_ID)).isFalse()
    }

    @Test
    fun taskScreenLocked_sessionInvalidated() {
        val authController = SupervisionAuthController.getInstance(mockContext)
        authController.startSession(TASK_ID)
        ShadowLooper.idleMainLooper()
        assertThat(authController.isSessionActive(TASK_ID)).isTrue()

        val broadcastReceiverCaptor = argumentCaptor<BroadcastReceiver>()
        val intentFilterCaptor = argumentCaptor<IntentFilter>()
        verify(mockContext)
            .registerReceiver(broadcastReceiverCaptor.capture(), intentFilterCaptor.capture())

        val screenOffReceiver: BroadcastReceiver = broadcastReceiverCaptor.firstValue
        screenOffReceiver.onReceive(mockContext, Intent(Intent.ACTION_SCREEN_OFF))
        ShadowLooper.idleMainLooper()
        assertThat(authController.isSessionActive(TASK_ID)).isFalse()
        verify(mockContext).unregisterReceiver(screenOffReceiver)
    }

    @Test
    fun supervisionActivityLosesFocus_sessionInvalidated() {
        val mockTask =
            mock<ActivityManager.AppTask>().stub {
                on { taskInfo } doReturn FOCUSED_SUPERVISION_TASK_INFO
            }
        mockActivityManager.stub { on { appTasks } doReturn listOf(mockTask) }

        val authController = SupervisionAuthController.getInstance(mockContext)
        authController.startSession(TASK_ID)
        authController.mTaskStackListener.onTaskStackChanged()
        ShadowLooper.idleMainLooper()
        assertThat(authController.isSessionActive(TASK_ID)).isTrue()

        mockTask.stub { on { taskInfo } doReturn FOCUSED_OTHER_SETTINGS_TASK_INFO }
        authController.mTaskStackListener.onTaskStackChanged()
        ShadowLooper.idleMainLooper()
        assertThat(authController.isSessionActive(TASK_ID)).isFalse()
    }

    @Test
    fun supervisionSessionTimesOut_sessionInvalidated() {
        val authController = SupervisionAuthController.getInstance(mockContext)
        authController.startSession(TASK_ID)
        ShadowLooper.idleMainLooper()
        assertThat(authController.isSessionActive(TASK_ID)).isTrue()

        val timeoutMillis = SupervisionAuthController.SESSION_TIMEOUT_MILLIS + 1
        ShadowSystemClock.advanceBy(Duration.ofMillis(timeoutMillis))
        ShadowLooper.idleMainLooper()

        assertThat(authController.isSessionActive(TASK_ID)).isFalse()
    }

    @Test
    fun supervisionDashboardActivityLosesFocus_sessionInvalidated() {
        val mockTask =
            mock<ActivityManager.AppTask>().stub {
                on { taskInfo } doReturn FOCUSED_SUPERVISION_DASHBOARD_TASK_INFO
            }
        mockActivityManager.stub { on { appTasks } doReturn listOf(mockTask) }

        val authController = SupervisionAuthController.getInstance(mockContext)
        authController.startSession(TASK_ID)
        authController.mTaskStackListener.onTaskStackChanged()
        ShadowLooper.idleMainLooper()
        assertThat(authController.isSessionActive(TASK_ID)).isTrue()

        mockTask.stub { on { taskInfo } doReturn FOCUSED_OTHER_SETTINGS_TASK_INFO }
        authController.mTaskStackListener.onTaskStackChanged()
        ShadowLooper.idleMainLooper()
        assertThat(authController.isSessionActive(TASK_ID)).isFalse()
    }

    private companion object {
        const val TASK_ID = 100
        val SUPERVISION_PACKAGE_NAME = "com.android.supervision"
        val FOCUSED_SUPERVISION_TASK_INFO =
            ActivityManager.RecentTaskInfo().apply {
                taskId = TASK_ID
                isRunning = true
                isFocused = true
                topActivity = ComponentName(SUPERVISION_PACKAGE_NAME, "SomeSupervisionActivity")
            }
        val FOCUSED_OTHER_SETTINGS_TASK_INFO =
            ActivityManager.RecentTaskInfo().apply {
                taskId = TASK_ID
                isRunning = true
                isFocused = true
                topActivity = ComponentName("com.android.settings", "OtherActivity")
            }
        val FOCUSED_SUPERVISION_DASHBOARD_TASK_INFO =
            ActivityManager.RecentTaskInfo().apply {
                taskId = TASK_ID
                isRunning = true
                isFocused = true
                topActivity =
                    ComponentName(
                        "com.android.settings",
                        SupervisionDashboardActivity::class.java.name,
                    )
            }
        val NOT_FOCUSED_SUPERVISION_DASHBOARD_TASK_INFO =
            ActivityManager.RecentTaskInfo().apply {
                taskId = TASK_ID
                isRunning = true
                isFocused = false
                topActivity =
                    ComponentName(
                        "com.android.settings",
                        SupervisionDashboardActivity::class.java.name,
                    )
            }
    }
}
