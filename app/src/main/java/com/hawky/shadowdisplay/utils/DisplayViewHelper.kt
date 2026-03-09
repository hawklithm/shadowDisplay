package com.hawky.shadowdisplay.utils

import android.os.Handler
import android.os.Looper
import android.view.View
import java.util.Random

/**
 * 显示视图辅助类
 * 封装烧屏保护的通用逻辑
 */
class DisplayViewHelper(
    private val targetView: View
) {
    companion object {
        private const val TAG = "DisplayViewHelper"
        private const val BURN_IN_PROTECTION_OFFSET_MAX = 8 // 最大偏移像素
        private const val BURN_IN_PROTECTION_UPDATE_INTERVAL_MS = 30000L // 30秒更新一次
    }

    private val random = Random()
    private val handler = Handler(Looper.getMainLooper())
    private var burnInRunnable: Runnable? = null
    private var offsetX = 0f
    private var offsetY = 0f
    private var isEnabled = false

    /**
     * 启动烧屏保护
     */
    fun startBurnInProtection() {
        if (isEnabled) return

        isEnabled = true
        burnInRunnable = object : Runnable {
            override fun run() {
                // 生成新的随机偏移
                offsetX = random.nextFloat() * BURN_IN_PROTECTION_OFFSET_MAX - (BURN_IN_PROTECTION_OFFSET_MAX / 2)
                offsetY = random.nextFloat() * BURN_IN_PROTECTION_OFFSET_MAX - (BURN_IN_PROTECTION_OFFSET_MAX / 2)

                // 应用偏移
                targetView.translationX = offsetX
                targetView.translationY = offsetY

                // 继续下一次偏移
                handler.postDelayed(this, BURN_IN_PROTECTION_UPDATE_INTERVAL_MS)
            }
        }

        // 开始第一次偏移
        burnInRunnable?.run()
    }

    /**
     * 停止烧屏保护
     */
    fun stopBurnInProtection() {
        if (!isEnabled) return

        isEnabled = false
        burnInRunnable?.let {
            handler.removeCallbacks(it)
        }

        // 重置偏移
        offsetX = 0f
        offsetY = 0f
        targetView.translationX = 0f
        targetView.translationY = 0f

        burnInRunnable = null
    }

    /**
     * 获取当前偏移X
     */
    fun getOffsetX(): Float = offsetX

    /**
     * 获取当前偏移Y
     */
    fun getOffsetY(): Float = offsetY

    /**
     * 是否正在运行
     */
    fun isRunning(): Boolean = isEnabled
}
