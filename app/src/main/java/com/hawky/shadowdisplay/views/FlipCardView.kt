package com.hawky.shadowdisplay.views

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.view.animation.DecelerateInterpolator
import kotlin.math.min

/**
 * 翻页卡片 - 单个数字的翻页效果
 */
class FlipCardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    companion object {
        private const val TAG = "FlipCardView"
        private const val ANIMATION_DURATION = 600L
    }

    init {
        setWillNotDraw(false)
        Log.d(TAG, "FlipCardView created")
    }

    // 绘制相关
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textAlign = Paint.Align.CENTER
        typeface = Typeface.MONOSPACE
    }

    private val cardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#1A1A1A")
        style = Paint.Style.FILL
    }

    private val dividerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        style = Paint.Style.FILL
    }

    // 当前数字和目标数字
    private var currentDigit = 0
    private var targetDigit = 0
    private var isAnimating = false
    private var animationProgress = 0f

    // 卡片尺寸
    private var cardWidth = 0f
    private var cardHeight = 0f
    private var digitSize = 0f

    /**
     * 设置数字（带动画）
     */
    fun setDigit(digit: Int, animate: Boolean = true) {
        if (digit == currentDigit) return

        targetDigit = digit

        if (animate && isAttachedToWindow) {
            startFlipAnimation()
        } else {
            currentDigit = targetDigit
            invalidate()
        }
    }

    /**
     * 立即设置数字（无动画）
     */
    fun setDigitImmediate(digit: Int) {
        currentDigit = digit
        targetDigit = digit
        isAnimating = false
        animationProgress = 0f
        invalidate()
    }

    /**
     * 开始翻页动画
     */
    private fun startFlipAnimation() {
        isAnimating = true
        animationProgress = 0f

        ValueAnimator.ofFloat(0f, 1f).apply {
            duration = ANIMATION_DURATION
            interpolator = DecelerateInterpolator()
            addUpdateListener { animation ->
                animationProgress = animation.animatedValue as Float
                invalidate()
            }
            addListener(object : android.animation.AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: android.animation.Animator) {
                    currentDigit = targetDigit
                    isAnimating = false
                    invalidate()
                }
            })
            start()
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        Log.d(TAG, "onSizeChanged: w=$w, h=$h, oldw=$oldw, oldh=$oldh")

        // 计算卡片尺寸
        cardWidth = w.toFloat()
        cardHeight = h.toFloat()

        // 数字大小为卡片宽度的70%
        digitSize = min(w, h) * 0.7f
        paint.textSize = digitSize

        // 立即设置初始数字
        setDigitImmediate(currentDigit)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        Log.d(TAG, "onDraw: currentDigit=$currentDigit, targetDigit=$targetDigit, isAnimating=$isAnimating")

        // 绘制卡片背景
        drawCardBackground(canvas)

        if (isAnimating) {
            // 绘制翻页动画
            drawFlipAnimation(canvas)
        } else {
            // 绘制静态数字
            drawStaticDigit(canvas)
        }
    }

    /**
     * 绘制卡片背景
     */
    private fun drawCardBackground(canvas: Canvas) {
        // 绘制背景
        canvas.drawRect(0f, 0f, cardWidth, cardHeight, cardPaint)

        // 绘制中间分割线
        val dividerY = cardHeight / 2
        canvas.drawRect(0f, dividerY - 2f, cardWidth, dividerY + 2f, dividerPaint)

        // 绘制边框（白色）
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.STROKE
            strokeWidth = 3f
        }
        canvas.drawRect(1f, 1f, cardWidth - 1, cardHeight - 1, borderPaint)
    }

    /**
     * 绘制静态数字
     */
    private fun drawStaticDigit(canvas: Canvas) {
        paint.color = Color.WHITE

        // 上半部分
        canvas.save()
        canvas.clipRect(0f, 0f, cardWidth, cardHeight / 2)
        canvas.drawText(
            currentDigit.toString(),
            cardWidth / 2,
            cardHeight / 4 + digitSize / 3,
            paint
        )
        canvas.restore()

        // 下半部分
        canvas.save()
        canvas.clipRect(0f, cardHeight / 2, cardWidth, cardHeight)
        canvas.drawText(
            currentDigit.toString(),
            cardWidth / 2,
            cardHeight * 3 / 4 + digitSize / 3,
            paint
        )
        canvas.restore()
    }

    /**
     * 绘制翻页动画
     */
    private fun drawFlipAnimation(canvas: Canvas) {
        val centerY = cardHeight / 2

        // 第一阶段：上半部分翻转（0-0.5）
        if (animationProgress < 0.5f) {
            val progress = animationProgress * 2 // 0-1

            // 绘制下半部分（旧的数字）
            canvas.save()
            canvas.clipRect(0f, centerY, cardWidth, cardHeight)
            paint.color = Color.WHITE
            canvas.drawText(
                currentDigit.toString(),
                cardWidth / 2,
                centerY + digitSize / 3,
                paint
            )
            canvas.restore()

            // 绘制翻转的上半部分
            drawFlippingUpperHalf(canvas, currentDigit, targetDigit, progress)
        }
        // 第二阶段：下半部分翻转（0.5-1）
        else {
            val progress = (animationProgress - 0.5f) * 2 // 0-1

            // 绘制上半部分（新的数字）
            canvas.save()
            canvas.clipRect(0f, 0f, cardWidth, centerY)
            paint.color = Color.WHITE
            canvas.drawText(
                targetDigit.toString(),
                cardWidth / 2,
                centerY - digitSize / 6,
                paint
            )
            canvas.restore()

            // 绘制翻转的下半部分
            drawFlippingLowerHalf(canvas, currentDigit, targetDigit, progress)
        }
    }

    /**
     * 绘制翻转的上半部分
     */
    private fun drawFlippingUpperHalf(canvas: Canvas, oldDigit: Int, newDigit: Int, progress: Float) {
        val centerY = cardHeight / 2

        canvas.save()

        // 创建翻转效果
        val scaleY = 1f - progress
        canvas.scale(1f, scaleY, cardWidth / 2, centerY)

        // 绘制背景
        val flipPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#1A1A1A")
            style = Paint.Style.FILL
        }
        canvas.drawRect(0f, 0f, cardWidth, centerY, flipPaint)

        // 绘制旧的数字上半部分
        canvas.save()
        canvas.clipRect(0f, 0f, cardWidth, centerY)
        paint.color = Color.WHITE
        canvas.drawText(
            oldDigit.toString(),
            cardWidth / 2,
            centerY - digitSize / 6,
            paint
        )
        canvas.restore()

        // 绘制新的数字（在翻转过程中逐渐出现）
        if (progress > 0.3f) {
            canvas.save()
            canvas.clipRect(0f, centerY - cardHeight * progress, cardWidth, centerY)
            paint.color = Color.WHITE
            canvas.drawText(
                newDigit.toString(),
                cardWidth / 2,
                centerY - digitSize / 6,
                paint
            )
            canvas.restore()
        }

        canvas.restore()
    }

    /**
     * 绘制翻转的下半部分
     */
    private fun drawFlippingLowerHalf(canvas: Canvas, oldDigit: Int, newDigit: Int, progress: Float) {
        val centerY = cardHeight / 2

        canvas.save()

        // 创建翻转效果
        val scaleY = progress
        canvas.scale(1f, scaleY, cardWidth / 2, centerY)

        // 绘制背景
        val flipPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#1A1A1A")
            style = Paint.Style.FILL
        }
        canvas.drawRect(0f, centerY, cardWidth, cardHeight, flipPaint)

        // 绘制新的数字下半部分
        canvas.save()
        canvas.clipRect(0f, centerY, cardWidth, cardHeight)
        paint.color = Color.WHITE
        canvas.drawText(
            newDigit.toString(),
            cardWidth / 2,
            centerY + digitSize / 3,
            paint
        )
        canvas.restore()

        canvas.restore()
    }
}
