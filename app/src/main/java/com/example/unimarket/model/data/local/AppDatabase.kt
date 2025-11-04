package com.example.unimarket.model.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.unimarket.model.data.local.dao.BusinessLocalDao
import com.example.unimarket.model.data.local.entity.BusinessLocalEntity

@Database(
    entities = [BusinessLocalEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun businessLocalDao(): BusinessLocalDao
}