package com.chenzixin.wrongbook.data.repository

import com.chenzixin.wrongbook.core.WbResult
import com.chenzixin.wrongbook.data.local.entity.ApiProfileEntity
import com.chenzixin.wrongbook.data.remote.LlmClient
import kotlinx.coroutines.flow.Flow

/**
 * 大模型接入配置。API Key 在仓储层内加解密，UI 只经手明文一次（用户输入的那一刻）。
 */
interface SettingsRepository {

    fun observeProfiles(): Flow<List<ApiProfileEntity>>

    suspend fun getProfile(id: Long): ApiProfileEntity?

    /** 新建或更新；apiKey 为 null 表示「不修改已保存的 Key」。 */
    suspend fun saveProfile(profile: ApiProfileEntity, apiKey: String?): WbResult<Long>

    suspend fun deleteProfile(id: Long): WbResult<Unit>

    suspend fun setDefault(id: Long): WbResult<Unit>

    /** 默认配置 + 解密后的 Key；未配置时返回 null。 */
    suspend fun resolveActive(): ActiveProfile?

    /** 测试连接（会解密 Key）。 */
    suspend fun testConnection(id: Long): WbResult<Long>

    /** 取某个配置的 Key 明文（仅在内存中短暂使用）。 */
    suspend fun revealApiKey(id: Long): String?

    data class ActiveProfile(
        val profile: ApiProfileEntity,
        val apiKey: String,
    )
}
