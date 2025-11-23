package com.example.unimarket.model.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.unimarket.model.data.local.entity.TopBusinessLocalEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TopBusinessDao {

    @Query("""
        SELECT * FROM top_business_local
        WHERE categoryId = :categoryId
        ORDER BY rank ASC
    """)
    fun observeTopByCategory(categoryId: String): Flow<List<TopBusinessLocalEntity>>

    @Query("""
        SELECT * FROM top_business_local
        ORDER BY categoryId, rank
    """)
    fun observeAll(): Flow<List<TopBusinessLocalEntity>>

    @Query("DELETE FROM top_business_local WHERE categoryId = :categoryId")
    suspend fun clearForCategory(categoryId: String)

    @Query("DELETE FROM top_business_local")
    suspend fun clearAll()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<TopBusinessLocalEntity>)

    @Query("""
        SELECT * FROM top_business_local
        WHERE categoryId = :categoryId
        ORDER BY rank ASC
    """)
    suspend fun getTopByCategory(categoryId: String): List<TopBusinessLocalEntity>

    @Query("""
        SELECT * FROM top_business_local
        ORDER BY categoryId, rank
    """)
    suspend fun getAllOnce(): List<TopBusinessLocalEntity>
}