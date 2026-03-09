package com.hawky.shadowdisplay

import android.animation.ValueAnimator
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.graphics.*
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import com.hawky.shadowdisplay.models.DisplayMode
import com.hawky.shadowdisplay.settings.SettingsManager
import com.hawky.shadowdisplay.utils.BrightnessHelper
import com.hawky.shadowdisplay.utils.DisplayViewHelper
import com.hawky.shadowdisplay.utils.RotationHelper
import com.hawky.shadowdisplay.views.AnalogClockView
import com.hawky.shadowdisplay.views.FlipClockView
import com.hawky.shadowdisplay.views.RobotEyesView
import java.util.Calendar
import kotlin.math.min

/**
 * 全屏显示Activity - 用于模拟息屏显示效果
 * 统一支持所有显示模式的：自动旋转、自动亮度、烧屏保护
 */
class DisplayActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "DisplayActivity"
        private const val EXTRA_DISPLAY_MODE = "display_mode"

        fun start(context: Context, mode: DisplayMode) {
            val intent = Intent(context, DisplayActivity::class.java).apply {
                putExtra(EXTRA_DISPLAY_MODE, mode.name)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            context.startActivity(intent)
        }
    }

    // 显示视图
    private var currentView: View? = null
    private var flipClockView: FlipClockView? = null

    // 设置
    private lateinit var settings: SettingsManager
    private var displayMode: DisplayMode = DisplayMode.DIGITAL_CLOCK

    // 辅助类
    private var displayViewHelper: DisplayViewHelper? = null
    private var brightnessHelper: BrightnessHelper? = null
    private var rotationHelper: RotationHelper? = null

    // 电池接收器
    private var batteryReceiver: BroadcastReceiver? = null

    // 状态
    private var isAutoRotation = false
    private var isAutoBrightness = false
    private var isBurnInProtection = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 读取显示模式
        displayMode = try {
            DisplayMode.valueOf(intent.getStringExtra(EXTRA_DISPLAY_MODE) ?: DisplayMode.DIGITAL_CLOCK.name)
        } catch (e: Exception) {
            Log.e(TAG, "解析显示模式失败", e)
            DisplayMode.DIGITAL_CLOCK
        }

        // 初始化设置
        settings = SettingsManager.getInstance(this)
        val currentSettings = settings.getSettings()
        isAutoRotation = currentSettings.autoRotation
        isAutoBrightness = currentSettings.autoBrightness
        isBurnInProtection = currentSettings.burnInProtection

        // 创建显示视图
        createDisplayView()

        // 隐藏状态栏和导航栏
        hideSystemUI()

        Log.d(TAG, "DisplayActivity创建完成，模式: ${displayMode.displayName}")
    }

    /**
     * 创建显示视图
     */
    private fun createDisplayView() {
        // 创建根布局
        val rootLayout = FrameLayout(this).apply {
            setBackgroundColor(Color.BLACK)
        }

        // 根据显示模式创建对应的视图
        currentView = when (displayMode) {
            DisplayMode.DIGITAL_CLOCK -> DigitalClockView(this).apply {
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                )
            }

            DisplayMode.ANALOG_CLOCK -> AnalogClockView(this).apply {
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                )
            }

            DisplayMode.FLIP_CLOCK -> FlipClockView(this).apply {
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                ).apply {
                    gravity = android.view.Gravity.CENTER
                }
            }

            DisplayMode.ROBOT_EYES_WALL_E -> RobotEyesView(this).apply {
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                )
            }

            DisplayMode.ROBOT_EYES_MINION -> RobotEyesView(this).apply {
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                )
            }
        }

        // 保存FlipClockView引用
        flipClockView = currentView as? FlipClockView

        // 添加到布局
        currentView?.let { view ->
            if (displayMode == DisplayMode.FLIP_CLOCK) {
                // FlipClockView 已经有自己的边距处理
                rootLayout.addView(view)
            } else {
                // 其他模式添加边距容器
                val marginContainer = FrameLayout(this).apply {
                    setBackgroundColor(Color.BLACK)
                    layoutParams = FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT
                    )
                }
                val screenSize = min(rootLayout.width.takeIf { it > 0 } ?: 800, rootLayout.height.takeIf { it > 0 } ?: 800)
                val margin = (screenSize * 0.05f).toInt()
                marginContainer.addView(view, FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                ).apply {
                    setMargins(margin, margin, margin, margin)
                })
                rootLayout.addView(marginContainer)
            }
        }

        setContentView(rootLayout)

        // 应用显示设置
        applyDisplaySettings()

        Log.d(TAG, "已创建视图: ${displayMode.displayName}")
    }

    /**
     * 应用显示设置（亮度、烧屏保护、旋转）
     */
    private fun applyDisplaySettings() {
        // 初始化亮度辅助类
        brightnessHelper = BrightnessHelper(this, window)
        brightnessHelper?.initialize()
        brightnessHelper?.applyBrightnessSettings()

        // 初始化旋转辅助类
        rotationHelper = RotationHelper { isLandscape ->
            updateViewOrientation(isLandscape)
        }

        // 启动烧屏保护
        if (isBurnInProtection) {
            currentView?.let {
                displayViewHelper = DisplayViewHelper(it)
                displayViewHelper?.startBurnInProtection()
            }
        }

        // 注册电池接收器
        registerBatteryReceiver()
    }

    /**
     * 注册电池接收器
     */
    private fun registerBatteryReceiver() {
        batteryReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
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
     * 更新视图的横竖屏状态
     */
    private fun updateViewOrientation(isLandscape: Boolean) {
        when (currentView) {
            is DigitalClockView -> (currentView as DigitalClockView).setLandscape(isLandscape)
            is AnalogClockView -> (currentView as AnalogClockView).setLandscape(isLandscape)
            is RobotEyesView -> (currentView as RobotEyesView).setLandscape(isLandscape)
            is FlipClockView -> flipClockView?.updateMargins()
        }
        Log.d(TAG, "更新视图方向: ${if (isLandscape) "横屏" else "竖屏"}")
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "DisplayActivity已恢复")
    }

    override fun onPause() {
        super.onPause()
        Log.d(TAG, "DisplayActivity已暂停")
    }

    override fun onDestroy() {
        super.onDestroy()

        // 停止烧屏保护
        displayViewHelper?.stopBurnInProtection()
        displayViewHelper = null

        // 清理亮度辅助类
        brightnessHelper?.cleanup()
        brightnessHelper = null

        // 注销电池接收器
        batteryReceiver?.let {
            try {
                unregisterReceiver(it)
            } catch (e: Exception) {
                Log.e(TAG, "注销电池接收器失败", e)
            }
        }
        batteryReceiver = null

        Log.d(TAG, "DisplayActivity已销毁")
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)

        // 处理自动旋转
        rotationHelper?.onConfigurationChanged(newConfig, isAutoRotation)

        Log.d(TAG, "屏幕方向改变: ${if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) "横屏" else "竖屏"}")
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        // 点击任意位置退出
        if (event.action == MotionEvent.ACTION_DOWN) {
            finish()
            return true
        }
        return super.onTouchEvent(event)
    }

    private fun hideSystemUI() {
        // 隐藏ActionBar
        supportActionBar?.hide()

        // 隐藏状态栏和导航栏
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.let {
                it.hide(WindowInsets.Type.systemBars())
                it.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            )
            @Suppress("DEPRECATION")
            window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }
}
