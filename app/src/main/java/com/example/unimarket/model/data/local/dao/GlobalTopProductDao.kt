package com.example.unimarket.model.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.unimarket.model.data.local.entity.GlobalTopProductEntity

@Dao
interface GlobalTopProductDao {

    @Query("SELECT * FROM global_top_products ORDER BY rating DESC LIMIT :limit")
    suspend fun getTopProducts(limit: Int): List<GlobalTopProductEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(products: List<GlobalTopProductEntity>)

    @Query("DELETE FROM global_top_products")
    suspend fun clearAll()
}