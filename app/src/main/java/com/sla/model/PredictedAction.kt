package com.sla.model

sealed class PredictedAction {
    abstract val confidence: Float
    
    data class Click(
        val x: Float,
        val y: Float,
        override val confidence: Float
    ) : PredictedAction()
    
    data class Swipe(
        val startX: Float,
        val startY: Float,
        val endX: Float,
        val endY: Float,
        override val confidence: Float
    ) : PredictedAction()
    
    data class TypeText(
        val text: String,
        override val confidence: Float
    ) : PredictedAction()
}
