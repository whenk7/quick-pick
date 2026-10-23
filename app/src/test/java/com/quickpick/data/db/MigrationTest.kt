package com.quickpick.data.db

import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import com.quickpick.data.db.entity.PackageRecordEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Room v2→v3 迁移测试：真 SQLite（Robolectric）上验证 ALTER 语义。
 * 手工建 v2 形状的表 + 旧数据 → 跑 MIGRATION_2_3 → 列在、数据在、新列 NULL。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class MigrationTest {

    @Test
    fun `v2 to v3 adds sms_sender column and preserves rows`() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val helper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(null)   // 内存库
                .callback(object : SupportSQLiteOpenHelper.Callback(2) {
                    override fun onConfigure(db: androidx.sqlite.db.SupportSQLiteDatabase) {}

                    override fun onCreate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                        // v2 形状的 package_records（无 sms_sender），列定义与 v2 Entity 一致
                        db.execSQL(
                            """
                            CREATE TABLE package_records (
                                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                                company_name TEXT NOT NULL,
                                tracking_number TEXT,
                                pickup_code TEXT,
                                address TEXT,
                                phone TEXT,
                                sms_body TEXT NOT NULL,
                                sms_timestamp INTEGER NOT NULL,
                                is_picked_up INTEGER NOT NULL,
                                rule_id INTEGER,
                                sms_id TEXT,
                                sms_thread_id INTEGER,
                                created_at INTEGER NOT NULL,
                                FOREIGN KEY(rule_id) REFERENCES match_rules(id) ON UPDATE NO ACTION ON DELETE SET NULL
                            )
                            """.trimIndent()
                        )
                        db.execSQL("CREATE TABLE match_rules (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, name TEXT NOT NULL)")
                        db.execSQL(
                            "INSERT INTO package_records (company_name, pickup_code, sms_body, sms_timestamp, is_picked_up, created_at) " +
                                "VALUES ('菜鸟驿站', '4-5-0877', '正文', 1700000000000, 0, 1700000000000)"
                        )
                    }

                    override fun onUpgrade(
                        db: androidx.sqlite.db.SupportSQLiteDatabase,
                        oldVersion: Int,
                        newVersion: Int
                    ) {
                    }
                })
                .build()
        )

        helper.writableDatabase.use { db ->
            // 迁移前列不存在
            val beforeCols = mutableListOf<String>()
            db.query("PRAGMA table_info(package_records)").use { c ->
                while (c.moveToNext()) beforeCols.add(c.getString(1))
            }
            assertTrue("sms_sender" !in beforeCols)

            MIGRATION_2_3.migrate(db)

            // 迁移后：列在、旧数据在、新列 NULL
            val afterCols = mutableListOf<String>()
            db.query("PRAGMA table_info(package_records)").use { c ->
                while (c.moveToNext()) afterCols.add(c.getString(1))
            }
            assertTrue("sms_sender" in afterCols, "迁移后应有 sms_sender 列")

            db.query("SELECT pickup_code, sms_sender FROM package_records").use { c ->
                assertTrue(c.moveToFirst())
                assertEquals("4-5-0877", c.getString(0))
                assertTrue(c.isNull(1), "老记录 sms_sender 应为 NULL（重扫回填）")
            }
        }
    }

    @Test
    fun `v3 entity round-trips sms_sender`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val db = androidx.room.Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .build()
        try {
            db.packageRecordDao().insert(
                PackageRecordEntity(
                    companyName = "圆通速递", pickupCode = "44651320",
                    smsBody = "正文", smsTimestamp = 1700000000000,
                    smsSender = "10684110405676900"
                )
            )
            val list = db.packageRecordDao().observeAll().first()
            assertEquals("10684110405676900", list.single().smsSender)
        } finally {
            db.close()
        }
    }

    @Test
    fun `v3 to v4 adds merged_sms_ids with empty default`() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val helper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(null)
                .callback(object : SupportSQLiteOpenHelper.Callback(3) {
                    override fun onConfigure(db: androidx.sqlite.db.SupportSQLiteDatabase) {}

                    override fun onCreate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                        // v3 形状的 package_records（无 merged_sms_ids）
                        db.execSQL(
                            """
                            CREATE TABLE package_records (
                                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                                company_name TEXT NOT NULL,
                                tracking_number TEXT,
                                pickup_code TEXT,
                                address TEXT,
                                phone TEXT,
                                sms_body TEXT NOT NULL,
                                sms_timestamp INTEGER NOT NULL,
                                is_picked_up INTEGER NOT NULL,
                                rule_id INTEGER,
                                sms_id TEXT,
                                sms_thread_id INTEGER,
                                sms_sender TEXT,
                                created_at INTEGER NOT NULL
                            )
                            """.trimIndent()
                        )
                        db.execSQL(
                            "INSERT INTO package_records (company_name, pickup_code, sms_body, sms_timestamp, is_picked_up, created_at) " +
                                "VALUES ('菜鸟驿站', '4-5-0877', '正文', 1700000000000, 0, 1700000000000)"
                        )
                    }

                    override fun onUpgrade(
                        db: androidx.sqlite.db.SupportSQLiteDatabase,
                        oldVersion: Int,
                        newVersion: Int
                    ) {
                    }
                })
                .build()
        )

        helper.writableDatabase.use { db ->
            MIGRATION_3_4.migrate(db)
            val cols = mutableListOf<String>()
            db.query("PRAGMA table_info(package_records)").use { c ->
                while (c.moveToNext()) cols.add(c.getString(1))
            }
            assertTrue("merged_sms_ids" in cols, "迁移后应有 merged_sms_ids 列")
            db.query("SELECT pickup_code, merged_sms_ids FROM package_records").use { c ->
                assertTrue(c.moveToFirst())
                assertEquals("4-5-0877", c.getString(0))
                assertEquals("[]", c.getString(1), "老记录默认空列表")
            }
        }
    }
}
