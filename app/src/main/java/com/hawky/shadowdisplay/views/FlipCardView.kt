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

        // 数字大小为卡片高度的85%（更饱满）
        digitSize = h * 0.85f
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
        val cornerRadius = 20f

        // 绘制背景（纯黑色，带圆角）
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#000000")
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(0f, 0f, cardWidth, cardHeight, cornerRadius, cornerRadius, bgPaint)

        // 绘制中间分割线（非常细的黑色，几乎不可见）
        val dividerY = cardHeight / 2
        canvas.drawRect(0f, dividerY - 1f, cardWidth, dividerY + 1f, dividerPaint)

        // 绘制边框（很细的深灰色边框，带圆角）
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#1A1A1A")
            style = Paint.Style.STROKE
            strokeWidth = 2f
        }
        canvas.drawRoundRect(1f, 1f, cardWidth - 1, cardHeight - 1, cornerRadius, cornerRadius, borderPaint)
    }

    /**
     * 绘制静态数字
     */
    private fun drawStaticDigit(canvas: Canvas) {
        paint.color = Color.WHITE

        // 计算文字基线，使数字居中
        val textBounds = android.graphics.Rect()
        paint.getTextBounds(currentDigit.toString(), 0, 1, textBounds)

        // 绘制数字（垂直和水平都居中）
        val x = cardWidth / 2f
        val y = cardHeight / 2f + (textBounds.height() / 2).toFloat()
        canvas.drawText(currentDigit.toString(), x, y, paint)
    }

    /**
     * 绘制翻页动画
     */
    private fun drawFlipAnimation(canvas: Canvas) {
        val centerY = cardHeight / 2
        val cornerRadius = 20f
        val textBounds = android.graphics.Rect()
        paint.getTextBounds(currentDigit.toString(), 0, 1, textBounds)

        // 第一阶段：上半部分向下翻转（0-0.5）
        if (animationProgress < 0.5f) {
            val progress = animationProgress * 2 // 0-1

            // 绘制下半部分（数字A的下半部分，静止）
            canvas.save()
            canvas.clipRect(0f, centerY, cardWidth, cardHeight)
            val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#000000")
                style = Paint.Style.FILL
            }
            canvas.drawRoundRect(0f, centerY, cardWidth, cardHeight, cornerRadius, cornerRadius, bgPaint)
            val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#1A1A1A")
                style = Paint.Style.STROKE
                strokeWidth = 2f
            }
            canvas.drawRoundRect(1f, centerY + 1, cardWidth - 1, cardHeight - 1, cornerRadius, cornerRadius, borderPaint)
            paint.color = Color.WHITE
            val y = centerY + (textBounds.height() / 2).toFloat()
            canvas.drawText(currentDigit.toString(), cardWidth / 2, y, paint)
            canvas.restore()

            // 绘制翻转的上半部分（使用3D透视模拟）
            drawFlippingUpperHalfWithPerspective(canvas, currentDigit, targetDigit, progress, textBounds)
        }
        // 第二阶段：下半部分向上展开（0.5-1）
        else {
            val progress = (animationProgress - 0.5f) * 2 // 0-1

            // 绘制上半部分（数字B的上半部分，静止）
            canvas.save()
            canvas.clipRect(0f, 0f, cardWidth, centerY)
            val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#000000")
                style = Paint.Style.FILL
            }
            canvas.drawRoundRect(0f, 0f, cardWidth, centerY, cornerRadius, cornerRadius, bgPaint)
            val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#1A1A1A")
                style = Paint.Style.STROKE
                strokeWidth = 2f
            }
            canvas.drawRoundRect(1f, 1f, cardWidth - 1, centerY - 1, cornerRadius, cornerRadius, borderPaint)
            paint.color = Color.WHITE
            val y = centerY - (textBounds.height() / 2).toFloat()
            canvas.drawText(targetDigit.toString(), cardWidth / 2, y, paint)
            canvas.restore()

            // 绘制翻转的下半部分（使用3D透视模拟）
            drawFlippingLowerHalfWithPerspective(canvas, targetDigit, progress, textBounds)
        }
    }

    /**
     * 绘制翻转的上半部分（使用3D透视模拟）
     * 第一阶段：上半部分向下翻转
     * - flap的正面显示数字A的上半部分（随翻转逐渐变小）
     * - flap的背面显示数字B的上半部分（随翻转逐渐变大）
     */
    private fun drawFlippingUpperHalfWithPerspective(canvas: Canvas, oldDigit: Int, newDigit: Int, progress: Float, textBounds: android.graphics.Rect) {
        val centerY = cardHeight / 2
        val halfWidth = cardWidth / 2

        // 计算梯形透视：上窄下宽
        // 随着翻转progress（0-1），梯形逐渐变小
        val shrinkFactor = 1f - progress * 0.6f // 从1变到0.4
        val topWidth = cardWidth * shrinkFactor
        val topX = (cardWidth - topWidth) / 2

        // 使用 Path 绘制梯形 flap
        val path = android.graphics.Path()
        path.moveTo(topX, 0f)
        path.lineTo(topX + topWidth, 0f)
        path.lineTo(halfWidth, centerY)
        path.lineTo(halfWidth - topWidth / 2, centerY)
        path.close()

        canvas.save()

        // 裁剪为梯形区域
        canvas.clipPath(path)

        // 绘制背景（flap正面 - 数字A）
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#000000")
            style = Paint.Style.FILL
        }
        canvas.drawRect(0f, 0f, cardWidth, centerY, bgPaint)

        // 绘制数字A的上半部分（正面）
        canvas.save()
        canvas.clipRect(0f, 0f, cardWidth, centerY)
        paint.color = Color.WHITE
        val y = centerY - (textBounds.height() / 2).toFloat()
        canvas.drawText(oldDigit.toString(), cardWidth / 2, y, paint)
        canvas.restore()

        canvas.restore()

        // 绘制背面（数字B）- 随着翻转逐渐显露
        if (progress > 0.3f) {
            val backProgress = (progress - 0.3f) / 0.7f // 0-1

            canvas.save()

            // 背面的梯形是逐渐展开的
            val backShrinkFactor = 0.4f + backProgress * 0.6f // 从0.4变到1
            val backTopWidth = cardWidth * backShrinkFactor
            val backTopX = (cardWidth - backTopWidth) / 2

            val backPath = android.graphics.Path()
            backPath.moveTo(backTopX, 0f)
            backPath.lineTo(backTopX + backTopWidth, 0f)
            backPath.lineTo(halfWidth, centerY)
            backPath.lineTo(halfWidth - backTopWidth / 2, centerY)
            backPath.close()

            canvas.clipPath(backPath)

            // 绘制背景（flap背面 - 数字B）
            val bgPaint2 = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#000000")
                style = Paint.Style.FILL
            }
            canvas.drawRect(0f, 0f, cardWidth, centerY, bgPaint2)

            // 绘制数字B的上半部分（背面）
            canvas.save()
            canvas.clipRect(0f, 0f, cardWidth, centerY)
            paint.color = Color.WHITE
            val y2 = centerY - (textBounds.height() / 2).toFloat()
            canvas.drawText(newDigit.toString(), cardWidth / 2, y2, paint)
            canvas.restore()

            canvas.restore()
        }
    }

    /**
     * 绘制翻转的下半部分（使用3D透视模拟）
     * 第二阶段：下半部分向上展开
     * - flap显示数字B的下半部分，从折叠状态逐渐展开
     */
    private fun drawFlippingLowerHalfWithPerspective(canvas: Canvas, newDigit: Int, progress: Float, textBounds: android.graphics.Rect) {
        val centerY = cardHeight / 2
        val halfWidth = cardWidth / 2

        // 计算梯形透视：下宽上窄
        // 随着展开progress（0-1），梯形逐渐变大
        val expandFactor = 0.4f + progress * 0.6f // 从0.4变到1
        val bottomWidth = cardWidth * expandFactor
        val bottomX = (cardWidth - bottomWidth) / 2

        // 使用 Path 绘制梯形 flap
        val path = android.graphics.Path()
        path.moveTo(bottomX, centerY)
        path.lineTo(bottomX + bottomWidth, centerY)
        path.lineTo(halfWidth, cardHeight)
        path.lineTo(halfWidth - bottomWidth / 2, cardHeight)
        path.close()

        canvas.save()

        // 裁剪为梯形区域
        canvas.clipPath(path)

        // 绘制背景（flap - 数字B）
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#000000")
            style = Paint.Style.FILL
        }
        canvas.drawRect(0f, centerY, cardWidth, cardHeight, bgPaint)

        // 绘制数字B的下半部分
        canvas.save()
        canvas.clipRect(0f, centerY, cardWidth, cardHeight)
        paint.color = Color.WHITE
        val y = centerY + (textBounds.height() / 2).toFloat()
        canvas.drawText(newDigit.toString(), cardWidth / 2, y, paint)
        canvas.restore()

        canvas.restore()
    }
}
