package com.example.unimarket.model.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.example.unimarket.model.data.local.entity.TopBusinessLocalEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TopBusinessDao {

    @Query("""
        SELECT * FROM top_business_local
        ORDER BY categoryId ASC, rank ASC
    """)
    suspend fun getAllOnce(): List<TopBusinessLocalEntity>

    @Query("SELECT * FROM top_business_local WHERE categoryId = :catId ORDER BY rank ASC")
    suspend fun getTopByCategory(catId: String): List<TopBusinessLocalEntity>

    @Query("DELETE FROM top_business_local WHERE categoryId = :catId")
    suspend fun clearForCategory(catId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<TopBusinessLocalEntity>)

    @Transaction
    suspend fun replaceForCategory(catId: String, items: List<TopBusinessLocalEntity>) {
        clearForCategory(catId)
        if (items.isNotEmpty()) upsertAll(items)
    }
}