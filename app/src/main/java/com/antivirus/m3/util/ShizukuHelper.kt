package com.antivirus.m3.util

import android.content.pm.PackageManager
import com.antivirus.m3.data.ProcessInfo
import rikka.shizuku.Shizuku
import java.io.BufferedReader
import java.io.InputStreamReader

class ShizukuHelper {

    companion object {
        const val PERMISSION_REQUEST_CODE = 1001

        fun isAvailable(): Boolean {
            return try {
                Shizuku.pingBinder()
            } catch (e: Exception) {
                false
            }
        }

        fun hasPermission(): Boolean {
            return try {
                Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
            } catch (e: Exception) {
                false
            }
        }

        fun requestPermission() {
            try {
                Shizuku.requestPermission(PERMISSION_REQUEST_CODE)
            } catch (e: Exception) {
                // Shizuku not available
            }
        }

        fun execute(command: String): String {
            return try {
                val process = Runtime.getRuntime().exec(arrayOf("sh", "-c", command))
                val reader = BufferedReader(InputStreamReader(process.inputStream))
                val errorReader = BufferedReader(InputStreamReader(process.errorStream))
                val output = StringBuilder()
                reader.forEachLine { output.appendLine(it) }
                errorReader.forEachLine { output.appendLine(it) }
                process.waitFor()
                output.toString()
            } catch (e: Exception) {
                "Error: ${e.message}"
            }
        }

        fun getProcessList(): List<ProcessInfo> {
            val output = execute("ps -A -o PID,USER,NAME,RSS")
            val processes = mutableListOf<ProcessInfo>()
            val lines = output.lines()
            for (i in 1 until lines.size) {
                val line = lines[i].trim()
                if (line.isEmpty()) continue
                val parts = line.split(Regex("\\s+"))
                if (parts.size >= 4) {
                    try {
                        val pid = parts[0].toInt()
                        val user = parts[1]
                        val name = parts[2]
                        val rss = parts[3].toLongOrNull() ?: 0L
                        val isSystem = user == "root" || user == "system" ||
                                name.startsWith("android.") || name.startsWith("com.android.")
                        processes.add(ProcessInfo(pid, name, user, rss, isSystem))
                    } catch (_: NumberFormatException) {}
                }
            }
            return processes.sortedByDescending { it.memoryKb }
        }

        fun killProcess(pid: Int): Boolean {
            val output = execute("kill -9 $pid")
            return output.isEmpty() || !output.contains("No such process")
        }

        fun uninstallApp(packageName: String): Boolean {
            val output = execute("pm uninstall --user 0 $packageName")
            return output.contains("Success")
        }

        fun forceStopApp(packageName: String): Boolean {
            val output = execute("am force-stop $packageName")
            return output.isEmpty()
        }

        fun clearAppData(packageName: String): Boolean {
            val output = execute("pm clear $packageName")
            return output.contains("Success")
        }

        fun disableApp(packageName: String): Boolean {
            val output = execute("pm disable-user --user 0 $packageName")
            return output.contains("Success")
        }

        fun getAppLogs(packageName: String): String {
            return execute("logcat -d -s ${packageName.take(20)}:* | tail -100")
        }

        fun getAllAppLogs(): String {
            return execute("logcat -d | tail -200")
        }
    }
}
