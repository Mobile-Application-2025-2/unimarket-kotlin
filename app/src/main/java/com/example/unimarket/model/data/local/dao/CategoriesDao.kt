package com.example.unimarket.model.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.unimarket.model.data.local.entity.CategoryLocalEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoriesDao {

    @Query("SELECT * FROM categories_local ORDER BY name")
    fun observeAll(): Flow<List<CategoryLocalEntity>>

    @Query("DELETE FROM categories_local")
    suspend fun clearAll()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<CategoryLocalEntity>)
}