package cumulus.battery.stats.utils

import android.os.Build
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.Calendar
import java.util.TimeZone

fun GetTimeStamp(): Long {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
        val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        return (calendar.timeInMillis / 1000)
    }
    val currentDateTime = LocalDateTime.now()
    return currentDateTime.toInstant(ZoneOffset.UTC).epochSecond
}

fun DurationToText(duration: Long): String {
    if (duration >= 60) {
        var text = ""
        val day = duration / 3600 / 24
        if (day > 0) {
            text += "${day}天"
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