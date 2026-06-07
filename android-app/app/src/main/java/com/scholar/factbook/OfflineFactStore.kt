package com.scholar.factbook

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File

data class OfflineFact(
    val id: String,
    val text: String,
    val sourceUrl: String,
    val sourceTitle: String,
    val context: String,
    val timestamp: Long = System.currentTimeMillis()
)

class OfflineFactStore(context: Context) {
    private val gson = Gson()
    private val file = File(context.filesDir, "offline_facts.json")

    @Synchronized
    fun getFacts(): List<OfflineFact> {
        if (!file.exists()) return emptyList()
        return try {
            val json = file.readText()
            val type = object : TypeToken<List<OfflineFact>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    @Synchronized
    fun saveFacts(facts: List<OfflineFact>) {
        try {
            val json = gson.toJson(facts)
            file.writeText(json)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @Synchronized
    fun addFact(fact: OfflineFact) {
        val current = getFacts().toMutableList()
        current.add(fact)
        saveFacts(current)
    }

    @Synchronized
    fun removeFact(id: String) {
        val current = getFacts().filter { it.id != id }
        saveFacts(current)
    }

    @Synchronized
    fun clear() {
        if (file.exists()) {
            file.delete()
        }
    }
}
