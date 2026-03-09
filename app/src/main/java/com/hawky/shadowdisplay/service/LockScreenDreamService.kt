package com.hawky.shadowdisplay.service

import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.graphics.Color
import android.os.BatteryManager
import android.os.Build
import android.service.dreams.DreamService
import android.util.Log
import android.view.View
import android.view.WindowManager
import com.hawky.shadowdisplay.models.DisplayMode
import com.hawky.shadowdisplay.models.TriggerMode
import com.hawky.shadowdisplay.settings.SettingsManager
import com.hawky.shadowdisplay.utils.BrightnessHelper
import com.hawky.shadowdisplay.utils.DisplayViewHelper
import com.hawky.shadowdisplay.utils.RotationHelper
import com.hawky.shadowdisplay.views.AnalogClockView
import com.hawky.shadowdisplay.DigitalClockView
import com.hawky.shadowdisplay.views.FlipClockView
import com.hawky.shadowdisplay.views.RobotEyesView

/**
 * 趣味屏保服务
 * 支持5种显示模式：数字时钟、指针时钟、翻页时钟、瓦力眼睛、小黄人眼睛
 * 统一支持：自动旋转、自动亮度、烧屏保护
 * 使用DisplayViewHelper和BrightnessHelper实现代码复用
 */
class LockScreenDreamService : DreamService() {

    companion object {
        private const val TAG = "LockScreenDreamService"
    }

    // 显示视图
    private var currentView: View? = null

    // 设置
    private val settings = SettingsManager.getInstance(this)

    // 辅助类 - 复用DisplayViewHelper和BrightnessHelper
    private var displayViewHelper: DisplayViewHelper? = null
    private var brightnessHelper: BrightnessHelper? = null
    private var rotationHelper: RotationHelper? = null

    // 电池接收器
    private var batteryReceiver: android.content.BroadcastReceiver? = null

    // 状态
    private var isAutoRotation = false
    private var isAutoBrightness = false
    private var isBurnInProtection = false

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "onCreate")

        // 读取设置
        val currentSettings = settings.getSettings()
        isAutoRotation = currentSettings.autoRotation
        isAutoBrightness = currentSettings.autoBrightness
        isBurnInProtection = currentSettings.burnInProtection

        // 初始化辅助类
        brightnessHelper = BrightnessHelper(this, window)
        brightnessHelper?.initialize()

        rotationHelper = RotationHelper { isLandscape ->
            updateViewOrientation(isLandscape)
        }
    }

    override fun onDreamingStarted() {
        super.onDreamingStarted()
        Log.d(TAG, "屏保已启动")

        // 检查触发条件
        if (!shouldShowScreensaver()) {
            Log.d(TAG, "不满足触发条件，退出屏保")
            finish()
            return
        }

        // 检查电池状态
        brightnessHelper?.applyBrightnessSettings()
        checkBatteryStatus()

        // 设置窗口属性
        setupWindow()

        // 创建显示视图
        createContentView()

        // 启动烧屏保护
        if (isBurnInProtection) {
            currentView?.let {
                displayViewHelper = DisplayViewHelper(it)
                displayViewHelper?.startBurnInProtection()
            }
        }

        // 进入沉浸式全屏模式
        enterFullscreenImmersive()

        // 注册电池接收器
        registerBatteryReceiver()

        Log.d(TAG, "屏保设置完成")
    }

    /**
     * 检查是否应该显示屏保
     */
    private fun shouldShowScreensaver(): Boolean {
        val currentSettings = settings.getSettings()
        val triggerMode = currentSettings.triggerMode

        return when (triggerMode) {
            TriggerMode.MANUAL -> false  // 手动模式不在DreamService中显示

            TriggerMode.CHARGING_ONLY -> {
                // 检查充电状态
                val batteryStatus = registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
                val status = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
                val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                                status == BatteryManager.BATTERY_STATUS_FULL
                isCharging
            }
        }
    }

    /**
     * 检查电池状态
     */
    private fun checkBatteryStatus() {
        try {
            val batteryStatus = registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            val level = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1

            if (level >= 0) {
                brightnessHelper?.checkBatteryStatus(level)
            }
        } catch (e: Exception) {
            Log.e(TAG, "检查电池状态失败", e)
        }
    }

    /**
     * 注册电池接收器
     */
    private fun registerBatteryReceiver() {
        batteryReceiver = object : android.content.BroadcastReceiver() {
            override fun onReceive(context: android.content.Context?, intent: Intent?) {
                val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
                if (level >= 0) {
                    brightnessHelper?.checkBatteryStatus(level)
                }
            }
        }

        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        registerReceiver(batteryReceiver, filter)
    }

    /**
     * 设置窗口属性
     */
    private fun setupWindow() {
        isInteractive = false  // 不需要交互
    }

    /**
     * 创建显示视图
     */
    private fun createContentView() {
        val currentSettings = settings.getSettings()
        val displayMode = currentSettings.displayMode

        currentView = when (displayMode) {
            DisplayMode.DIGITAL_CLOCK -> DigitalClockView(this)
            DisplayMode.ANALOG_CLOCK -> AnalogClockView(this)
            DisplayMode.FLIP_CLOCK -> FlipClockView(this)
            DisplayMode.ROBOT_EYES_WALL_E -> RobotEyesView(this).apply {
                setEyeStyle(RobotEyesView.EyeStyle.WALL_E)
            }
            DisplayMode.ROBOT_EYES_MINION -> RobotEyesView(this).apply {
                setEyeStyle(RobotEyesView.EyeStyle.MINION)
            }
        }

        // 设置横竖屏
        val isLandscape = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        updateViewOrientation(isLandscape)

        setContentView(currentView)

        // 设置窗口背景为黑色
        window?.decorView?.setBackgroundColor(Color.BLACK)

        Log.d(TAG, "已创建视图: ${displayMode.displayName}")
    }

    /**
     * 更新视图的横竖屏状态
     */
    private fun updateViewOrientation(isLandscape: Boolean) {
        when (currentView) {
            is DigitalClockView -> (currentView as DigitalClockView).setLandscape(isLandscape)
            is AnalogClockView -> (currentView as AnalogClockView).setLandscape(isLandscape)
            is RobotEyesView -> (currentView as RobotEyesView).setLandscape(isLandscape)
            is FlipClockView -> (currentView as FlipClockView).updateMargins()
        }
    }

    /**
     * 进入沉浸式全屏模式
     */
    private fun enterFullscreenImmersive() {
        window?.apply {
            addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
            addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            statusBarColor = Color.TRANSPARENT
            navigationBarColor = Color.TRANSPARENT

            decorView.systemUiVisibility = (
                    View.SYSTEM_UI_FLAG_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                    View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_LOW_PROFILE
                    )
        }

        Log.d(TAG, "已进入沉浸式全屏模式")
    }

    override fun onDreamingStopped() {
        super.onDreamingStopped()
        Log.d(TAG, "屏保已停止")

        // 停止烧屏保护
        displayViewHelper?.stopBurnInProtection()
        displayViewHelper = null

        // 注销电池接收器
        batteryReceiver?.let {
            try {
                unregisterReceiver(it)
            } catch (e: Exception) {
                Log.e(TAG, "注销电池接收器失败", e)
            }
        }
        batteryReceiver = null

        currentView = null
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            enterFullscreenImmersive()
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        Log.d(TAG, "配置变化: ${if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) "横屏" else "竖屏"}")

        // 处理自动旋转
        rotationHelper?.onConfigurationChanged(newConfig, isAutoRotation)
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy")

        // 清理资源
        displayViewHelper?.stopBurnInProtection()
        displayViewHelper = null

        brightnessHelper?.cleanup()
        brightnessHelper = null
    }
}
