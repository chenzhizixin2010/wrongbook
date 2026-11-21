import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    // AGP 9 的「内置 Kotlin」与当前 KSP 不兼容，故关闭内置 Kotlin（见 gradle.properties），
    // 显式应用 Kotlin 与 Compose 编译器插件。
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "com.chenzixin.wrongbook"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.chenzixin.wrongbook"
        minSdk = 24
        targetSdk = 37
        versionCode = 3
        versionName = "0.3.0"

        // 更新源：仓库信息与镜像前缀。owner/repo 也可以在设置页里改。
        buildConfigField("String", "GITHUB_OWNER", "\"chenzixin\"")
        buildConfigField("String", "GITHUB_REPO", "\"wrongbook\"")
        buildConfigField("String", "UPDATE_MIRRORS", "\"https://ghproxy.net/,https://gh-proxy.com/,https://hub.gitmirror.com/\"")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables { useSupportLibrary = true }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    // Core
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)

    // Compose
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.foundation)
    implementation(libs.compose.material3)
    implementation(libs.compose.ui.tooling.preview)
    debugImplementation(libs.compose.ui.tooling)

    // Cupertino（iOS 风格）
    implementation(libs.cupertino)

    // Hilt
    implementation(libs.hilt.android)
    implementation(libs.hilt.navigation.compose)
    ksp(libs.hilt.compiler)

    // Room
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    // 数据 / 安全
    implementation(libs.androidx.datastore.preferences)

    // 图片
    implementation(libs.coil.compose)

    // 网络与序列化（P1 起使用）
    implementation(libs.okhttp)
    implementation(libs.kotlinx.serialization.json)

    // Markdown + LaTeX（P6 聊天渲染）
    implementation(libs.markwon.core)
    implementation(libs.markwon.ext.latex)
    implementation(libs.jlatexmath.android)

    // 相机（P3）
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)

    // 本地 OCR（P3 使用）
    implementation(libs.mlkit.text.chinese)

    // 后台任务（P8 复习提醒）
    implementation(libs.androidx.work.runtime.ktx)

    // 测试
    testImplementation(libs.junit)

    // 仪器测试（DAO 行为，需要真机/模拟器）
    androidTestImplementation(libs.junit)
    androidTestImplementation(libs.androidx.test.junit)
    androidTestImplementation(libs.androidx.test.core)
    androidTestImplementation(libs.room.testing)
    androidTestImplementation(libs.kotlinx.coroutines.test)
}
