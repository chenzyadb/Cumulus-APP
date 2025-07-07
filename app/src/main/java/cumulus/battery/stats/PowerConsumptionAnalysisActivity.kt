package cumulus.battery.stats

import android.content.pm.PackageManager
import android.content.res.Resources
import android.graphics.drawable.Drawable
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.appcompat.content.res.AppCompatResources
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import androidx.core.graphics.drawable.toDrawable
import cumulus.battery.stats.charts.MultiLineChart
import cumulus.battery.stats.objects.BatteryStatsRecorder
import cumulus.battery.stats.ui.theme.CumulusTheme
import cumulus.battery.stats.ui.theme.cumulusColor
import cumulus.battery.stats.utils.BattStatsRecordAnalysis
import cumulus.battery.stats.utils.DurationToText
import cumulus.battery.stats.utils.SimplifyDataPoints
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class PowerConsumptionAnalysisActivity : ComponentActivity() {
    private var screenOnUsedPercentage: Int by mutableIntStateOf(0)
    private var screenOffUsedPercentage: Int by mutableIntStateOf(0)
    private var screenOnAveragePower: Int by mutableIntStateOf(0)
    private var screenOffAveragePower: Int by mutableIntStateOf(0)
    private var screenOnDuration: Long by mutableLongStateOf(0)
    private var screenOffDuration: Long by mutableLongStateOf(0)
    private var perappPowerList: Map<String, List<Int>> by mutableStateOf(mapOf())
    private var perappTemperatureList: Map<String, List<Int>> by mutableStateOf(mapOf())
    private var perappUsedTime: Map<String, Long> by mutableStateOf(mapOf())

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
                                .padding(
                                    top = it.calculateTopPadding() + 10.dp,
                                    start = 20.dp,
                                    end = 20.dp
                                )
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
                                color = cumulusColor().purple
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
        updateRecordAnalysis()
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
                        text = "${screenOnUsedPercentage}%",
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
                        text = "亮屏平均功耗",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Text(
                        text = "${screenOnAveragePower}mW",
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
                        text = "亮屏时长",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Text(
                        text = DurationToText(screenOnDuration),
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
                    Text(
                        text = "熄屏耗电",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Text(
                        text = "${screenOffUsedPercentage}%",
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
                        text = "熄屏平均功耗",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Text(
                        text = "${screenOffAveragePower}mW",
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
                        text = "熄屏时长",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Text(
                        text = DurationToText(screenOffDuration),
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
    private fun AppDetailsList() {
        Column(
            modifier = Modifier
                .padding(top = 5.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            for ((pkgName, usedTime) in perappUsedTime) {
                if (!pkgName.equals("standby") && !pkgName.equals("other") && usedTime > 0) {
                    AppDetailsBar(pkgName)
                }
            }
        }
    }

    @Composable
    private fun AppDetailsBar(pkgName: String) {
        val usedTime = perappUsedTime[pkgName]
        val appPowerList = perappPowerList[pkgName]
        val appTemperatureList = perappTemperatureList[pkgName]
        if (usedTime != null && !appPowerList.isNullOrEmpty() && !appTemperatureList.isNullOrEmpty()) {
            val maxPower = appPowerList.max()
            val averagePower = appPowerList.average().toInt()
            val maxTemperature = appTemperatureList.max()
            val averageTemperarure = appTemperatureList.average().toInt()
            val usedPercentage = (averagePower.toDouble() * usedTime) /
                    (screenOnAveragePower * screenOnDuration) * screenOnUsedPercentage

            Column(
                modifier = Modifier
                    .padding(top = 10.dp)
                    .fillMaxWidth()
                    .background(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(10.dp)
                    ),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                var showMoreDetails by remember { mutableStateOf(false) }
                TextButton(
                    onClick = { showMoreDetails = !showMoreDetails },
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(0.dp),
                    modifier = Modifier
                        .height(60.dp)
                        .fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .padding(start = 20.dp)
                            .fillMaxSize(),
                        horizontalArrangement = Arrangement.Start,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        var appIcon = getAppIcon(pkgName)
                        if (appIcon == null) {
                            appIcon = getUnknownAppIcon()
                        }
                        Image(
                            bitmap = appIcon.toBitmap().asImageBitmap(),
                            contentDescription = null,
                            modifier = Modifier
                                .height(40.dp)
                                .width(40.dp)
                        )
                        Column(
                            modifier = Modifier
                                .padding(start = 20.dp)
                                .fillMaxHeight()
                                .width(180.dp),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.Start
                        ) {
                            Text(
                                text = getAppName(pkgName),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(20.dp),
                                horizontalArrangement = Arrangement.Start,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "使用时间: ",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Normal,
                                    color = MaterialTheme.colorScheme.primary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = DurationToText(usedTime),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = cumulusColor().blue,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        Row(
                            modifier = Modifier
                                .padding(end = 20.dp)
                                .fillMaxSize(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            var usedPercentageText = "<1%"
                            if (usedPercentage >= 1.0) {
                                usedPercentageText = usedPercentage.toInt().toString() + "%"
                            }
                            Text(
                                text = usedPercentageText,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = cumulusColor().blue,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
                AnimatedVisibility(visible = showMoreDetails) {
                    Column(
                        modifier = Modifier
                            .padding(start = 20.dp, top = 10.dp, end = 20.dp)
                            .fillMaxWidth()
                            .height(190.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        val line0DataArray =
                            appPowerList.map { it / 1000 }.toTypedArray().toIntArray()
                        MultiLineChart(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp),
                            line0DataArray = SimplifyDataPoints(line0DataArray),
                            line1DataArray = SimplifyDataPoints(appTemperatureList.toIntArray()),
                            tick0Max = (maxPower / 1000 / 10 + 1) * 10,
                            tick1Max = (maxTemperature / 10 + 1) * 10,
                            line0Color = cumulusColor().blue,
                            line1Color = cumulusColor().pink,
                            line0Title = "功率(W)",
                            line1Title = "温度(°C)"
                        )
                        Row(
                            modifier = Modifier
                                .padding(top = 10.dp)
                                .fillMaxWidth()
                                .height(20.dp),
                            horizontalArrangement = Arrangement.Start,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "最高功耗: ",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Normal,
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = maxPower.toString(),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = cumulusColor().blue,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "mW, 平均功耗: ",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Normal,
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = averagePower.toString(),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = cumulusColor().blue,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "mW",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Normal,
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Row(
                            modifier = Modifier
                                .padding(top = 5.dp)
                                .fillMaxWidth()
                                .height(20.dp),
                            horizontalArrangement = Arrangement.Start,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "最高温度: ",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Normal,
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = maxTemperature.toString(),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = cumulusColor().blue,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "°C, 平均温度: ",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Normal,
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = averageTemperarure.toString(),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = cumulusColor().blue,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "°C",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Normal,
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }

    private fun updateRecordAnalysis() {
        CoroutineScope(Dispatchers.Default).launch {
            val records = BatteryStatsRecorder.getRecords()
            val recordAnalysis = BattStatsRecordAnalysis(records)
            screenOnUsedPercentage = recordAnalysis.getScreenOnUsedPercentage()
            screenOffUsedPercentage = recordAnalysis.getScreenOffUsedPercentage()
            screenOnAveragePower = recordAnalysis.getScreenOnAveragePower()
            screenOffAveragePower = recordAnalysis.getScreenOffAveragePower()
            screenOnDuration = recordAnalysis.getScreenOnDuration()
            screenOffDuration = recordAnalysis.getScreenOffDuration()
            perappPowerList = recordAnalysis.getPerappPowerList()
            perappTemperatureList = recordAnalysis.getPerappTemperatureList()
            perappUsedTime = recordAnalysis.getPerappUsedTime()
        }
    }

    private fun getAppName(pkgName: String): String {
        try {
            val appInfo =
                packageManager.getApplicationInfo(pkgName, PackageManager.GET_META_DATA)
            return packageManager.getApplicationLabel(appInfo).toString()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return "unknown"
    }

    private fun getAppIcon(pkgName: String): Drawable? {
        try {
            val appInfo =
                packageManager.getApplicationInfo(pkgName, PackageManager.GET_META_DATA)
            return packageManager.getApplicationIcon(appInfo)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    private fun getUnknownAppIcon(): Drawable {
        val icon = AppCompatResources.getDrawable(applicationContext, R.drawable.icon_app_unknown)
        if (icon != null) {
            return icon
        }
        return Color.White.toArgb().toDrawable()
    }
}