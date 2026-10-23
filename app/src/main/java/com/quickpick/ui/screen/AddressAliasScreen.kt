package com.quickpick.ui.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.quickpick.ui.theme.Background
import com.quickpick.ui.theme.InputFieldColors
import com.quickpick.ui.theme.Primary
import com.quickpick.ui.theme.TextPrimary
import com.quickpick.ui.theme.TextSecondary
import com.quickpick.ui.theme.TextTertiary
import com.quickpick.ui.viewmodel.AddressAliasViewModel
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 全局地址簿（v1.1.7 §6）：「包含文本 → 显示为」行式列表。
 * 同一地点被多家快递短信提到只配一次；显示时替换，改了即时生效。
 */
@Composable
fun AddressAliasScreen(viewModel: AddressAliasViewModel, onBack: () -> Unit) {
    val aliases by viewModel.aliases.collectAsStateWithLifecycle()
    var matchInput by remember { mutableStateOf("") }
    var aliasInput by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Text("←", style = MiuixTheme.textStyles.title4, color = TextPrimary)
                }
                Text(
                    text = "地址别名",
                    style = MiuixTheme.textStyles.headline1,
                    color = TextPrimary,
                    modifier = Modifier.weight(1f)
                )
            }
        },
        containerColor = Background
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            item {
                Text(
                    text = "地址太长或有多种叫法？换成好记的名字显示在卡片上。",
                    style = MiuixTheme.textStyles.body2,
                    color = TextTertiary,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            if (aliases.isEmpty()) {
                item {
                    Text(
                        text = "还没有地址别名",
                        style = MiuixTheme.textStyles.body2,
                        color = TextTertiary,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                }
            }

            items(aliases, key = { it.match }) { item ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    cornerRadius = 12.dp
                ) {
                    Row(
                        modifier = Modifier.padding(start = 14.dp, top = 10.dp, bottom = 10.dp, end = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = item.match,
                            style = MiuixTheme.textStyles.body1,
                            color = TextPrimary,
                            modifier = Modifier.weight(1f)
                        )
                        Text("→", style = MiuixTheme.textStyles.body2, color = TextTertiary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = item.alias,
                            style = MiuixTheme.textStyles.body1,
                            color = Primary
                        )
                        IconButton(
                            onClick = { viewModel.remove(item) },
                            modifier = Modifier
                                .width(40.dp)
                                .height(40.dp)
                        ) {
                            Text("×", style = MiuixTheme.textStyles.body1, color = TextTertiary)
                        }
                    }
                }
            }

            // 添加区
            item {
                SmallTitle(text = "添加")
                Card(modifier = Modifier.fillMaxWidth(), cornerRadius = 12.dp) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        TextField(
                            value = matchInput,
                            onValueChange = { matchInput = it },
                            label = "包含文本（如：绿洲家园）",
                            useLabelAsPlaceholder = true,
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            colors = InputFieldColors
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        TextField(
                            value = aliasInput,
                            onValueChange = { aliasInput = it },
                            label = "显示为（如：家门口）",
                            useLabelAsPlaceholder = true,
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            colors = InputFieldColors
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = {
                                viewModel.add(matchInput, aliasInput)
                                matchInput = ""
                                aliasInput = ""
                            },
                            enabled = matchInput.isNotBlank() && aliasInput.isNotBlank(),
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColorsPrimary()
                        ) {
                            Text("添加", color = androidx.compose.ui.graphics.Color.White)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}
