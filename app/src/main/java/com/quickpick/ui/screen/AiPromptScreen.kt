package com.quickpick.ui.screen

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.quickpick.ui.theme.*
import com.quickpick.ui.viewmodel.AiPromptViewModel
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.theme.MiuixTheme

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
                    // 返回箭头字形：原依赖 titleLarge 内嵌色，T6 起显式传色
                    Text("←", style = MiuixTheme.textStyles.title4, color = TextPrimary)
                }
                Text(
                    text = "AI 生成规则",
                    style = MiuixTheme.textStyles.headline1,
                    color = TextPrimary,
                    modifier = Modifier.weight(1f)
                )
            }
        },
        containerColor = Background
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
                style = MiuixTheme.textStyles.body2,
                color = TextTertiary
            )
            Spacer(modifier = Modifier.height(8.dp))
            TextField(
                value = smsText,
                onValueChange = { viewModel.updateSmsText(it) },
                label = "从短信 App 复制一条快递短信粘贴到这里...",
                useLabelAsPlaceholder = true,
                modifier = Modifier.fillMaxWidth(),
                minLines = 4,
                colors = InputFieldColors
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 生成按钮
            Button(
                onClick = { viewModel.generatePrompt() },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColorsPrimary(),
                enabled = smsText.isNotBlank()
            ) {
                Text("生成提示词", color = Color.White)
            }

            if (prompt.isNotEmpty()) {
                Spacer(modifier = Modifier.height(20.dp))

                // Step 2: 复制提示词
                Text(
                    text = "第 2 步：复制提示词并去 AI 网站提问",
                    style = MiuixTheme.textStyles.body2,
                    color = TextTertiary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 16.dp,
                    colors = CardDefaults.defaultColors(color = InputBackground)
                ) {
                    Text(
                        text = prompt,
                        modifier = Modifier.padding(14.dp),
                        style = MiuixTheme.textStyles.body2.copy(
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
                    colors = ButtonDefaults.buttonColorsPrimary()
                ) {
                    Text("📋 一键复制提示词", color = Color.White)
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Step 3: 导入 JSON
                Text(
                    text = "第 3 步：粘贴 AI 返回的 JSON",
                    style = MiuixTheme.textStyles.body2,
                    color = TextTertiary
                )
                Spacer(modifier = Modifier.height(8.dp))
                TextField(
                    value = importedJson,
                    onValueChange = { viewModel.updateImportedJson(it) },
                    label = "将从 AI 网站获取的 JSON 结果粘贴到这里...",
                    useLabelAsPlaceholder = true,
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 5,
                    colors = InputFieldColors
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
                    colors = ButtonDefaults.buttonColorsPrimary(),
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
