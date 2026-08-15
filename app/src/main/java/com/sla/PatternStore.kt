package com.sla

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.sla.model.ActionPattern

class PatternStore(context: Context) {
    
    private val prefs: SharedPreferences = context.getSharedPreferences("sla_patterns", Context.MODE_PRIVATE)
    private val gson = Gson()
    
    fun savePattern(pattern: ActionPattern) {
        val patterns = getAllPatterns().toMutableList()
        val existing = patterns.find { it.contextHash == pattern.contextHash }
        
        if (existing != null) {
            existing.frequency++
            existing.lastExecuted = System.currentTimeMillis()
        } else {
            pattern.id = patterns.size + 1
            patterns.add(pattern)
        }
        
        prefs.edit().putString("patterns", gson.toJson(patterns)).apply()
    }
    
    fun getAllPatterns(): List<ActionPattern> {
        val json = prefs.getString("patterns", "[]") ?: "[]"
        val type = object : TypeToken<List<ActionPattern>>() {}.type
        return gson.fromJson(json, type)
    }
    
    fun findSimilar(contextHash: String, minFrequency: Int = 1): List<ActionPattern> {
        return getAllPatterns()
            .filter { it.contextHash == contextHash && it.frequency >= minFrequency }
            .sortedByDescending { it.frequency }
    }
    
    fun clear() {
        prefs.edit().clear().apply()
    }
}
