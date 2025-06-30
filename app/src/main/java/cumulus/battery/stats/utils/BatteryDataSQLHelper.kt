package cumulus.battery.stats.utils

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

data class BatteryStatsItem(
    var timestamp: Long = 0,
    var packageName: String = "",
    var batteryStatus: Int = 0,
    var batteryPercentage: Int = 0,
    var batteryPower: Int = 0,
    var batteryTemperature: Int = 0
)

class BatteryDataSQLHelper(context: Context) :
    SQLiteOpenHelper(context, "battery_data.db", null, 1) {

    private val tableName = "BatteryData"

    override fun onCreate(dataBase: SQLiteDatabase?) {
        if (dataBase != null) {
            createTable(dataBase)
        }
    }

    override fun onUpgrade(dataBase: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        if (dataBase != null) {
            createTable(dataBase)
        }
    }

    fun createTable(dataBase: SQLiteDatabase) {
        val createTableEntries = """
            CREATE TABLE IF NOT EXISTS ${tableName} (
                timestamp INTEGER PRIMARY KEY,
                packageName TEXT NOT NULL,
                batteryStatus INTEGER NOT NULL,
                batteryPercentage INTEGER NOT NULL,
                batteryPower INTEGER NOT NULL,
                batteryTemperature INTEGER NOT NULL
            )
        """.trimIndent()

        try {
            dataBase.execSQL(createTableEntries)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun optimize() {
        try {
            val dataBase = writableDatabase
            val outOfDateTimeStamp = (GetTimeStamp() - (7L * 24L * 3600L)).toString()
            dataBase.execSQL("DELETE FROM ${tableName} WHERE timestamp < ${outOfDateTimeStamp}")
            dataBase.execSQL("PRAGMA synchronous = NORMAL")
            dataBase.execSQL("PRAGMA wal_checkpoint(FULL)")
            dataBase.execSQL("PRAGMA optimize")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun insert(item: BatteryStatsItem) {
        val dataBase = writableDatabase
        createTable(dataBase)

        try {
            val contentValues = ContentValues()
            contentValues.apply {
                put("timestamp", item.timestamp)
                put("packageName", item.packageName)
                put("batteryStatus", item.batteryStatus)
                put("batteryPercentage", item.batteryPercentage)
                put("batteryPower", item.batteryPower)
                put("batteryTemperature", item.batteryTemperature)
            }
            dataBase.insertWithOnConflict(
                tableName,
                null,
                contentValues,
                SQLiteDatabase.CONFLICT_REPLACE
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun readAll(): MutableList<BatteryStatsItem>? {
        try {
            val records: MutableList<BatteryStatsItem> = mutableListOf()
            readableDatabase.query(
                tableName,
                null,
                null,
                null,
                null,
                null,
                "timestamp ASC"
            ).use { cursor ->
                while (cursor.moveToNext()) {
                    val item = BatteryStatsItem()
                    item.timestamp = cursor.getLong(cursor.getColumnIndexOrThrow("timestamp"))
                    item.packageName = cursor.getString(cursor.getColumnIndexOrThrow("packageName"))
                    item.batteryStatus =
                        cursor.getInt(cursor.getColumnIndexOrThrow("batteryStatus"))
                    item.batteryPercentage =
                        cursor.getInt(cursor.getColumnIndexOrThrow("batteryPercentage"))
                    item.batteryPower =
                        cursor.getInt(cursor.getColumnIndexOrThrow("batteryPower"))
                    item.batteryTemperature =
                        cursor.getInt(cursor.getColumnIndexOrThrow("batteryTemperature"))
                    records.add(item)
                }
            }
            return records
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    fun deleteAll() {
        try {
            writableDatabase.execSQL("DROP TABLE IF EXISTS ${tableName}")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}