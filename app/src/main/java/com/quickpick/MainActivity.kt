package com.quickpick

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.quickpick.ui.navigation.AppNavigation
import com.quickpick.ui.theme.Background
import com.quickpick.ui.theme.QuickPickTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            // QuickPickTheme 内部从 DataStore 读 colorMode/useMonet/paletteStyle
            // 构建 MiuixTheme 的 ThemeController（异步读取期间首帧短暂默认配色，接受闪烁）
            QuickPickTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = Background) {
                    // 首启判定逻辑在 AppNavigation 内（DataStore）
                    AppNavigation()
                }
            }
        }
    }
}
