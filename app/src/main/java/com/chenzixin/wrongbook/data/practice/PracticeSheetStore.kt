package com.chenzixin.wrongbook.data.practice

import android.content.Context
import com.chenzixin.wrongbook.core.WbResult
import com.chenzixin.wrongbook.di.IoDispatcher
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * 练习卷历史：生成过的卷子存成 HTML 文件 + 一份索引 JSON。
 *
 * 为什么不进 Room：练习卷是「一次性产物」，不需要查询/关联；
 * 放 filesDir 里既不用为它写数据库迁移，也能直接再次打印或另存 PDF。
 */
@Singleton
class PracticeSheetStore @Inject constructor(
    @ApplicationContext private val context: Context,
    @IoDispatcher private val io: CoroutineDispatcher,
) {

    @Serializable
    data class Record(
        val id: String,
        val title: String,
        val subtitle: String,
        val count: Int,
        val createdAt: Long,
        /** 卷子的结构化副本，用来导出 Word；老记录可能没有 */
        val sheetJson: String = "",
    )

    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }

    private val dir: File get() = File(context.filesDir, "practice").apply { if (!exists()) mkdirs() }
    private val indexFile: File get() = File(dir, "index.json")

    suspend fun save(
        html: String,
        title: String,
        subtitle: String,
        count: Int,
        sheetJson: String = "",
    ): WbResult<Record> =
        withContext(io) {
            runCatching {
                val record = Record(
                    id = "sheet-${System.currentTimeMillis()}",
                    title = title,
                    subtitle = subtitle,
                    count = count,
                    createdAt = System.currentTimeMillis(),
                    sheetJson = sheetJson,
                )
                File(dir, "${record.id}.html").writeText(html, Charsets.UTF_8)
                val list = (readIndex() + record).sortedByDescending { it.createdAt }
                val kept = list.take(MAX_HISTORY)
                // 超出上限的老卷子连文件一起删掉
                list.drop(MAX_HISTORY).forEach { old ->
                    runCatching { File(dir, "${old.id}.html").delete() }
                }
                writeIndex(kept)
                record
            }.fold(
                onSuccess = { WbResult.Ok(it) },
                onFailure = { WbResult.Err("保存历史记录失败", it) },
            )
        }

    suspend fun list(): List<Record> = withContext(io) {
        runCatching { readIndex() }.getOrDefault(emptyList())
    }

    suspend fun read(id: String): String? = withContext(io) {
        runCatching { File(dir, "$id.html").takeIf { it.exists() }?.readText(Charsets.UTF_8) }.getOrNull()
    }

    suspend fun delete(id: String): WbResult<Unit> = withContext(io) {
        runCatching {
            File(dir, "$id.html").delete()
            writeIndex(readIndex().filterNot { it.id == id })
            Unit
        }.fold(
            onSuccess = { WbResult.Ok(Unit) },
            onFailure = { WbResult.Err("删除失败", it) },
        )
    }

    private fun readIndex(): List<Record> {
        if (!indexFile.exists()) return emptyList()
        val text = indexFile.readText(Charsets.UTF_8)
        if (text.isBlank()) return emptyList()
        return runCatching { json.decodeFromString<List<Record>>(text) }.getOrDefault(emptyList())
    }

    private fun writeIndex(list: List<Record>) {
        indexFile.writeText(json.encodeToString(list), Charsets.UTF_8)
    }

    private companion object {
        const val MAX_HISTORY = 30
    }
}
