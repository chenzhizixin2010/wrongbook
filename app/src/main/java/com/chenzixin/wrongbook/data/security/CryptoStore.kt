package com.chenzixin.wrongbook.data.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

/**
 * API Key 加密存储（AES/GCM，密钥在 Android Keystore 里，不出系统）。
 *
 * 存进数据库的格式：`v1:<base64(iv)>:<base64(ciphertext)>`。
 * 明文只在内存里短暂存在，禁止写日志、禁止进导出包。
 *
 * 不用 androidx.security:security-crypto（已废弃且引入额外依赖），
 * 直接走 Keystore + JCE，行为可控、可审计。
 */
@Singleton
class CryptoStore @Inject constructor() {

    private val alias = "wrongbook_api_key_v1"

    private fun keyStore(): KeyStore =
        KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }

    private fun getOrCreateKey(): SecretKey {
        val ks = keyStore()
        (ks.getEntry(alias, null) as? KeyStore.SecretKeyEntry)?.let { return it.secretKey }

        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        generator.init(
            KeyGenParameterSpec.Builder(
                alias,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build(),
        )
        return generator.generateKey()
    }

    /** 加密；空串直接返回空串（表示「未配置」）。 */
    fun encrypt(plain: String): String {
        if (plain.isEmpty()) return ""
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        val iv = cipher.iv
        val encrypted = cipher.doFinal(plain.toByteArray(Charsets.UTF_8))
        return "v1:${b64(iv)}:${b64(encrypted)}"
    }

    /** 解密失败（换机、密钥被清除、数据被篡改）返回 null，由调用方提示重新填写。 */
    fun decrypt(stored: String): String? {
        if (stored.isEmpty()) return ""
        if (!stored.startsWith("v1:")) return null
        val parts = stored.split(':')
        if (parts.size != 3) return null
        return try {
            val iv = Base64.decode(parts[1], Base64.NO_WRAP)
            val data = Base64.decode(parts[2], Base64.NO_WRAP)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), GCMParameterSpec(128, iv))
            String(cipher.doFinal(data), Charsets.UTF_8)
        } catch (t: Throwable) {
            null
        }
    }

    private fun b64(bytes: ByteArray): String = Base64.encodeToString(bytes, Base64.NO_WRAP)

    private companion object {
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
    }
}
