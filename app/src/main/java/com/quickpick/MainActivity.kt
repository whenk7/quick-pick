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
