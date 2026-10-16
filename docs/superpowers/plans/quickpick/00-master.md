# 取件宝（QuickPick）— 最终执行计划 · 总纲

> 2026-10-16 全面复审后的定稿版本。本分册是架构与执行的**总入口**；
> 每个 Phase 的完整可执行代码在对应分册里，照抄即可。
> 权威产品规格：`docs/superpowers/specs/2026-05-30-quickpick-design.md`
> 权威视觉系统：`docs/superpowers/design/design-system.md`

---

## 一、产品定位（一句话）

打开应用 → 快递取件信息按天整理好 → 取件码一目了然 → 取完一键标记。
**只读短信、只存本地、不联网、无账号、无第三方 SDK。**

## 二、功能清单（最终口径）

| 模块 | 功能 |
|------|------|
| 快递列表 | 按天分组（可折叠）、取件码 28sp 大字、点击复制各字段、勾选标记已取（置灰保留）、全部已取/按天已取（二次确认）、下拉全量重扫、回前台自动增量扫描 |
| 匹配规则 | 预置 2 家（顺丰/京东）+ 自定义；新增/编辑/删除/单条分享；前后缀锚点纯字符串匹配（无正则） |
| 规则编辑器 | 粘贴短信 → 分词块（吸顶固定）→ 点击词块自动填锚点 → 字段独立卡片（选中抬高+左侧色线）→ 底部与列表页完全一致的实时卡片预览 → 底部固定保存大按钮 |
| AI 辅助 | 仅模式 B：生成提示词 → 用户复制去外部 AI → 粘贴 JSON 回来（自动剥离 markdown 围栏）→ 自动跳转编辑器回显 |
| 导入导出 | 全量导出 / 单条分享（系统分享面板）/ 文件导入（同名规则逐条 覆盖/跳过 → 结果摘要） |
| 权限与合规 | 首次启动引导页（合规声明）→ 系统授权；权限被拒/回收的降级展示；引导页+设置页双处合规声明 |

**明确不做**：物流追踪、云端同步、AI 内置 API、鸿蒙/iOS、Google Play（READ_SMS 政策不允许，走 GitHub Release 侧载）、strings.xml 国际化、Hilt。

## 三、技术架构

### 分层（MVVM）

```
┌─ ui/screen + ui/component + ui/viewmodel ─┐   Compose UI，StateFlow 驱动
│         事件 ↓          ↑ 状态             │
├─ domain/（MatchEngine 引擎 + 纯模型）     ─┤   纯 Kotlin，无 Android 依赖，可单测
│         调用 ↓          ↑ 模型             │
├─ data/repository ────────────────────────┤   实体↔领域模型映射、去重/覆盖策略
│         DAO ↓                            │
├─ data/db（Room 3 表） + data/local ──────┤   Room + DataStore
└─ util/（SmsReader 系统短信、JsonExporter） ┘   唯二的系统边界
```

### 关键机制

- **匹配引擎**：`indexOf` 前后缀锚点。多规则命中时**匹配字段数多者优先，相同按规则列表顺序**（预置在前）。
- **去重**：`scanned_sms` 表记录已扫短信 ID（唯一索引）；同日同单号内容一致跳过、取件码不同覆盖（换柜）。
- **权限模型**：不依赖后台推送。每次回前台增量扫描；权限状态由 UI 层 `checkSelfPermission` 前置判定（不靠读结果猜）。
- **首启判定**：DataStore 标记（权限被回收不会弹回引导页；预置规则删光不复活）。
- **不完整性**：`isIncomplete = 已配置字段中未命中数 > 0`（电话可选，不硬编码 4）。
- **时区**：SQL 日期比较一律带 `'localtime'`，UI 分组用本地日界，两者对齐。

### 版本矩阵（2026-10 核实）

Kotlin 2.3.20 ｜ AGP 8.13.0 ｜ KSP 2.3.12（KSP2）｜ Compose BOM 2026.09.00 ｜ Room 2.8.4 ｜ Navigation 2.9.5 ｜ Lifecycle 2.9.4 ｜ DataStore 1.1.7 ｜ Gson 2.13.1 ｜ compileSdk/targetSdk 36 ｜ minSdk 30 ｜ Gradle 8.14.3 ｜ JDK 17

> 补丁号若 sync 时提示不存在，按 IDE 建议微调即可，不影响计划其余部分。

## 四、文件地图（43 个 Kotlin 源文件 + 5 个资源/Manifest 文件）

```
app/src/main/
├── AndroidManifest.xml              [01] READ_SMS 唯一权限，无 INTERNET
├── res/values/themes.xml            [01] 平台主题（不引 MDC）
├── res/values/colors.xml            [01]
├── res/mipmap-anydpi-v26/ic_launcher.xml + res/drawable/ic_launcher_foreground.xml  [01] 小取图标
└── java/com/quickpick/
    ├── QuickPickApp.kt              [01 最小版 → 02 接库 → 02 末定稿]
    ├── MainActivity.kt              [01 占位 → 06 定稿]
    ├── data/
    │   ├── db/AppDatabase.kt                    [02]
    │   ├── db/entity/{MatchRule,PackageRecord,ScannedSms}Entity.kt  [02]
    │   ├── db/dao/{MatchRule,PackageRecord,ScannedSms}Dao.kt        [02]
    │   ├── local/SettingsStore.kt               [02] DataStore
    │   ├── preset/PresetRules.kt                [02] 顺丰/京东
    │   └── repository/{Rule,Package,Sms}Repository.kt               [03]
    ├── domain/
    │   ├── model/{MatchRule,PackageRecord,ExtractedFields}.kt       [02]
    │   └── engine/MatchEngine.kt                [02] 锚点引擎
    ├── ui/
    │   ├── theme/{Color,Type,Shape,Theme}.kt    [01] 治愈系豆绿
    │   ├── component/{EmptyState,ConfirmDialog,BottomNavBar}.kt     [04]
    │   ├── component/{PackageCard,DayGroup}.kt  [04]
    │   ├── component/{TokenChip,FieldCard}.kt   [05b]
    │   ├── screen/PackageListScreen.kt          [05a]
    │   ├── screen/RuleListScreen.kt             [05a]
    │   ├── screen/RuleEditorScreen.kt           [05b]
    │   ├── screen/AiPromptScreen.kt             [05c]
    │   ├── screen/SettingsScreen.kt             [05c]
    │   ├── screen/OnboardingScreen.kt           [05c]
    │   ├── viewmodel/PackageListViewModel.kt    [05a]
    │   ├── viewmodel/RuleListViewModel.kt       [05a]
    │   ├── viewmodel/RuleEditorViewModel.kt     [05b]
    │   ├── viewmodel/AiPromptViewModel.kt       [05c]
    │   ├── viewmodel/SettingsViewModel.kt       [05c]
    │   └── navigation/AppNavigation.kt          [06]
    └── util/{SmsReader,JsonExporter}.kt         [03]
```

## 五、执行轮次（5 轮，轮间可验证）

| 轮次 | 分册 | 内容 | 出口标准 |
|------|------|------|----------|
| 1 | [01](01-foundation.md) + [02](02-data-engine.md) | 项目骨架 + 数据层 + 引擎 | `assembleDebug` 过，App 能起，预置规则入库 |
| 2 | [03](03-repository.md) | 仓库 + 工具 | 编译过 |
| 3 | [04](04-components.md) | UI 组件 | 编译过 |
| 4 | [05a](05a-screens-list.md) + [05b](05b-editor.md) + [05c](05c-ai-settings.md) | 全部页面 | 编译过，各页可单独预览 |
| 5 | [06](06-navigation.md) + [07](07-polish.md) | 导航 + 验收 | 全链路可跑，07 清单全过 |

每轮结束跑 `./gradlew assembleDebug` 验证，绿了就 commit。

## 六、验收

最终验收清单在 [07-polish.md](07-polish.md) Task 7.2——已并入规格书全部 11 条验收场景 + 权限边界 + 数据正确性用例，执行完逐条过。

---

## 修订记录

- **2026-05-31** 初版（40 文件计划）
- **2026-10-16** 四路并行评审后全面修订定稿：修复 9 处编译/运行阻断（Gradle DSL、gradle.properties 缺失、MDC 主题引用、7 处 import、LazyColumn 无限高崩溃、AI 回显断链、预置复活）、补齐 10 项规格缺失功能（规则导入、删除入口、分词吸顶、分组折叠、下拉重扫、改规则重扫提示、同日同单号去重/换柜覆盖、5 种空状态、权限回收 banner、前往授权接线）、版本矩阵整体升级、设计系统逐条还原（M3 全槽位配色、勾选圆圈可见性、删除线、强调色细线、底部保存大按钮）、图标定案 emoji+品牌色圆点、分发定案 GitHub Release 侧载。拆分为 1 总纲 + 9 分册。
