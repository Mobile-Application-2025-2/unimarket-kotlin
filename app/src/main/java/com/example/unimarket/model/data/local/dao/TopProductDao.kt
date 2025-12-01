package com.example.unimarket.model.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.example.unimarket.model.data.local.entity.TopProductLocalEntity

@Dao
interface TopProductDao {

    @Query("""
        SELECT * FROM top_product_local
        WHERE businessId = :businessId
        ORDER BY subcategory ASC, rank ASC, rating DESC
    """)
    suspend fun getByBusinessOnce(businessId: String): List<TopProductLocalEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<TopProductLocalEntity>)

    @Query("DELETE FROM top_product_local WHERE businessId = :businessId")
    suspend fun clearForBusiness(businessId: String)

    @Query("""
        DELETE FROM top_product_local
        WHERE businessId = :businessId AND subcategory = :subcategory
    """)
    suspend fun clearFor(businessId: String, subcategory: String)

    @Transaction
    suspend fun replaceForBusinessAndSubcat(
        businessId: String,
        subcategory: String,
        items: List<TopProductLocalEntity>
    ) {
        clearFor(businessId, subcategory)
        if (items.isNotEmpty()) upsertAll(items)
    }
}