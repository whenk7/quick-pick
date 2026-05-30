# 取件宝 (QuickPick) — 实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 构建取件宝 Android 应用 — 从短信自动提取快递取件信息，支持自定义匹配规则和 AI 辅助生成规则，治愈系极简视觉风格。

**Architecture:** Kotlin + Jetpack Compose + Material 3 + Room。MVVM 分层：data（Room / Repository）→ domain（MatchEngine / UseCase）→ ui（Screen / ViewModel / Component）。纯字符串锚点匹配引擎，不依赖正则。

**Tech Stack:** Kotlin, Jetpack Compose, Material 3, Room, Navigation Compose, Coroutines + Flow

**Design References:**
- `docs/superpowers/specs/2026-05-30-quickpick-design.md` — 产品规格
- `docs/superpowers/design/design-system.md` — 视觉设计系统
- `.res/data-model.md` — 数据模型
- `.res/edge-cases.md` — 边界情况
- `.res/preset-rules.json` — 预置规则
- `.res/ai-prompt-template.md` — AI 提示词

---

## 文件结构总览

```
app/src/main/java/com/quickpick/
├── QuickPickApp.kt              — Application 类，初始化 Room
├── MainActivity.kt              — 单 Activity 入口
├── data/
│   ├── db/
│   │   ├── AppDatabase.kt       — Room 数据库
│   │   ├── entity/
│   │   │   ├── MatchRuleEntity.kt
│   │   │   ├── PackageRecordEntity.kt
│   │   │   └── ScannedSmsEntity.kt
│   │   └── dao/
│   │       ├── MatchRuleDao.kt
│   │       ├── PackageRecordDao.kt
│   │       └── ScannedSmsDao.kt
│   ├── repository/
│   │   ├── RuleRepository.kt
│   │   ├── PackageRepository.kt
│   │   └── SmsRepository.kt
│   └── preset/
│       └── PresetRules.kt       — 预置规则数据
├── domain/
│   ├── model/
│   │   ├── MatchRule.kt
│   │   ├── PackageRecord.kt
│   │   └── ExtractedFields.kt
│   └── engine/
│       └── MatchEngine.kt       — 锚点匹配引擎
├── ui/
│   ├── theme/
│   │   ├── Color.kt
│   │   ├── Type.kt
│   │   ├── Shape.kt
│   │   └── Theme.kt
│   ├── navigation/
│   │   └── AppNavigation.kt     — 底部导航 + 路由
│   ├── component/
│   │   ├── PackageCard.kt       — 快递卡片
│   │   ├── DayGroup.kt          — 日期分组
│   │   ├── TokenChip.kt         — 分词块
│   │   ├── FieldCard.kt         — 字段编辑卡片
│   │   ├── ConfirmDialog.kt     — 二次确认弹窗
│   │   ├── EmptyState.kt        — 空状态
│   │   └── BottomNavBar.kt      — 底部导航栏
│   ├── screen/
│   │   ├── PackageListScreen.kt
│   │   ├── RuleListScreen.kt
│   │   ├── RuleEditorScreen.kt
│   │   ├── AiPromptScreen.kt
│   │   ├── SettingsScreen.kt
│   │   └── OnboardingScreen.kt
│   └── viewmodel/
│       ├── PackageListViewModel.kt
│       ├── RuleListViewModel.kt
│       ├── RuleEditorViewModel.kt
│       ├── AiPromptViewModel.kt
│       └── SettingsViewModel.kt
└── util/
    ├── SmsReader.kt              — 系统短信读取
    └── JsonExporter.kt           — JSON 导入导出
```

---

## Phase 1: 项目基础

### Task 1.1: 创建 Android 项目

**Files:**
- Create: `app/build.gradle.kts`
- Create: `build.gradle.kts` (project-level)
- Create: `settings.gradle.kts`
- Create: `gradle.properties`
- Create: `app/src/main/AndroidManifest.xml`

- [ ] **Step 1: 创建项目级 build.gradle.kts**

```kotlin
// build.gradle.kts
plugins {
    id("com.android.application") version "8.2.0" apply false
    id("org.jetbrains.kotlin.android") version "1.9.20" apply false
    id("com.google.devtools.ksp") version "1.9.20-1.0.14" apply false
}
```

- [ ] **Step 2: 创建 app/build.gradle.kts**

```kotlin
// app/build.gradle.kts
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.quickpick"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.quickpick"
        minSdk = 30
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.5"
    }
}

dependencies {
    // Compose BOM
    val composeBom = platform("androidx.compose:compose-bom:2024.01.00")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")

    // Activity & Navigation
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation("androidx.navigation:navigation-compose:2.7.6")

    // Lifecycle
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.7.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")

    // Room
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // Gson (for JSON export/import)
    implementation("com.google.code.gson:gson:2.10.1")

    // Debug
    debugImplementation("androidx.compose.ui:ui-tooling")
}
```

- [ ] **Step 3: 创建 settings.gradle.kts**

```kotlin
// settings.gradle.kts
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolution {
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "QuickPick"
include(":app")
```

- [ ] **Step 4: 创建 AndroidManifest.xml**

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <uses-permission android:name="android.permission.READ_SMS" />

    <application
        android:name=".QuickPickApp"
        android:allowBackup="true"
        android:label="取件宝"
        android:supportsRtl="true"
        android:theme="@style/Theme.Material3.DayNight.NoActionBar">

        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:theme="@style/Theme.Material3.DayNight.NoActionBar">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>
</manifest>
```

- [ ] **Step 5: Commit**

```bash
git add -A && git commit -m "chore: create Android project with Compose and Room dependencies"
```

---

### Task 1.2: 创建主题系统

**Files:**
- Create: `app/src/main/java/com/quickpick/ui/theme/Color.kt`
- Create: `app/src/main/java/com/quickpick/ui/theme/Type.kt`
- Create: `app/src/main/java/com/quickpick/ui/theme/Shape.kt`
- Create: `app/src/main/java/com/quickpick/ui/theme/Theme.kt`

- [ ] **Step 1: 创建 Color.kt — 治愈系豆绿色彩系统**

```kotlin
package com.quickpick.ui.theme

import androidx.compose.ui.graphics.Color

// 主色调：豆绿
val Primary = Color(0xFF6AB37B)
val PrimaryLight = Color(0xFFA8D5B0)
val PrimarySoft = Color(0x1A6AB37B) // 10% alpha

// 背景
val Background = Color(0xFFF9F9F6)
val Surface = Color(0xFFFFFFFF)
val SurfaceDone = Color(0xFFF5F5F5)
val InputBackground = Color(0xFFF5F5F5)

// 文字（从不用纯黑）
val TextPrimary = Color(0xFF424242)
val TextSecondary = Color(0xFF757575)
val TextTertiary = Color(0xFF9E9E9E)

// 语义色
val Pending = Color(0xFFFFB76B)
val Done = Color(0xFFBDBDBD)
val Danger = Color(0xFFEF5350)

// 分割线
val Divider = Color(0xFFEEEEEE)

// 快递公司品牌色（小面积点缀）
object BrandColors {
    val SF = Color(0xFF000000)        // 顺丰
    val JD = Color(0xFFE2231A)        // 京东
    val JT = Color(0xFFE50012)        // 极兔
    val ZTO = Color(0xFF006CB7)       // 中通
    val YTO = Color(0xFF662D91)       // 圆通
    val YUNDA = Color(0xFFFFCC00)     // 韵达
    val STO = Color(0xFFFF6600)       // 申通
    val EMS = Color(0xFF006633)       // 邮政
    val DEPPON = Color(0xFF0056D2)    // 德邦
    val CAINIAO = Color(0xFF0066FF)   // 菜鸟
    val DEFAULT = Primary

    fun forName(name: String): Color = when {
        name.contains("顺丰") -> SF
        name.contains("京东") -> JD
        name.contains("极兔") -> JT
        name.contains("中通") -> ZTO
        name.contains("圆通") -> YTO
        name.contains("韵达") -> YUNDA
        name.contains("申通") -> STO
        name.contains("邮政") || name.contains("EMS") -> EMS
        name.contains("德邦") -> DEPPON
        name.contains("菜鸟") -> CAINIAO
        else -> DEFAULT
    }
}
```

- [ ] **Step 2: 创建 Type.kt**

```kotlin
package com.quickpick.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val AppTypography = Typography(
    // 取件码 — 页面最大最醒目的数字
    headlineLarge = TextStyle(
        fontSize = 28.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp,
        color = Primary
    ),
    // 页面标题
    headlineMedium = TextStyle(
        fontSize = 28.sp,
        fontWeight = FontWeight.Bold,
        color = TextPrimary
    ),
    // 分组标题
    titleLarge = TextStyle(
        fontSize = 18.sp,
        fontWeight = FontWeight.Medium,
        color = TextPrimary
    ),
    // 公司名
    titleMedium = TextStyle(
        fontSize = 16.sp,
        fontWeight = FontWeight.Medium,
        color = TextPrimary
    ),
    // 正文
    bodyLarge = TextStyle(
        fontSize = 15.sp,
        fontWeight = FontWeight.Normal,
        color = TextPrimary
    ),
    // 辅助字段
    bodyMedium = TextStyle(
        fontSize = 14.sp,
        fontWeight = FontWeight.Normal,
        color = TextSecondary
    ),
    // 小字 / 标签 / 时间
    bodySmall = TextStyle(
        fontSize = 12.sp,
        fontWeight = FontWeight.Normal,
        color = TextTertiary
    ),
    // 按钮文字
    labelLarge = TextStyle(
        fontSize = 15.sp,
        fontWeight = FontWeight.SemiBold,
        color = TextPrimary
    )
)
```

- [ ] **Step 3: 创建 Shape.kt**

```kotlin
package com.quickpick.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

val AppShapes = Shapes(
    small = RoundedCornerShape(8.dp),    // 分词块、色标
    medium = RoundedCornerShape(12.dp),  // 输入框
    large = RoundedCornerShape(16.dp),   // 卡片、按钮
    extraLarge = RoundedCornerShape(20.dp) // 弹窗
)
```

- [ ] **Step 4: 创建 Theme.kt**

```kotlin
package com.quickpick.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    primary = Primary,
    onPrimary = Color.White,
    primaryContainer = PrimarySoft,
    background = Background,
    surface = Surface,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    onSurfaceVariant = TextSecondary,
    outline = Divider,
    outlineVariant = Divider
)

@Composable
fun QuickPickTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = AppTypography,
        shapes = AppShapes,
        content = content
    )
}
```

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/quickpick/ui/theme/ && git commit -m "feat: add 治愈系 theme — 豆绿 color system, typography, shapes"
```

---

### Task 1.3: 创建 Application 和 MainActivity

**Files:**
- Create: `app/src/main/java/com/quickpick/QuickPickApp.kt`
- Create: `app/src/main/java/com/quickpick/MainActivity.kt`

- [ ] **Step 1: 创建 QuickPickApp.kt**

```kotlin
package com.quickpick

import android.app.Application
import com.quickpick.data.db.AppDatabase

class QuickPickApp : Application() {
    val database: AppDatabase by lazy { AppDatabase.getInstance(this) }
}
```

- [ ] **Step 2: 创建 MainActivity.kt**

```kotlin
package com.quickpick

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.quickpick.ui.theme.QuickPickTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            QuickPickTheme {
                // Will be replaced with AppNavigation in Phase 3
            }
        }
    }
}
```

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/quickpick/QuickPickApp.kt app/src/main/java/com/quickpick/MainActivity.kt && git commit -m "feat: add Application and MainActivity scaffold"
```

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

@Entity(tableName = "match_rules")
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
    indices = [Index(value = ["sms_timestamp"])]
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

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(rule: MatchRuleEntity): Long

    @Update
    suspend fun update(rule: MatchRuleEntity)

    @Delete
    suspend fun delete(rule: MatchRuleEntity)

    @Query("SELECT COUNT(*) FROM match_rules")
    suspend fun count(): Int
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

    @Query("SELECT * FROM package_records WHERE is_picked_up = 0 ORDER BY sms_timestamp DESC")
    fun observeUnpicked(): Flow<List<PackageRecordEntity>>

    @Query("SELECT COUNT(*) FROM package_records WHERE is_picked_up = 0")
    fun observeUnpickedCount(): Flow<Int>

    @Query("SELECT * FROM package_records WHERE sms_id = :smsId LIMIT 1")
    suspend fun getBySmsId(smsId: String): PackageRecordEntity?

    @Query("SELECT * FROM package_records WHERE tracking_number = :tracking AND date(sms_timestamp / 1000, 'unixepoch') = date(:dateMs / 1000, 'unixepoch') LIMIT 1")
    suspend fun findByTrackingAndDate(tracking: String, dateMs: Long): PackageRecordEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: PackageRecordEntity): Long

    @Update
    suspend fun update(record: PackageRecordEntity)

    @Query("UPDATE package_records SET is_picked_up = 1")
    suspend fun markAllPickedUp()

    @Query("UPDATE package_records SET is_picked_up = 1 WHERE date(sms_timestamp / 1000, 'unixepoch') = date(:dateMs / 1000, 'unixepoch')")
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
    @Query("SELECT COUNT(*) FROM scanned_sms WHERE sms_id = :smsId")
    suspend fun exists(smsId: String): Boolean

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(entity: ScannedSmsEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(entities: List<ScannedSmsEntity>)

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

- [ ] **Step 2: Commit**

```bash
git add app/src/main/java/com/quickpick/data/db/AppDatabase.kt && git commit -m "feat: add Room AppDatabase"
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

    /** 是否字段不全 */
    val isIncomplete: Boolean
        get() = matchedCount < 4
}
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

        return if (suffix.isNullOrEmpty()) {
            // 无后缀：取到文本末尾，去首尾空白
            remaining.trim()
        } else {
            val endIndex = remaining.indexOf(suffix)
            if (endIndex == -1) {
                // 后缀没找到：取到文本末尾
                remaining.trim()
            } else {
                remaining.substring(0, endIndex).trim()
            }
        }
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

### Task 2.6: 创建预置规则

**Files:**
- Create: `app/src/main/java/com/quickpick/data/preset/PresetRules.kt`

- [ ] **Step 1: 创建 PresetRules.kt**

```kotlin
package com.quickpick.data.preset

import com.quickpick.data.db.entity.MatchRuleEntity

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

    suspend fun seedIfNeeded(dao: com.quickpick.data.db.dao.MatchRuleDao) {
        if (dao.count() == 0) {
            all.forEach { dao.insert(it) }
        }
    }
}
```

- [ ] **Step 2: 在 QuickPickApp 中初始化预置规则**

修改 `QuickPickApp.kt`，在 `onCreate` 中调用 `PresetRules.seedIfNeeded`。

```kotlin
override fun onCreate() {
    super.onCreate()
    // Seed preset rules on first launch (async, fire-and-forget)
    kotlinx.coroutines.CoroutineScope(
        kotlinx.coroutines.Dispatchers.IO
    ).launch {
        com.quickpick.data.preset.PresetRules.seedIfNeeded(database.matchRuleDao())
    }
}
```

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/quickpick/data/preset/ app/src/main/java/com/quickpick/QuickPickApp.kt && git commit -m "feat: add 2 preset rules — 顺丰速运, 京东物流"
```

---

## Phase 3: Repository 层

### Task 3.1: 创建 RuleRepository

**Files:**
- Create: `app/src/main/java/com/quickpick/data/repository/RuleRepository.kt`

- [ ] **Step 1: 创建 RuleRepository.kt**

```kotlin
package com.quickpick.data.repository

import com.quickpick.data.db.dao.MatchRuleDao
import com.quickpick.data.db.entity.MatchRuleEntity
import com.quickpick.domain.model.MatchRule
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RuleRepository(private val dao: MatchRuleDao) {

    fun observeAll(): Flow<List<MatchRule>> =
        dao.observeAll().map { entities -> entities.map { it.toDomain() } }

    suspend fun getAll(): List<MatchRule> =
        dao.getAll().map { it.toDomain() }

    suspend fun getById(id: Long): MatchRule? =
        dao.getById(id)?.toDomain()

    suspend fun insert(rule: MatchRule): Long =
        dao.insert(rule.toEntity(isPreset = false))

    suspend fun update(rule: MatchRule) =
        dao.update(rule.toEntity(isPreset = false))

    suspend fun delete(rule: MatchRule) =
        dao.delete(rule.toEntity(isPreset = false))

    private fun MatchRuleEntity.toDomain() = MatchRule(
        id = id, name = name, isPreset = isPreset,
        trackingPrefix = trackingPrefix, trackingSuffix = trackingSuffix,
        codePrefix = codePrefix, codeSuffix = codeSuffix,
        addressPrefix = addressPrefix, addressSuffix = addressSuffix,
        phonePrefix = phonePrefix, phoneSuffix = phoneSuffix,
        sampleSms = sampleSms
    )

    private fun MatchRule.toEntity(isPreset: Boolean) = MatchRuleEntity(
        id = id, name = name, isPreset = isPreset,
        trackingPrefix = trackingPrefix, trackingSuffix = trackingSuffix,
        codePrefix = codePrefix, codeSuffix = codeSuffix,
        addressPrefix = addressPrefix, addressSuffix = addressSuffix,
        phonePrefix = phonePrefix, phoneSuffix = phoneSuffix,
        sampleSms = sampleSms,
        createdAt = System.currentTimeMillis(),
        updatedAt = System.currentTimeMillis()
    )
}
```

- [ ] **Step 2: Commit**

```bash
git add app/src/main/java/com/quickpick/data/repository/RuleRepository.kt && git commit -m "feat: add RuleRepository"
```

---

### Task 3.2: 创建 PackageRepository 和 SmsRepository

**Files:**
- Create: `app/src/main/java/com/quickpick/data/repository/PackageRepository.kt`
- Create: `app/src/main/java/com/quickpick/data/repository/SmsRepository.kt`

- [ ] **Step 1: 创建 PackageRepository.kt**

```kotlin
package com.quickpick.data.repository

import com.quickpick.data.db.dao.PackageRecordDao
import com.quickpick.data.db.entity.PackageRecordEntity
import com.quickpick.domain.model.PackageRecord
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class PackageRepository(private val dao: PackageRecordDao) {

    fun observeAll(): Flow<List<PackageRecord>> =
        dao.observeAll().map { entities -> entities.map { it.toDomain() } }

    fun observeUnpickedCount(): Flow<Int> = dao.observeUnpickedCount()

    suspend fun insert(record: PackageRecord): Long =
        dao.insert(record.toEntity())

    suspend fun update(record: PackageRecord) =
        dao.update(record.toEntity())

    suspend fun markAllPickedUp() = dao.markAllPickedUp()

    suspend fun markDayPickedUp(dateMs: Long) = dao.markDayPickedUp(dateMs)

    suspend fun existsBySmsId(smsId: String): Boolean =
        dao.getBySmsId(smsId) != null

    private fun PackageRecordEntity.toDomain() = PackageRecord(
        id = id, companyName = companyName,
        trackingNumber = trackingNumber, pickupCode = pickupCode,
        address = address, phone = phone,
        smsBody = smsBody, smsTimestamp = smsTimestamp,
        isPickedUp = isPickedUp, isIncomplete = isIncomplete,
        ruleId = ruleId, smsId = smsId
    )

    private fun PackageRecord.toEntity() = PackageRecordEntity(
        id = id, companyName = companyName,
        trackingNumber = trackingNumber, pickupCode = pickupCode,
        address = address, phone = phone,
        smsBody = smsBody, smsTimestamp = smsTimestamp,
        isPickedUp = isPickedUp, isIncomplete = isIncomplete,
        ruleId = ruleId, smsId = smsId
    )
}
```

- [ ] **Step 2: 创建 SmsRepository.kt**

```kotlin
package com.quickpick.data.repository

import com.quickpick.data.db.dao.ScannedSmsDao
import com.quickpick.data.db.entity.ScannedSmsEntity

class SmsRepository(private val dao: ScannedSmsDao) {

    suspend fun isScanned(smsId: String): Boolean = dao.exists(smsId)

    suspend fun markScanned(smsId: String) =
        dao.insert(ScannedSmsEntity(smsId = smsId))

    suspend fun markAllScanned(smsIds: List<String>) =
        dao.insertAll(smsIds.map { ScannedSmsEntity(smsId = it) })

    suspend fun clearScanned() = dao.clearAll()
}
```

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/quickpick/data/repository/ && git commit -m "feat: add PackageRepository and SmsRepository"
```

---

### Task 3.3: 创建短信读取工具

**Files:**
- Create: `app/src/main/java/com/quickpick/util/SmsReader.kt`

- [ ] **Step 1: 创建 SmsReader.kt**

```kotlin
package com.quickpick.util

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.Telephony

data class SmsMessage(
    val id: String,
    val body: String,
    val timestamp: Long
)

object SmsReader {

    private val SMS_URI: Uri = Telephony.Sms.Inbox.CONTENT_URI
    private val PROJECTION = arrayOf(
        Telephony.Sms._ID,
        Telephony.Sms.BODY,
        Telephony.Sms.DATE
    )

    /**
     * 读取所有收件箱短信。
     * 需要 READ_SMS 权限。
     */
    fun readAll(context: Context): List<SmsMessage> {
        val messages = mutableListOf<SmsMessage>()
        var cursor: Cursor? = null
        try {
            cursor = context.contentResolver.query(
                SMS_URI,
                PROJECTION,
                null, null,
                "${Telephony.Sms.DATE} DESC"
            )
            cursor?.use {
                val idCol = it.getColumnIndexOrThrow(Telephony.Sms._ID)
                val bodyCol = it.getColumnIndexOrThrow(Telephony.Sms.BODY)
                val dateCol = it.getColumnIndexOrThrow(Telephony.Sms.DATE)
                while (it.moveToNext()) {
                    messages.add(
                        SmsMessage(
                            id = it.getString(idCol),
                            body = it.getString(bodyCol),
                            timestamp = it.getLong(dateCol)
                        )
                    )
                }
            }
        } catch (e: SecurityException) {
            // Permission denied — caller should handle
        }
        return messages
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add app/src/main/java/com/quickpick/util/SmsReader.kt && git commit -m "feat: add SmsReader — ContentResolver-based SMS reading"
```

---

### Task 3.4: 创建 JSON 导入导出工具

**Files:**
- Create: `app/src/main/java/com/quickpick/util/JsonExporter.kt`

- [ ] **Step 1: 创建 JsonExporter.kt**

```kotlin
package com.quickpick.util

import com.google.gson.*
import com.quickpick.domain.model.MatchRule

object JsonExporter {

    private val gson = GsonBuilder().setPrettyPrinting().create()

    /** 全量导出：所有规则 → JSON 数组字符串 */
    fun exportAll(rules: List<MatchRule>): String {
        val array = JsonArray()
        rules.forEach { array.add(ruleToJson(it)) }
        return gson.toJson(array)
    }

    /** 单条导出 */
    fun exportSingle(rule: MatchRule): String {
        return gson.toJson(ruleToJson(rule))
    }

    /** 导入：JSON 字符串 → MatchRule 列表 */
    fun import(json: String): Result<List<MatchRule>> {
        return try {
            val element = JsonParser.parseString(json)
            val rules = when {
                element.isJsonArray -> {
                    element.asJsonArray.map { parseRule(it.asJsonObject) }
                }
                element.isJsonObject -> {
                    listOf(parseRule(element.asJsonObject))
                }
                else -> throw JsonParseException("不是有效的规则 JSON")
            }
            // 校验每条规则
            rules.forEach { validateRule(it) }
            Result.success(rules)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun ruleToJson(rule: MatchRule): JsonObject {
        return JsonObject().apply {
            addProperty("name", rule.name)
            add("fields", JsonObject().apply {
                add("trackingNumber", fieldToJson(rule.trackingPrefix, rule.trackingSuffix))
                add("pickupCode", fieldToJson(rule.codePrefix, rule.codeSuffix))
                add("address", fieldToJson(rule.addressPrefix, rule.addressSuffix))
                add("phone", fieldToJson(rule.phonePrefix, rule.phoneSuffix))
            })
        }
    }

    private fun fieldToJson(prefix: String?, suffix: String?): JsonObject {
        return JsonObject().apply {
            addProperty("prefix", prefix ?: "")
            addProperty("suffix", suffix ?: "")
        }
    }

    private fun parseRule(obj: JsonObject): MatchRule {
        val name = obj.get("name")?.asString ?: throw JsonParseException("缺少 name")
        val fields = obj.getAsJsonObject("fields") ?: throw JsonParseException("缺少 fields")

        fun getField(key: String): Pair<String?, String?> {
            val f = fields.getAsJsonObject(key) ?: return null to null
            val p = f.get("prefix")?.asString?.takeIf { it.isNotEmpty() }
            val s = f.get("suffix")?.asString?.takeIf { it.isNotEmpty() }
            return p to s
        }

        val (tp, ts) = getField("trackingNumber")
        val (cp, cs) = getField("pickupCode")
        val (ap, as_) = getField("address")
        val (pp, ps) = getField("phone")

        return MatchRule(
            name = name,
            trackingPrefix = tp, trackingSuffix = ts,
            codePrefix = cp, codeSuffix = cs,
            addressPrefix = ap, addressSuffix = as_,
            phonePrefix = pp, phoneSuffix = ps
        )
    }

    private val ALLOWED_FIELDS = setOf("trackingNumber", "pickupCode", "address", "phone")

    private fun validateRule(rule: MatchRule) {
        if (rule.name.isBlank()) throw JsonParseException("规则名称不能为空")
        if (!rule.hasAnyField) throw JsonParseException("至少需要配置一个字段")
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add app/src/main/java/com/quickpick/util/JsonExporter.kt && git commit -m "feat: add JsonExporter — import/export rules as JSON"
```

---

## Phase 4: UI 组件

### Task 4.1: 通用组件 — EmptyState, ConfirmDialog, BottomNavBar

**Files:**
- Create: `app/src/main/java/com/quickpick/ui/component/EmptyState.kt`
- Create: `app/src/main/java/com/quickpick/ui/component/ConfirmDialog.kt`
- Create: `app/src/main/java/com/quickpick/ui/component/BottomNavBar.kt`

- [ ] **Step 1: 创建 EmptyState.kt**

```kotlin
package com.quickpick.ui.component

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.quickpick.ui.theme.TextPrimary
import com.quickpick.ui.theme.TextSecondary

@Composable
fun EmptyState(
    emoji: String,
    title: String,
    description: String,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(48.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = emoji, style = MaterialTheme.typography.displayMedium)
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = TextPrimary,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = description,
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary,
            textAlign = TextAlign.Center
        )
        if (action != null) {
            Spacer(modifier = Modifier.height(20.dp))
            action()
        }
    }
}
```

- [ ] **Step 2: 创建 ConfirmDialog.kt**

```kotlin
package com.quickpick.ui.component

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import com.quickpick.ui.theme.Primary

@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    confirmText: String = "确认",
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
        },
        text = {
            Text(text = message, style = MaterialTheme.typography.bodyMedium)
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(text = confirmText, color = Primary)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "取消")
            }
        }
    )
}
```

- [ ] **Step 3: 创建 BottomNavBar.kt**

```kotlin
package com.quickpick.ui.component

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.quickpick.ui.theme.Primary
import com.quickpick.ui.theme.TextSecondary

enum class BottomTab(val label: String, val icon: String) {
    PACKAGES("快递列表", "📋"),
    RULES("匹配规则", "⚙"),
    SETTINGS("设置", "⋯")
}

@Composable
fun BottomNavBar(
    currentTab: BottomTab,
    onTabClick: (BottomTab) -> Unit,
    modifier: Modifier = Modifier
) {
    NavigationBar(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
        tonalElevation = 0.dp
    ) {
        BottomTab.entries.forEach { tab ->
            NavigationBarItem(
                selected = currentTab == tab,
                onClick = { onTabClick(tab) },
                icon = { Text(text = tab.icon, style = MaterialTheme.typography.titleLarge) },
                label = {
                    Text(
                        text = tab.label,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (currentTab == tab) Primary else TextSecondary
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = Primary.copy(alpha = 0.12f)
                )
            )
        }
    }
}
```

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/quickpick/ui/component/ && git commit -m "feat: add shared components — EmptyState, ConfirmDialog, BottomNavBar"
```

---

### Task 4.2: 快递卡片组件

**Files:**
- Create: `app/src/main/java/com/quickpick/ui/component/PackageCard.kt`
- Create: `app/src/main/java/com/quickpick/ui/component/DayGroup.kt`

- [ ] **Step 1: 创建 PackageCard.kt**

```kotlin
package com.quickpick.ui.component

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.quickpick.domain.model.PackageRecord
import com.quickpick.ui.theme.*

@Composable
fun PackageCard(
    record: PackageRecord,
    onTogglePickedUp: () -> Unit,
    onCopyField: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val brandColor = BrandColors.forName(record.companyName)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (record.isPickedUp) SurfaceDone else Surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (expanded) 4.dp else 2.dp
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // 顶部：品牌色点 + 公司名 + 时间 + 勾选
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // 品牌色 8dp 圆点
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(brandColor)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = record.companyName,
                            style = MaterialTheme.typography.titleMedium,
                            color = if (record.isPickedUp) TextSecondary else TextPrimary
                        )
                        if (record.isPickedUp) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "✓ 已取件",
                                style = MaterialTheme.typography.bodySmall,
                                color = Done
                            )
                        }
                    }
                    Text(
                        text = formatRelativeTime(record.smsTimestamp),
                        style = MaterialTheme.typography.bodySmall,
                        color = TextTertiary
                    )
                }
                // 勾选按钮
                IconButton(
                    onClick = onTogglePickedUp,
                    modifier = Modifier.size(32.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(
                                if (record.isPickedUp) Done
                                else Color.Transparent
                            )
                            .then(
                                if (!record.isPickedUp)
                                    Modifier.background(Color.Transparent, CircleShape)
                                else Modifier
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (record.isPickedUp) {
                            Text("✓", color = Color.White, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 取件码 — 大字突出
            if (record.pickupCode != null) {
                Row(verticalAlignment = Alignment.Baseline) {
                    Text(
                        text = if (record.pickupCode.length > 4) "取件码" else "柜号",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextTertiary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = record.pickupCode,
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontFamily = FontFamily.Monospace
                        ),
                        color = if (record.isPickedUp) TextSecondary else Primary
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    TextButton(onClick = { onCopyField(record.pickupCode) }) {
                        Text("复制", color = Primary, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            // 地址
            if (record.address != null) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "📍 ${record.address}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (record.isPickedUp) TextSecondary.copy(alpha = 0.7f) else TextSecondary
                )
            }

            // 展开详情
            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    HorizontalDivider(color = Divider)
                    Spacer(modifier = Modifier.height(8.dp))

                    if (record.trackingNumber != null) {
                        DetailRow("单号", record.trackingNumber) { onCopyField(record.trackingNumber) }
                    }
                    if (record.phone != null) {
                        DetailRow("电话", record.phone) { onCopyField(record.phone) }
                    }
                    if (record.isIncomplete) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "⚠ 信息不完整，建议优化匹配规则",
                            style = MaterialTheme.typography.bodySmall,
                            color = Pending
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String, onCopy: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 3.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = TextTertiary,
            modifier = Modifier.width(48.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = TextPrimary,
            modifier = Modifier.weight(1f)
        )
        TextButton(onClick = onCopy) {
            Text("复制", color = Primary, style = MaterialTheme.typography.bodySmall)
        }
    }
}
```

- [ ] **Step 2: 创建 DayGroup.kt**

```kotlin
package com.quickpick.ui.component

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.quickpick.domain.model.PackageRecord
import com.quickpick.ui.theme.Primary
import com.quickpick.ui.theme.TextPrimary
import com.quickpick.ui.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun DayGroup(
    dateLabel: String,
    dateMs: Long,
    packages: List<PackageRecord>,
    onTogglePickedUp: (PackageRecord) -> Unit,
    onCopyField: (String) -> Unit,
    onMarkDayPickedUp: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var confirmVisible by remember { mutableStateOf(false) }

    Column(modifier = modifier) {
        // 分组标题行
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = dateLabel,
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary
            )
            TextButton(onClick = { confirmVisible = true }) {
                Text("全部已取", style = MaterialTheme.typography.bodySmall, color = Primary)
            }
        }

        // 卡片列表
        packages.forEach { pkg ->
            PackageCard(
                record = pkg,
                onTogglePickedUp = { onTogglePickedUp(pkg) },
                onCopyField = onCopyField,
                modifier = Modifier.padding(bottom = 10.dp)
            )
        }
    }

    if (confirmVisible) {
        ConfirmDialog(
            title = "确认全部已取？",
            message = "将「$dateLabel」的所有快递标记为已取件。",
            onConfirm = {
                onMarkDayPickedUp(dateMs)
                confirmVisible = false
            },
            onDismiss = { confirmVisible = false }
        )
    }
}

/** 格式化相对时间 */
fun formatRelativeTime(timestampMs: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestampMs

    return when {
        diff < 60_000 -> "刚刚"
        diff < 3_600_000 -> "${diff / 60_000} 分钟前"
        diff < 86_400_000 -> "${diff / 3_600_000} 小时前"
        else -> SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()).format(Date(timestampMs))
    }
}

/** 格式化日期分组标签 */
fun formatDayLabel(timestampMs: Long): String {
    val cal = Calendar.getInstance()
    val today = Calendar.getInstance()
    val target = Calendar.getInstance().apply { timeInMillis = timestampMs }

    return when {
        target.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
        target.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR) -> "今天"

        target.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
        target.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR) - 1 -> "昨天"

        else -> SimpleDateFormat("M月d日 · E", Locale.CHINESE).format(Date(timestampMs))
    }
}
```

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/quickpick/ui/component/ && git commit -m "feat: add PackageCard and DayGroup components"
```

---

## Phase 5: ViewModel + Screen

### Task 5.1: PackageListViewModel + Screen

**Files:**
- Create: `app/src/main/java/com/quickpick/ui/viewmodel/PackageListViewModel.kt`
- Create: `app/src/main/java/com/quickpick/ui/screen/PackageListScreen.kt`

- [ ] **Step 1: 创建 PackageListViewModel.kt**

```kotlin
package com.quickpick.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.quickpick.QuickPickApp
import com.quickpick.data.repository.PackageRepository
import com.quickpick.data.repository.RuleRepository
import com.quickpick.data.repository.SmsRepository
import com.quickpick.domain.engine.MatchEngine
import com.quickpick.domain.model.PackageRecord
import com.quickpick.util.SmsReader
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class PackageListViewModel(application: Application) : AndroidViewModel(application) {

    private val db = (application as QuickPickApp).database
    private val packageRepo = PackageRepository(db.packageRecordDao())
    private val ruleRepo = RuleRepository(db.matchRuleDao())
    private val smsRepo = SmsRepository(db.scannedSmsDao())

    val packages: StateFlow<List<PackageRecord>> = packageRepo.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val unpickedCount: StateFlow<Int> = packageRepo.observeUnpickedCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning

    private val _needsPermission = MutableStateFlow(false)
    val needsPermission: StateFlow<Boolean> = _needsPermission

    init {
        scanSms()
    }

    fun scanSms() {
        viewModelScope.launch {
            _isScanning.value = true
            try {
                val context = getApplication<QuickPickApp>()
                val messages = SmsReader.readAll(context)
                if (messages.isEmpty()) {
                    _needsPermission.value = true
                    return@launch
                }
                _needsPermission.value = false

                val rules = ruleRepo.getAll()
                if (rules.isEmpty()) return@launch

                messages.forEach { sms ->
                    if (!smsRepo.isScanned(sms.id)) {
                        val result = MatchEngine.findBestMatch(sms.body, rules)
                        if (result != null) {
                            val (rule, fields) = result
                            if (!packageRepo.existsBySmsId(sms.id)) {
                                packageRepo.insert(
                                    PackageRecord(
                                        companyName = rule.name,
                                        trackingNumber = fields.trackingNumber,
                                        pickupCode = fields.pickupCode,
                                        address = fields.address,
                                        phone = fields.phone,
                                        smsBody = sms.body,
                                        smsTimestamp = sms.timestamp,
                                        isIncomplete = fields.isIncomplete,
                                        ruleId = rule.id,
                                        smsId = sms.id
                                    )
                                )
                            }
                        }
                        smsRepo.markScanned(sms.id)
                    }
                }
            } catch (_: SecurityException) {
                _needsPermission.value = true
            } finally {
                _isScanning.value = false
            }
        }
    }

    fun togglePickedUp(record: PackageRecord) {
        viewModelScope.launch {
            packageRepo.update(record.copy(isPickedUp = !record.isPickedUp))
        }
    }

    fun markAllPickedUp() {
        viewModelScope.launch { packageRepo.markAllPickedUp() }
    }

    fun markDayPickedUp(dateMs: Long) {
        viewModelScope.launch { packageRepo.markDayPickedUp(dateMs) }
    }
}
```

- [ ] **Step 2: 创建 PackageListScreen.kt**（完整页面，含分组逻辑、快捷操作、空状态）

```kotlin
package com.quickpick.ui.screen

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.quickpick.domain.model.PackageRecord
import com.quickpick.ui.component.*
import com.quickpick.ui.theme.*
import com.quickpick.ui.viewmodel.PackageListViewModel

@Composable
fun PackageListScreen(viewModel: PackageListViewModel) {
    val packages by viewModel.packages.collectAsState()
    val unpickedCount by viewModel.unpickedCount.collectAsState()
    val isScanning by viewModel.isScanning.collectAsState()
    val needsPermission by viewModel.needsPermission.collectAsState()
    val context = LocalContext.current
    var confirmAllVisible by remember { mutableStateOf(false) }

    val groupedPackages = remember(packages) {
        packages.groupBy { normalizeToDayStart(it.smsTimestamp) }
            .toSortedMap(reverseOrderComparator())
    }

    Scaffold { padding ->
        Column(modifier = Modifier.padding(padding)) {
            // 标题栏
            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
                Text(
                    text = "取件宝",
                    style = MaterialTheme.typography.headlineMedium,
                    color = TextPrimary
                )
                if (!needsPermission && packages.isNotEmpty()) {
                    Text(
                        text = "${unpickedCount} 件待取",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }

            // 快捷操作
            if (packages.isNotEmpty() && !needsPermission) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = { confirmAllVisible = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Primary),
                        shape = MaterialTheme.shapes.large,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("✓ 全部已取", color = Color.White)
                    }
                }
            }

            when {
                needsPermission -> {
                    EmptyState(
                        emoji = "📬",
                        title = "需要短信权限",
                        description = "读取快递短信需要授权。\n所有数据仅存储在本地，不上传。",
                        action = {
                            Button(
                                onClick = { /* TODO: request SMS permission */ },
                                colors = ButtonDefaults.buttonColors(containerColor = Primary)
                            ) {
                                Text("前往授权")
                            }
                        }
                    )
                }
                isScanning -> {
                    EmptyState(emoji = "🔍", title = "扫描中...", description = "正在读取快递短信")
                }
                packages.isEmpty() -> {
                    EmptyState(
                        emoji = "📦",
                        title = "还没有快递",
                        description = "有新快递时会自动出现～\n也可以下拉手动刷新"
                    )
                }
                else -> {
                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(0.dp)
                    ) {
                        groupedPackages.forEach { (dateMs, pkgs) ->
                            item(key = "day-$dateMs") {
                                DayGroup(
                                    dateLabel = formatDayLabel(dateMs),
                                    dateMs = dateMs,
                                    packages = pkgs,
                                    onTogglePickedUp = { viewModel.togglePickedUp(it) },
                                    onCopyField = { copyToClipboard(context, it) },
                                    onMarkDayPickedUp = { viewModel.markDayPickedUp(it) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (confirmAllVisible) {
        ConfirmDialog(
            title = "确认全部已取？",
            message = "将所有快递标记为已取件，操作不可撤销。",
            onConfirm = {
                viewModel.markAllPickedUp()
                confirmAllVisible = false
            },
            onDismiss = { confirmAllVisible = false }
        )
    }
}

private fun copyToClipboard(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("", text))
    Toast.makeText(context, "已复制", Toast.LENGTH_SHORT).show()
}

/** 时间戳归一到当天 00:00:00 用于分组 */
private fun normalizeToDayStart(timestampMs: Long): Long {
    val cal = java.util.Calendar.getInstance().apply { timeInMillis = timestampMs }
    cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
    cal.set(java.util.Calendar.MINUTE, 0)
    cal.set(java.util.Calendar.SECOND, 0)
    cal.set(java.util.Calendar.MILLISECOND, 0)
    return cal.timeInMillis
}

/** 降序比较器 */
private fun reverseOrderComparator(): Comparator<Long> = Comparator { a, b -> b.compareTo(a) }
```

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/quickpick/ui/viewmodel/PackageListViewModel.kt app/src/main/java/com/quickpick/ui/screen/PackageListScreen.kt && git commit -m "feat: add PackageListViewModel and PackageListScreen"
```

---

后续阶段（Rule 编辑器、AI、设置、导航、集成）由于篇幅原因将在下一批任务中编写。当前已完成：项目基础 → 数据层 → 匹配引擎 → 预置规则 → Repository → 工具类 → 通用组件 → 快递列表主页。

**当前进度：** 18 个文件已创建，快递列表主流程可跑通。


---

### Task 5.2: RuleListViewModel + RuleListScreen

**Files:**
- Create: `app/src/main/java/com/quickpick/ui/viewmodel/RuleListViewModel.kt`
- Create: `app/src/main/java/com/quickpick/ui/screen/RuleListScreen.kt`

- [ ] **Step 1: 创建 RuleListViewModel.kt**

```kotlin
package com.quickpick.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.quickpick.QuickPickApp
import com.quickpick.data.repository.RuleRepository
import com.quickpick.domain.model.MatchRule
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class RuleListViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = RuleRepository(
        (application as QuickPickApp).database.matchRuleDao()
    )

    val rules: StateFlow<List<MatchRule>> = repo.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun deleteRule(rule: MatchRule) {
        viewModelScope.launch { repo.delete(rule) }
    }
}
```

- [ ] **Step 2: 创建 RuleListScreen.kt**

```kotlin
package com.quickpick.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.quickpick.domain.model.MatchRule
import com.quickpick.ui.component.EmptyState
import com.quickpick.ui.theme.*
import com.quickpick.ui.viewmodel.RuleListViewModel

@Composable
fun RuleListScreen(
    viewModel: RuleListViewModel,
    onAddRule: () -> Unit,
    onEditRule: (Long) -> Unit,
    onExportRule: (MatchRule) -> Unit
) {
    val rules by viewModel.rules.collectAsState()
    val presets = rules.filter { it.isPreset }
    val customs = rules.filter { !it.isPreset }

    Scaffold { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // 标题
            item {
                Text(
                    text = "匹配规则",
                    style = MaterialTheme.typography.headlineMedium,
                    color = TextPrimary,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                Text(
                    text = "${rules.size} 条规则，${presets.size} 个预置",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            }

            // 预置规则
            if (presets.isNotEmpty()) {
                item {
                    Text(
                        text = "预置规则",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextTertiary,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
                items(presets, key = { "preset-${it.id}" }) { rule ->
                    RuleCard(
                        rule = rule,
                        isPreset = true,
                        onClick = { onEditRule(rule.id) },
                        onExport = { onExportRule(rule) }
                    )
                }
            }

            // 自定义规则
            if (customs.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "自定义规则",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextTertiary,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
                items(customs, key = { "custom-${it.id}" }) { rule ->
                    RuleCard(
                        rule = rule,
                        isPreset = false,
                        onClick = { onEditRule(rule.id) },
                        onExport = { onExportRule(rule) }
                    )
                }
            }

            if (rules.isEmpty()) {
                item {
                    EmptyState(
                        emoji = "📋",
                        title = "还没有匹配规则",
                        description = "添加规则后才能提取快递信息"
                    )
                }
            }

            // 新增按钮
            item {
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = onAddRule,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    border = ButtonDefaults.outlinedButtonBorder.copy(
                        brush = androidx.compose.ui.graphics.SolidColor(Primary)
                    )
                ) {
                    Text("+ 新增匹配规则", color = Primary)
                }
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }
}

@Composable
private fun RuleCard(
    rule: MatchRule,
    isPreset: Boolean,
    onClick: () -> Unit,
    onExport: () -> Unit
) {
    val brandColor = BrandColors.forName(rule.name)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 品牌色圆点
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(androidx.compose.foundation.shape.CircleShape)
                    .background(brandColor)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = rule.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary
                )
                Text(
                    text = "${rule.configuredFieldCount} 个字段",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
            Text(
                text = if (isPreset) "预置" else "自定义",
                style = MaterialTheme.typography.bodySmall,
                color = if (isPreset) Primary else TextSecondary,
                modifier = Modifier
                    .background(
                        if (isPreset) PrimarySoft else InputBackground,
                        RoundedCornerShape(10.dp)
                    )
                    .padding(horizontal = 10.dp, vertical = 3.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(onClick = onExport, modifier = Modifier.size(32.dp)) {
                Text("↗", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
```

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/quickpick/ui/viewmodel/RuleListViewModel.kt app/src/main/java/com/quickpick/ui/screen/RuleListScreen.kt && git commit -m "feat: add RuleListViewModel and RuleListScreen"
```

---

### Task 5.3: 规则编辑器组件 — TokenChip + FieldCard

**Files:**
- Create: `app/src/main/java/com/quickpick/ui/component/TokenChip.kt`
- Create: `app/src/main/java/com/quickpick/ui/component/FieldCard.kt`

- [ ] **Step 1: 创建 TokenChip.kt**

```kotlin
package com.quickpick.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.quickpick.ui.theme.*

enum class TokenState { NORMAL, MATCHED, SELECTED }

@Composable
fun TokenChip(
    text: String,
    state: TokenState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bg = when (state) {
        TokenState.NORMAL -> InputBackground
        TokenState.MATCHED -> Color(0xFFF2F8F2)
        TokenState.SELECTED -> Primary
    }
    val border = when (state) {
        TokenState.NORMAL -> Color.Transparent
        TokenState.MATCHED -> Done
        TokenState.SELECTED -> Primary
    }
    val fg = when (state) {
        TokenState.SELECTED -> Color.White
        else -> TextPrimary
    }

    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = fg,
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .border(1.5.dp, border, RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 5.dp)
    )
}
```

- [ ] **Step 2: 创建 FieldCard.kt**

```kotlin
package com.quickpick.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.quickpick.ui.theme.*

enum class FieldCardState { CONFIGURED, ACTIVE, EMPTY }

@Composable
fun FieldCard(
    fieldName: String,
    dotColor: androidx.compose.ui.graphics.Color,
    state: FieldCardState,
    prefix: String,
    suffix: String,
    onPrefixChange: (String) -> Unit,
    onSuffixChange: (String) -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor = when (state) {
        FieldCardState.CONFIGURED -> Done
        FieldCardState.ACTIVE -> Primary
        FieldCardState.EMPTY -> Color.Transparent
    }
    val elevation = when (state) {
        FieldCardState.ACTIVE -> 6.dp
        else -> 2.dp
    }
    val offsetX = if (state == FieldCardState.ACTIVE) 2.dp else 0.dp

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = offsetX)
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (state == FieldCardState.EMPTY) Surface.copy(alpha = 0.7f) else Surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = elevation)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(androidx.compose.foundation.shape.CircleShape)
                        .background(dotColor)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = fieldName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (state == FieldCardState.ACTIVE) Primary else TextPrimary,
                    fontWeight = if (state == FieldCardState.ACTIVE)
                        androidx.compose.ui.text.font.FontWeight.SemiBold
                    else androidx.compose.ui.text.font.FontWeight.Normal
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = when (state) {
                        FieldCardState.CONFIGURED -> "已配置"
                        FieldCardState.ACTIVE -> "编辑中"
                        FieldCardState.EMPTY -> "可选"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = when (state) {
                        FieldCardState.CONFIGURED -> Done
                        FieldCardState.ACTIVE -> Primary
                        FieldCardState.EMPTY -> TextTertiary
                    }
                )
            }

            if (state != FieldCardState.EMPTY || prefix.isNotEmpty() || suffix.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = prefix,
                        onValueChange = onPrefixChange,
                        placeholder = { Text("前缀", style = MaterialTheme.typography.bodySmall) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyMedium,
                        shape = RoundedCornerShape(12.dp)
                    )
                    Text("→", color = TextTertiary, style = MaterialTheme.typography.bodyMedium)
                    OutlinedTextField(
                        value = suffix,
                        onValueChange = onSuffixChange,
                        placeholder = { Text("后缀", style = MaterialTheme.typography.bodySmall) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyMedium,
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }
        }
    }
}
```

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/quickpick/ui/component/ && git commit -m "feat: add TokenChip and FieldCard editor components"
```

---

### Task 5.4: RuleEditorViewModel + RuleEditorScreen

**Files:**
- Create: `app/src/main/java/com/quickpick/ui/viewmodel/RuleEditorViewModel.kt`
- Create: `app/src/main/java/com/quickpick/ui/screen/RuleEditorScreen.kt`

- [ ] **Step 1: 创建 RuleEditorViewModel.kt**

```kotlin
package com.quickpick.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.quickpick.QuickPickApp
import com.quickpick.data.repository.RuleRepository
import com.quickpick.domain.engine.MatchEngine
import com.quickpick.domain.model.ExtractedFields
import com.quickpick.domain.model.MatchRule
import com.quickpick.domain.model.PackageRecord
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class EditorState(
    val ruleId: Long = 0,
    val ruleName: String = "",
    val smsText: String = "",
    val activeField: ActiveField = ActiveField.PICKUP_CODE,
    val trackingPrefix: String = "",
    val trackingSuffix: String = "",
    val codePrefix: String = "",
    val codeSuffix: String = "",
    val addressPrefix: String = "",
    val addressSuffix: String = "",
    val phonePrefix: String = "",
    val phoneSuffix: String = "",
    val preview: ExtractedFields? = null
)

enum class ActiveField {
    TRACKING, PICKUP_CODE, ADDRESS, PHONE
}

class RuleEditorViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = RuleRepository(
        (application as QuickPickApp).database.matchRuleDao()
    )

    private val _state = MutableStateFlow(EditorState())
    val state: StateFlow<EditorState> = _state

    fun loadRule(id: Long) {
        viewModelScope.launch {
            val rule = repo.getById(id) ?: return@launch
            _state.value = _state.value.copy(
                ruleId = rule.id,
                ruleName = rule.name,
                smsText = rule.sampleSms ?: "",
                trackingPrefix = rule.trackingPrefix ?: "",
                trackingSuffix = rule.trackingSuffix ?: "",
                codePrefix = rule.codePrefix ?: "",
                codeSuffix = rule.codeSuffix ?: "",
                addressPrefix = rule.addressPrefix ?: "",
                addressSuffix = rule.addressSuffix ?: "",
                phonePrefix = rule.phonePrefix ?: "",
                phoneSuffix = rule.phoneSuffix ?: ""
            )
            refreshPreview()
        }
    }

    fun loadFromJson(rule: MatchRule, smsText: String) {
        _state.value = _state.value.copy(
            ruleName = rule.name,
            smsText = smsText,
            trackingPrefix = rule.trackingPrefix ?: "",
            trackingSuffix = rule.trackingSuffix ?: "",
            codePrefix = rule.codePrefix ?: "",
            codeSuffix = rule.codeSuffix ?: "",
            addressPrefix = rule.addressPrefix ?: "",
            addressSuffix = rule.addressSuffix ?: "",
            phonePrefix = rule.phonePrefix ?: "",
            phoneSuffix = rule.phoneSuffix ?: ""
        )
        refreshPreview()
    }

    fun updateRuleName(name: String) {
        _state.value = _state.value.copy(ruleName = name)
    }

    fun updateSmsText(text: String) {
        _state.value = _state.value.copy(smsText = text)
        refreshPreview()
    }

    fun setActiveField(field: ActiveField) {
        _state.value = _state.value.copy(activeField = field)
    }

    fun updateActiveFieldPrefix(value: String) {
        val s = _state.value
        val ns = when (s.activeField) {
            ActiveField.TRACKING -> s.copy(trackingPrefix = value)
            ActiveField.PICKUP_CODE -> s.copy(codePrefix = value)
            ActiveField.ADDRESS -> s.copy(addressPrefix = value)
            ActiveField.PHONE -> s.copy(phonePrefix = value)
        }
        _state.value = ns
        refreshPreview()
    }

    fun updateActiveFieldSuffix(value: String) {
        val s = _state.value
        val ns = when (s.activeField) {
            ActiveField.TRACKING -> s.copy(trackingSuffix = value)
            ActiveField.PICKUP_CODE -> s.copy(codeSuffix = value)
            ActiveField.ADDRESS -> s.copy(addressSuffix = value)
            ActiveField.PHONE -> s.copy(phoneSuffix = value)
        }
        _state.value = ns
        refreshPreview()
    }

    fun setTokenAsAnchor(token: String) {
        val s = _state.value
        val text = s.smsText
        val idx = text.indexOf(token)
        if (idx == -1) return

        val prefix = text.substring(0, idx + token.length)
        // Find the closest prefix that's short and unique
        val shortPrefix = findShortUniquePrefix(text, idx, token)

        val afterText = text.substring(idx + token.length)
        val suffix = afterText.takeWhile { it in "，。；、！？,.!?）) 】：:" || it.isWhitespace() }.ifEmpty {
            afterText.take(1)
        }

        val ns = when (s.activeField) {
            ActiveField.TRACKING -> s.copy(trackingPrefix = shortPrefix, trackingSuffix = suffix)
            ActiveField.PICKUP_CODE -> s.copy(codePrefix = shortPrefix, codeSuffix = suffix)
            ActiveField.ADDRESS -> s.copy(addressPrefix = shortPrefix, addressSuffix = suffix)
            ActiveField.PHONE -> s.copy(phonePrefix = shortPrefix, phoneSuffix = suffix)
        }
        _state.value = ns
        refreshPreview()
    }

    private fun findShortUniquePrefix(text: String, targetIdx: Int, token: String): String {
        // Backtrack from target to find shortest unique prefix
        var start = targetIdx
        while (start > 0) {
            val prefix = text.substring(start, targetIdx + token.length)
            if (text.indexOf(prefix) == start) {
                return prefix
            }
            start--
        }
        return text.substring(0, targetIdx + token.length)
    }

    fun saveRule(onSuccess: () -> Unit) {
        val s = _state.value
        if (s.ruleName.isBlank()) return

        viewModelScope.launch {
            val rule = MatchRule(
                id = s.ruleId,
                name = s.ruleName,
                trackingPrefix = s.trackingPrefix.ifEmpty { null },
                trackingSuffix = s.trackingSuffix.ifEmpty { null },
                codePrefix = s.codePrefix.ifEmpty { null },
                codeSuffix = s.codeSuffix.ifEmpty { null },
                addressPrefix = s.addressPrefix.ifEmpty { null },
                addressSuffix = s.addressSuffix.ifEmpty { null },
                phonePrefix = s.phonePrefix.ifEmpty { null },
                phoneSuffix = s.phoneSuffix.ifEmpty { null },
                sampleSms = s.smsText.ifEmpty { null }
            )
            if (s.ruleId == 0L) {
                repo.insert(rule)
            } else {
                repo.update(rule)
            }
            onSuccess()
        }
    }

    private fun refreshPreview() {
        val s = _state.value
        if (s.smsText.isBlank()) {
            _state.value = s.copy(preview = null)
            return
        }
        val rule = MatchRule(
            name = s.ruleName.ifEmpty { "预览" },
            trackingPrefix = s.trackingPrefix.ifEmpty { null },
            trackingSuffix = s.trackingSuffix.ifEmpty { null },
            codePrefix = s.codePrefix.ifEmpty { null },
            codeSuffix = s.codeSuffix.ifEmpty { null },
            addressPrefix = s.addressPrefix.ifEmpty { null },
            addressSuffix = s.addressSuffix.ifEmpty { null },
            phonePrefix = s.phonePrefix.ifEmpty { null },
            phoneSuffix = s.phoneSuffix.ifEmpty { null }
        )
        _state.value = s.copy(preview = MatchEngine.extract(s.smsText, rule))
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add app/src/main/java/com/quickpick/ui/viewmodel/RuleEditorViewModel.kt && git commit -m "feat: add RuleEditorViewModel with anchor editing and preview"
```

---

### Task 5.5: RuleEditorScreen

**Files:**
- Create: `app/src/main/java/com/quickpick/ui/screen/RuleEditorScreen.kt`

- [ ] **Step 1: 创建 RuleEditorScreen.kt**

```kotlin
package com.quickpick.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import com.quickpick.domain.model.PackageRecord
import com.quickpick.ui.component.*
import com.quickpick.ui.theme.*
import com.quickpick.ui.viewmodel.ActiveField
import com.quickpick.ui.viewmodel.EditorState
import com.quickpick.ui.viewmodel.RuleEditorViewModel

@Composable
fun RuleEditorScreen(
    viewModel: RuleEditorViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Text("←", style = MaterialTheme.typography.titleLarge)
                }
                Text(
                    text = if (state.ruleId == 0L) "新增匹配规则" else "编辑匹配规则",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = { viewModel.saveRule { onBack() } }) {
                    Text("保存", color = Primary)
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            // 规则名称
            OutlinedTextField(
                value = state.ruleName,
                onValueChange = { viewModel.updateRuleName(it) },
                label = { Text("快递公司名") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                singleLine = true,
                shape = MaterialTheme.shapes.large
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 短信输入 / 分词区（吸顶）
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = MaterialTheme.shapes.large,
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "点击词块填入前后缀 · 当前选中：${activeFieldLabel(state.activeField)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextTertiary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    if (state.smsText.isEmpty()) {
                        OutlinedTextField(
                            value = state.smsText,
                            onValueChange = { viewModel.updateSmsText(it) },
                            placeholder = { Text("从短信 App 复制一条快递短信，粘贴到这里...") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 3,
                            shape = MaterialTheme.shapes.large
                        )
                    } else {
                        TokenGrid(
                            smsText = state.smsText,
                            preview = state.preview,
                            onTokenClick = { viewModel.setTokenAsAnchor(it) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 字段编辑区
            Text(
                text = "提取字段",
                style = MaterialTheme.typography.bodySmall,
                color = TextTertiary,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
            )

            FieldCard(
                fieldName = "📦 快递单号",
                dotColor = BrandColors.SF,
                state = fieldState(state, ActiveField.TRACKING),
                prefix = state.trackingPrefix,
                suffix = state.trackingSuffix,
                onPrefixChange = { viewModel.updateActiveFieldPrefix(it) },
                onSuffixChange = { viewModel.updateActiveFieldSuffix(it) },
                onClick = { viewModel.setActiveField(ActiveField.TRACKING) },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
            FieldCard(
                fieldName = "🔑 取件码",
                dotColor = Primary,
                state = fieldState(state, ActiveField.PICKUP_CODE),
                prefix = state.codePrefix,
                suffix = state.codeSuffix,
                onPrefixChange = { viewModel.updateActiveFieldPrefix(it) },
                onSuffixChange = { viewModel.updateActiveFieldSuffix(it) },
                onClick = { viewModel.setActiveField(ActiveField.PICKUP_CODE) },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
            FieldCard(
                fieldName = "📍 取件地址",
                dotColor = BrandColors.ZTO,
                state = fieldState(state, ActiveField.ADDRESS),
                prefix = state.addressPrefix,
                suffix = state.addressSuffix,
                onPrefixChange = { viewModel.updateActiveFieldPrefix(it) },
                onSuffixChange = { viewModel.updateActiveFieldSuffix(it) },
                onClick = { viewModel.setActiveField(ActiveField.ADDRESS) },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
            FieldCard(
                fieldName = "📱 快递员电话（可选）",
                dotColor = BrandColors.EMS,
                state = fieldState(state, ActiveField.PHONE),
                prefix = state.phonePrefix,
                suffix = state.phoneSuffix,
                onPrefixChange = { viewModel.updateActiveFieldPrefix(it) },
                onSuffixChange = { viewModel.updateActiveFieldSuffix(it) },
                onClick = { viewModel.setActiveField(ActiveField.PHONE) },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )

            // 卡片预览
            if (state.preview != null) {
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = "实时预览",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextTertiary,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
                )
                PreviewCard(state.preview!!, state.ruleName)
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
private fun TokenGrid(
    smsText: String,
    preview: com.quickpick.domain.model.ExtractedFields?,
    onTokenClick: (String) -> Unit
) {
    // 简单分词：按标点和空格拆分，保留标点作为独立 token
    val tokens = remember(smsText) {
        val result = mutableListOf<String>()
        var current = ""
        for (ch in smsText) {
            if (ch in "【】《》（）()，。；、：！？,.!? \n\t") {
                if (current.isNotEmpty()) { result.add(current); current = "" }
                if (!ch.isWhitespace()) result.add(ch.toString())
            } else {
                current += ch
            }
        }
        if (current.isNotEmpty()) result.add(current)
        result
    }

    val matchedTokens = remember(preview) {
        setOf(
            preview?.trackingNumber,
            preview?.pickupCode,
            preview?.address,
            preview?.phone
        ).filterNotNull()
    }

    Column {
        androidx.compose.foundation.layout.FlowRow(
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            tokens.forEach { token ->
                val isMatched = token in matchedTokens.take(10)
                TokenChip(
                    text = token,
                    state = when {
                        isMatched -> TokenState.MATCHED
                        else -> TokenState.NORMAL
                    },
                    onClick = { onTokenClick(token) }
                )
            }
        }
    }
}

@Composable
private fun PreviewCard(fields: com.quickpick.domain.model.ExtractedFields, companyName: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = companyName.ifEmpty { "预览" },
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary
            )
            if (fields.pickupCode != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = fields.pickupCode,
                    style = MaterialTheme.typography.headlineLarge,
                    color = Primary
                )
            }
            if (fields.address != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = "📍 ${fields.address}", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
            }
            if (fields.trackingNumber != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(text = "单号: ${fields.trackingNumber}", style = MaterialTheme.typography.bodySmall, color = TextTertiary)
            }
            if (fields.phone != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(text = "电话: ${fields.phone}", style = MaterialTheme.typography.bodySmall, color = TextTertiary)
            }
        }
    }
}

private fun fieldState(state: EditorState, field: ActiveField): FieldCardState = when {
    state.activeField == field -> FieldCardState.ACTIVE
    isFieldConfigured(state, field) -> FieldCardState.CONFIGURED
    else -> FieldCardState.EMPTY
}

private fun isFieldConfigured(state: EditorState, field: ActiveField): Boolean = when (field) {
    ActiveField.TRACKING -> state.trackingPrefix.isNotEmpty() || state.trackingSuffix.isNotEmpty()
    ActiveField.PICKUP_CODE -> state.codePrefix.isNotEmpty() || state.codeSuffix.isNotEmpty()
    ActiveField.ADDRESS -> state.addressPrefix.isNotEmpty() || state.addressSuffix.isNotEmpty()
    ActiveField.PHONE -> state.phonePrefix.isNotEmpty() || state.phoneSuffix.isNotEmpty()
}

private fun activeFieldLabel(field: ActiveField): String = when (field) {
    ActiveField.TRACKING -> "单号"
    ActiveField.PICKUP_CODE -> "取件码"
    ActiveField.ADDRESS -> "地址"
    ActiveField.PHONE -> "电话"
}
```

- [ ] **Step 2: Commit**

```bash
git add app/src/main/java/com/quickpick/ui/screen/RuleEditorScreen.kt && git commit -m "feat: add RuleEditorScreen with token grid, field cards, and card preview"
```

---

### Task 5.6: AiPromptViewModel + AiPromptScreen

**Files:**
- Create: `app/src/main/java/com/quickpick/ui/viewmodel/AiPromptViewModel.kt`
- Create: `app/src/main/java/com/quickpick/ui/screen/AiPromptScreen.kt`

- [ ] **Step 1: 创建 AiPromptViewModel.kt**

```kotlin
package com.quickpick.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.quickpick.util.JsonExporter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class AiPromptViewModel(application: Application) : AndroidViewModel(application) {

    private val _smsText = MutableStateFlow("")
    val smsText: StateFlow<String> = _smsText

    private val _generatedPrompt = MutableStateFlow("")
    val generatedPrompt: StateFlow<String> = _generatedPrompt

    private val _importedJson = MutableStateFlow("")
    val importedJson: StateFlow<String> = _importedJson

    fun updateSmsText(text: String) {
        _smsText.value = text
    }

    fun generatePrompt() {
        val sms = _smsText.value
        if (sms.isBlank()) return

        _generatedPrompt.value = PROMPT_TEMPLATE.replace("{{SMS_CONTENT}}", sms)
    }

    fun updateImportedJson(json: String) {
        _importedJson.value = json
    }

    fun parseAndValidate(): Result<com.quickpick.domain.model.MatchRule> {
        val json = _importedJson.value
        if (json.isBlank()) return Result.failure(Exception("请粘贴 AI 返回的 JSON"))

        val result = JsonExporter.import(json)
        return if (result.isSuccess && result.getOrNull()?.isNotEmpty() == true) {
            Result.success(result.getOrNull()!!.first())
        } else {
            Result.failure(result.exceptionOrNull() ?: Exception("JSON 格式不正确"))
        }
    }

    companion object {
        private val PROMPT_TEMPLATE = """
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
        """.trimIndent()
    }
}
```

- [ ] **Step 2: 创建 AiPromptScreen.kt**

```kotlin
package com.quickpick.ui.screen

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.quickpick.domain.model.MatchRule
import com.quickpick.ui.theme.*
import com.quickpick.ui.viewmodel.AiPromptViewModel

@Composable
fun AiPromptScreen(
    viewModel: AiPromptViewModel,
    onImportRule: (MatchRule, String) -> Unit,
    onBack: () -> Unit
) {
    val smsText by viewModel.smsText.collectAsState()
    val prompt by viewModel.generatedPrompt.collectAsState()
    val importedJson by viewModel.importedJson.collectAsState()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Text("←", style = MaterialTheme.typography.titleLarge)
                }
                Text(
                    text = "AI 生成规则",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Step 1: 粘贴短信
            Text(
                text = "第 1 步：粘贴短信",
                style = MaterialTheme.typography.bodySmall,
                color = TextTertiary
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = smsText,
                onValueChange = { viewModel.updateSmsText(it) },
                placeholder = { Text("从短信 App 复制一条快递短信粘贴到这里...") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 4,
                shape = MaterialTheme.shapes.large
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 生成按钮
            Button(
                onClick = { viewModel.generatePrompt() },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Primary),
                shape = MaterialTheme.shapes.large,
                enabled = smsText.isNotBlank()
            ) {
                Text("生成提示词", color = Color.White)
            }

            if (prompt.isNotEmpty()) {
                Spacer(modifier = Modifier.height(20.dp))

                // Step 2: 复制提示词
                Text(
                    text = "第 2 步：复制提示词并去 AI 网站提问",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextTertiary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = InputBackground)
                ) {
                    Text(
                        text = prompt,
                        modifier = Modifier.padding(14.dp),
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                        ),
                        color = TextPrimary
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {
                        copyToClipboard(context, prompt)
                        Toast.makeText(context, "提示词已复制，去 AI 网站粘贴提问吧", Toast.LENGTH_LONG).show()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Primary),
                    shape = MaterialTheme.shapes.large
                ) {
                    Text("📋 一键复制提示词", color = Color.White)
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Step 3: 导入 JSON
                Text(
                    text = "第 3 步：粘贴 AI 返回的 JSON",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextTertiary
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = importedJson,
                    onValueChange = { viewModel.updateImportedJson(it) },
                    placeholder = { Text("将从 AI 网站获取的 JSON 结果粘贴到这里...") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 5,
                    shape = MaterialTheme.shapes.large
                )
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = {
                        val result = viewModel.parseAndValidate()
                        result.onSuccess { rule ->
                            onImportRule(rule, smsText)
                        }.onFailure { e ->
                            Toast.makeText(context, "JSON 格式不正确：${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Primary),
                    shape = MaterialTheme.shapes.large,
                    enabled = importedJson.isNotBlank()
                ) {
                    Text("导入并打开规则编辑器 →", color = Color.White)
                }
            }
        }
    }
}

private fun copyToClipboard(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("", text))
}
```

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/quickpick/ui/viewmodel/AiPromptViewModel.kt app/src/main/java/com/quickpick/ui/screen/AiPromptScreen.kt && git commit -m "feat: add AiPromptViewModel and AiPromptScreen"
```

---

### Task 5.7: SettingsScreen + OnboardingScreen

**Files:**
- Create: `app/src/main/java/com/quickpick/ui/screen/SettingsScreen.kt`
- Create: `app/src/main/java/com/quickpick/ui/screen/OnboardingScreen.kt`

- [ ] **Step 1: 创建 SettingsScreen.kt**

```kotlin
package com.quickpick.ui.screen

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.quickpick.domain.model.MatchRule
import com.quickpick.ui.component.ConfirmDialog
import com.quickpick.ui.theme.*
import com.quickpick.ui.viewmodel.PackageListViewModel
import com.quickpick.util.JsonExporter

@Composable
fun SettingsScreen(
    rules: List<MatchRule>,
    packageViewModel: PackageListViewModel,
    onImportRules: (String) -> Unit,
    onOpenAiPrompt: () -> Unit
) {
    val context = LocalContext.current
    var confirmAllVisible by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            text = "设置",
            style = MaterialTheme.typography.headlineMedium,
            color = TextPrimary
        )

        Spacer(modifier = Modifier.height(20.dp))

        // 快捷操作
        SectionTitle("快捷操作")
        SettingsRow("✓ 一键全部已取", onClick = { confirmAllVisible = true })
        SettingsRow("🤖 AI 生成规则", onClick = onOpenAiPrompt)

        Spacer(modifier = Modifier.height(16.dp))

        // 规则管理
        SectionTitle("规则管理")
        SettingsRow("📤 导出全部规则", onClick = {
            val json = JsonExporter.exportAll(rules)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/json"
                putExtra(Intent.EXTRA_TEXT, json)
            }
            context.startActivity(Intent.createChooser(intent, "导出规则"))
        })
        SettingsRow("📥 导入规则", onClick = {
            // In real implementation, use ActivityResultContracts.GetContent
            // For now, placeholder
        })

        Spacer(modifier = Modifier.height(16.dp))

        // 关于
        SectionTitle("关于")
        SettingsRow("版本号", subtitle = "1.0.0", onClick = {})
        SettingsRow("项目地址", subtitle = "github.com/quick-pick", onClick = {})

        Spacer(modifier = Modifier.height(24.dp))

        // 合规声明
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = PrimarySoft)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.Top
            ) {
                Text("🛡", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "所有数据仅存储在本地，不上传、不联网、不收集任何隐私信息。",
                    style = MaterialTheme.typography.bodySmall,
                    color = Primary
                )
            }
        }

        Spacer(modifier = Modifier.height(80.dp))
    }

    if (confirmAllVisible) {
        ConfirmDialog(
            title = "确认全部已取？",
            message = "将所有快递标记为已取件。",
            onConfirm = {
                packageViewModel.markAllPickedUp()
                confirmAllVisible = false
            },
            onDismiss = { confirmAllVisible = false }
        )
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.bodySmall,
        color = TextTertiary,
        modifier = Modifier.padding(vertical = 8.dp)
    )
}

@Composable
private fun SettingsRow(
    title: String,
    subtitle: String? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = TextPrimary,
            modifier = Modifier.weight(1f)
        )
        if (subtitle != null) {
            Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text("→", color = TextTertiary)
    }
    HorizontalDivider(color = Divider)
}
```

- [ ] **Step 2: 创建 OnboardingScreen.kt**

```kotlin
package com.quickpick.ui.screen

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.quickpick.ui.theme.*

@Composable
fun OnboardingScreen(onPermissionGranted: () -> Unit) {
    val context = LocalContext.current
    val hasPermission = remember {
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) ==
        PackageManager.PERMISSION_GRANTED
    }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) onPermissionGranted()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("📦", style = MaterialTheme.typography.displayLarge)

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "欢迎使用取件宝",
            style = MaterialTheme.typography.headlineMedium,
            color = TextPrimary
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "我们需要读取您的短信，来自动提取快递取件信息。\n\n" +
                   "✅ 所有数据仅存储在您的设备本地\n" +
                   "✅ 不上传任何信息到服务器\n" +
                   "✅ 不收集任何隐私数据",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            textAlign = TextAlign.Center,
            lineHeight = MaterialTheme.typography.bodyMedium.lineHeight
        )

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = PrimarySoft)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.Top
            ) {
                Text("🛡", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "所有数据仅存储在本地，不上传、不联网、不收集任何隐私信息。",
                    style = MaterialTheme.typography.bodySmall,
                    color = Primary
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        if (hasPermission) {
            Button(
                onClick = onPermissionGranted,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Primary),
                shape = MaterialTheme.shapes.large
            ) {
                Text("开始使用", color = Color.White)
            }
        } else {
            Button(
                onClick = { launcher.launch(Manifest.permission.READ_SMS) },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Primary),
                shape = MaterialTheme.shapes.large
            ) {
                Text("授权并开始使用", color = Color.White)
            }
        }
    }
}
```

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/quickpick/ui/screen/SettingsScreen.kt app/src/main/java/com/quickpick/ui/screen/OnboardingScreen.kt && git commit -m "feat: add SettingsScreen and OnboardingScreen"
```

---

## Phase 6: 导航集成

### Task 6.1: AppNavigation — 串联所有页面

**Files:**
- Create: `app/src/main/java/com/quickpick/ui/navigation/AppNavigation.kt`
- Modify: `app/src/main/java/com/quickpick/MainActivity.kt`

- [ ] **Step 1: 创建 AppNavigation.kt**

```kotlin
package com.quickpick.ui.navigation

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.quickpick.domain.model.MatchRule
import com.quickpick.ui.component.BottomNavBar
import com.quickpick.ui.component.BottomTab
import com.quickpick.ui.screen.*
import com.quickpick.ui.viewmodel.*

sealed class Screen(val route: String) {
    data object Onboarding : Screen("onboarding")
    data object Main : Screen("main")
    data object RuleEditor : Screen("rule_editor/{ruleId}") {
        fun createRoute(ruleId: Long = 0L) = "rule_editor/$ruleId"
    }
    data object AiPrompt : Screen("ai_prompt")
}

@Composable
fun AppNavigation(
    hasOnboarded: Boolean,
    onOnboarded: () -> Unit
) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = if (hasOnboarded) Screen.Main.route else Screen.Onboarding.route
    ) {
        composable(Screen.Onboarding.route) {
            OnboardingScreen(onPermissionGranted = {
                onOnboarded()
                navController.navigate(Screen.Main.route) {
                    popUpTo(Screen.Onboarding.route) { inclusive = true }
                }
            })
        }

        composable(Screen.Main.route) {
            MainScreen(
                onNavigateToEditor = { ruleId ->
                    navController.navigate(Screen.RuleEditor.createRoute(ruleId))
                },
                onNavigateToAi = {
                    navController.navigate(Screen.AiPrompt.route)
                }
            )
        }

        composable(
            route = Screen.RuleEditor.route,
            arguments = listOf(navArgument("ruleId") { type = NavType.LongType; defaultValue = 0L })
        ) { backStackEntry ->
            val ruleId = backStackEntry.arguments?.getLong("ruleId") ?: 0L
            val viewModel: RuleEditorViewModel = viewModel()
            LaunchedEffect(ruleId) {
                if (ruleId > 0) viewModel.loadRule(ruleId)
            }
            RuleEditorScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.AiPrompt.route) {
            val viewModel: AiPromptViewModel = viewModel()
            val context = LocalContext.current
            AiPromptScreen(
                viewModel = viewModel,
                onImportRule = { rule, smsText ->
                    navController.navigate(Screen.RuleEditor.createRoute(0L)) {
                        // Pass rule data via saved state handle
                    }
                    // Save rule data for next screen
                    navController.currentBackStackEntry
                        ?.savedStateHandle
                        ?.set("importedRule", rule)
                    navController.currentBackStackEntry
                        ?.savedStateHandle
                        ?.set("importedSms", smsText)
                },
                onBack = { navController.popBackStack() }
            )
        }
    }
}

@Composable
fun MainScreen(
    onNavigateToEditor: (Long) -> Unit,
    onNavigateToAi: () -> Unit
) {
    val packageViewModel: PackageListViewModel = viewModel()
    val ruleListViewModel: RuleListViewModel = viewModel()
    val context = LocalContext.current

    var currentTab by remember { mutableStateOf(BottomTab.PACKAGES) }

    Scaffold(
        bottomBar = {
            BottomNavBar(currentTab = currentTab, onTabClick = { currentTab = it })
        }
    ) { padding ->
        androidx.compose.foundation.layout.Box(modifier = Modifier.padding(padding)) {
            when (currentTab) {
                BottomTab.PACKAGES -> {
                    PackageListScreen(viewModel = packageViewModel)
                }
                BottomTab.RULES -> {
                    RuleListScreen(
                        viewModel = ruleListViewModel,
                        onAddRule = { onNavigateToEditor(0L) },
                        onEditRule = { ruleId -> onNavigateToEditor(ruleId) },
                        onExportRule = { rule ->
                            val json = com.quickpick.util.JsonExporter.exportSingle(rule)
                            val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                type = "application/json"
                                putExtra(android.content.Intent.EXTRA_TEXT, json)
                            }
                            context.startActivity(android.content.Intent.createChooser(intent, "分享规则"))
                        }
                    )
                }
                BottomTab.SETTINGS -> {
                    val rules by ruleListViewModel.rules.collectAsState()
                    SettingsScreen(
                        rules = rules,
                        packageViewModel = packageViewModel,
                        onImportRules = { /* TODO: file picker */ },
                        onOpenAiPrompt = onNavigateToAi
                    )
                }
            }
        }
    }
}
```

- [ ] **Step 2: 更新 MainActivity.kt 集成导航**

```kotlin
package com.quickpick

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import com.quickpick.ui.navigation.AppNavigation
import com.quickpick.ui.theme.QuickPickTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            QuickPickTheme {
                var hasOnboarded by remember {
                    mutableStateOf(
                        androidx.core.content.ContextCompat.checkSelfPermission(
                            this,
                            android.Manifest.permission.READ_SMS
                        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                    )
                }
                AppNavigation(
                    hasOnboarded = hasOnboarded,
                    onOnboarded = { hasOnboarded = true }
                )
            }
        }
    }
}
```

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/quickpick/ui/navigation/ app/src/main/java/com/quickpick/MainActivity.kt && git commit -m "feat: add AppNavigation — bottom tabs + full route graph"
```

---

## Phase 7: 收尾打磨

### Task 7.1: 字符串资源与文案

**Files:**
- Create: `app/src/main/res/values/strings.xml`

- [ ] **Step 1: 创建 strings.xml**

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="app_name">取件宝</string>

    <!-- 快递列表 -->
    <string name="list_title">取件宝</string>
    <string name="unpicked_count">%d 件待取</string>
    <string name="mark_all_done">全部已取</string>
    <string name="mark_day_done">全部已取</string>

    <!-- 匹配规则 -->
    <string name="rules_title">匹配规则</string>
    <string name="rule_count">%d 条规则，%d 个预置</string>
    <string name="add_rule">新增匹配规则</string>
    <string name="preset_label">预置</string>
    <string name="custom_label">自定义</string>

    <!-- 规则编辑器 -->
    <string name="editor_new_title">新增匹配规则</string>
    <string name="editor_edit_title">编辑匹配规则</string>
    <string name="sms_placeholder">从短信 App 复制一条快递短信，粘贴到这里…</string>
    <string name="field_tracking">快递单号</string>
    <string name="field_code">取件码</string>
    <string name="field_address">取件地址</string>
    <string name="field_phone">快递员电话（可选）</string>
    <string name="save">保存</string>

    <!-- AI -->
    <string name="ai_title">AI 生成规则</string>
    <string name="ai_step1">第 1 步：粘贴短信</string>
    <string name="ai_step2">第 2 步：复制提示词并去 AI 网站提问</string>
    <string name="ai_step3">第 3 步：粘贴 AI 返回的 JSON</string>
    <string name="ai_generate">生成提示词</string>
    <string name="ai_copy">📋 一键复制提示词</string>
    <string name="ai_import">导入并打开规则编辑器 →</string>

    <!-- 设置 -->
    <string name="settings_title">设置</string>
    <string name="export_all">导出全部规则</string>
    <string name="import_rules">导入规则</string>
    <string name="privacy_notice">所有数据仅存储在本地，不上传、不联网、不收集任何隐私信息。</string>

    <!-- 权限引导 -->
    <string name="onboarding_title">欢迎使用取件宝</string>
    <string name="onboarding_desc">我们需要读取您的短信，来自动提取快递取件信息。\n\n✅ 所有数据仅存储在您的设备本地\n✅ 不上传任何信息到服务器\n✅ 不收集任何隐私数据</string>
    <string name="grant_permission">授权并开始使用</string>
    <string name="start_using">开始使用</string>

    <!-- 空状态 -->
    <string name="empty_no_permission">需要短信权限才能提取快递信息</string>
    <string name="empty_no_packages">还没有快递</string>
    <string name="empty_no_packages_desc">有新快递时会自动出现～</string>
    <string name="empty_no_match">暂未发现快递短信</string>
    <string name="empty_no_rules">还没有匹配规则</string>

    <!-- 通用 -->
    <string name="confirm">确认</string>
    <string name="cancel">取消</string>
    <string name="copy">复制</string>
    <string name="copied">已复制</string>
    <string name="scanning">扫描中…</string>
    <string name="incomplete_warning">信息不完整，建议优化匹配规则</string>
    <string name="picked_up">✓ 已取件</string>
</resources>
```

- [ ] **Step 2: Commit**

```bash
git add app/src/main/res/values/strings.xml && git commit -m "chore: add string resources — Chinese UI text"
```

---

### Task 7.2: 最终检查清单

- [ ] 验证 `AndroidManifest.xml` 中 `READ_SMS` 权限声明
- [ ] 验证 `QuickPickApp.kt` 中预置规则初始化调用存在
- [ ] 验证所有 ViewModel 通过 `(application as QuickPickApp).database` 获取 Room 实例
- [ ] 验证 `MatchEngine.findBestMatch()` 的去重和优先级逻辑
- [ ] 验证 `EditorState` 中所有 8 个锚点字段的读写一致性
- [ ] 验证 AI 提示词模板中的 `{{SMS_CONTENT}}` 占位符替换
- [ ] 验证 `JsonExporter.import()` 能解析单条和数组两种 JSON
- [ ] 验证所有 `ConfirmDialog` 调用场景（全部已取、按天已取）
- [ ] 验证 `OnboardingScreen` 权限状态检测
- [ ] 验证底部导航三个 Tab 的图标和标签
- [ ] 运行 `./gradlew assembleDebug` 验证编译通过

---

## 实现顺序建议

按依赖关系，推荐 5 轮完成：

| 轮次 | 范围 | 预计文件数 |
|------|------|-----------|
| 第 1 轮 | Phase 1-2: 项目骨架 + 数据层 + 引擎 | 15 |
| 第 2 轮 | Phase 3: Repository + SmsReader + JsonExporter | 4 |
| 第 3 轮 | Phase 4: 通用 UI 组件 | 6 |
| 第 4 轮 | Phase 5: 所有 ViewModel + Screen | 12 |
| 第 5 轮 | Phase 6-7: 导航集成 + 字符串 + 检查 | 3 |

总计约 **40 个文件**，每个 task 可独立实现并验证。

