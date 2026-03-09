package com.hawky.shadowdisplay.views

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.util.Log
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
        // 目标：在保持比例的前提下，最大化利用屏幕空间

        // 根据可用宽度和高度计算卡片尺寸
        var cardWidth = 0
        var cardHeight = 0
        var colonWidth = 0

        if (isLandscape) {
            Log.d(TAG, "--- 横屏计算 ---")
            // 横屏策略：
            // 1. 先按高度限制计算：卡片高 = 可用高度，卡片宽 = 高/2
            // 2. 检查横向是否足够
            // 3. 如果横向有剩余，放大；如果横向不足，缩小
            // 4. 放大/缩小时保持1:2比例

            // 方案A：按高度限制
            var cardHeightByHeight = availableHeight
            var cardWidthByHeight = cardHeightByHeight / 2
            var colonWidthByHeight = (cardWidthByHeight * 0.4f).toInt()
            var totalWidthByHeight = cardWidthByHeight * cardCount + totalSpacing + colonWidthByHeight

            // 方案B：按宽度限制（假设时钟宽度占可用宽度的90%，留10%边距）
            var totalWidthByWidth = availableWidth * 0.9f
            var cardWidthByWidth = ((totalWidthByWidth - totalSpacing) / 4.4f).toInt()
            var cardHeightByWidth = cardWidthByWidth * 2
            var colonWidthByWidth = (cardWidthByWidth * 0.4f).toInt()

            Log.d(TAG, "方案A（按高度）：cardW=$cardWidthByHeight, cardH=$cardHeightByHeight, totalW=$totalWidthByHeight")
            Log.d(TAG, "方案B（按宽度）：cardW=$cardWidthByWidth, cardH=$cardHeightByWidth, totalW=$totalWidthByWidth")

            // 选择满足约束条件的最大方案
            if (totalWidthByHeight <= availableWidth) {
                // 方案A满足横向约束，使用方案A
                cardWidth = cardWidthByHeight
                cardHeight = cardHeightByHeight
                colonWidth = colonWidthByHeight
                Log.d(TAG, "选择方案A（按高度），完全利用纵向空间")
            } else if (cardHeightByWidth <= availableHeight) {
                // 方案B满足纵向约束，使用方案B
                cardWidth = cardWidthByWidth
                cardHeight = cardHeightByWidth
                colonWidth = colonWidthByWidth
                Log.d(TAG, "选择方案B（按宽度），充分利用横向空间")
            } else {
                // 两个方案都不满足，选择较小的那个
                if (totalWidthByHeight - availableWidth < availableHeight - cardHeightByWidth) {
                    cardWidth = cardWidthByHeight
                    cardHeight = cardHeightByHeight
                    colonWidth = colonWidthByHeight
                    Log.d(TAG, "方案A超出较少，选择方案A后缩小")
                    val scale = availableWidth.toFloat() / totalWidthByHeight
                    cardWidth = (cardWidth * scale).toInt()
                    cardHeight = (cardHeight * scale).toInt()
                    colonWidth = (colonWidth * scale).toInt()
                } else {
                    cardWidth = cardWidthByWidth
                    cardHeight = cardHeightByWidth
                    colonWidth = colonWidthByWidth
                    Log.d(TAG, "方案B超出较少，选择方案B后缩小")
                    val scale = availableHeight.toFloat() / cardHeightByWidth
                    cardWidth = (cardWidth * scale).toInt()
                    cardHeight = availableHeight
                    colonWidth = (colonWidth * scale).toInt()
                }
            }
        } else {
            Log.d(TAG, "--- 竖屏计算 ---")
            // 竖屏策略：
            // 1. 先按宽度限制计算：卡片宽 = (可用宽度-间距)/4.4，卡片高 = 宽*2
            // 2. 检查纵向是否足够
            // 3. 如果纵向有剩余，放大；如果纵向不足，缩小
            // 4. 放大/缩小时保持1:2比例

            // 方案A：按宽度限制（充分利用横向空间）
            var cardWidthByWidth = ((availableWidth - totalSpacing) / 4.4f).toInt()
            var cardHeightByWidth = cardWidthByWidth * 2
            var colonWidthByWidth = (cardWidthByWidth * 0.4f).toInt()

            // 方案B：按高度限制（时钟高度占可用高度的90%）
            var totalHeightByHeight = availableHeight * 0.9f
            var cardHeightByHeight = totalHeightByHeight.toInt()
            var cardWidthByHeight = cardHeightByHeight / 2
            var colonWidthByHeight = (cardWidthByHeight * 0.4f).toInt()
            var totalWidthByHeight = cardWidthByHeight * cardCount + totalSpacing + colonWidthByHeight

            Log.d(TAG, "方案A（按宽度）：cardW=$cardWidthByWidth, cardH=$cardHeightByWidth, totalH=$cardHeightByWidth")
            Log.d(TAG, "方案B（按高度）：cardW=$cardWidthByHeight, cardH=$cardHeightByHeight, totalW=$totalWidthByHeight")

            // 选择满足约束条件的最大方案
            if (cardHeightByWidth <= availableHeight && totalWidthByHeight <= availableWidth) {
                // 两个方案都满足，选择面积更大的
                val areaA = cardWidthByWidth * cardHeightByWidth
                val areaB = cardWidthByHeight * cardHeightByHeight
                if (areaA >= areaB) {
                    cardWidth = cardWidthByWidth
                    cardHeight = cardHeightByWidth
                    colonWidth = colonWidthByWidth
                    Log.d(TAG, "方案A面积更大，选择方案A（充分利用横向空间）")
                } else {
                    cardWidth = cardWidthByHeight
                    cardHeight = cardHeightByHeight
                    colonWidth = colonWidthByHeight
                    Log.d(TAG, "方案B面积更大，选择方案B（充分利用纵向空间）")
                }
            } else if (cardHeightByWidth <= availableHeight) {
                // 方案A满足纵向约束，使用方案A
                cardWidth = cardWidthByWidth
                cardHeight = cardHeightByWidth
                colonWidth = colonWidthByWidth
                Log.d(TAG, "方案A满足纵向约束，选择方案A")
            } else if (totalWidthByHeight <= availableWidth) {
                // 方案B满足横向约束，使用方案B
                cardWidth = cardWidthByHeight
                cardHeight = cardHeightByHeight
                colonWidth = colonWidthByHeight
                Log.d(TAG, "方案B满足横向约束，选择方案B")
            } else {
                // 两个方案都不满足，选择较小的缩放比例
                val scaleWidth = availableHeight.toFloat() / cardHeightByWidth
                val scaleHeight = availableWidth.toFloat() / totalWidthByHeight
                if (scaleWidth <= scaleHeight) {
                    cardWidth = cardWidthByWidth
                    cardHeight = cardHeightByWidth
                    colonWidth = colonWidthByWidth
                    Log.d(TAG, "方案A超出较少，按纵向缩小")
                    val scale = scaleWidth
                    cardWidth = (cardWidth * scale).toInt()
                    cardHeight = (cardHeight * scale).toInt()
                    colonWidth = (colonWidth * scale).toInt()
                } else {
                    cardWidth = cardWidthByHeight
                    cardHeight = cardHeightByHeight
                    colonWidth = colonWidthByHeight
                    Log.d(TAG, "方案B超出较少，按横向缩小")
                    val scale = scaleHeight
                    cardWidth = (cardWidth * scale).toInt()
                    cardHeight = (cardHeight * scale).toInt()
                    colonWidth = (colonWidth * scale).toInt()
                }
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

        // FlipClockView保持MATCH_PARENT，不设置边距
        // 边距已经在计算可用空间时考虑了（availableWidth/Height）
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

        // 横竖屏切换时，尺寸会发生显著变化，需要重置计算状态
        if (oldw > 0 && oldh > 0 && (w != oldw || h != oldh)) {
            Log.d(TAG, "尺寸发生变化，重置计算状态")
            // 重置计算结果，避免使用旧的尺寸数据
            calculatedCardWidth = 0
            calculatedCardHeight = 0
            calculatedColonWidth = 0
            calculatedMargin = 0
        }

        // 更新边距和卡片间距（只调用一次）
        updateLayoutParams()

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

            // 关键修改：使用父容器传递的尺寸，而不是自己计算的 totalWidth
            // 这样 FlipClockView 就会填满父容器
            val width = MeasureSpec.getSize(widthMeasureSpec)
            val height = MeasureSpec.getSize(heightMeasureSpec)
            setMeasuredDimension(width, height)
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

        // 计算时钟总宽度
        val totalClockWidth = calculatedCardWidth * 4 + cardSpacing * 3 + calculatedColonWidth

        // 水平居中：让时钟在 FlipClockView 中水平居中
        val currentX = (width - totalClockWidth) / 2

        // 垂直居中：让时钟在 FlipClockView 中垂直居中
        val currentY = (height - calculatedCardHeight) / 2

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

        Log.d(TAG, "onLayout: 卡片布局完成，currentX=$currentX, currentY=$currentY, totalClockWidth=$totalClockWidth, cardHeight=$calculatedCardHeight")
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

        // 冒号大小根据卡片高度计算，缩小到原来的1/4（从0.08改为0.02）
        val cardHeight = if (calculatedCardHeight > 0) calculatedCardHeight.toFloat() else colonView.height.takeIf { it > 0 }?.toFloat() ?: 300f
        val radius = cardHeight * 0.02f

        // 冒号位置居中（在卡片区域内居中）
        val centerX = colonView.x + colonView.width / 2f
        val centerY = colonView.y + cardHeight / 2f

        // 绘制冒号（两个点）
        canvas.drawCircle(centerX, centerY - cardHeight * 0.15f, radius, paint)
        canvas.drawCircle(centerX, centerY + cardHeight * 0.15f, radius, paint)
    }
}
