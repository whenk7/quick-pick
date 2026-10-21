package com.quickpick.ui.screen

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.quickpick.ui.theme.*
import com.quickpick.ui.viewmodel.AiPromptViewModel

@Composable
fun AiPromptScreen(
    viewModel: AiPromptViewModel,
    onImportRule: (json: String, smsText: String) -> Unit,
    onBack: () -> Unit
) {
    val smsText by viewModel.smsText.collectAsStateWithLifecycle()
    val prompt by viewModel.generatedPrompt.collectAsStateWithLifecycle()
    val importedJson by viewModel.importedJson.collectAsStateWithLifecycle()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            // 自定义顶栏要自己处理状态栏 inset（targetSdk 36 强制 edge-to-edge）
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
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
                shape = RoundedCornerShape(16.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 生成按钮
            Button(
                onClick = { viewModel.generatePrompt() },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Primary),
                shape = RoundedCornerShape(16.dp),
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
                    shape = RoundedCornerShape(16.dp),
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
                    shape = RoundedCornerShape(16.dp)
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
                    shape = RoundedCornerShape(16.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = {
                        // 温和文案：不把 Gson 英文异常原文漏给用户
                        val clean = viewModel.validatedJson()
                        if (clean != null) {
                            onImportRule(clean, smsText)
                        } else {
                            Toast.makeText(context, "没看懂这段内容，检查一下再粘贴试试～", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Primary),
                    shape = RoundedCornerShape(16.dp),
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
