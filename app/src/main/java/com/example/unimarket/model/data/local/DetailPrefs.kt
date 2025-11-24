package com.example.unimarket.model.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore("detail_prefs")

class DetailPrefs(private val ctx: Context) {
    private fun key(businessId: String) = stringPreferencesKey("detail_filter_$businessId")
    fun filterFlow(businessId: String) = ctx.dataStore.data.map { it[key(businessId)] ?: "all" }
    suspend fun saveFilter(businessId: String, tag: String) =
        ctx.dataStore.edit { it[key(businessId)] = tag }
}