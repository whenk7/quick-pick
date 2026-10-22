# 主题系统重做 + 设置页重构设计（v1.1.2）

2026-10-22。权威调研来源：miuix 0.9.4 源码（本地）+ InstallerX-Revived 实现（/tmp/installerx-research，调研报告在本会话）。

## 裁决记录（用户拍板）

- 豆绿配色从项目全面移除（代码/文档/注释不留字样），不再作为品牌身份
- 老主题配置键（colorMode/useMonet/paletteStyle）不做迁移兼容，直接弃用
- 固定配色 = miuix 内置默认板（`lightColorScheme()`/`darkColorScheme()`），不自建
- 种子色选择 = ColorPalette 全网格（miuix 现成组件），不做 InstallerX 饼图
- 不做预览卡——设置即改即全局生效，全局就是预览
- 玻璃底栏「稍微再强烈一些」→ 新默认值整体加强一档 + 4 个滑杆可调

## 架构（照 InstallerX 双路径）

```
主题引擎 FIXED：ThemeController(colorSchemeMode = System/Light/Dark)
  → light/darkColors 用 miuix 内置默认（不传参）
主题引擎 MONET：ThemeController(colorSchemeMode = MonetSystem/MonetLight/MonetDark,
  keyColor = 动态取色开且 API≥31 → null（miuix 内部走壁纸/系统调色板）
             否则 → Color(seedColor),
  paletteStyle = 9 选一, colorSpec = Spec2021/Spec2025)
```

偏好与生效值分离（抄 InstallerX `effectiveSpec`）：风格不支持所选规格时生效值回退 2021，不改写存储。规格行在只有一种可选时置灰 + summary 说明。

## DataStore 键（全新，老键弃用不迁移）

| 键 | 类型 | 默认 |
|---|---|---|
| theme_mode | String: SYSTEM/LIGHT/DARK | SYSTEM |
| theme_engine | String: FIXED/MONET | FIXED |
| dynamic_color | Boolean（Monet 内的壁纸取色） | true |
| seed_color | Int（动态取色关或 API<31 时用） | 0xFF3482FF（miuix 默认主色） |
| palette_style | String: 9 枚举名 | TonalSpot |
| color_spec | String: Spec2021/Spec2025 | Spec2025 |
| glass_blur | Float 0..1 → blur 半径 2..12dp | 0.6 |
| glass_lens | Float 0..1 → 折射 12..36dp | 0.55 |
| glass_highlight | Float 0..1 → 高光 alpha 0.4..1.0 | 0.85 |
| glass_alpha | Float 0..1 → 容器不透明度 0.25..0.6 | 0.32（反向：越低越透） |

## 页面

**设置主页**：短信扫描 4 行平铺 → 一行 `WindowSpinnerPreference`；主题段删除，改为入口行「主题与外观 ›」。

**主题与外观页**（新路由 theme_settings，左上角 ← 返回，同 RuleEditor 先例）：
- SmallTitle 分组 + Card 容器（miuix 设置页范式）
- 外观：主题模式 spinner（跟随系统/浅色/深色）
- 配色：引擎选择 spinner（固定配色/动态配色 Monet）；Monet 时 AnimatedVisibility 展开：动态取色 switch、种子色 ColorPalette（动态取色关或 API<31 时显示）、调色板风格 spinner（9 全量中文名）、色彩规格 spinner（置灰逻辑）
- 玻璃底栏：4 个 Slider 行（实时生效）

依赖：`+= top.yukonga.miuix.kmp:miuix-preference-android:0.9.4`（WindowSpinnerPreference/SwitchPreference 在此构件）。

## 清洗清单

- Color.kt：删 Primary/PrimaryLight/明暗原始色板/Pending（死代码）；语义 getter 不动；`BrandColors.DEFAULT` 改中性灰；InputFieldColors 聚焦边改跟主题 primary
- Theme.kt：整文件重写为双路径
- LiquidGlassNavigationBar.kt：4 参数从设置读，注释去豆绿
- CLAUDE.md：设计方向段重写（治愈系极简保留，色彩身份 = miuix 默认 + Monet）
- .res/治愈系极简设计落地方案.md：色彩节改为新方向
- 删 .res/JetpackComposeMaterial3完整主题代码.md（M3 遗物，整体过时）

## 不做

- 预览卡/饼图色板（用户裁决）
- 老配置迁移
- InstallerX 的 UI 引擎切换（我们纯 miuix，无 Google 引擎路径）
