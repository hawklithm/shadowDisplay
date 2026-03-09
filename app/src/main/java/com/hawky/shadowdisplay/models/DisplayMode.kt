package com.hawky.shadowdisplay.models

/**
 * 屏保显示模式
 */
enum class DisplayMode(val displayName: String, val description: String) {
    DIGITAL_CLOCK("数字时钟", "大字体数字时钟显示"),
    ANALOG_CLOCK("指针时钟", "传统表盘指针显示"),
    FLIP_CLOCK("翻页时钟", "经典复古翻页时钟"),
    ROBOT_EYES_WALL_E("瓦力眼睛", "机械感大眼睛"),
    ROBOT_EYES_MINION("小黄人眼睛", "活泼单眼眼镜")
}
