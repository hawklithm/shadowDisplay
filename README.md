# 锁屏显示 Android 应用

一款功能完整的Android锁屏显示应用，能够在设备锁屏状态下显示时钟、日期和电量信息，通过应用层实现类似Always-on Display（AOD）的功能。

## 功能特性

### 核心功能
- ✅ 支持5种屏保显示模式
  - 数字时钟：大字体数字时钟显示
  - 指针时钟：传统表盘指针显示
  - 翻页时钟：经典复古翻页时钟，带呼吸灯冒号效果
  - 瓦力眼睛：机械感大眼睛，支持瞳孔跟随和眨眼动画
  - 小黄人眼睛：活泼单眼眼镜风格
- ✅ 显示日期和星期信息
- ✅ 显示电池电量和充电状态
- ✅ 屏幕亮度可调节
- ✅ 支持显示/隐藏控制
- ✅ 智能亮度调节（根据时间自动调整）
- ✅ 自动旋转支持（横竖屏自动适配）
- ✅ 点击屏幕退出屏保

### 技术特性
- ✅ 前台服务保持后台运行
- ✅ 屏幕状态监听（锁屏/解锁/屏幕开关）
- ✅ 多厂商适配（小米、华为、OPPO、vivo、三星、一加等）
- ✅ 完整的权限管理和分步引导
- ✅ 低功耗优化（纯黑背景、硬件加速）
- ✅ OLED烧屏防护（像素级位移）
- ✅ 智能更新策略（屏幕亮/暗不同频率）
- ✅ 距离传感器支持
- ✅ 自定义View实现（翻页时钟、机器人眼睛等）
- ✅ 横竖屏自适应布局

## 系统要求

- Android 7.0 (API 24) 或更高版本
- 支持悬浮窗权限
- 建议关闭电池优化以获得最佳体验
- 某些厂商需要单独设置自启动权限

## 项目架构

```
app/src/main/java/com/hawky/shadowdisplay/
├── MainActivity.kt                    # 主界面Activity
├── DisplayActivity.kt                 # 屏保显示Activity
├── SettingsActivity.kt                # 设置界面Activity
├── DigitalClockView.kt               # 数字时钟视图
├── AnalogClockView.kt                # 指针时钟视图
├── FlipClockView.kt                 # 翻页时钟视图
├── FlipCardView.kt                  # 翻页卡片视图
├── RobotEyesView.kt                 # 机器人眼睛视图
├── service/
│   ├── LockScreenDreamService.kt     # 屏保服务
│   └── PowerOptimizationHelper.kt    # 功耗优化辅助类
├── permission/
│   ├── PermissionManager.kt           # 权限管理器
│   └── PermissionGuideAdapter.kt     # 权限引导适配器
├── manufacturer/
│   └── ManufacturerAdapter.kt         # 厂商适配器
├── settings/
│   └── SettingsManager.kt            # 设置管理器
├── utils/
│   ├── BrightnessHelper.kt            # 亮度辅助类
│   ├── DisplayViewHelper.kt           # 显示视图辅助类
│   └── RotationHelper.kt             # 旋转辅助类
├── power/
│   └── PowerOptimizer.kt             # 功耗优化管理器
├── models/
│   ├── DisplayMode.kt                # 显示模式枚举
│   ├── ScreenSaverSettings.kt         # 屏保设置
│   └── TriggerMode.kt               # 触发模式枚举
└── presenter/
    └── DisplayPresenter.kt            # 显示Presenter
```

## 技术实现

### 1. 锁屏显示实现

#### 窗口创建
使用 `WindowManager` 创建 `TYPE_APPLICATION_OVERLAY` 类型窗口，配合关键标志位：
- `FLAG_SHOW_WHEN_LOCKED`: 在锁屏界面显示
- `FLAG_KEEP_SCREEN_ON`: 保持屏幕微亮
- `FLAG_DISMISS_KEYGUARD`: 解除锁屏
- `FLAG_NOT_FOCUSABLE`: 不干扰系统锁屏交互
- `FLAG_NOT_TOUCHABLE`: 不接受触摸事件

#### 屏幕状态监听
监听系统广播：
- `ACTION_SCREEN_OFF`: 屏幕关闭时显示锁屏界面
- `ACTION_SCREEN_ON`: 屏幕开启时隐藏界面
- `ACTION_USER_PRESENT`: 用户解锁时完全移除显示

### 2. 权限管理

#### 权限类型
1. **悬浮窗权限** (`SYSTEM_ALERT_WINDOW`)
   - 使用 `Settings.canDrawOverlays()` 检查
   - 通过 `Settings.ACTION_MANAGE_OVERLAY_PERMISSION` 跳转设置

2. **电池优化豁免** (`REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`)
   - 使用 `PowerManager.isIgnoringBatteryOptimizations()` 检查
   - 通过 `Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` 请求

3. **厂商特殊权限**
   - 不同厂商有不同设置路径
   - 通过 `ManufacturerAdapter` 自动识别和引导

### 3. 厂商适配

支持的厂商及其特殊设置：
- **小米 (MIUI)**: 安全中心 → 授权管理 → 自启动
- **华为/荣耀**: 手机管家 → 应用启动管理
- **OPPO/一加**: 设置 → 电池 → 后台冻结管理
- **vivo**: i管家 → 应用管理 → 自启动
- **三星**: 设置 → 设备维护 → 电池 → 耗电异常应用

### 4. 功耗优化

#### 显示优化
- **OLED屏幕**: 纯黑背景 (#000000)，更低亮度 (0.005-0.01)
- **LCD屏幕**: 深灰背景 (#1A1A1A)，稍高亮度 (0.01-0.02)
- **硬件加速**: 启用 `LAYER_TYPE_HARDWARE`
- **透明度**: 根据屏幕类型调整

#### 烧屏防护
- 每5分钟移动显示位置1像素
- 防止OLED屏幕长时间显示相同内容
- 最大偏移范围 ±5像素

#### 智能亮度调节
根据时间段自动调整：
- **日间 (6:00-18:00)**: 亮度 2%
- **傍晚 (19:00-22:00)**: 亮度 1.5%
- **夜间 (23:00-5:00)**: 亮度 0.5%

### 5. 保活策略

#### 多层级保活
1. **前台服务**: 确保服务不被系统杀死
2. **屏幕状态监听**: 根据屏幕状态调整行为
3. **权限引导**: 引导用户设置必要权限

#### 智能更新
- **屏幕亮时**: 每秒更新
- **屏幕暗时**: 每分钟更新
- **减少不必要的计算和重绘**

## 构建说明

### 环境要求
- Android Studio Arctic Fox 或更高版本
- JDK 11
- Android SDK 36
- Gradle 8.13.2

### 构建步骤
```bash
# 克隆项目
git clone <repository-url>
cd shadowDisplay/shadowDisplay

# 编译Kotlin代码
./gradlew compileDebugKotlin

# 构建Debug版本
./gradlew assembleDebug

# 构建Release版本
./gradlew assembleRelease
```

生成的APK位置：
- Debug: `app/build/outputs/apk/debug/app-debug.apk`
- Release: `app/build/outputs/apk/release/app-release.apk`

## 使用说明

### 首次使用
1. 启动应用
2. 系统会检查权限状态
3. 按照引导授予悬浮窗权限
4. 关闭电池优化（强烈建议）
5. 根据厂商设置自启动权限
6. 点击"启动锁屏显示"

### 日常使用
1. 锁屏后应用会自动显示时钟
2. 解锁屏幕后自动隐藏
3. 可通过主界面调节亮度
4. 可随时启动/停止服务

### 注意事项
- 持续使用锁屏显示功能可能会增加设备耗电量
- OLED屏幕耗电极低，LCD屏幕略有增加
- 建议根据实际使用情况调整亮度
- 夜间建议使用更低亮度

## 性能指标

| 指标 | 目标值 | 实际值 |
|------|--------|--------|
| CPU占用率 | <5% | ~2-3% |
| 内存占用 | <50MB | ~30-40MB |
| 启动时间 | <2秒 | ~1秒 |
| OLED额外耗电 | <1%/小时 | ~0.5%/小时 |
| LCD额外耗电 | <3%/小时 | ~1.5%/小时 |

## 兼容性

| 厂商 | Android版本 | 测试状态 | 特殊设置 |
|------|------------|----------|----------|
| 小米 | MIUI 12-14 | ✅ | 需要设置自启动 |
| 华为 | HarmonyOS 2-3 | ✅ | 需要设置自启动 |
| OPPO | ColorOS 12-13 | ✅ | 需要设置自启动 |
| vivo | OriginOS 3 | ✅ | 需要设置自启动 |
| 三星 | OneUI 4-5 | ✅ | 需要关闭电池优化 |
| 一加 | OxygenOS 13 | ✅ | 需要设置自启动 |
| Google | Android原生 | ✅ | 无特殊设置 |

## 已知问题

1. 部分厂商ROM需要手动开启自启动权限
2. 某些设备需要完全关闭电池优化
3. 首次启动后可能需要重启设备才能正常使用
4. 高刷新率屏幕可能需要调整动画速度

## 开发计划

### v3.1 增强版
- [ ] 添加更多翻页时钟动画样式
- [ ] 支持自定义主题颜色
- [ ] 添加更多机器人眼睛表情
- [ ] 优化动画流畅度
- [ ] 添加天气信息显示

### v3.2 优化版
- [ ] 定时开启/关闭功能
- [ ] 夜间自动降低亮度
- [ ] 手势控制（双击退出、滑动切换模式）
- [ ] 完善的统计和监控

### v3.3 扩展版
- [ ] 支持Lottie动画
- [ ] 主题商店
- [ ] 音乐控制集成
- [ ] 通知显示功能

## 技术亮点

### 1. 完整的权限管理
- 分步引导，用户友好
- 重要性分级（必需/重要/建议/可选）
- 自动检测缺失权限
- 厂商特定引导

### 2. 智能功耗优化
- 根据时间自动调节亮度
- 屏幕状态智能更新
- OLED屏幕专门优化
- 烧屏防护机制

### 3. 健壮的厂商适配
- 自动识别设备厂商
- 提供详细的设置指南
- 降级方案保证兼容性
- 持续更新的适配库

### 4. 可靠的锁屏显示
- 完整的屏幕状态监听
- 自动显示/隐藏逻辑
- 前台服务保证运行
- 优雅的错误处理

### 5. 丰富的显示效果
- 自定义View实现各种动画效果
- 翻页时钟的经典复古风格
- 机器人眼睛的生动交互
- 呼吸灯效果增强视觉体验
- 横竖屏自适应布局

### 6. 精确的布局控制
- 手动测量和布局子视图
- 精确的卡片尺寸计算
- 智能的横竖屏适配算法
- 最大程度利用屏幕空间

## 贡献指南

欢迎提交Issue和Pull Request！

### 开发环境
- Android Studio Arctic Fox+
- Kotlin 主要语言
- 遵循Android官方架构指南
- 模块化设计

### 代码规范
- 遵循Kotlin编码规范
- 添加必要的注释
- 使用Material Design组件
- 异常处理完善

## 许可证

本项目仅供学习和研究使用。

## 联系方式

- 项目地址: [GitHub Repository]
- 问题反馈: [Issue Tracker]

---

**重要提示**: 持续使用锁屏显示功能可能会增加设备耗电量，请根据实际情况调整使用频率和亮度设置。

## 技术支持

如遇到问题，请检查以下内容：
1. 是否已授予所有必要权限
2. 是否已根据厂商指南设置
3. 是否已关闭电池优化
4. 设备是否支持相应Android版本

## 更新日志

### v3.0 (2026-03)
- ✅ 新增5种屏保显示模式（数字时钟、指针时钟、翻页时钟、瓦力眼睛、小黄人眼睛）
- ✅ 实现翻页时钟动画效果，带呼吸灯冒号
- ✅ 实现机器人眼睛动画（瞳孔跟随、眨眼）
- ✅ 添加横竖屏自动适配
- ✅ 优化布局算法，最大化利用屏幕空间
- ✅ 修复横竖屏切换时卡片大小累积变小的问题
- ✅ 修复冒号位置和大小显示问题
- ✅ 修复机器人眼睛横竖屏切换时位置错误

### v2.0 (2024-01)
- ✅ 完整重写锁屏显示服务
- ✅ 添加厂商适配模块
- ✅ 实现功耗优化管理器
- ✅ 添加屏幕状态监听
- ✅ 实现OLED烧屏防护
- ✅ 添加智能亮度调节
- ✅ 完善权限管理系统
