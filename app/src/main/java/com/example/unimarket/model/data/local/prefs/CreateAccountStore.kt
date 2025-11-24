package com.example.unimarket.model.data.local.prefs

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

object CreateAccountStore {
    private val KEY_NAME  = stringPreferencesKey("create_name")
    private val KEY_EMAIL = stringPreferencesKey("create_email")
    private val KEY_TYPE  = stringPreferencesKey("create_type")

    suspend fun save(ctx: Context, name: String, email: String, type: String) {
        ctx.dataStore.edit { p ->
            p[KEY_NAME]  = name
            p[KEY_EMAIL] = email
            p[KEY_TYPE]  = type
        }
    }

    suspend fun read(ctx: Context): Triple<String?, String?, String?> {
        return ctx.dataStore.data.map { p ->
            Triple(p[KEY_NAME], p[KEY_EMAIL], p[KEY_TYPE])
        }.first()
    }

    suspend fun clear(ctx: Context) {
        ctx.dataStore.edit { it.remove(KEY_NAME); it.remove(KEY_EMAIL); it.remove(KEY_TYPE) }
    }
}