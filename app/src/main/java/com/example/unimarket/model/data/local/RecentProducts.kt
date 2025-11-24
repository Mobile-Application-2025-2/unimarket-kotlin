package com.example.unimarket.model.data.local

import android.content.Context
import org.json.JSONArray
import java.io.File

class RecentProducts(ctx: Context, businessId: String) {
    private val file = File(ctx.filesDir, "recent_products_$businessId.json")

    fun save(productIds: List<String>) {
        val arr = JSONArray()
        productIds.forEach { arr.put(it) }
        file.writeText(arr.toString())
    }

    fun load(): List<String> {
        if (!file.exists()) return emptyList()
        return runCatching {
            val arr = JSONArray(file.readText())
            (0 until arr.length()).map { arr.getString(it) }
        }.getOrDefault(emptyList())
    }
}