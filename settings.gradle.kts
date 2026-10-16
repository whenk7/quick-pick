// settings.gradle.kts
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

// 正确的 DSL 是 dependencyResolutionManagement（初版误写 dependencyResolution，配置阶段直接失败）
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "QuickPick"
include(":app")
