package com.sla

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.graphics.Rect
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.sla.model.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ScreenLearningAccessibilityService : AccessibilityService() {
    
    companion object {
        const val TAG = "SLA"
        var instance: ScreenLearningAccessibilityService? = null
        var isLearning = true
    }
    
    private lateinit var engine: PatternEngine
    private lateinit var store: PatternStore
    private val scope = CoroutineScope(Dispatchers.Default)
    
    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        store = PatternStore(applicationContext)
        engine = PatternEngine(store)
        
        serviceInfo = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPE_VIEW_CLICKED or
                    AccessibilityEvent.TYPE_VIEW_SCROLLED or
                    AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED or
                    AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
        }
        
        Log.d(TAG, "SLA 서비스 시작됨")
    }
    
    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        when (event.eventType) {
            AccessibilityEvent.TYPE_VIEW_CLICKED -> {
                val node = event.source
                val bounds = Rect()
                node?.getBoundsInScreen(bounds)
                val action = UserAction.Click(
                    x = (bounds.left + bounds.right) / 2f,
                    y = (bounds.top + bounds.bottom) / 2f,
                    targetText = node?.text?.toString(),
                    className = node?.className?.toString()
                )
                handleAction(action)
            }
            
            AccessibilityEvent.TYPE_VIEW_SCROLLED -> {
                val action = UserAction.Swipe(
                    startX = 540f, startY = 1200f,
                    endX = 540f, endY = 600f,
                    direction = SwipeDirection.UP
                )
                handleAction(action)
            }
            
            AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED -> {
                val action = UserAction.TypeText(
                    text = event.text?.joinToString() ?: "",
                    targetHint = event.source?.hintText?.toString()
                )
                handleAction(action)
            }
        }
    }
    
    private fun handleAction(action: UserAction) {
        val state = detectScreenState()
        
        if (isLearning) {
            engine.observe(action, state)
            Log.d(TAG, "[학습] ${action.javaClass.simpleName}")
        } else {
            scope.launch {
                val prediction = engine.predict(state)
                prediction?.let { execute(it) }
            }
        }
    }
    
    private fun detectScreenState(): ScreenState {
        val root = rootInActiveWindow ?: return ScreenState.UNKNOWN
        val packageName = root.packageName?.toString() ?: ""
        
        return when {
            packageName.contains("launcher") -> ScreenState.HOME
            root.findAccessibilityNodeInfosByText("로그인").isNotEmpty() -> ScreenState.LOGIN
            else -> ScreenState.GENERAL
        }
    }
    
    fun execute(action: PredictedAction) {
        when (action) {
            is PredictedAction.Click -> performClick(action.x, action.y)
            is PredictedAction.Swipe -> performSwipe(action.startX, action.startY, action.endX, action.endY)
            is PredictedAction.TypeText -> performType(action.text)
        }
    }
    
    private fun performClick(x: Float, y: Float) {
        val path = Path().apply { moveTo(x, y) }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, 100))
            .build()
        dispatchGesture(gesture, null, null)
        Log.d(TAG, "[실행] 클릭 ($x, $y)")
    }
    
    private fun performSwipe(sx: Float, sy: Float, ex: Float, ey: Float) {
        val path = Path().apply {
            moveTo(sx, sy)
            lineTo(ex, ey)
        }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, 300))
            .build()
        dispatchGesture(gesture, null, null)
        Log.d(TAG, "[실행] 스와이프")
    }
    
    private fun performType(text: String) {
        val focused = rootInActiveWindow?.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
        val args = android.os.Bundle().apply {
            putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
        }
        focused?.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
        Log.d(TAG, "[실행] 텍스트 입력: $text")
    }
    
    override fun onInterrupt() {}
}
