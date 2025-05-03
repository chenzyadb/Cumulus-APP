package cumulus.battery.stats.objects

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import com.alibaba.fastjson2.JSON
import com.alibaba.fastjson2.JSONObject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File

object BatteryStatsProvider {
    private val mutex = Mutex()
    private var adjustFile: File? = null

    fun init(context: Context) {
        CoroutineScope(Dispatchers.Default).launch {
            mutex.withLock {
                if (adjustFile != null) {
                    return@withLock
                }
                val filesPath = context.filesDir.absolutePath
                adjustFile = File("${filesPath}/battery_current_adjust.json")
                if (!adjustFile!!.exists()) {
                    adjustFile!!.createNewFile()
                    val adjustJSON = JSONObject()
                    adjustJSON["dualBattery"] = false
                    adjustJSON["currentUnitUA"] = true
                    adjustJSON["currentReverse"] = false
                    adjustFile!!.writeText(adjustJSON.toJSONString(), Charsets.UTF_8)
                }
            }
        }
    }

    @SuppressLint("PrivateApi")
    fun getBatteryDesignCapacity(context: Context): Int {
        var batteryDesignCapacity = 0
        try {
            val powerProfileClassName = "com.android.internal.os.PowerProfile"
            val mPowerProfile = Class.forName(powerProfileClassName).getConstructor(Context::class.java).newInstance(context)
            val capacity = Class.forName(powerProfileClassName).getMethod("getBatteryCapacity").invoke(mPowerProfile)
            batteryDesignCapacity = (capacity as Double).toInt()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return batteryDesignCapacity
    }

    fun getBatteryHealth(context: Context): Int {
        val intentFilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        intentFilter.priority = IntentFilter.SYSTEM_HIGH_PRIORITY
        val intent = context.registerReceiver(null, intentFilter)
        if (intent != null) {
            return intent.getIntExtra(BatteryManager.EXTRA_HEALTH, BatteryManager.BATTERY_HEALTH_UNKNOWN)
        }
        return BatteryManager.BATTERY_HEALTH_UNKNOWN
    }

    fun getBatteryTechnology(context: Context): String {
        val intentFilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        intentFilter.priority = IntentFilter.SYSTEM_HIGH_PRIORITY
        val intent = context.registerReceiver(null, intentFilter)
        if (intent != null) {
            val technology = intent.getStringExtra(BatteryManager.EXTRA_TECHNOLOGY)
            if (technology != null) {
                return technology
            }
        }
        return "unknown"
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
                return batteryVolt * 1000;
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
        var batteryCurrent = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW)
        var adjustJson: JSONObject? = null
        runBlocking {
            mutex.withLock {
                if (adjustFile == null) {
                    return@withLock
                }
                adjustJson = JSON.parseObject(adjustFile!!.readText(Charsets.UTF_8))
            }
        }
        if (adjustJson != null) {
            if (adjustJson!!.getBoolean("dualBattery")) {
                batteryCurrent *= 2
            }
            if (adjustJson!!.getBoolean("currentUnitUA")) {
                batteryCurrent /= 1000
            }
            if (adjustJson!!.getBoolean("currentReverse")) {
                batteryCurrent = -batteryCurrent
            }
        }
        return batteryCurrent
    }

    fun setDualBattery(dualBattery: Boolean) {
        CoroutineScope(Dispatchers.Default).launch {
            mutex.withLock {
                if (adjustFile == null) {
                    return@withLock
                }
                val adjustJson: JSONObject? = JSON.parseObject(adjustFile!!.readText(Charsets.UTF_8))
                if (adjustJson != null) {
                    adjustJson["dualBattery"] = dualBattery
                    adjustFile!!.writeText(adjustJson.toJSONString(), Charsets.UTF_8)
                }
            }
        }
    }

    fun setCurrentUnitUA(currentUnitUA: Boolean) {
        CoroutineScope(Dispatchers.Default).launch {
            mutex.withLock {
                if (adjustFile == null) {
                    return@withLock
                }
                val adjustJson: JSONObject? = JSON.parseObject(adjustFile!!.readText(Charsets.UTF_8))
                if (adjustJson != null) {
                    adjustJson["currentUnitUA"] = currentUnitUA
                    adjustFile!!.writeText(adjustJson.toJSONString(), Charsets.UTF_8)
                }
            }
        }
    }

    fun setCurrentReverse(currentReverse: Boolean) {
        CoroutineScope(Dispatchers.Default).launch {
            mutex.withLock {
                if (adjustFile == null) {
                    return@withLock
                }
                val adjustJson: JSONObject? = JSON.parseObject(adjustFile!!.readText(Charsets.UTF_8))
                if (adjustJson != null) {
                    adjustJson["currentReverse"] = currentReverse
                    adjustFile!!.writeText(adjustJson.toJSONString(), Charsets.UTF_8)
                }
            }
        }
    }

    fun getDualBattery(): Boolean {
        var adjustJson: JSONObject? = null
        runBlocking {
            mutex.withLock {
                if (adjustFile == null) {
                    return@withLock
                }
                adjustJson = JSON.parseObject(adjustFile!!.readText(Charsets.UTF_8))
            }
        }
        if (adjustJson != null) {
            return adjustJson!!.getBoolean("dualBattery")
        }
        return false
    }

    fun getCurrentUnitUA(): Boolean {
        var adjustJson: JSONObject? = null
        runBlocking {
            mutex.withLock {
                if (adjustFile == null) {
                    return@withLock
                }
                adjustJson = JSON.parseObject(adjustFile!!.readText(Charsets.UTF_8))
            }
        }
        if (adjustJson != null) {
            return adjustJson!!.getBoolean("currentUnitUA")
        }
        return false
    }

    fun getCurrentReverse(): Boolean {
        var adjustJson: JSONObject? = null
        runBlocking {
            mutex.withLock {
                if (adjustFile == null) {
                    return@withLock
                }
                adjustJson = JSON.parseObject(adjustFile!!.readText(Charsets.UTF_8))
            }
        }
        if (adjustJson != null) {
            return adjustJson!!.getBoolean("currentReverse")
        }
        return false
    }
}