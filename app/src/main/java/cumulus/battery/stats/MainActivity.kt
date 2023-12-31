package cumulus.battery.stats

import android.content.Intent
import android.content.res.Resources
import android.os.BatteryManager
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cumulus.battery.stats.charts.SingleLineChart
import cumulus.battery.stats.objects.BatteryStatsProvider
import cumulus.battery.stats.objects.BatteryStatsRecorder
import cumulus.battery.stats.ui.theme.CumulusTheme
import cumulus.battery.stats.ui.theme.cumulusBlue
import cumulus.battery.stats.ui.theme.cumulusPurple
import cumulus.battery.stats.ui.theme.cumulusYellow
import cumulus.battery.stats.utils.BattStatsRecordAnalysis
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*

class MainActivity : ComponentActivity() {
    private var refreshTimer: Timer? = null
    private var backgroundServiceCreated by mutableStateOf(false)
    private var batteryCapacity: Int by mutableStateOf(0)
    private var batteryCurrent: Int by mutableStateOf(0)
    private var batteryPower: Int by mutableStateOf(0)
    private var batteryTemperature: Int by mutableStateOf(0)
    private var batteryStatus: Int by mutableStateOf(BatteryManager.BATTERY_STATUS_UNKNOWN)
    private var lastChargingCapacity: Int by mutableStateOf(0)
    private var recordDuration: Long by mutableStateOf(0)
    private var batteryCapacityArray: IntArray by mutableStateOf(IntArray(0))
    private var screenOnDuration: Long by mutableStateOf(0)
    private var remainingUsageTime: Long by mutableStateOf(0)

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        BatteryStatsProvider.init(applicationContext)
        BatteryStatsRecorder.init(applicationContext)
        setContent {
            CumulusTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        topBar = {
                            Row(
                                modifier = Modifier
                                    .padding(top = 10.dp, start = 30.dp)
                                    .height(50.dp)
                                    .fillMaxWidth(),
                                horizontalArrangement = Arrangement.Start,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Image(
                                    painter = painterResource(id = R.mipmap.ic_launcher_round),
                                    contentDescription = "icon",
                                    modifier = Modifier
                                        .height(40.dp)
                                        .width(40.dp)
                                )
                                Text(
                                    modifier = Modifier
                                        .padding(start = 20.dp),
                                    text = "Cumulus",
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(top = it.calculateTopPadding() + 20.dp, start = 20.dp, end = 20.dp)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.Top,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            BatteryStatsBar()
                            BackgroundServiceHint()
                            CurRecordBasicInfoBar()
                            PowerConsumptionAnalysisButton()
                            ChargingProcessButton()
                            AdditionalFunctionButton()
                            SettingsButton()
                        }
                    }
                }
            }
        }
    }

    override fun getResources(): Resources {
        val resources = super.getResources()
        val configContext = createConfigurationContext(resources.configuration)
        return configContext.resources.apply {
            configuration.fontScale = 1.0f
        }
    }

    override fun onStart() {
        super.onStart()
        StartRefleshTimer()
        UpdateRecordAnalysis()
    }

    override fun onStop() {
        super.onStop()
        StopRefleshTimer()
    }

    @Composable
    private fun BatteryStatsBar() {
        val statusString = hashMapOf(
            BatteryManager.BATTERY_STATUS_FULL to "已充满电",
            BatteryManager.BATTERY_STATUS_CHARGING to "充电中",
            BatteryManager.BATTERY_STATUS_DISCHARGING to "放电中",
            BatteryManager.BATTERY_STATUS_NOT_CHARGING to "未在充电",
            BatteryManager.BATTERY_STATUS_UNKNOWN to "未知状态"
        )
        var showCurrentAdjustBar by remember { mutableStateOf(false) }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(10.dp)
                ),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            TextButton(
                onClick = { showCurrentAdjustBar = !showCurrentAdjustBar },
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(0.dp),
                modifier = Modifier
                    .height(80.dp)
                    .fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .padding(start = 20.dp)
                        .fillMaxSize(),
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${batteryCapacity}%",
                        fontSize = 40.sp,
                        fontWeight = FontWeight.Bold,
                        color = cumulusBlue,
                        maxLines = 1,
                    )
                    Column(
                        modifier = Modifier
                            .padding(start = 20.dp)
                            .fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.Start
                    ) {
                        Text(
                            text = statusString[batteryStatus]!!,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = cumulusBlue,
                            maxLines = 1
                        )
                        Text(
                            text = "${batteryPower} mW (${batteryCurrent} mA) ${batteryTemperature} °C",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.secondary,
                            maxLines = 1
                        )
                    }
                }
            }

            AnimatedVisibility(visible = showCurrentAdjustBar) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .padding(start = 20.dp, end = 20.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.Top,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val switchColors = SwitchDefaults.colors(
                        checkedThumbColor = cumulusPurple,
                        checkedTrackColor = MaterialTheme.colorScheme.tertiary,
                        checkedBorderColor = MaterialTheme.colorScheme.outline,
                        uncheckedThumbColor = MaterialTheme.colorScheme.secondary,
                        uncheckedTrackColor = MaterialTheme.colorScheme.surface,
                        uncheckedBorderColor = MaterialTheme.colorScheme.outline
                    )
                    Text(
                        modifier = Modifier
                            .padding(top = 5.dp, bottom = 5.dp)
                            .height(20.dp)
                            .fillMaxWidth(),
                        text = "电流显示调整",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = cumulusPurple,
                        maxLines = 1
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp),
                        horizontalArrangement = Arrangement.Start,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.current),
                            contentDescription = null,
                            modifier = Modifier
                                .height(24.dp)
                                .width(24.dp)
                        )
                        Text(
                            modifier = Modifier.padding(start = 5.dp),
                            text = "电流反转",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1
                        )
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            var currentReverse by remember { mutableStateOf(BatteryStatsProvider.getCurrentReverse()) }
                            Switch(
                                modifier = Modifier
                                    .height(20.dp),
                                colors = switchColors,
                                checked = currentReverse,
                                onCheckedChange = {
                                    currentReverse = it
                                    BatteryStatsProvider.setCurrentReverse(currentReverse)
                                }
                            )
                        }
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp),
                        horizontalArrangement = Arrangement.Start,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.swap),
                            contentDescription = null,
                            modifier = Modifier
                                .height(24.dp)
                                .width(24.dp)
                        )
                        Text(
                            modifier = Modifier.padding(start = 5.dp),
                            text = "uA-mA单位切换",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1
                        )
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            var currentUnitUA by remember { mutableStateOf(BatteryStatsProvider.getCurrentUnitUA()) }
                            Switch(
                                modifier = Modifier
                                    .height(20.dp),
                                colors = switchColors,
                                checked = currentUnitUA,
                                onCheckedChange = {
                                    currentUnitUA = it
                                    BatteryStatsProvider.setCurrentUnitUA(currentUnitUA)
                                }
                            )
                        }
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp),
                        horizontalArrangement = Arrangement.Start,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.device),
                            contentDescription = null,
                            modifier = Modifier
                                .height(24.dp)
                                .width(24.dp)
                        )
                        Text(
                            modifier = Modifier.padding(start = 5.dp),
                            text = "双电芯设备",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1
                        )
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            var dualBattery by remember { mutableStateOf(BatteryStatsProvider.getDualBattery()) }
                            Switch(
                                modifier = Modifier
                                    .height(20.dp),
                                colors = switchColors,
                                checked = dualBattery,
                                onCheckedChange = {
                                    dualBattery = it
                                    BatteryStatsProvider.setDualBattery(dualBattery)
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun BackgroundServiceHint() {
        AnimatedVisibility(visible = !backgroundServiceCreated) {
            val buttonColor = cumulusYellow.copy(alpha = 0.5f)
            TextButton(
                onClick = {
                    Toast.makeText(applicationContext, "打开Cumulus无障碍以启用后台服务", Toast.LENGTH_LONG).show()
                    val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                    startActivity(intent)
                },
                contentPadding = PaddingValues(0.dp),
                modifier = Modifier
                    .padding(top = 10.dp)
                    .height(50.dp)
                    .fillMaxWidth()
                    .background(
                        color = buttonColor,
                        shape = RoundedCornerShape(10.dp)
                    ),
                shape = RoundedCornerShape(10.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(start = 20.dp, end = 20.dp),
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.warning),
                        contentDescription = null,
                        modifier = Modifier
                            .height(24.dp)
                            .width(24.dp)
                    )
                    Text(
                        modifier = Modifier.padding(start = 5.dp),
                        text = "后台服务未运行, 点击此处启用",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1
                    )
                }
            }
        }
    }

    @Composable
    private fun CurRecordBasicInfoBar() {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(210.dp)
                .padding(top = 10.dp)
                .background(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surface
                ),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            var durationText: String
            if (recordDuration > 60) {
                durationText = "已使用"
                val day = recordDuration / 3600 / 24
                val hour = (recordDuration / 3600) % 24
                val minute = (recordDuration / 60) % 60
                if (day > 0) {
                    durationText += "${day}天"
                }
                if (hour > 0) {
                    durationText += "${hour}小时"
                }
                if (minute > 0) {
                    durationText += "${minute}分钟"
                }
            } else {
                durationText = "刚刚开始使用"
            }
            Text(
                modifier = Modifier
                    .padding(start = 20.dp, end = 20.dp, top = 10.dp)
                    .height(20.dp)
                    .fillMaxWidth(),
                text = "上次充电至 ${lastChargingCapacity}% , ${durationText}",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = cumulusBlue,
                maxLines = 1
            )
            SingleLineChart(
                lineDataArray = batteryCapacityArray,
                tickMax = 100,
                modifier = Modifier
                    .padding(start = 20.dp, end = 20.dp, top = 10.dp)
                    .height(120.dp)
                    .fillMaxWidth(),
                lineColor = cumulusBlue
            )
            var screenOnDurationText: String
            if (screenOnDuration > 60) {
                screenOnDurationText = "已亮屏 "
                val hour = screenOnDuration / 3600
                val minute = (screenOnDuration / 60) % 60
                if (hour > 0) {
                    screenOnDurationText += "${hour}小时"
                }
                if (minute > 0) {
                    screenOnDurationText += "${minute}分钟"
                }
            } else {
                screenOnDurationText = "暂未统计亮屏时间"
            }
            var remainingUsageTimeText: String
            if (remainingUsageTime > 60) {
                remainingUsageTimeText = "预计可用 "
                val hour = remainingUsageTime / 3600
                val minute = (remainingUsageTime / 60) % 60
                if (hour > 0) {
                    remainingUsageTimeText += "${hour}小时"
                }
                if (minute > 0) {
                    remainingUsageTimeText += "${minute}分钟"
                }
            } else {
                remainingUsageTimeText = "无法预测可用时间"
            }
            Text(
                modifier = Modifier
                    .padding(start = 20.dp, end = 20.dp, top = 10.dp)
                    .fillMaxWidth(),
                text = "${screenOnDurationText} , ${remainingUsageTimeText}",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1
            )
        }
    }

    @Composable
    private fun PowerConsumptionAnalysisButton() {
        TextButton(
            onClick = {
                val intent = Intent(applicationContext, PowerConsumptionAnalysisActivity::class.java)
                startActivity(intent)
            },
            shape = RoundedCornerShape(10.dp),
            contentPadding = PaddingValues(0.dp),
            modifier = Modifier
                .padding(top = 10.dp)
                .height(50.dp)
                .fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 20.dp, end = 20.dp),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(id = R.drawable.analysis),
                    contentDescription = null,
                    modifier = Modifier
                        .height(28.dp)
                        .width(28.dp)
                )
                Text(
                    modifier = Modifier.padding(start = 20.dp),
                    text = "耗电分析",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1
                )
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.arrow_forward),
                        contentDescription = null,
                        modifier = Modifier
                            .height(16.dp)
                            .width(16.dp)
                    )
                }
            }
        }
    }

    @Composable
    private fun ChargingProcessButton() {
        TextButton(
            onClick = {
                val intent = Intent(applicationContext, ChargingProcessActivity::class.java)
                startActivity(intent)
            },
            shape = RoundedCornerShape(10.dp),
            contentPadding = PaddingValues(0.dp),
            modifier = Modifier
                .padding(top = 5.dp)
                .height(50.dp)
                .fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 20.dp, end = 20.dp),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(id = R.drawable.charging),
                    contentDescription = null,
                    modifier = Modifier
                        .height(28.dp)
                        .width(28.dp)
                )
                Text(
                    modifier = Modifier.padding(start = 20.dp),
                    text = "充电过程",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1
                )
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.arrow_forward),
                        contentDescription = null,
                        modifier = Modifier
                            .height(16.dp)
                            .width(16.dp)
                    )
                }
            }
        }
    }

    @Composable
    private fun AdditionalFunctionButton() {
        TextButton(
            onClick = {
                val intent = Intent(applicationContext, AdditionalFunctionActivity::class.java)
                startActivity(intent)
            },
            shape = RoundedCornerShape(10.dp),
            contentPadding = PaddingValues(0.dp),
            modifier = Modifier
                .padding(top = 5.dp)
                .height(50.dp)
                .fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 20.dp, end = 20.dp),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(id = R.drawable.apps),
                    contentDescription = null,
                    modifier = Modifier
                        .height(28.dp)
                        .width(28.dp)
                )
                Text(
                    modifier = Modifier.padding(start = 20.dp),
                    text = "附加功能",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1
                )
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.arrow_forward),
                        contentDescription = null,
                        modifier = Modifier
                            .height(16.dp)
                            .width(16.dp)
                    )
                }
            }
        }
    }

    @Composable
    private fun SettingsButton() {
        TextButton(
            onClick = {
                val intent = Intent(applicationContext, SettingsActivity::class.java)
                startActivity(intent)
            },
            shape = RoundedCornerShape(10.dp),
            contentPadding = PaddingValues(0.dp),
            modifier = Modifier
                .padding(top = 5.dp)
                .height(50.dp)
                .fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 20.dp, end = 20.dp),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(id = R.drawable.settings),
                    contentDescription = null,
                    modifier = Modifier
                        .height(28.dp)
                        .width(28.dp)
                )
                Text(
                    modifier = Modifier.padding(start = 20.dp),
                    text = "设置",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1
                )
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.arrow_forward),
                        contentDescription = null,
                        modifier = Modifier
                            .height(16.dp)
                            .width(16.dp)
                    )
                }
            }
        }
    }

    private fun UpdateRecordAnalysis() {
        CoroutineScope(Dispatchers.Main).launch {
            val record = BatteryStatsRecorder.getCurRecord()
            val recordAnalysis = BattStatsRecordAnalysis(applicationContext, record)
            recordAnalysis.doAnalysis()
            lastChargingCapacity = recordAnalysis.getlastChargingCapacity()
            recordDuration = recordAnalysis.getRecordDuration()
            batteryCapacityArray = recordAnalysis.getBatteryCapacityArray()
            screenOnDuration = recordAnalysis.getScreenOnDuration()
            remainingUsageTime = recordAnalysis.getRemainingUsageTime()
        }
    }

    private fun UpdateBatteryStats() {
        backgroundServiceCreated = BackgroundService.isServiceCreated()
        batteryCapacity = BatteryStatsProvider.getBatteryCapacity(applicationContext)
        batteryCurrent = BatteryStatsProvider.getBatteryCurrent(applicationContext)
        batteryPower = BatteryStatsProvider.getBatteryVoltage(applicationContext) * batteryCurrent / 1000
        batteryTemperature = BatteryStatsProvider.getBatteryTemperature(applicationContext)
        batteryStatus = BatteryStatsProvider.getBatteryStatus(applicationContext)
    }

    private fun StartRefleshTimer() {
        if (refreshTimer != null) {
            return
        }
        refreshTimer = Timer()
        refreshTimer!!.schedule(object : TimerTask() {
            override fun run() {
                UpdateBatteryStats()
            }
        }, 0, 1000)
    }

    private fun StopRefleshTimer() {
        if (refreshTimer == null) {
            return
        }
        refreshTimer!!.cancel()
        refreshTimer = null
    }
}
