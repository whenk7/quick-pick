package com.quickpick.ui.screen

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.quickpick.ui.theme.*

@Composable
fun OnboardingScreen(onPermissionGranted: () -> Unit) {
    val context = LocalContext.current
    // 用 var + remember：授权结果和从系统设置返回都要能刷新状态（初版 remember 不刷新是死状态）
    var hasPermission by remember { mutableStateOf(checkSmsPermission(context)) }

    // 从系统设置页返回时重新检查
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasPermission = checkSmsPermission(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission = granted
        // 「授权并开始使用」即授权即进入（spec 验收 1）；被拒则界面保留「去系统设置开启」入口
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
            shape = RoundedCornerShape(16.dp),
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
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("开始使用", color = Color.White)
            }
        } else {
            Button(
                onClick = { launcher.launch(Manifest.permission.READ_SMS) },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Primary),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("授权并开始使用", color = Color.White)
            }
            Spacer(modifier = Modifier.height(12.dp))
            // 永久拒绝时的出口：跳本应用系统设置页
            TextButton(onClick = {
                context.startActivity(
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", context.packageName, null)
                    }
                )
            }) {
                Text("去系统设置开启", color = TextSecondary)
            }
        }
    }
}

private fun checkSmsPermission(context: Context): Boolean =
    ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) ==
        PackageManager.PERMISSION_GRANTED
