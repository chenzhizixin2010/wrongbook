package com.chenzixin.wrongbook.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.updateDataStore by preferencesDataStore(name = "update_prefs")

/**
 * 更新提示的记住状态。
 *
 * 记住「用户点过稍后的那个版本」，避免每次启动都拿同一个大版本弹窗骚扰。
 * 下次发新版（版本号变了）会重新弹。
 */
@Singleton
class UpdatePrefs @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val dismissedKey = stringPreferencesKey("dismissed_major_version")

    suspend fun dismissedMajorVersion(): String? =
        context.updateDataStore.data.map { it[dismissedKey] }.first()

    suspend fun dismissMajor(version: String) {
        context.updateDataStore.edit { it[dismissedKey] = version }
    }
}
