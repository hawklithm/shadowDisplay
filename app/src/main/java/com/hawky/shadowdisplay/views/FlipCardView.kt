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

        // 数字大小为卡片高度的80%-85%，根据屏幕比例动态调整
        // 横屏时数字稍大，竖屏时数字稍小以适应窄屏
        val screenRatio = if (w > h) {
            0.85f // 横屏
        } else {
            0.78f // 竖屏
        }
        digitSize = h * screenRatio
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

        // 计算文字基线，使数字整体居中
        val textBounds = android.graphics.Rect()
        paint.getTextBounds(currentDigit.toString(), 0, 1, textBounds)

        // 数字整体居中（基线在中心线下方）
        val x = cardWidth / 2f
        val y = cardHeight / 2f + textBounds.height() / 2f - textBounds.bottom
        canvas.drawText(currentDigit.toString(), x, y, paint)
    }

    /**
     * 绘制翻页动画
     * 使用方案C：正确模拟3D旋转，正面和背面不会同时显示
     */
    private fun drawFlipAnimation(canvas: Canvas) {
        val centerY = cardHeight / 2
        val cornerRadius = min(cardWidth, cardHeight) * 0.08f
        val borderRadius = maxOf(cornerRadius, 15f) // 最小15px

        // 第一阶段：上半部分向下翻转（0-0.5）
        if (animationProgress < 0.5f) {
            val progress = animationProgress * 2 // 0-1

            // 绘制下半部分（数字A的下半部分，静止）
            drawStaticLowerHalf(canvas, currentDigit, centerY, borderRadius)

            // 绘制翻转的上半部分
            drawFlippingUpperHalf3D(canvas, currentDigit, targetDigit, progress, centerY, borderRadius)
        }
        // 第二阶段：下半部分向上展开（0.5-1）
        else {
            val progress = (animationProgress - 0.5f) * 2 // 0-1

            // 绘制上半部分（数字B的上半部分，静止）
            drawStaticUpperHalf(canvas, targetDigit, centerY, borderRadius)

            // 绘制展开的下半部分
            drawFlippingLowerHalf3D(canvas, targetDigit, progress, centerY, borderRadius)
        }
    }

    /**
     * 绘制静止的下半部分
     */
    private fun drawStaticLowerHalf(canvas: Canvas, digit: Int, centerY: Float, cornerRadius: Float) {
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
        val textBounds = android.graphics.Rect()
        paint.getTextBounds(digit.toString(), 0, 1, textBounds)
        val y = cardHeight / 2f + textBounds.height() / 2f - textBounds.bottom
        canvas.drawText(digit.toString(), cardWidth / 2, y, paint)
        
        canvas.restore()
    }

    /**
     * 绘制静止的上半部分
     */
    private fun drawStaticUpperHalf(canvas: Canvas, digit: Int, centerY: Float, cornerRadius: Float) {
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
        val textBounds = android.graphics.Rect()
        paint.getTextBounds(digit.toString(), 0, 1, textBounds)
        val y = cardHeight / 2f + textBounds.height() / 2f - textBounds.bottom
        canvas.drawText(digit.toString(), cardWidth / 2, y, paint)
        
        canvas.restore()
    }

    /**
     * 绘制翻转的上半部分（3D旋转模拟）
     * Progress 0-0.5：显示正面（数字A）
     * Progress 0.5-1.0：显示背面（数字B）
     */
    private fun drawFlippingUpperHalf3D(canvas: Canvas, oldDigit: Int, newDigit: Int, progress: Float, centerY: Float, cornerRadius: Float) {
        // 正面旋转：progress 0 → 0.5，角度 0° → 90°
        // 背面旋转：progress 0.5 → 1，角度 90° → 0°
        
        if (progress <= 0.5f) {
            // 显示正面（数字A的上半部分）
            val frontProgress = progress * 2 // 0-1
            drawFlapFront(canvas, oldDigit, frontProgress, centerY, cornerRadius, isUpper = true)
        } else {
            // 显示背面（数字B的上半部分）
            val backProgress = (progress - 0.5f) * 2 // 0-1
            drawFlapBack(canvas, newDigit, backProgress, centerY, cornerRadius)
        }
    }

    /**
     * 绘制展开的下半部分（3D旋转模拟）
     * Progress 0-1：从折叠状态展开
     */
    private fun drawFlippingLowerHalf3D(canvas: Canvas, digit: Int, progress: Float, centerY: Float, cornerRadius: Float) {
        // 下半部分展开：progress 0 → 1，角度 90° → 0°
        // 显示数字B的下半部分
        drawFlapFront(canvas, digit, progress, centerY, cornerRadius, isUpper = false)
    }

    /**
     * 绘制 flap 的正面
     * isUpper: true=上半部分向下翻，false=下半部分向上展开
     */
    private fun drawFlapFront(canvas: Canvas, digit: Int, progress: Float, centerY: Float, cornerRadius: Float, isUpper: Boolean) {
        // 计算透视缩放
        // progress 0 → 1，透视宽度从 100% → 0%（上半部分）或 0% → 100%（下半部分）
        val perspectiveFactor = if (isUpper) {
            1f - progress * 0.7f // 从 1 变到 0.3
        } else {
            0.3f + progress * 0.7f // 从 0.3 变到 1
        }
        
        val topWidth = cardWidth * perspectiveFactor
        val topX = (cardWidth - topWidth) / 2
        val halfWidth = cardWidth / 2
        
        // 绘制梯形 flap
        val path = Path()
        if (isUpper) {
            // 上半部分：上窄下宽
            path.moveTo(topX, 0f)
            path.lineTo(topX + topWidth, 0f)
            path.lineTo(halfWidth + cardWidth / 2, centerY)
            path.lineTo(halfWidth - cardWidth / 2, centerY)
        } else {
            // 下半部分：上窄下宽
            path.moveTo(halfWidth - topWidth / 2, centerY)
            path.lineTo(halfWidth + topWidth / 2, centerY)
            path.lineTo(halfWidth + cardWidth / 2, cardHeight)
            path.lineTo(halfWidth - cardWidth / 2, cardHeight)
        }
        path.close()
        
        canvas.save()
        canvas.clipPath(path)
        
        // 绘制背景
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#000000")
            style = Paint.Style.FILL
        }
        val rectTop = if (isUpper) 0f else centerY
        val rectBottom = if (isUpper) centerY else cardHeight
        canvas.drawRect(0f, rectTop, cardWidth, rectBottom, bgPaint)
        
        // 绘制数字
        paint.color = Color.WHITE
        val textBounds = android.graphics.Rect()
        paint.getTextBounds(digit.toString(), 0, 1, textBounds)
        val y = cardHeight / 2f + textBounds.height() / 2f - textBounds.bottom
        canvas.drawText(digit.toString(), cardWidth / 2, y, paint)
        
        canvas.restore()
    }

    /**
     * 绘制 flap 的背面
     * 只用于上半部分翻转
     */
    private fun drawFlapBack(canvas: Canvas, digit: Int, progress: Float, centerY: Float, cornerRadius: Float) {
        // 背面展开：progress 0 → 1，透视宽度从 30% → 100%
        val perspectiveFactor = 0.3f + progress * 0.7f
        
        val topWidth = cardWidth * perspectiveFactor
        val topX = (cardWidth - topWidth) / 2
        val halfWidth = cardWidth / 2
        
        // 绘制梯形 flap（背面）
        val path = Path()
        path.moveTo(topX, 0f)
        path.lineTo(topX + topWidth, 0f)
        path.lineTo(halfWidth + cardWidth / 2, centerY)
        path.lineTo(halfWidth - cardWidth / 2, centerY)
        path.close()
        
        canvas.save()
        canvas.clipPath(path)
        
        // 绘制背景
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#000000")
            style = Paint.Style.FILL
        }
        canvas.drawRect(0f, 0f, cardWidth, centerY, bgPaint)
        
        // 绘制数字（背面的数字是镜像的，但我们不镜像，直接显示）
        paint.color = Color.WHITE
        val textBounds = android.graphics.Rect()
        paint.getTextBounds(digit.toString(), 0, 1, textBounds)
        val y = cardHeight / 2f + textBounds.height() / 2f - textBounds.bottom
        canvas.drawText(digit.toString(), cardWidth / 2, y, paint)
        
        canvas.restore()
    }
}
