package com.m3sv.selectcontentdirectory

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.m3sv.plainupnp.interfaces.LifecycleManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class ForegroundNotificationService : LifecycleService() {

    @Inject
    lateinit var lifecycleManager: LifecycleManager

    override fun onCreate() {
        super.onCreate()
        lifecycleScope.launchWhenCreated {
            lifecycleManager.doOnClose {
                stopForeground(true)
                stopSelf()
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        when (intent?.action) {
            NotificationBuilder.ACTION_EXIT -> finishApplication()
            START_SERVICE -> startForeground(
                NotificationBuilder.SERVER_NOTIFICATION,
                NotificationBuilder(this).buildNotification()
            )
        }

        return START_STICKY
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        finishApplication()
        super.onTaskRemoved(rootIntent)
    }

    private fun finishApplication() {
        lifecycleManager.finish()
    }

    companion object {
        fun start(context: Context) {
            val intent = Intent(context, ForegroundNotificationService::class.java).apply {
                action = START_SERVICE
            }

            ContextCompat.startForegroundService(context, intent)
        }

        private const val START_SERVICE = "START_UPNP_SERVICE"
    }
}
