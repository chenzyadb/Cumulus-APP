package cumulus.battery.stats

import android.annotation.SuppressLint
import android.content.res.Resources
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cumulus.battery.stats.charts.MultiLineChart
import cumulus.battery.stats.charts.SingleLineChart
import cumulus.battery.stats.objects.BatteryStatsProvider
import cumulus.battery.stats.objects.BatteryStatsRecorder
import cumulus.battery.stats.ui.theme.CumulusTheme
import cumulus.battery.stats.ui.theme.cumulusColor
import cumulus.battery.stats.utils.BattStatsRecordAnalysis
import cumulus.battery.stats.utils.BatteryHealthReport
import cumulus.battery.stats.utils.DurationToText
import cumulus.battery.stats.utils.SimplifyDataPoints
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ChargingProcessActivity : ComponentActivity() {
    private var chargingPercentageArray: IntArray by mutableStateOf(IntArray(0))
    private var chargingPowerArray: IntArray by mutableStateOf(IntArray(0))
    private var chargingTemperatureArray: IntArray by mutableStateOf(IntArray(0))
    private var chargingDuration: Long by mutableLongStateOf(0)
    private var chargingPercentage: Int by mutableIntStateOf(0)
    private var chargingMaxPower: Int by mutableIntStateOf(0)
    private var chargingAveragePower: Int by mutableIntStateOf(0)
    private var chargingMaxTemperature: Int by mutableIntStateOf(0)
    private var chargingAverageTemperature: Int by mutableIntStateOf(0)
    private var batteryHealthReport: BatteryHealthReport by mutableStateOf(BatteryHealthReport())

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
                                    .padding(top = 10.dp, start = 10.dp, end = 10.dp)
                                    .height(50.dp)
                                    .fillMaxWidth(),
                                horizontalArrangement = Arrangement.Start,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TextButton(
                                    onClick = { finish() },
                                    modifier = Modifier
                                        .height(50.dp)
                                        .width(50.dp),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Image(
                                        painter = painterResource(id = R.drawable.arrow_back),
                                        contentDescription = null,
                                        modifier = Modifier
                                            .height(32.dp)
                                            .width(32.dp)
                                    )
                                }
                                Text(
                                    modifier = Modifier
                                        .padding(start = 10.dp),
                                    text = "充电过程",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(
                                    top = it.calculateTopPadding() + 10.dp,
                                    start = 20.dp,
                                    end = 20.dp
                                )
                                .fillMaxSize()
                                .padding(top = 10.dp)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.Top,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            ChargingBasicInfoBar()
                            ChargingCapacityChart()
                            ChargingPowerTemperatureChart()
                            BatteryHealthBar()
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
        updateRecordAnalysis()
    }

    @SuppressLint("DefaultLocale")
    @Composable
    private fun ChargingBasicInfoBar() {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .background(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(10.dp)
                ),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier
                        .width(100.dp)
                        .height(50.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "充电时长",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Text(
                        text = DurationToText(chargingDuration),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = cumulusColor().blue,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Column(
                    modifier = Modifier
                        .width(100.dp)
                        .height(50.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "充入电量",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Text(
                        text = "${chargingPercentage}%",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = cumulusColor().blue,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Column(
                    modifier = Modifier
                        .width(100.dp)
                        .height(50.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val powerText = String.format("%.2f", chargingMaxPower.toFloat() / 1000)
                    Text(
                        text = "最高功率",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Text(
                        text = "${powerText}W",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = cumulusColor().blue,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier
                        .width(100.dp)
                        .height(50.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val powerText = String.format("%.2f", chargingAveragePower.toFloat() / 1000)
                    Text(
                        text = "平均功率",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Text(
                        text = "${powerText}W",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = cumulusColor().blue,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Column(
                    modifier = Modifier
                        .width(100.dp)
                        .height(50.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "最高温度",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Text(
                        text = "${chargingMaxTemperature}°C",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = cumulusColor().blue,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Column(
                    modifier = Modifier
                        .width(100.dp)
                        .height(50.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "平均温度",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Text(
                        text = "${chargingAverageTemperature}°C",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = cumulusColor().blue,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }

    @Composable
    private fun ChargingCapacityChart() {
        Column(
            modifier = Modifier
                .padding(top = 10.dp)
                .fillMaxWidth()
                .height(180.dp)
                .background(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(10.dp)
                ),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                modifier = Modifier.padding(start = 20.dp, top = 10.dp),
                text = "电池充电曲线",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = cumulusColor().purple
            )
            SingleLineChart(
                modifier = Modifier
                    .padding(start = 20.dp, end = 20.dp, top = 10.dp)
                    .fillMaxWidth()
                    .height(120.dp),
                lineDataArray = SimplifyDataPoints(chargingPercentageArray),
                tickMax = 100,
                lineColor = cumulusColor().blue
            )
        }
    }

    @Composable
    private fun ChargingPowerTemperatureChart() {
        Column(
            modifier = Modifier
                .padding(top = 10.dp)
                .fillMaxWidth()
                .height(200.dp)
                .background(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(10.dp)
                ),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.Start
        ) {
            var tick0Max = 10
            if (chargingMaxPower > 0) {
                tick0Max = chargingMaxPower / 1000 / 10 * 10 + 10
            }
            var tick1Max = 50
            if (chargingMaxTemperature > 0) {
                tick1Max = chargingMaxTemperature / 10 * 10 + 10
            }
            val line0DataArray = chargingPowerArray.map { it / 1000 }.toTypedArray().toIntArray()
            Text(
                modifier = Modifier.padding(start = 20.dp, top = 10.dp),
                text = "功率/温度变化曲线",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = cumulusColor().purple
            )
            MultiLineChart(
                modifier = Modifier
                    .padding(start = 20.dp, end = 20.dp, top = 10.dp)
                    .fillMaxWidth()
                    .height(150.dp),
                line0DataArray = SimplifyDataPoints(line0DataArray),
                line1DataArray = SimplifyDataPoints(chargingTemperatureArray),
                tick0Max = tick0Max,
                tick1Max = tick1Max,
                line0Color = cumulusColor().blue,
                line1Color = cumulusColor().pink,
                line0Title = "功率(W)",
                line1Title = "温度(°C)"
            )
        }
    }

    @SuppressLint("DefaultLocale")
    @Composable
    private fun BatteryHealthBar() {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .padding(top = 10.dp)
                .background(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surface
                ),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                modifier = Modifier.padding(start = 20.dp, top = 10.dp),
                text = "电池健康数据",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = cumulusColor().purple
            )
            Row(
                modifier = Modifier
                    .padding(start = 20.dp, top = 10.dp)
                    .fillMaxWidth()
                    .height(25.dp),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "基于最近",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Normal,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = batteryHealthReport.sampleSize.toString(),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = cumulusColor().blue,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "次充电估算的电池健康信息: ",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Normal,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Row(
                modifier = Modifier
                    .padding(start = 20.dp)
                    .fillMaxWidth()
                    .height(25.dp),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "总共充入",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Normal,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = batteryHealthReport.totalChargedCapacity.toString(),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = cumulusColor().blue,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "mWh (",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Normal,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = batteryHealthReport.totalChargedPercentage.toString(),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = cumulusColor().blue,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "%) 电量",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Normal,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Row(
                modifier = Modifier
                    .padding(start = 20.dp)
                    .fillMaxWidth()
                    .height(25.dp),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "电池剩余容量",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Normal,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = batteryHealthReport.estimatingCapacity.toString(),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = cumulusColor().blue,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "mWh (",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Normal,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = (batteryHealthReport.estimatingCapacity.toDouble() / 3.85).toInt()
                        .toString(),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = cumulusColor().blue,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "mAh)",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Normal,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Row(
                modifier = Modifier
                    .padding(start = 20.dp)
                    .fillMaxWidth()
                    .height(25.dp),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val designCapacity =
                    BatteryStatsProvider.getBatteryDesignCapacity(applicationContext)
                val healthPercentage =
                    (batteryHealthReport.estimatingCapacity.toDouble() / 3.85) / designCapacity * 100.0
                Text(
                    text = "标称容量",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Normal,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = designCapacity.toString(),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = cumulusColor().blue,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "mAh, 健康度",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Normal,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = String.format("%.2f", healthPercentage),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = cumulusColor().blue,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "%",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Normal,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Row(
                modifier = Modifier
                    .padding(start = 20.dp, top = 10.dp)
                    .fillMaxWidth()
                    .height(25.dp),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "所有mAh数据均基于3.85V的标称电压, 可能存在误差.",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Normal,
                    color = MaterialTheme.colorScheme.secondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }

    private fun updateRecordAnalysis() {
        CoroutineScope(Dispatchers.Default).launch {
            val records = BatteryStatsRecorder.getRecords()
            val recordAnalysis = BattStatsRecordAnalysis(records)
            chargingPercentageArray = recordAnalysis.getLastChargingPercentageList().toIntArray()
            chargingPowerArray = recordAnalysis.getLastChargingPowerList().toIntArray()
            chargingTemperatureArray = recordAnalysis.getLastChargingTemperatureList().toIntArray()
            chargingDuration = recordAnalysis.getLastChargingDuration()
            if (chargingPercentageArray.isNotEmpty()) {
                chargingPercentage = chargingPercentageArray.max() - chargingPercentageArray.min()
            }
            if (chargingPowerArray.isNotEmpty()) {
                chargingMaxPower = chargingPowerArray.max()
                chargingAveragePower = chargingPowerArray.average().toInt()
            }
            if (chargingTemperatureArray.isNotEmpty()) {
                chargingMaxTemperature = chargingTemperatureArray.max()
                chargingAverageTemperature = chargingTemperatureArray.average().toInt()
            }
            batteryHealthReport = recordAnalysis.getBatteryHealthReport()
        }
    }
}