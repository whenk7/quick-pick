# Phase 1：项目基础（Gradle 骨架 + 治愈系主题 + 入口）

> 所属轮次：第 1 轮 ｜ 前置：无，从空目录开始
> 本文档为可执行清单：按 Task 顺序照做即可，每个 Step 含完整代码。

---

## Phase 1: 项目基础

### Task 1.1: 创建 Android 项目

**Files:**
- Create: `build.gradle.kts` (project-level)
- Create: `app/build.gradle.kts`
- Create: `settings.gradle.kts`
- Create: `gradle.properties`
- Create: `app/src/main/AndroidManifest.xml`
- Create: `app/src/main/res/values/themes.xml`
- Create: `app/src/main/res/values/colors.xml`
- Create: `app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml`
- Create: `app/src/main/res/drawable/ic_launcher_foreground.xml`

**版本矩阵（2026-10 核实）**：Kotlin 2.3.20 + AGP 8.13.0 + KSP 2.3.12 + Compose BOM 2026.09.00 + Room 2.8.4 + compileSdk/targetSdk 36 + Gradle 8.14.3 + JDK 17。补丁版本号若 sync 时 IDE 提示不存在，按 IDE 建议微调即可，不影响本计划其他内容。注：KSP 自 KSP2 起用独立版本号（如 `2.3.12`，不再是 `Kotlin版本-补丁` 格式），与 Kotlin 2.3.x/2.4.x 配套。

- [ ] **Step 1: 创建项目级 build.gradle.kts**

```kotlin
// build.gradle.kts
plugins {
    id("com.android.application") version "8.13.0" apply false
    id("org.jetbrains.kotlin.android") version "2.3.20" apply false
    // Kotlin 2.x 起 Compose 编译器独立为插件，与 Kotlin 版本一一对应
    id("org.jetbrains.kotlin.plugin.compose") version "2.3.20" apply false
    id("com.google.devtools.ksp") version "2.3.12" apply false
}
```

- [ ] **Step 2: 创建 app/build.gradle.kts**

```kotlin
// app/build.gradle.kts
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.quickpick"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.quickpick"
        minSdk = 30
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true   // 设置页显示 BuildConfig.VERSION_NAME
    }

    packaging {
        resources.excludes += "META-INF/{AL2.0,LGPL2.1}"
    }
}

// Kotlin 2.x 推荐写法，替代已废弃的 android.kotlinOptions
kotlin {
    jvmToolchain(17)
}

dependencies {
    // Compose BOM 2026.09（Compose 1.12 / M3 1.5：强跳过模式默认开启，FlowRow 已转正）
    val composeBom = platform("androidx.compose:compose-bom:2026.09.00")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    // 注意：不引 material-icons-extended（图标全部用 emoji，见设计系统第九节）

    // Activity & Navigation
    implementation("androidx.activity:activity-compose:1.11.0")
    implementation("androidx.navigation:navigation-compose:2.9.5")

    // Lifecycle（collectAsStateWithLifecycle 需要 runtime-compose）
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.9.4")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.9.4")

    // Room（KSP2）
    implementation("androidx.room:room-runtime:2.8.4")
    implementation("androidx.room:room-ktx:2.8.4")
    ksp("androidx.room:room-compiler:2.8.4")

    // DataStore（首启标记、预置规则灌入标记）
    implementation("androidx.datastore:datastore-preferences:1.1.7")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")

    // Gson（JSON 导入导出）
    implementation("com.google.code.gson:gson:2.13.1")

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

// 正确的 DSL 是 dependencyResolutionManagement（初版误写 dependencyResolution，配置阶段直接失败）
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "QuickPick"
include(":app")
```

- [ ] **Step 4: 创建 gradle.properties**

```properties
# gradle.properties
org.gradle.jvmargs=-Xmx2048m -Dfile.encoding=UTF-8
org.gradle.parallel=true
org.gradle.caching=true
# 必须开启，否则 AGP 检测到 androidx 依赖直接报错
android.useAndroidX=true
android.nonTransitiveRClass=true
kotlin.code.style=official
```

- [ ] **Step 5: 创建 themes.xml 和 colors.xml**

Compose 应用的 XML 主题用平台主题即可（不引 MDC 库）。

```xml
<!-- app/src/main/res/values/themes.xml -->
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <style name="Theme.QuickPick" parent="android:Theme.Material.Light.NoActionBar">
        <item name="android:windowBackground">@color/bg_base</item>
    </style>
</resources>
```

```xml
<!-- app/src/main/res/values/colors.xml -->
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <color name="bg_base">#F9F9F6</color>
    <color name="ic_launcher_background">#6AB37B</color>
</resources>
```

- [ ] **Step 6: 创建 launcher 图标（小取 · 圆润小包裹）**

```xml
<!-- app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml -->
<?xml version="1.0" encoding="utf-8"?>
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@color/ic_launcher_background"/>
    <foreground android:drawable="@drawable/ic_launcher_foreground"/>
</adaptive-icon>
```

```xml
<!-- app/src/main/res/drawable/ic_launcher_foreground.xml -->
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp" android:height="108dp"
    android:viewportWidth="108" android:viewportHeight="108">
    <!-- 圆润小包裹身体 -->
    <path
        android:fillColor="#FFFFFF"
        android:pathData="M43,40 h22 a9,9 0 0 1 9,9 v14 a9,9 0 0 1 -9,9 h-22 a9,9 0 0 1 -9,-9 v-14 a9,9 0 0 1 9,-9 z"/>
    <!-- 顶部胶带 -->
    <path
        android:fillColor="#F9F9F6"
        android:pathData="M51,40 h6 v10 l-3,-3 l-3,3 z"/>
    <!-- 双眼 + 微笑 -->
    <path android:fillColor="#6AB37B" android:pathData="M47,56 m-2,0 a2,2 0 1,1 4,0 a2,2 0 1,1 -4,0"/>
    <path android:fillColor="#6AB37B" android:pathData="M61,56 m-2,0 a2,2 0 1,1 4,0 a2,2 0 1,1 -4,0"/>
    <path
        android:strokeColor="#6AB37B" android:strokeWidth="2"
        android:strokeLineCap="round" android:fillColor="#00000000"
        android:pathData="M49,62 q5,4 10,0"/>
</vector>
```

- [ ] **Step 7: 创建 AndroidManifest.xml**

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <!-- 唯一的权限请求。无任何网络权限，物理层面保证"不联网" -->
    <uses-permission android:name="android.permission.READ_SMS" />

    <application
        android:name=".QuickPickApp"
        android:allowBackup="true"
        android:icon="@mipmap/ic_launcher"
        android:label="取件宝"
        android:supportsRtl="true"
        android:theme="@style/Theme.QuickPick">

        <activity
            android:name=".MainActivity"
            android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>
</manifest>
```

- [ ] **Step 8: 生成 Gradle Wrapper**

计划里不写 wrapper 二进制/脚本内容，二选一：
1. 用 Android Studio 打开项目目录，自动生成对应版本的 wrapper
2. 本机已有 gradle：`gradle wrapper --gradle-version 8.14.3`（AGP 8.13 要求 Gradle ≥ 8.13）

- [ ] **Step 9: Commit**

```bash
git add -A && git commit -m "chore: create Android project — Kotlin 2.3 / AGP 8.13 / Compose BOM 2026.09 / targetSdk 36"
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
val PrimarySoft = Color(0xFFE9F3EB) // 10% 豆绿叠白的等效实体色（容器色必须实体，避免透出底层背景）

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
    small = RoundedCornerShape(12.dp),   // 小组件（分词块单独用 8dp，见 TokenChip）
    medium = RoundedCornerShape(16.dp),  // 卡片
    large = RoundedCornerShape(16.dp),   // 按钮、输入框
    extraLarge = RoundedCornerShape(20.dp) // 弹窗
)
```

- [ ] **Step 4: 创建 Theme.kt**

```kotlin
package com.quickpick.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// 覆盖完整槽位，杜绝 M3 默认紫色（弹窗容器 surfaceContainerHigh 等）泄漏
private val LightColorScheme = lightColorScheme(
    primary = Primary,
    onPrimary = Color.White,
    primaryContainer = PrimarySoft,
    onPrimaryContainer = Primary,
    secondary = Primary,
    onSecondary = Color.White,
    secondaryContainer = PrimarySoft,   // 底部导航选中胶囊
    onSecondaryContainer = Primary,
    tertiary = PrimaryLight,
    onTertiary = TextPrimary,
    background = Background,
    onBackground = TextPrimary,
    surface = Surface,
    onSurface = TextPrimary,
    surfaceVariant = InputBackground,   // 输入框/浅灰底
    onSurfaceVariant = TextSecondary,
    surfaceContainerLowest = Surface,
    surfaceContainerLow = Surface,
    surfaceContainer = Surface,
    surfaceContainerHigh = Surface,     // AlertDialog 默认容器
    surfaceContainerHighest = SurfaceDone,
    error = Danger,                     // 删除操作专用
    onError = Color.White,
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

注意：QuickPickApp 此时**不引用** AppDatabase（Task 2.3 才创建，2.6 给出最终完整版），保证 Phase 1 可独立编译运行。

- [ ] **Step 1: 创建 QuickPickApp.kt（最小版）**

```kotlin
package com.quickpick

import android.app.Application

class QuickPickApp : Application() {
    // Room 数据库在 Task 2.3 接入，预置规则灌入在 Task 2.6 给出最终完整版
}
```

- [ ] **Step 2: 创建 MainActivity.kt**

```kotlin
package com.quickpick

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.quickpick.ui.theme.Background
import com.quickpick.ui.theme.QuickPickTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // targetSdk 36 强制 edge-to-edge，内容 inset 由各 Screen 的 Scaffold 处理
        enableEdgeToEdge()
        setContent {
            QuickPickTheme {
                // 占位页：Phase 6 替换为 AppNavigation
                Surface(modifier = Modifier.fillMaxSize(), color = Background) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("取件宝 📦", style = MaterialTheme.typography.headlineMedium)
                    }
                }
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

