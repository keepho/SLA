package com.sla.model

sealed class UserAction {
    abstract val timestamp: Long
    
    data class Click(
        val x: Float,
        val y: Float,
        val targetText: String?,
        val className: String?,
        override val timestamp: Long = System.currentTimeMillis()
    ) : UserAction()
    
    data class Swipe(
        val startX: Float,
        val startY: Float,
        val endX: Float,
        val endY: Float,
        val direction: SwipeDirection,
        override val timestamp: Long = System.currentTimeMillis()
    ) : UserAction()
    
    data class TypeText(
        val text: String,
        val targetHint: String?,
        override val timestamp: Long = System.currentTimeMillis()
    ) : UserAction()
}

enum class SwipeDirection { UP, DOWN, LEFT, RIGHT }
