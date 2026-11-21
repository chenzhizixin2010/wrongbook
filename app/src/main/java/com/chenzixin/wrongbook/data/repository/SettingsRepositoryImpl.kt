package com.chenzixin.wrongbook.data.repository

import androidx.room.withTransaction
import com.chenzixin.wrongbook.core.WbResult
import com.chenzixin.wrongbook.core.runCatchingWb
import com.chenzixin.wrongbook.data.local.WrongBookDatabase
import com.chenzixin.wrongbook.data.local.dao.ApiProfileDao
import com.chenzixin.wrongbook.data.local.entity.ApiProfileEntity
import com.chenzixin.wrongbook.data.remote.LlmClient
import com.chenzixin.wrongbook.data.remote.LlmErrors
import com.chenzixin.wrongbook.data.security.CryptoStore
import com.chenzixin.wrongbook.di.IoDispatcher
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    private val db: WrongBookDatabase,
    private val dao: ApiProfileDao,
    private val crypto: CryptoStore,
    private val llm: LlmClient,
    @IoDispatcher private val io: CoroutineDispatcher,
) : SettingsRepository {

    override fun observeProfiles(): Flow<List<ApiProfileEntity>> = dao.observeAll()

    override suspend fun getProfile(id: Long): ApiProfileEntity? = withContext(io) { dao.findById(id) }

    override suspend fun saveProfile(profile: ApiProfileEntity, apiKey: String?): WbResult<Long> =
        withContext(io) {
            val name = profile.name.trim()
            val baseUrl = profile.baseUrl.trim()
            val chatModel = profile.chatModel.trim()
            when {
                name.isEmpty() -> return@withContext WbResult.Err("配置名称不能为空")
                baseUrl.isEmpty() -> return@withContext WbResult.Err("接口地址不能为空")
                chatModel.isEmpty() -> return@withContext WbResult.Err("对话模型名不能为空")
            }

            runCatchingWb("配置保存失败") {
                val encrypted = if (apiKey != null) crypto.encrypt(apiKey) else null
                db.withTransaction {
                    val isFirst = dao.count() == 0
                    // 关键：调用方传进来的往往是 apiKeyEnc = "" 的新实体，
                    // 用户只改温度时不能拿这个空串覆盖库里已保存的密文
                    val existingKeyEnc = if (profile.id != 0L) dao.findById(profile.id)?.apiKeyEnc else null
                    val target = profile.copy(
                        name = name,
                        baseUrl = baseUrl,
                        chatModel = chatModel,
                        visionModel = profile.visionModel.trim(),
                        apiKeyEnc = encrypted ?: existingKeyEnc ?: profile.apiKeyEnc,
                        isDefault = profile.isDefault || isFirst,
                    )
                    val id = if (target.id == 0L) {
                        dao.insert(target)
                    } else {
                        dao.update(target)
                        target.id
                    }
                    if (target.isDefault) {
                        dao.clearDefault()
                        dao.markDefault(id)
                    }
                    id
                }
            }
        }

    override suspend fun deleteProfile(id: Long): WbResult<Unit> = withContext(io) {
        runCatchingWb("删除失败") {
            db.withTransaction {
                val wasDefault = dao.findById(id)?.isDefault == true
                dao.deleteById(id)
                if (wasDefault) {
                    // 删掉默认配置后，把剩下的第一套设为默认，避免「有配置但没有默认」
                    dao.getAllOnce().firstOrNull()?.let { dao.markDefault(it.id) }
                }
            }
        }
    }

    override suspend fun setDefault(id: Long): WbResult<Unit> = withContext(io) {
        runCatchingWb("设置默认失败") {
            db.withTransaction {
                dao.clearDefault()
                dao.markDefault(id)
            }
        }
    }

    override suspend fun resolveActive(): SettingsRepository.ActiveProfile? = withContext(io) {
        val profile = dao.findDefault() ?: return@withContext null
        val key = crypto.decrypt(profile.apiKeyEnc)
        if (key.isNullOrBlank()) return@withContext null
        SettingsRepository.ActiveProfile(profile, key)
    }

    override suspend fun testConnection(id: Long): WbResult<Long> = withContext(io) {
        val profile = dao.findById(id) ?: return@withContext WbResult.Err("配置不存在")
        val key = crypto.decrypt(profile.apiKeyEnc)
        if (key.isNullOrBlank()) return@withContext WbResult.Err(LlmErrors.NO_KEY)
        llm.testConnection(profile, key)
    }

    override suspend fun revealApiKey(id: Long): String? = withContext(io) {
        val profile = dao.findById(id) ?: return@withContext null
        crypto.decrypt(profile.apiKeyEnc)
    }
}
