package cumulus.battery.stats.utils

import android.content.Context
import android.os.BatteryManager
import com.alibaba.fastjson2.JSONArray
import cumulus.battery.stats.objects.BatteryStatsProvider
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.math.abs

class BattStatsRecordAnalysis(
    private val context: Context,
    private val record: JSONArray?
) {
    private val mutex = Mutex()
    private var recordDuration: Long = 0
    private var lastChargingCapacity: Int = 0
    private var screenOnDuration: Long = 0
    private var screenOffDuration: Long = 0
    private var screenOnAveragePower: Int = 0
    private var screenOffAveragePower: Int = 0
    private var screenOnCapacityCost: Int = 0
    private var screenOffCapacityCost: Int = 0
    private var remainingUsageTime: Long = 0
    private var batteryCapacityArray: IntArray = IntArray(0)
    private var chargingDuration: Long = 0
    private var chargingCapacity: Int = 0
    private var chargingMaxPower: Int = 0
    private var chargingAveragePower: Int = 0
    private var chargingMaxTemperarure: Int = 0
    private var chargingAverageTemperature: Int = 0
    private var chargingCapacityArray: IntArray = IntArray(0)
    private var chargingPowerArray: IntArray = IntArray(0)
    private var chargingTemperatureArray: IntArray = IntArray(0)
    private var estimatingBatteryCapacity: Int = 0
    private var appsDurationMap: HashMap<String, Long> = hashMapOf()
    private var appsMaxPowerMap: HashMap<String, Int> = hashMapOf()
    private var appsAveragePowerMap: HashMap<String, Int> = hashMapOf()
    private var appsMaxTemperatureMap: HashMap<String, Int> = hashMapOf()
    private var appsAverageTemperatureMap: HashMap<String, Int> = hashMapOf()

    suspend fun doAnalysis() {
        mutex.withLock {
            if (record == null || record.size < 10) {
                return@withLock
            }

            val recordStartTime = record.getJSONObject(0).getLong("timeStamp")
            val recordEndTime = record.getJSONObject(record.size - 1).getLong("timeStamp")
            recordDuration = recordEndTime - recordStartTime
            lastChargingCapacity = record.getJSONObject(0).getIntValue("batteryCapacity")
            batteryCapacityArray = IntArray(100) { -1 }
            batteryCapacityArray[0] = lastChargingCapacity

            var screenOnEnergyCost: Long = 0
            var screenOffEnergyCost: Long = 0
            var chargingEnergyIncrease: Long = 0
            val chargingCapacityList = mutableListOf<Int>()
            val chargingTemperatureList = mutableListOf<Int>()
            val chargingPowerList = mutableListOf<Int>()
            val appsPowerListMap = hashMapOf<String, MutableList<Int>>()
            val appsTemperatureListMap = hashMapOf<String, MutableList<Int>>()
            for (idx in 1 until record.size) {
                val item = record.getJSONObject(idx - 1)
                val nextItem = record.getJSONObject(idx)
                val timeStamp = item.getLong("timeStamp")
                val duration = (nextItem.getLong("timeStamp") - timeStamp).toInt()
                val pkgName = item.getString("pkgName")
                val batteryStatus = item.getIntValue("batteryStatus")
                val batteryCapacity = item.getIntValue("batteryCapacity")
                val batteryCapacityDiff = abs(batteryCapacity - nextItem.getIntValue("batteryCapacity"))
                val batteryPower = abs(item.getIntValue("batteryPower"))
                val batteryTemperature = item.getIntValue("batteryTemperature")

                if (batteryStatus == BatteryManager.BATTERY_STATUS_DISCHARGING ||
                    batteryStatus == BatteryManager.BATTERY_STATUS_NOT_CHARGING
                ) {
                    if (pkgName != "standby") {
                        screenOnEnergyCost += batteryPower * duration
                        screenOnDuration += duration
                        screenOnCapacityCost += batteryCapacityDiff

                        if (appsDurationMap.containsKey(pkgName)) {
                            appsDurationMap[pkgName] = appsDurationMap[pkgName]!! + duration
                        } else {
                            appsDurationMap[pkgName] = duration.toLong()
                        }
                        if (appsPowerListMap.containsKey(pkgName)) {
                            appsPowerListMap[pkgName]!!.add(batteryPower)
                        } else {
                            appsPowerListMap[pkgName] = mutableListOf(batteryPower)
                        }
                        if (appsTemperatureListMap.containsKey(pkgName)) {
                            appsTemperatureListMap[pkgName]!!.add(batteryTemperature)
                        } else {
                            appsTemperatureListMap[pkgName] = mutableListOf(batteryTemperature)
                        }
                    } else {
                        screenOffEnergyCost += batteryPower * duration
                        screenOffDuration += duration
                        screenOffCapacityCost += batteryCapacityDiff
                    }
                } else if (batteryStatus == BatteryManager.BATTERY_STATUS_CHARGING) {
                    chargingCapacityList.add(batteryCapacity)
                    chargingTemperatureList.add(batteryTemperature)
                    chargingPowerList.add(batteryPower)
                    chargingEnergyIncrease += batteryPower * duration
                    chargingDuration += duration
                }

                val batteryCapacityArrayIdx = (timeStamp - recordStartTime) * (batteryCapacityArray.size - 1) / recordDuration
                if (batteryCapacityArrayIdx in 1..batteryCapacityArray.lastIndex) {
                    batteryCapacityArray[batteryCapacityArrayIdx.toInt()] = batteryCapacity
                }
            }

            var lastBatteryCapacity = batteryCapacityArray[0]
            for (idx in 1 until batteryCapacityArray.size) {
                if (batteryCapacityArray[idx] == -1) {
                    batteryCapacityArray[idx] = lastBatteryCapacity
                } else {
                    lastBatteryCapacity = batteryCapacityArray[idx]
                }
            }

            if (screenOnDuration > 0) {
                screenOnAveragePower = (screenOnEnergyCost / screenOnDuration).toInt()
            }
            if (screenOffDuration > 0) {
                screenOffAveragePower = (screenOffEnergyCost / screenOffDuration).toInt()
            }

            if (screenOnCapacityCost >= 10) {
                val batteryCurCapacity = BatteryStatsProvider.getBatteryCapacity(context)
                remainingUsageTime = screenOnDuration / screenOnCapacityCost * batteryCurCapacity
            } else if (screenOnAveragePower > 0) {
                val batteryCurCapacity = BatteryStatsProvider.getBatteryCapacity(context)
                val batteryDesignCapacity = BatteryStatsProvider.getBatteryDesignCapacity(context)
                val batteryRemainingEnergy = batteryDesignCapacity.toLong() * 3880 * 3600 / 1000 * batteryCurCapacity / 100
                remainingUsageTime = batteryRemainingEnergy / screenOnAveragePower
            }

            if (chargingCapacityList.size > 0) {
                chargingCapacity = chargingCapacityList.max() - chargingCapacityList.min()
            }
            if (chargingPowerList.size > 0) {
                chargingMaxPower = chargingPowerList.max()
            }
            if (chargingDuration > 0) {
                chargingAveragePower = (chargingEnergyIncrease / chargingDuration).toInt()
            }
            if (chargingTemperatureList.size > 0) {
                chargingMaxTemperarure = chargingTemperatureList.max()
                var chargingTemperatureSum: Long = 0
                chargingTemperatureList.forEach {
                    chargingTemperatureSum += it
                }
                chargingAverageTemperature = (chargingTemperatureSum / chargingTemperatureList.size).toInt()
            }

            if (chargingCapacityList.size <= 100) {
                chargingCapacityArray = chargingCapacityList.toIntArray()
            } else {
                chargingCapacityArray = IntArray(100)
                val step = chargingCapacityList.size.toDouble() / 100
                for (idx in 0 until chargingCapacityArray.size) {
                    val listIdx = (idx * step).toInt()
                    if (listIdx < chargingCapacityList.size) {
                        chargingCapacityArray[idx] = chargingCapacityList[listIdx]
                    }
                }
            }
            if (chargingPowerList.size <= 500) {
                chargingPowerArray = chargingPowerList.toIntArray()
            } else {
                chargingPowerArray = IntArray(500)
                val step = chargingPowerList.size.toDouble() / 500
                for (idx in 0 until chargingPowerArray.size) {
                    val listIdx = (idx * step).toInt()
                    if (listIdx < chargingPowerList.size) {
                        chargingPowerArray[idx] = chargingPowerList[listIdx]
                    }
                }
            }
            if (chargingTemperatureList.size <= 500) {
                chargingTemperatureArray = chargingTemperatureList.toIntArray()
            } else {
                chargingTemperatureArray = IntArray(500)
                val step = chargingTemperatureList.size.toDouble() / 500
                for (idx in 0 until chargingTemperatureArray.size) {
                    val listIdx = (idx * step).toInt()
                    if (listIdx < chargingTemperatureList.size) {
                        chargingTemperatureArray[idx] = chargingTemperatureList[listIdx]
                    }
                }
            }

            if (chargingCapacity > 10) {
                val chargingEnergy = chargingAveragePower * chargingDuration / 3600
                estimatingBatteryCapacity = (chargingEnergy * 100 / chargingCapacity).toInt() * 1000 / 3880
            }

            for ((pkgName, list) in appsPowerListMap.entries) {
                if (list.size > 0) {
                    appsMaxPowerMap[pkgName] = list.max()
                    var sum: Long = 0
                    list.forEach {
                        sum += it
                    }
                    appsAveragePowerMap[pkgName] = (sum / list.size).toInt()
                }
            }
            for ((pkgName, list) in appsTemperatureListMap.entries) {
                if (list.size > 0) {
                    appsMaxTemperatureMap[pkgName] = list.max()
                    var sum: Long = 0
                    list.forEach {
                        sum += it
                    }
                    appsAverageTemperatureMap[pkgName] = (sum / list.size).toInt()
                }
            }
        }
    }

    fun getRecordDuration(): Long {
        var duration: Long
        runBlocking {
            mutex.withLock {
                duration = recordDuration
            }
        }
        return duration
    }

    fun getlastChargingCapacity(): Int {
        var capacity: Int
        runBlocking {
            mutex.withLock {
                capacity = lastChargingCapacity
            }
        }
        return capacity
    }

    fun getScreenOnDuration(): Long {
        var duration: Long
        runBlocking {
            mutex.withLock {
                duration = screenOnDuration
            }
        }
        return duration
    }

    fun getScreenOffDuration(): Long {
        var duration: Long
        runBlocking {
            mutex.withLock {
                duration = screenOffDuration
            }
        }
        return duration
    }

    fun getScreenOnAveragePower(): Int {
        var power: Int
        runBlocking {
            mutex.withLock {
                power = screenOnAveragePower
            }
        }
        return power
    }

    fun getScreenOffAveragePower(): Int {
        var power: Int
        runBlocking {
            mutex.withLock {
                power = screenOffAveragePower
            }
        }
        return power
    }

    fun getScreenOnCapacityCost(): Int {
        var capacity: Int
        runBlocking {
            mutex.withLock {
                capacity = screenOnCapacityCost
            }
        }
        return capacity
    }

    fun getScreenOffCapacityCost(): Int {
        var capacity: Int
        runBlocking {
            mutex.withLock {
                capacity = screenOffCapacityCost
            }
        }
        return capacity
    }

    fun getRemainingUsageTime(): Long {
        var time: Long
        runBlocking {
            mutex.withLock {
                time = remainingUsageTime
            }
        }
        return time
    }

    fun getBatteryCapacityArray(): IntArray {
        var array: IntArray
        runBlocking {
            mutex.withLock {
                array = batteryCapacityArray
            }
        }
        return array
    }

    fun getChargingDuration(): Long {
        var duration: Long
        runBlocking {
            mutex.withLock {
                duration = chargingDuration
            }
        }
        return duration
    }

    fun getChargingCapacity(): Int {
        var capacity: Int
        runBlocking {
            mutex.withLock {
                capacity = chargingCapacity
            }
        }
        return capacity
    }

    fun getChargingMaxPower(): Int {
        var power: Int
        runBlocking {
            mutex.withLock {
                power = chargingMaxPower
            }
        }
        return power
    }

    fun getChargingAveragePower(): Int {
        var power: Int
        runBlocking {
            mutex.withLock {
                power = chargingAveragePower
            }
        }
        return power
    }

    fun getChargingMaxTemperature(): Int {
        var temperature: Int
        runBlocking {
            mutex.withLock {
                temperature = chargingMaxTemperarure
            }
        }
        return temperature
    }

    fun getChargingAverageTemperature(): Int {
        var temperature: Int
        runBlocking {
            mutex.withLock {
                temperature = chargingAverageTemperature
            }
        }
        return temperature
    }

    fun getChargingCapacityArray(): IntArray {
        var array: IntArray
        runBlocking {
            mutex.withLock {
                array = chargingCapacityArray
            }
        }
        return array
    }

    fun getChargingPowerArray(): IntArray {
        var array: IntArray
        runBlocking {
            mutex.withLock {
                array = chargingPowerArray
            }
        }
        return array
    }

    fun getChargingTemperatureArray(): IntArray {
        var array: IntArray
        runBlocking {
            mutex.withLock {
                array = chargingTemperatureArray
            }
        }
        return array
    }

    fun getEstimatingBatteryCapacity(): Int {
        var batteryCapacity: Int
        runBlocking {
            mutex.withLock {
                batteryCapacity = estimatingBatteryCapacity
            }
        }
        return batteryCapacity
    }

    fun getAppsDurationMap(): HashMap<String, Long> {
        var map: HashMap<String, Long>
        runBlocking {
            mutex.withLock {
                map = appsDurationMap
            }
        }
        return map
    }

    fun getAppsMaxPowerMap(): HashMap<String, Int> {
        var map: HashMap<String, Int>
        runBlocking {
            mutex.withLock {
                map = appsMaxPowerMap
            }
        }
        return map
    }

    fun getAppsAveragePowerMap(): HashMap<String, Int> {
        var map: HashMap<String, Int>
        runBlocking {
            mutex.withLock {
                map = appsAveragePowerMap
            }
        }
        return map
    }

    fun getAppsMaxTemperatureMap(): HashMap<String, Int> {
        var map: HashMap<String, Int>
        runBlocking {
            mutex.withLock {
                map = appsMaxTemperatureMap
            }
        }
        return map
    }

    fun getAppsAverageTemperatureMap(): HashMap<String, Int> {
        var map: HashMap<String, Int>
        runBlocking {
            mutex.withLock {
                map = appsAverageTemperatureMap
            }
        }
        return map
    }
}