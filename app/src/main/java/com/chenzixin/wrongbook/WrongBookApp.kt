package com.chenzixin.wrongbook

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Application 入口。
 *
 * 只负责 Hilt 装配。「巩固/每日提醒」已随功能下线一起移除，
 * 需要提醒类能力时再单独加，避免留下没人用的通知渠道。
 */
@HiltAndroidApp
class WrongBookApp : Application()
