package com.taskflow

import android.app.Application
import com.taskflow.core.notifications.ReminderScheduler

class TaskFlowApp : Application() {
    override fun onCreate() {
        super.onCreate()
        ReminderScheduler.ensureChannel(this)
    }
}
