package com.hawky.shadowdisplay.service

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.BatteryManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.WindowManager
import com.hawky.shadowdisplay.settings.SettingsManager
import java.util.Random

/**
 * 功耗优化辅助类
 * 负责处理自动亮度、烧屏保护、低电量检测
 */
class PowerOptimizationHelper(
    private val service: LockScreenDreamService,
    private val settingsManager: SettingsManager
) {

    companion object {
        private const val TAG = "PowerOptimization"
        private const val BURN_IN_PROTECTION_OFFSET_MAX = 8 // 最大偏移像素
        private const val BURN_IN_PROTECTION_UPDATE_INTERVAL_MS = 30000L // 30秒更新一次
        private const val LOW_BATTERY_THRESHOLD = 20 // 低电量阈值20%
    }

    // 光照传感器
    private var sensorManager: SensorManager? = null
    private var lightSensor: Sensor? = null
    private var sensorEventListener: SensorEventListener? = null

    // 烧屏保护
    private var burnInProtectionHandler: Handler? = null
    private var burnInProtectionRunnable: Runnable? = null
    private var offsetX = 0f
    private var offsetY = 0f
    private var currentView: View? = null
    private val random = Random()

    // 低电量检测
    private var isLowBattery = false

    /**
     * 设置光照传感器
     */
    fun setupLightSensor() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            Log.d(TAG, "系统版本不支持光照传感器API")
            return
        }

        val context = service as? Context ?: return
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
     * 调整屏幕亮度
     */
    private fun adjustScreenBrightness(lux: Float) {
        val currentSettings = settingsManager.getSettings()

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

        val window = service.window
        window?.attributes = window?.attributes?.apply {
            screenBrightness = brightness
        }

        Log.d(TAG, "光照: ${lux.toInt()} lux, 亮度: $brightness")
    }

    /**
     * 应用亮度设置
     */
    fun applyBrightnessSettings() {
        val currentSettings = settingsManager.getSettings()

        if (!currentSettings.autoBrightness) {
            // 使用手动亮度
            val brightness = currentSettings.manualBrightness
            val window = service.window
            window?.attributes = window?.attributes?.apply {
                screenBrightness = brightness
            }
            Log.d(TAG, "使用手动亮度: $brightness")
        }
    }

    /**
     * 检查电池状态
     */
    fun checkBatteryStatus() {
        try {
            val context = service as? android.content.Context ?: return
            val batteryStatus = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            val level = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1

            if (level >= 0) {
                val wasLowBattery = isLowBattery
                isLowBattery = level < LOW_BATTERY_THRESHOLD

                if (isLowBattery && !wasLowBattery) {
                    Log.d(TAG, "进入低电量模式: $level%")
                    onLowBatteryEnter()
                } else if (!isLowBattery && wasLowBattery) {
                    Log.d(TAG, "退出低电量模式: $level%")
                    onLowBatteryExit()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "检查电池状态失败", e)
        }
    }

    /**
     * 进入低电量模式
     */
    private fun onLowBatteryEnter() {
        // 降低屏幕亮度
        val window = service.window
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
     * 启动烧屏保护
     */
    fun startBurnInProtection(view: View) {
        currentView = view
        burnInProtectionHandler = Handler(Looper.getMainLooper())
        burnInProtectionRunnable = object : Runnable {
            override fun run() {
                // 生成新的随机偏移
                offsetX = random.nextFloat() * BURN_IN_PROTECTION_OFFSET_MAX - (BURN_IN_PROTECTION_OFFSET_MAX / 2)
                offsetY = random.nextFloat() * BURN_IN_PROTECTION_OFFSET_MAX - (BURN_IN_PROTECTION_OFFSET_MAX / 2)

                // 应用偏移
                currentView?.translationX = offsetX
                currentView?.translationY = offsetY

                Log.d(TAG, "烧屏保护偏移: x=$offsetX, y=$offsetY")

                // 继续下一次偏移
                burnInProtectionHandler?.postDelayed(this, BURN_IN_PROTECTION_UPDATE_INTERVAL_MS)
            }
        }

        // 开始第一次偏移
        burnInProtectionRunnable?.run()

        Log.d(TAG, "烧屏保护已启动")
    }

    /**
     * 停止烧屏保护
     */
    fun stopBurnInProtection() {
        burnInProtectionRunnable?.let {
            burnInProtectionHandler?.removeCallbacks(it)
        }

        // 重置偏移
        offsetX = 0f
        offsetY = 0f
        currentView?.translationX = 0f
        currentView?.translationY = 0f

        burnInProtectionRunnable = null
        burnInProtectionHandler = null
        currentView = null

        Log.d(TAG, "烧屏保护已停止")
    }

    /**
     * 清理资源
     */
    fun cleanup() {
        stopBurnInProtection()
        releaseLightSensor()
    }
}
