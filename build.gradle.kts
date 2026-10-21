// build.gradle.kts
plugins {
    id("com.android.application") version "9.4.1" apply false
    // AGP 9.0+ 内置 Kotlin 支持，org.jetbrains.kotlin.android 插件已废除
    // Kotlin 2.x 起 Compose 编译器独立为插件，与 Kotlin 版本一一对应
    id("org.jetbrains.kotlin.plugin.compose") version "2.4.21" apply false
    id("com.google.devtools.ksp") version "2.3.12" apply false
}
