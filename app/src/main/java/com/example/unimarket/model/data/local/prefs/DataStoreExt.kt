package com.example.unimarket.model.data.local.prefs

import android.content.Context
import androidx.datastore.preferences.preferencesDataStore

val Context.dataStore by preferencesDataStore(name = "unimarket_prefs")