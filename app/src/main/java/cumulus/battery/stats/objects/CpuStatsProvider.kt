package cumulus.battery.stats.objects

import java.io.File

object CpuStatsProvider {
    private val cpuGovernorFile: File = File("/sys/devices/system/cpu/cpu0/cpufreq/scaling_governor")
    private val cpuFreqFiles: MutableList<File> = mutableListOf()

    init {
        for (cpuCore in 0..9) {
            val cpuFreqFile = File("/sys/devices/system/cpu/cpu${cpuCore}/cpufreq/scaling_cur_freq")
            if (cpuFreqFile.exists()) {
                cpuFreqFiles.add(cpuFreqFile)
            }
        }
    }

    fun getCpuGovernor(): String {
        if (cpuGovernorFile.exists()) {
            var cpuGovernor = ""
            try {
                cpuGovernor = cpuGovernorFile.readText(Charsets.UTF_8)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return cpuGovernor
                .replace("\n", "")
                .replace(" ", "")
        }
        return "unknown"
    }

    fun getCpuFreqs(): IntArray {
        val cpuFreqs = IntArray(cpuFreqFiles.size) { 0 }
        for (cpuCore in 0 until cpuFreqFiles.size) {
            var cpuFreq = 0
            try {
                cpuFreq = cpuFreqFiles[cpuCore]
                    .readText(Charsets.UTF_8)
                    .replace("\n", "")
                    .replace(" ", "")
                    .toInt()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            cpuFreqs[cpuCore] = cpuFreq
        }
        return cpuFreqs
    }
}