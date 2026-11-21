package com.chenzixin.wrongbook.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.chenzixin.wrongbook.ui.component.WbAlertDialog
import com.chenzixin.wrongbook.ui.component.WbChip
import com.chenzixin.wrongbook.ui.component.WbDangerButton
import com.chenzixin.wrongbook.ui.component.WbPrimaryButton
import com.chenzixin.wrongbook.ui.component.WbSecondaryButton
import com.chenzixin.wrongbook.ui.component.WbTextButton
import com.chenzixin.wrongbook.ui.component.WbHintBar
import com.chenzixin.wrongbook.ui.component.WbListRow
import com.chenzixin.wrongbook.ui.component.WbSection
import com.chenzixin.wrongbook.ui.component.WbTextField
import com.chenzixin.wrongbook.ui.component.WbTopBar
import com.chenzixin.wrongbook.ui.theme.wbColors
import io.github.robinpcrd.cupertino.CupertinoText

/**
 * 新增 / 编辑一套 API 配置。
 *
 * 安全约定：Key 输入框永远不回显已保存的值，只显示「已保存，留空不修改」。
 */
@Composable
fun ProfileEditScreen(
    onBack: () -> Unit,
    vm: ProfileEditViewModel = hiltViewModel(),
) {
    val state by vm.state.collectAsState()
    val colors = wbColors()
    var confirmDelete by remember { mutableStateOf(false) }
    var guidePreset by remember { mutableStateOf<ProviderPreset?>(null) }

    LaunchedEffect(state.finished) {
        if (state.finished) onBack()
    }

    Column(modifier = Modifier.fillMaxSize()) {
        WbTopBar(
            title = if (state.isNew) "添加 API 配置" else "编辑 API 配置",
            onBack = onBack,
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 32.dp),
        ) {
            WbSection(
                header = "免费服务商",
                footer = "点任意一个，能看到「去哪申请 Key」和领取步骤，再一键填入地址与模型名。Key 需要自己申请，免费政策以官网为准。",
            ) {
                PROVIDER_PRESETS.forEachIndexed { index, preset ->
                    WbListRow(
                        title = if (preset.needsVpn) "${preset.name} · 需外网" else preset.name,
                        subtitle = preset.quota,
                        showDivider = index != PROVIDER_PRESETS.lastIndex,
                        onClick = { guidePreset = preset },
                        trailing = {
                            if (state.baseUrl == preset.baseUrl) {
                                CupertinoText(text = "使用中", fontSize = 11.sp, color = colors.tint)
                            }
                        },
                    )
                }
            }

            WbSection(header = "基础") {
                Column(modifier = Modifier.padding(16.dp)) {
                    WbTextField(
                        value = state.name,
                        onValueChange = vm::onNameChange,
                        placeholder = "配置名称，例如 DeepSeek 主力",
                    )
                    Spacer(Modifier.height(12.dp))
                    WbTextField(
                        value = state.baseUrl,
                        onValueChange = vm::onBaseUrlChange,
                        placeholder = "接口地址，例如 https://api.deepseek.com/v1",
                        keyboardType = KeyboardType.Uri,
                    )
                    Spacer(Modifier.height(12.dp))
                    WbTextField(
                        value = state.apiKeyInput,
                        onValueChange = vm::onApiKeyChange,
                        placeholder = if (state.hasSavedKey) "API Key 已保存，留空则不修改" else "API Key",
                        visualTransformation = PasswordVisualTransformation(),
                    )
                }
            }

            WbSection(
                header = "模型",
                footer = "视觉模型留空 = 不支持识图，扫描流程将使用本地 OCR。",
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    WbTextField(
                        value = state.chatModel,
                        onValueChange = vm::onChatModelChange,
                        placeholder = "对话模型，例如 deepseek-chat",
                    )
                    Spacer(Modifier.height(12.dp))
                    WbTextField(
                        value = state.visionModel,
                        onValueChange = vm::onVisionModelChange,
                        placeholder = "视觉模型（可空），例如 qwen-vl-plus",
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CupertinoText(text = "温度 ${"%.1f".format(state.temperature)}", fontSize = 15.sp, color = colors.label)
                        Spacer(Modifier.width(16.dp))
                        WbTextButton(text = "－", onClick = { vm.adjustTemperature(-0.1f) })
                        WbTextButton(text = "＋", onClick = { vm.adjustTemperature(0.1f) })

                    Spacer(Modifier.height(10.dp))
                    CupertinoText(
                        text = "最大输出 ${state.maxTokens} tokens",
                        fontSize = 15.sp,
                        color = colors.label,
                    )
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(4096, 8192, 16384).forEach { n ->
                            WbChip(
                                label = "$n",
                                selected = state.maxTokens == n,
                                onClick = { vm.setMaxTokens(n) },
                            )
                        }
                    }
                    CupertinoText(
                        text = "回答被截断时调大这一项；模型自身的上限以服务商文档为准。",
                        modifier = Modifier.padding(top = 4.dp),
                        fontSize = 11.sp,
                        color = colors.secondaryLabel,
                    )
                    }
                }
            }

            state.message?.let { msg ->
                WbHintBar(text = msg, onClick = vm::clearMessage)
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                WbPrimaryButton(
                    text = if (state.saving) "保存中…" else "保存",
                    onClick = vm::save,
                    modifier = Modifier.weight(1f),
                    enabled = !state.saving,
                )
                WbSecondaryButton(
                    text = if (state.testing) "测试中…" else "测试连接",
                    onClick = vm::testConnection,
                    modifier = Modifier.weight(1f),
                    enabled = !state.testing,
                )
                WbSecondaryButton(
                    text = if (state.visionTesting) "自检中…" else "测试识图",
                    onClick = vm::testVision,
                    modifier = Modifier.weight(1f),
                    enabled = !state.visionTesting,
                )
            }

            if (!state.isNew) {
                WbSection {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            WbSecondaryButton(
                                text = if (state.isDefault) "已是默认" else "设为默认",
                                onClick = vm::setDefault,
                                modifier = Modifier.weight(1f),
                            )
                            WbDangerButton(
                                text = "删除",
                                onClick = { confirmDelete = true },
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }
        }
    }

    guidePreset?.let { preset ->
        ProviderGuideDialog(
            preset = preset,
            onApply = { vm.applyPreset(preset) },
            onDismiss = { guidePreset = null },
        )
    }

    WbAlertDialog(
        visible = confirmDelete,
        title = "删除这套配置？",
        message = "删除后需要重新填写 API Key。",
        confirmText = "删除",
        destructive = true,
        onConfirm = {
            confirmDelete = false
            vm.delete()
        },
        onDismiss = { confirmDelete = false },
    )
}
