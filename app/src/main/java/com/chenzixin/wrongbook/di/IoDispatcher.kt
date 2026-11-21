package com.chenzixin.wrongbook.di

import javax.inject.Qualifier

/** 标记 IO 线程池，避免仓储层直接用 Dispatchers.IO（不可测）。 */
@Retention(AnnotationRetention.BINARY)
@Qualifier
annotation class IoDispatcher
