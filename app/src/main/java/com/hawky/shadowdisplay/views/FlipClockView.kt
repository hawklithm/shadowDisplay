package com.hawky.shadowdisplay.views

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
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
) : LinearLayout(context, attrs, defStyleAttr) {

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

    // 更新任务
    private val updateRunnable = object : Runnable {
        override fun run() {
            updateTime()
            postDelayed(this, UPDATE_INTERVAL)
        }
    }

    init {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER
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
            layoutParams = LayoutParams(
                0,
                LayoutParams.WRAP_CONTENT,
                1f
            ).apply {
                setMargins(4, 4, 4, 4)
            }
            setWillNotDraw(false)
        }
    }

    /**
     * 动态调整卡片间距和尺寸
     */
    fun updateLayoutParams() {
        if (width <= 0 || height <= 0) return

        val isLandscape = width > height
        val minDimension = min(width, height)

        // 计算边距：至少5%，确保卡片有足够空间
        val marginPercent = if (isLandscape) 0.05f else 0.08f
        val margin = (minDimension * marginPercent).toInt()

        // 计算卡片的合适高度
        // 竖屏时根据宽度计算，确保数字能完整显示
        // 横屏时根据高度计算
        val availableWidth = width - margin * 2
        val availableHeight = height - margin * 2

        // 卡片数量：4个数字 + 1个冒号
        val cardCount = 4
        val colonWidth = dpToPx(12)
        val cardSpacing = dpToPx(8)
        val totalSpacing = cardSpacing * (cardCount - 1)
        val totalColonWidth = colonWidth

        val availableCardWidth = (availableWidth - totalSpacing - totalColonWidth) / cardCount

        // 卡片高度：根据宽度来，确保是方形或接近方形
        val cardHeight = if (isLandscape) {
            // 横屏：高度-边距
            availableHeight - margin * 2
        } else {
            // 竖屏：根据宽度计算，让卡片接近正方形
            (availableCardWidth * 1.3f).toInt().coerceAtMost(availableHeight - margin)
        }

        // 更新每个卡片的尺寸
        val cards = listOf(hourTensCard, hourUnitsCard, minuteTensCard, minuteUnitsCard)
        cards.forEach { card ->
            val lp = card.layoutParams as LayoutParams
            lp.width = availableCardWidth
            lp.height = cardHeight
            lp.setMargins(cardSpacing / 2, margin / 2, cardSpacing / 2, margin / 2)
            card.layoutParams = lp
        }

        // 更新冒号尺寸
        val colonLp = colonView.layoutParams as LayoutParams
        colonLp.width = colonWidth
        colonLp.height = cardHeight
        colonView.layoutParams = colonLp

        // 更新自己的边距
        val parent = parent as? FrameLayout
        if (parent != null) {
            val layoutParams = this.layoutParams as? FrameLayout.LayoutParams
            if (layoutParams != null) {
                layoutParams.setMargins(margin, margin, margin, margin)
                layoutParams.gravity = Gravity.CENTER
                parent.updateViewLayout(this, layoutParams)
            }
        }

        Log.d(TAG, "updateLayoutParams: w=$width, h=$height, isLandscape=$isLandscape, cardHeight=$cardHeight, cardWidth=$availableCardWidth, margin=$margin")
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
            layoutParams = LayoutParams(
                dpToPx(12),
                LayoutParams.WRAP_CONTENT
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
