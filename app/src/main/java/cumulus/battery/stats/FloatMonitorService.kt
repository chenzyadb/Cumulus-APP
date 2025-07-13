package cumulus.battery.stats

import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.Gravity
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.res.ResourcesCompat
import cumulus.battery.stats.objects.BatteryStatsProvider
import cumulus.battery.stats.objects.CpuStatsProvider
import java.util.Timer
import java.util.TimerTask

class FloatMonitorService : Service() {
    companion object {
        private var serviceCreated = false
        fun isServiceCreated(): Boolean {
            return serviceCreated
        }
    }

    private var floatWindow: TextView? = null
    private var timer: Timer? = null

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        serviceCreated = true
        createFloatWindow()
        startTimer()
    }

    override fun onDestroy() {
        stopTimer()
        destroyFloatWindow()
        serviceCreated = false
        super.onDestroy()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return super.onStartCommand(intent, START_FLAG_REDELIVERY, startId)
    }

    private fun createFloatWindow() {
        if (floatWindow != null) {
            return
        }
        floatWindow = TextView(this)
        floatWindow!!.setBackgroundColor(Color(0x88000000).toArgb())
        val penddingVal = (applicationContext.resources.displayMetrics.density * 5 + 0.5f).toInt()
        floatWindow!!.setPadding(penddingVal, penddingVal, penddingVal, penddingVal)
        floatWindow!!.text = ""
        floatWindow!!.setTextColor(Color(0xFFFFFFFF).toArgb())
        floatWindow!!.textSize = 9f
        val typeface = ResourcesCompat.getFont(applicationContext, R.font.jetbrainsmono)
        floatWindow!!.typeface = typeface

        val windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        val layoutParams = WindowManager.LayoutParams()
        layoutParams.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        layoutParams.flags =
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE + WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
        layoutParams.format = PixelFormat.RGBA_8888
        layoutParams.width = LinearLayout.LayoutParams.WRAP_CONTENT
        layoutParams.height = LinearLayout.LayoutParams.WRAP_CONTENT
        layoutParams.gravity = Gravity.TOP or Gravity.END
        layoutParams.alpha = 0.6f
        windowManager.addView(floatWindow, layoutParams)
    }

    private fun destroyFloatWindow() {
        val windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        windowManager.removeViewImmediate(floatWindow)
    }

    private fun updateMonitorText() {
        val batteryCapacity = BatteryStatsProvider.getBatteryCapacity(applicationContext)
        val batteryTemperature = BatteryStatsProvider.getBatteryTemperature(applicationContext)
        val batteryCurrent = BatteryStatsProvider.getBatteryCurrent(applicationContext)
        val batteryPower =
            BatteryStatsProvider.getBatteryVoltage(applicationContext) * batteryCurrent / 1000
        val cpuFreqs = CpuStatsProvider.getCpuFreqs()
        val cpuGovernor = CpuStatsProvider.getCpuGovernor()
        var monitorText = ""
        monitorText += "#Battery ${batteryCapacity} % ${batteryTemperature} °C \n"
        monitorText += "#Power ${batteryPower} mW \n"
        monitorText += "#Current ${batteryCurrent} mA \n"
        for (cpuCore in 0 until cpuFreqs.size) {
            val cpuFreqMHz = cpuFreqs[cpuCore] / 1000
            monitorText += "#CPU${cpuCore}    ${cpuFreqMHz} MHz \n"
        }
        monitorText += "#Governor ${cpuGovernor}"
        val handler = Handler(Looper.getMainLooper())
        handler.post {
            if (floatWindow == null) {
                return@post
            }
            floatWindow!!.text = monitorText
        }
    }

    private fun startTimer() {
        if (timer != null) {
            return
        }
        timer = Timer()
        timer!!.schedule(object : TimerTask() {
            override fun run() {
                updateMonitorText()
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