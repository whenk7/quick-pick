// app/build.gradle.kts
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
    // 界面渲染审查：./gradlew recordRoborazziDebug 出 PNG（无头环境验收用）
    id("io.github.takahirom.roborazzi") version "1.77.0"
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
        versionCode = 8
        versionName = "1.1.6"
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
            // 开源项目不混淆（裁决 2026-10-23）：R8 只瘦身+优化，不更名——栈可读、体积仍小
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
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

    testOptions {
        // Robolectric/Roborazzi 渲染审查需要读 Android 资源
        unitTests.isIncludeAndroidResources = true
    }

    packaging {
        resources.excludes += "META-INF/{AL2.0,LGPL2.1}"
    }
}

// Kotlin 2.x 推荐写法，替代已废弃的 android.kotlinOptions
kotlin {
    jvmToolchain(17)
}

// miuix 0.9.4 的 class 是 Java 21 字节码——Robolectric 渲染测试要加载它，
// 单测统一跑 21（纯 Kotlin 测试不受影响）。本机 21 在 ~/.gradle/gradle.properties 登记，CI 直接装 21。
tasks.withType<Test> {
    javaLauncher = javaToolchains.launcherFor { languageVersion.set(JavaLanguageVersion.of(21)) }
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
    // miuix 窗口弹层的返回手势需要 NavigationEventDispatcherOwner（本是运行时传递依赖，编译期不可见，显式引入）
    implementation("androidx.navigationevent:navigationevent-compose:1.1.2")

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
    // 界面渲染审查：无头环境把 Compose 渲成 PNG 供人工看图验收（不进 APK）
    testImplementation("io.github.takahirom.roborazzi:roborazzi:1.77.0")
    testImplementation("io.github.takahirom.roborazzi:roborazzi-compose:1.77.0")
    testImplementation("org.robolectric:robolectric:4.17")
    // 点击/交互复现测试（compose-ui-test，版本随 BOM）
    testImplementation("androidx.compose.ui:ui-test-junit4")
}
