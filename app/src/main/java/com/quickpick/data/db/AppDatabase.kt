package com.quickpick.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.quickpick.data.db.dao.MatchRuleDao
import com.quickpick.data.db.dao.PackageRecordDao
import com.quickpick.data.db.dao.ScannedSmsDao
import com.quickpick.data.db.entity.MatchRuleEntity
import com.quickpick.data.db.entity.PackageRecordEntity
import com.quickpick.data.db.entity.ScannedSmsEntity

/**
 * v1→v2：match_rules 加 sender_pattern/brand_prefix/brand_suffix，
 * package_records 加 sms_thread_id、删 is_incomplete（SQLite < 3.35 不支持
 * DROP COLUMN，走新表迁移，列定义与 PackageRecordEntity 注解一一对应）。
 * 建表语句以 spec §九 为骨架，按 Entity 注解核对修正：原表没有 updated_at 列，
 * rule_id/sms_id 可空，外键（SET_NULL）与两个索引必须在新表上重建。
 */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // match_rules: 加列（默认 NULL，不需要回填）
        db.execSQL("ALTER TABLE match_rules ADD COLUMN sender_pattern TEXT")
        db.execSQL("ALTER TABLE match_rules ADD COLUMN brand_prefix TEXT")
        db.execSQL("ALTER TABLE match_rules ADD COLUMN brand_suffix TEXT")
        // package_records: 加列
        db.execSQL("ALTER TABLE package_records ADD COLUMN sms_thread_id INTEGER")
        // package_records: 删 is_incomplete 列，走新表迁移
        db.execSQL(
            """
            CREATE TABLE package_records_new (
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
        db.execSQL(
            """
            INSERT INTO package_records_new
                (id, company_name, tracking_number, pickup_code, address, phone,
                 sms_body, sms_timestamp, is_picked_up, rule_id, sms_id, sms_thread_id, created_at)
            SELECT id, company_name, tracking_number, pickup_code, address, phone,
                   sms_body, sms_timestamp, is_picked_up, rule_id, sms_id, NULL, created_at
            FROM package_records
            """.trimIndent()
        )
        db.execSQL("DROP TABLE package_records")
        db.execSQL("ALTER TABLE package_records_new RENAME TO package_records")
        // 重建索引（对应 Entity 的 @Index 注解，索引名遵循 Room 约定）
        db.execSQL("CREATE INDEX IF NOT EXISTS index_package_records_sms_timestamp ON package_records (sms_timestamp)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_package_records_rule_id ON package_records (rule_id)")
    }
}

/** v2→v3：package_records 加 sms_sender（发件人号码，「短信原文」弹窗用）。纯加列，零数据风险 */
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE package_records ADD COLUMN sms_sender TEXT")
    }
}

/** v3→v4：package_records 加 merged_sms_ids（归并被顶掉的旧短信 id，删除记录时全忽略）。纯加列带默认值 */
val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE package_records ADD COLUMN merged_sms_ids TEXT NOT NULL DEFAULT '[]'")
    }
}

@Database(
    entities = [
        MatchRuleEntity::class,
        PackageRecordEntity::class,
        ScannedSmsEntity::class
    ],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun matchRuleDao(): MatchRuleDao
    abstract fun packageRecordDao(): PackageRecordDao
    abstract fun scannedSmsDao(): ScannedSmsDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "quickpick.db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)   // 不加 fallbackToDestructiveMigration：迁移失败就崩，比静默丢数据安全
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
