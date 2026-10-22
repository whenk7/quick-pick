# 取件宝 (QuickPick)

## 项目定位

Android 快递取件信息管理应用。从短信自动提取取件码/柜号/地址，不追踪物流，不上传数据。

## 设计方向：治愈系极简

**核心：** 低饱和豆绿 `#6AB37B` 为主色，暖米白 `#F9F9F6` 为底色，深灰文字不用纯黑。所有元素大圆角(≥12dp)，动效柔和不弹跳，文案温和友不用"错误""失败"。

完整设计系统见 `docs/superpowers/design/design-system.md`。

## 技术栈

Kotlin + Jetpack Compose + miuix KMP + Room
min API 33（与 miuix-blur 对齐），架构 MVVM
UI：v1.1.0 起 M3 全面迁移到 miuix 0.9.4（ui+blur），app/src 零 material3 引用，material3 gradle 依赖已移除
匹配引擎：纯字符串前后缀锚点，不用正则；v1.1.0 加发件方过滤（senderPattern 子串，空则不过滤向后兼容）+ 品牌提取（【】锚点，BrandColors.forName contains 模糊匹配）
版本矩阵：Kotlin 2.4.21 / Compose BOM 2026.09.00 / AGP 9.4.1 / Gradle wrapper 9.8.1 / compileSdk+targetSdk 37（旧「BOM 降级 2026.06」裁决已作废）

## 我的偏好

- 全程中文对话与文档
- Caveman 模式：不要废话，不要 filler，不要讨好语气
- 写 HTML mockup 时不要浪费 token 在无意义文件上（如 waiting.html）
- UI 侧重于审美方向，功能细节在 spec 里
- 不要 README（我上班了自己写）

## 关键文件地图

```
取件宝/
├── CLAUDE.md                          ← 本文件。换电脑后先读这个
├── .res/                              ← 设计参考资料（非代码）
│   ├── 治愈系极简设计落地方案.md        ← 我自己写的审美方向，权威
│   ├── 主流快递公司品牌色调色系大全.md   ← 10 家快递品牌色 + Compose 枚举
│   ├── preset-rules.json              ← 2 条预置规则（顺丰、京东）
│   ├── ai-prompt-template.md          ← AI 生成规则的提示词模板
│   ├── edge-cases.md                  ← 7 类边界情况处理方案
│   └── data-model.md                  ← Room 3 表结构
├── docs/superpowers/
│   ├── specs/2026-05-30-quickpick-design.md   ← 产品规格书（权威）
│   ├── specs/2026-10-21-miuix-migration-design.md ← miuix 迁移设计（v1.1.0 权威）
│   ├── design/
│   │   ├── design-system.md           ← 治愈系极简设计系统
│   │   ├── theme.css                  ← 完整 CSS 令牌
│   │   ├── quickpick.html + mockup-*.html  ← 页面 mockup 参考
│   ├── plans/quickpick/               ← v1.0.0 执行计划：00-master.md 总纲 + 9 个 Phase 分册
│   └── plans/2026-10-21-miuix-migration.md  ← v1.1.0 迁移计划（10 task 全完成）
└── app/                               ← 还没创建，计划执行时建
```

## 审美决策记录

1. 我拒绝了 Frost（冷白+靛蓝）和 Graphite（暖灰+青绿），因为与快递场景不搭
2. 我自己写了「治愈系极简」方向：豆绿色 = 快递柜绿，用户天然心智关联
3. 取件码 28sp bold，因为现在手机都 6 寸以上
4. 单强调色原则：全页只有豆绿一个强调色，快递公司品牌色缩到 8dp 小圆点
5. 颜色不东拼西凑，有冷感但不冰冷，有温度但不甜腻
6. 图标用 emoji + 品牌色 8dp 圆点，弃用 Iconfont 方案（版权零风险、实现简单，底部导航 📦/🧩/⚙️）

## 关键功能决策

- 仅提取取件短信（不是物流追踪），定位"取件宝"
- AI 只用模式 B（复制提示词），不做模式 A（内置 API 调用）
- 规则编辑器：分词区吸顶 + 每个字段独立卡片 + 选中抬高阴影 + 底部卡片预览
- 匹配优先级：字段数多者优先，同样多时按规则顺序
- 已取件不删除，置灰保留
- "全部已取"操作需二次确认弹窗
- 合规声明在权限引导页和设置页两处展示
- 预置规则先跑 2 家（顺丰/京东），后期再决定是否扩充
- 分发渠道：GitHub Release 侧载；不上 Google Play（READ_SMS 受限权限政策不覆盖本品类）；仅 Android，不支持鸿蒙/iOS
- 仅处理普通文本短信
- v1.1.0 新增：扫描窗口四档（调小不回收已入库记录）；主题明/暗/Monet 三态；长按卡片分享原文；JSON 预览弹窗 + 跳原短信会话（threadId）；悬浮玻璃底栏（blur+lens+highlight）；删设置页「全部已取」入口（列表页操作保留）

## 分支与发布

单人项目，最简模型：**main 永远可发布，分支按"事情"开，版本用 tag 打**。

- **main**：主干，文档+代码都在，永远保持可构建状态
- **开发分支**按事情命名，干完合回 main 就删：
  `feat/xxx`（新功能）`fix/xxx`（修 bug）`refactor/xxx`（重构）`docs/xxx`（文档）
- **版本不是分支，是 tag**：`v1.0.0`、`v1.1.0` 打在 main 上，GitHub Release 从 tag 发 APK；回溯用 `git checkout v1.0.0`
- **版本号**：`app/build.gradle.kts` 的 `versionName` 与 tag 一致，`versionCode` 每次发布 +1
- **节奏**：v1.0.0 发布前直接提交 main（无用户，无仪式）；发布后新东西走分支，攒够一波打新 tag
- **不要** develop/release 分支——那是多人团队的 GitFlow，单人项目只剩负担
- commit 信息沿用 `feat:/fix:/docs:/chore:` 前缀
- **CI 发版**：推 `v*` tag 触发 `.github/workflows/release.yml` → 签名 APK 自动挂 Release。keystore 在 `~/quickpick-release.jks`（**单密码**，PKCS12 不支持库/key 双密码，keypass 会被静默忽略），4 个 GitHub secrets 已配。2026-10-16 首跑踩坑记录：setup-android action 卡协议（用 runner 预装 SDK）、sdkmanager 不在 PATH（走 `$ANDROID_HOME/cmdline-tools` 全路径）、GITHUB_TOKEN 默认只读（workflow 需 `permissions: contents: write`）

## 交流风格

用户（wenk）偏好：直接、废话少、不讨好。技术问题给精确答案。不喜欢"Sure! I'd be happy to..."这类 filler。用中文。

## 当前进度

- ✅ v1.0.0 已发布（tag `v1.0.0`，GitHub Release 发 APK，2026-10-16；R1–R5 档案在 `.superpowers/sdd/00-master/`）
- ✅ v1.1.0 miuix 迁移 + 引擎修复 + 功能补齐：Task 1–10 全部完成（2026-10-22，分支 `spike/miuix-align`，每 task 独立评审 clean，ledger 在 `.superpowers/sdd/2026-10-21-miuix-migration/`）
- ✅ 全局验证：assembleRelease + lintVitalRelease 绿，APK 3.07MB 未签名（<5MB 目标），M3 引用清零
- ⬜ miuix 新 UI 真机验收（明/暗/Monet 三态；**验收前先清应用数据**——旧 debug 机预置规则品牌锚点 seeded 是错值）
- ⬜ 合 main → 打 tag `v1.1.0` → CI 发 APK

## 执行模式（已和用户商定）

- **主会话（强模型）当总控**：派活、审 diff、排障、把每轮出口标准
- **执行 agent 用 sonnet 档**（档级映射随模型配置调整）：按分册建文件，照抄计划代码，不自由发挥
- **评审分层**：转录轮（实现者报零偏差+构建绿）可跳独立评审，复杂轮/集成轮保留，终审用顶格模型——2026-10-16 实测：终审抓到 2 个 Critical 跨层缺陷，轮级评审的价值在确认不在发现
- **构建环境已装好**：JDK 17.0.20（/home/wen/dev/jdk）+ Gradle wrapper 9.8.1 + SDK 37（/home/wen/Android/Sdk），local.properties 已配，`./gradlew` 可用；本地无 KEYSTORE_FILE 环境变量时 assembleRelease 产未签名包（正常，签名包由 CI 打）

## 下一步

1. 真机装 `app/build/outputs/apk/debug/app-debug.apk`（先清应用数据），验收重点：明/暗/Monet 三态主题、玻璃底栏（三键导航不遮挡）、发件方过滤、品牌色点、扫描窗口、JSON 弹窗、跳原短信、长按分享、DayGroup 吸顶、弹窗退出动画
2. 验收过 → 合 main → 打 tag `v1.1.0` → CI 发 APK；Release 附 THIRD-PARTY-NOTICES（miuix KMP + Kyant0/AndroidLiquidGlass，均 Apache-2.0）
3. v1.1.1 候选（迁移延期 minors + v1.0.0 遗留）：BottomNavBar.kt 改名 BottomTab.kt、死代码清理（SmsReader.readAll、SettingsViewModel.rules、Color.Pending）、rememberSaveable tab、编辑器重复词块锚定增强、SCAN_WINDOWS/主题初值提常量、ScanWindowRow a11y 语义、lastJsonRule SideEffect 化、JsonPreviewDialog 加分享按钮、KSP 对齐 Kotlin 2.4.21、exportSchema 开启入库、`--warning-mode all` 定位 Gradle 10 deprecation 来源
