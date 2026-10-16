package com.quickpick.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.quickpick.data.db.dao.MatchRuleDao
import com.quickpick.data.db.dao.PackageRecordDao
import com.quickpick.data.db.dao.ScannedSmsDao
import com.quickpick.data.db.entity.MatchRuleEntity
import com.quickpick.data.db.entity.PackageRecordEntity
import com.quickpick.data.db.entity.ScannedSmsEntity

@Database(
    entities = [
        MatchRuleEntity::class,
        PackageRecordEntity::class,
        ScannedSmsEntity::class
    ],
    version = 1,
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
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
