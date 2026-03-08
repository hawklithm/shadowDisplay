package com.hawky.shadowdisplay.permission

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import com.hawky.shadowdisplay.manufacturer.ManufacturerAdapter

/**
 * 权限步骤数据类
 */
data class PermissionStep(
    val title: String,
    val description: String,
    val importance: PermissionImportance
) {
    enum class PermissionImportance {
        CRITICAL,
        HIGH,
        MEDIUM,
        LOW
    }
}

/**
 * 权限管理器
 */
class PermissionManager private constructor() {

    companion object {
        private const val TAG = "PermissionManager"

        @Volatile
        private var instance: PermissionManager? = null

        fun getInstance(): PermissionManager {
            return instance ?: synchronized(this) {
                instance ?: PermissionManager().also { instance = it }
            }
        }

        const val REQUEST_OVERLAY_PERMISSION = 1001
        const val REQUEST_BATTERY_OPTIMIZATION = 1002
    }

    fun isAllPermissionsGranted(context: Context): Boolean {
        return checkOverlayPermission(context) && checkBatteryOptimization(context)
    }

    fun checkOverlayPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(context)
        } else {
            true
        }
    }

    fun checkBatteryOptimization(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            powerManager.isIgnoringBatteryOptimizations(context.packageName)
        } else {
            true
        }
    }

    fun showManufacturerGuide(activity: Activity) {
        val manufacturer = ManufacturerAdapter.getCurrentManufacturer()
        val instructions = ManufacturerAdapter.getPermissionInstructions(manufacturer)

        AlertDialog.Builder(activity)
            .setTitle("${ManufacturerAdapter.getManufacturerName(manufacturer)}设备设置指南")
            .setMessage(instructions.joinToString("\n\n"))
            .setPositiveButton("前往设置") { _, _ ->
                try {
                    activity.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.parse("package:${activity.packageName}")
                    })
                } catch (e: Exception) {
                    Log.e(TAG, "无法打开设置", e)
                }
            }
            .setNegativeButton("稍后", null)
            .setCancelable(false)
            .show()
    }

    fun requestAllPermissions(activity: Activity) {
        if (!checkOverlayPermission(activity)) {
            requestOverlayPermission(activity)
        } else if (!checkBatteryOptimization(activity)) {
            requestBatteryOptimization(activity)
        } else if (ManufacturerAdapter.needsSpecialAdaptation()) {
            showManufacturerGuide(activity)
        }
    }

    private fun requestOverlayPermission(activity: Activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:${activity.packageName}")
            )
            activity.startActivityForResult(intent, REQUEST_OVERLAY_PERMISSION)
        }
    }

    private fun requestBatteryOptimization(activity: Activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.parse("package:${activity.packageName}")
            }
            activity.startActivityForResult(intent, REQUEST_BATTERY_OPTIMIZATION)
        }
    }

    fun isOLEDScreen(context: Context): Boolean {
        return try {
            false
        } catch (e: Exception) {
            Log.e(TAG, "无法检测屏幕类型", e)
            false
        }
    }

    fun getRecommendedBrightness(context: Context): Float {
        return if (isOLEDScreen(context)) {
            0.005f
        } else {
            0.01f
        }
    }

    fun getRecommendedBrightnessByTime(context: Context): Float {
        val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)

        return when {
            hour in 6..18 -> 0.02f
            hour in 19..22 -> 0.015f
            else -> 0.005f
        }
    }
}
