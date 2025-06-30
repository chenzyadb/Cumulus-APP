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
        var batteryDesignCapacity = 0
        try {
            val powerProfileClassName = "com.android.internal.os.PowerProfile"
            val mPowerProfile =
                Class.forName(powerProfileClassName).getConstructor(Context::class.java)
                    .newInstance(context)
            val capacity = Class.forName(powerProfileClassName).getMethod("getBatteryCapacity")
                .invoke(mPowerProfile)
            batteryDesignCapacity = (capacity as Double).toInt()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return batteryDesignCapacity
    }

    fun getBatteryTemperature(context: Context): Int {
        val intentFilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        intentFilter.priority = IntentFilter.SYSTEM_HIGH_PRIORITY
        val intent = context.registerReceiver(null, intentFilter)
        if (intent != null) {
            return (intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) / 10)
        }
        return 0
    }

    fun getBatteryVoltage(context: Context): Int {
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
        return 0
    }

    fun getBatteryStatus(context: Context): Int {
        val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        return batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_STATUS)
    }

    fun getBatteryCapacity(context: Context): Int {
        val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        return batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
    }

    fun getBatteryCurrent(context: Context): Int {
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
}