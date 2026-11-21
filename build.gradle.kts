// 根构建脚本：只声明插件版本，不做任何 apply。
// 本项目关闭了 AGP 9 的内置 Kotlin（见 gradle.properties），因此仍需 kotlin-android / kotlin-compose。
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt) apply false
}
