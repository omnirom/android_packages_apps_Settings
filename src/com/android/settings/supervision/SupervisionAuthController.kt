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

import android.annotation.MainThread
import android.app.ActivityManager
import android.app.ActivityTaskManager
import android.app.TaskStackListener
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import androidx.annotation.VisibleForTesting

/**
 * Manages a supervision authentication session.
 *
 * After the supervising profile is authenticated, the user should not need to authenticate again,
 * unless they leave the Settings app or lock their device or the session times out. This session
 * should persist across interactions with native settings and those injected from the separate
 * supervision APK.
 *
 * Activities responsible for authentication should start a session only after the user has been
 * successfully authenticated, and the session will be automatically invalidated when the task that
 * started the session stops running or goes into the background, or the device is being locked.
 *
 * The session also has a fixed timeout of 10 minutes, after which re-authentication is required.
 */
class SupervisionAuthController private constructor(private val appContext: Context) {
    private val handler = Handler(Looper.getMainLooper())
    private val activityManager = appContext.getSystemService(ActivityManager::class.java)
    private var currentTaskId: Int? = null
    private var sessionStartTime: Long? = null

    // Receiver to invalidate session when the screen is turned off.
    val mScreenOffReceiver: BroadcastReceiver =
        object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                handler.post {
                    if (currentTaskId != null) {
                        invalidateSession()
                    }
                }
            }
        }

    @VisibleForTesting
    val mTaskStackListener: TaskStackListener =
        object : TaskStackListener() {
            override fun onTaskStackChanged() {
                handler.post {
                    if (currentTaskId != null && !isSupervisionActivityFocused()) {
                        invalidateSession()
                    }
                }
            }
        }

    init {
        ActivityTaskManager.getInstance().registerTaskStackListener(mTaskStackListener)
    }

    /**
     * Starts an auth session, indicating that a parent has been authenticated on the current task.
     */
    @MainThread
    fun startSession(taskId: Int) {
        currentTaskId = taskId
        sessionStartTime = SystemClock.elapsedRealtime()
        val offFilter = IntentFilter(Intent.ACTION_SCREEN_OFF)
        offFilter.addAction(Intent.ACTION_USER_PRESENT)
        appContext.registerReceiver(mScreenOffReceiver, offFilter)
    }

    /** Returns whether an auth session is currently active on this task. */
    @MainThread
    fun isSessionActive(taskId: Int): Boolean {
        // Checks for timeout
        val currentTime = SystemClock.elapsedRealtime()
        val isTimedOut =
            sessionStartTime != null && (currentTime - sessionStartTime!!) > SESSION_TIMEOUT_MILLIS
        if (isTimedOut) {
            invalidateSession()
            return false
        }
        return currentTaskId == taskId
    }

    /**
     * Invalidates the current session. This should only be done if a task with an active session
     * goes into the background.
     */
    private fun invalidateSession() {
        currentTaskId = null
        sessionStartTime = null
        try {
            appContext.unregisterReceiver(mScreenOffReceiver)
        } catch (e: IllegalArgumentException) {
            // Ignore exception if receiver is not registered.
        }
    }

    /**
     * Whether the task with a currently active auth session is focused and running a supervision
     * activity.
     */
    private fun isSupervisionActivityFocused(): Boolean {
        if (currentTaskId == null) return false
        val appTasks = activityManager.appTasks ?: emptyList()
        val task = appTasks.find { it.taskInfo.taskId == currentTaskId }
        if (task == null) return false
        return task.taskInfo.isRunning &&
            task.taskInfo.isFocused &&
            isSupervisionActivity(task.taskInfo.topActivity)
    }

    private fun isSupervisionActivity(component: ComponentName?): Boolean {
        if (component == null) return false
        return component.packageName == appContext.systemSupervisionPackageName ||
            component.className == SupervisionDashboardActivity::class.java.name
    }

    companion object {
        @Volatile @VisibleForTesting var sInstance: SupervisionAuthController? = null

        @VisibleForTesting const val SESSION_TIMEOUT_MILLIS = 10 * 60 * 1000L // 10 minutes

        fun getInstance(context: Context): SupervisionAuthController {
            return sInstance
                ?: synchronized(this) {
                    sInstance
                        ?: SupervisionAuthController(context.applicationContext).also {
                            sInstance = it
                        }
                }
        }
    }
}
