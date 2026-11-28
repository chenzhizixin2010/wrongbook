package com.chenzixin.wrongbook.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 一套大模型接入配置（可多套并存，其中一套为默认）。
 *
 * 安全要求：apiKeyEnc 只能存密文。明文只存在于内存中的 ApiKeyStore（Keystore 解密后使用），
 * 任何日志、导出、崩溃上报都不得包含它。
 */
@Entity(tableName = "api_profiles")
data class ApiProfileEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String = "默认配置",
    /** OpenAI 兼容地址，例如 https://api.deepseek.com/v1 */
    val baseUrl: String = "",
    /** Keystore 加密后的密文（Base64） */
    val apiKeyEnc: String = "",
    /** 对话模型，例如 deepseek-chat */
    val chatModel: String = "",
    /** 视觉模型，留空 = 不支持识图，扫描流程自动降级为纯本地 OCR */
    val visionModel: String = "",
    val temperature: Float = 0.7f,
    val maxTokens: Int = 0,
    val isDefault: Boolean = false,
    val enabled: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
)
