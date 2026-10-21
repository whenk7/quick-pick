# Miuix 迁移 + 引擎修复 + 功能补齐 实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将取件宝从 Material 3 全面迁移到 miuix KMP，修复匹配引擎硬缺陷，补齐测试文档中的功能项，发布 v1.1.0。

**Architecture:** 在现有 MVVM 分层上做增量改造。数据层先加列（Room migration v1→v2）+ 模型扩展；引擎层加发件方过滤和品牌提取；UI 层整体换皮（M3→miuix 组件逐文件替换）+ 主题系统重构（MiuixTheme + 豆绿色板）+ 悬浮玻璃底栏。每步 assembleDebug 绿再继续。

**Tech Stack:** Kotlin 2.4.21 + Compose BOM 2026.09.00 + miuix KMP 0.9.4 (ui+blur) + Room 2.8.4 + AGP 9.4.1 + Gradle 9.8.1 + minSdk 33 + compileSdk/targetSdk 37

**Spec:** `docs/superpowers/specs/2026-10-21-miuix-migration-design.md`

## Global Constraints

- minSdk 33（与 miuix-blur 对齐）
- compileSdk / targetSdk 37
- AGP 9.4.1，Gradle 9.8.1，Kotlin Compose 插件 2.4.21
- Compose BOM 2026.09.00
- miuix-ui-android 0.9.4 + miuix-blur-android 0.9.4
- `org.jetbrains.kotlin.android` 插件已废除（AGP 9.0+ 内置）
- 不加 `fallbackToDestructiveMigration()`
- 文案不用"错误""失败"等负面词
- 动效柔和不弹跳
- 每步完成后 `./gradlew assembleDebug` 必须绿

## Review Focus

1. **发件方过滤空规则仍匹配所有短信**：senderPattern=null 的旧规则应不过滤发件方（向后兼容），不应因新列默认 NULL 而拒绝匹配
2. **品牌提取结果与 BrandColors.forName 匹配断裂**：提取结果可能是"圆通快递"而非常量"圆通"，`forName` 用 `contains` 模糊匹配需覆盖
3. **Room migration v1→v2 在已有数据上执行**：旧表有 ForeignKey 约束和索引，新表迁移需完整保留
4. **miuix OverlayDialog 依赖 Scaffold 的 MiuixPopupHost**：所有用 ConfirmDialog 的页面必须在 miuix Scaffold 内
5. **冷启动 DataStore 异步读主题偏好时首帧配色闪烁**：接受一帧闪烁，不引入 SplashScreen

---

### Task 1: Spike 提交 + minSdk 对齐

**Files:**
- Modify: `app/build.gradle.kts:13` (minSdk 30→33)
- Commit: 4 spike files already in working tree

**Interfaces:**
- Produces: minSdk=33 的可构建项目，miuix-ui-android 0.9.4 已引入，MiuixTheme 冒烟通过

- [ ] **Step 1: 改 minSdk**

`app/build.gradle.kts` 第 13 行：`minSdk = 30` → `minSdk = 33`

- [ ] **Step 2: 验证构建**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: 提交 spike 产出**

```bash
git add app/build.gradle.kts app/src/main/java/com/quickpick/MainActivity.kt build.gradle.kts gradle/wrapper/gradle-wrapper.properties
git commit -m "feat: miuix 0.9.4 spike — 版本矩阵对齐 + 冒烟验证（minSdk 33）"
```

---

### Task 2: Room v1→v2 migration + 数据模型扩展

**Files:**
- Modify: `app/src/main/java/com/quickpick/data/db/AppDatabase.kt`
- Modify: `app/src/main/java/com/quickpick/data/db/entity/MatchRuleEntity.kt`
- Modify: `app/src/main/java/com/quickpick/data/db/entity/PackageRecordEntity.kt`
- Modify: `app/src/main/java/com/quickpick/domain/model/MatchRule.kt`
- Modify: `app/src/main/java/com/quickpick/domain/model/ExtractedFields.kt`
- Modify: `app/src/main/java/com/quickpick/domain/model/PackageRecord.kt`
- Modify: `app/src/main/java/com/quickpick/data/repository/RuleRepository.kt`
- Modify: `app/src/main/java/com/quickpick/data/repository/PackageRepository.kt`

**Interfaces:**
- Consumes: 现有 MatchRule/ExtractedFields/PackageRecord 模型
- Produces:
  - `MatchRule.senderPattern: String?`、`MatchRule.brandPrefix: String?`、`MatchRule.brandSuffix: String?`
  - `ExtractedFields.brandName: String?`
  - `PackageRecord` 不再有 `isIncomplete` 属性
  - `MatchRuleEntity` 新增 `sender_pattern`、`brand_prefix`、`brand_suffix` 列
  - `PackageRecordEntity` 新增 `sms_thread_id` 列，删除 `is_incomplete` 列
  - `AppDatabase` version=2，注册 `MIGRATION_1_2`

- [ ] **Step 1: MatchRuleEntity 加列**

在 `MatchRuleEntity` 加三个字段（nullable，默认 null）：
- `@ColumnInfo(name = "sender_pattern") val senderPattern: String? = null`
- `@ColumnInfo(name = "brand_prefix") val brandPrefix: String? = null`
- `@ColumnInfo(name = "brand_suffix") val brandSuffix: String? = null`

- [ ] **Step 2: PackageRecordEntity 改表**

加 `smsThreadId`，删 `isIncomplete`：
- 加 `@ColumnInfo(name = "sms_thread_id") val smsThreadId: Long? = null`
- 删除 `@ColumnInfo(name = "is_incomplete") val isIncomplete: Boolean = false`

注意：Room 不支持直接 DROP COLUMN（SQLite < 3.35），migration 中需要新表迁移。

- [ ] **Step 3: 写 Migration_1_2**

在 `AppDatabase.kt` 中加：

```kotlin
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE match_rules ADD COLUMN sender_pattern TEXT")
        db.execSQL("ALTER TABLE match_rules ADD COLUMN brand_prefix TEXT")
        db.execSQL("ALTER TABLE match_rules ADD COLUMN brand_suffix TEXT")
        db.execSQL("ALTER TABLE package_records ADD COLUMN sms_thread_id INTEGER")
        // 删 is_incomplete：新表迁移
        db.execSQL("CREATE TABLE package_records_new (...)")  // 完整建表语句，不含 is_incomplete
        db.execSQL("INSERT INTO package_records_new SELECT id,...,NULL,... FROM package_records")  // 全列映射
        db.execSQL("DROP TABLE package_records")
        db.execSQL("ALTER TABLE package_records_new RENAME TO package_records")
        // 重建索引（参考原 Entity 的 @Index 注解）
    }
}
```

`databaseBuilder` 加 `.addMigrations(MIGRATION_1_2)`。`version` 改为 2。

- [ ] **Step 4: MatchRule domain model 加字段**

加 `val senderPattern: String? = null`、`val brandPrefix: String? = null`、`val brandSuffix: String? = null`。
`configuredFieldCount` 不计入这三个字段（它们是过滤器不是提取器）。

- [ ] **Step 5: ExtractedFields 加 brandName**

加 `val brandName: String? = null`。`matchedCount` 不计入 brandName。

- [ ] **Step 6: PackageRecord 删 isIncomplete**

从 `PackageRecord` data class 删除 `isIncomplete` 属性。

- [ ] **Step 7: 更新 Repository 映射**

`RuleRepository.toDomain()`：加 senderPattern/brandPrefix/brandSuffix 映射。
`RuleRepository.toEntity()`：加 senderPattern/brandPrefix/brandSuffix 映射。
`PackageRepository.toDomain()`：加 smsThreadId 映射，删 isIncomplete 映射。
`PackageRepository.toEntity()`：加 smsThreadId 映射，删 isIncomplete 映射。

- [ ] **Step 8: 修复编译错误**

`PackageListViewModel.scanSms` 中 `isIncomplete = ...` 赋值删除。
`RuleEditorScreen` 中预览 `PackageRecord(isIncomplete = false)` 删除该参数。
全局搜索 `isIncomplete` 确认零引用。

- [ ] **Step 9: 验证构建**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 10: 提交**

```bash
git add -A && git commit -m "feat: Room v1→v2 migration + 数据模型扩展（发件方/品牌/线程ID，删 isIncomplete）"
```

---

### Task 3: 匹配引擎修复 + SmsReader 扩展

**Files:**
- Modify: `app/src/main/java/com/quickpick/util/SmsReader.kt`
- Modify: `app/src/main/java/com/quickpick/domain/engine/MatchEngine.kt`
- Modify: `app/src/main/java/com/quickpick/ui/viewmodel/PackageListViewModel.kt`

**Interfaces:**
- Consumes: Task 2 的 MatchRule.senderPattern/brandPrefix/brandSuffix，PackageRecord 无 isIncomplete
- Produces:
  - `SmsMessage(address: String, threadId: Long?)` — 新增发件方和线程 ID
  - `SmsReader.readSince(context, cutoffMs: Long): List<SmsMessage>` — 按时间窗口读取
  - `MatchEngine.extract(smsBody, rule, smsAddress: String?)` — 加发件方过滤 + 品牌提取
  - `MatchEngine.findBestMatch(smsBody, rules, smsAddress: String?)` — 透传 smsAddress
  - `PackageListViewModel.scanSms` 中 `companyName = fields.brandName ?: rule.name`

- [ ] **Step 1: SmsMessage 扩展**

`SmsMessage` data class 加 `val address: String`、`val threadId: Long?`。
`SmsReader.PROJECTION` 加 `Telephony.Sms.ADDRESS`、`Telephony.Sms.THREAD_ID`。
`readAll()` 读取并填充新字段。

- [ ] **Step 2: SmsReader.readSince(cutoffMs)**

新增方法：selection 为 `"${Telephony.Sms.DATE} >= ?"`，selectionArgs 为 `cutoffMs.toString()`。其余同 readAll。

- [ ] **Step 3: MatchEngine 加发件方过滤**

`extract()` 加参数 `smsAddress: String? = null`。
在方法开头加发件方前置过滤：
```
if (!rule.senderPattern.isNullOrEmpty() && smsAddress != null) {
    if (smsAddress.indexOf(rule.senderPattern) == -1) return ExtractedFields()
}
```
`senderPattern` 为空时不过滤（向后兼容）。

- [ ] **Step 4: MatchEngine 加品牌提取**

`extract()` 中加品牌提取：
```
brandName = extractField(smsBody, rule.brandPrefix, rule.brandSuffix)
```
`brandPrefix` 为空时 `brandName = null`（companyName 回退到 rule.name）。

- [ ] **Step 5: findBestMatch 透传 smsAddress**

`findBestMatch(smsBody, rules)` 加参数 `smsAddress: String? = null`，传给 `extract(smsBody, rule, smsAddress)`。

- [ ] **Step 6: PackageListViewModel 改用新 API**

`scanSms()` 中：
- `SmsReader.readAll(context)` → `SmsReader.readSince(context, cutoffMs)`（cutoffMs 从 SettingsStore.scanWindowDays 计算，暂硬编码 7 天毫秒数）
- `MatchEngine.findBestMatch(sms.body, rules)` → `MatchEngine.findBestMatch(sms.body, rules, sms.address)`
- `companyName = rule.name` → `companyName = fields.brandName ?: rule.name`
- 存入 `smsThreadId = sms.threadId`
- 删除 `isIncomplete = ...` 计算逻辑

- [ ] **Step 7: 验证构建**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 8: 提交**

```bash
git add -A && git commit -m "feat: 匹配引擎发件方过滤 + 品牌提取 + SmsReader 扩展"
```

---

### Task 4: 预置规则升级 + 编辑器新字段

**Files:**
- Modify: `app/src/main/java/com/quickpick/data/preset/PresetRules.kt`
- Modify: `app/src/main/java/com/quickpick/data/local/SettingsStore.kt`
- Modify: `app/src/main/java/com/quickpick/ui/viewmodel/RuleEditorViewModel.kt`
- Modify: `app/src/main/java/com/quickpick/ui/screen/RuleEditorScreen.kt`
- Modify: `app/src/main/java/com/quickpick/util/JsonExporter.kt`

**Interfaces:**
- Consumes: Task 3 的 MatchRule.senderPattern/brandPrefix/brandSuffix
- Produces:
  - 预置规则含 senderPattern + 品牌锚点
  - v1.0.0 用户升级时旧预置规则自动补新字段
  - `EditorState` 含 senderPattern/brandPrefix/brandSuffix
  - `ActiveField` 枚举含 SENDER、BRAND
  - `JsonExporter` 的 JSON 格式含 senderPattern/brand 字段
  - `SettingsStore` 含 `scanWindowDays`、`colorMode`、`useMonet`、`paletteStyle`

- [ ] **Step 1: PresetRules 加 senderPattern + 品牌锚点**

发件方过滤策略（基于调研结论）：`senderPattern` 匹配 SMS `address` 字段（发件方号码），用 `"1069"` 前缀作为粗筛（排除非平台短信如河南水利厅、中国移动）。品牌识别靠 body 中 `【签名】` 提取（brandPrefix/brandSuffix）。

顺丰：`senderPattern = "1069"`，`brandPrefix = "顺丰"`，`brandSuffix = "，"`
京东：`senderPattern = "1069"`，`brandPrefix = "京东"`，`brandSuffix = "，"`

> 注意：1069 号码不与快递公司一一对应（同一家快递用不同 1069 子号段），所以 senderPattern 统一用 `"1069"` 而非公司名。如果某规则需要更严格的发件方过滤（如只匹配顺丰专用号段），用户可自定义更精确的值。

- [ ] **Step 2: 旧预置规则升级逻辑**

`SettingsStore` 加 `KEY_PRESET_V2_SEEDED = booleanPreferencesKey("preset_v2_seeded")`。
`PresetRules.upgradeIfNeeded(store, dao)`：若未 seeded，对 `isPreset=true` 的规则更新 senderPattern/brandPrefix/brandSuffix（按 name 匹配），然后标记 seeded。在 `QuickPickApp.onCreate` 中调用。

- [ ] **Step 3: SettingsStore 扩展**

加 4 个偏好：
- `scanWindowDays: Flow<Int>`（key: `scan_window_days`，默认 7）
- `colorMode: Flow<String>`（key: `color_mode`，默认 "SYSTEM"）
- `useMonet: Flow<Boolean>`（key: `use_monet`，默认 false）
- `paletteStyle: Flow<String>`（key: `palette_style`，默认 "TonalSpot"）
- 对应的 `suspend fun setXxx()` 方法

- [ ] **Step 4: EditorState 扩展**

`EditorState` 加：
- `val senderPattern: String = ""`
- `val brandPrefix: String = ""`
- `val brandSuffix: String = ""`

`ActiveField` 枚举加 `SENDER`、`BRAND`。

- [ ] **Step 5: RuleEditorViewModel 扩展**

`loadRule()`：加 senderPattern/brandPrefix/brandSuffix 读取。
`loadFromJson()`：加新字段读取。
`updateActiveFieldPrefix/Suffix`：加 SENDER → senderPattern、BRAND → brandPrefix/brandSuffix 分支。
`setTokenAsAnchor()`：加 SENDER 和 BRAND 分支。
`saveRule()`：构建 MatchRule 时填入新字段。
`refreshPreview()`：构建预览用 MatchRule 时填入新字段。
`saveRule()` 验证：`hasAnyField` 不计入 senderPattern/brand（它们可选）。

- [ ] **Step 6: RuleEditorScreen 加 2 个 FieldCard**

在"提取字段"区域，4 个 FieldCard 之后加：
- `FieldCard(fieldName = "📮 发件方过滤", dotColor = BrandColors.YTO, ...)`
  - prefix 绑定 `state.senderPattern`，无 suffix（senderPattern 是单字段匹配，不分前后缀）
  - 实际上 senderPattern 用单个 TextField 而非 prefix/suffix 对，FieldCard 需适配或用简化版
- `FieldCard(fieldName = "🏷 品牌提取", dotColor = BrandColors.CAINIAO, ...)`
  - prefix 绑定 `state.brandPrefix`，suffix 绑定 `state.brandSuffix`

senderPattern 的 FieldCard 特殊处理：只显示一个 TextField（不是 prefix/suffix 对），因为发件方过滤是单字符串匹配而非前后缀锚点对。

- [ ] **Step 7: JsonExporter 扩展**

`ruleToJson()`：加 `senderPattern`（顶级字段）和 `fields.brand`（brandPrefix/brandSuffix）。
`parseRule()`：解析 `senderPattern` 和 `fields.brand`。
向后兼容：旧 JSON 无 senderPattern/brand 时默认 null。

- [ ] **Step 8: 验证构建**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 9: 提交**

```bash
git add -A && git commit -m "feat: 预置规则升级 + 编辑器发件方/品牌字段 + SettingsStore 扩展 + JsonExporter 扩展"
```

---

### Task 5: 扫描窗口 + 设置页功能项

**Files:**
- Modify: `app/src/main/java/com/quickpick/ui/viewmodel/PackageListViewModel.kt`
- Modify: `app/src/main/java/com/quickpick/ui/viewmodel/SettingsViewModel.kt`
- Modify: `app/src/main/java/com/quickpick/ui/screen/SettingsScreen.kt`

**Interfaces:**
- Consumes: Task 4 的 `SettingsStore.scanWindowDays`
- Produces: 设置页有扫描窗口四档选项、项目地址跳转 GitHub

- [ ] **Step 1: PackageListViewModel 使用 scanWindowDays**

`scanSms()` 中从 `settingsStore.scanWindowDays.first()` 读取天数，计算 `cutoffMs = now - days * 86400000L`，传给 `SmsReader.readSince(context, cutoffMs)`。
需要在 ViewModel 构造时传入 `SettingsStore`（当前已有 `db`，需加 `store` 参数）。

- [ ] **Step 2: SettingsViewModel 加扫描窗口 + 主题偏好读写**

加 `scanWindowDays: StateFlow<Int>`、`colorMode: StateFlow<String>`、`useMonet: StateFlow<Boolean>`、`paletteStyle: StateFlow<String>`。
加对应的 `setScanWindowDays(days: Int)`、`setColorMode(mode: String)`、`setUseMonet(enabled: Boolean)`、`setPaletteStyle(style: String)` 方法。

- [ ] **Step 3: SettingsScreen 加扫描窗口选项**

在"快捷操作"区域前加"短信扫描"区域：
- `SectionTitle("短信扫描")`
- 四个选项行：一周 / 一月 / 三月 / 半年（选中态用 Primary 色标记）
- 点击调用 `settingsViewModel.setScanWindowDays(n)`

- [ ] **Step 4: 项目地址跳转 GitHub**

`SettingsRow("项目地址")` 的 onClick 改为：
```kotlin
context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/whenk7/quick-pick")))
```

- [ ] **Step 5: 删设置页「全部已取」入口**

删除 `SettingsRow("✓ 一键全部已取", onClick = { confirmAllVisible = true })` 及其相关 `ConfirmDialog`。

- [ ] **Step 6: 验证构建**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 7: 提交**

```bash
git add -A && git commit -m "feat: 扫描窗口四档 + 设置页项目地址跳转 + 删除全部已取入口"
```

---

### Task 6: 主题系统重写

**Files:**
- Modify: `app/src/main/java/com/quickpick/ui/theme/Theme.kt`
- Modify: `app/src/main/java/com/quickpick/ui/theme/Color.kt`
- Modify: `app/src/main/java/com/quickpick/ui/theme/Type.kt`
- Modify: `app/src/main/java/com/quickpick/ui/theme/Shape.kt`
- Modify: `app/src/main/java/com/quickpick/MainActivity.kt`
- Modify: `app/src/main/java/com/quickpick/ui/screen/SettingsScreen.kt`
- Modify: `app/src/main/java/com/quickpick/ui/viewmodel/SettingsViewModel.kt`

**Interfaces:**
- Consumes: Task 4 的 `SettingsStore.colorMode/useMonet/paletteStyle`
- Produces:
  - `MiuixTheme` 包住 `QuickPickTheme`，`ThemeController` 从 DataStore 构建
  - 固定配色模式：豆绿 LightColorScheme / DarkColorScheme
  - Monet 模式：keyColor=#6AB37B，paletteStyle 可选
  - 设置页有明暗切换 + 固定/Monet 切换 + PaletteStyle 选择
  - `AppTypography` 不依赖 M3 `Typography` 类，改为独立 `TextStyles` 对象
  - `Color.kt` 语义常量保留，从 `MiuixTheme.colorScheme` 取值映射

- [ ] **Step 1: 定义豆绿 DarkColorScheme**

在 `Color.kt` 或 `Theme.kt` 中加暗色色板：
```kotlin
val DarkBackground = Color(0xFF1A1A1A)
val DarkSurface = Color(0xFF2C2C2C)
val DarkSurfaceDone = Color(0xFF242424)
val DarkTextPrimary = Color(0xFFE0E0E0)
val DarkTextSecondary = Color(0xFF9E9E9E)
val DarkTextTertiary = Color(0xFF6E6E6E)
val DarkInputBackground = Color(0xFF333333)
val DarkDivider = Color(0xFF3A3A3A)
// Primary/PrimaryLight/Done/Danger 不变（暗色下豆绿和橙色保持原色）
```

- [ ] **Step 2: 重写 Theme.kt**

```kotlin
@Composable
fun QuickPickTheme(content: @Composable () -> Unit) {
    val colorMode = ... // 从 SettingsStore 读
    val useMonet = ...
    val paletteStyle = ...

    val miuixColorMode = when {
        useMonet && colorMode == "DARK" -> ColorSchemeMode.MonetDark
        useMonet && colorMode == "LIGHT" -> ColorSchemeMode.MonetLight
        useMonet -> ColorSchemeMode.MonetSystem
        colorMode == "DARK" -> ColorSchemeMode.Dark
        colorMode == "LIGHT" -> ColorSchemeMode.Light
        else -> ColorSchemeMode.System
    }

    val miuixPaletteStyle = ThemePaletteStyle.valueOf(paletteStyle)

    MiuixTheme(
        controller = remember {
            ThemeController(
                colorSchemeMode = miuixColorMode,
                lightColors = 豆绿LightColors,
                darkColors = 豆绿DarkColors,
                keyColor = if (useMonet) Primary else null,
                paletteStyle = miuixPaletteStyle
            )
        }
    ) {
        // Color.kt 的语义常量在此从 MiuixTheme.colorScheme 映射
        // 或直接在 Color.kt 中提供 @Composable getter
        content()
    }
}
```

豆绿 LightColors / DarkColors：用 miuix 的 `lightColorScheme()` / `darkColorScheme()` 构建，各 token 赋豆绿色板值。

- [ ] **Step 3: AppTypography 脱离 M3**

将 `AppTypography` 从 `Typography()`（M3 类）改为 miuix 的 `TextStyles()` 或自定义 data class：
```kotlin
val AppTypography = TextStyles(  // miuix 类
    title1 = TextStyle(fontSize = 28.sp, fontWeight = FontWeight.Bold, ...),  // 取件码 / 页面标题
    title4 = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Medium, ...),  // 分组标题
    headline1 = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Medium, ...),  // 公司名
    body1 = TextStyle(fontSize = 15.sp, ...),  // 正文
    body2 = TextStyle(fontSize = 14.sp, ...),  // 辅助
    footnote1 = TextStyle(fontSize = 12.sp, ...),  // 小字
    subtitle = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.SemiBold, ...),  // 按钮
)
```

映射关系（M3→miuix token）：
- `headlineLarge` → `title1`（28sp→32sp，保持 28sp 需覆盖）
- `headlineMedium` → `title2`（28sp→24sp，同上）
- `titleLarge` → `title4`（18sp=18sp，匹配）
- `titleMedium` → `headline1`（16sp→17sp，接近）
- `bodyLarge` → `main`（15sp→17sp，保持 15sp 需覆盖）
- `bodyMedium` → `body1`（14sp→16sp，保持 14sp 需覆盖）
- `bodySmall` → `body2`（12sp→14sp，保持 12sp 需覆盖）
- `labelLarge` → `subtitle`（15sp SemiBold→14sp Bold，接近）

由于 miuix 的 TextStyles token 大小与设计系统不完全对齐，保持自定义字号覆盖 miuix 默认值。

- [ ] **Step 4: AppShapes 适配**

miuix 用 `cornerRadius: Dp` 而非 `Shapes` 类。删除 `AppShapes`（M3 类），各组件直接用 `RoundedCornerShape(16.dp)` 等硬编码值（当前已这样用，`MaterialTheme.shapes.large` 仅 3 处引用，直接替换）。

- [ ] **Step 5: Color.kt 语义常量适配**

`Color.kt` 的常量（Primary, Background, Surface 等）改为 `@Composable` getter，从 `MiuixTheme.colorScheme` 取值，暗色模式下自动切换：
```kotlin
val Background: Color @Composable get() = MiuixTheme.colorScheme.background
val Surface: Color @Composable get() = MiuixTheme.colorScheme.surface
val TextPrimary: Color @Composable get() = MiuixTheme.colorScheme.onBackground
// Primary 保持固定 #6AB37B（不受 Monet 影响，豆绿是品牌色）
```

或者：在 `QuickPickTheme` 内用 `CompositionLocal` 提供语义色，Color.kt 的常量从 LocalProvider 读取。选择更简方案：直接 `@Composable get()`。

- [ ] **Step 6: MainActivity 接入主题偏好**

从 DataStore 读 colorMode/useMonet/paletteStyle 构建初始 ThemeController。接受首帧默认配色闪烁（DataStore 异步读取期间）。

- [ ] **Step 7: 设置页主题控件**

加"主题"区域：
- 明暗：跟随系统 / 浅色 / 深色（三选一）
- 动态取色：关闭 / 开启（开关）
- 取色风格（Monet 开启时显示）：TonalSpot / Vibrant / Expressive（三选一）

- [ ] **Step 8: 验证构建 + 主题切换**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL，设置页可切换明暗/Monet

- [ ] **Step 9: 提交**

```bash
git add -A && git commit -m "feat: 主题系统重写 — MiuixTheme + 豆绿暗色板 + 动态取色 + 设置页控件"
```

---

### Task 7: M3→miuix 组件迁移

**Files:**
- Modify: 全部 14 个 UI 文件（见设计文档文件改动清单）
- 重点：替换 `import androidx.compose.material3.*` 为 miuix 导入

**Interfaces:**
- Consumes: Task 6 的 MiuixTheme + AppTypography + Color 语义常量
- Produces: 零（或极少量）M3 import 的 UI 层

**关键迁移规则：**

| M3 用法 | miuix 替代 |
|---------|-----------|
| `import androidx.compose.material3.*` | `import top.yukonga.miuix.kmp.basic.*` |
| `MaterialTheme.typography.bodySmall` | `MiuixTheme.textStyles.body2`（或 `AppTypography.xxx`） |
| `MaterialTheme.colorScheme.surface` | `MiuixTheme.colorScheme.surface` |
| `Card(colors = CardDefaults.cardColors(...), elevation = ...)` | `Card(colors = CardDefaults.defaultColors(), cornerRadius = 16.dp)` + `Modifier.shadow(2.dp, ...)` |
| `Button(colors = ButtonDefaults.buttonColors(containerColor = Primary), shape = ...)` | `Button(colors = ButtonDefaults.buttonColorsPrimary())` 或自定义 colors |
| `TextButton(onClick) { Text("xxx") }` | `TextButton(text = "xxx", onClick = ...)`（miuix 直接接 String） |
| `OutlinedButton(...)` | `TextButton(text = "...", onClick = ..., colors = ...)` + 边框 Modifier（miuix 无 OutlinedButton） |
| `OutlinedTextField(...)` | `TextField(value, onValueChange, label = "xxx", cornerRadius = 16.dp)` |
| `AlertDialog(onDismissRequest, shape, containerColor, title, text, confirmButton, dismissButton)` | `OverlayDialog(show = visible, title = "...", summary = "...", onDismissRequest = ...)` + 内部按钮 |
| `Scaffold(contentWindowInsets = WindowInsets(0))` | `Scaffold(contentWindowInsets = WindowInsets(0))`（同名参数，行为一致） |
| `NavigationBar` + `NavigationBarItem` | `FloatingNavigationBar`（Task 8 替换，此步先保留占位） |
| `PullToRefreshBox(isRefreshing, onRefresh)` | `PullToRefresh(isRefreshing, onRefresh)` |
| `HorizontalDivider(color = ...)` | `Divider(color = ...)`（miuix Divider） |
| `Surface(color = Background)` | `Box(modifier = Modifier.fillMaxSize().background(Background))` |

- [ ] **Step 1: 替换 Scaffold**

`AppNavigation.kt`、`PackageListScreen.kt`、`RuleListScreen.kt`、`RuleEditorScreen.kt`、`AiPromptScreen.kt` 中的 `Scaffold` 改为 miuix Scaffold。确保 `MiuixPopupHost` 通过 Scaffold 的 `popupHost` 参数生效。

- [ ] **Step 2: 替换 Card + 删除 elevation**

全局替换 `Card(colors = CardDefaults.cardColors(...), elevation = CardDefaults.cardElevation(...))` 为 `Card(colors = ..., cornerRadius = 16.dp)` + `Modifier.shadow(N.dp, RoundedCornerShape(16.dp))`。
`CardDefaults.cardColors(containerColor = ...)` → `CardDefaults.defaultColors().copy(color = ...)`（miuix CardColors 只有 `color` + `contentColor`）。

- [ ] **Step 3: 替换 Button / TextButton / IconButton**

Button: `ButtonDefaults.buttonColors(containerColor = Primary)` → `ButtonDefaults.buttonColorsPrimary()` 或自定义 `ButtonColors(color = Primary, contentColor = Color.White)`。
TextButton: `{ Text("xxx") }` → `text = "xxx"`。
IconButton: 基本兼容，改导入即可。

- [ ] **Step 4: 替换 OutlinedTextField → miuix TextField**

`OutlinedTextField(value, onValueChange, label = { Text("xxx") }, shape = MaterialTheme.shapes.large)` → `TextField(value, onValueChange, label = "xxx", cornerRadius = 16.dp)`。
6 处引用：FieldCard×2、RuleEditorScreen×2、AiPromptScreen×2。

- [ ] **Step 5: 替换 AlertDialog → OverlayDialog**

`ConfirmDialog` 重写：用 `OverlayDialog(show = visible, title = ..., summary = ..., onDismissRequest = ...)` + 内部 Row 放确认/取消按钮。
注意：OverlayDialog 依赖 Scaffold 的 MiuixPopupHost，需确保调用页面在 miuix Scaffold 内。

- [ ] **Step 6: 替换 PullToRefreshBox → PullToRefresh**

`PullToRefreshBox(isRefreshing, onRefresh)` → `PullToRefresh(isRefreshing = ..., onRefresh = ...)`。

- [ ] **Step 7: 替换 MaterialTheme.typography → AppTypography**

全局替换 `MaterialTheme.typography.headlineLarge` → `AppTypography.title1` 等。
如 AppTypography 的字号与设计系统不完全对齐，在 AppTypography 中用自定义 TextStyle 覆盖 miuix 默认值保持原字号。

- [ ] **Step 8: 替换 Surface / Divider / 其他**

`Surface(color = Background)` → `Box(Modifier.fillMaxSize().background(Background))`。
`HorizontalDivider(color = Divider)` → `Divider(color = Divider)`（miuix Divider）。
其他少量 M3 引用逐个替换。

- [ ] **Step 9: 删除 M3 依赖残留**

确认 `grep -r "import androidx.compose.material3" app/src/` 返回零（或仅 PullToRefresh/BottomNavBar 占位保留的必要引用）。

- [ ] **Step 10: 列表页 UI 清理（与迁移同步）**

- PackageListScreen: 删顶部 `Button("✓ 全部已取")` 整段
- PackageCard: 删 `isIncomplete` 相关的 `Text("不完整")` 和 `Text("信息不完整...")`（已在 Task 2 删数据层，此步删 UI 层残余）
- DayGroup: 「当日已取」TextButton 的 onClick 直接调 `onMarkDayPickedUp(dateMs)`，删二次确认弹窗

- [ ] **Step 11: 验证构建 + 全屏遍历**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL
手动验证：快递列表、规则列表、规则编辑、AI 生成、设置页、权限引导——所有页面渲染正常

- [ ] **Step 12: 提交**

```bash
git add -A && git commit -m "feat: M3→miuix 组件全面迁移 + 列表页 UI 清理"
```

---

### Task 8: 悬浮玻璃底栏

**Files:**
- Create: `app/src/main/java/com/quickpick/ui/component/glass/LiquidGlassNavigationBar.kt`
- Create: `app/src/main/java/com/quickpick/ui/component/glass/GlassBackdrop.kt`（合并 Lens/Vibrancy/InnerShadow/CombinedBackdrop 为单文件，简化）
- Create: `app/src/main/java/com/quickpick/ui/component/glass/GlassEffects.kt`（Highlight + BloomStroke + LightSource）
- Create: `app/src/main/java/com/quickpick/ui/component/glass/DampedDragAnimation.kt`
- Modify: `app/src/main/java/com/quickpick/ui/component/BottomNavBar.kt`
- Modify: `app/src/main/java/com/quickpick/ui/navigation/AppNavigation.kt`
- Modify: `app/build.gradle.kts`（加 miuix-blur-android 依赖）

**Interfaces:**
- Consumes: Task 7 的 miuix Scaffold，`BottomTab` 枚举
- Produces: 替换 BottomNavBar 为玻璃效果悬浮底栏

- [ ] **Step 1: 加 miuix-blur 依赖**

`app/build.gradle.kts` 加：
```kotlin
implementation("top.yukonga.miuix.kmp:miuix-blur-android:0.9.4")
```

- [ ] **Step 2: 实现玻璃效果组件**

基于 miuix-blur 的 `Modifier.textureBlur()` 和 `Modifier.drawBackdrop()` API：

```kotlin
// GlassBackdrop.kt — LayerBackdrop + blur + highlight
@Composable
fun rememberGlassBackdrop(): LayerBackdrop {
    return rememberLayerBackdrop(onDraw = {
        drawRect(color = MiuixTheme.colorScheme.surface.copy(alpha = 0.4f))
        drawContent()
    })
}

// GlassEffects.kt — Highlight + BloomStroke + rememberTiltLight
// 使用 miuix-blur 的 Highlight、BloomStroke、LightSource 类
// 参数：blurRadius=4dp, highlight=GlassStrokeMiddleLight, tiltLight with sensitivity=0.1f
```

- [ ] **Step 3: 实现 LiquidGlassNavigationBar**

使用 `Modifier.textureBlur(backdrop, shape, blurRadius=4f, highlight=...)` 或 `Modifier.drawBackdrop(backdrop) { blur(4f) }` 渲染玻璃效果。
3 个 Tab（📦🧩⚙️ emoji），选中态 pill 背景（豆绿 12% 透明度），弹簧动画。
栏高 64dp，距底 12dp + navigationBarsPadding。

- [ ] **Step 4: 实现 DampedDragAnimation**

弹簧动画 `spring(1f, 300f, 0.5f)`，指示器拖拽回弹。

- [ ] **Step 5: 接入 AppNavigation**

`MainScreen` 的 `Scaffold(bottomBar = { BottomNavBar(...) })` 改为：
```kotlin
Scaffold(bottomBar = {
    LiquidGlassNavigationBar(
        currentTab = currentTab,
        onTabClick = { currentTab = it }
    )
})
```

- [ ] **Step 6: 验证构建 + 玻璃效果**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL，底栏显示玻璃模糊效果

- [ ] **Step 7: 提交**

```bash
git add -A && git commit -m "feat: 悬浮玻璃底栏 — blur+lens+highlight 弹簧动画"
```

---

### Task 9: 功能补齐 — JSON 弹窗 + 跳短信 + 主题设置 + 吸顶

**Files:**
- Create: `app/src/main/java/com/quickpick/ui/component/JsonPreviewDialog.kt`
- Modify: `app/src/main/java/com/quickpick/ui/screen/RuleListScreen.kt`
- Modify: `app/src/main/java/com/quickpick/ui/component/PackageCard.kt`
- Modify: `app/src/main/java/com/quickpick/ui/component/DayGroup.kt`
- Modify: `app/src/main/java/com/quickpick/ui/screen/SettingsScreen.kt`

**Interfaces:**
- Consumes: Task 7 的 OverlayDialog，Task 2 的 smsThreadId
- Produces: JSON 弹窗 + 卡片跳短信 + DayGroup 吸顶 + 设置页主题控件（如 Task 6 未完成）

- [ ] **Step 1: JsonPreviewDialog**

```kotlin
@Composable
fun JsonPreviewDialog(
    show: Boolean,
    json: String,
    onDismiss: () -> Unit
)
```
用 `OverlayDialog(show, title = "规则 JSON", summary = null)` + 内部 `LazyColumn`（maxHeight 限制）+ 底部 `TextButton(text = "复制", onClick = { clipboard; toast })`。

- [ ] **Step 2: RuleListScreen — ↗ 换 `</>` 图标 + JSON 弹窗**

RuleCard 的 `IconButton(onClick = onExport)` 中 `Text("↗")` → `Text("</>")`。
点击调出 `JsonPreviewDialog(show = jsonDialogVisible, json = JsonExporter.exportSingle(rule), ...)`。
分享功能移到长按或另加按钮（保留导出能力）。

- [ ] **Step 3: PackageCard — 跳原短信**

卡片整体或详情区加跳转入口：
```kotlin
record.smsThreadId?.let { threadId ->
    TextButton(text = "查看短信", onClick = {
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("sms:$threadId")))
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(context, "无法打开短信应用", Toast.LENGTH_SHORT).show()
        }
    })
}
```

- [ ] **Step 4: DayGroup stickyHeader**

`LazyColumn` 中 DayGroup 的分组标题改用 `stickyHeader`：
```kotlin
stickyHeader(key = "day-$dateMs") {
    // 分组标题行，背景色 = Background（遮住下方滚动内容）
}
```
折叠视觉区分：折叠态加底色 `SurfaceDone` + 左侧 3dp Primary 色线。

- [ ] **Step 5: 验证构建**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 6: 提交**

```bash
git add -A && git commit -m "feat: JSON 弹窗 + 跳原短信 + DayGroup 吸顶 + 折叠视觉区分"
```

---

### Task 10: 全局验证 + 版本号 + 清理

**Files:**
- Modify: `app/build.gradle.kts`（versionCode/versionName）
- Modify: `docs/superpowers/specs/2026-10-21-miuix-migration-design.md`（进度更新）
- 可能修改: CLAUDE.md

**Interfaces:**
- Consumes: 所有前序 task 的产出

- [ ] **Step 1: Release 构建验证**

Run: `./gradlew assembleRelease`
Expected: BUILD SUCCESSFUL

- [ ] **Step 2: APK 体积检查**

Run: `ls -la app/build/outputs/apk/release/app-release.apk`
Expected: < 5MB（v1.0.0 是 2.84MB）

- [ ] **Step 3: M3 引用清零检查**

Run: `grep -r "import androidx.compose.material3" app/src/`
Expected: 零引用或仅不可避免的 pulltorefresh 残留（如有）

- [ ] **Step 4: 版本号更新**

`app/build.gradle.kts`：
- `versionCode = 2`
- `versionName = "1.1.0"`

- [ ] **Step 5: 提交**

```bash
git add -A && git commit -m "chore: v1.1.0 版本号 + 全局验证通过"
```
