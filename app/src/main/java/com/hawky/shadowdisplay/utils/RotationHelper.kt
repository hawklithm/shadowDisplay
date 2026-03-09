package com.hawky.shadowdisplay.utils

import android.content.res.Configuration
import android.util.Log

/**
 * 旋转辅助类
 * 处理自动旋转功能
 */
class RotationHelper(
    private val onOrientationChanged: (Boolean) -> Unit
) {
    companion object {
        private const val TAG = "RotationHelper"
    }

    /**
     * 处理配置变化
     * @param newConfig 新的配置
     * @param autoRotation 是否开启自动旋转
     */
    fun onConfigurationChanged(newConfig: Configuration, autoRotation: Boolean) {
        if (autoRotation) {
            val isLandscape = newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE
            Log.d(TAG, "自动旋转: ${if (isLandscape) "横屏" else "竖屏"}")
            onOrientationChanged(isLandscape)
        }
    }

    /**
     * 获取当前是否横屏
     */
    fun isLandscape(configuration: Configuration): Boolean {
        return configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    }
}
