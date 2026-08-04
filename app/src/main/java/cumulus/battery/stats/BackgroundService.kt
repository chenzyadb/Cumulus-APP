package cumulus.battery.stats

import android.accessibilityservice.AccessibilityService
import android.annotation.SuppressLint
import android.app.KeyguardManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.display.DisplayManager
import android.os.BatteryManager
import android.os.PowerManager
import android.view.Display
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityWindowInfo
import cumulus.battery.stats.objects.BatteryStatsProvider
import cumulus.battery.stats.objects.BatteryStatsRecorder
import cumulus.battery.stats.utils.GetTimeStamp
import java.util.Timer
import java.util.TimerTask

@SuppressLint("AccessibilityPolicy")
class BackgroundService : AccessibilityService() {
    companion object {
        private const val RECORD_INTERVAL_SCREEN_ON_MS = 2000L
        private const val RECORD_INTERVAL_SCREEN_OFF_MS = 10000L
        private const val RECORD_INTERVAL_SCREEN_OFF_SECONDS = RECORD_INTERVAL_SCREEN_OFF_MS / 1000L
        private const val SCREEN_STATE_SETTLE_SECONDS = 10L
        private const val USAGE_EVENTS_LOOKBACK_MS = 5 * 60 * 1000L

        @Volatile
        private var serviceCreated = false

        fun isServiceCreated(): Boolean {
            return serviceCreated
        }
    }

    private var timer: Timer? = null
    @Volatile
    private var timerIdle = false
    @Volatile
    private var lastScreenOn: Boolean? = null
    @Volatile
    private var lastScreenStateChangedTimestamp = 0L
    @Volatile
    private var lastScreenOffRecordTimestamp = 0L
    @Volatile
    private var lastForegroundPkgName: String? = null

    override fun onCreate() {
        super.onCreate()
        serviceCreated = true
    }

    override fun onServiceConnected() {
        super.onServiceConnected()

        BatteryStatsProvider.init(applicationContext)
        BatteryStatsRecorder.init(applicationContext)
        BatteryStatsRecorder.optimize()

        updateScreenState()
        restartTimer()
    }

    override fun onInterrupt() { }

    override fun onDestroy() {
        serviceCreated = false
        stopTimer()
        super.onDestroy()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) {
            return
        }

        updateForegroundPackageFromEvent(event)
        if (timerIdle) {
            updateScreenState()
            timerIdle = !isScreenOn() && !isCharging()
        }
    }

    private fun addRecordItem() {
        val now = GetTimeStamp()
        val screenOnBefore = updateScreenState()
        val charging = isCharging()
        timerIdle = !screenOnBefore && !charging

        if (shouldSkipUnstableScreenOffSample(screenOnBefore)) {
            return
        }
        if (timerIdle && now - lastScreenOffRecordTimestamp < RECORD_INTERVAL_SCREEN_OFF_SECONDS) {
            return
        }

        val keyguardLockedBefore = isKeyguardLocked()
        val foregroundPkgName = if (screenOnBefore && !keyguardLockedBefore) {
            getForegroundAppPackageName()
        } else {
            "other"
        }

        val context = applicationContext
        val batteryIntent = getBatteryChangedIntent(context)
        var batteryVoltage = 0
        var batteryStatus = BatteryManager.BATTERY_STATUS_UNKNOWN
        var batteryPercentage = 0
        var batteryTemperature = 0
        if (batteryIntent != null) {
            batteryVoltage = batteryIntent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0)
            if (batteryVoltage in 1..999) {
                batteryVoltage *= 1000
            }
            batteryStatus = batteryIntent.getIntExtra(
                BatteryManager.EXTRA_STATUS,
                BatteryManager.BATTERY_STATUS_UNKNOWN
            )
            batteryPercentage = batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0)
            batteryTemperature =
                batteryIntent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) / 10
        }
        val batteryPower = batteryVoltage *
                BatteryStatsProvider.getBatteryCurrent(context) / 1000
        val screenOnAfter = updateScreenState()
        if (screenOnAfter != screenOnBefore) {
            timerIdle = !screenOnAfter && !isCharging()
            return
        }
        if (shouldSkipUnstableScreenOffSample(screenOnAfter)) {
            return
        }
        timerIdle = !screenOnAfter && !isCharging()
        if (timerIdle) {
            lastScreenOffRecordTimestamp = GetTimeStamp()
        }

        val pkgName = when {
            !screenOnAfter -> "standby"
            !keyguardLockedBefore -> foregroundPkgName
            else -> "other"
        }
        BatteryStatsRecorder.addItem(
            pkgName,
            batteryStatus,
            batteryPercentage,
            batteryPower,
            batteryTemperature
        )
    }

    private fun getBatteryChangedIntent(context: Context): Intent? {
        return try {
            context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun getForegroundAppPackageName(): String {
        return getForegroundAppPackageNameFromWindows()
            ?: getForegroundAppPackageNameFromActiveRoot()
            ?: getForegroundAppPackageNameFromUsageEvents()
            ?: lastForegroundPkgName
            ?: "other"
    }

    private fun getForegroundAppPackageNameFromWindows(): String? {
        var bestPkgName: String? = null
        var bestLayer = Int.MIN_VALUE
        for (window in windows) {
            if (window.type != AccessibilityWindowInfo.TYPE_APPLICATION) {
                continue
            }

            val pkgName = window.root?.packageName?.toString() ?: continue
            if (!isIgnoredPackage(pkgName) && window.layer > bestLayer) {
                bestLayer = window.layer
                bestPkgName = pkgName
            }
        }
        return bestPkgName?.also { rememberForegroundPackage(it) }
    }

    private fun getForegroundAppPackageNameFromActiveRoot(): String? {
        val pkgName = rootInActiveWindow?.packageName?.toString() ?: return null
        if (isIgnoredPackage(pkgName)) {
            return null
        }
        return pkgName.also { rememberForegroundPackage(it) }
    }

    @Suppress("deprecation")
    private fun getForegroundAppPackageNameFromUsageEvents(): String? {
        val usageStatsManager = getSystemService(USAGE_STATS_SERVICE) as UsageStatsManager
        val endTime = System.currentTimeMillis()
        val usageEvents = try {
            usageStatsManager.queryEvents(endTime - USAGE_EVENTS_LOOKBACK_MS, endTime)
        } catch (_: SecurityException) {
            return null
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }

        val event = UsageEvents.Event()
        var foregroundPkgName: String? = null
        while (usageEvents.hasNextEvent()) {
            usageEvents.getNextEvent(event)
            val pkgName = event.packageName ?: continue
            if (isIgnoredPackage(pkgName)) {
                continue
            }

            when (event.eventType) {
                UsageEvents.Event.MOVE_TO_FOREGROUND,
                UsageEvents.Event.ACTIVITY_RESUMED -> foregroundPkgName = pkgName
                UsageEvents.Event.MOVE_TO_BACKGROUND,
                UsageEvents.Event.ACTIVITY_PAUSED,
                UsageEvents.Event.ACTIVITY_STOPPED -> {
                    if (foregroundPkgName == pkgName) {
                        foregroundPkgName = null
                    }
                }
            }
        }
        return foregroundPkgName?.also { rememberForegroundPackage(it) }
    }

    private fun updateForegroundPackageFromEvent(event: AccessibilityEvent) {
        when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED,
            AccessibilityEvent.TYPE_WINDOWS_CHANGED,
            AccessibilityEvent.TYPE_VIEW_FOCUSED -> Unit
            else -> return
        }

        val pkgName = event.packageName?.toString() ?: return
        if (!isIgnoredPackage(pkgName)) {
            rememberForegroundPackage(pkgName)
        }
    }

    private fun rememberForegroundPackage(pkgName: String) {
        lastForegroundPkgName = pkgName
    }

    private val ignoredPackageRegexes = listOf(
        Regex("""(?i)systemui"""),
        Regex("""(?i)inputmethod"""),
        Regex("""(?i)input_method"""),
        Regex("""(?i)\binput\b.*method"""),
        Regex("""[Ii]nput[Mm]ethod"""),
        Regex("""(?i)sogou.*input|baidu.*input|iflytek.*input"""),
        Regex("""(?i)tencent.*mobileqq.*:tool"""),
    )

    private fun isIgnoredPackage(pkgName: String): Boolean {
        return ignoredPackageRegexes.any { it.containsMatchIn(pkgName) }
    }

    private fun restartTimer() {
        stopTimer()

        timerIdle = !updateScreenState() && !isCharging()
        val newTimer = Timer("BatteryStatsTimer", true)
        newTimer.schedule(object : TimerTask() {
            override fun run() {
                addRecordItem()
            }
        }, 0L, RECORD_INTERVAL_SCREEN_ON_MS)
        timer = newTimer
    }

    private fun stopTimer() {
        timer?.cancel()
        timer = null
    }

    private fun isScreenOn(): Boolean {
        val powerManager = getSystemService(POWER_SERVICE) as PowerManager
        if (powerManager.isInteractive) {
            return true
        }

        val displayManager = getSystemService(DISPLAY_SERVICE) as DisplayManager
        val defaultDisplay = displayManager.getDisplay(Display.DEFAULT_DISPLAY)
        return when (defaultDisplay?.state) {
            Display.STATE_ON,
            Display.STATE_DOZE,
            Display.STATE_DOZE_SUSPEND -> true
            else -> false
        }
    }

    private fun updateScreenState(screenOn: Boolean = isScreenOn()): Boolean {
        val now = GetTimeStamp()
        val previousScreenOn = lastScreenOn
        if (previousScreenOn == null || previousScreenOn != screenOn) {
            lastScreenOn = screenOn
            lastScreenStateChangedTimestamp = now
        }
        return screenOn
    }

    private fun shouldSkipUnstableScreenOffSample(screenOn: Boolean): Boolean {
        return !screenOn &&
                GetTimeStamp() - lastScreenStateChangedTimestamp < SCREEN_STATE_SETTLE_SECONDS
    }

    private fun isCharging(): Boolean {
        val status = BatteryStatsProvider.getBatteryStatus(applicationContext)
        return status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL
    }

    private fun isKeyguardLocked(): Boolean {
        val keyguardManager = getSystemService(KEYGUARD_SERVICE) as KeyguardManager
        return keyguardManager.isKeyguardLocked
    }
}
