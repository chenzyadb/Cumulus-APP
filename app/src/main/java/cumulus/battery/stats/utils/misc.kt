package cumulus.battery.stats.utils

import java.time.LocalDateTime
import java.time.ZoneOffset

fun GetTimeStamp(): Long {
    val currentDateTime = LocalDateTime.now()
    return currentDateTime.toInstant(ZoneOffset.UTC).epochSecond
}

fun DurationToText(duration: Long): String {
    if (duration >= 60) {
        var text = ""
        val day = duration / 3600 / 24
        if (day > 0) {
            text += "${day}天 "
        }
        val hour = (duration / 3600) % 24
        if (hour > 0) {
            text += "${hour}小时"
        }
        val minute = (duration / 60) % 60
        if (minute > 0) {
            text += "${minute}分钟"
        }
        return text
    }
    return "小于1分钟"
}

fun SimplifyDataPoints(dataPoints: IntArray): IntArray {
    if (dataPoints.size > 1000) {
        val simplifiedDataPoints: MutableList<Int> = mutableListOf()
        val factor = dataPoints.size.toDouble() / 500.0
        for (i in 0 until 500) {
            simplifiedDataPoints.add(dataPoints[(factor * i).toInt()])
        }
        simplifiedDataPoints.add(dataPoints.last())
        return simplifiedDataPoints.toIntArray()
    }
    return dataPoints
}