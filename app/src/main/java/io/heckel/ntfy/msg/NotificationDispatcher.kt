package io.heckel.ntfy.msg

import android.content.Context
import io.heckel.ntfy.data.Notification
import io.heckel.ntfy.data.Repository
import io.heckel.ntfy.data.Subscription
import io.heckel.ntfy.up.Distributor

class NotificationDispatcher(val context: Context, val repository: Repository) {
    private val notifier = NotificationService(context)
    private val broadcaster = BroadcastService(context)
    private val distributor = Distributor(context)

    fun init() {
        notifier.createNotificationChannels()
    }

    fun dispatch(subscription: Subscription, notification: Notification) {
        val muted = checkMuted(subscription)
        val notify = checkNotify(subscription, notification, muted)
        val broadcast = subscription.upAppId == ""
        val distribute = subscription.upAppId != ""
        if (notify) {
            notifier.send(subscription, notification)
        }
        if (broadcast) {
            broadcaster.send(subscription, notification, muted)
        }
        if (distribute) {
            distributor.sendMessage(subscription.upAppId, subscription.upConnectorToken, notification.message)
        }
    }

    private fun checkNotify(subscription: Subscription, notification: Notification, muted: Boolean): Boolean {
        if (subscription.upAppId != "") {
            return false
        }
        val detailsVisible = repository.detailViewSubscriptionId.get() == notification.subscriptionId
        return !detailsVisible && !muted
    }

    private fun checkMuted(subscription: Subscription): Boolean {
        if (repository.isGlobalMuted()) {
            return true
        }
        return subscription.mutedUntil == 1L || (subscription.mutedUntil > 1L && subscription.mutedUntil > System.currentTimeMillis()/1000)
    }

    companion object {
        private const val TAG = "NtfyNotificationDispatcher"
    }
}