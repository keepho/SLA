package com.sla

import android.util.Log
import com.sla.model.*
import kotlinx.coroutines.*
import java.security.MessageDigest

class PatternEngine(private val store: PatternStore) {
    
    private val currentSequence = mutableListOf<TimedAction>()
    private var sequenceStartTime: Long = 0
    var isLearning = true
    
    private val scope = CoroutineScope(Dispatchers.Default)
    
    fun observe(action: UserAction, screenState: ScreenState) {
        val now = System.currentTimeMillis()
        
        if (currentSequence.isEmpty()) {
            sequenceStartTime = now
        }
        
        val timedAction = TimedAction(
            action = action,
            timeDelta = now - sequenceStartTime,
            screenHash = screenState.name
        )
        
        currentSequence.add(timedAction)
        
        if (currentSequence.size >= 3) {
            storePattern()
        }
        
        scope.launch {
            delay(5000)
            if (currentSequence.isNotEmpty() && now - currentSequence.last().action.timestamp > 5000) {
                finalizeSequence()
            }
        }
    }
    
    private fun storePattern() {
        val hash = computeHash(currentSequence.first().screenHash)
        val pattern = ActionPattern(
            sequence = currentSequence.toList(),
            contextHash = hash
        )
        store.savePattern(pattern)
        Log.d("SLA", "패턴 저장됨: ${currentSequence.size}개 동작")
    }
    
    fun predict(screenState: ScreenState): PredictedAction? {
        if (isLearning) return null
        
        val hash = computeHash(screenState.name)
        val patterns = store.findSimilar(hash, minFrequency = 1)
        
        if (patterns.isEmpty()) return null
        
        val best = patterns.first()
        val nextAction = best.sequence.getOrNull(currentSequence.size) ?: return null
        
        return when (val action = nextAction.action) {
            is UserAction.Click -> PredictedAction.Click(action.x, action.y, 0.8f)
            is UserAction.Swipe -> PredictedAction.Swipe(
                action.startX, action.startY, action.endX, action.endY, 0.8f
            )
            is UserAction.TypeText -> PredictedAction.TypeText(action.text, 0.8f)
        }
    }
    
    private fun finalizeSequence() {
        if (currentSequence.size >= 2) {
            storePattern()
        }
        currentSequence.clear()
    }
    
    private fun computeHash(input: String): String {
        return MessageDigest.getInstance("MD5")
            .digest(input.toByteArray())
            .joinToString("") { "%02x".format(it) }
    }
    
    fun clear() {
        currentSequence.clear()
        store.clear()
    }
}
