# 取件宝 — Room 数据模型

## 表结构

### 1. match_rules（匹配规则）

| 列名 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | INTEGER | PK, AUTO | 主键 |
| name | TEXT | NOT NULL | 规则名 = 快递公司名 |
| is_preset | INTEGER | NOT NULL, DEFAULT 0 | 0=自定义, 1=预置 |
| tracking_prefix | TEXT | NULLABLE | 单号-前缀 |
| tracking_suffix | TEXT | NULLABLE | 单号-后缀 |
| code_prefix | TEXT | NULLABLE | 取件码-前缀 |
| code_suffix | TEXT | NULLABLE | 取件码-后缀 |
| address_prefix | TEXT | NULLABLE | 地址-前缀 |
| address_suffix | TEXT | NULLABLE | 地址-后缀 |
| phone_prefix | TEXT | NULLABLE | 电话-前缀 |
| phone_suffix | TEXT | NULLABLE | 电话-后缀 |
| sample_sms | TEXT | NULLABLE | 原始样本短信 |
| created_at | INTEGER | NOT NULL | 创建时间戳(ms) |
| updated_at | INTEGER | NOT NULL | 更新时间戳(ms) |

### 2. package_records（快递记录）

| 列名 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | INTEGER | PK, AUTO | 主键 |
| company_name | TEXT | NOT NULL | 快递公司名 |
| tracking_number | TEXT | NULLABLE | 快递单号 |
| pickup_code | TEXT | NULLABLE | 取件码/柜号 |
| address | TEXT | NULLABLE | 取件地址/驿站 |
| phone | TEXT | NULLABLE | 快递员/客服电话 |
| sms_body | TEXT | NOT NULL | 原始短信全文 |
| sms_timestamp | INTEGER | NOT NULL | 短信接收时间(ms) |
| is_picked_up | INTEGER | NOT NULL, DEFAULT 0 | 0=未取, 1=已取 |
| is_incomplete | INTEGER | NOT NULL, DEFAULT 0 | 0=完整, 1=字段不全 |
| rule_id | INTEGER | FK → match_rules.id | 匹配的规则 ID |
| sms_id | TEXT | NULLABLE | 系统短信 ID（去重用） |
| created_at | INTEGER | NOT NULL | 记录创建时间(ms) |

### 3. scanned_sms（已扫描短信）

| 列名 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | INTEGER | PK, AUTO | 主键 |
| sms_id | TEXT | UNIQUE, NOT NULL | 系统短信消息 ID |
| scanned_at | INTEGER | NOT NULL | 扫描时间戳(ms) |

## 索引

```sql
-- 快递列表按时间倒序，按天分组
CREATE INDEX idx_package_timestamp ON package_records(sms_timestamp DESC);

-- 去重查询
CREATE UNIQUE INDEX idx_scanned_sms_id ON scanned_sms(sms_id);

-- 按规则名查找
CREATE INDEX idx_rules_name ON match_rules(name);
```

## 数据流

```
系统短信 → 读取 → 查 scanned_sms 去重
                  ↓
            跑 match_rules 匹配
                  ↓
            写入 package_records
                  ↓
            SMS ID 写入 scanned_sms
```

## 与 JSON 导出格式的映射

导出 JSON 格式见 `preset-rules.json`。导入时逐字段映射到表列：

| JSON key | 表列 |
|----------|------|
| name | name |
| fields.trackingNumber.prefix | tracking_prefix |
| fields.trackingNumber.suffix | tracking_suffix |
| fields.pickupCode.prefix | code_prefix |
| fields.pickupCode.suffix | code_suffix |
| fields.address.prefix | address_prefix |
| fields.address.suffix | address_suffix |
| fields.phone.prefix | phone_prefix |
| fields.phone.suffix | phone_suffix |

导入时 `is_preset` 一律设 0（自定义），`sample_sms` 留空。
