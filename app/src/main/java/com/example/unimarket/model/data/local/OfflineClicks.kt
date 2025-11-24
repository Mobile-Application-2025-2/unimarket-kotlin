package com.example.unimarket.model.data.local

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

class OfflineClicks(private val ctx: Context) {
    private val file = File(ctx.filesDir, "category_clicks_queue.json")

    fun enqueue(docId: String) {
        val arr = readArray()
        arr.put(JSONObject(mapOf("docId" to docId, "ts" to System.currentTimeMillis())))
        file.writeText(arr.toString())
    }

    fun drain(): List<String> {
        val arr = readArray()
        val list = mutableListOf<String>()
        for (i in 0 until arr.length()) list += arr.getJSONObject(i).getString("docId")
        file.writeText("[]") // limpia
        return list
    }

    private fun readArray(): JSONArray =
        if (file.exists()) runCatching { JSONArray(file.readText()) }.getOrElse { JSONArray() }
        else JSONArray()
}