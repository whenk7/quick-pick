package com.quickpick

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.quickpick.ui.navigation.AppNavigation
import com.quickpick.ui.theme.Background
import com.quickpick.ui.theme.QuickPickTheme
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            // spike 冒烟：miuix 主题包住现有 M3 界面，验证库可初始化
            MiuixTheme(controller = remember { ThemeController(ColorSchemeMode.System) }) {
                QuickPickTheme {
                    Surface(modifier = Modifier.fillMaxSize(), color = Background) {
                        // 首启判定逻辑在 AppNavigation 内（DataStore）
                        AppNavigation()
                    }
                }
            }
        }
    }
}
