package cumulus.battery.stats.utils

import android.os.BatteryManager
import cumulus.battery.stats.widgets.DataPointList
import cumulus.battery.stats.widgets.DataPointMutableList
import kotlin.math.abs

data class BatteryHealthReport(
    var sampleSize: Int = 0,
    var totalChargedPercentage: Int = 0,
    var totalChargedCapacity: Int = 0,
    var estimatingCapacity: Int = 0
)

data class BatteryStatsReport(
    var duration: Long = 0L,
    var percentageDifference: Int = 0,
    var averagePower: Int = 0,
    var maxPower: Int = 0,
    var averageTemperature: Int = 0,
    var maxTemperature: Int = 0,
    var percentageDataPoints: DataPointList = listOf(),
    var powerDataPoints: DataPointList = listOf(),
    var temperatureDataPoints: DataPointList = listOf()
)

class BatteryStatsRecordAnalysis(private val records: List<BatteryStatsItem>) {
    private val dischargingRecords = getLastDischargingRecords()
    private val chargingRecords = getLastChargingRecords()

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

    private fun generateBatteryStatsReport(records: List<BatteryStatsItem>): BatteryStatsReport? {
        if (records.size > 10) {
            val statsReport = BatteryStatsReport()

            val timestampList: MutableList<Long> = mutableListOf()
            val percentageList: MutableList<Int> = mutableListOf()
            val powerList: MutableList<Int> = mutableListOf()
            val temperatureList: MutableList<Int> = mutableListOf()
            for (item in records) {
                timestampList.add(item.timestamp)
                percentageList.add(item.batteryPercentage)
                if (item.batteryStatus == BatteryManager.BATTERY_STATUS_CHARGING) {
                    powerList.add(when {
                        (item.batteryPower > 0) -> item.batteryPower
                        else -> 0
                    })
                } else {
                    powerList.add(when {
                        (item.batteryPower < 0) -> -item.batteryPower
                        else -> 0
                    })
                }
                temperatureList.add(item.batteryTemperature)
            }

            statsReport.percentageDifference = percentageList.max() - percentageList.min()
            statsReport.averagePower = powerList.average().toInt()
            statsReport.maxPower = powerList.max()
            statsReport.averageTemperature = temperatureList.average().toInt()
            statsReport.maxTemperature = temperatureList.max()

            val percentageDataPoints: DataPointMutableList = mutableListOf()
            val powerDataPoints: DataPointMutableList = mutableListOf()
            val temperatureDataPoints: DataPointMutableList = mutableListOf()
            var durationSinceStart = 0U
            for (i in 1 until timestampList.size) {
                val duration = (timestampList[i] - timestampList[i - 1]).toUInt()
                if (duration < 5U) {
                    durationSinceStart += duration
                    percentageDataPoints.add(Pair(durationSinceStart, percentageList[i].toUInt()))
                    powerDataPoints.add(Pair(durationSinceStart, (powerList[i] / 1000).toUInt()))
                    temperatureDataPoints.add(Pair(durationSinceStart, temperatureList[i].toUInt()))
                }
            }
            statsReport.duration = durationSinceStart.toLong()
            statsReport.percentageDataPoints = percentageDataPoints
            statsReport.powerDataPoints = powerDataPoints
            statsReport.temperatureDataPoints = temperatureDataPoints

            return statsReport
        }
        return null
    }

    fun getUsagePercentageDataPoints(): DataPointList {
        if (records.isNotEmpty()) {
            val percentageDataPoints: DataPointMutableList = mutableListOf()
            val minTimestamp = GetTimeStamp() - 24L * 3600L
            for (i in records.lastIndex downTo 0) {
                if (records[i].timestamp > minTimestamp) {
                    percentageDataPoints.add(
                        Pair(
                            (records[i].timestamp - minTimestamp).toUInt(),
                            records[i].batteryPercentage.toUInt()
                        )
                    )
                } else {
                    break
                }
            }
            return percentageDataPoints.reversed()
        }
        return listOf()
    }

    fun getScreenOnDuration(): Long {
        if (dischargingRecords.isNotEmpty()) {
            var duration = 0L
            for (i in (dischargingRecords.lastIndex - 1) downTo 0) {
                if (dischargingRecords[i].packageName != "standby") {
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
                if (dischargingRecords[i].packageName != "standby" &&
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
                if (dischargingRecords[i].packageName != "standby") {
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
                if (dischargingRecords[i].packageName == "standby") {
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
                if (dischargingRecords[i].packageName == "standby" &&
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
                if (dischargingRecords[i].packageName == "standby") {
                    percentage +=
                        dischargingRecords[i].batteryPercentage - dischargingRecords[i + 1].batteryPercentage
                }
            }
            return percentage
        }
        return 0
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

    fun getPerappBatteryStatsReport(): Map<String, BatteryStatsReport> {
        val perappBatteryStatsReport: MutableMap<String, BatteryStatsReport> = mutableMapOf()
        if (dischargingRecords.isNotEmpty()) {
            val perappRecords: MutableMap<String, MutableList<BatteryStatsItem>> = mutableMapOf()
            for (item in dischargingRecords) {
                if (!perappRecords.contains(item.packageName)) {
                    perappRecords[item.packageName] = mutableListOf()
                }
                perappRecords[item.packageName]!!.add(item)
            }
            for ((pkgName, records) in perappRecords) {
                if (pkgName != "other" && pkgName != "standby") {
                    val batteryStatsReport = generateBatteryStatsReport(records)
                    if (batteryStatsReport != null) {
                        perappBatteryStatsReport[pkgName] = batteryStatsReport
                    }
                }
            }
        }
        return perappBatteryStatsReport
    }

    fun getChargingBatteryStatsReport(): BatteryStatsReport {
        val batteryStatsReport = generateBatteryStatsReport(chargingRecords)
        if (batteryStatsReport != null) {
            return batteryStatsReport
        }
        return BatteryStatsReport()
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