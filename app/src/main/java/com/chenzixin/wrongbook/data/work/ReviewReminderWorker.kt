package com.chenzixin.wrongbook.data.work

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.chenzixin.wrongbook.MainActivity
import com.chenzixin.wrongbook.R

/**
 * 每日复习提醒。
 *
 * 刻意不在 Worker 里读数据库：WorkManager 的 worker 拿 Hilt 注入需要 hilt-work，
 * 而「提醒该复习了」本身不需要具体数量，少一层依赖更稳。
 * 需要显示「今天有 N 道」时，再引入 HiltWorkerFactory。
 */
class ReviewReminderWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val context = applicationContext
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return Result.success() // 没授权就静默跳过，不打扰用户
        }

        val intent = PendingIntent.getActivity(
            context,
            0,
            android.content.Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher)
            .setContentTitle("该复习错题了")
            .setContentText("花 5 分钟过一遍今天的错题，记得更牢。")
            .setContentIntent(intent)
            .setAutoCancel(true)
            .build()

        val manager = context.getSystemService(NotificationManager::class.java)
        manager?.notify(NOTIFICATION_ID, notification)
        return Result.success()
    }

    companion object {
        const val CHANNEL_ID = "review_reminder"
        const val NOTIFICATION_ID = 1001

        fun ensureChannel(context: Context) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
            val manager = context.getSystemService(NotificationManager::class.java) ?: return
            if (manager.getNotificationChannel(CHANNEL_ID) != null) return
            val channel = NotificationChannel(
                CHANNEL_ID,
                "复习提醒",
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply { description = "每天提醒复习错题" }
            manager.createNotificationChannel(channel)
        }
    }
}
