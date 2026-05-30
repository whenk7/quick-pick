# AI 生成提取规则 — 预置提示词

## 中文版（推荐）

```
你是一个快递短信解析专家。我会给你一条快递取件通知短信，请你分析短信内容，
为每个字段提取"前缀"和"后缀"锚点，输出 JSON 格式的提取规则。

字段说明：
- trackingNumber: 快递单号（运单号，如 SF1234567890）
- pickupCode: 取件码 / 快递柜号 / 货架号（取件凭证）
- address: 取件地址 / 驿站名 / 快递柜位置
- phone: 快递员或客服电话（短信中有则提取，无则留空）

规则格式：
每个字段用 prefix（前缀固定文本）和 suffix（后缀固定文本）定位。
提取逻辑：在短信中找到 prefix，取其后内容，直到 suffix 截止。
prefix 和 suffix 尽量取最短、最能唯一定位的文本。
如果短信中没有该字段，prefix 和 suffix 都留空字符串 ""。

输出严格 JSON，不要任何解释：

{
  "name": "快递公司名",
  "fields": {
    "trackingNumber": { "prefix": "单号", "suffix": "，" },
    "pickupCode":    { "prefix": "取件码：", "suffix": "，" },
    "address":       { "prefix": "到达", "suffix": "，" },
    "phone":         { "prefix": "电话：", "suffix": "" }
  }
}

短信内容：
---
{{SMS_CONTENT}}
---

请输出 JSON：
```

## 使用方式

App 将 `{{SMS_CONTENT}}` 替换为用户粘贴的短信原文，生成完整 prompt。
用户一键复制 → 去 ChatGPT / Claude / Kimi / 豆包 等任意 AI 服务粘贴 → 获得 JSON → 导入取件宝。

## JSON 校验

导入时 App 做以下校验：
1. 是否为合法 JSON
2. 是否包含 name 和 fields
3. fields 中的 key 是否在允许列表中（trackingNumber / pickupCode / address / phone）
4. 每个 field 是否包含 prefix 和 suffix 字符串
5. 校验不通过 → 提示用户 JSON 格式不正确，请重新生成
