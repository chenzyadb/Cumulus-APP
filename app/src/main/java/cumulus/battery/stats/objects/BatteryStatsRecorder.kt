package cumulus.battery.stats.objects

import android.content.Context
import cumulus.battery.stats.utils.BatteryDataSQLHelper
import cumulus.battery.stats.utils.BatteryStatsItem
import cumulus.battery.stats.utils.GetTimeStamp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object BatteryStatsRecorder {
    private var sqlHelper: BatteryDataSQLHelper? = null

    fun init(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            if (sqlHelper == null) {
                sqlHelper = BatteryDataSQLHelper(context)
            }
        }
    }

    fun addItem(
        packageName: String,
        batteryStatus: Int,
        batteryPercentage: Int,
        batteryPower: Int,
        batteryTemperature: Int
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            val item = BatteryStatsItem()
            item.timestamp = GetTimeStamp()
            item.packageName = packageName
            item.batteryStatus = batteryStatus
            item.batteryPercentage = batteryPercentage
            item.batteryPower = batteryPower
            item.batteryTemperature = batteryTemperature
            sqlHelper?.insert(item)
        }
    }

    fun getRecords(): MutableList<BatteryStatsItem> {
        if (sqlHelper != null) {
            val records = sqlHelper!!.readAll()
            if (records != null) {
                return records
            }
        }
        return mutableListOf()
    }

    fun optimize() {
        CoroutineScope(Dispatchers.IO).launch {
            if (sqlHelper != null) {
                sqlHelper!!.optimize()
            }
        }
    }

    fun deleteHistoryData() {
        CoroutineScope(Dispatchers.IO).launch {
            if (sqlHelper != null) {
                sqlHelper!!.deleteAll()
            }
        }
    }
}
