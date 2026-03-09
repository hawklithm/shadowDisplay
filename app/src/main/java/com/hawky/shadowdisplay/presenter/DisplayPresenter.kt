package com.hawky.shadowdisplay.presenter

import android.content.Context
import android.view.View
import com.hawky.shadowdisplay.settings.SettingsManager
import com.hawky.shadowdisplay.utils.BrightnessHelper
import com.hawky.shadowdisplay.utils.DisplayViewHelper

/**
 * 显示Presenter接口
 * 统一管理显示视图的通用功能：烧屏保护、自动亮度、自动旋转
 */
interface DisplayPresenter {

    /**
     * 获取当前视图
     */
    fun getView(): View?

    /**
     * 初始化显示
     */
    fun initialize()

    /**
     * 启动显示
     */
    fun start()

    /**
     * 停止显示
     */
    fun stop()

    /**
     * 销毁显示
     */
    fun destroy()

    /**
     * 设置横竖屏
     */
    fun setLandscape(landscape: Boolean)
}

/**
 * 显示Presenter默认实现
 * 提供烧屏保护、自动亮度、自动旋转的默认实现
 */
abstract class AbstractDisplayPresenter(
    protected val context: Context
) : DisplayPresenter {

    protected var settingsManager: SettingsManager? = null
    protected var displayViewHelper: DisplayViewHelper? = null
    protected var brightnessHelper: BrightnessHelper? = null

    protected var isAutoRotation = false
    protected var isAutoBrightness = false
    protected var isBurnInProtection = false

    override fun initialize() {
        settingsManager = SettingsManager.getInstance(context)
        val settings = settingsManager?.getSettings()

        isAutoRotation = settings?.autoRotation ?: false
        isAutoBrightness = settings?.autoBrightness ?: false
        isBurnInProtection = settings?.burnInProtection ?: false
    }

    /**
     * 启动烧屏保护
     */
    protected fun startBurnInProtection(view: View) {
        if (isBurnInProtection) {
            displayViewHelper = DisplayViewHelper(view)
            displayViewHelper?.startBurnInProtection()
        }
    }

    /**
     * 停止烧屏保护
     */
    protected fun stopBurnInProtection() {
        displayViewHelper?.stopBurnInProtection()
        displayViewHelper = null
    }

    /**
     * 应用亮度设置
     */
    protected fun applyBrightnessSettings(window: android.view.Window?) {
        if (brightnessHelper == null && window != null) {
            brightnessHelper = BrightnessHelper(context, window)
            brightnessHelper?.initialize()
        }
        brightnessHelper?.applyBrightnessSettings()
    }

    /**
     * 检查电池状态
     */
    protected fun checkBatteryStatus(batteryLevel: Int) {
        brightnessHelper?.checkBatteryStatus(batteryLevel)
    }

    override fun destroy() {
        stopBurnInProtection()
        brightnessHelper?.cleanup()
        brightnessHelper = null
    }
}
