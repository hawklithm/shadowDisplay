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
import com.hawky.shadowdisplay.DigitalClockView
import com.hawky.shadowdisplay.models.DisplayMode
import com.hawky.shadowdisplay.models.TriggerMode
import com.hawky.shadowdisplay.settings.SettingsManager
import com.hawky.shadowdisplay.views.AnalogClockView
import com.hawky.shadowdisplay.views.RobotEyesView

/**
 * 趣味屏保服务
 * 支持4种显示模式：数字时钟、指针时钟、瓦力眼睛、小黄人眼睛
 * 支持功耗优化：自动亮度、烧屏保护、低电量检测
 */
class LockScreenDreamService : DreamService() {

    companion object {
        private const val TAG = "FunScreenSaver"
    }

    private var currentView: View? = null
    private val settings = SettingsManager.getInstance(this)
    private var powerOptimization: PowerOptimizationHelper? = null

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "FunScreenSaver onCreate")
        
        // 初始化功耗优化
        powerOptimization = PowerOptimizationHelper(this, settings)
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
        powerOptimization?.checkBatteryStatus()

        // 设置窗口属性
        setupWindow()

        // 应用亮度设置
        powerOptimization?.applyBrightnessSettings()

        // 创建显示视图
        createContentView()

        // 进入沉浸式全屏模式
        enterFullscreenImmersive()

        // 启动烧屏保护
        if (settings.getSettings().burnInProtection) {
            currentView?.let { powerOptimization?.startBurnInProtection(it) }
        }

        Log.d(TAG, "屏保设置完成")
    }

    /**
     * 检查是否应该显示屏保
     */
    private fun shouldShowScreensaver(): Boolean {
        val settings = settings.getSettings()
        val triggerMode = settings.triggerMode

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
     * 设置窗口属性
     */
    private fun setupWindow() {
        isInteractive = false  // 不需要交互
    }

    /**
     * 创建显示视图
     */
    private fun createContentView() {
        val settings = settings.getSettings()
        val displayMode = settings.displayMode

        currentView = when (displayMode) {
            DisplayMode.DIGITAL_CLOCK -> DigitalClockView(this)
            DisplayMode.ANALOG_CLOCK -> AnalogClockView(this)
            DisplayMode.FLIP_CLOCK -> DigitalClockView(this)  // 翻页时钟暂时使用数字时钟
            DisplayMode.ROBOT_EYES_WALL_E -> RobotEyesView(this).apply {
                setEyeStyle(RobotEyesView.EyeStyle.WALL_E)
            }
            DisplayMode.ROBOT_EYES_MINION -> RobotEyesView(this).apply {
                setEyeStyle(RobotEyesView.EyeStyle.MINION)
            }
        }

        // 设置横竖屏
        val isLandscape = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        when (currentView) {
            is DigitalClockView -> (currentView as DigitalClockView).setLandscape(isLandscape)
            is AnalogClockView -> (currentView as AnalogClockView).setLandscape(isLandscape)
            is RobotEyesView -> (currentView as RobotEyesView).setLandscape(isLandscape)
        }

        setContentView(currentView)

        // 设置窗口背景为黑色
        window?.decorView?.setBackgroundColor(Color.BLACK)

        Log.d(TAG, "已创建视图: ${displayMode.displayName}")
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

        // 如果开启了自动旋转，更新视图布局
        if (settings.getSettings().autoRotation) {
            updateViewOrientation()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "DreamService onDestroy")
        
        // 清理功耗优化资源
        powerOptimization?.cleanup()
    }

    /**
     * 更新视图的横竖屏状态
     */
    private fun updateViewOrientation() {
        val isLandscape = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        Log.d(TAG, "更新视图方向: ${if (isLandscape) "横屏" else "竖屏"}")

        when (currentView) {
            is DigitalClockView -> (currentView as DigitalClockView).setLandscape(isLandscape)
            is AnalogClockView -> (currentView as AnalogClockView).setLandscape(isLandscape)
            is RobotEyesView -> (currentView as RobotEyesView).setLandscape(isLandscape)
        }
    }
}
