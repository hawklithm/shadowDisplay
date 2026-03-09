package com.hawky.shadowdisplay.views

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import kotlin.math.min
import java.util.*

/**
 * 翻页时钟视图
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
                LayoutParams.MATCH_PARENT,
                1f
            ).apply {
                setMargins(6, 6, 6, 6)
            }
            setWillNotDraw(false)
        }
    }

    /**
     * 动态调整卡片间距
     */
    fun updateCardMargins() {
        // 间距根据屏幕尺寸动态调整
        val margin = (min(width, height) * 0.01f).toInt()

        val cards = listOf(hourTensCard, hourUnitsCard, minuteTensCard, minuteUnitsCard)
        cards.forEach { card ->
            val lp = card.layoutParams as LayoutParams
            lp.setMargins(margin, margin, margin, margin)
            card.layoutParams = lp
        }

        Log.d(TAG, "updateCardMargins: margin=$margin")
    }

    /**
     * 更新边距以适应屏幕尺寸
     */
    fun updateMargins() {
        // 获取父容器
        val parent = parent as? android.widget.FrameLayout ?: return

        // 计算边距：使用屏幕最小边长的5%
        val margin = (min(width, height) * 0.05f).toInt()
        val layoutParams = layoutParams as? android.widget.FrameLayout.LayoutParams ?: return
        layoutParams.setMargins(margin, margin, margin, margin)
        parent.updateViewLayout(this, layoutParams)

        Log.d(TAG, "updateMargins: margin=$margin, width=$width, height=$height")
    }

    /**
     * 创建分隔符
     */
    private fun createColonView(context: Context): View {
        return View(context).apply {
            layoutParams = LayoutParams(
                dpToPx(15),
                LayoutParams.MATCH_PARENT
            ).apply {
                setMargins(5, 0, 5, 0)
            }
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

        val radius = height * 0.03f

        // 绘制冒号（两个点）
        canvas.drawCircle(
            colonView.x + colonView.width / 2f,
            height * 0.4f,
            radius,
            paint
        )
        canvas.drawCircle(
            colonView.x + colonView.width / 2f,
            height * 0.6f,
            radius,
            paint
        )
    }
}
