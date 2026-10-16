// app/build.gradle.kts
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.quickpick"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.quickpick"
        minSdk = 30
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true   // 设置页显示 BuildConfig.VERSION_NAME
    }

    packaging {
        resources.excludes += "META-INF/{AL2.0,LGPL2.1}"
    }
}

// Kotlin 2.x 推荐写法，替代已废弃的 android.kotlinOptions
kotlin {
    jvmToolchain(17)
}

dependencies {
    // Compose BOM 2026.06.01（Compose 1.11.4 / M3 1.4.0）
    // 偏差说明：分册原定 2026.09.00（Compose 1.12.1），但其 AAR 元数据要求
    // minCompileSdk=37 + AGP≥9.1.0，与本计划 compileSdk 36 + AGP 8.13 矩阵冲突。
    // 2026.06.01 是映射到 Compose 1.11.x（minCompileSdk=35, minAGP=8.6）的最新版 BOM。
    val composeBom = platform("androidx.compose:compose-bom:2026.06.01")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    // 注意：不引 material-icons-extended（图标全部用 emoji，见设计系统第九节）

    // Activity & Navigation
    implementation("androidx.activity:activity-compose:1.11.0")
    implementation("androidx.navigation:navigation-compose:2.9.5")

    // Lifecycle（collectAsStateWithLifecycle 需要 runtime-compose）
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.9.4")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.9.4")

    // Room（KSP2）
    implementation("androidx.room:room-runtime:2.8.4")
    implementation("androidx.room:room-ktx:2.8.4")
    ksp("androidx.room:room-compiler:2.8.4")

    // DataStore（首启标记、预置规则灌入标记）
    implementation("androidx.datastore:datastore-preferences:1.1.7")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")

    // Gson（JSON 导入导出）
    implementation("com.google.code.gson:gson:2.13.1")

    // Debug
    debugImplementation("androidx.compose.ui:ui-tooling")
}
