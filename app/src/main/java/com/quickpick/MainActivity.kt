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
            QuickPickTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = Background) {
                    // 首启判定逻辑在 AppNavigation 内（DataStore）
                    AppNavigation()
                }
            }
        }
    }
}
