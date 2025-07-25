package cumulus.battery.stats

import android.content.Intent
import android.content.res.Resources
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.appcompat.content.res.AppCompatResources
import androidx.compose.foundation.Image
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cumulus.battery.stats.ui.theme.CumulusTheme
import cumulus.battery.stats.widgets.GoToButton

class AdditionalFunctionActivity : ComponentActivity() {
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
                                    text = "附加功能",
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
                            StartFloatMonitorButton()
                            OpenBatteryUsageSettingButton()
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

    @Composable
    private fun StartFloatMonitorButton() {
        GoToButton(
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            icon = AppCompatResources.getDrawable(applicationContext, R.drawable.monitor),
            text = "监视器悬浮窗"
        ) {
            if (FloatMonitorService.isServiceCreated()) {
                val intent = Intent(applicationContext, FloatMonitorService::class.java)
                stopService(intent)
            } else {
                if (!Settings.canDrawOverlays(this)) {
                    Toast.makeText(applicationContext, "请授权悬浮窗权限", Toast.LENGTH_LONG)
                        .show()
                    val intent = Intent()
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    intent.action = "android.settings.APPLICATION_DETAILS_SETTINGS"
                    intent.data = Uri.fromParts("package", packageName, null)
                    startActivity(intent)
                } else {
                    val intent = Intent(applicationContext, FloatMonitorService::class.java)
                    startService(intent)
                }
            }
        }
    }

    @Composable
    private fun OpenBatteryUsageSettingButton() {
        GoToButton(
            modifier = Modifier
                .padding(top = 5.dp)
                .fillMaxWidth()
                .height(50.dp),
            icon = AppCompatResources.getDrawable(applicationContext, R.drawable.app_settings),
            text = "电池使用状况"
        ) {
            val intent = Intent(Intent.ACTION_POWER_USAGE_SUMMARY)
            startActivity(intent)
        }
    }
}