package com.quickpick

import android.app.Application
import com.quickpick.data.db.AppDatabase
import com.quickpick.data.local.SettingsStore

class QuickPickApp : Application() {

    val database: AppDatabase by lazy { AppDatabase.getInstance(this) }
    val settingsStore: SettingsStore by lazy { SettingsStore(this) }

    // v1.1.11 起不再内置任何预置规则（短信格式各家差异太大，内置规则命中率极低，
    // 反而让用户以为「装了就该有数据」）。规则由用户自己在规则页新建或 AI 生成。
    // 老用户库里已有的两条预置规则不自动删除——那是用户的数据，让他自己处理。

    override fun onCreate() {
        super.onCreate()
    }
}
