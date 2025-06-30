package cumulus.battery.stats.utils

import android.os.BatteryManager

data class BatteryHealthReport(
    var sampleSize: Int = 0,
    var totalChargedPercentage: Int = 0,
    var totalChargedCapacity: Int = 0,
    var estimatingCapacity: Int = 0
)

class BattStatsRecordAnalysis(private val records: List<BatteryStatsItem>) {
    val dischargingRecords = getLastDischargingRecords()
    val chargingRecords = getLastChargingRecords()

    private fun getLastChargingRecords(): List<BatteryStatsItem> {
        if (records.isNotEmpty()) {
            var endPos = 0
            for (i in records.lastIndex downTo 0) {
                if (records[i].batteryStatus == BatteryManager.BATTERY_STATUS_CHARGING) {
                    endPos = i
                    break
                }
            }

            var beginPos = endPos
            for (i in endPos downTo 0) {
                if (records[i].batteryStatus != BatteryManager.BATTERY_STATUS_CHARGING) {
                    beginPos = i
                    break
                }
            }
            if (beginPos == endPos) {
                beginPos = 0
            }

            return records.subList(beginPos, endPos)
        }
        return listOf()
    }

    private fun getLastDischargingRecords(): List<BatteryStatsItem> {
        if (records.isNotEmpty()) {
            var endPos = 0
            for (i in records.lastIndex downTo 0) {
                if (records[i].batteryStatus == BatteryManager.BATTERY_STATUS_DISCHARGING ||
                    records[i].batteryStatus == BatteryManager.BATTERY_STATUS_NOT_CHARGING
                ) {
                    endPos = i
                    break
                }
            }

            var beginPos = endPos
            for (i in endPos downTo 0) {
                if (records[i].batteryStatus != BatteryManager.BATTERY_STATUS_DISCHARGING &&
                    records[i].batteryStatus != BatteryManager.BATTERY_STATUS_NOT_CHARGING
                ) {
                    beginPos = i
                    break
                }
            }
            if (beginPos == endPos) {
                beginPos = 0
            }

            return records.subList(beginPos, endPos)
        }
        return listOf()
    }

    fun getUsagePercentageList(): List<Int> {
        if (dischargingRecords.isNotEmpty()) {
            val percentageList: MutableList<Int> = mutableListOf()
            for (item in dischargingRecords) {
                percentageList.add(item.batteryPercentage)
            }
            return percentageList
        }
        return listOf()
    }

    fun getScreenOnDuration(): Long {
        if (dischargingRecords.isNotEmpty()) {
            var duration = 0L
            for (i in (dischargingRecords.lastIndex - 1) downTo 0) {
                if (!dischargingRecords[i].packageName.equals("standby")) {
                    duration += dischargingRecords[i + 1].timestamp - dischargingRecords[i].timestamp
                }
            }
            return duration
        }
        return 0L
    }

    fun getScreenOnAveragePower(): Int {
        if (dischargingRecords.isNotEmpty()) {
            var averagePower = 0.0
            var recordNum = 0
            for (i in dischargingRecords.lastIndex downTo 0) {
                if (!dischargingRecords[i].packageName.equals("standby") &&
                    dischargingRecords[i].batteryPower < 0
                ) {
                    averagePower =
                        (averagePower * recordNum + (-dischargingRecords[i].batteryPower)) / (recordNum + 1)
                    recordNum++
                }
            }
            return averagePower.toInt()
        }
        return 0
    }

    fun getScreenOnUsedPercentage(): Int {
        if (dischargingRecords.isNotEmpty()) {
            var percentage = 0
            for (i in (dischargingRecords.lastIndex - 1) downTo 0) {
                if (!dischargingRecords[i].packageName.equals("standby")) {
                    percentage +=
                        dischargingRecords[i].batteryPercentage - dischargingRecords[i + 1].batteryPercentage
                }
            }
            return percentage
        }
        return 0
    }

    fun getScreenOffDuration(): Long {
        if (dischargingRecords.isNotEmpty()) {
            var duration = 0L
            for (i in (dischargingRecords.lastIndex - 1) downTo 0) {
                if (dischargingRecords[i].packageName.equals("standby")) {
                    duration += dischargingRecords[i + 1].timestamp - dischargingRecords[i].timestamp
                }
            }
            return duration
        }
        return 0L
    }

    fun getScreenOffAveragePower(): Int {
        if (dischargingRecords.isNotEmpty()) {
            var averagePower = 0.0
            var recordNum = 0
            for (i in dischargingRecords.lastIndex downTo 0) {
                if (dischargingRecords[i].packageName.equals("standby") &&
                    dischargingRecords[i].batteryPower < 0
                ) {
                    averagePower =
                        (averagePower * recordNum + (-dischargingRecords[i].batteryPower)) / (recordNum + 1)
                    recordNum++
                }
            }
            return averagePower.toInt()
        }
        return 0
    }

    fun getScreenOffUsedPercentage(): Int {
        if (dischargingRecords.isNotEmpty()) {
            var percentage = 0
            for (i in (dischargingRecords.lastIndex - 1) downTo 0) {
                if (dischargingRecords[i].packageName.equals("standby")) {
                    percentage +=
                        dischargingRecords[i].batteryPercentage - dischargingRecords[i + 1].batteryPercentage
                }
            }
            return percentage
        }
        return 0
    }

    fun getPerappPowerList(): Map<String, List<Int>> {
        if (dischargingRecords.isNotEmpty()) {
            val perappPowerList: MutableMap<String, MutableList<Int>> = mutableMapOf()
            for (item in dischargingRecords) {
                if (!perappPowerList.containsKey(item.packageName)) {
                    perappPowerList[item.packageName] = mutableListOf()
                }
                if (item.batteryPower < 0) {
                    perappPowerList[item.packageName]!!.add(-item.batteryPower)
                }
            }
            return perappPowerList
        }
        return mapOf()
    }

    fun getPerappTemperatureList(): Map<String, List<Int>> {
        if (dischargingRecords.isNotEmpty()) {
            val perappTemperatureList: MutableMap<String, MutableList<Int>> = mutableMapOf()
            for (item in dischargingRecords) {
                if (!perappTemperatureList.containsKey(item.packageName)) {
                    perappTemperatureList[item.packageName] = mutableListOf()
                }
                perappTemperatureList[item.packageName]!!.add(item.batteryTemperature)
            }
            return perappTemperatureList
        }
        return mapOf()
    }

    fun getPerappUsedPercentage(): Map<String, Int> {
        if (dischargingRecords.isNotEmpty()) {
            val perappUsedPercentage: MutableMap<String, Int> = mutableMapOf()
            for (i in (dischargingRecords.lastIndex - 1) downTo 0) {
                if ((dischargingRecords[i].batteryPercentage - dischargingRecords[i + 1].batteryPercentage) > 0) {
                    if (!perappUsedPercentage.containsKey(dischargingRecords[i].packageName)) {
                        perappUsedPercentage[dischargingRecords[i].packageName] = 0
                    }
                    perappUsedPercentage[dischargingRecords[i].packageName] =
                        perappUsedPercentage[dischargingRecords[i].packageName]!! + 1
                }
            }
            return perappUsedPercentage
        }
        return mapOf()
    }

    fun getPerappUsedTime(): Map<String, Long> {
        if (dischargingRecords.isNotEmpty()) {
            val perappUsedTime: MutableMap<String, Long> = mutableMapOf()
            for (i in (dischargingRecords.lastIndex - 1) downTo 0) {
                val duration = dischargingRecords[i + 1].timestamp - dischargingRecords[i].timestamp
                if (duration > 0) {
                    if (!perappUsedTime.containsKey(dischargingRecords[i].packageName)) {
                        perappUsedTime[dischargingRecords[i].packageName] = 0L
                    }
                    perappUsedTime[dischargingRecords[i].packageName] =
                        perappUsedTime[dischargingRecords[i].packageName]!! + duration
                }
            }
            return perappUsedTime
        }
        return mapOf()
    }

    fun getRemainingUsageTime(): Long {
        val remainingPercentage = records.lastOrNull()?.batteryPercentage
        if (remainingPercentage != null && remainingPercentage > 0) {
            val screenOnDuration = getScreenOnDuration()
            val screenOnAveragePower = getScreenOnAveragePower()
            val screenOnUsedPercentage = getScreenOnUsedPercentage()
            if (screenOnDuration > 0 && screenOnAveragePower > 0 && screenOnUsedPercentage > 0) {
                val predictBatteryCapacity =
                    (screenOnAveragePower.toDouble() * screenOnDuration / 3600.0) /
                            ((screenOnUsedPercentage.toDouble() + 1.0) / 100.0)
                val remainingBatteryCapacity = predictBatteryCapacity * remainingPercentage / 100.0
                return (remainingBatteryCapacity / screenOnAveragePower * 3600.0).toLong()
            }
        }
        return 0L
    }

    fun getLastChargingDuration(): Long {
        if (chargingRecords.isNotEmpty()) {
            return (chargingRecords.last().timestamp - chargingRecords.first().timestamp)
        }
        return 0L
    }

    fun getLastChargingPowerList(): List<Int> {
        if (chargingRecords.isNotEmpty()) {
            val chargingPowerList: MutableList<Int> = mutableListOf()
            for (item in chargingRecords) {
                if (item.batteryPower > 0) {
                    chargingPowerList.add(item.batteryPower)
                } else {
                    chargingPowerList.add(0)
                }
            }
            return chargingPowerList
        }
        return listOf()
    }

    fun getLastChargingTemperatureList(): List<Int> {
        if (chargingRecords.isNotEmpty()) {
            val chargingTemperatureList: MutableList<Int> = mutableListOf()
            for (item in chargingRecords) {
                chargingTemperatureList.add(item.batteryTemperature)
            }
            return chargingTemperatureList
        }
        return listOf()
    }

    fun getLastChargingPercentageList(): List<Int> {
        if (chargingRecords.isNotEmpty()) {
            val chargingPercentageList: MutableList<Int> = mutableListOf()
            for (item in chargingRecords) {
                chargingPercentageList.add(item.batteryPercentage)
            }
            return chargingPercentageList
        }
        return listOf()
    }

    fun getBatteryHealthReport(): BatteryHealthReport {
        if (records.isNotEmpty()) {
            val samples: MutableList<List<BatteryStatsItem>> = mutableListOf()
            var pos = 0
            while (pos < records.lastIndex) {
                if (records[pos].batteryStatus == BatteryManager.BATTERY_STATUS_CHARGING) {
                    val sample: MutableList<BatteryStatsItem> = mutableListOf()
                    for (i in pos until records.size) {
                        if (records[i].batteryStatus == BatteryManager.BATTERY_STATUS_CHARGING) {
                            sample.add(records[i])
                        } else {
                            break
                        }
                    }
                    samples.add(sample)
                    pos += sample.size
                } else {
                    pos++
                }
            }

            val report = BatteryHealthReport()
            for (sample in samples) {
                val chargedPercentage =
                    sample.last().batteryPercentage - sample.first().batteryPercentage
                if (sample.size > 100 && chargedPercentage > 50) {
                    report.totalChargedPercentage += chargedPercentage
                    report.sampleSize++

                    var chargedCapacity = 0.0
                    for (i in 0 until sample.lastIndex) {
                        val duration = sample[i + 1].timestamp - sample[i].timestamp
                        val power =
                            (sample[i].batteryPower + sample[i + 1].batteryPower).toDouble() / 2.0
                        chargedCapacity += power * duration / 3600.0
                    }
                    report.totalChargedCapacity += chargedCapacity.toInt()
                }
            }
            if (report.totalChargedPercentage > 0 && report.totalChargedCapacity > 0) {
                report.estimatingCapacity =
                    (report.totalChargedCapacity.toDouble() * 100.0 / report.totalChargedPercentage).toInt()
            }
            return report
        }
        return BatteryHealthReport()
    }
}