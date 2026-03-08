package com.hawky.shadowdisplay.views

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import java.util.*

/**
 * 指针时钟视图
 */
class AnalogClockView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    companion object {
        private const val TAG = "AnalogClockView"
        private const val TARGET_FPS = 5 // 秒针动画需要5fps
        private const val FRAME_DELAY_MS = 1000 / TARGET_FPS
    }

    // 画笔
    private val dialPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 4f
        color = Color.WHITE
    }

    private val markerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.WHITE
    }

    private val hourHandPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.WHITE
    }

    private val minuteHandPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.WHITE
    }

    private val secondHandPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2f
        color = Color.RED
    }

    private val centerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.WHITE
    }

    // 状态
    private var isLandscape = false
    private var clockRadius = 0f
    private var centerX = 0f
    private var centerY = 0f

    // 平滑秒针
    private var smoothSeconds = true

    init {
        // 平滑秒针动画
        smoothSeconds = true
    }

    fun setLandscape(landscape: Boolean) {
        isLandscape = landscape
        invalidate()
    }

    fun setSmoothSeconds(smooth: Boolean) {
        smoothSeconds = smooth
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        calculateClockDimensions()
    }

    private fun calculateClockDimensions() {
        val width = width.toFloat()
        val height = height.toFloat()

        if (isLandscape) {
            // 横屏：椭圆形表盘，宽高比1.5:1
            clockRadius = height * 0.4f
            centerX = width / 2f
            centerY = height / 2f
        } else {
            // 竖屏：圆形表盘
            clockRadius = (Math.min(width, height) * 0.35f).toFloat()
            centerX = width / 2f
            centerY = height / 2f
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // 绘制黑色背景
        canvas.drawColor(Color.BLACK)

        // 绘制表盘
        drawClockFace(canvas)

        // 计算指针角度
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)
        val second = calendar.get(Calendar.SECOND)
        val millisecond = calendar.get(Calendar.MILLISECOND)

        // 绘制指针
        drawHourHand(canvas, hour, minute)
        drawMinuteHand(canvas, minute, second)
        drawSecondHand(canvas, second, millisecond)

        // 绘制中心点
        canvas.drawCircle(centerX, centerY, 8f, centerPaint)

        // 计划下一次绘制
        postInvalidateDelayed(FRAME_DELAY_MS.toLong())
    }

    private fun drawClockFace(canvas: Canvas) {
        // 绘制外圈
        dialPaint.strokeWidth = 4f
        canvas.drawOval(
            centerX - clockRadius,
            centerY - clockRadius,
            centerX + clockRadius,
            centerY + clockRadius,
            dialPaint
        )

        // 绘制内圈
        dialPaint.strokeWidth = 2f
        canvas.drawOval(
            centerX - clockRadius * 0.95f,
            centerY - clockRadius * 0.95f,
            centerX + clockRadius * 0.95f,
            centerY + clockRadius * 0.95f,
            dialPaint
        )

        // 绘制刻度
        for (i in 0 until 12) {
            val angle = Math.toRadians((i * 30 - 90).toDouble()).toFloat()
            val isMainHour = i % 3 == 0

            val startRadius = if (isMainHour) clockRadius * 0.85f else clockRadius * 0.9f
            val endRadius = clockRadius * 0.95f

            val startX = centerX + Math.cos(angle.toDouble()).toFloat() * startRadius
            val startY = centerY + Math.sin(angle.toDouble()).toFloat() * startRadius
            val endX = centerX + Math.cos(angle.toDouble()).toFloat() * endRadius
            val endY = centerY + Math.sin(angle.toDouble()).toFloat() * endRadius

            dialPaint.strokeWidth = if (isMainHour) 3f else 1.5f
            canvas.drawLine(startX, startY, endX, endY, dialPaint)
        }
    }

    private fun drawHourHand(canvas: Canvas, hour: Int, minute: Int) {
        val hourAngle = ((hour % 12 + minute / 60.0) * 30 - 90).toFloat()
        val hourAngleRad = Math.toRadians(hourAngle.toDouble()).toFloat()

        val handLength = clockRadius * 0.5f
        val endX = centerX + Math.cos(hourAngleRad.toDouble()).toFloat() * handLength
        val endY = centerY + Math.sin(hourAngleRad.toDouble()).toFloat() * handLength

        // 绘制时针
        hourHandPaint.strokeWidth = 8f
        canvas.drawLine(centerX, centerY, endX, endY, hourHandPaint)
    }

    private fun drawMinuteHand(canvas: Canvas, minute: Int, second: Int) {
        val minuteAngle = ((minute + second / 60.0) * 6 - 90).toFloat()
        val minuteAngleRad = Math.toRadians(minuteAngle.toDouble()).toFloat()

        val handLength = clockRadius * 0.7f
        val endX = centerX + Math.cos(minuteAngleRad.toDouble()).toFloat() * handLength
        val endY = centerY + Math.sin(minuteAngleRad.toDouble()).toFloat() * handLength

        // 绘制分针
        minuteHandPaint.strokeWidth = 5f
        canvas.drawLine(centerX, centerY, endX, endY, minuteHandPaint)
    }

    private fun drawSecondHand(canvas: Canvas, second: Int, millisecond: Int) {
        val secondAngle: Float
        if (smoothSeconds) {
            secondAngle = ((second + millisecond / 1000.0) * 6 - 90).toFloat()
        } else {
            secondAngle = (second * 6 - 90).toFloat()
        }

        val secondAngleRad = Math.toRadians(secondAngle.toDouble()).toFloat()

        val handLength = clockRadius * 0.8f
        val endX = centerX + Math.cos(secondAngleRad.toDouble()).toFloat() * handLength
        val endY = centerY + Math.sin(secondAngleRad.toDouble()).toFloat() * handLength

        // 绘制秒针
        secondHandPaint.strokeWidth = 2f
        canvas.drawLine(centerX, centerY, endX, endY, secondHandPaint)

        // 秒针尾部
        val tailLength = clockRadius * 0.15f
        val tailX = centerX - Math.cos(secondAngleRad.toDouble()).toFloat() * tailLength
        val tailY = centerY - Math.sin(secondAngleRad.toDouble()).toFloat() * tailLength
        canvas.drawLine(centerX, centerY, tailX, tailY, secondHandPaint)
    }
}
