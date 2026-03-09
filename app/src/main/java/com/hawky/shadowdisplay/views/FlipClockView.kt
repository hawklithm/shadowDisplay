package com.hawky.shadowdisplay.views

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import kotlin.math.min
import java.util.*

/**
 * 翻页时钟视图
 * 自适应横竖屏，确保数字完整显示
 */
class FlipClockView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    companion object {
        private const val TAG = "FlipClockView"
        private const val UPDATE_INTERVAL = 1000L // 每秒更新一次
    }

    // 卡片视图
    private lateinit var hourTensCard: FlipCardView
    private lateinit var hourUnitsCard: FlipCardView
    private lateinit var minuteTensCard: FlipCardView
    private lateinit var minuteUnitsCard: FlipCardView

    // 分隔符
    private lateinit var colonView: View

    // 时间状态
    private var lastHour = -1
    private var lastMinute = -1
    private var lastSecond = -1

    // 计算后的卡片尺寸
    private var calculatedCardWidth = 0
    private var calculatedCardHeight = 0
    private var calculatedColonWidth = 0
    private var calculatedMargin = 0

    // 更新任务
    private val updateRunnable = object : Runnable {
        override fun run() {
            updateTime()
            postDelayed(this, UPDATE_INTERVAL)
        }
    }

    init {
        setWillNotDraw(false)
        setBackgroundColor(Color.BLACK)
        Log.d(TAG, "FlipClockView created")

        // 小时十位
        hourTensCard = createFlipCard(context)
        addView(hourTensCard)

        // 小时个位
        hourUnitsCard = createFlipCard(context)
        addView(hourUnitsCard)

        // 分隔符 - 固定宽度
        colonView = createColonView(context)
        addView(colonView)

        // 分钟十位
        minuteTensCard = createFlipCard(context)
        addView(minuteTensCard)

        // 分钟个位
        minuteUnitsCard = createFlipCard(context)
        addView(minuteUnitsCard)
    }

    /**
     * 创建翻页卡片
     */
    private fun createFlipCard(context: Context): FlipCardView {
        return FlipCardView(context).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            )
            setWillNotDraw(false)
        }
    }

    /**
     * 动态调整卡片间距和尺寸
     */
    fun updateLayoutParams() {
        if (width <= 0 || height <= 0) return

        val isLandscape = width > height

        // 边距：至少5%
        val margin = (min(width, height) * 0.05f).toInt()

        // 可用空间（减去边距）
        val availableWidth = width - margin * 2
        val availableHeight = height - margin * 2

        // 卡片数量：4个数字
        val cardCount = 4

        // 分隔符和间距
        val cardSpacing = dpToPx(6)
        val totalSpacing = cardSpacing * (cardCount - 1)

        Log.d(TAG, "========== updateLayoutParams 开始 ==========")
        Log.d(TAG, "设备尺寸: 宽=${width}px, 高=${height}px")
        Log.d(TAG, "屏幕方向: ${if (isLandscape) "横屏" else "竖屏"}")
        Log.d(TAG, "边距: ${margin}px (5%)")
        Log.d(TAG, "可用空间: 宽=${availableWidth}px, 高=${availableHeight}px")
        Log.d(TAG, "卡片数量: $cardCount, 卡片间距: ${cardSpacing}px")
        Log.d(TAG, "总间距(不含冒号): ${totalSpacing}px")

        // 策略：单个卡片宽高比固定为1:2（宽:高）
        // 整个时钟 = 4个卡片 + 3个间距 + 冒号，宽高比约为2:1
        // 冒号宽度约为单个卡片宽度的0.4倍

        // 根据可用宽度和高度计算卡片尺寸
        var cardWidth = 0
        var cardHeight = 0
        var colonWidth = 0

        if (isLandscape) {
            Log.d(TAG, "--- 横屏计算 ---")
            // 横屏：主要受高度限制
            // 卡片高 = 可用高度
            cardHeight = availableHeight
            Log.d(TAG, "初始卡片高度 = 可用高度: $cardHeight")
            // 卡片宽 = 卡片高 / 2
            cardWidth = cardHeight / 2
            Log.d(TAG, "初始卡片宽度 = 高度/2: $cardWidth")
            // 冒号宽度约为卡片宽的0.4倍
            colonWidth = (cardWidth * 0.4f).toInt()
            Log.d(TAG, "初始冒号宽度 = 卡片宽*0.4: $colonWidth")

            // 检查是否超出横向空间
            val totalClockWidth = cardWidth * cardCount + totalSpacing + colonWidth
            Log.d(TAG, "总时钟宽度 = ${cardWidth}*$cardCount + $totalSpacing + $colonWidth = $totalClockWidth")
            Log.d(TAG, "可用宽度 = $availableWidth")

            if (totalClockWidth > availableWidth) {
                // 需要缩小
                val scale = availableWidth.toFloat() / totalClockWidth
                Log.d(TAG, "超出横向空间，缩小比例 = $scale")
                cardWidth = (cardWidth * scale).toInt()
                cardHeight = (cardHeight * scale).toInt()
                colonWidth = (colonWidth * scale).toInt()
            } else {
                // 横向有剩余，可以放大到充分利用空间
                val scale = availableWidth.toFloat() / totalClockWidth
                Log.d(TAG, "横向有剩余，放大比例 = $scale")
                cardWidth = (cardWidth * scale).toInt()
                cardHeight = (cardHeight * scale).toInt()
                colonWidth = (colonWidth * scale).toInt()
                // 确保放大后不超过可用高度
                if (cardHeight > availableHeight) {
                    val scaleHeight = availableHeight.toFloat() / cardHeight
                    Log.d(TAG, "放大后超出高度，再次缩小比例 = $scaleHeight")
                    cardWidth = (cardWidth * scaleHeight).toInt()
                    cardHeight = availableHeight
                    colonWidth = (colonWidth * scaleHeight).toInt()
                }
            }
        } else {
            Log.d(TAG, "--- 竖屏计算 ---")
            // 竖屏：主要受宽度限制
            // 4个卡片宽 + 3个间距 + 冒号 = 可用宽度
            // 假设冒号 = 0.4 * 卡片宽
            // 4.4 * 卡片宽 + 3个间距 = 可用宽度
            cardWidth = ((availableWidth - totalSpacing) / 4.4f).toInt()
            Log.d(TAG, "计算卡片宽度 = ($availableWidth - $totalSpacing) / 4.4 = $cardWidth")
            // 卡片高 = 卡片宽 * 2
            cardHeight = cardWidth * 2
            Log.d(TAG, "卡片高度 = 卡片宽 * 2 = $cardHeight")
            // 冒号宽度
            colonWidth = (cardWidth * 0.4f).toInt()
            Log.d(TAG, "冒号宽度 = 卡片宽 * 0.4 = $colonWidth")

            // 检查是否超出纵向空间
            if (cardHeight > availableHeight) {
                // 需要缩小
                val scale = availableHeight.toFloat() / cardHeight
                Log.d(TAG, "超出纵向空间，缩小比例 = $scale")
                cardWidth = (cardWidth * scale).toInt()
                cardHeight = availableHeight
                colonWidth = (colonWidth * scale).toInt()
            } else {
                Log.d(TAG, "纵向空间充足，无需缩小")
            }
        }

        Log.d(TAG, "---------- 最终结果 ----------")
        Log.d(TAG, "卡片宽度: $cardWidth px")
        Log.d(TAG, "卡片高度: $cardHeight px")
        Log.d(TAG, "冒号宽度: $colonWidth px")
        Log.d(TAG, "卡片宽高比: ${String.format("%.2f", cardWidth.toFloat() / cardHeight)}")
        Log.d(TAG, "========== updateLayoutParams 结束 ==========")

        // 保存计算结果，供onLayout使用
        calculatedCardWidth = cardWidth
        calculatedCardHeight = cardHeight
        calculatedColonWidth = colonWidth
        calculatedMargin = margin

        // 更新自己的边距（FlipClockView自身的边距，保持5%边距）
        val parent = parent as? FrameLayout
        if (parent != null) {
            val layoutParams = this.layoutParams as? FrameLayout.LayoutParams
            if (layoutParams != null) {
                layoutParams.setMargins(margin, margin, margin, margin)
                layoutParams.gravity = Gravity.CENTER
                parent.updateViewLayout(this, layoutParams)
            }
        }

        // 触发布局更新，使onLayout被调用
        requestLayout()
    }

    /**
     * 更新边距以适应屏幕尺寸（兼容旧调用）
     */
    fun updateMargins() {
        updateLayoutParams()
    }

    /**
     * 动态调整卡片间距（兼容旧调用）
     */
    fun updateCardMargins() {
        updateLayoutParams()
    }

    /**
     * 创建分隔符
     */
    private fun createColonView(context: Context): View {
        return View(context).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            )
            setWillNotDraw(false)
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        Log.d(TAG, "onSizeChanged: w=$w, h=$h, oldw=$oldw, oldh=$oldh")
        // 更新边距和卡片间距
        updateMargins()
        updateCardMargins()
        // 立即更新一次时间
        updateTime()
    }

    /**
     * 手动测量布局
     * 确保子视图按照计算好的尺寸进行测量
     */
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        // 如果计算结果可用，则手动测量子视图
        if (calculatedCardWidth > 0 && calculatedCardHeight > 0) {
            val cardSpacing = dpToPx(6)

            // 计算总宽度
            val totalWidth = calculatedCardWidth * 4 + cardSpacing * 3 + calculatedColonWidth

            // 手动测量每个卡片
            hourTensCard.measure(
                MeasureSpec.makeMeasureSpec(calculatedCardWidth, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(calculatedCardHeight, MeasureSpec.EXACTLY)
            )
            hourUnitsCard.measure(
                MeasureSpec.makeMeasureSpec(calculatedCardWidth, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(calculatedCardHeight, MeasureSpec.EXACTLY)
            )
            minuteTensCard.measure(
                MeasureSpec.makeMeasureSpec(calculatedCardWidth, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(calculatedCardHeight, MeasureSpec.EXACTLY)
            )
            minuteUnitsCard.measure(
                MeasureSpec.makeMeasureSpec(calculatedCardWidth, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(calculatedCardHeight, MeasureSpec.EXACTLY)
            )

            // 测量冒号
            colonView.measure(
                MeasureSpec.makeMeasureSpec(calculatedColonWidth, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(calculatedCardHeight, MeasureSpec.EXACTLY)
            )

            // 设置ViewGroup的测量尺寸
            setMeasuredDimension(totalWidth, calculatedCardHeight)
        } else {
            // 如果计算结果不可用，使用默认测量
            super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        }
    }

    /**
     * 手动布局子视图
     */
    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        if (calculatedCardWidth <= 0 || calculatedCardHeight <= 0) return

        val cardSpacing = dpToPx(6)
        val currentX = 0
        val currentY = 0

        // 计算每个卡片的x坐标
        val hourTensX = currentX
        val hourUnitsX = hourTensX + calculatedCardWidth + cardSpacing
        val colonX = hourUnitsX + calculatedCardWidth + cardSpacing
        val minuteTensX = colonX + calculatedColonWidth + cardSpacing
        val minuteUnitsX = minuteTensX + calculatedCardWidth + cardSpacing

        // 布局每个卡片
        hourTensCard.layout(hourTensX, currentY, hourTensX + calculatedCardWidth, currentY + calculatedCardHeight)
        hourUnitsCard.layout(hourUnitsX, currentY, hourUnitsX + calculatedCardWidth, currentY + calculatedCardHeight)
        colonView.layout(colonX, currentY, colonX + calculatedColonWidth, currentY + calculatedCardHeight)
        minuteTensCard.layout(minuteTensX, currentY, minuteTensX + calculatedCardWidth, currentY + calculatedCardHeight)
        minuteUnitsCard.layout(minuteUnitsX, currentY, minuteUnitsX + calculatedCardWidth, currentY + calculatedCardHeight)

        Log.d(TAG, "onLayout: 卡片布局完成，totalX=${minuteUnitsX + calculatedCardWidth}, totalY=$calculatedCardHeight")
    }

    /**
     * 开始时钟更新
     */
    fun start() {
        updateTime()
        post(updateRunnable)
    }

    /**
     * 停止时钟更新
     */
    fun stop() {
        removeCallbacks(updateRunnable)
    }

    /**
     * 更新时间显示
     */
    private fun updateTime() {
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)
        val second = calendar.get(Calendar.SECOND)

        // 更新小时（24小时制）
        updateHour(hour)

        // 更新分钟
        updateMinute(minute)
    }

    /**
     * 更新小时显示
     */
    private fun updateHour(hour: Int) {
        if (hour == lastHour) return

        val hour24 = hour
        val tens = hour24 / 10
        val units = hour24 % 10

        Log.d(TAG, "updateHour: hour=$hour, tens=$tens, units=$units")

        hourTensCard.setDigit(tens, hourTensCard.isAttachedToWindow)
        hourUnitsCard.setDigit(units, hourUnitsCard.isAttachedToWindow)

        lastHour = hour
    }

    /**
     * 更新分钟显示
     */
    private fun updateMinute(minute: Int) {
        if (minute == lastMinute) return

        val tens = minute / 10
        val units = minute % 10

        Log.d(TAG, "updateMinute: minute=$minute, tens=$tens, units=$units")

        minuteTensCard.setDigit(tens, minuteTensCard.isAttachedToWindow)
        minuteUnitsCard.setDigit(units, minuteUnitsCard.isAttachedToWindow)

        lastMinute = minute
    }

    /**
     * dp转px
     */
    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        start()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        stop()
    }

    override fun onDraw(canvas: android.graphics.Canvas) {
        super.onDraw(canvas)

        // 绘制分隔符（冒号）
        drawColons(canvas)
    }

    /**
     * 绘制冒号分隔符
     */
    private fun drawColons(canvas: android.graphics.Canvas) {
        val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = android.graphics.Paint.Style.FILL
        }

        // 冒号大小根据卡片高度计算
        val cardHeight = if (colonView.height > 0) colonView.height.toFloat() else height * 0.6f
        val radius = cardHeight * 0.08f

        // 冒号位置居中
        val centerX = colonView.x + colonView.width / 2f
        val centerY = height / 2f

        // 绘制冒号（两个点）
        canvas.drawCircle(centerX, centerY - cardHeight * 0.15f, radius, paint)
        canvas.drawCircle(centerX, centerY + cardHeight * 0.15f, radius, paint)
    }
}
