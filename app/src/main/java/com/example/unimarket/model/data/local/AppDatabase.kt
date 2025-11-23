package com.example.unimarket.model.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.unimarket.model.data.local.dao.CategoriesDao
import com.example.unimarket.model.data.local.dao.TopBusinessDao
import com.example.unimarket.model.data.local.entity.CategoryLocalEntity
import com.example.unimarket.model.data.local.entity.TopBusinessLocalEntity

@Database(
    entities = [
        CategoryLocalEntity::class,
        TopBusinessLocalEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun categoriesDao(): CategoriesDao
    abstract fun topBusinessDao(): TopBusinessDao

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
