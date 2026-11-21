package com.chenzixin.wrongbook.core

/**
 * 全项目统一的返回类型：仓储层不抛裸异常，调用方必须显式处理失败分支。
 *
 * 用法：
 * ```
 * when (val r = repo.save(question)) {
 *     is WbResult.Ok -> ...
 *     is WbResult.Err -> toast(r.message)
 * }
 * ```
 */
sealed interface WbResult<out T> {
    data class Ok<out T>(val value: T) : WbResult<T>

    data class Err(
        val message: String,
        val cause: Throwable? = null,
    ) : WbResult<Nothing>

    val isOk: Boolean get() = this is Ok

    fun getOrNull(): T? = (this as? Ok)?.value
}

inline fun <T, R> WbResult<T>.map(transform: (T) -> R): WbResult<R> = when (this) {
    is WbResult.Ok -> WbResult.Ok(transform(value))
    is WbResult.Err -> this
}

/** 把可能抛异常的代码包成 WbResult，message 用于给用户看的兜底文案。 */
inline fun <T> runCatchingWb(message: String, block: () -> T): WbResult<T> =
    try {
        WbResult.Ok(block())
    } catch (c: kotlinx.coroutines.CancellationException) {
        // 协程取消必须原样抛出，否则会把「用户离开页面」当成业务失败
        throw c
    } catch (t: Throwable) {
        WbResult.Err(message, t)
    }
