package cumulus.battery.stats.objects

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.os.BatteryManager
import androidx.core.content.edit

object BatteryStatsProvider {
    private var sharedPreferences: SharedPreferences? = null

    fun init(context: Context) {
        if (sharedPreferences == null) {
            sharedPreferences = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
        }
    }

    @SuppressLint("PrivateApi")
    fun getBatteryDesignCapacity(context: Context): Int {
        try {
            val powerProfileClassName = "com.android.internal.os.PowerProfile"
            val mPowerProfile =
                Class.forName(powerProfileClassName).getConstructor(Context::class.java)
                    .newInstance(context)
            val capacity = Class.forName(powerProfileClassName).getMethod("getBatteryCapacity")
                .invoke(mPowerProfile)
            return (capacity as Double).toInt()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return 0
    }

    fun getBatteryTemperature(context: Context): Int {
        try {
            val intentFilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            intentFilter.priority = IntentFilter.SYSTEM_HIGH_PRIORITY
            val intent = context.registerReceiver(null, intentFilter)
            if (intent != null) {
                return (intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) / 10)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return 0
    }

    fun getBatteryVoltage(context: Context): Int {
        try {
            val intentFilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            intentFilter.priority = IntentFilter.SYSTEM_HIGH_PRIORITY
            val intent = context.registerReceiver(null, intentFilter)
            if (intent != null) {
                val batteryVolt = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0)
                if (batteryVolt < 1000) {
                    return batteryVolt * 1000
                }
                return batteryVolt
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return 0
    }

    fun getBatteryStatus(context: Context): Int {
        try {
            val intentFilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            intentFilter.priority = IntentFilter.SYSTEM_HIGH_PRIORITY
            val intent = context.registerReceiver(null, intentFilter)
            if (intent != null) {
                return intent.getIntExtra(
                    BatteryManager.EXTRA_STATUS,
                    BatteryManager.BATTERY_STATUS_UNKNOWN
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return BatteryManager.BATTERY_STATUS_UNKNOWN
    }

    fun getBatteryCapacity(context: Context): Int {
        try {
            val intentFilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            intentFilter.priority = IntentFilter.SYSTEM_HIGH_PRIORITY
            val intent = context.registerReceiver(null, intentFilter)
            if (intent != null) {
                return intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return 0
    }

    fun getBatteryCurrent(context: Context): Int {
        try {
            val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
            var batteryCurrent =
                batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW)
            if (isDualBattery()) {
                batteryCurrent *= 2
            }
            if (isCurrentUnitUA()) {
                batteryCurrent /= 1000
            }
            if (isCurrentReverse()) {
                batteryCurrent = -batteryCurrent
            }
            return batteryCurrent
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return 0
    }

    fun getBatteryHealth(context: Context): Int {
        try {
            val intentFilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            intentFilter.priority = IntentFilter.SYSTEM_HIGH_PRIORITY
            val intent = context.registerReceiver(null, intentFilter)
            val batteryHealth = intent?.getIntExtra(
                BatteryManager.EXTRA_HEALTH,
                BatteryManager.BATTERY_HEALTH_UNKNOWN
            )
            if (batteryHealth != null) {
                return batteryHealth
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return BatteryManager.BATTERY_HEALTH_UNKNOWN
    }

    fun getBatteryTechnology(context: Context): String {
        try {
            val intentFilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            intentFilter.priority = IntentFilter.SYSTEM_HIGH_PRIORITY
            val intent = context.registerReceiver(null, intentFilter)
            val batteryTechnology = intent?.getStringExtra(BatteryManager.EXTRA_TECHNOLOGY)
            if (batteryTechnology != null) {
                return batteryTechnology
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return "unknown"
    }

    @SuppressLint("InlinedApi")
    fun getBatteryCycleCount(context: Context): Int {
        try {
            val intentFilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            intentFilter.priority = IntentFilter.SYSTEM_HIGH_PRIORITY
            val intent = context.registerReceiver(null, intentFilter)
            if (intent != null) {
                return intent.getIntExtra(BatteryManager.EXTRA_CYCLE_COUNT, 0)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return 0
    }

    fun setDualBattery(dualBattery: Boolean) {
        sharedPreferences?.edit {
            putBoolean("dualBattery", dualBattery)
        }
    }

    fun setCurrentUnitUA(currentUnitUA: Boolean) {
        sharedPreferences?.edit {
            putBoolean("currentUnitUA", currentUnitUA)
        }
    }

    fun setCurrentReverse(currentReverse: Boolean) {
        sharedPreferences?.edit {
            putBoolean("currentReverse", currentReverse)
        }
    }

    fun setCurrentAdjusted(currentAdjusted: Boolean) {
        sharedPreferences?.edit {
            putBoolean("currentAdjusted", currentAdjusted)
        }
    }

    fun isDualBattery(): Boolean {
        if (sharedPreferences != null) {
            return sharedPreferences!!.getBoolean("dualBattery", false)
        }
        return false
    }

    fun isCurrentUnitUA(): Boolean {
        if (sharedPreferences != null) {
            return sharedPreferences!!.getBoolean("currentUnitUA", false)
        }
        return false
    }

    fun isCurrentReverse(): Boolean {
        if (sharedPreferences != null) {
            return sharedPreferences!!.getBoolean("currentReverse", false)
        }
        return false
    }

    fun isCurrentAdjusted(): Boolean {
        if (sharedPreferences != null) {
            return sharedPreferences!!.getBoolean("currentAdjusted", false)
        }
        return false
    }
}