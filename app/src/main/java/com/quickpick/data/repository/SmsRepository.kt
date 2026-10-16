package com.quickpick.data.repository

import com.quickpick.data.db.dao.ScannedSmsDao
import com.quickpick.data.db.entity.ScannedSmsEntity

class SmsRepository(private val dao: ScannedSmsDao) {

    suspend fun isScanned(smsId: String): Boolean = dao.exists(smsId)

    suspend fun markScanned(smsId: String) =
        dao.insert(ScannedSmsEntity(smsId = smsId))

    /** 已扫描总数（用于区分空状态：收件箱空 vs 扫了但没匹配到） */
    suspend fun scannedCount(): Int = dao.count()

    /** 清除「未匹配短信」的已扫标记，下次扫描自动重试（规则更新后用） */
    suspend fun deleteUnmatched() = dao.deleteUnmatched()

    /** 全量重扫前清空 */
    suspend fun clearScanned() = dao.clearAll()
}
