package cumulus.battery.stats

import android.accessibilityservice.AccessibilityService
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.PowerManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityWindowInfo
import androidx.core.app.NotificationCompat
import cumulus.battery.stats.objects.BatteryStatsProvider
import cumulus.battery.stats.objects.BatteryStatsRecorder
import java.util.Timer
import java.util.TimerTask

class BackgroundService : AccessibilityService() {
    companion object {
        private var serviceCreated = false
        fun isServiceCreated(): Boolean {
            return serviceCreated
        }
    }

    private val notificationId: Int = 0x0d000721
    private var timer: Timer? = null
    private var foregroundPkgName: String = "other"
    private var batteryCapacity: Int = 0
    private var batteryCurrent: Int = 0
    private var batteryPower: Int = 0
    private var batteryTemperature: Int = 0
    private var batteryStatus = BatteryManager.BATTERY_STATUS_UNKNOWN

    override fun onCreate() {
        super.onCreate()
        serviceCreated = true
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        BatteryStatsProvider.init(applicationContext)
        BatteryStatsRecorder.init(applicationContext)
        updateForegroundPkgName()
        updateBatteryStats()
        updateNotification()
        addRecordItem()
        startTimer()
        registerBroadcastReceiver()
    }

    override fun onInterrupt() {
        BatteryStatsRecorder.optimize()
    }

    override fun onDestroy() {
        serviceCreated = false
        stopTimer()
        BatteryStatsRecorder.optimize()
        unregisterBroadcastReceiver()
        cancelNotification()
        super.onDestroy()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        try {
            val pkgName = event?.packageName?.toString()
            if (pkgName != null && !pkgName.contains("systemui") && pkgName != foregroundPkgName) {
                foregroundPkgName = pkgName
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        updateBatteryStats()
        updateNotification()
        addRecordItem()
    }

    private fun updateForegroundPkgName() {
        if (!isScreenOn()) {
            foregroundPkgName = "standby"
            return
        }
        try {
            var focusedWindow: AccessibilityWindowInfo? = null
            for (window in windows) {
                if (window != null && window.isFocused) {
                    focusedWindow = window
                    break
                }
            }
            if (focusedWindow != null) {
                val pkgName = focusedWindow.root?.packageName?.toString()
                if (pkgName != null && !pkgName.contains("systemui")) {
                    foregroundPkgName = pkgName
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun updateBatteryStats() {
        batteryCapacity = BatteryStatsProvider.getBatteryCapacity(applicationContext)
        batteryCurrent = BatteryStatsProvider.getBatteryCurrent(applicationContext)
        batteryPower =
            BatteryStatsProvider.getBatteryVoltage(applicationContext) * batteryCurrent / 1000
        batteryTemperature = BatteryStatsProvider.getBatteryTemperature(applicationContext)
        batteryStatus = BatteryStatsProvider.getBatteryStatus(applicationContext)
    }

    private fun addRecordItem() {
        BatteryStatsRecorder.addItem(
            foregroundPkgName,
            batteryStatus,
            batteryCapacity,
            batteryPower,
            batteryTemperature
        )
    }

    @SuppressLint("InlinedApi")
    private fun updateNotification() {
        if (foregroundPkgName == "standby") {
            return
        }

        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        val pendingIntentFlags = PendingIntent.FLAG_UPDATE_CURRENT + PendingIntent.FLAG_MUTABLE
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, pendingIntentFlags)
        val notificationTitle = "Background Service"
        val notificationText =
            "电池状态: ${batteryPower} mW (${batteryCurrent} mA)  ${batteryTemperature} °C"
        val notification =
            NotificationCompat.Builder(this, "Cumulus")
                .setSmallIcon(R.mipmap.noti_icon)
                .setTicker("Cumulus")
                .setContentTitle(notificationTitle)
                .setContentText(notificationText)
                .setDefaults(Notification.FLAG_NO_CLEAR)
                .setOnlyAlertOnce(true)
                .setOngoing(true)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .setVisibility(NotificationCompat.VISIBILITY_SECRET)
                .setContentIntent(pendingIntent)
                .build()
        val notificationManager = getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            "Cumulus",
            "BackgroundService",
            NotificationManager.IMPORTANCE_DEFAULT
        )
        notificationManager.createNotificationChannel(channel)
        notificationManager.notify(notificationId, notification)
    }

    private fun cancelNotification() {
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.cancel(notificationId)
    }

    private fun startTimer() {
        if (timer != null) {
            return
        }
        timer = Timer()
        timer!!.schedule(object : TimerTask() {
            override fun run() {
                updateForegroundPkgName()
                updateBatteryStats()
                updateNotification()
                addRecordItem()
            }
        }, 0, 5000)
    }

    private fun stopTimer() {
        if (timer == null) {
            return
        }
        timer!!.cancel()
        timer = null
    }

    private val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                Intent.ACTION_SCREEN_OFF -> {
                    foregroundPkgName = "standby"
                    updateBatteryStats()
                    addRecordItem()
                }

                Intent.ACTION_SCREEN_ON -> {
                    updateForegroundPkgName()
                    updateBatteryStats()
                    addRecordItem()
                }
            }
        }
    }

    private fun registerBroadcastReceiver() {
        val broadcastFilter = IntentFilter()
        broadcastFilter.addAction(Intent.ACTION_SCREEN_ON)
        broadcastFilter.addAction(Intent.ACTION_SCREEN_OFF)
        broadcastFilter.priority = IntentFilter.SYSTEM_HIGH_PRIORITY
        registerReceiver(broadcastReceiver, broadcastFilter)
    }

    private fun unregisterBroadcastReceiver() {
        unregisterReceiver(broadcastReceiver)
    }

    private fun isScreenOn(): Boolean {
        val powerManager = getSystemService(POWER_SERVICE) as PowerManager
        return powerManager.isInteractive
    }
}