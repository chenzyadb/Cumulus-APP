package cumulus.battery.stats

import android.accessibilityservice.AccessibilityService
import android.annotation.SuppressLint
import android.graphics.Rect
import android.os.PowerManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityWindowInfo
import cumulus.battery.stats.objects.BatteryStatsProvider
import cumulus.battery.stats.objects.BatteryStatsRecorder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Timer
import java.util.TimerTask

@SuppressLint("AccessibilityPolicy")
class BackgroundService : AccessibilityService() {
    companion object {
        private var serviceCreated = false

        fun isServiceCreated(): Boolean {
            return serviceCreated
        }
    }

    private var timer: Timer? = null
    private var deviceScreenSize: Long = 0
    private var focusedAppPkgName: String = "other"

    override fun onCreate() {
        super.onCreate()
        serviceCreated = true
        deviceScreenSize = getDeviceScreenSize()
    }

    override fun onServiceConnected() {
        super.onServiceConnected()

        BatteryStatsProvider.init(applicationContext)
        BatteryStatsRecorder.init(applicationContext)

        CoroutineScope(Dispatchers.Default).launch {
            BatteryStatsRecorder.optimize()
        }

        startTimer()
    }

    override fun onInterrupt() { }

    override fun onDestroy() {
        serviceCreated = false
        stopTimer()
        super.onDestroy()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event != null) {
            focusedAppPkgName = event.packageName.toString()
        }
    }

    private fun addRecordItem() {
        var foregroundPkgName = "standby"
        if (isScreenOn()) {
            val topWindowPkgName = getTopWindowPkgName()
            if (topWindowPkgName != null) {
                foregroundPkgName = topWindowPkgName
            } else {
                foregroundPkgName = focusedAppPkgName
            }
        }

        val context = applicationContext
        val batteryPower = BatteryStatsProvider.getBatteryVoltage(context) *
                BatteryStatsProvider.getBatteryCurrent(context) / 1000
        BatteryStatsRecorder.addItem(
            foregroundPkgName,
            BatteryStatsProvider.getBatteryStatus(context),
            BatteryStatsProvider.getBatteryCapacity(context),
            batteryPower,
            BatteryStatsProvider.getBatteryTemperature(context)
        )
    }

    private fun startTimer() {
        if (timer == null) {
            timer = Timer()
            timer!!.schedule(object : TimerTask() {
                override fun run() {
                    addRecordItem()
                }
            }, 0, 2000)
        }
    }

    private fun stopTimer() {
        if (timer != null) {
            timer!!.cancel()
            timer = null
        }
    }

    private fun isScreenOn(): Boolean {
        val powerManager = getSystemService(POWER_SERVICE) as PowerManager
        return powerManager.isInteractive
    }

    private fun getTopWindowPkgName(): String? {
        var topWindow: AccessibilityWindowInfo? = null
        windows.forEach { window ->
            val windowRect = Rect()
            window.getBoundsInScreen(windowRect)
            val windowSize = windowRect.width().toLong() * windowRect.height()
            if (windowSize > (deviceScreenSize / 2)) {
                topWindow = window
            }
        }
        if (topWindow != null) {
            val pkgName = topWindow.root?.packageName?.toString()
            if (pkgName != null) {
                return pkgName
            }
        }
        return null
    }

    private fun getDeviceScreenSize(): Long {
        val metrics = resources.displayMetrics
        return metrics.widthPixels.toLong() * metrics.heightPixels
    }
}