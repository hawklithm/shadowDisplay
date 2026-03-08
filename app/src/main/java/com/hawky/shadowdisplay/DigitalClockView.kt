package com.hawky.shadowdisplay

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.util.Log
import android.view.View
import com.hawky.shadowdisplay.settings.SettingsManager
import java.util.*

/**
 * 数字时钟视图
 * 低功耗时钟显示，支持时间、日期、电量显示
 * 支持横竖屏自适应
 */
class DigitalClockView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    companion object {
        private const val TAG = "DigitalClockView"
        private const val TARGET_FPS = 1 // 数字时钟只需要1fps
        private const val FRAME_DELAY_MS = 1000 / TARGET_FPS
    }

    // 画笔
    private val timePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.FILL
        textAlign = Paint.Align.CENTER
    }

    private val datePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.FILL
        textAlign = Paint.Align.CENTER
    }

    // 状态
    private var isLandscape = false
    private var centerX = 0f
    private var centerY = 0f
    private var timeTextSize = 0f

    // 设置
    private var settings: SettingsManager? = null

    // 电量接收器
    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val level = intent?.getIntExtra(android.os.BatteryManager.EXTRA_LEVEL, 0) ?: 0
            val status = intent?.getIntExtra(android.os.BatteryManager.EXTRA_STATUS, android.os.BatteryManager.BATTERY_STATUS_UNKNOWN)
                ?: android.os.BatteryManager.BATTERY_STATUS_UNKNOWN
            batteryLevel = level
            isCharging = status == android.os.BatteryManager.BATTERY_STATUS_CHARGING ||
                         status == android.os.BatteryManager.BATTERY_STATUS_FULL
            invalidate()
        }
    }

    // 数据
    private var batteryLevel = 0
    private var isCharging = false
    private var currentHour = 0
    private var currentMinute = 0
    private var currentSecond = 0

    init {
        // 初始化将在onAttachedToWindow中进行
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        // 初始化设置
        if (settings == null) {
            settings = SettingsManager.getInstance(context)
        }
        registerBatteryReceiver()
    }

    fun setLandscape(landscape: Boolean) {
        isLandscape = landscape
        calculateDimensions()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldh, oldh)
        calculateDimensions()
    }

    private fun calculateDimensions() {
        val width = width.toFloat()
        val height = height.toFloat()

        if (isLandscape) {
            // 横屏：水平居中，填充屏幕高度60%
            timeTextSize = height * 0.4f
            centerX = width / 2f
            centerY = height / 2f
        } else {
            // 竖屏：垂直居中，填充屏幕宽度80%
            timeTextSize = width * 0.25f
            centerX = width / 2f
            centerY = height / 2f
        }

        // 应用用户设置的大小调整
        val currentSettings = settings?.getSettings()
        if (currentSettings != null) {
            timeTextSize *= currentSettings.digitalClockSize
        }

        timePaint.textSize = timeTextSize
        datePaint.textSize = timeTextSize * 0.3f
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // 绘制纯黑背景（省电）
        canvas.drawColor(Color.BLACK)

        updateTime()

        drawTime(canvas)
        drawDate(canvas)
        drawBattery(canvas)

        // 计划重绘（控制帧率）
        postInvalidateDelayed(FRAME_DELAY_MS.toLong())
    }

    private fun updateTime() {
        val calendar = Calendar.getInstance()
        currentHour = calendar.get(Calendar.HOUR_OF_DAY)
        currentMinute = calendar.get(Calendar.MINUTE)
        currentSecond = calendar.get(Calendar.SECOND)
    }

    private fun drawTime(canvas: Canvas) {
        val timeText = String.format("%02d:%02d", currentHour, currentMinute)
        canvas.drawText(timeText, centerX, centerY + timeTextSize / 3, timePaint)
    }

    private fun drawDate(canvas: Canvas) {
        val currentSettings = settings?.getSettings() ?: return
        if (!currentSettings.digitalClockShowDate && !currentSettings.digitalClockShowWeekday) return

        val calendar = Calendar.getInstance()
        val dateParts = mutableListOf<String>()

        if (currentSettings.digitalClockShowDate) {
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH) + 1
            val day = calendar.get(Calendar.DAY_OF_MONTH)
            dateParts.add("$year-$month-$day")
        }

        if (currentSettings.digitalClockShowWeekday) {
            val weekdays = arrayOf("周日", "周一", "周二", "周三", "周四", "周五", "周六")
            dateParts.add(weekdays[calendar.get(Calendar.DAY_OF_WEEK) - 1])
        }

        if (dateParts.isNotEmpty()) {
            val dateText = dateParts.joinToString(" ")
            canvas.drawText(dateText, centerX, centerY + timeTextSize * 0.8f, datePaint)
        }
    }

    private fun drawBattery(canvas: Canvas) {
        val batteryText = if (isCharging) {
            "⚡$batteryLevel%"
        } else {
            "$batteryLevel%"
        }
        canvas.drawText(batteryText, centerX, centerY - timeTextSize * 0.5f, datePaint)
    }

    private fun registerBatteryReceiver() {
        val filter = IntentFilter().apply {
            addAction(android.content.Intent.ACTION_BATTERY_CHANGED)
        }
        context.registerReceiver(batteryReceiver, filter)
        Log.d(TAG, "电池状态监听器已注册")
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        try {
            context.unregisterReceiver(batteryReceiver)
        } catch (e: Exception) {
            Log.e(TAG, "注销电池接收器失败", e)
        }
    }
}
