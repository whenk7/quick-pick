# 取件宝 (QuickPick)

## 项目定位

Android 快递取件信息管理应用。从短信自动提取取件码/柜号/地址，不追踪物流，不上传数据。

## 设计方向：治愈系极简

**核心：** 低饱和豆绿 `#6AB37B` 为主色，暖米白 `#F9F9F6` 为底色，深灰文字不用纯黑。所有元素大圆角(≥12dp)，动效柔和不弹跳，文案温和友不用"错误""失败"。

完整设计系统见 `docs/superpowers/design/design-system.md`。

## 技术栈

Kotlin + Jetpack Compose + Material 3 + Room
min API 30，架构 MVVM
匹配引擎：纯字符串前后缀锚点，不用正则
Compose BOM 实际 2026.06.01（计划写 2026.09，因其硬要求 compileSdk 37 + AGP 9.1，2026-10-16 裁决降级）

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
│   ├── design/
│   │   ├── design-system.md           ← 治愈系极简设计系统
│   │   ├── theme.css                  ← 完整 CSS 令牌
│   │   ├── quickpick.html + mockup-*.html  ← 页面 mockup 参考
│   └── plans/quickpick/               ← 最终执行计划：00-master.md 总纲 + 9 个 Phase 分册
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

- ✅ 产品规格
- ✅ 设计系统
- ✅ 实现计划（总纲+9分册）
- ✅ 代码实现 R1–R5 全部完成（2026-10-16，SDD 流程：逐轮实现+评审+opus 终审+修复波；档案在 `.superpowers/sdd/00-master/`，含 ledger/各轮报告/评审包）
- ⬜ 真机验收（16 项运行时清单：`.superpowers/sdd/00-master/r5-report.md` §5.4，重点：首扫、换柜覆盖、AI 导入回显、权限回收 banner、编辑保存→重扫弹窗）
- ⬜ 真机过后打 tag `v1.0.0` + GitHub Release 发 APK

## 执行模式（已和用户商定）

- **主会话（强模型）当总控**：派活、审 diff、排障、把每轮出口标准
- **执行 agent 用 sonnet 档**（档级映射随模型配置调整）：按分册建文件，照抄计划代码，不自由发挥
- **评审分层**：转录轮（实现者报零偏差+构建绿）可跳独立评审，复杂轮/集成轮保留，终审用顶格模型——2026-10-16 实测：终审抓到 2 个 Critical 跨层缺陷，轮级评审的价值在确认不在发现
- **构建环境已装好**：JDK 17（/home/wen/dev/jdk）+ Gradle 8.14.3 + SDK 36（/home/wen/Android/Sdk），local.properties 已配，`./gradlew` 可用

## 下一步

1. 真机装 `app/build/outputs/apk/debug/app-debug.apk`，过 16 项运行时验收（清单见 `.superpowers/sdd/00-master/r5-report.md` §5.4）
2. 验收过 → 打 tag `v1.0.0` → GitHub Release 发 APK（assembleRelease 前先确认 `./gradlew lintVitalRelease` 绿）
3. v1.0.1 候选（终审判定可延期项）：outlinedButtonBorder deprecation、rememberSaveable tab、死代码清理（SettingsViewModel.rules、Color.Pending）、「全部已取」权限门控放开、编辑器重复词块锚定增强
