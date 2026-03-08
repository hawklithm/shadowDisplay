package com.hawky.shadowdisplay.models

/**
 * 屏保设置数据类
 */
data class ScreenSaverSettings(
    // 屏保模式
    val displayMode: DisplayMode = DisplayMode.DIGITAL_CLOCK,

    // 触发设置
    val triggerMode: TriggerMode = TriggerMode.CHARGING_ONLY,

    // 显示设置
    val autoRotation: Boolean = false,
    val autoBrightness: Boolean = true,
    val manualBrightness: Float = 1.0f, // 0.1% - 20%
    val burnInProtection: Boolean = true,

    // 数字时钟样式
    val digitalClockShowDate: Boolean = true,
    val digitalClockShowWeekday: Boolean = true,
    val digitalClockColor: Int = 0xFFFFFFFF.toInt(),
    val digitalClockSize: Float = 1.0f, // 0.5 - 2.0

    // 指针时钟样式
    val analogClockStyle: Int = 0, // 0-2
    val analogClockSmoothSeconds: Boolean = true,

    // 眼睛样式
    val eyesGazeFrequency: Int = 5, // 3-10秒
    val eyesBlinkFrequency: Int = 10, // 5-15秒
    val eyesIrisColor: Int = 0xFF4CAF50.toInt()
)
