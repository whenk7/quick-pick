# Preflight Scan — QuickPick 执行计划（2026-10-16 修订版验证）

> 扫描对象：`docs/superpowers/plans/quickpick/` 00-master + 9 分册（~4800 行，全量通读）。
> 口径：只报编译失败 / 运行崩溃 / 分册间矛盾级问题；风格与有意决策不报。
> 方法：纯文本比对 + 凭知识判断，未做 javap/反编译/源码下载（遵守禁令）。单项调查 ≤3 次工具调用。

## 总结

- **硬冲突：0**
- **存疑：4**（跨分册 1、内部 1、M3 兼容性 2）
- 前次评审修复的 9 处编译/运行阻断，本轮复核均已闭环（AI 回显链路、LazyColumn 无限高、MDC 主题、import、预置复活、去重换柜、权限前置判定等，见下表逐项确认）。

---

## 一、跨分册接口对

格式：`| 分册A ↔ 分册B | 接口/文件 | 发现 |`

### 链路 1：02 引擎/实体 → 03 仓库 → 05 ViewModel

| 分册A ↔ 分册B | 接口/文件 | 发现 |
|---|---|---|
| 02 ↔ 03 | MatchRuleDao 7 方法（observeAll/getAll/getById/getByName/insert/update/deleteById）→ RuleRepository | 一致 |
| 02 ↔ 03 | PackageRecordDao 8 方法（含 findByTrackingAndDate/markDayPickedUp，SQL 'localtime' 日界）→ PackageRepository | 一致 |
| 02 ↔ 03 | ScannedSmsDao 5 方法（exists/insert/count/deleteUnmatched/clearAll）→ SmsRepository | 一致 |
| 02 ↔ 03 | PackageRecordEntity 字段 ↔ toEntity()/copy(id,createdAt)（insertFromSms 换柜覆盖） | 一致（data class，字段全对齐） |
| 02 ↔ 03 | MatchRuleEntity ↔ RuleRepository.update 保留 isPreset/createdAt | 一致 |
| 02 ↔ 05a | MatchEngine.findBestMatch(body, rules): Pair<MatchRule,ExtractedFields>? → VM 解构 | 一致 |
| 02 ↔ 05a | isIncomplete = matchedCount < configuredFieldCount（master §3 口径，ExtractedFields 注释明示无该属性） | 一致（三处口径统一） |
| 02 ↔ 05a | PackageRecord 构造（VM 扫描入库，10 个具名参数） | 一致（未传字段均有默认值） |
| 02 ↔ 05b | MatchEngine.extract(smsText, rule) → EditorState.preview | 一致 |
| 02 ↔ 06 | QuickPickApp.database / settingsStore（02 Task2.6 定稿版）→ AppNavigation 首启判定 | 一致（master 地图"02 末定稿"口径吻合） |
| 02 内部 | PresetRules.seedIfNeeded(store, dao) ↔ QuickPickApp.onCreate 调用 | 一致 |
| 03 ↔ 05a | PackageRepository.insertFromSms/setPickedUp/markAllPickedUp/markDayPickedUp/observeAll/observeUnpickedCount | 一致 |
| 03 ↔ 05a | SmsReader.readAll + SmsMessage(id:String,body,timestamp)；SecurityException 上抛约定 ↔ VM catch 置 hasPermission=false | 一致（03 注释与 05a 实现互为镜像） |
| 03 ↔ 05a | **refresh()=clearScanned+全量重扫 ↔ insertFromSms 仅按 tracking+date 去重** | **存疑**：无单号字段的规则（trackingNumber 提取失败或未配置）命中短信，每次下拉刷新会重复插入新行（insertFromSms 对 tracking 为空的记录直接 insert）。非崩溃非编译错，属运行期数据重复；ON_RESUME 增量扫描不受影响（有 scanned_sms 守门）。建议 insertFromSms 增加按 smsId 查重兜底 |
| 03 ↔ 05b | RuleRepository.getById/insert/update + SmsRepository.deleteUnmatched（rescanUnmatched） | 一致 |
| 03 ↔ 05b | JsonExporter.sanitizeAiJson+import → loadFromJson（getOrNull().firstOrNull()） | 一致 |
| 03 ↔ 05c | JsonExporter.exportAll/import/sanitizeAiJson → SettingsViewModel/AiPromptViewModel.validatedJson | 一致 |
| 03 ↔ 05c | RuleRepository.getByName → ImportConflict(incoming, existingId)；resolveConflict 走 update（保 isPreset） | 一致 |
| 03 ↔ 06 | JsonExporter.exportSingle(rule: MatchRule) → RuleListScreen.onExportRule 单条分享 | 一致 |

### 链路 2：04 组件签名 → 05/06 调用

| 分册A ↔ 分册B | 接口/文件 | 发现 |
|---|---|---|
| 04 ↔ 05a | DayGroup(dateLabel,dateMs,packages,collapsed,onToggleCollapse,onTogglePickedUp,onCopyField,onMarkDayPickedUp) | 一致（具名参数逐一对上，类型 (Long)/(PackageRecord)/(String) 吻合） |
| 04 ↔ 05a/05b | PackageCard(record,onTogglePickedUp,onCopyField,modifier) | 一致（04 内 DayGroup 调用与 05b 预览调用均匹配） |
| 04 ↔ 05a | EmptyState(emoji,title,description,modifier,action) → 5 种空状态 | 一致（LazyColumn item 内调用不传 weight，合法） |
| 04 ↔ 05a/05b/05c | ConfirmDialog(title,message,confirmText,dismissText,dangerConfirm,onConfirm,onDismiss) → 5 处调用 | 一致（默认参数覆盖全部调用形态） |
| 04 ↔ 05a | formatDayLabel/formatRelativeTime 顶层函数（DayGroup.kt，public）→ 05a 经 `import com.quickpick.ui.component.*` | 一致 |
| 04 ↔ 06 | BottomNavBar(currentTab,onTabClick) + BottomTab enum（PACKAGES/RULES/SETTINGS，entries 遍历） | 一致 |
| 04 ↔ 05b | PackageCard 复用为编辑器实时预览（04 注释指向 Task 5.5，05b 确以 PackageRecord 构造预览） | 一致（引用编号正确） |
| 05b 内部 | TokenChip(text,state:TokenState,onClick)/FieldCard(fieldName,dotColor,state:FieldCardState,prefix,suffix,onPrefixChange,onSuffixChange,onClick) → RuleEditorScreen | 一致（枚举同包导出，screen 侧通配 import 覆盖） |

### 链路 3：01 主题/资源 → 04/05/06 引用

| 分册A ↔ 分册B | 接口/文件 | 发现 |
|---|---|---|
| 01 ↔ 04/05abc/06 | Color.kt 全部 14 色值 + BrandColors.forName/SF/ZTO/EMS | 一致（逐名核对，所有引用均存在；Pending 定义未用，非冲突） |
| 01 ↔ 各 UI 分册 | `Surface`/`Background`/`Divider` 与 M3 同名 composable 函数并存（双通配 import） | 一致（全部为值位置引用，Kotlin 解析到 theme val，无歧义；06/MainActivity 用显式 import 规避） |
| 01 ↔ 04/05c | AppTypography 8 槽位；EmptyState 用 displayMedium、Onboarding 用 displayLarge | 一致（未定义槽位回落 M3 默认 Typography，合法） |
| 01 ↔ 05c | buildConfig=true + namespace=com.quickpick → `import com.quickpick.BuildConfig`（VERSION_NAME） | 一致 |
| 01 内部 | themes.xml @color/bg_base、manifest @style/Theme.QuickPick、@mipmap/ic_launcher → colors.xml/ic_launcher.xml/foreground | 一致（minSdk 30 无需 anydpi-v26 以外 fallback） |

### 链路 4：05 页面 ↔ 06 导航

| 分册A ↔ 分册B | 接口/文件 | 发现 |
|---|---|---|
| 05a ↔ 06 | PackageListScreen(viewModel,onGoToRules) / RuleListScreen(viewModel,onAddRule,onEditRule:(Long),onExportRule:(MatchRule)) | 一致 |
| 05b ↔ 06 | RuleEditorScreen(viewModel,onBack)；loadRule(id)/savedEvents(wasEdit) 语义 | 一致 |
| 05c ↔ 06 | OnboardingScreen(onPermissionGranted) / SettingsScreen(settingsViewModel,packageViewModel,onOpenAiPrompt) / AiPromptScreen(viewModel,onImportRule:(String,String),onBack) | 一致（packageViewModel 传共享实例，跨页"全部已取"生效） |
| 05c ↔ 06 | **AI 回显链路**：onImportRule → navigate("rule_editor/0") → currentBackStackEntry.savedStateHandle.set("importedJson"/"importedSms") → 编辑器 LaunchedEffect 读取 → loadFromJson(json,sms) → remove 键 | 一致（键名三处吻合；navigate 同步建栈、写在组合前，时序成立；上次评审的"断链"已修复） |
| 05abc ↔ 06 | 5 个 AndroidViewModel(application) ← navigation-compose `viewModel()` 默认工厂 | 一致（NavBackStackEntry 的 SavedStateViewModelFactory 支持 Application 注入） |
| 02 ↔ 06 | Screen 路由 "rule_editor/{ruleId}" + NavType.LongType default 0 ↔ createRoute(Long) | 一致 |

---

## 二、分册内部一致性（import 路径 vs 文件地图；创建 vs 引用）

| 分册A ↔ 分册B | 接口/文件 | 发现 |
|---|---|---|
| 01 内部 | settings.gradle.kts `dependencyResolutionManagement` DSL、packaging.resources.excludes、jvmToolchain(17)、wrapper 8.14.3 ≥ AGP 8.13 要求 | 一致（上次评审修复项复核通过） |
| 01 ↔ 05a/05c | ContextCompat 使用但依赖清单未显式声明 androidx.core | 一致（activity-compose → activity → core 为 compile 依赖，传递可见；标准做法） |
| 02 内部 | AppDatabase import 3 DAO + 3 实体，均本册前序 Task 创建 | 一致 |
| 03 内部 | 3 仓库 + 2 工具的 import（dao/entity/domain/gson/Telephony）全部指向 01/02 已建文件或已声明依赖 | 一致 |
| 04 内部 | PackageCard 调 formatRelativeTime（同包 DayGroup.kt，无需 import）；IntrinsicSize/fillMaxHeight 经 layout.* 覆盖 | 一致 |
| 05b 内部 | FieldCard.kt 花括号层级（Column 嵌 Row 内、缩进异常） | 一致（逐层数过：if/Column/Row/Card/fun 五层闭合正确，仅缩进误导，不报风格） |
| 05c 内部 | SettingsScreen import PackageListViewModel（05a 产物，同轮 R4） | 一致（R4 出口=三分册全建完再编译） |
| 06 内部 | 全部 import（navigation.compose.*/viewmodel.*/screen.*/component.*/util.JsonExporter/QuickPickApp）vs 各分册产物路径 | 一致；`when(val done=…) else` 分支 Boolean 智能转换合法 |
| 07 ↔ 02/03/05a | **文案 grep 门禁** `grep "错误\|失败\|必须"` 要求命中 0 次 | **存疑（轻）**：会命中 3 处代码注释——02 PackageRecordDao"注意必须带 'localtime'"、03 JsonExporter"不剥离会直接解析失败"、05a PackageListScreen"LazyColumn 必须在 weight(1f) 容器内"。门禁按字面永不通过；需注明"仅查 UI 字符串、豁免注释"或微调 grep。不影响编译运行 |
| 00 ↔ 01 | 版本矩阵（Kotlin 2.3.20/AGP 8.13.0/KSP 2.3.12/BOM 2026.09.00/Room 2.8.4/Nav 2.9.5/Lifecycle 2.9.4/DataStore 1.1.7/Gson 2.13.1/SDK 36/minSdk 30/Gradle 8.14.3/JDK 17） | 一致（两处逐项吻合，含 KSP2 独立版本号说明） |
| 00 ↔ 各分册 | 轮次归属（R1=01+02，R2=03，R3=04，R4=05abc，R5=06+07） | 一致（各分册头部"所属轮次"逐一吻合） |

---

## 三、文件地图核对（00-master §4 ↔ 各分册实际清单）

| 分册A ↔ 分册B | 接口/文件 | 发现 |
|---|---|---|
| 00§4 ↔ 01-06 | 43 个 Kotlin 源文件 | 一致（逐条点名核对：2 入口 + 1 db + 3 entity + 3 dao + 1 local + 1 preset + 3 repository + 3 model + 1 engine + 4 theme + 7 component + 6 screen + 5 viewmodel + 1 navigation + 2 util = 43；无多、无少、无错位） |
| 00§4 ↔ 01 | 5 个资源/Manifest（manifest/themes/colors/ic_launcher.xml/ic_launcher_foreground.xml） | 一致（均 Task 1.1 创建） |
| 00§4 ↔ 01 | gradle 四件套（根/app build.gradle.kts、settings、gradle.properties）不在 43+5 计数内 | 一致（地图口径为 app/src/main，自洽；wrapper 二进制明示不写入计划，Step 8 给出两种生成方式） |
| 00§4 ↔ 05b/06 | QuickPickApp.kt 三阶段演进标注（01 最小版→02 接库→02 末定稿）、MainActivity（01 占位→06 定稿） | 一致（01/02/06 正文与标注完全对应，均有"整文件替换"指令） |

---

## 四、M3 1.4 兼容性（凭知识浅查，未考古；编译门禁兜底）

> 判断基准：我的知识覆盖到 material3 ~1.3/1.4-alpha、foundation ~1.9。计划声称 BOM 2026.09.00 = Compose 1.12 / M3 1.5，**超出知识截止**，故凡依赖"1.5 已稳定"的项一律标存疑。1.4→1.5 之间的改名清单无法凭知识枚举，不做臆测。

### 4.1 API 清单与标注

| 分册A ↔ 分册B | 接口/文件 | 发现 |
|---|---|---|
| 04 ↔ M3 | Text/Card/CardDefaults.cardColors/cardElevation/TextButton/IconButton/MaterialTheme | 一致（≤1.2 全稳定，1.4/1.5 安全） |
| 04 ↔ M3 | AlertDialog(shape,containerColor,tonalElevation 重载) | 一致（1.1 起稳定） |
| 04 ↔ M3 | NavigationBar/NavigationBarItem/NavigationBarItemDefaults.colors(indicatorColor) | 一致（1.0/1.1 起稳定） |
| 05a ↔ M3 | Scaffold(contentWindowInsets)/Button/ButtonDefaults.buttonColors/OutlinedButton | 一致（稳定 API；WindowInsets(0) 单参构造存在） |
| 05a ↔ M3 | ButtonDefaults.outlinedButtonBorder.copy(brush=…) | 一致（BorderStroke 有 copy(width,brush)；1.4 expressive 改版未移除此属性——凭知识判断，低风险） |
| **05a ↔ M3** | **PullToRefreshBox（material3.pulltorefresh，未加 @OptIn）** | **存疑**：1.3.0 引入即为 @ExperimentalMaterial3Api，截至我知识（1.4.x）未转正；计划隐含"M3 1.5 已稳定"。若实际解析到 ≤1.4 或 1.5 仍实验 → **编译失败**（experimental 未 opt-in 是 error 级）。参数形态 (isRefreshing,onRefresh,modifier){content} 与 1.3/1.4 签名兼容。保险做法：无条件加 @OptIn(ExperimentalMaterial3Api::class)，已稳定时也无副作用 |
| **05b ↔ foundation** | **FlowRow（未加 @OptIn，计划注释称"foundation 1.7+ 已稳定"）** | **存疑**：凭我知识 FlowRow/FlowColumn 至 foundation ~1.9 仍是 @ExperimentalLayoutApi，"1.7+ 已稳定"的说法与我的知识矛盾；可能在 1.10-1.12（知识截止后）转正。若未转正 → **编译失败**。保险做法：加 @OptIn(ExperimentalLayoutApi::class) |
| 05b ↔ M3 | Scaffold(topBar,bottomBar)/OutlinedTextField(minLines,shape,textStyle,singleLine)/Card | 一致（minLines 为 M3 1.1+ 稳定参数） |
| 05c ↔ M3 | HorizontalDivider(color=…) | 一致（1.2.0 引入并取代 Divider；1.4/1.5 安全） |
| 05c ↔ M3 | Scaffold/OutlinedTextField/Button/TextButton/Card/displayLarge | 一致 |
| 06 ↔ M3 | Scaffold(bottomBar)/Surface（显式 import） | 一致 |
| 01 ↔ M3（顺带） | lightColorScheme 的 surfaceContainer{Lowest,Low,,High,Highest} 5 槽位 | 一致（1.2.0 引入；1.4/1.5 安全。若 BOM 回退到 M3 ≤1.1 才会编译失败——计划版本矩阵下不成立） |
| 01 ↔ M3（顺带） | Shapes(extraLarge)、Typography 具名槽位 | 一致（1.1+/1.0 起稳定） |
| 04/05/06 ↔ M3 | 未使用 TopAppBar/SearchBar/ModalBottomSheet 等长期实验区 API（顶栏为自定义 Row + statusBarsPadding） | 一致（规避了主要实验 API 风险面） |
| 01/06 ↔ activity | enableEdgeToEdge（activity 1.8+）、rememberLauncherForActivityResult、LocalLifecycleOwner（androidx.lifecycle.compose，lifecycle 2.8+） | 一致（声明版本 1.11.0/2.9.4 覆盖） |

### 4.2 结论

- 凭知识可确认 1.4 安全的：上表除两条存疑外全部。
- **唯二可能编译阻断**：PullToRefreshBox、FlowRow 的 experimental opt-in，均取决于实际解析版本是否如计划声称（M3 1.5 / foundation 1.12 转正）。两处的 @OptIn 保险成本为零，建议执行 R4 时直接加上，编译门禁自然兜底。
- 1.5 新引入/1.4→1.5 改名的 API：计划未使用任何我能识别的"1.5 独有"API（无 ButtonGroup/FloatingToolbar/MarqueeText 等 1.4 expressive 新件，也无更激进的新 API），故"用了 1.5 才有"的风险面本身很小。

---

## 五、存疑清单汇总（4 条，0 硬冲突）

1. **[05a×03·运行数据]** 下拉全量重扫对无单号记录不去重 → 每次刷新重复入库（建议 insertFromSms 按 smsId 兜底查重）
2. **[07×02/03/05a·验收门禁]** 文案 grep 会命中 3 处代码注释中的"必须/失败"，门禁按字面永不通过（需注明豁免注释）
3. **[05a×M3·编译]** PullToRefreshBox 未加 @OptIn；若解析到 M3 ≤1.4（实验期）则编译失败，建议无条件加 ExperimentalMaterial3Api opt-in
4. **[05b×foundation·编译]** FlowRow 未加 @OptIn；"1.7+ 已稳定"与扫描 agent 知识（≤1.9 仍实验）矛盾，建议无条件加 ExperimentalLayoutApi opt-in
