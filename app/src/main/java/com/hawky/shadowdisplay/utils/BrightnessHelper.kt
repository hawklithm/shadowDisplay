package com.hawky.shadowdisplay.utils

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Window
import com.hawky.shadowdisplay.settings.SettingsManager

/**
 * 亮度辅助类
 * 处理自动亮度功能
 */
class BrightnessHelper(
    private val context: Context,
    private val window: Window?
) {
    companion object {
        private const val TAG = "BrightnessHelper"
        private const val LOW_BATTERY_THRESHOLD = 20 // 低电量阈值20%
    }

    private var settingsManager: SettingsManager? = null
    private var sensorManager: SensorManager? = null
    private var lightSensor: Sensor? = null
    private var sensorEventListener: SensorEventListener? = null

    private var isLowBattery = false
    private val handler = Handler(Looper.getMainLooper())

    /**
     * 初始化
     */
    fun initialize() {
        settingsManager = SettingsManager.getInstance(context)
        setupLightSensor()
    }

    /**
     * 设置光照传感器
     */
    private fun setupLightSensor() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            Log.d(TAG, "系统版本不支持光照传感器API")
            return
        }

        sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        lightSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_LIGHT)

        if (lightSensor != null) {
            sensorEventListener = object : SensorEventListener {
                override fun onSensorChanged(event: SensorEvent?) {
                    if (event?.sensor?.type == Sensor.TYPE_LIGHT && event.values.isNotEmpty()) {
                        val lux = event.values[0]
                        adjustScreenBrightness(lux)
                    }
                }

                override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
                    // 不需要处理精度变化
                }
            }
            sensorManager?.registerListener(sensorEventListener, lightSensor, SensorManager.SENSOR_DELAY_NORMAL)
            Log.d(TAG, "光照传感器已注册")
        } else {
            Log.d(TAG, "设备不支持光照传感器")
        }
    }

    /**
     * 释放光照传感器
     */
    fun releaseLightSensor() {
        try {
            sensorManager?.unregisterListener(sensorEventListener)
            sensorEventListener = null
            lightSensor = null
            sensorManager = null
            Log.d(TAG, "已释放光照传感器")
        } catch (e: Exception) {
            Log.e(TAG, "释放光照传感器失败", e)
        }
    }

    /**
     * 根据光照强度调整屏幕亮度
     */
    private fun adjustScreenBrightness(lux: Float) {
        val currentSettings = settingsManager?.getSettings() ?: return

        if (!currentSettings.autoBrightness || isLowBattery) {
            // 未开启自动亮度或低电量时，不调整
            return
        }

        // 根据光照强度调整亮度 (0-1.0)
        val brightness = when {
            lux < 10 -> 0.1f  // 很暗
            lux < 50 -> 0.3f  // 昏暗
            lux < 200 -> 0.6f // 中等
            lux < 500 -> 0.8f // 明亮
            else -> 1.0f      // 非常亮
        }

        window?.attributes = window?.attributes?.apply {
            screenBrightness = brightness
        }

        Log.d(TAG, "光照: ${lux.toInt()} lux, 亮度: $brightness")
    }

    /**
     * 应用亮度设置（手动或自动）
     */
    fun applyBrightnessSettings() {
        val currentSettings = settingsManager?.getSettings() ?: return

        if (!currentSettings.autoBrightness) {
            // 使用手动亮度
            val brightness = currentSettings.manualBrightness
            window?.attributes = window?.attributes?.apply {
                screenBrightness = brightness
            }
            Log.d(TAG, "使用手动亮度: $brightness")
        }
    }

    /**
     * 检查电池状态并应用低电量优化
     */
    fun checkBatteryStatus(batteryLevel: Int) {
        val wasLowBattery = isLowBattery
        isLowBattery = batteryLevel in 1 until LOW_BATTERY_THRESHOLD

        if (isLowBattery && !wasLowBattery) {
            Log.d(TAG, "进入低电量模式: $batteryLevel%")
            onLowBatteryEnter()
        } else if (!isLowBattery && wasLowBattery) {
            Log.d(TAG, "退出低电量模式: $batteryLevel%")
            onLowBatteryExit()
        }
    }

    /**
     * 进入低电量模式
     */
    private fun onLowBatteryEnter() {
        // 降低屏幕亮度
        window?.attributes = window?.attributes?.apply {
            screenBrightness = 0.3f
        }
        Log.d(TAG, "已应用低电量优化")
    }

    /**
     * 退出低电量模式
     */
    private fun onLowBatteryExit() {
        // 恢复亮度设置
        applyBrightnessSettings()
        Log.d(TAG, "已恢复正常模式")
    }

    /**
     * 清理资源
     */
    fun cleanup() {
        releaseLightSensor()
    }
}
