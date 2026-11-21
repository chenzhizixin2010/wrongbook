package com.chenzixin.wrongbook

import android.app.Application
import com.chenzixin.wrongbook.data.work.ReminderScheduler
import com.chenzixin.wrongbook.data.work.ReviewReminderWorker
import dagger.hilt.android.HiltAndroidApp

/**
 * Application 入口。
 *
 * 只做两件事：Hilt 装配、注册每日复习提醒。
 * 提醒是「尽力而为」的：没给通知权限时 Worker 会自己跳过，不影响 App 使用。
 */
@HiltAndroidApp
class WrongBookApp : Application() {

    override fun onCreate() {
        super.onCreate()
        ReviewReminderWorker.ensureChannel(this)
        ReminderScheduler.scheduleDaily(this)
    }
}
