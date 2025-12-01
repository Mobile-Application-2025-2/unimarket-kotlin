package com.example.unimarket.model.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.unimarket.model.data.local.dao.CategoriesDao
import com.example.unimarket.model.data.local.dao.TopBusinessDao
import com.example.unimarket.model.data.local.dao.TopProductDao
import com.example.unimarket.model.data.local.dao.GlobalTopProductDao
import com.example.unimarket.model.data.local.entity.CategoryLocalEntity
import com.example.unimarket.model.data.local.entity.TopBusinessLocalEntity
import com.example.unimarket.model.data.local.entity.TopProductLocalEntity
import com.example.unimarket.model.data.local.entity.GlobalTopProductEntity

@Database(
    entities = [
        TopBusinessLocalEntity::class,
        TopProductLocalEntity::class,
        CategoryLocalEntity::class,
        GlobalTopProductEntity::class
    ],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun topBusinessDao(): TopBusinessDao
    abstract fun topProductDao(): TopProductDao
    abstract fun categoryLocalDao(): CategoriesDao
    abstract fun globalTopProductDao(): GlobalTopProductDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null
        fun getInstance(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "unimarket.db"
                )
                .fallbackToDestructiveMigration()
                .build().also { INSTANCE = it }
            }
    }
}