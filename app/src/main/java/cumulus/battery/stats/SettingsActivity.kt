package cumulus.battery.stats

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Resources
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.appcompat.app.AlertDialog
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
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.core.net.toUri
import cumulus.battery.stats.objects.BatteryStatsRecorder
import cumulus.battery.stats.ui.theme.CumulusTheme
import cumulus.battery.stats.ui.theme.cumulusColor
import cumulus.battery.stats.widgets.GoToButton

class SettingsActivity : ComponentActivity() {
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
                                    text = "设置",
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
                            Text(
                                modifier = Modifier
                                    .padding(start = 20.dp, top = 10.dp)
                                    .height(20.dp)
                                    .fillMaxWidth(),
                                text = "应用设置",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = cumulusColor().purple,
                                maxLines = 1
                            )
                            RequireIgnoreBatteryOptimizationButton()
                            DeleteHistoryDataButton()
                            AdjustCurrentButton()
                            Text(
                                modifier = Modifier
                                    .padding(start = 20.dp, top = 20.dp)
                                    .height(20.dp)
                                    .fillMaxWidth(),
                                text = "关于",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = cumulusColor().purple,
                                maxLines = 1
                            )
                            AboutInformation()
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

    @SuppressLint("BatteryLife")
    @Composable
    private fun RequireIgnoreBatteryOptimizationButton() {
        GoToButton(
            modifier = Modifier
                .padding(top = 10.dp)
                .fillMaxWidth()
                .height(40.dp),
            text = "请求忽略电池优化"
        ) {
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
            intent.data = ("package:" + applicationContext.packageName).toUri()
            startActivity(intent)
        }
    }

    @Composable
    private fun DeleteHistoryDataButton() {
        GoToButton(
            modifier = Modifier
                .padding(top = 5.dp)
                .fillMaxWidth()
                .height(40.dp),
            text = "清除历史数据"
        ) {
            val builder = AlertDialog.Builder(this)
            builder.setTitle("警告")
            builder.setMessage("清除数据操作不可逆")
            builder.setPositiveButton("继续") { _, _ ->
                BatteryStatsRecorder.deleteHistoryData()
                Toast.makeText(
                    applicationContext,
                    "历史数据已清除",
                    Toast.LENGTH_LONG
                ).show()
            }
            builder.setNegativeButton("取消") { _, _ ->
                Toast.makeText(applicationContext, "已取消操作", Toast.LENGTH_LONG).show()
            }
            val dialog = builder.create()
            dialog.show()
        }
    }

    @Composable
    private fun AdjustCurrentButton() {
        GoToButton(
            modifier = Modifier
                .padding(top = 5.dp)
                .fillMaxWidth()
                .height(40.dp),
            text = "调整电流显示"
        ) {
            val intent =
                Intent(applicationContext, CurrentAdjustActivity::class.java)
            startActivity(intent)
        }
    }

    @Suppress("deprecation")
    @Composable
    private fun AboutInformation() {
        val versionName =
            packageManager.getPackageInfo(packageName, PackageManager.GET_META_DATA).versionName
        val versionCode =
            packageManager.getPackageInfo(packageName, PackageManager.GET_META_DATA).versionCode

        Column(
            modifier = Modifier
                .padding(start = 20.dp, end = 20.dp, top = 5.dp)
                .fillMaxWidth()
                .height(90.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.Start
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Cu",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = cumulusColor().pink,
                    maxLines = 1
                )
                Text(
                    text = "mulus",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = cumulusColor().blue,
                    maxLines = 1
                )
            }
            Text(
                text = "版本: ${versionName} (${versionCode})",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.secondary,
                maxLines = 1
            )
            Text(
                text = "Copyright (C) Chenzyadb 2025",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.secondary,
                maxLines = 1
            )
        }
    }
}