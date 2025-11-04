package com.example.unimarket.model.data.local.dao

import androidx.room.*
import com.example.unimarket.model.data.local.entity.BusinessLocalEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BusinessLocalDao {
    @Query("SELECT * FROM business_local ORDER BY name")
    fun observeAll(): Flow<List<BusinessLocalEntity>>

    @Query("DELETE FROM business_local")
    suspend fun clear()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<BusinessLocalEntity>)
}