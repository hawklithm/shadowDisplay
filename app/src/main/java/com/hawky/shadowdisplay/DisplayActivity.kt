package com.hawky.shadowdisplay

import android.animation.ValueAnimator
import android.content.Context
import android.content.Intent
import android.graphics.*
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
import android.view.animation.DecelerateInterpolator
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import com.hawky.shadowdisplay.models.DisplayMode
import com.hawky.shadowdisplay.settings.SettingsManager
import com.hawky.shadowdisplay.views.FlipClockView
import java.util.Calendar
import kotlin.math.min

/**
 * 全屏显示Activity - 用于模拟息屏显示效果
 */
class DisplayActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "DisplayActivity"
        private const val EXTRA_DISPLAY_MODE = "display_mode"
        private const val EXTRA_AUTO_ROTATION = "auto_rotation"
        private const val EXTRA_AUTO_BRIGHTNESS = "auto_brightness"
        private const val EXTRA_BURN_IN_PROTECTION = "burn_in_protection"

        fun start(context: Context, mode: DisplayMode, autoRotation: Boolean,
                   autoBrightness: Boolean, burnInProtection: Boolean) {
            val intent = Intent(context, DisplayActivity::class.java).apply {
                putExtra(EXTRA_DISPLAY_MODE, mode.name)
                putExtra(EXTRA_AUTO_ROTATION, autoRotation)
                putExtra(EXTRA_AUTO_BRIGHTNESS, autoBrightness)
                putExtra(EXTRA_BURN_IN_PROTECTION, burnInProtection)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            context.startActivity(intent)
        }
    }

    private var displayView: DisplayView? = null
    private var flipClockView: FlipClockView? = null
    private lateinit var settings: SettingsManager
    private var displayMode: DisplayMode = DisplayMode.DIGITAL_CLOCK
    private var autoRotation = false
    private var autoBrightness = false
    private var burnInProtection = false

    private val handler = Handler(Looper.getMainLooper())
    private val updateRunnable = object : Runnable {
        override fun run() {
            displayView?.updateTime()
            handler.postDelayed(this, 1000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 读取参数
        displayMode = try {
            DisplayMode.valueOf(intent.getStringExtra(EXTRA_DISPLAY_MODE) ?: DisplayMode.DIGITAL_CLOCK.name)
        } catch (e: Exception) {
            Log.e(TAG, "解析显示模式失败", e)
            DisplayMode.DIGITAL_CLOCK
        }
        autoRotation = intent.getBooleanExtra(EXTRA_AUTO_ROTATION, false)
        autoBrightness = intent.getBooleanExtra(EXTRA_AUTO_BRIGHTNESS, false)
        burnInProtection = intent.getBooleanExtra(EXTRA_BURN_IN_PROTECTION, false)

        // 初始化设置管理器
        settings = SettingsManager.getInstance(this)

        // 创建显示视图
        if (displayMode == DisplayMode.FLIP_CLOCK) {
            // 翻页时钟使用专门的布局
            val layout = android.widget.FrameLayout(this).apply {
                setBackgroundColor(Color.BLACK)
            }
            flipClockView = FlipClockView(this).apply {
                layoutParams = android.widget.FrameLayout.LayoutParams(
                    android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                    android.widget.FrameLayout.LayoutParams.MATCH_PARENT
                )
            }

            // 添加边距容器，四周留5%空间
            val marginContainer = android.widget.FrameLayout(this).apply {
                setBackgroundColor(Color.BLACK)
                layoutParams = android.widget.FrameLayout.LayoutParams(
                    android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                    android.widget.FrameLayout.LayoutParams.MATCH_PARENT
                )
            }
            // 边距将在onSizeChanged中动态计算
            marginContainer.addView(flipClockView, android.widget.FrameLayout.LayoutParams(
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT
            ))

            layout.addView(marginContainer)
            setContentView(layout)
        } else {
            // 其他模式使用自定义绘制
            displayView = DisplayView(this, displayMode, burnInProtection)
            setContentView(displayView)
        }

        // 隐藏状态栏和导航栏（必须在setContentView之后调用）
        hideSystemUI()

        Log.d(TAG, "DisplayActivity创建完成，模式: ${displayMode.displayName}")
    }

    override fun onResume() {
        super.onResume()
        if (displayMode != DisplayMode.FLIP_CLOCK) {
            handler.post(updateRunnable)
            displayView?.startBurnInProtection()
        }
        Log.d(TAG, "DisplayActivity已恢复")
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(updateRunnable)
        displayView?.stopBurnInProtection()
        Log.d(TAG, "DisplayActivity已暂停")
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(updateRunnable)
        displayView?.stopBurnInProtection()
        Log.d(TAG, "DisplayActivity已销毁")
    }

    override fun onConfigurationChanged(newConfig: android.content.res.Configuration) {
        super.onConfigurationChanged(newConfig)
        displayView?.onConfigurationChanged()
        flipClockView?.updateMargins()
        Log.d(TAG, "屏幕方向改变")
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

    /**
     * 显示视图 - 负责绘制各种屏保模式
     */
    private inner class DisplayView(
        context: Context,
        private val mode: DisplayMode,
        private val burnInProtection: Boolean
    ) : View(context) {

        private val paint = Paint().apply {
            isAntiAlias = true
            color = Color.WHITE
        }

        private val backgroundPaint = Paint().apply {
            color = Color.BLACK
            style = Paint.Style.FILL
        }

        // 烧屏保护相关
        private var offsetX = 0f
        private var offsetY = 0f
        private val burnInHandler = Handler(Looper.getMainLooper())
        private var burnInRunnable: Runnable? = null

        init {
            keepScreenOn = true
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)

            // 绘制黑色背景
            canvas.drawPaint(backgroundPaint)

            // 应用烧屏保护偏移
            canvas.save()
            canvas.translate(offsetX, offsetY)

            // 绘制各种模式
            when (mode) {
                DisplayMode.DIGITAL_CLOCK -> drawDigitalClock(canvas)
                DisplayMode.ANALOG_CLOCK -> drawAnalogClock(canvas)
                DisplayMode.ROBOT_EYES_WALL_E -> drawRobotEyesWallE(canvas)
                DisplayMode.ROBOT_EYES_MINION -> drawRobotEyesMinion(canvas)
                DisplayMode.FLIP_CLOCK -> { /* FlipClockView单独处理 */ }
            }

            canvas.restore()
        }

        fun updateTime() {
            invalidate()
        }

        private fun drawDigitalClock(canvas: Canvas) {
            val calendar = Calendar.getInstance()
            val hour = calendar.get(Calendar.HOUR_OF_DAY)
            val minute = calendar.get(Calendar.MINUTE)
            val second = calendar.get(Calendar.SECOND)

            val centerX = width / 2f
            val centerY = height / 2f

            paint.textSize = min(width, height) * 0.15f
            paint.textAlign = Paint.Align.CENTER
            paint.color = Color.WHITE

            val timeText = String.format("%02d:%02d:%02d", hour, minute, second)
            canvas.drawText(timeText, centerX, centerY + paint.textSize / 3, paint)

            // 绘制日期
            paint.textSize = min(width, height) * 0.04f
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH) + 1
            val day = calendar.get(Calendar.DAY_OF_MONTH)
            val weekDay = arrayOf("周日", "周一", "周二", "周三", "周四", "周五", "周六")[calendar.get(Calendar.DAY_OF_WEEK) - 1]
            val dateText = String.format("%d年%d月%d日 %s", year, month, day, weekDay)
            canvas.drawText(dateText, centerX, centerY + paint.textSize * 4, paint)
        }

        private fun drawAnalogClock(canvas: Canvas) {
            val calendar = Calendar.getInstance()
            val hour = calendar.get(Calendar.HOUR)
            val minute = calendar.get(Calendar.MINUTE)
            val second = calendar.get(Calendar.SECOND)

            val centerX = width / 2f
            val centerY = height / 2f
            val radius = min(width, height) * 0.35f

            // 绘制表盘
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 4f
            paint.color = Color.WHITE
            canvas.drawCircle(centerX, centerY, radius, paint)

            // 绘制刻度
            for (i in 0 until 12) {
                val angle = (i * 30) * Math.PI / 180
                val startX = centerX + (radius - 20) * Math.sin(angle).toFloat()
                val startY = centerY - (radius - 20) * Math.cos(angle).toFloat()
                val endX = centerX + radius * Math.sin(angle).toFloat()
                val endY = centerY - radius * Math.cos(angle).toFloat()

                paint.strokeWidth = if (i % 3 == 0) 6f else 3f
                canvas.drawLine(startX, startY, endX, endY, paint)
            }

            // 绘制时针
            val hourAngle = ((hour + minute / 60.0) * 30) * Math.PI / 180
            paint.strokeWidth = 8f
            val hourX = centerX + (radius * 0.5f * Math.sin(hourAngle).toFloat())
            val hourY = centerY - (radius * 0.5f * Math.cos(hourAngle).toFloat())
            canvas.drawLine(centerX, centerY, hourX, hourY, paint)

            // 绘制分针
            val minuteAngle = (minute * 6) * Math.PI / 180
            paint.strokeWidth = 5f
            val minuteX = centerX + (radius * 0.7f * Math.sin(minuteAngle).toFloat())
            val minuteY = centerY - (radius * 0.7f * Math.cos(minuteAngle).toFloat())
            canvas.drawLine(centerX, centerY, minuteX, minuteY, paint)

            // 绘制秒针
            val secondAngle = (second * 6) * Math.PI / 180
            paint.strokeWidth = 2f
            paint.color = Color.RED
            val secondX = centerX + (radius * 0.8f * Math.sin(secondAngle).toFloat())
            val secondY = centerY - (radius * 0.8f * Math.cos(secondAngle).toFloat())
            canvas.drawLine(centerX, centerY, secondX, secondY, paint)

            // 绘制中心点
            paint.style = Paint.Style.FILL
            paint.color = Color.WHITE
            canvas.drawCircle(centerX, centerY, 8f, paint)
        }

        private fun drawRobotEyesWallE(canvas: Canvas) {
            val centerX = width / 2f
            val centerY = height / 2f
            val eyeRadius = min(width, height) * 0.15f

            // 绘制左眼
            drawWallEEye(canvas, centerX - eyeRadius * 1.5f, centerY, eyeRadius)

            // 绘制右眼
            drawWallEEye(canvas, centerX + eyeRadius * 1.5f, centerY, eyeRadius)
        }

        private fun drawWallEEye(canvas: Canvas, centerX: Float, centerY: Float, radius: Float) {
            // 外圈
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 6f
            paint.color = Color.WHITE
            canvas.drawCircle(centerX, centerY, radius, paint)

            // 内圈（瞳孔）
            paint.style = Paint.Style.FILL
            paint.color = Color.parseColor("#FFFF00")
            canvas.drawCircle(centerX, centerY, radius * 0.4f, paint)

            // 眼睛上的反光
            paint.color = Color.WHITE
            canvas.drawCircle(centerX - radius * 0.2f, centerY - radius * 0.2f, radius * 0.1f, paint)
        }

        private fun drawRobotEyesMinion(canvas: Canvas) {
            val centerX = width / 2f
            val centerY = height / 2f
            val eyeRadius = min(width, height) * 0.12f

            // 绘制左眼（单眼模式）
            drawMinionEye(canvas, centerX, centerY, eyeRadius)
        }

        private fun drawMinionEye(canvas: Canvas, centerX: Float, centerY: Float, radius: Float) {
            // 眼镜框
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 8f
            paint.color = Color.parseColor("#888888")
            canvas.drawCircle(centerX, centerY, radius, paint)

            // 眼白
            paint.style = Paint.Style.FILL
            paint.color = Color.WHITE
            canvas.drawCircle(centerX, centerY, radius * 0.85f, paint)

            // 瞳孔（棕色）
            paint.color = Color.parseColor("#8B4513")
            canvas.drawCircle(centerX, centerY, radius * 0.45f, paint)

            // 瞳孔中心（黑色）
            paint.color = Color.BLACK
            canvas.drawCircle(centerX, centerY, radius * 0.25f, paint)

            // 眼睛反光
            paint.color = Color.WHITE
            canvas.drawCircle(centerX - radius * 0.15f, centerY - radius * 0.15f, radius * 0.08f, paint)
        }

        fun onConfigurationChanged() {
            invalidate()
        }

        fun startBurnInProtection() {
            if (!burnInProtection) return

            burnInRunnable = object : Runnable {
                override fun run() {
                    // 随机偏移
                    offsetX = (Math.random() * 10 - 5).toFloat()
                    offsetY = (Math.random() * 10 - 5).toFloat()
                    invalidate()
                    burnInHandler.postDelayed(this, 30000)
                }
            }
            burnInHandler.post(burnInRunnable!!)
        }

        fun stopBurnInProtection() {
            burnInRunnable?.let {
                burnInHandler.removeCallbacks(it)
            }
            burnInRunnable = null
            offsetX = 0f
            offsetY = 0f
        }
    }
}
