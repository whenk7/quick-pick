# 取件宝(QuickPick) Jetpack Compose Material 3 完整主题代码
以下是完全符合你设计规范的可直接复制使用的主题代码，包含**颜色系统、形状系统、字体系统、尺寸系统**和完整的主题包装器。所有数值都严格对应之前的设计方案，并且针对Android 11+做了优化。

## 第一步：添加字体依赖
首先在 `app/build.gradle.kts` 的 `dependencies` 块中添加 Google Fonts 依赖，这样可以直接使用圆润的 Roboto Rounded 字体：
```kotlin
dependencies {
    // Material 3 核心库
    implementation("androidx.compose.material3:material3:1.3.0")
    
    // Google Fonts 支持（用于 Roboto Rounded）
    implementation("androidx.compose.ui:ui-text-google-fonts:1.7.0")
}
```

## 第二步：创建 Theme.kt 文件
在你的项目中创建 `ui/theme/Theme.kt` 文件，复制以下完整代码：

```kotlin
package com.yourname.quickpick.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.text.googlefonts.Font
import com.yourname.quickpick.R

// ==============================
// 字体系统 - 圆润无衬线字体
// ==============================
private val googleFontProvider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs
)

private val robotoRounded = FontFamily(
    Font(
        googleFont = GoogleFont("Roboto Rounded"),
        fontProvider = googleFontProvider,
        weight = FontWeight.Normal
    ),
    Font(
        googleFont = GoogleFont("Roboto Rounded"),
        fontProvider = googleFontProvider,
        weight = FontWeight.Medium
    ),
    Font(
        googleFont = GoogleFont("Roboto Rounded"),
        fontProvider = googleFontProvider,
        weight = FontWeight.SemiBold
    ),
    Font(
        googleFont = GoogleFont("Roboto Rounded"),
        fontProvider = googleFontProvider,
        weight = FontWeight.Bold
    )
)

// 完全符合设计规范的文字层级
val QuickPickTypography = Typography(
    // 主标题（如引导页大标题）
    headlineLarge = TextStyle(
        fontFamily = robotoRounded,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 34.sp,
        letterSpacing = 0.sp
    ),
    // 页面标题（如顶部导航栏标题）
    headlineMedium = TextStyle(
        fontFamily = robotoRounded,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    ),
    // 卡片标题（如快递公司名）
    titleLarge = TextStyle(
        fontFamily = robotoRounded,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.sp
    ),
    // 取件码（最醒目的文字）
    titleMedium = TextStyle(
        fontFamily = robotoRounded,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 30.sp,
        letterSpacing = 0.5.sp
    ),
    // 正文（如快递地址、单号）
    bodyLarge = TextStyle(
        fontFamily = robotoRounded,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.25.sp
    ),
    // 次要文字（如接收时间、匹配字段数）
    bodyMedium = TextStyle(
        fontFamily = robotoRounded,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.25.sp
    ),
    // 辅助文字（如提示语、标签）
    bodySmall = TextStyle(
        fontFamily = robotoRounded,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.4.sp
    ),
    // 按钮文字
    labelLarge = TextStyle(
        fontFamily = robotoRounded,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    )
)

// ==============================
// 颜色系统 - 治愈系低饱和配色
// ==============================
// 主色调：低饱和豆绿色 (RGB: 106, 179, 123)
private val PrimaryGreen = Color(0xFF6AB37B)
private val OnPrimaryGreen = Color(0xFFFFFFFF)
private val PrimaryContainerGreen = Color(0xFFE8F5E9)
private val OnPrimaryContainerGreen = Color(0xFF1B5E20)

// 辅助色1：浅橙色（用于待处理状态）
private val SecondaryOrange = Color(0xFFFFB76B)
private val OnSecondaryOrange = Color(0xFFFFFFFF)
private val SecondaryContainerOrange = Color(0xFFFFF3E0)
private val OnSecondaryContainerOrange = Color(0xFFE65100)

// 辅助色2：低饱和红色（仅用于删除操作）
private val TertiaryRed = Color(0xFFEF5350)
private val OnTertiaryRed = Color(0xFFFFFFFF)
private val TertiaryContainerRed = Color(0xFFFFEBEE)
private val OnTertiaryContainerRed = Color(0xFFB71C1C)

// 中性色系统（全部使用深灰系列，避免纯黑纯白）
private val BackgroundLight = Color(0xFFF9F9F6) // 暖调米白色
private val OnBackgroundLight = Color(0xFF424242) // 主文字色
private val SurfaceLight = Color(0xFFFFFFFF) // 卡片背景色
private val OnSurfaceLight = Color(0xFF424242)
private val SurfaceVariantLight = Color(0xFFF5F5F5) // 已取件卡片背景
private val OnSurfaceVariantLight = Color(0xFF757575) // 次要文字色
private val OutlineLight = Color(0xFFBDBDBD) // 边框色

// 深色模式配色（同样保持低饱和柔和调性）
private val BackgroundDark = Color(0xFF1E1E1E) // 低饱和深灰，避免纯黑
private val OnBackgroundDark = Color(0xFFE0E0E0)
private val SurfaceDark = Color(0xFF252525)
private val OnSurfaceDark = Color(0xFFE0E0E0)
private val SurfaceVariantDark = Color(0xFF2D2D2D)
private val OnSurfaceVariantDark = Color(0xFFBDBDBD)
private val OutlineDark = Color(0xFF616161)

// 浅色模式配色方案
private val LightColorScheme = lightColorScheme(
    primary = PrimaryGreen,
    onPrimary = OnPrimaryGreen,
    primaryContainer = PrimaryContainerGreen,
    onPrimaryContainer = OnPrimaryContainerGreen,
    secondary = SecondaryOrange,
    onSecondary = OnSecondaryOrange,
    secondaryContainer = SecondaryContainerOrange,
    onSecondaryContainer = OnSecondaryContainerOrange,
    tertiary = TertiaryRed,
    onTertiary = OnTertiaryRed,
    tertiaryContainer = TertiaryContainerRed,
    onTertiaryContainer = OnTertiaryContainerRed,
    background = BackgroundLight,
    onBackground = OnBackgroundLight,
    surface = SurfaceLight,
    onSurface = OnSurfaceLight,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = OnSurfaceVariantLight,
    outline = OutlineLight,
    surfaceTint = PrimaryGreen
)

// 深色模式配色方案
private val DarkColorScheme = darkColorScheme(
    primary = PrimaryGreen,
    onPrimary = OnPrimaryGreen,
    primaryContainer = PrimaryContainerGreen.copy(alpha = 0.2f),
    onPrimaryContainer = OnPrimaryGreen,
    secondary = SecondaryOrange,
    onSecondary = OnSecondaryOrange,
    secondaryContainer = SecondaryContainerOrange.copy(alpha = 0.2f),
    onSecondaryContainer = OnSecondaryOrange,
    tertiary = TertiaryRed,
    onTertiary = OnTertiaryRed,
    tertiaryContainer = TertiaryContainerRed.copy(alpha = 0.2f),
    onTertiaryContainer = OnTertiaryRed,
    background = BackgroundDark,
    onBackground = OnBackgroundDark,
    surface = SurfaceDark,
    onSurface = OnSurfaceDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = OnSurfaceVariantDark,
    outline = OutlineDark,
    surfaceTint = PrimaryGreen
)

// ==============================
// 形状系统 - 全大圆角设计
// ==============================
val QuickPickShapes = Shapes(
    // 小圆角：分词块、标签
    small = RoundedCornerShape(8.dp),
    // 中圆角：快递卡片、按钮、输入框（最常用）
    medium = RoundedCornerShape(16.dp),
    // 大圆角：底部弹窗、卡片组
    large = RoundedCornerShape(24.dp)
)

// ==============================
// 统一尺寸系统
// ==============================
object Dimens {
    // 内边距
    val cardPadding = 16.dp
    val screenPadding = 16.dp
    val itemSpacing = 12.dp
    val sectionSpacing = 24.dp
    
    // 组件尺寸
    val buttonHeight = 48.dp
    val cardElevation = 2.dp
    val iconSize = 24.dp
    
    // 圆角
    val smallCornerRadius = 8.dp
    val mediumCornerRadius = 16.dp
    val largeCornerRadius = 24.dp
}

// ==============================
// 主题包装器
// ==============================
@Composable
fun QuickPickTheme(
    darkTheme: Boolean = false, // 可根据系统设置动态切换
    dynamicColor: Boolean = false, // 关闭Material 3动态颜色，使用我们的自定义配色
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S -> {
            if (darkTheme) androidx.compose.material3.dynamicDarkColorScheme(androidx.compose.ui.platform.LocalContext.current)
            else androidx.compose.material3.dynamicLightColorScheme(androidx.compose.ui.platform.LocalContext.current)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = QuickPickTypography,
        shapes = QuickPickShapes,
        content = content
    )
}
```

## 第三步：在应用中使用主题
修改你的 `MainActivity.kt` 文件，将整个应用包裹在 `QuickPickTheme` 中：

```kotlin
package com.yourname.quickpick

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.yourname.quickpick.ui.theme.QuickPickTheme
import com.yourname.quickpick.ui.screens.MainScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            QuickPickTheme {
                // 一个使用主题背景色的Surface作为根布局
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen() // 你的主屏幕
                }
            }
        }
    }
}
```

## 第四步：关键组件使用示例
以下是几个核心组件的正确使用方式，确保完全符合设计规范：

### 1. 主按钮（下一步/提交）
```kotlin
Button(
    onClick = { /* 点击事件 */ },
    modifier = Modifier
        .fillMaxWidth()
        .height(Dimens.buttonHeight),
    shape = MaterialTheme.shapes.medium, // 使用16dp大圆角
    elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp) // 去掉默认阴影
) {
    Text(
        text = "授权并开始使用",
        style = MaterialTheme.typography.labelLarge
    )
}
```

### 2. 快递卡片
```kotlin
Card(
    modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 8.dp),
    shape = MaterialTheme.shapes.medium,
    elevation = CardDefaults.cardElevation(defaultElevation = Dimens.cardElevation),
    colors = if (isPickedUp) {
        CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    } else {
        CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    }
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(Dimens.cardPadding)
    ) {
        // 取件码（最醒目）
        Text(
            text = pickupCode,
            style = MaterialTheme.typography.titleMedium,
            color = if (isPickedUp) {
                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            } else {
                MaterialTheme.colorScheme.primary
            },
            textDecoration = if (isPickedUp) TextDecoration.LineThrough else TextDecoration.None
        )
        
        // 快递公司名
        Text(
            text = companyName,
            style = MaterialTheme.typography.titleLarge,
            color = if (isPickedUp) {
                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            } else {
                MaterialTheme.colorScheme.onSurface
            },
            modifier = Modifier.padding(top = 8.dp)
        )
        
        // 其他字段...
    }
}
```

### 3. 分词块（规则编辑器）
```kotlin
Surface(
    modifier = Modifier.padding(4.dp),
    shape = MaterialTheme.shapes.small, // 使用8dp小圆角
    color = if (isSelected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    },
    onClick = { /* 点击事件 */ }
) {
    Text(
        text = word,
        style = MaterialTheme.typography.bodyMedium,
        color = if (isSelected) {
            MaterialTheme.colorScheme.onPrimary
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
    )
}
```

## 重要注意事项
1. **关闭动态颜色**：代码中默认关闭了Material 3的动态颜色功能，这是为了保证我们的治愈系配色在所有设备上保持一致。如果你想支持动态颜色，可以将 `dynamicColor` 参数改为 `true`。

2. **字体加载**：Roboto Rounded字体需要Google Play服务支持。如果在没有Google服务的设备上运行，系统会自动回退到默认的无衬线字体，不会影响功能。

3. **深色模式**：深色模式配色同样遵循了低饱和柔和的原则，避免了纯黑背景带来的压抑感。你可以在设置页添加一个开关，让用户手动切换深色/浅色模式。

4. **点击反馈**：Material 3默认的波纹效果可能会破坏整体的柔和感。建议在所有可点击组件中使用自定义的点击反馈：
   ```kotlin
   Modifier.clickable(
       interactionSource = remember { MutableInteractionSource() },
       indication = null // 去掉默认波纹
   ) {
       // 点击事件
   }
   ```
   然后添加一个缩放动画作为替代，这在之前的设计方案中有提到。