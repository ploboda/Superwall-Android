package com.superwall.sdk.store.transactions.notifications

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.superwall.sdk.paywall.vc.SuperwallPaywallActivity

class NotificationWorker(
    val context: Context,
    workerParams: WorkerParameters
) : Worker(context, workerParams) {

    override fun doWork(): Result {
        val notificationId = inputData.getInt("id", 0)
        val title = inputData.getString("title")
        val text = inputData.getString("body")

        val builder = NotificationCompat.Builder(applicationContext, SuperwallPaywallActivity.NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(context.applicationInfo.icon)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_HIGH)

        with(NotificationManagerCompat.from(applicationContext)) {
            // Ignore error here, we check permissions before getting here.
            notify(notificationId, builder.build())
        }

        return Result.success()
    }
}