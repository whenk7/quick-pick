# R3 报告：UI 组件库（04 分册）

状态：DONE
日期：2026-10-16

## 创建的文件（5 个，全部位于 app/src/main/java/com/quickpick/ui/component/）

| 文件 | 内容 | 对应 Task |
|---|---|---|
| EmptyState.kt | 空状态组件（emoji + 标题 + 描述 + 可选 action 槽） | 4.1 Step 1 |
| ConfirmDialog.kt | 二次确认弹窗（白容器 20dp 圆角，dangerConfirm 危险色确认键） | 4.1 Step 2 |
| BottomNavBar.kt | 底部导航（BottomTab 枚举 📦/🧩/⚙️，emoji 图标） | 4.1 Step 3 |
| PackageCard.kt | 快递卡片（品牌色 8dp 圆点、勾选圆圈、展开详情、DetailRow 私有组件） | 4.2 Step 1 |
| DayGroup.kt | 按天分组（折叠箭头动画、全部已取+二次确认、formatRelativeTime / formatDayLabel 工具函数） | 4.2 Step 2 |

## 与分册的偏差

**零偏差。** 5 个文件代码逐字照抄 04 分册，无转录错误，无分册自身错误需修复。

依赖核对（构建前确认，全部命中 R1/R2 已有符号）：
- ui/theme/Color.kt：Primary、Surface、SurfaceDone、InputBackground、TextPrimary、TextSecondary、TextTertiary、Done、Danger、BrandColors.forName 均存在
- domain/model/PackageRecord：companyName、trackingNumber、pickupCode、address、phone、smsTimestamp、isPickedUp、isIncomplete 字段均存在
- 未用到任何 M3 1.5+ API（AlertDialog/NavigationBar/Card 签名均为 M3 1.4.0 兼容），BOM 2026.06.01 无冲突

## 构建结果

`./gradlew assembleDebug` → **BUILD SUCCESSFUL in 3s**（40 tasks: 7 executed, 33 up-to-date），首次构建即绿，无错误无返工。

## Commit

d4f8b8b feat: R3 UI 组件库（04 分册）——5 files changed, 481 insertions

## 遗留疑虑

1. PackageCard.kt 同时通配导入 `material3.*` 和 `ui.theme.*`，`Surface` 一名两边都有（M3 Surface 是可组合函数、theme.Surface 是 Color 值）。Kotlin 按值引用解析到 theme 属性，编译通过；仅提示后续维护者留意该名字双关，属分册原文设计，未改动。
2. DayGroup.formatDayLabel 中 `today.apply { add(...) }` 会变更 Calendar 实例——本函数每次调用新建实例且判昨天后不再复用，逻辑正确，照抄无改。
3. 组件尚无任何页面引用（R4 屏幕层接入），本轮不跑 Preview/模拟器，符合门禁约定。
