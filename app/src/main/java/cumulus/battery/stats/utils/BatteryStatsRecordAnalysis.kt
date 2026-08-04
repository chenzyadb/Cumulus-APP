package cumulus.battery.stats.utils

import android.os.BatteryManager
import cumulus.battery.stats.widgets.DataPointList
import cumulus.battery.stats.widgets.DataPointMutableList
import kotlin.math.roundToInt
import kotlin.math.roundToLong

private const val MIN_CHARGING_SESSION_PERCENTAGE = 10
private const val MAX_RECORD_GAP_SECONDS = 120L
private const val EXPECTED_SCREEN_ON_RECORD_INTERVAL_SECONDS = 2L
private const val MAX_CONTINUOUS_SCREEN_ON_GAP_SECONDS = 5L
private const val MIN_SCREEN_OFF_POWER_SAMPLE_SIZE_FOR_OUTLIER_FILTER = 3
private const val SCREEN_OFF_POWER_HIGH_OUTLIER_MULTIPLIER = 3.0
private const val REMAINING_TIME_LOOKBACK_SECONDS = 24L * 3600L
private const val MAX_REMAINING_TIME_SAMPLE_DROP_PERCENTAGE = 20
private const val MIN_REMAINING_TIME_SCREEN_ON_DURATION_SECONDS = 60L

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
        if (records.isEmpty()) {
            return listOf()
        }
        var fallbackRecords: List<BatteryStatsItem> = listOf()
        var endPos = records.lastIndex
        while (endPos >= 0) {
            while (endPos >= 0 &&
                records[endPos].batteryStatus != BatteryManager.BATTERY_STATUS_CHARGING
            ) {
                endPos--
            }
            if (endPos < 0) {
                break
            }
            var beginPos = endPos
            while (beginPos >= 0 &&
                records[beginPos].batteryStatus == BatteryManager.BATTERY_STATUS_CHARGING
            ) {
                beginPos--
            }
            val chargingSession = records.subList(beginPos + 1, endPos + 1)
            if (fallbackRecords.isEmpty()) {
                fallbackRecords = chargingSession
            }
            val chargedPercentage = chargingSession.last().batteryPercentage -
                    chargingSession.first().batteryPercentage
            if (chargedPercentage >= MIN_CHARGING_SESSION_PERCENTAGE) {
                return chargingSession
            }
            endPos = beginPos
        }
        return fallbackRecords
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
                    powerList.add(
                        when {
                            (item.batteryPower > 0) -> item.batteryPower
                            else -> 0
                        }
                    )
                } else {
                    powerList.add(
                        when {
                            (item.batteryPower < 0) -> -item.batteryPower
                            else -> 0
                        }
                    )
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
            var durationSinceStart = 0L
            for (i in 1 until timestampList.size) {
                val duration = timestampList[i] - timestampList[i - 1]
                if (duration in 0L until 5L) {
                    durationSinceStart += duration
                    percentageDataPoints.add(
                        Pair(durationSinceStart.toFloat(), percentageList[i].toFloat())
                    )
                    powerDataPoints.add(
                        Pair(durationSinceStart.toFloat(), powerList[i].toFloat() / 1000f)
                    )
                    temperatureDataPoints.add(
                        Pair(durationSinceStart.toFloat(), temperatureList[i].toFloat())
                    )
                }
            }
            statsReport.duration = durationSinceStart
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
            val recentRecords = records
                .filter { it.timestamp > minTimestamp }
                .sortedBy { it.timestamp }
            var lastPercentage: Int? = null
            var lastTimestamp: Long? = null
            for (record in recentRecords) {
                val x = (record.timestamp - minTimestamp).toFloat()
                val y = record.batteryPercentage.toFloat()
                if (record.timestamp == lastTimestamp) {
                    if (record.batteryPercentage != lastPercentage && percentageDataPoints.isNotEmpty()) {
                        percentageDataPoints[percentageDataPoints.lastIndex] = Pair(x, y)
                        lastPercentage = record.batteryPercentage
                    }
                    continue
                }
                if (record.batteryPercentage == lastPercentage) {
                    continue
                }
                percentageDataPoints.add(Pair(x, y))
                lastPercentage = record.batteryPercentage
                lastTimestamp = record.timestamp
            }
            return percentageDataPoints
        }
        return listOf()
    }

    fun getScreenOnDuration(): Long {
        if (dischargingRecords.isNotEmpty()) {
            var duration = 0L
            for (i in (dischargingRecords.lastIndex - 1) downTo 0) {
                val intervalDuration = getIntervalDuration(
                    dischargingRecords[i],
                    dischargingRecords[i + 1]
                )
                if (intervalDuration > 0L) {
                    duration += intervalDuration - estimateScreenOffDuration(
                        dischargingRecords[i],
                        dischargingRecords[i + 1]
                    )
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
        return getScreenUsagePercentages().first
    }

    fun getScreenOffDuration(): Long {
        if (dischargingRecords.isNotEmpty()) {
            var duration = 0L
            for (i in (dischargingRecords.lastIndex - 1) downTo 0) {
                duration += estimateScreenOffDuration(
                    dischargingRecords[i],
                    dischargingRecords[i + 1]
                )
            }
            return duration
        }
        return 0L
    }

    private fun estimateScreenOffDuration(
        currentRecord: BatteryStatsItem,
        nextRecord: BatteryStatsItem
    ): Long {
        val intervalDuration = getIntervalDuration(currentRecord, nextRecord)
        if (intervalDuration <= 0L) {
            return 0L
        }

        val currentScreenOff = currentRecord.packageName == "standby"
        val nextScreenOff = nextRecord.packageName == "standby"
        val estimatedDuration = when {
            currentScreenOff || nextScreenOff -> intervalDuration
            intervalDuration >= MAX_CONTINUOUS_SCREEN_ON_GAP_SECONDS ->
                intervalDuration - EXPECTED_SCREEN_ON_RECORD_INTERVAL_SECONDS
            else -> 0L
        }
        return estimatedDuration.coerceIn(0L, intervalDuration)
    }

    private fun getIntervalDuration(
        currentRecord: BatteryStatsItem,
        nextRecord: BatteryStatsItem
    ): Long {
        return nextRecord.timestamp - currentRecord.timestamp
    }

    fun getScreenOffAveragePower(): Int {
        if (dischargingRecords.isNotEmpty()) {
            val powerList: MutableList<Int> = mutableListOf()
            for (i in dischargingRecords.lastIndex downTo 0) {
                if (dischargingRecords[i].packageName == "standby" &&
                    dischargingRecords[i].batteryPower < 0
                ) {
                    powerList.add(-dischargingRecords[i].batteryPower)
                }
            }
            if (powerList.isNotEmpty()) {
                return filterScreenOffPowerOutliers(powerList).average().toInt()
            }
        }
        return 0
    }

    private fun filterScreenOffPowerOutliers(powerList: List<Int>): List<Int> {
        if (powerList.size < MIN_SCREEN_OFF_POWER_SAMPLE_SIZE_FOR_OUTLIER_FILTER) {
            return powerList
        }

        val sortedPowerList = powerList.sorted()
        val medianPower = sortedPowerList[(sortedPowerList.size - 1) / 2]
        val maxNormalPower = medianPower * SCREEN_OFF_POWER_HIGH_OUTLIER_MULTIPLIER
        val filteredPowerList = powerList.filter { it <= maxNormalPower }
        return filteredPowerList.ifEmpty { powerList }
    }

    fun getScreenOffUsedPercentage(): Int {
        return getScreenUsagePercentages().second
    }

    private fun getScreenUsagePercentages(): Pair<Int, Int> {
        if (dischargingRecords.isEmpty()) {
            return Pair(0, 0)
        }

        var totalUsedPercentage = 0
        var screenOffUsedPercentage = 0.0
        for (i in (dischargingRecords.lastIndex - 1) downTo 0) {
            val currentRecord = dischargingRecords[i]
            val nextRecord = dischargingRecords[i + 1]
            val intervalDuration = getIntervalDuration(currentRecord, nextRecord)
            if (intervalDuration <= 0L) {
                continue
            }

            val intervalUsedPercentage =
                (currentRecord.batteryPercentage - nextRecord.batteryPercentage).coerceAtLeast(0)
            if (intervalUsedPercentage == 0) {
                continue
            }

            val screenOffDuration = estimateScreenOffDuration(currentRecord, nextRecord)
            totalUsedPercentage += intervalUsedPercentage
            screenOffUsedPercentage += intervalUsedPercentage.toDouble() *
                    screenOffDuration.toDouble() / intervalDuration.toDouble()
        }

        val roundedScreenOffUsedPercentage =
            screenOffUsedPercentage.roundToInt().coerceIn(0, totalUsedPercentage)
        return Pair(
            totalUsedPercentage - roundedScreenOffUsedPercentage,
            roundedScreenOffUsedPercentage
        )
    }

    fun getRemainingUsageTime(): Long {
        val latestRecord = records.lastOrNull() ?: return 0L
        if (!latestRecord.isDischarging() || latestRecord.batteryPercentage <= 0) {
            return 0L
        }

        var nextRecord = latestRecord
        var screenOnDuration = 0L
        var screenOnUsedPercentage = 0.0
        for (i in records.lastIndex - 1 downTo 0) {
            val candidate = records[i]
            if (!candidate.isDischarging() || candidate.timestamp >= nextRecord.timestamp) {
                break
            }

            val intervalUsedPercentage = candidate.batteryPercentage - nextRecord.batteryPercentage
            if (intervalUsedPercentage < 0) {
                break
            }

            val sampleDuration = latestRecord.timestamp - candidate.timestamp
            val usedPercentage = candidate.batteryPercentage - latestRecord.batteryPercentage
            if (sampleDuration > REMAINING_TIME_LOOKBACK_SECONDS ||
                usedPercentage > MAX_REMAINING_TIME_SAMPLE_DROP_PERCENTAGE
            ) {
                break
            }

            val intervalDuration = nextRecord.timestamp - candidate.timestamp
            val screenOffDuration = estimateScreenOffDuration(candidate, nextRecord)
            val intervalScreenOnDuration = intervalDuration - screenOffDuration
            if (intervalScreenOnDuration > 0L) {
                screenOnDuration += intervalScreenOnDuration
                screenOnUsedPercentage += intervalUsedPercentage.toDouble() *
                        intervalScreenOnDuration.toDouble() / intervalDuration.toDouble()
            }
            nextRecord = candidate
        }

        if (screenOnDuration < MIN_REMAINING_TIME_SCREEN_ON_DURATION_SECONDS ||
            screenOnUsedPercentage <= 0.0
        ) {
            return 0L
        }

        return (screenOnDuration.toDouble() * latestRecord.batteryPercentage /
                screenOnUsedPercentage)
            .roundToLong()
    }

    private fun BatteryStatsItem.isDischarging(): Boolean {
        return batteryStatus == BatteryManager.BATTERY_STATUS_DISCHARGING ||
                batteryStatus == BatteryManager.BATTERY_STATUS_NOT_CHARGING
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
                if (pkgName != "standby") {
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
                var maxPercentage = sample.first().batteryPercentage
                for (item in sample) {
                    if (item.batteryPercentage > maxPercentage) {
                        maxPercentage = item.batteryPercentage
                    }
                }

                val chargedPercentage = maxPercentage - sample.first().batteryPercentage
                if (sample.size > 100 && chargedPercentage > 50) {
                    report.totalChargedPercentage += chargedPercentage
                    report.sampleSize++

                    var chargedCapacity = 0.0
                    for (i in 0 until sample.size - 1) {
                        val duration = sample[i + 1].timestamp - sample[i].timestamp
                        if (duration in 1..MAX_RECORD_GAP_SECONDS) {
                            val power =
                                (sample[i].batteryPower + sample[i + 1].batteryPower).toDouble() / 2.0
                            chargedCapacity += power * duration / 3600.0
                        }
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
