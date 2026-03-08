package com.hawky.shadowdisplay.manufacturer

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings

/**
 * 厂商适配器
 */
object ManufacturerAdapter {

    enum class Manufacturer {
        XIAOMI, HUAWEI, HONOR, OPPO, VIVO, SAMSUNG, ONEPLUS, OTHERS
    }

    fun getCurrentManufacturer(): Manufacturer {
        val brand = Build.MANUFACTURER.lowercase()
        return when {
            brand.contains("xiaomi") -> Manufacturer.XIAOMI
            brand.contains("huawei") -> Manufacturer.HUAWEI
            brand.contains("honor") -> Manufacturer.HONOR
            brand.contains("oppo") -> Manufacturer.OPPO
            brand.contains("oneplus") -> Manufacturer.ONEPLUS
            brand.contains("vivo") -> Manufacturer.VIVO
            brand.contains("samsung") -> Manufacturer.SAMSUNG
            else -> Manufacturer.OTHERS
        }
    }

    fun getManufacturerName(manufacturer: Manufacturer): String {
        return when (manufacturer) {
            Manufacturer.XIAOMI -> "小米"
            Manufacturer.HUAWEI -> "华为"
            Manufacturer.HONOR -> "荣耀"
            Manufacturer.OPPO -> "OPPO"
            Manufacturer.VIVO -> "vivo"
            Manufacturer.SAMSUNG -> "三星"
            Manufacturer.ONEPLUS -> "一加"
            Manufacturer.OTHERS -> "其他"
        }
    }

    fun needsSpecialAdaptation(): Boolean {
        val manufacturer = getCurrentManufacturer()
        return manufacturer in listOf(
            Manufacturer.XIAOMI,
            Manufacturer.HUAWEI,
            Manufacturer.HONOR,
            Manufacturer.OPPO,
            Manufacturer.VIVO,
            Manufacturer.SAMSUNG
        )
    }

    fun getPermissionInstructions(manufacturer: Manufacturer): List<String> {
        return when (manufacturer) {
            Manufacturer.XIAOMI -> listOf(
                "1. 打开「设置」→「应用设置」→「授权管理」",
                "2. 找到「息屏显示」并开启「后台弹出界面」",
                "3. 返回上级菜单，开启「自启动」",
                "4. 进入「省电与电池」→「应用省电策略」",
                "5. 将「息屏显示」设为「无限制」"
            )
            Manufacturer.HUAWEI, Manufacturer.HONOR -> listOf(
                "1. 打开手机管家→「应用启动管理」",
                "2. 找到「息屏显示」并关闭「自动管理」",
                "3. 开启「后台活动」和「允许自启动」",
                "4. 进入「省电模式」→「应用耗电管理」",
                "5. 将「息屏显示」设为「允许后台高耗电」"
            )
            Manufacturer.OPPO -> listOf(
                "1. 打开「设置」→「电池」→「耗电详情」",
                "2. 找到「息屏显示」并关闭「耗电优化」",
                "3. 进入「其他耗电管理」→「后台冻结管理」",
                "4. 关闭「息屏显示」的冻结设置",
                "5. 进入「权限与隐私」→「自启动管理」",
                "6. 允许「息屏显示」自启动"
            )
            Manufacturer.VIVO -> listOf(
                "1. 打开「i管家」→「App管理」→「自启动」",
                "2. 允许「息屏显示」自启动",
                "3. 进入「省电管理」→「耗电管理」",
                "4. 选择「息屏显示」并关闭「后台高耗电」",
                "5. 进入「权限管理」→「后台弹出界面」",
                "6. 允许「息屏显示」后台弹出"
            )
            Manufacturer.SAMSUNG -> listOf(
                "1. 打开「设置」→「设备维护」→「电池」",
                "2. 点击「耗电异常的应用」",
                "3. 选择「息屏显示」并关闭「优化电池使用」",
                "4. 进入「应用程序」→「应用程序管理器」",
                "5. 选择「息屏显示」→「电池」",
                "6. 关闭「不活动时限制后台活动」"
            )
            else -> emptyList()
        }
    }
}
