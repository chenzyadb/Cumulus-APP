package cumulus.battery.stats

import android.content.res.Resources
import android.os.BatteryManager
import android.os.Bundle
import android.widget.Toast
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import cumulus.battery.stats.ui.theme.CumulusTheme
import cumulus.battery.stats.ui.theme.cumulusColor
import cumulus.battery.stats.widgets.GoToButton
import cumulus.battery.stats.widgets.Switch
import java.util.Timer
import java.util.TimerTask

class CurrentAdjustActivity : ComponentActivity() {
    private var timer: Timer? = null
    private var batteryCurrent by mutableIntStateOf(0)
    private var batteryStatus by mutableIntStateOf(BatteryManager.BATTERY_STATUS_UNKNOWN)
    private var currentReverse by mutableStateOf(false)
    private var currentUnitUA by mutableStateOf(false)
    private var dualBattery by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        BatteryStatsProvider.setCurrentAdjusted(true)
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
                                    text = "电流调整",
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
                            BatteryCurrentBar()
                            CurrentReverseSwitch()
                            CurrentUnitUASwitch()
                            DualBatterySwitch()
                            AutoAdjustButton()
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
    }

    override fun onStop() {
        stopTimer()
        super.onStop()
    }

    @Composable
    private fun BatteryCurrentBar() {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .background(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surface
                ),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "${batteryCurrent} mA",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = cumulusColor().blue
            )

            var tipText = "请确保电流值为负且大小正确"
            if (batteryStatus == BatteryManager.BATTERY_STATUS_CHARGING) {
                tipText = "请确保电流值为正且大小正确"
            }
            Text(
                modifier = Modifier.padding(top = 5.dp),
                text = tipText,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.secondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }

    @Composable
    fun CurrentReverseSwitch() {
        Switch(
            modifier = Modifier
                .padding(top = 20.dp)
                .height(50.dp)
                .fillMaxWidth()
                .background(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surface
                ),
            icon = AppCompatResources.getDrawable(
                applicationContext,
                R.drawable.current
            ),
            text = "电流反转",
            state = currentReverse
        ) { state ->
            BatteryStatsProvider.setCurrentReverse(state)
            currentReverse = state
        }
    }

    @Composable
    fun CurrentUnitUASwitch() {
        Switch(
            modifier = Modifier
                .padding(top = 5.dp)
                .height(50.dp)
                .fillMaxWidth()
                .background(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surface
                ),
            icon = AppCompatResources.getDrawable(applicationContext, R.drawable.swap),
            text = "uA-mA单位切换",
            state = currentUnitUA
        ) { state ->
            BatteryStatsProvider.setCurrentUnitUA(state)
            currentUnitUA = state
        }
    }

    @Composable
    fun DualBatterySwitch() {
        Switch(
            modifier = Modifier
                .padding(top = 5.dp)
                .height(50.dp)
                .fillMaxWidth()
                .background(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surface
                ),
            icon = AppCompatResources.getDrawable(
                applicationContext,
                R.drawable.device
            ),
            text = "双电芯设备",
            state = dualBattery
        ) { state ->
            BatteryStatsProvider.setDualBattery(state)
            dualBattery = state
        }
    }

    @Composable
    fun AutoAdjustButton() {
        GoToButton(
            modifier = Modifier
                .padding(top = 20.dp)
                .height(50.dp)
                .fillMaxWidth()
                .background(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surface
                ),
            icon = AppCompatResources.getDrawable(applicationContext, R.drawable.auto_done),
            text = "自动调整"
        ) {
            autoAdjustCurrent()
        }
    }

    private fun autoAdjustCurrent() {
        if ((batteryStatus != BatteryManager.BATTERY_STATUS_CHARGING && batteryCurrent > 0) ||
            (batteryStatus == BatteryManager.BATTERY_STATUS_CHARGING && batteryCurrent < 0)
        ) {
            BatteryStatsProvider.setCurrentReverse(!BatteryStatsProvider.isCurrentReverse())
        }
        if (batteryCurrent > 100000 || batteryCurrent < -100000) {
            BatteryStatsProvider.setCurrentUnitUA(true)
        } else if (batteryCurrent == 0) {
            BatteryStatsProvider.setCurrentUnitUA(false)
        }
        updateBatteryStats()
        Toast.makeText(applicationContext, "自动调整完毕", Toast.LENGTH_LONG).show()
    }

    private fun updateBatteryStats() {
        batteryCurrent = BatteryStatsProvider.getBatteryCurrent(applicationContext)
        batteryStatus = BatteryStatsProvider.getBatteryStatus(applicationContext)
        currentReverse = BatteryStatsProvider.isCurrentReverse()
        currentUnitUA = BatteryStatsProvider.isCurrentUnitUA()
        dualBattery = BatteryStatsProvider.isDualBattery()
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