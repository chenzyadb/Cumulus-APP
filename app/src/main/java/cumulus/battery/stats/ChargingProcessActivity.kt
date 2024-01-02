package cumulus.battery.stats

import android.content.res.Resources
import android.os.BatteryManager
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alibaba.fastjson2.JSONArray
import cumulus.battery.stats.charts.MultiLineChart
import cumulus.battery.stats.charts.SingleLineChart
import cumulus.battery.stats.objects.BatteryStatsProvider
import cumulus.battery.stats.objects.BatteryStatsRecorder
import cumulus.battery.stats.ui.theme.CumulusTheme
import cumulus.battery.stats.ui.theme.cumulusBlue
import cumulus.battery.stats.ui.theme.cumulusPink
import cumulus.battery.stats.ui.theme.cumulusPurple
import cumulus.battery.stats.utils.BattStatsRecordAnalysis
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ChargingProcessActivity : ComponentActivity() {
    private var chargingDuration: Long by mutableStateOf(0)
    private var chargingCapacity: Int by mutableStateOf(0)
    private var chargingMaxPower: Int by mutableStateOf(0)
    private var chargingAveragePower: Int by mutableStateOf(0)
    private var chargingMaxTemperature: Int by mutableStateOf(0)
    private var chargingAverageTemperature: Int by mutableStateOf(0)
    private var chargingCapacityArray: IntArray by mutableStateOf(IntArray(0))
    private var chargingPowerArray: IntArray by mutableStateOf(IntArray(0))
    private var chargingTemperatureArray: IntArray by mutableStateOf(IntArray(0))
    private var estimatingBatteryCapacity: Int by mutableStateOf(0)

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
                                .padding(top = it.calculateTopPadding() + 10.dp, start = 20.dp, end = 20.dp)
                                .fillMaxSize()
                                .padding(top = 10.dp)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.Top,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            ChargingBasicInfoBar()
                            ChargingCapacityChart()
                            ChargingPowerTemperatureChart()
                            EstimatingBatteryCapacityBar()
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
        UpdateRecordAnalysis()
    }

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
                    var chargingDurationText: String
                    if (chargingDuration > 60) {
                        chargingDurationText = ""
                        val hour = chargingDuration / 3600
                        val minute = (chargingDuration / 60) % 60
                        if (hour > 0) {
                            chargingDurationText += "${hour}时"
                        }
                        if (minute > 0) {
                            chargingDurationText += "${minute}分"
                        }
                    } else {
                        chargingDurationText = "0"
                    }
                    Text(
                        text = "充电时长",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Text(
                        modifier = Modifier.padding(top = 2.dp),
                        text = chargingDurationText,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = cumulusBlue
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
                        modifier = Modifier.padding(top = 2.dp),
                        text = "${chargingCapacity}%",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = cumulusBlue
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
                        modifier = Modifier.padding(top = 2.dp),
                        text = "${powerText}W",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = cumulusBlue
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
                        modifier = Modifier.padding(top = 2.dp),
                        text = "${powerText}W",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = cumulusBlue
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
                        modifier = Modifier.padding(top = 2.dp),
                        text = "${chargingMaxTemperature}°C",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = cumulusBlue
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
                        modifier = Modifier.padding(top = 2.dp),
                        text = "${chargingAverageTemperature}°C",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = cumulusBlue
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
                .height(170.dp)
                .background(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(10.dp)
                ),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                modifier = Modifier.padding(start = 20.dp, top = 10.dp),
                text = "电量变化曲线",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = cumulusPurple
            )
            SingleLineChart(
                modifier = Modifier
                    .padding(start = 20.dp, end = 20.dp, top = 10.dp)
                    .fillMaxWidth()
                    .height(120.dp),
                lineDataArray = chargingCapacityArray,
                tickMax = 100,
                lineColor = cumulusBlue
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
            var tick0Max = 1000
            if (chargingPowerArray.size > 0) {
                tick0Max = chargingPowerArray.max() / 100 * 100 + 100
            }
            var tick1Max = 100
            if (chargingTemperatureArray.size > 0) {
                tick1Max = chargingTemperatureArray.max() / 10 * 10 + 10
            }
            Text(
                modifier = Modifier.padding(start = 20.dp, top = 10.dp),
                text = "功率/温度变化曲线",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = cumulusPurple
            )
            MultiLineChart(
                modifier = Modifier
                    .padding(start = 20.dp, end = 20.dp, top = 10.dp)
                    .fillMaxWidth()
                    .height(150.dp),
                line0DataArray = chargingPowerArray,
                line1DataArray = chargingTemperatureArray,
                tick0Max = tick0Max,
                tick1Max = tick1Max,
                line0Color = cumulusBlue,
                line1Color = cumulusPink,
                line0Title = "功率(mW)",
                line1Title = "温度(°C)"
            )
        }
    }

    @Composable
    private fun EstimatingBatteryCapacityBar() {
        Row(
            modifier = Modifier
                .padding(top = 10.dp)
                .fillMaxWidth()
                .height(50.dp)
                .background(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(10.dp)
                ),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(id = R.drawable.usage),
                contentDescription = null,
                modifier = Modifier
                    .padding(start = 20.dp)
                    .height(24.dp)
                    .width(24.dp)
            )
            Text(
                modifier = Modifier.padding(start = 5.dp),
                text = "基于此次充电预估的电池容量:",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1
            )
            Text(
                modifier = Modifier.padding(start = 5.dp),
                text = "${estimatingBatteryCapacity} mAH",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = cumulusBlue,
                maxLines = 1
            )
        }
    }

    private fun UpdateRecordAnalysis() {
        CoroutineScope(Dispatchers.Main).launch {
            val record: JSONArray?
            if (BatteryStatsProvider.getBatteryStatus(applicationContext) == BatteryManager.BATTERY_STATUS_CHARGING) {
                record = BatteryStatsRecorder.getCurRecord()
            } else {
                record = BatteryStatsRecorder.getRecord("prevRecord")
            }
            if (record != null) {
                val recordAnalysis = BattStatsRecordAnalysis(applicationContext, record)
                recordAnalysis.doAnalysis()
                chargingDuration = recordAnalysis.getChargingDuration()
                chargingCapacity = recordAnalysis.getChargingCapacity()
                chargingMaxPower = recordAnalysis.getChargingMaxPower()
                chargingAveragePower = recordAnalysis.getChargingAveragePower()
                chargingMaxTemperature = recordAnalysis.getChargingMaxTemperature()
                chargingAverageTemperature = recordAnalysis.getChargingAverageTemperature()
                chargingCapacityArray = recordAnalysis.getChargingCapacityArray()
                chargingPowerArray = recordAnalysis.getChargingPowerArray()
                chargingTemperatureArray = recordAnalysis.getChargingTemperatureArray()
                estimatingBatteryCapacity = recordAnalysis.getEstimatingBatteryCapacity()
            }
        }
    }
}