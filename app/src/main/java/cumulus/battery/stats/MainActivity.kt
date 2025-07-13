package cumulus.battery.stats

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.content.Intent
import android.content.res.Resources
import android.os.BatteryManager
import android.os.Bundle
import android.provider.Settings
import android.view.KeyEvent
import android.widget.Toast
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import cumulus.battery.stats.objects.BatteryStatsProvider
import cumulus.battery.stats.objects.BatteryStatsRecorder
import cumulus.battery.stats.ui.theme.CumulusTheme
import cumulus.battery.stats.ui.theme.cumulusColor
import cumulus.battery.stats.utils.BatteryStatsRecordAnalysis
import cumulus.battery.stats.utils.DurationToText
import cumulus.battery.stats.widgets.DataPointList
import cumulus.battery.stats.widgets.GoToButton
import cumulus.battery.stats.widgets.SingleLineChart
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Timer
import java.util.TimerTask

class MainActivity : ComponentActivity() {
    private var timer: Timer? = null
    private var backgroundServiceCreated by mutableStateOf(false)
    private var currentAdjusted by mutableStateOf(false)
    private var batteryCapacity: Int by mutableIntStateOf(0)
    private var batteryCurrent: Int by mutableIntStateOf(0)
    private var batteryPower: Int by mutableIntStateOf(0)
    private var batteryTemperature: Int by mutableIntStateOf(0)
    private var batteryStatus: Int by mutableIntStateOf(BatteryManager.BATTERY_STATUS_UNKNOWN)
    private var batteryPercentageDataPoints: DataPointList by mutableStateOf(listOf())
    private var screenOnDuration: Long by mutableLongStateOf(0)
    private var remainingUsageTime: Long by mutableLongStateOf(0)

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
                                    painter = painterResource(id = R.mipmap.icon_round),
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
                                .padding(
                                    top = it.calculateTopPadding() + 20.dp,
                                    start = 20.dp,
                                    end = 20.dp
                                )
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.Top,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            BatteryStatsBar()
                            BackgroundServiceHint()
                            AdjustCurrentHint()
                            BatteryBasicInfoBar()
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
        startTimer()
        updateRecordAnalysis()
    }

    override fun onStop() {
        stopTimer()
        super.onStop()
    }

    override fun onDestroy() {
        BatteryStatsRecorder.optimize()
        super.onDestroy()
    }

    @SuppressLint("ServiceCast")
    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        if (event.keyCode == KeyEvent.KEYCODE_BACK) {
            try {
                val activityManager = getSystemService(ACTIVITY_SERVICE) as ActivityManager
                val tasks = activityManager.appTasks.filterNotNull()
                tasks.forEach { task ->
                    task.setExcludeFromRecents(true)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            finish()
            return false
        }
        return super.onKeyUp(keyCode, event)
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

        Row(
            modifier = Modifier
                .height(80.dp)
                .fillMaxWidth()
                .background(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(10.dp)
                ),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                modifier = Modifier.padding(start = 20.dp),
                text = "${batteryCapacity}%",
                fontSize = 40.sp,
                fontWeight = FontWeight.Bold,
                color = cumulusColor().blue,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
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
                    color = cumulusColor().blue,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${batteryPower} mW (${batteryCurrent} mA) ${batteryTemperature} °C",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.secondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }

    @Composable
    private fun BackgroundServiceHint() {
        AnimatedVisibility(visible = !backgroundServiceCreated) {
            val buttonColor = cumulusColor().yellow
            TextButton(
                onClick = {
                    Toast.makeText(
                        applicationContext,
                        "打开Cumulus无障碍以启用后台服务",
                        Toast.LENGTH_LONG
                    ).show()
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
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }

    @Composable
    private fun AdjustCurrentHint() {
        AnimatedVisibility(visible = !currentAdjusted) {
            val buttonColor = cumulusColor().yellow
            TextButton(
                onClick = {
                    val intent =
                        Intent(applicationContext, CurrentAdjustActivity::class.java)
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
                        text = "请确保电流值显示正确, 点击此处调整",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }

    @Composable
    private fun BatteryBasicInfoBar() {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(190.dp)
                .padding(top = 10.dp)
                .background(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surface
                ),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.Start
        ) {
            SingleLineChart(
                dataPointList = batteryPercentageDataPoints,
                modifier = Modifier
                    .padding(start = 20.dp, end = 20.dp, top = 20.dp)
                    .height(120.dp)
                    .fillMaxWidth(),
                lineColor = cumulusColor().blue
            )
            Row(
                modifier = Modifier
                    .padding(start = 20.dp, top = 10.dp)
                    .fillMaxWidth()
                    .height(20.dp),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "屏幕使用",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Normal,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = DurationToText(screenOnDuration),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = cumulusColor().blue,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = ", 预计可用",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Normal,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = DurationToText(remainingUsageTime),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = cumulusColor().blue,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }

    @Composable
    private fun PowerConsumptionAnalysisButton() {
        GoToButton(
            modifier = Modifier
                .padding(top = 10.dp)
                .height(50.dp)
                .fillMaxWidth(),
            icon = AppCompatResources.getDrawable(applicationContext, R.drawable.analysis),
            text = "耗电分析"
        ) {
            val intent =
                Intent(applicationContext, PowerConsumptionAnalysisActivity::class.java)
            startActivity(intent)
        }
    }

    @Composable
    private fun ChargingProcessButton() {
        GoToButton(
            modifier = Modifier
                .padding(top = 5.dp)
                .height(50.dp)
                .fillMaxWidth(),
            icon = AppCompatResources.getDrawable(applicationContext, R.drawable.charging),
            text = "充电过程"
        ) {
            val intent = Intent(applicationContext, ChargingProcessActivity::class.java)
            startActivity(intent)
        }
    }

    @Composable
    private fun AdditionalFunctionButton() {
        GoToButton(
            modifier = Modifier
                .padding(top = 5.dp)
                .height(50.dp)
                .fillMaxWidth(),
            icon = AppCompatResources.getDrawable(applicationContext, R.drawable.apps),
            text = "附加功能"
        ) {
            val intent = Intent(applicationContext, AdditionalFunctionActivity::class.java)
            startActivity(intent)
        }
    }

    @Composable
    private fun SettingsButton() {
        GoToButton(
            modifier = Modifier
                .padding(top = 5.dp)
                .height(50.dp)
                .fillMaxWidth(),
            icon = AppCompatResources.getDrawable(applicationContext, R.drawable.settings),
            text = "设置"
        ) {
            val intent = Intent(applicationContext, SettingsActivity::class.java)
            startActivity(intent)
        }
    }

    private fun updateRecordAnalysis() {
        CoroutineScope(Dispatchers.Default).launch {
            val records = BatteryStatsRecorder.getRecords()
            val recordAnalysis = BatteryStatsRecordAnalysis(records)
            batteryPercentageDataPoints = recordAnalysis.getUsagePercentageDataPoints()
            screenOnDuration = recordAnalysis.getScreenOnDuration()
            remainingUsageTime = recordAnalysis.getRemainingUsageTime()
        }
    }

    private fun updateBatteryStats() {
        backgroundServiceCreated = BackgroundService.isServiceCreated()
        currentAdjusted = BatteryStatsProvider.isCurrentAdjusted()
        batteryCapacity = BatteryStatsProvider.getBatteryCapacity(applicationContext)
        batteryCurrent = BatteryStatsProvider.getBatteryCurrent(applicationContext)
        batteryPower =
            BatteryStatsProvider.getBatteryVoltage(applicationContext) * batteryCurrent / 1000
        batteryTemperature = BatteryStatsProvider.getBatteryTemperature(applicationContext)
        batteryStatus = BatteryStatsProvider.getBatteryStatus(applicationContext)
    }

    private fun startTimer() {
        if (timer != null) {
            return
        }
        timer = Timer()
        timer!!.schedule(object : TimerTask() {
            override fun run() {
                updateBatteryStats()
            }
        }, 0, 1000)
    }

    private fun stopTimer() {
        if (timer == null) {
            return
        }
        timer!!.cancel()
        timer = null
    }
}
