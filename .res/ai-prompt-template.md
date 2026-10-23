# AI 生成规则 — 提示词说明

> 2026-10-23 重写。运行时使用的最新模板在 `AiPromptViewModel.PROMPT_TEMPLATE`（代码为权威），
> 本文档只描述设计意图，不再复制全文以免双写漂移。

## 设计要点

- **批量样本**：用户短信样本（快递 15 + 通知 5 封顶 20）随提示词一起复制。
  通知样本是反面教材，提示词明确要求"不要为通知生成规则、且规则不得误提取通知"。
- **只带正文**：发送方/时间不进提示词（用户在页面逐条审查，可移除，可手动补齐）。
- **引擎语义写进提示词**：indexOf 首次出现、suffix 留空取到文末、suffix 配了没命中判 null（E1）、
  锚点宁长勿短防撞「已到站/到咸阳」式首次出现陷阱、brand 统一取【】签名。
- **完整 schema**：name + senderPattern + fields{trackingNumber/pickupCode/address/phone/brand}。
- 输出严格 JSON 数组，允许 markdown 围栏（导入方 JsonExporter.sanitizeAiJson 自动剥）。

## 导入侧校验（JsonExporter）

1. 合法 JSON（单对象或数组均可，智能识别）
2. 必含 name 和 fields；fields 内 key 在允许列表（trackingNumber/pickupCode/address/phone/brand）
3. 每字段含 prefix/suffix 字符串；空串归一为 null
4. 同名冲突逐条裁决：覆盖 / 跳过
