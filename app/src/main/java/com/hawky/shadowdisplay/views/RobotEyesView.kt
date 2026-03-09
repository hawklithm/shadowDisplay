package com.hawky.shadowdisplay.views

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.View
import com.hawky.shadowdisplay.models.DisplayMode
import com.hawky.shadowdisplay.settings.SettingsManager
import java.util.*

/**
 * 机器人眼睛视图
 * 支持瓦力和小黄人两种风格
 */
class RobotEyesView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    companion object {
        private const val TAG = "RobotEyesView"
        private const val TARGET_FPS = 30
        private const val FRAME_DELAY_MS = 1000 / TARGET_FPS
    }

    // 眼睛风格
    enum class EyeStyle {
        WALL_E,   // 瓦力风格
        MINION    // 小黄人风格
    }

    // 画笔
    private val eyePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.FILL
    }

    private val pupilPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        style = Paint.Style.FILL
    }

    private val highlightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.FILL
    }

    private val clockPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.FILL
        textAlign = Paint.Align.CENTER
        textSize = 48f
    }

    // 状态
    private var eyeStyle = EyeStyle.WALL_E
    private var isLandscape = false

    // 瞳孔位置 (-1.0 到 1.0)
    private var leftPupilX = 0f
    private var leftPupilY = 0f
    private var rightPupilX = 0f
    private var rightPupilY = 0f

    // 眨眼 (1.0 = 完全睁开, 0.0 = 完全闭合)
    private var blinkFactor = 1.0f
    private var isBlinking = false

    // 眼睛布局
    private var leftEyeRect = RectF()
    private var rightEyeRect = RectF()

    // Handler
    private val handler = Handler(Looper.getMainLooper())
    private var gazeRunnable: Runnable? = null
    private var blinkRunnable: Runnable? = null

    // 设置
    private var settings: SettingsManager? = null

    init {
        // 初始化将在onAttachedToWindow中进行
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        // 初始化设置
        if (settings == null) {
            settings = SettingsManager.getInstance(context)
        }

        // 根据显示模式设置眼睛风格
        val displayMode = settings?.getSettings()?.displayMode ?: DisplayMode.ROBOT_EYES_WALL_E
        eyeStyle = when (displayMode) {
            DisplayMode.ROBOT_EYES_WALL_E -> EyeStyle.WALL_E
            DisplayMode.ROBOT_EYES_MINION -> EyeStyle.MINION
            else -> EyeStyle.WALL_E
        }

        // 启动动画
        startRandomBehaviors()
    }

    fun setEyeStyle(style: EyeStyle) {
        eyeStyle = style
        invalidate()
    }

    fun setLandscape(landscape: Boolean) {
        isLandscape = landscape
        invalidate()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        calculateEyePositions()
    }

    private fun calculateEyePositions() {
        val width = width.toFloat()
        val height = height.toFloat()

        if (isLandscape) {
            // 横屏：眼睛居中显示
            val eyeHeight = height * 0.5f
            val eyeWidth = eyeHeight * 0.8f // 宽高比

            val eyeSpacing = eyeWidth * 0.2f
            val totalWidth = eyeWidth * 2 + eyeSpacing
            val startX = (width - totalWidth) / 2f
            val centerY = height / 2f

            leftEyeRect = RectF(startX, centerY - eyeHeight / 2f, startX + eyeWidth, centerY + eyeHeight / 2f)
            rightEyeRect = RectF(startX + eyeWidth + eyeSpacing, centerY - eyeHeight / 2f,
                startX + eyeWidth + eyeSpacing + eyeWidth, centerY + eyeHeight / 2f)
        } else {
            // 竖屏：眼睛居中显示，适当靠上一点为数字时钟留空间
            val eyeWidth = width * 0.3f // 每只眼睛占30%
            val eyeHeight = eyeWidth * 1.2f // 宽高比

            val eyeSpacing = eyeWidth * 0.2f
            val totalWidth = eyeWidth * 2 + eyeSpacing
            val startX = (width - totalWidth) / 2f
            val centerY = height * 0.4f // 眼睛中心在屏幕40%的位置

            leftEyeRect = RectF(startX, centerY - eyeHeight / 2f, startX + eyeWidth, centerY + eyeHeight / 2f)
            rightEyeRect = RectF(startX + eyeWidth + eyeSpacing, centerY - eyeHeight / 2f,
                startX + eyeWidth + eyeSpacing + eyeWidth, centerY + eyeHeight / 2f)
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // 绘制黑色背景
        canvas.drawColor(Color.BLACK)

        // 绘制眼睛
        drawEye(canvas, leftEyeRect, leftPupilX, leftPupilY)
        drawEye(canvas, rightEyeRect, rightPupilX, rightPupilY)

        // 绘制数字时钟（右下角）
        drawDigitalClock(canvas)

        // 计划下一次绘制
        postInvalidateDelayed(FRAME_DELAY_MS.toLong())
    }

    private fun drawEye(canvas: Canvas, eyeRect: RectF, pupilOffsetX: Float, pupilOffsetY: Float) {
        // 应用眨眼效果
        val blinkingRect = RectF(eyeRect)
        if (blinkFactor < 1.0f) {
            val heightChange = eyeRect.height() * (1 - blinkFactor) / 2
            blinkingRect.top += heightChange
            blinkingRect.bottom -= heightChange
        }

        // 绘制眼白
        canvas.drawOval(blinkingRect, eyePaint)

        // 计算瞳孔位置
        val centerX = blinkingRect.centerX()
        val centerY = blinkingRect.centerY()
        val maxOffset = blinkingRect.width() * 0.2f

        val pupilX = centerX + pupilOffsetX * maxOffset
        val pupilY = centerY + pupilOffsetY * maxOffset

        // 绘制瞳孔
        val pupilRadius = blinkingRect.width() * 0.15f
        canvas.drawCircle(pupilX, pupilY, pupilRadius, pupilPaint)

        // 绘制高光（仅小黄人风格）
        if (eyeStyle == EyeStyle.MINION) {
            val highlightRadius = pupilRadius * 0.3f
            canvas.drawCircle(
                pupilX - pupilRadius * 0.3f,
                pupilY - pupilRadius * 0.3f,
                highlightRadius,
                highlightPaint
            )
        }
    }

    private fun drawDigitalClock(canvas: Canvas) {
        val currentSettings = settings?.getSettings()
        if (currentSettings == null || (!currentSettings.digitalClockShowDate && !currentSettings.digitalClockShowWeekday)) {
            // 不显示日期和星期时，只显示时间
        }

        // 计算时钟位置（右下角）
        val clockTextSize = Math.sqrt((width * width + height * height).toDouble()) * 0.03
        clockPaint.textSize = clockTextSize.toFloat()

        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)
        val timeText = String.format("%02d:%02d", hour, minute)

        val textX = (width - clockTextSize * 3).toFloat()
        val textY = (height - clockTextSize * 1.5f).toFloat()

        canvas.drawText(timeText, textX, textY, clockPaint)
    }

    private fun startRandomBehaviors() {
        startGazeAnimation()
        startBlinkAnimation()
    }

    private fun startGazeAnimation() {
        val currentSettings = settings?.getSettings()
        val delay = currentSettings?.eyesGazeFrequency?.toLong()?.times(1000L) ?: 5000L

        gazeRunnable = object : Runnable {
            override fun run() {
                movePupilsRandomly()
                handler.postDelayed(this, delay)
            }
        }
        handler.postDelayed(gazeRunnable!!, 1000)
    }

    private fun startBlinkAnimation() {
        val currentSettings = settings?.getSettings()
        val delay = currentSettings?.eyesBlinkFrequency?.toLong()?.times(1000L) ?: 10000L

        blinkRunnable = object : Runnable {
            override fun run() {
                performBlink()
                handler.postDelayed(this, delay)
            }
        }
        handler.postDelayed(blinkRunnable!!, 2000)
    }

    private fun movePupilsRandomly() {
        // 生成新的随机目标
        val newLeftX = (Math.random() * 2 - 1).toFloat() // -1到1
        val newLeftY = (Math.random() * 2 - 1).toFloat()
        val newRightX = (Math.random() * 2 - 1).toFloat()
        val newRightY = (Math.random() * 2 - 1).toFloat()

        // 使用ValueAnimator平滑移动
        val animator = ValueAnimator.ofFloat(0f, 1f)
        animator.duration = 800
        animator.interpolator = android.view.animation.AccelerateDecelerateInterpolator()

        val startLeftX = leftPupilX
        val startLeftY = leftPupilY
        val startRightX = rightPupilX
        val startRightY = rightPupilY

        animator.addUpdateListener { animation ->
            val fraction = animation.animatedValue as Float
            leftPupilX = startLeftX + (newLeftX - startLeftX) * fraction
            leftPupilY = startLeftY + (newLeftY - startLeftY) * fraction
            rightPupilX = startRightX + (newRightX - startRightX) * fraction
            rightPupilY = startRightY + (newRightY - startRightY) * fraction
            invalidate()
        }

        animator.start()
    }

    private fun performBlink() {
        if (isBlinking) return

        isBlinking = true
        val blinkAnimator = ValueAnimator.ofFloat(1.0f, 0.0f, 1.0f)
        blinkAnimator.duration = 300
        blinkAnimator.interpolator = android.view.animation.AccelerateDecelerateInterpolator()

        blinkAnimator.addUpdateListener { animation ->
            blinkFactor = animation.animatedValue as Float
            invalidate()
        }

        blinkAnimator.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                isBlinking = false
            }
        })

        blinkAnimator.start()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        // 停止动画
        gazeRunnable?.let { handler.removeCallbacks(it) }
        blinkRunnable?.let { handler.removeCallbacks(it) }
    }
}
