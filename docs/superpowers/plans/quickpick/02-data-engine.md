# Phase 2：数据层 + 匹配引擎（Room 实体/DAO/库 + 锚点引擎 + 预置规则）

> 所属轮次：第 1 轮 ｜ 前置：Phase 1
> 本文档为可执行清单：按 Task 顺序照做即可，每个 Step 含完整代码。

---

## Phase 2: 数据层 + 匹配引擎

### Task 2.1: 创建 Room 实体

**Files:**
- Create: `app/src/main/java/com/quickpick/data/db/entity/MatchRuleEntity.kt`
- Create: `app/src/main/java/com/quickpick/data/db/entity/PackageRecordEntity.kt`
- Create: `app/src/main/java/com/quickpick/data/db/entity/ScannedSmsEntity.kt`

参考 `.res/data-model.md` 表结构。

- [ ] **Step 1: 创建 MatchRuleEntity.kt**

```kotlin
package com.quickpick.data.db.entity

import androidx.room.*

@Entity(
    tableName = "match_rules",
    indices = [Index(value = ["name"])]   // 按名查找（导入同名冲突检测）
)
data class MatchRuleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "is_preset") val isPreset: Boolean = false,
    @ColumnInfo(name = "tracking_prefix") val trackingPrefix: String? = null,
    @ColumnInfo(name = "tracking_suffix") val trackingSuffix: String? = null,
    @ColumnInfo(name = "code_prefix") val codePrefix: String? = null,
    @ColumnInfo(name = "code_suffix") val codeSuffix: String? = null,
    @ColumnInfo(name = "address_prefix") val addressPrefix: String? = null,
    @ColumnInfo(name = "address_suffix") val addressSuffix: String? = null,
    @ColumnInfo(name = "phone_prefix") val phonePrefix: String? = null,
    @ColumnInfo(name = "phone_suffix") val phoneSuffix: String? = null,
    @ColumnInfo(name = "sample_sms") val sampleSms: String? = null,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "updated_at") val updatedAt: Long = System.currentTimeMillis()
)
```

- [ ] **Step 2: 创建 PackageRecordEntity.kt**

```kotlin
package com.quickpick.data.db.entity

import androidx.room.*

@Entity(
    tableName = "package_records",
    foreignKeys = [
        ForeignKey(
            entity = MatchRuleEntity::class,
            parentColumns = ["id"],
            childColumns = ["rule_id"],
            onDelete = ForeignKey.SET_NULL   // 删除规则后历史记录保留，引用置空
        )
    ],
    indices = [Index(value = ["sms_timestamp"]), Index(value = ["rule_id"])]
)
data class PackageRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "company_name") val companyName: String,
    @ColumnInfo(name = "tracking_number") val trackingNumber: String? = null,
    @ColumnInfo(name = "pickup_code") val pickupCode: String? = null,
    @ColumnInfo(name = "address") val address: String? = null,
    @ColumnInfo(name = "phone") val phone: String? = null,
    @ColumnInfo(name = "sms_body") val smsBody: String,
    @ColumnInfo(name = "sms_timestamp") val smsTimestamp: Long,
    @ColumnInfo(name = "is_picked_up") val isPickedUp: Boolean = false,
    @ColumnInfo(name = "is_incomplete") val isIncomplete: Boolean = false,
    @ColumnInfo(name = "rule_id") val ruleId: Long? = null,
    @ColumnInfo(name = "sms_id") val smsId: String? = null,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis()
)
```

- [ ] **Step 3: 创建 ScannedSmsEntity.kt**

```kotlin
package com.quickpick.data.db.entity

import androidx.room.*

@Entity(
    tableName = "scanned_sms",
    indices = [Index(value = ["sms_id"], unique = true)]
)
data class ScannedSmsEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "sms_id") val smsId: String,
    @ColumnInfo(name = "scanned_at") val scannedAt: Long = System.currentTimeMillis()
)
```

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/quickpick/data/db/entity/ && git commit -m "feat: add Room entities — MatchRule, PackageRecord, ScannedSms"
```

---

### Task 2.2: 创建 DAO

**Files:**
- Create: `app/src/main/java/com/quickpick/data/db/dao/MatchRuleDao.kt`
- Create: `app/src/main/java/com/quickpick/data/db/dao/PackageRecordDao.kt`
- Create: `app/src/main/java/com/quickpick/data/db/dao/ScannedSmsDao.kt`

- [ ] **Step 1: 创建 MatchRuleDao.kt**

```kotlin
package com.quickpick.data.db.dao

import androidx.room.*
import com.quickpick.data.db.entity.MatchRuleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MatchRuleDao {
    @Query("SELECT * FROM match_rules ORDER BY is_preset DESC, created_at ASC")
    fun observeAll(): Flow<List<MatchRuleEntity>>

    @Query("SELECT * FROM match_rules ORDER BY is_preset DESC, created_at ASC")
    suspend fun getAll(): List<MatchRuleEntity>

    @Query("SELECT * FROM match_rules WHERE id = :id")
    suspend fun getById(id: Long): MatchRuleEntity?

    /** 导入时的同名冲突检测 */
    @Query("SELECT * FROM match_rules WHERE name = :name")
    suspend fun getByName(name: String): List<MatchRuleEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(rule: MatchRuleEntity): Long

    @Update
    suspend fun update(rule: MatchRuleEntity)

    @Query("DELETE FROM match_rules WHERE id = :id")
    suspend fun deleteById(id: Long)
}
```

- [ ] **Step 2: 创建 PackageRecordDao.kt**

```kotlin
package com.quickpick.data.db.dao

import androidx.room.*
import com.quickpick.data.db.entity.PackageRecordEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PackageRecordDao {
    @Query("SELECT * FROM package_records ORDER BY sms_timestamp DESC")
    fun observeAll(): Flow<List<PackageRecordEntity>>

    @Query("SELECT COUNT(*) FROM package_records WHERE is_picked_up = 0")
    fun observeUnpickedCount(): Flow<Int>

    // 注意必须带 'localtime'：date() 默认 UTC 日界，UTC+8 下会标错天
    @Query("SELECT * FROM package_records WHERE tracking_number = :tracking AND date(sms_timestamp / 1000, 'unixepoch', 'localtime') = date(:dateMs / 1000, 'unixepoch', 'localtime') LIMIT 1")
    suspend fun findByTrackingAndDate(tracking: String, dateMs: Long): PackageRecordEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: PackageRecordEntity): Long

    @Update
    suspend fun update(record: PackageRecordEntity)

    /** 标记已取/未取的专用更新：不动 created_at 等其他字段 */
    @Query("UPDATE package_records SET is_picked_up = :picked WHERE id = :id")
    suspend fun updatePickedUp(id: Long, picked: Boolean)

    @Query("UPDATE package_records SET is_picked_up = 1")
    suspend fun markAllPickedUp()

    @Query("UPDATE package_records SET is_picked_up = 1 WHERE date(sms_timestamp / 1000, 'unixepoch', 'localtime') = date(:dateMs / 1000, 'unixepoch', 'localtime')")
    suspend fun markDayPickedUp(dateMs: Long)
}
```

- [ ] **Step 3: 创建 ScannedSmsDao.kt**

```kotlin
package com.quickpick.data.db.dao

import androidx.room.*
import com.quickpick.data.db.entity.ScannedSmsEntity

@Dao
interface ScannedSmsDao {
    @Query("SELECT EXISTS(SELECT 1 FROM scanned_sms WHERE sms_id = :smsId)")
    suspend fun exists(smsId: String): Boolean

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(entity: ScannedSmsEntity)

    @Query("SELECT COUNT(*) FROM scanned_sms")
    suspend fun count(): Int

    /** 重扫未匹配短信：清除没有对应快递记录的已扫标记，下次扫描自动重试 */
    @Query("DELETE FROM scanned_sms WHERE sms_id NOT IN (SELECT sms_id FROM package_records WHERE sms_id IS NOT NULL)")
    suspend fun deleteUnmatched()

    @Query("DELETE FROM scanned_sms")
    suspend fun clearAll()
}
```

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/quickpick/data/db/dao/ && git commit -m "feat: add Room DAOs — MatchRule, PackageRecord, ScannedSms"
```

---

### Task 2.3: 创建 Room 数据库

**Files:**
- Create: `app/src/main/java/com/quickpick/data/db/AppDatabase.kt`

- [ ] **Step 1: 创建 AppDatabase.kt**

```kotlin
package com.quickpick.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.quickpick.data.db.dao.MatchRuleDao
import com.quickpick.data.db.dao.PackageRecordDao
import com.quickpick.data.db.dao.ScannedSmsDao
import com.quickpick.data.db.entity.MatchRuleEntity
import com.quickpick.data.db.entity.PackageRecordEntity
import com.quickpick.data.db.entity.ScannedSmsEntity

@Database(
    entities = [
        MatchRuleEntity::class,
        PackageRecordEntity::class,
        ScannedSmsEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun matchRuleDao(): MatchRuleDao
    abstract fun packageRecordDao(): PackageRecordDao
    abstract fun scannedSmsDao(): ScannedSmsDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "quickpick.db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
```

- [ ] **Step 2: 更新 QuickPickApp.kt 接入数据库**

```kotlin
package com.quickpick

import android.app.Application
import com.quickpick.data.db.AppDatabase

class QuickPickApp : Application() {
    val database: AppDatabase by lazy { AppDatabase.getInstance(this) }
    // 预置规则灌入（需 DataStore）在 Task 2.6 给出最终完整版
}
```

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/quickpick/data/db/AppDatabase.kt app/src/main/java/com/quickpick/QuickPickApp.kt && git commit -m "feat: add Room AppDatabase and wire into Application"
```

---

### Task 2.4: 创建领域模型

**Files:**
- Create: `app/src/main/java/com/quickpick/domain/model/MatchRule.kt`
- Create: `app/src/main/java/com/quickpick/domain/model/PackageRecord.kt`
- Create: `app/src/main/java/com/quickpick/domain/model/ExtractedFields.kt`

- [ ] **Step 1: 创建 MatchRule.kt**

```kotlin
package com.quickpick.domain.model

data class MatchRule(
    val id: Long = 0,
    val name: String,
    val isPreset: Boolean = false,
    val trackingPrefix: String? = null,
    val trackingSuffix: String? = null,
    val codePrefix: String? = null,
    val codeSuffix: String? = null,
    val addressPrefix: String? = null,
    val addressSuffix: String? = null,
    val phonePrefix: String? = null,
    val phoneSuffix: String? = null,
    val sampleSms: String? = null
) {
    /** 有配置锚点的字段数 */
    val configuredFieldCount: Int
        get() = listOf(
            trackingPrefix != null || trackingSuffix != null,
            codePrefix != null || codeSuffix != null,
            addressPrefix != null || addressSuffix != null,
            phonePrefix != null || phoneSuffix != null
        ).count { it }

    /** 是否有任何字段配置了锚点 */
    val hasAnyField: Boolean
        get() = configuredFieldCount > 0
}
```

- [ ] **Step 2: 创建 ExtractedFields.kt**

```kotlin
package com.quickpick.domain.model

data class ExtractedFields(
    val companyName: String? = null,
    val trackingNumber: String? = null,
    val pickupCode: String? = null,
    val address: String? = null,
    val phone: String? = null
) {
    /** 成功提取的字段数 */
    val matchedCount: Int
        get() = listOfNotNull(
            trackingNumber, pickupCode, address, phone
        ).size

    /** 是否至少有一个取件相关字段（取件码或地址） */
    val isPickupRelated: Boolean
        get() = pickupCode != null || address != null
}
// 注意：没有 isIncomplete 属性。「不完整」由调用方按规则口径计算：
// isIncomplete = matchedCount < rule.configuredFieldCount（规则配置了但没提取到才算不完整；
// 电话是可选字段，不能按固定 4 个字段硬编码，否则没电话的快递永远显示"不完整"）
```

- [ ] **Step 3: 创建 PackageRecord.kt**

```kotlin
package com.quickpick.domain.model

data class PackageRecord(
    val id: Long = 0,
    val companyName: String,
    val trackingNumber: String? = null,
    val pickupCode: String? = null,
    val address: String? = null,
    val phone: String? = null,
    val smsBody: String,
    val smsTimestamp: Long,
    val isPickedUp: Boolean = false,
    val isIncomplete: Boolean = false,
    val ruleId: Long? = null,
    val smsId: String? = null
)
```

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/quickpick/domain/model/ && git commit -m "feat: add domain models — MatchRule, PackageRecord, ExtractedFields"
```

---

### Task 2.5: 创建匹配引擎

**文件:**
- Create: `app/src/main/java/com/quickpick/domain/engine/MatchEngine.kt`

- [ ] **Step 1: 创建 MatchEngine.kt**

```kotlin
package com.quickpick.domain.engine

import com.quickpick.domain.model.ExtractedFields
import com.quickpick.domain.model.MatchRule

object MatchEngine {

    /**
     * 用前缀/后缀锚点从短信中提取字段内容。
     * 找到 prefix → 取其后内容 → 直到 suffix 截止。
     * 不使用正则，纯 indexOf 定位。
     */
    fun extract(smsBody: String, rule: MatchRule): ExtractedFields {
        return ExtractedFields(
            companyName = rule.name,
            trackingNumber = extractField(smsBody, rule.trackingPrefix, rule.trackingSuffix),
            pickupCode = extractField(smsBody, rule.codePrefix, rule.codeSuffix),
            address = extractField(smsBody, rule.addressPrefix, rule.addressSuffix),
            phone = extractField(smsBody, rule.phonePrefix, rule.phoneSuffix)
        )
    }

    private fun extractField(text: String, prefix: String?, suffix: String?): String? {
        if (prefix.isNullOrEmpty()) return null

        val startIndex = text.indexOf(prefix)
        if (startIndex == -1) return null

        val contentStart = startIndex + prefix.length
        if (contentStart >= text.length) return null

        val remaining = text.substring(contentStart)

        val raw = if (suffix.isNullOrEmpty()) {
            // 无后缀：取到文本末尾
            remaining
        } else {
            val endIndex = remaining.indexOf(suffix)
            // 后缀没找到：取到文本末尾
            if (endIndex == -1) remaining else remaining.substring(0, endIndex)
        }
        // 空串视为未匹配（前缀后紧跟后缀 / 只有空白），否则会污染 matchedCount 优先级比较
        return raw.trim().ifEmpty { null }
    }

    /**
     * 对一条短信跑所有规则，返回匹配字段数最多的结果。
     * 字段数相同时取规则列表顺序靠前的。
     */
    fun findBestMatch(smsBody: String, rules: List<MatchRule>): Pair<MatchRule, ExtractedFields>? {
        var best: Pair<MatchRule, ExtractedFields>? = null

        for (rule in rules) {
            if (!rule.hasAnyField) continue

            val fields = extract(smsBody, rule)
            if (!fields.isPickupRelated) continue

            val current = best
            if (current == null || fields.matchedCount > current.second.matchedCount) {
                best = rule to fields
            }
        }

        return best
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add app/src/main/java/com/quickpick/domain/engine/MatchEngine.kt && git commit -m "feat: add anchor-based MatchEngine — prefix/suffix string extraction"
```

---

### Task 2.6: 创建预置规则 + SettingsStore

**Files:**
- Create: `app/src/main/java/com/quickpick/data/preset/PresetRules.kt`
- Create: `app/src/main/java/com/quickpick/data/local/SettingsStore.kt`
- Modify: `app/src/main/java/com/quickpick/QuickPickApp.kt`（最终完整版）

- [ ] **Step 1: 创建 SettingsStore.kt（DataStore 轻量偏好）**

```kotlin
package com.quickpick.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "quickpick_prefs")

/** 轻量偏好存储：权限引导完成标记、预置规则灌入标记 */
class SettingsStore(private val context: Context) {

    companion object {
        private val KEY_ONBOARDING_DONE = booleanPreferencesKey("onboarding_done")
        private val KEY_RULES_SEEDED = booleanPreferencesKey("rules_seeded")
    }

    val onboardingDone: Flow<Boolean> =
        context.dataStore.data.map { it[KEY_ONBOARDING_DONE] ?: false }

    suspend fun isOnboardingDone(): Boolean = onboardingDone.first()

    suspend fun setOnboardingDone() {
        context.dataStore.edit { it[KEY_ONBOARDING_DONE] = true }
    }

    suspend fun isRulesSeeded(): Boolean =
        context.dataStore.data.map { it[KEY_RULES_SEEDED] ?: false }.first()

    suspend fun setRulesSeeded() {
        context.dataStore.edit { it[KEY_RULES_SEEDED] = true }
    }
}
```

- [ ] **Step 2: 创建 PresetRules.kt**

```kotlin
package com.quickpick.data.preset

import com.quickpick.data.db.dao.MatchRuleDao
import com.quickpick.data.db.entity.MatchRuleEntity
import com.quickpick.data.local.SettingsStore

object PresetRules {

    val all: List<MatchRuleEntity> = listOf(
        MatchRuleEntity(
            name = "顺丰速运",
            isPreset = true,
            trackingPrefix = "单号",
            trackingSuffix = "，",
            codePrefix = "取件码：",
            codeSuffix = "，",
            addressPrefix = "到达",
            addressSuffix = "，",
            phonePrefix = "电话：",
            phoneSuffix = "",
            sampleSms = "【顺丰速运】您的包裹已到达菜鸟驿站(阳光花园店)，取件码：3-2-1088，单号SF1234567890123，请尽快取件。电话：13800001234"
        ),
        MatchRuleEntity(
            name = "京东物流",
            isPreset = true,
            trackingPrefix = "订单",
            trackingSuffix = "已送达",
            codePrefix = "取货码",
            codeSuffix = "，",
            addressPrefix = "送达",
            addressSuffix = "，",
            phonePrefix = "联系电话",
            phoneSuffix = "，",
            sampleSms = "【京东物流】您的订单4901234567已送达京东便利店(万达店)，取货码8-3-1024，联系电话13800005678，请尽快取件。"
        )
    )

    /**
     * 仅首次启动灌入预置规则。
     * 判定用 DataStore 标记而非「表是否为空」——否则用户删光规则后下次启动预置会复活，
     * 「用户可删除预置规则」和「删光规则的空状态」都会被破坏。
     */
    suspend fun seedIfNeeded(store: SettingsStore, dao: MatchRuleDao) {
        if (store.isRulesSeeded()) return
        all.forEach { dao.insert(it) }
        store.setRulesSeeded()
    }
}
```

- [ ] **Step 3: QuickPickApp.kt 最终完整版（整文件替换）**

```kotlin
package com.quickpick

import android.app.Application
import android.util.Log
import com.quickpick.data.db.AppDatabase
import com.quickpick.data.local.SettingsStore
import com.quickpick.data.preset.PresetRules
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class QuickPickApp : Application() {

    val database: AppDatabase by lazy { AppDatabase.getInstance(this) }
    val settingsStore: SettingsStore by lazy { SettingsStore(this) }

    // SupervisorJob：种子出错不影响其他协程；带日志，不静默
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        appScope.launch {
            runCatching {
                PresetRules.seedIfNeeded(settingsStore, database.matchRuleDao())
            }.onFailure { Log.w("QuickPickApp", "预置规则没能灌入", it) }
        }
    }
}
```

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/quickpick/data/ app/src/main/java/com/quickpick/QuickPickApp.kt && git commit -m "feat: add preset rules (顺丰/京东) + DataStore SettingsStore, seed on first launch only"
```

---

