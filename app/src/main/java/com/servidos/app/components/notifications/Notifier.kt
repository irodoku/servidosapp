package com.servidos.app.components.notifications

import android.content.Context
import com.servidos.app.db.AccountEntity
import com.servidos.app.entity.Notification

/**
 * Shows notifications.
 */
interface Notifier {
    fun show(notification: Notification, account: AccountEntity, isFirstInBatch: Boolean)
}

class SystemNotifier(
    private val context: Context
) : Notifier {
    override fun show(notification: Notification, account: AccountEntity, isFirstInBatch: Boolean) {
        NotificationHelper.make(context, notification, account, isFirstInBatch)
    }
}
