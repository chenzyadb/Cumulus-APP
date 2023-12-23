package cumulus.battery.stats.objects

import android.content.Context
import android.os.BatteryManager
import com.alibaba.fastjson2.JSON
import com.alibaba.fastjson2.JSONArray
import com.alibaba.fastjson2.JSONObject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import java.time.LocalDateTime
import java.time.ZoneOffset

object BatteryStatsRecorder {
    private val mutex = Mutex()
    private var recordFile: File? = null
    private var recordName: String = "null"
    private var record: JSONArray? = null

    fun init(context: Context) {
        runBlocking {
            mutex.withLock {
                if (recordFile != null && recordName != "null" && record != null) {
                    val recordJson = JSON.parseObject(recordFile!!.readText(Charsets.UTF_8))
                    recordJson[recordName] = record
                    recordFile!!.writeText(recordJson.toJSONString(), Charsets.UTF_8)
                    return@withLock
                }
                val filesPath = context.filesDir.absolutePath
                recordFile = File("${filesPath}/battery_stats_record.json")
                if (!recordFile!!.exists()) {
                    recordFile!!.createNewFile()
                    val recordJson = JSONObject()
                    recordJson["curRecord"] = "default"
                    recordJson["default"] = JSONArray()
                    recordFile!!.writeText(recordJson.toJSONString(), Charsets.UTF_8)
                } else {
                    val recordJson = JSON.parseObject(recordFile!!.readText(Charsets.UTF_8))
                    if (recordJson.containsKey("curRecord")) {
                        recordName = recordJson.getString("curRecord")
                    } else {
                        recordName = "default"
                        recordJson["curRecord"] = recordName
                    }
                    if (recordJson.containsKey(recordName)) {
                        record = recordJson.getJSONArray(recordName)
                    } else {
                        record = JSONArray()
                        recordJson[recordName] = record
                    }
                    recordFile!!.writeText(recordJson.toJSONString(), Charsets.UTF_8)
                }
            }
        }
    }

    fun addItem(
        pkgName: String,
        batteryStatus: Int,
        batteryCapacity: Int,
        batteryPower: Int,
        batteryCurrent: Int,
        batteryTemperature: Int
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            mutex.withLock {
                if (record == null) {
                    return@withLock
                }
                var lastBatteryStatus = BatteryManager.BATTERY_STATUS_UNKNOWN
                if (record!!.size > 0) {
                    lastBatteryStatus = record!!.getJSONObject(record!!.size - 1).getIntValue("batteryStatus")
                }
                val item = JSONObject()
                val currentDateTime = LocalDateTime.now()
                val timestamp = currentDateTime.toEpochSecond(ZoneOffset.UTC)
                item["timeStamp"] = timestamp
                item["pkgName"] = pkgName
                item["batteryStatus"] = batteryStatus
                item["batteryCapacity"] = batteryCapacity
                item["batteryPower"] = batteryPower
                item["batteryCurrent"] = batteryCurrent
                item["batteryTemperature"] = batteryTemperature
                record!!.add(item)

                if (recordFile == null) {
                    return@withLock
                }
                if (lastBatteryStatus == BatteryManager.BATTERY_STATUS_CHARGING ||
                    lastBatteryStatus == BatteryManager.BATTERY_STATUS_FULL
                ) {
                    if (batteryStatus == BatteryManager.BATTERY_STATUS_DISCHARGING ||
                        batteryStatus == BatteryManager.BATTERY_STATUS_NOT_CHARGING
                    ) {
                        val recordJson = JSON.parseObject(recordFile!!.readText(Charsets.UTF_8))
                        if (recordName != "default") {
                            recordJson[recordName] = JSON.parseArray(record!!.toJSONString())
                            recordName = "default"
                        }
                        recordJson["prevRecord"] = JSON.parseArray(record!!.toJSONString())
                        record = JSONArray()
                        recordJson["curRecord"] = recordName
                        recordJson[recordName] = record
                        recordFile!!.writeText(recordJson.toJSONString(), Charsets.UTF_8)
                    }
                }
            }
        }
    }

    fun saveRecord() {
        CoroutineScope(Dispatchers.IO).launch {
            mutex.withLock {
                if (recordFile == null || recordName == "null" || record == null) {
                    return@withLock
                }
                val recordJson = JSON.parseObject(recordFile!!.readText(Charsets.UTF_8))
                recordJson["curRecord"] = recordName
                recordJson[recordName] = record
                recordFile!!.writeText(recordJson.toJSONString(), Charsets.UTF_8)
            }
        }
    }

    fun copyRecord(origName: String, newName: String) {
        CoroutineScope(Dispatchers.IO).launch {
            mutex.withLock {
                if (recordFile == null) {
                    return@withLock
                }
                val recordJson = JSON.parseObject(recordFile!!.readText(Charsets.UTF_8))
                if (origName == recordName) {
                    recordJson[newName] = record
                } else if (recordJson.containsKey(origName)) {
                    val origRecord = recordJson[origName]
                    recordJson[newName] = origRecord
                } else {
                    recordJson[newName] = JSONArray()
                }
                recordFile!!.writeText(recordJson.toJSONString(), Charsets.UTF_8)
            }
        }
    }

    fun clearRecord() {
        CoroutineScope(Dispatchers.IO).launch {
            mutex.withLock {
                if (recordFile == null) {
                    return@withLock
                }
                record = JSONArray()
                val recordJson = JSON.parseObject(recordFile!!.readText(Charsets.UTF_8))
                recordJson[recordName] = record
                recordFile!!.writeText(recordJson.toJSONString(), Charsets.UTF_8)
            }
        }
    }

    fun deleteRecord(name: String) {
        CoroutineScope(Dispatchers.IO).launch {
            mutex.withLock {
                if (name == recordName) {
                    record = JSONArray()
                }
                if (recordFile != null) {
                    val recordJson = JSON.parseObject(recordFile!!.readText(Charsets.UTF_8))
                    if (recordJson.containsKey(name)) {
                        recordJson.remove(name)
                    }
                    recordFile!!.writeText(recordJson.toJSONString(), Charsets.UTF_8)
                }
            }
        }
    }

    fun deleteAllRecord() {
        CoroutineScope(Dispatchers.IO).launch {
            mutex.withLock {
                recordName = "default"
                record = JSONArray()
                if (recordFile != null) {
                    val recordJson = JSONObject()
                    recordJson["curRecord"] = "default"
                    recordJson["default"] = record
                    recordFile!!.writeText(recordJson.toJSONString(), Charsets.UTF_8)
                }
            }
        }
    }

    fun getRecord(name: String): JSONArray? {
        var recordJson: JSONObject? = null
        runBlocking {
            mutex.withLock {
                if (recordFile == null) {
                    return@withLock
                }
                recordJson = JSON.parseObject(recordFile!!.readText(Charsets.UTF_8))
            }
        }
        if (recordJson != null && recordJson!!.containsKey(name)) {
            return recordJson!!.getJSONArray(name)
        }
        return null
    }

    fun getCurRecord(): JSONArray? {
        var curRecord: JSONArray?
        runBlocking {
            mutex.withLock {
                curRecord = JSON.parseArray(record?.toJSONString())
            }
        }
        return curRecord
    }

    fun getRecordName(): String? {
        var curRecordName: String?
        runBlocking {
            mutex.withLock {
                curRecordName = recordName
            }
        }
        return curRecordName
    }

    fun setRecordName(name: String) {
        runBlocking {
            mutex.withLock {
                if (recordFile == null) {
                    return@withLock
                }
                recordName = name
                val recordJson = JSON.parseObject(recordFile!!.readText(Charsets.UTF_8))
                recordJson["curRecord"] = recordName
                recordJson[recordName] = record
                recordFile!!.writeText(recordJson.toJSONString(), Charsets.UTF_8)
            }
        }
    }
}
