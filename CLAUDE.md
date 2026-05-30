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
│   ├── 阿里巴巴Iconfont 完整使用指南.md  ← 图标来源 + 版权注意
│   ├── preset-rules.json              ← 2 条预置规则（顺丰、京东）
│   ├── ai-prompt-template.md          ← AI 生成规则的提示词模板
│   ├── edge-cases.md                  ← 7 类边界情况处理方案
│   └── data-model.md                  ← Room 3 表结构
├── docs/superpowers/
│   ├── specs/2026-05-30-quickpick-design.md   ← 产品规格书（权威）
│   ├── design/
│   │   ├── design-system.md           ← 治愈系极简设计系统
│   │   ├── theme.css                  ← 完整 CSS 令牌
│   │   ├── aesthetic-v1.html          ← Frost vs Graphite 对比（已弃用，参考）
│   │   └── quickpick.html             ← 页面 mockup 参考
│   └── plans/2026-05-31-quickpick-implementation.md  ← 实现计划（下一步执行）
└── app/                               ← 还没创建，计划执行时建
```

## 审美决策记录

1. 我拒绝了 Frost（冷白+靛蓝）和 Graphite（暖灰+青绿），因为与快递场景不搭
2. 我自己写了「治愈系极简」方向：豆绿色 = 快递柜绿，用户天然心智关联
3. 取件码 28sp bold，因为现在手机都 6 寸以上
4. 单强调色原则：全页只有豆绿一个强调色，快递公司品牌色缩到 8dp 小圆点
5. 颜色不东拼西凑，有冷感但不冰冷，有温度但不甜腻

## 关键功能决策

- 仅提取取件短信（不是物流追踪），定位"取件宝"
- AI 只用模式 B（复制提示词），不做模式 A（内置 API 调用）
- 规则编辑器：分词区吸顶 + 每个字段独立卡片 + 选中抬高阴影 + 底部卡片预览
- 匹配优先级：字段数多者优先，同样多时按规则顺序
- 已取件不删除，置灰保留
- "全部已取"操作需二次确认弹窗
- 合规声明在权限引导页和设置页两处展示

## 交流风格

用户（wenk）偏好：直接、废话少、不讨好。技术问题给精确答案。不喜欢"Sure! I'd be happy to..."这类 filler。用中文。

## 当前进度

- ✅ 产品规格
- ✅ 设计系统
- ✅ 实现计划（含完整代码，约 40 个文件待创建）
- ⬜ 开始写代码（按 `docs/superpowers/plans/2026-05-31-quickpick-implementation.md`）

## 下一步

在新电脑上打开项目后：
1. 读 `docs/superpowers/plans/2026-05-31-quickpick-implementation.md`
2. 按 Phase 1 → Phase 7 顺序执行
3. 计划里每个 task 的代码已完整写出，直接复制创建文件即可
4. 推荐分 5 轮执行，每轮一个 Phase，轮间检查
