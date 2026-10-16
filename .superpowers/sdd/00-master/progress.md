# SDD ledger — plan: docs/superpowers/plans/quickpick/00-master.md

## 恢复状态（2026-10-16 会话重启后接手）

- BASE commit: b0a2019（执行前 HEAD）
- 环境已就绪：JDK 17 (/home/wen/dev/jdk/jdk-17.0.20.1+1)、Gradle 8.14.3 (PATH)、Android SDK 36 (/home/wen/Android/Sdk)、local.properties 已配
- R1（01+02 分册）23 个文件已存在于磁盘但未提交、从未编译 —— R1 是**续跑**不是新派
- 无 gradle wrapper

## 裁决记录（Ruling）

- Ruling: 不用 worktree，直接在 main 上提交 —— CLAUDE.md「v1.0.0 发布前直接提交 main」明确授权 —— 若错：历史线性，单人项目无成本
- Ruling: R1 出口标准降为「assembleDebug 绿」，运行验证（App 起、预置规则入库）并入 R5 —— 本机 headless 无模拟器 —— 若错：R1 入库的运行时缺陷延迟到 R5 才发现，成本可控（数据层代码是照抄分册的）
- Ruling: 主会话不干杂活（不建文件、不修代码），只协调/评审/裁决 —— 用户 2026-10-16 明确指示，覆盖 CLAUDE.md「R1 强模型直接跑」— 若错：Gradle 排障由 sonnet 档 agent 执行，卡住时升级 opus
- Ruling: 执行 agent 用 sonnet 档 —— CLAUDE.md 钦定；分册含完整代码，本质是转录+修编译错误 —— 若错：转录错误率略升，由每轮评审兜底
- Ruling: Compose BOM 2026.09.00 → 2026.06.01（Compose 1.11.4 + M3 1.4.0）—— BOM 2026.09 硬要求 minCompileSdk 37 + AGP≥9.1，与计划矩阵（compileSdk 36/AGP 8.13）不可能并存；本机仅装 SDK 36；降级是最小修复 —— 若错：后续分册若用 M3 1.5 独有 API 需再升级整套工具链（AGP 9.1+Gradle 9.x+SDK 37），已由扫描 agent 交叉核对，见 preflight-scan.md「M3 1.4 兼容性」节

## 预检扫描

计划 2026-10-16 已经四路并行评审修订（修 9 处编译/运行阻断 + 补 10 项缺失）。
第一次扫描 agent 在 M3 版本考古上钻牛角尖（javap NavController）44 分钟无产出，已杀。
Ruling: 扫描重派，M3 兼容性改为浅查（凭知识标注，禁止反编译），API 存在性由编译门禁兜底 —— 版本考古成本高且不收敛 —— 若错：漏标的 M3 1.5 API 在 R4/R5 编译时暴露，修复成本一轮修复循环
第二次扫描已派出，结果见 preflight-scan.md，回来补表。

## 预检扫描结果（v2，已归档 preflight-scan.md）

0 硬冲突（~35 对接口逐一吻合，文件地图 43+5 无错位），4 存疑处置：
- #1 无单号短信重扫重复入库：与 R4a 评审 Minor 收敛（两 agent 独立发现）→ **升终审必修**
- #2 07 验收文案 grep 会误命中代码注释（02/03/05a 三处"必须/失败"在注释里）→ R5 执行验收时豁免注释行，不得机械按字面判不过
- #3 PullToRefreshBox @OptIn：被 R4a 编译绿经验性关闭（M3 1.4 已稳定）
- #4 FlowRow @OptIn：被 R4b 编译绿经验性关闭
- Ruling: 扫描发现的 #3/#4 不加防御性 @OptIn —— 编译证据已证明 API 稳定，加 OptIn 是死代码 —— 若错：零成本（编译器会立即报）

## 任务进度

- Task R1: minor (deferred): r1-report.md:37「零改动」声明夸大——4 个 XML 省略分册路径标记注释是技术必需（XML prolog 禁注释），但报告未列为偏差
- Task R1: minor (deferred): 分册给 9 条 per-Task commit 指令，实际单 commit 98aebd8——代码不受影响
- Ruling: 后续轮次一律一轮一个 commit，豁免分册 per-Task commit 指令 —— CLAUDE.md「v1.0.0 前无仪式」+ 分册 commit 粒度只为教学演示 —— 若错：回溯粒度变粗，单人项目可接受
- Task R1: complete (commits b0a2019..98aebd8, review clean)——24 文件逐字转录双重验证通过，唯一偏差 BOM 2026.06.01 等于裁决值
- Task R2: complete (commits 98aebd8..d1727c8, review clean)——5 文件 byte-exact，3 条实现者备注核实为分册原文语义，零发现
- Task R3: minor (deferred): DayGroup.kt:37-40 注释「200ms ease-out」与实现（默认 spring）不符——plan-mandated，分册原文如此；终审时统一决定补 animationSpec 或改注释
- Task R3: complete (commits d1727c8..d4f8b8b, review clean)——5 文件 481 行逐字一致，28sp bold 约束经 Type.kt 核查成立
- Task R4a: minor (deferred): RuleListScreen.kt:132 outlinedButtonBorder 无参版 M3 1.4.0 弃用（plan-mandated，分册原文）——终审统一改 outlinedButtonBorder(enabled = true)
- Task R4a: minor (deferred): PackageListViewModel.kt refresh() 与在途 scanSms 竞争：无单号短信可重复入库（触发条件苛刻，预置规则均含单号）——终审可选修 refresh 开头加 isScanning 守卫
- Ruling: RuleListScreen 两处 item{} 多组件未包 Column（分册 L574-582/L617-630 原文如此，与分册 L533 自身注释矛盾，运行时叠放/底部避让不足）——修复并入 R5（06 接导航时），同时回写 05a 分册消除矛盾 —— 若错：底部按钮贴导航栏的视觉缺陷存活到 R5，届时必修
- Task R4a: complete (commits d4f8b8b..80e0059, review clean + 1 plan-mandated Important 已裁决)——666 行逐字一致，实现者 3 条备注全部核实属实
- Ruling（流程）: R4c 跳过独立评审（条件：零偏差+构建绿），R4b/R5 保留评审，终审 opus 顶格 —— 用户质疑评审性价比，数据显示三轮评审零非平凡发现 —— 若错：R4c 转录缺陷漏网至终审才发现，成本可控（终审 diff 全覆盖）
- Task R4b: minor (deferred): RuleEditorScreen.kt:293 注释误写"BOM 2026.09"（plan-mandated）——终审统一改
- Task R4b: minor (deferred): setTokenAsAnchor indexOf 锚定首次出现 + 选中判定字符串相等 → 重复词块全部高亮（plan-mandated）——R5 联调用含重复词短信验证体验
- Task R4b: minor (deferred): TokenChip 圆角 8dp vs 全局 ≥12dp（plan-mandated，疑似小尺寸有意例外）——终审裁定
- Task R4b: minor (deferred): FieldCard.kt:58 缩进不齐（分册原文瑕疵）——终审顺手格式化
- Task R4b: complete (commits 80e0059..9e6cb13, review clean + 4 plan-mandated Minor 延期)——4/4 文件逐字节一致经脚本实证，预览区真复用 PackageCard 经 grep 证实
- Task R4c: minor (deferred): processNext() 递归 suspend 大批量导入栈风险（plan-mandated）——终审裁定
- Task R4c: minor (deferred): _events.tryEmit 极端下丢一条 Toast（plan-mandated）——终审裁定
- Task R4c: minor (deferred): validatedJson() 主线程 Gson（体量小无碍，plan-mandated）——终审裁定
- Task R4c: complete (commit c8a6de9, 零偏差+构建绿，按裁决跳过独立评审，风险并入终审)——5 文件 672 行，实现者自报 3 条分册原生瑕疵均已如实记录
