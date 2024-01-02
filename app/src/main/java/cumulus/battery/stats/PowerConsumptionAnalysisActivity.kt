package cumulus.battery.stats

import android.content.pm.PackageManager
import android.content.res.Resources
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.appcompat.content.res.AppCompatResources
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
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import cumulus.battery.stats.objects.BatteryStatsRecorder
import cumulus.battery.stats.ui.theme.CumulusTheme
import cumulus.battery.stats.ui.theme.cumulusBlue
import cumulus.battery.stats.ui.theme.cumulusPurple
import cumulus.battery.stats.utils.BattStatsRecordAnalysis
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class PowerConsumptionAnalysisActivity : ComponentActivity() {
    private var screenOnCapacityCost: Int by mutableStateOf(0)
    private var screenOffCapacityCost: Int by mutableStateOf(0)
    private var screenOnAveragePower: Int by mutableStateOf(0)
    private var screenOffAveragePower: Int by mutableStateOf(0)
    private var screenOnDuration: Long by mutableStateOf(0)
    private var screenOffDuration: Long by mutableStateOf(0)
    private var appsDurationStateMap = mutableStateMapOf<String, Long>()
    private var appsMaxPowerStateMap = mutableStateMapOf<String, Int>()
    private var appsAveragePowerStateMap = mutableStateMapOf<String, Int>()
    private var appsMaxTemperatureStateMap = mutableStateMapOf<String, Int>()
    private var appsAverageTemperatureStateMap = mutableStateMapOf<String, Int>()

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
                                    text = "耗电分析",
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
                                .padding(top = 10.dp),
                            verticalArrangement = Arrangement.Top,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            PowerConsumptionBasicInfoBar()
                            Text(
                                modifier = Modifier
                                    .padding(start = 20.dp, top = 20.dp)
                                    .fillMaxWidth(),
                                text = "应用使用详情",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = cumulusPurple
                            )
                            AppDetailsList()
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
    private fun PowerConsumptionBasicInfoBar() {
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
                        text = "亮屏耗电",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Text(
                        modifier = Modifier.padding(top = 2.dp),
                        text = "${screenOnCapacityCost}%",
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
                        text = "亮屏平均功耗",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Text(
                        modifier = Modifier.padding(top = 2.dp),
                        text = "${screenOnAveragePower}mW",
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
                    var screenOnDurationText: String
                    if (screenOnDuration > 60) {
                        screenOnDurationText = ""
                        val hour = screenOnDuration / 3600
                        val minute = (screenOnDuration / 60) % 60
                        if (hour > 0) {
                            screenOnDurationText += "${hour}时"
                        }
                        if (minute > 0) {
                            screenOnDurationText += "${minute}分"
                        }
                    } else {
                        screenOnDurationText = "小于1分"
                    }
                    Text(
                        text = "亮屏时长",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Text(
                        modifier = Modifier.padding(top = 2.dp),
                        text = screenOnDurationText,
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
                    Text(
                        text = "熄屏耗电",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Text(
                        modifier = Modifier.padding(top = 2.dp),
                        text = "${screenOffCapacityCost}%",
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
                        text = "熄屏平均功耗",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Text(
                        modifier = Modifier.padding(top = 2.dp),
                        text = "${screenOffAveragePower}mW",
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
                    var screenOffDurationText: String
                    if (screenOffDuration > 60) {
                        screenOffDurationText = ""
                        val hour = screenOffDuration / 3600
                        val minute = (screenOffDuration / 60) % 60
                        if (hour > 0) {
                            screenOffDurationText += "${hour}时"
                        }
                        if (minute > 0) {
                            screenOffDurationText += "${minute}分"
                        }
                    } else {
                        screenOffDurationText = "小于1分"
                    }
                    Text(
                        text = "熄屏时长",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Text(
                        modifier = Modifier.padding(top = 2.dp),
                        text = screenOffDurationText,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = cumulusBlue
                    )
                }
            }
        }
    }

    @Composable
    private fun AppDetailsList() {
        Column(
            modifier = Modifier
                .padding(top = 5.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            for ((pkgName, duration) in appsDurationStateMap.entries) {
                var appName = "unknown"
                var appIcon = AppCompatResources.getDrawable(applicationContext, R.drawable.icon_app_unknown)!!
                try {
                    val appInfo = packageManager.getApplicationInfo(pkgName, PackageManager.GET_META_DATA)
                    appName = packageManager.getApplicationLabel(appInfo).toString()
                    appIcon = packageManager.getApplicationIcon(appInfo)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                var durationText: String
                if (duration >= 60) {
                    durationText = ""
                    val hour = duration / 3600
                    val minute = (duration / 60) % 60
                    if (hour > 0) {
                        durationText += "${hour}时"
                    }
                    if (minute > 0) {
                        durationText += "${minute}分"
                    }
                } else {
                    durationText = "小于1分钟"
                }
                val maxPower = appsMaxPowerStateMap[pkgName]!!
                val averagePower = appsAveragePowerStateMap[pkgName]!!
                val maxTemperature = appsMaxTemperatureStateMap[pkgName]!!
                val averageTemperature = appsAverageTemperatureStateMap[pkgName]!!
                Row(
                    modifier = Modifier
                        .padding(top = 5.dp)
                        .fillMaxWidth()
                        .height(80.dp)
                        .background(
                            color = MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(10.dp)
                        ),
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        bitmap = appIcon.toBitmap().asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier
                            .padding(start = 20.dp)
                            .height(40.dp)
                            .width(40.dp)
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(start = 20.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.Start
                    ) {
                        Text(
                            text = "${appName} | 已使用 ${durationText}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1
                        )
                        Text(
                            modifier = Modifier.padding(top = 2.dp),
                            text = "功耗: 最高 ${maxPower}mW, 平均 ${averagePower} mW",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1
                        )
                        Text(
                            modifier = Modifier.padding(top = 2.dp),
                            text = "温度: 最高 ${maxTemperature} °C, 平均 ${averageTemperature} °C",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }

    private fun UpdateRecordAnalysis() {
        CoroutineScope(Dispatchers.Main).launch {
            val recordAnalysis = BattStatsRecordAnalysis(applicationContext, BatteryStatsRecorder.getCurRecord())
            recordAnalysis.doAnalysis()
            screenOnCapacityCost = recordAnalysis.getScreenOnCapacityCost()
            screenOffCapacityCost = recordAnalysis.getScreenOffCapacityCost()
            screenOnAveragePower = recordAnalysis.getScreenOnAveragePower()
            screenOffAveragePower = recordAnalysis.getScreenOffAveragePower()
            screenOnDuration = recordAnalysis.getScreenOnDuration()
            screenOffDuration = recordAnalysis.getScreenOffDuration()
            val appsDurationMap = recordAnalysis.getAppsDurationMap()
            for ((pkgName, duration) in appsDurationMap.entries) {
                appsDurationStateMap[pkgName] = duration
            }
            val appsMaxPowerMap = recordAnalysis.getAppsMaxPowerMap()
            for ((pkgName, maxPower) in appsMaxPowerMap.entries) {
                appsMaxPowerStateMap[pkgName] = maxPower
            }
            val appsAveragePowerMap = recordAnalysis.getAppsAveragePowerMap()
            for ((pkgName, averagePower) in appsAveragePowerMap.entries) {
                appsAveragePowerStateMap[pkgName] = averagePower
            }
            val appsMaxTemperatureMap = recordAnalysis.getAppsMaxTemperatureMap()
            for ((pkgName, maxTemperature) in appsMaxTemperatureMap.entries) {
                appsMaxTemperatureStateMap[pkgName] = maxTemperature
            }
            val appsAverageTemperatureMap = recordAnalysis.getAppsAverageTemperatureMap()
            for ((pkgName, averageTemperature) in appsAverageTemperatureMap.entries) {
                appsAverageTemperatureStateMap[pkgName] = averageTemperature
            }
        }
    }
}