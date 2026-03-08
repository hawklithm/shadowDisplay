package com.hawky.shadowdisplay.power

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.util.Log
import android.view.View
import android.view.WindowManager
import com.hawky.shadowdisplay.permission.PermissionManager
import java.util.Calendar
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

/**
 * 功耗优化管理器
 * 负责智能调整亮度、管理传感器和优化更新频率
 */
class PowerOptimizer(private val context: Context) {

    companion object {
        private const val TAG = "PowerOptimizer"
        private const val BURN_IN_PROTECTION_INTERVAL_MINUTES = 5L
        private const val BURN_IN_PROTECTION_PIXELS = 1
    }

    private val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    
    private val scheduledExecutor: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())
    
    private var currentView: View? = null
    private var currentWindowParams: WindowManager.LayoutParams? = null
    private var isOptimizing = false
    
    // 烧屏防护相关
    private var burnInOffsetX = 0
    private var burnInOffsetY = 0
    
    // 传感器监听
    private var proximitySensor: Sensor? = null
    private var proximityListener: SensorEventListener? = null
    private var isMonitoringProximity = false
    
    /**
     * 设置要优化的视图
     */
    fun setupView(view: View, windowParams: WindowManager.LayoutParams) {
        this.currentView = view
        this.currentWindowParams = windowParams
        
        // 应用初始优化
        applyPowerOptimizations()
        
        // 启动烧屏防护
        startBurnInProtection()
        
        // 应用智能亮度
        applySmartBrightness()
    }

    /**
     * 应用功耗优化
     */
    private fun applyPowerOptimizations() {
        val view = currentView ?: return
        
        // 1. 设置纯黑背景（OLED省电）
        view.setBackgroundColor(0xFF000000.toInt())
        
        // 2. 启用硬件加速
        view.setLayerType(View.LAYER_TYPE_HARDWARE, null)
        
        // 3. 针对OLED屏幕的优化
        if (PermissionManager.getInstance().isOLEDScreen(context)) {
            // OLED可以使用更低亮度
            view.alpha = 0.95f
        }
        
        Log.d(TAG, "功耗优化已应用")
    }

    /**
     * 启动烧屏防护
     */
    private fun startBurnInProtection() {
        // 每5分钟移动显示位置1像素
        scheduledExecutor.scheduleAtFixedRate({
            mainHandler.post {
                moveDisplayByPixel(BURN_IN_PROTECTION_PIXELS, BURN_IN_PROTECTION_PIXELS)
            }
        }, 0, BURN_IN_PROTECTION_INTERVAL_MINUTES, TimeUnit.MINUTES)
        
        Log.d(TAG, "烧屏防护已启动")
    }

    /**
     * 移动显示位置（像素级位移）
     */
    private fun moveDisplayByPixel(dx: Int, dy: Int) {
        val view = currentView ?: return
        
        burnInOffsetX += dx
        burnInOffsetY += dy
        
        // 防止偏移过大
        if (burnInOffsetX > 5) burnInOffsetX = -5
        if (burnInOffsetY > 5) burnInOffsetY = -5
        
        view.translationX = burnInOffsetX.toFloat()
        view.translationY = burnInOffsetY.toFloat()
        
        Log.d(TAG, "显示位置已移动: x=$burnInOffsetX, y=$burnInOffsetY")
    }

    /**
     * 应用智能亮度
     */
    private fun applySmartBrightness() {
        val brightness = PermissionManager.getInstance().getRecommendedBrightnessByTime(context)
        updateWindowBrightness(brightness)
        
        Log.d(TAG, "智能亮度已应用: $brightness")
    }

    /**
     * 更新窗口亮度
     */
    fun updateWindowBrightness(brightness: Float) {
        currentWindowParams?.screenBrightness = brightness.coerceIn(0.001f, 0.1f)
        Log.d(TAG, "窗口亮度已更新: $brightness")
    }

    /**
     * 启动智能更新策略
     */
    fun startSmartUpdate(onUpdate: () -> Unit) {
        isOptimizing = true
        
        mainHandler.post(object : Runnable {
            override fun run() {
                if (!isOptimizing) return
                
                val isScreenOn = powerManager.isInteractive
                
                if (isScreenOn) {
                    // 屏幕亮时正常更新（每秒）
                    onUpdate()
                    mainHandler.postDelayed(this, 1000)
                } else {
                    // 屏幕关闭时降低频率（每分钟）
                    onUpdate()
                    mainHandler.postDelayed(this, 60000)
                }
            }
        })
        
        Log.d(TAG, "智能更新策略已启动")
    }

    /**
     * 停止智能更新
     */
    fun stopSmartUpdate() {
        isOptimizing = false
        mainHandler.removeCallbacksAndMessages(null)
        Log.d(TAG, "智能更新策略已停止")
    }

    /**
     * 启动距离传感器监听（可选）
     */
    fun startProximityMonitoring() {
        if (isMonitoringProximity) return
        
        proximitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY)
        
        if (proximitySensor != null) {
            proximityListener = object : SensorEventListener {
                override fun onSensorChanged(event: SensorEvent?) {
                    event?.let {
                        val isNear = it.values[0] < 3.0f
                        Log.d(TAG, "距离传感器: $isNear")
                        // 根据距离调整显示策略
                        adjustDisplayForProximity(isNear)
                    }
                }
                
                override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
            }
            
            sensorManager.registerListener(
                proximityListener,
                proximitySensor,
                SensorManager.SENSOR_DELAY_NORMAL
            )
            
            isMonitoringProximity = true
            Log.d(TAG, "距离传感器监听已启动")
        } else {
            Log.d(TAG, "设备不支持距离传感器")
        }
    }

    /**
     * 停止距离传感器监听
     */
    fun stopProximityMonitoring() {
        if (!isMonitoringProximity) return
        
        proximityListener?.let {
            sensorManager.unregisterListener(it)
        }
        
        isMonitoringProximity = false
        proximityListener = null
        Log.d(TAG, "距离传感器监听已停止")
    }

    /**
     * 根据距离调整显示策略
     */
    private fun adjustDisplayForProximity(isNear: Boolean) {
        val view = currentView ?: return
        
        if (isNear) {
            // 贴近时降低亮度
            view.alpha = 0.8f
            updateWindowBrightness(0.005f)
        } else {
            // 远离时恢复亮度
            view.alpha = 1.0f
            applySmartBrightness()
        }
    }

    /**
     * 获取当前亮度模式描述
     */
    fun getBrightnessModeDescription(): String {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        
        return when {
            hour in 6..18 -> "日间模式（亮度较高）"
            hour in 19..22 -> "傍晚模式（中等亮度）"
            else -> "夜间模式（最低亮度）"
        }
    }

    /**
     * 获取电池状态信息
     */
    fun getBatteryStatus(): Map<String, Any> {
        val batteryStatus = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val intent = context.registerReceiver(null, batteryStatus)
        
        val level = intent?.getIntExtra("level", 0) ?: 0
        val plugged = intent?.getIntExtra("plugged", 0) ?: 0
        val isCharging = plugged > 0
        
        return mapOf(
            "level" to level,
            "isCharging" to isCharging,
            "plugged" to plugged
        )
    }

    /**
     * 清理资源
     */
    fun release() {
        isOptimizing = false
        stopProximityMonitoring()
        mainHandler.removeCallbacksAndMessages(null)
        scheduledExecutor.shutdown()
        
        currentView = null
        currentWindowParams = null
        
        Log.d(TAG, "资源已释放")
    }
}
