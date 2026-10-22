# 取件宝 v1.1.0 — Miuix 迁移 + 引擎修复 + 功能补齐

> 日期：2026-10-21
> 上一轮版本：v1.0.0（M3 组件库 + Compose BOM 2026.06）
> 本轮目标：全面替换 M3 为 miuix KMP，修复匹配引擎硬缺陷，补齐测试文档中的功能项

---

## 一、意图与动机

v1.0.0 功能完整但存在两类问题：

1. **匹配引擎缺陷导致误匹配/漏匹配**——无发件方过滤、无品牌自动识别、同品牌多模板冲突。这是功能正确性问题，必须修。
2. **UI 层停留在 M3 默认风格**——底部导航笨重、无视觉层级、无深色/动态取色支持。用户实际体验了 miuix 后明确要求迁移。

两个问题在一个版本里一起解决：引擎修复是数据正确性底线，miuix 迁移是体验升级。

---

## 二、目标

| # | 目标 | 验收标准 |
|---|------|---------|
| 1 | 匹配引擎加发件方过滤 + 品牌自动识别 | 河南水利厅、中国移动等非快递短信不再入库；圆通多模板不再冲突；品牌名从短信正文自动提取 |
| 2 | 6 屏 + 自定义组件全部从 M3 迁移到 miuix | `import androidx.compose.material3` 零引用；构建绿 |
| 3 | 主题系统全模式 | 明暗（System/Light/Dark）+ 取色（固定豆绿/Monet 动态取色）可独立切换，设置页有控件 |
| 4 | 悬浮玻璃底栏 | 替换当前 M3 NavigationBar；blur+lens+vibrancy+highlight；API 30-32 降级纯模糊 |
| 5 | 短信扫描窗口 | 首启默认近一周；设置页四档（一周/一月/三月/半年） |
| 6 | 规则 JSON 弹窗 | 规则卡片 `</>` 图标 → 弹窗显示格式化 JSON + 一键复制 |
| 7 | 卡片跳原短信 | 点击快递卡片跳系统短信会话（Intent） |
| 8 | 列表页清理 | 移除顶部全局「全部已取」按钮；移除「不完整」标签及提示文案；「当日已取」直执行不弹确认 |
| 9 | 设置页补齐 | 项目地址点击跳 GitHub；移除设置页「全部已取」入口（同 #8） |

---

## 三、非目标

- 不做物流追踪、云端同步、AI 内置 API（延续 v1.0.0 口径）
- 不做 strings.xml 国际化
- 不做 Hilt 依赖注入
- 玻璃底栏的手感参数（阻尼、折射量、高光角度）第一版可能不完美，后续真机调参
- 不引入 miuix-preference 模块（设置页用自写 Compose 即可，preference 是 XML 时代的桥接）

---

## 四、反目标（做了就算失败）

- 构建红或 APK 膨胀超 5MB（v1.0.0 是 2.84MB，miuix 新增不应翻倍）
- 原有功能回归：短信扫描、规则编辑、导入导出、权限引导任何一项退化
- Room 数据库迁移丢数据（v1.0.0 用户升级后数据必须完好）
- 耗电/内存明显增加（blur 效果是渲染层，不应拖累后台）

---

## 五、约束

- **版本矩阵**（spike/miuix-align 已验证构建绿）：

| 项 | v1.0.0 | v1.1.0 |
|----|--------|--------|
| Kotlin | 2.3.20 | 2.4.21 |
| AGP | 8.13.0 | 9.4.1 |
| Gradle | 8.14.3 | 9.8.1 |
| compileSdk/targetSdk | 36 | 37 |
| Compose BOM | 2026.06.01 | 2026.09.00 |
| miuix | — | 0.9.4 |

- `org.jetbrains.kotlin.android` 插件已废除（AGP 9.0+ 内置 Kotlin），从 build.gradle.kts 移除
- miuix 0.9.4 按 Compose 1.12.0 / Kotlin 2.4.20 构建，spike 验证与 2.4.21 / BOM 2026.09.00 兼容
- minSdk 30 → 33（miuix-blur 0.9.4 的 minSdk 为 33，与其对齐）
- 现有 Room schema version=1，本版需要 migration 到 version=2（加新列）

---

## 六、匹配引擎修复（A 类）

### 6.1 发件方过滤

**问题**：当前引擎只用短信正文匹配，不检查发件方。河南水利厅、中国移动等短信正文里恰好含"取件码"等关键词就被误入库。

**方案**：MatchRule schema 加 `senderPattern: String?` 字段。匹配时先检查短信发件方是否匹配：

```
短信发件方 = SmsReader 读出的 address 字段（发件人号码/签名）
senderPattern 非空 → 发件方 must contain senderPattern（indexOf 锚点，不用正则）
senderPattern 为空 → 不过滤发件方（向后兼容旧规则）
```

**改动**：

- `SmsMessage` data class 加 `address: String` 字段
- `SmsReader.PROJECTION` 加 `Telephony.Sms.ADDRESS`
- `MatchRule` / `MatchRuleEntity` 加 `senderPattern` 字段
- `MatchEngine.findBestMatch` 加发件方前置过滤
- Room migration v1→v2：`match_rules` 表加 `sender_pattern TEXT` 列
- 预置规则加 senderPattern（顺丰签名 "sf-express" / "顺丰" 等）
- v1.0.0 用户升级时自动补旧预置规则的 senderPattern / brand 锚点

### 6.2 品牌自动识别

**问题**：`companyName` 当前硬取 `rule.name`，无法从短信正文自动提取品牌。妈妈驿站的短信正文含"圆通快递"但公司名显示为规则名。

**方案**：MatchRule schema 加 `brandPrefix: String?` / `brandSuffix: String?` 锚点对，与其他字段一样走 `extractField` 逻辑。

```
brandPrefix 非空 → 从正文提取品牌名 → 填入 PackageRecord.companyName
brandPrefix 为空 → companyName = rule.name（向后兼容）
```

**改动**：

- `MatchRule` / `MatchRuleEntity` 加 `brandPrefix` / `brandSuffix`
- `ExtractedFields` 加 `brandName: String?`
- `MatchEngine.extract` 加品牌提取
- `PackageListViewModel.scanSms` 用 `fields.brandName ?: rule.name` 填 companyName
- 预置规则配品牌锚点（如顺丰："顺丰速运" prefix="顺丰" suffix="，"）
- Room migration v1→v2：`match_rules` 表加 `brand_prefix TEXT`、`brand_suffix TEXT` 列

### 6.3 同品牌多模板消歧

**问题**：两条同为圆通的短信（妈妈驿站模板 vs 圆通官方模板），同一条规则匹配时一条正常另一条解析异常。现有"字段数多者优先"不够——两者可能字段数相同但锚点完全不同。

**方案**：允许多条规则同名（同品牌），各自配不同锚点。匹配优先级不变（字段数多者优先，同数按顺序）。用户为同一品牌建多条规则即可覆盖不同短信模板。

当前 schema 已支持（`name` 不是唯一键，`id` 才是 PK），无需改表结构。预置规则可以为同一品牌配多条。

---

## 七、功能补齐（B 类）

### 7.1 短信扫描窗口

**问题**：首启一次性读全部 8000+ 历史短信，卡顿。

**方案**：

- `SettingsStore` 加 `scanWindowDays: Int`，枚举值 `7 / 30 / 90 / 180`
- `SmsReader.readAll` 改为 `readSince(cutoffMs: Long)`，SQL selection 按 `DATE >= cutoffMs` 过滤
- `PackageListViewModel.scanSms` 读取 scanWindowDays 计算 cutoff
- 首次启动时 scanWindowDays 默认 7；设置页加四档选项

### 7.2 规则 JSON 弹窗

**问题**：规则卡片当前只有 ↗ 分享和 🗑 删除按钮，无法查看规则 JSON。

**方案**：

- RuleCard 的 ↗ 按钮替换为 `</>` 图标
- 点击 → `AlertDialog`（miuix 迁移后用 miuix 的 dialog）展示格式化 JSON
- 弹窗最大宽高受限，内容溢出出滚动条
- 弹窗内置「复制」按钮，一键复制 JSON 文本

### 7.3 卡片跳原短信

**问题**：快递卡片无入口跳系统短信。

**方案**：

- `PackageRecordEntity` 加 `sms_thread_id: Long?` 列
- `SmsReader.PROJECTION` 加 `Telephony.Sms.THREAD_ID`
- `SmsMessage` 加 `threadId: Long?`
- `PackageListViewModel.scanSms` 存入 threadId
- PackageCard 点击 → Intent 跳转 `sms:{threadId}`（兼容性尽力而为，部分 ROM 可能不响应）
- Room migration v1→v2：`package_records` 表加 `sms_thread_id INTEGER` 列

### 7.4 列表页清理

- **移除顶部全局「全部已取」按钮**（PackageListScreen 里 `Button("✓ 全部已取")` 删掉）
- **完全删除 isIncomplete**：UI 显示 + PackageRecord.isIncomplete 属性 + scanSms 计算逻辑 + Room `is_incomplete` 列
- **「当日已取」直执行不弹确认**（DayGroup 里 `confirmVisible` 逻辑删掉，`onMarkDayPickedUp` 直接调用）

### 7.5 设置页补齐

- **项目地址跳转**：`SettingsRow("项目地址")` 的 onClick 改为 `Intent(ACTION_VIEW, Uri.parse("https://github.com/whenk7/quick-pick"))`
- **移除「全部已取」入口**：设置页 `SettingsRow("✓ 一键全部已取")` 删掉

---

## 八、Miuix 迁移（C 类）

### 8.1 依赖引入

Spike 已验证。在 spike/miuix-align 分支上：

```kotlin
// app/build.gradle.kts
implementation("top.yukonga.miuix.kmp:miuix-ui-android:0.9.4")

// 迁移 blur 模块时再加：
// implementation("top.yukonga.miuix.kmp:miuix-blur-android:0.9.4")
```

版本矩阵已对齐（AGP 9.4.1 / Gradle 9.8.1 / BOM 2026.09.00 / compileSdk 37）。

### 8.2 主题系统

**架构**：

```
MiuixTheme(controller = ThemeController)  ← 最外层，提供 miuix 色彩/形状/动效
  └─ QuickPickTheme  ← 内层，提供豆绿色板 token（Color.kt 的语义别名）
```

- `ThemeController` 由启动时从 DataStore 读出的偏好构建
- DataStore 存三项：`colorMode`（System/Light/Dark）、`useMonet`（Boolean）、`paletteStyle`（TonalSpot/Vibrant/Expressive，Monet 开启时有效）
- 固定配色模式：`ThemeController(lightColors = 豆绿LightColors, darkColors = 豆绿DarkColors)`
- Monet 模式：`ThemeController` 使用 `keyColor = #6AB37B`（豆绿作种子色）+ 用户选的 PaletteStyle

**豆绿色板保留**：现有设计系统的手写色板降级为"固定配色模式"的默认值，不废弃。miuix 的 `MiuixTheme.colors` 提供动态色板，`QuickPickTheme` 在其上叠加豆绿 token 覆盖。

**设置页**：主题区域用两个独立控件：
- 明暗：跟随系统 / 浅色 / 深色（三选一）
- 取色：固定配色 / 动态取色（开关），动态取色下可选 TonalSpot / Vibrant / Expressive

### 8.3 组件迁移映射

当前 M3 组件 → miuix 替代（基于代码扫描实际引用数）：

| M3 组件 | 引用数 | 使用文件 | miuix 替代 | 备注 |
|---------|--------|---------|-----------|------|
| `MaterialTheme.typography` | 89 | 全部 14 个 UI 文件 | `MiuixTheme.typography` | 最大工作量，逐文件替换 |
| `Text`（M3） | 85 | 全部 UI 文件 | `Text`（Compose 基础） | Text 本身不依赖 M3，改 typography 引用即可 |
| `Scaffold` | 7 | PackageListScreen×2, RuleListScreen×2, AiPromptScreen, RuleEditorScreen, AppNavigation | `Scaffold`（miuix） | API 相近 |
| `Card` + `CardDefaults` | 8+11 | PackageCard, FieldCard, RuleListScreen, AiPromptScreen, PackageListScreen, OnboardingScreen, RuleEditorScreen, SettingsScreen | `Card`（miuix） | 样式参数略有不同 |
| `Button` + `ButtonDefaults` | 9 | PackageListScreen×3, AiPromptScreen×3, OnboardingScreen×2, RuleEditorScreen | `Button`（miuix） | — |
| `TextButton` | 9 | PackageCard×2, ConfirmDialog×2, PackageListScreen×2, RuleEditorScreen, OnboardingScreen, DayGroup | `TextButton`（miuix） | — |
| `OutlinedButton` | 1 | RuleListScreen | `OutlinedButton`（miuix） | — |
| `IconButton` | 5 | RuleListScreen×2, AiPromptScreen, RuleEditorScreen, PackageCard | `IconButton`（miuix） | — |
| `AlertDialog` | 1 | ConfirmDialog | `AlertDialog`（miuix） | — |
| `NavigationBar` + `NavigationBarItem` | 1+1 | BottomNavBar | 自写 `FloatingBottomBar` | 见 8.4 |
| `OutlinedTextField` | 6 | AiPromptScreen×2, RuleEditorScreen×2, FieldCard×2 | `OutlinedTextField`（miuix） | — |
| `PullToRefreshBox` | 1 | PackageListScreen | 保留 M3 或切独立包 | miuix 无此组件 |
| `HorizontalDivider` | 1 | SettingsScreen | Compose 基础包 | 改导入路径 |
| `Surface`（M3 composable） | 2 | MainActivity, AppNavigation | `Surface`（Compose 基础） | 非 M3 组件 |

**不迁移的**：
- `PullToRefreshBox`：miuix 没有此组件，从 `androidx.compose.material3.pulltorefresh` 改为 `androidx.compose.material3` 之外的独立 compose 包（或保留 M3 pulltorefresh 单独依赖）
- `WindowInsets` / `Modifier` / `Layout` / `Spacer` / `Text`（基础 Composable）等基础 Compose API：不受 M3 迁移影响

**未使用的 M3 组件**（无需迁移）：TopAppBar, Switch, Checkbox, Slider, DropdownMenu, ModalBottomSheet, Tab/TabRow, FAB, ListItem, Badge, Snackbar, Tooltip, Chip 系列, Icon（M3 composable），ExperimentalMaterial3Api

### 8.4 悬浮玻璃底栏

**架构**（改编自 miuix 官方 example，Apache-2.0）：

```
FloatingBottomBar
├── Backdrop 层（miuix-blur）
│   ├── vibrancy()       提升鲜艳度
│   ├── blur(4dp)        高斯模糊
│   ├── lens(24dp)       光线折射+色差
│   └── highlight        双光源高光描边
├── 内容层
│   ├── 3 个 Tab（📦🧩⚙️ emoji 图标）
│   ├── 选中态 pill 背景（豆绿 12% 透明度）
│   └── 指示器（弹簧动画 spring(1f, 300f, 0.5f)）
└── 无传感器设备降级为静态高光（无重力跟随）
```

**参数**（数值来自 InstallerX，GPL 不保护参数值）：

| 参数 | 值 |
|------|---|
| 底层 pill | blur 4dp + lens 折射 24dp + 容器色 40% 透明 |
| 纯模糊降级 | 不需要（minSdk 33，blur/lens 均可用） |
| 高光 | 白 12% 透明度、内糊 2dp、主光强 1.0 / 副光 0.4、dualPeak |
| 拖拽弹簧 | spring(1f, 300f, 0.5f) |
| 栏高 | 64dp |
| 内边距 | 4dp |
| 距底 | 12dp + 手势条 inset |
| 阴影 | 10dp 半径，浅色 10% / 深色 20% 黑 |

**移植来源**：miuix 官方 example 的 7 个文件（Apache-2.0）：

```
component/liquid/LiquidGlassNavigationBar.kt
component/liquid/{Lens, Vibrancy, InnerShadow, CombinedBackdrop}.kt
component/animation/{DampedDragAnimation, InteractiveHighlight}.kt
```

改包名 + 导入后放入 `com.quickpick.ui.component.glass/`。

重力传感器高光（`rememberDeviceTilt`）和拖拽指示器在第一版包含，手感参数后续真机微调。

### 8.5 列表页视觉升级

测试文档要求的三项视觉改造与 miuix 迁移同步做：

1. **日期标题吸顶**：LazyColumn 用 `stickyHeader` 替代当前 `item`
2. **折叠视觉区分**：DayGroup 折叠态加底色 + 左侧色线；展开态保持白卡
3. **卡片独立圆角+阴影**：PackageCard 已有圆角 16dp 和 elevation，确认视觉足够后微调阴影参数

### 8.6 MainActivity 入口

```kotlin
setContent {
    MiuixTheme(controller = remember { ThemeController(mode) }) {
        QuickPickTheme {
            Surface(modifier = Modifier.fillMaxSize(), color = Background) {
                AppNavigation()
            }
        }
    }
}
```

`mode` 从 DataStore 读出构建。Spike 中已冒烟验证。

---

## 九、数据模型变更（Room Migration v1→v2）

### match_rules 表加列

| 新列 | 类型 | 默认值 | 用途 |
|------|------|--------|------|
| `sender_pattern` | TEXT | NULL | 发件方过滤锚点 |
| `brand_prefix` | TEXT | NULL | 品牌名提取前缀 |
| `brand_suffix` | TEXT | NULL | 品牌名提取后缀 |

### package_records 表加列

| 新列 | 类型 | 默认值 | 用途 |
|------|------|--------|------|
| `sms_thread_id` | INTEGER | NULL | 跳转系统短信会话 |

### package_records 表删列

| 删列 | 原类型 | 原用途 |
|------|--------|--------|
| `is_incomplete` | INTEGER | 不完整标记（UI 不再使用，数据层一并清除） |

> 注意：Room 不原生支持 DROP COLUMN（SQLite 3.35.0+ 才支持），需通过创建新表+数据迁移+删旧表+重命名实现，或利用 Room auto-migration（如可用）。

### Migration 策略

```kotlin
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // match_rules: 加列（默认 NULL，不需要回填）
        db.execSQL("ALTER TABLE match_rules ADD COLUMN sender_pattern TEXT")
        db.execSQL("ALTER TABLE match_rules ADD COLUMN brand_prefix TEXT")
        db.execSQL("ALTER TABLE match_rules ADD COLUMN brand_suffix TEXT")
        // package_records: 加列
        db.execSQL("ALTER TABLE package_records ADD COLUMN sms_thread_id INTEGER")
        // package_records: 删 is_incomplete 列（SQLite < 3.35 不支持 DROP COLUMN，走新表迁移）
        db.execSQL("""
            CREATE TABLE package_records_new (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                company_name TEXT NOT NULL,
                tracking_number TEXT,
                pickup_code TEXT,
                address TEXT,
                phone TEXT,
                sms_body TEXT NOT NULL,
                sms_timestamp INTEGER NOT NULL,
                is_picked_up INTEGER NOT NULL,
                rule_id INTEGER,
                sms_id TEXT,
                sms_thread_id INTEGER,
                created_at INTEGER NOT NULL,
                FOREIGN KEY(rule_id) REFERENCES match_rules(id) ON UPDATE NO ACTION ON DELETE SET NULL
            )
        """)
        db.execSQL("""
            INSERT INTO package_records_new
                (id, company_name, tracking_number, pickup_code, address, phone,
                 sms_body, sms_timestamp, is_picked_up, rule_id, sms_id, sms_thread_id, created_at)
            SELECT id, company_name, tracking_number, pickup_code, address, phone,
                   sms_body, sms_timestamp, is_picked_up, rule_id, sms_id, NULL, created_at
            FROM package_records
        """)
        db.execSQL("DROP TABLE package_records")
        db.execSQL("ALTER TABLE package_records_new RENAME TO package_records")
        // 重建索引（对应 Entity 的 @Index 注解，索引名遵循 Room 约定）
        db.execSQL("CREATE INDEX IF NOT EXISTS index_package_records_sms_timestamp ON package_records (sms_timestamp)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_package_records_rule_id ON package_records (rule_id)")
    }
}
```

> 2026-10-22 已按 `AppDatabase.kt` 实际实现同步。原草案 4 处错误已修正：多写 `updated_at` 列（实际表中不存在）、`rule_id`/`sms_id`/`is_picked_up`/`created_at` 错标 NOT NULL DEFAULT（实际可空/无默认）、缺外键（SET_NULL）、INSERT SELECT 选不存在列且未显式列名。

match_rules 纯加列（默认 NULL），不需要回填。package_records 加列+删 `is_incomplete` 走新表迁移。v1.0.0 用户升级后数据完好（is_incomplete 数据丢弃，UI 不再需要；sms_thread_id 回填 NULL，重扫后补齐）。

---

## 十、文件改动清单（预估）

### 新增文件

| 文件 | 用途 |
|------|------|
| `ui/component/glass/LiquidGlassNavigationBar.kt` | 悬浮底栏主体 |
| `ui/component/glass/Lens.kt` | 折射效果 |
| `ui/component/glass/Vibrancy.kt` | 鲜艳度 |
| `ui/component/glass/InnerShadow.kt` | 内阴影 |
| `ui/component/glass/CombinedBackdrop.kt` | Backdrop 组合 |
| `ui/component/glass/DampedDragAnimation.kt` | 阻尼拖拽 |
| `ui/component/glass/InteractiveHighlight.kt` | 高光交互 |
| `ui/component/JsonPreviewDialog.kt` | 规则 JSON 弹窗 |

### 修改文件

| 文件 | 改动要点 |
|------|---------|
| `build.gradle.kts` | AGP/Kotlin/Compose 版本升级（spike 已做） |
| `gradle-wrapper.properties` | Gradle 版本升级（spike 已做） |
| `app/build.gradle.kts` | compileSdk/targetSdk/BOM/miuix 依赖（spike 已做），加 miuix-blur |
| `MainActivity.kt` | MiuixTheme 包裹 + ThemeController 初始化 |
| `domain/model/MatchRule.kt` | 加 senderPattern / brandPrefix / brandSuffix |
| `domain/model/ExtractedFields.kt` | 加 brandName |
| `domain/engine/MatchEngine.kt` | 加发件方过滤 + 品牌提取 |
| `data/db/entity/MatchRuleEntity.kt` | 加三个新列 |
| `data/db/entity/PackageRecordEntity.kt` | 加 smsThreadId |
| `data/db/AppDatabase.kt` | version=2 + Migration v1→v2 |
| `data/local/SettingsStore.kt` | 加 scanWindowDays + colorMode + useMonet + paletteStyle |
| `data/preset/PresetRules.kt` | 加 senderPattern + 品牌锚点 + 可能加同品牌多规则 |
| `util/SmsReader.kt` | 加 address + threadId 字段 + readSince(cutoffMs) |
| `ui/theme/Theme.kt` | 重写：接 MiuixTheme，加动态/固定配色切换逻辑 |
| `ui/theme/Color.kt` | 调整：语义 token 保留，实现适配 miuix colors |
| `ui/theme/Type.kt` | 对齐 miuix typography token |
| `ui/theme/Shape.kt` | 对齐 miuix shape token |
| `ui/component/BottomNavBar.kt` | 重写为 FloatingBottomBar 调用点 |
| `ui/component/PackageCard.kt` | M3→miuix 组件替换；删「不完整」标签和提示；加跳短信入口 |
| `ui/component/DayGroup.kt` | M3→miuix；「当日已取」直执行不弹确认；stickyHeader |
| `ui/component/ConfirmDialog.kt` | M3 AlertDialog→miuix AlertDialog |
| `ui/component/FieldCard.kt` | M3→miuix |
| `ui/component/TokenChip.kt` | M3→miuix |
| `ui/component/EmptyState.kt` | M3→miuix |
| `ui/screen/PackageListScreen.kt` | M3→miuix；删顶部全局「全部已取」按钮 |
| `ui/screen/RuleListScreen.kt` | M3→miuix；↗ 换 `</>` 图标 + JSON 弹窗 |
| `ui/screen/SettingsScreen.kt` | M3→miuix；加主题设置区 + 扫描窗口选项 + 项目地址跳转；删「全部已取」 |
| `ui/screen/RuleEditorScreen.kt` | M3→miuix |
| `ui/screen/AiPromptScreen.kt` | M3→miuix |
| `ui/screen/OnboardingScreen.kt` | M3→miuix |
| `ui/navigation/AppNavigation.kt` | M3→miuix |
| `ui/viewmodel/PackageListViewModel.kt` | scanSms 传 cutoffMs + 存 threadId + 品牌名逻辑 |
| `ui/viewmodel/SettingsViewModel.kt` | 加主题偏好读写 + 扫描窗口读写 |

---

## 十一、执行顺序

1. **Spike 产出合入**（4 文件已改，commit）
2. **Room migration + 数据模型扩展**（新列、Migration、Entity/Model 同步）
3. **匹配引擎修复**（发件方过滤 + 品牌提取 + SmsReader 扩展）
4. **主题系统重写**（MiuixTheme + DataStore 偏好 + 豆绿色板适配）
5. **组件迁移**（6 屏 + 8 组件 M3→miuix 逐个替换）
6. **悬浮玻璃底栏**（移植 + 接入 + 降级分支）
7. **功能补齐**（扫描窗口 / JSON 弹窗 / 跳短信 / 列表页清理 / 设置页补齐）
8. **全局构建验证 + APK 体积检查**

每步完成后 `assembleDebug` 绿再进行下一步。

---

## 十二、Builder Check 决策记录

| # | 问题 | 决策 |
|---|------|------|
| 1 | 编辑器是否支持编辑 senderPattern / 品牌锚点 | **是**，编辑器加 2 个 FieldCard（发件方过滤 + 品牌提取），编辑项从 4→6，UI 重布局 |
| 2 | minSdk 30 vs miuix-blur minSdk 33 | **minSdk 提到 33**，与 miuix-blur 对齐，不再需要 API 30-32 降级分支 |
| 3 | "零 M3 引用"目标严格程度 | **非硬性**，miuix 无等效组件时（如 OutlinedTextField / PullToRefreshBox）保留少量 M3 import |
| 4 | 冷启动防闪烁 | **不加 SplashScreen**，接受一帧闪烁（DataStore 异步读取期间用默认配色） |
| 5 | 预置规则升级策略 | **升级旧预置规则**：v1.0.0 用户升级后自动补 senderPattern / brand 锚点到 isPreset 规则 |
| 6 | isIncomplete 处理 | **完全删除**：UI 显示 + PackageRecord.isIncomplete 属性 + scanSms 计算逻辑 + Room 列（migration DROP COLUMN） |
| 7 | configuredFieldCount 是否计入 senderPattern/brand | **不计入**：它们是过滤器不是提取器，优先级排序逻辑不变 |
| 8 | JsonExporter 导入导出 | **扩展**：新字段（senderPattern/brandPrefix/brandSuffix）加入 JSON 格式 |
| 9 | Room fallbackToDestructiveMigration | **不加**：migration 失败就崩，比静默丢数据安全 |
| 10 | BrandColors.forName 与品牌提取结果匹配 | 提取结果可能是"圆通快递"而非常量名"圆通"，`forName` 用 `contains` 模糊匹配，兼容 |
| 11 | 暗色豆绿色板 | 需定义 DarkColorScheme：各 token 在暗色下的色值（深背景+豆绿强调），写计划时产出 |
| 12 | DayGroup stickyHeader | Compose LazyColumn `stickyHeader` 是 foundation API，miuix Scaffold 下实测验证 |
| 13 | 玻璃底栏重力高光降级 | 无传感器设备降级为静态高光，不需要额外权限 |
| 14 | 预置规则 senderPattern 精确值 | 需采集各快递公司短信签名，写计划时调研 |
