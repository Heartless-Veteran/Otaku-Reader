package app.otakureader.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import app.otakureader.core.database.entity.DynamicCategoryRuleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DynamicCategoryRuleDao {
    @Query("SELECT * FROM dynamic_category_rules WHERE category_id = :categoryId")
    fun getRulesForCategory(categoryId: Long): Flow<List<DynamicCategoryRuleEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(rules: List<DynamicCategoryRuleEntity>)

    @Query("DELETE FROM dynamic_category_rules WHERE category_id = :categoryId")
    suspend fun deleteForCategory(categoryId: Long)

    @Query("SELECT COUNT(*) FROM dynamic_category_rules WHERE category_id = :categoryId")
    suspend fun countForCategory(categoryId: Long): Int
}
