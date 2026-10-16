package com.quickpick.data.db.dao

import androidx.room.*
import com.quickpick.data.db.entity.MatchRuleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MatchRuleDao {
    @Query("SELECT * FROM match_rules ORDER BY is_preset DESC, created_at ASC")
    fun observeAll(): Flow<List<MatchRuleEntity>>

    @Query("SELECT * FROM match_rules ORDER BY is_preset DESC, created_at ASC")
    suspend fun getAll(): List<MatchRuleEntity>

    @Query("SELECT * FROM match_rules WHERE id = :id")
    suspend fun getById(id: Long): MatchRuleEntity?

    /** 导入时的同名冲突检测 */
    @Query("SELECT * FROM match_rules WHERE name = :name")
    suspend fun getByName(name: String): List<MatchRuleEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(rule: MatchRuleEntity): Long

    @Update
    suspend fun update(rule: MatchRuleEntity)

    @Query("DELETE FROM match_rules WHERE id = :id")
    suspend fun deleteById(id: Long)
}
