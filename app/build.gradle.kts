// app/build.gradle.kts
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
}

// CI 签名接线：全部密钥从环境变量读取（GitHub Actions secret 注入）。
// 本地无 KEYSTORE_FILE 时 assembleRelease 产未签名包，不报错。
val keystoreFile = System.getenv("KEYSTORE_FILE")

android {
    namespace = "com.quickpick"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.quickpick"
        minSdk = 33
        targetSdk = 37
        versionCode = 3
        versionName = "1.1.1"
    }

    signingConfigs {
        if (keystoreFile != null) {
            create("release") {
                storeFile = file(keystoreFile)
                storePassword = System.getenv("KEYSTORE_PASSWORD")
                keyAlias = System.getenv("KEY_ALIAS")
                keyPassword = System.getenv("KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
            if (keystoreFile != null) {
                signingConfig = signingConfigs.getByName("release")
            }
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
    // Compose BOM 2026.09.00（Compose 1.12.1 / M3 1.5.x）—— miuix 0.9.4 按 CMP 1.12.0 构建，对齐
    // 硬要求：compileSdk 37 + AGP≥9.1（spike/miuix-align 已配齐）
    val composeBom = platform("androidx.compose:compose-bom:2026.09.00")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    // Material3 已随 T8 玻璃底栏完成全量迁移移除（最后一个引用是 M3 NavigationBar）
    // 注意：不引 material-icons-extended（图标全部用 emoji，见设计系统第九节）

    // miuix 组件库 + 玻璃模糊（悬浮玻璃底栏，T8）
    implementation("top.yukonga.miuix.kmp:miuix-ui-android:0.9.4")
    implementation("top.yukonga.miuix.kmp:miuix-blur-android:0.9.4")
    // 设置项组件（WindowSpinnerPreference/SwitchPreference 等，主题设置页用）
    implementation("top.yukonga.miuix.kmp:miuix-preference-android:0.9.4")

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

    // 单元测试：domain 层纯 Kotlin，JUnit4 直接跑 JVM（kotlin-test 版本随 Kotlin 插件）
    testImplementation(kotlin("test-junit"))
}
