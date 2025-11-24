package com.example.unimarket.model.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore("home_prefs")

object HomeKeys { val FILTER = stringPreferencesKey("home_filter") }

class HomePrefs(private val ctx: Context) {
    val filterFlow = ctx.dataStore.data.map { it[HomeKeys.FILTER] ?: "ALL" }
    suspend fun saveFilter(tag: String) = ctx.dataStore.edit { it[HomeKeys.FILTER] = tag }
}