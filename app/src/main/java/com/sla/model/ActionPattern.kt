package com.sla.model

data class ActionPattern(
    var id: Int = 0,
    val sequence: List<TimedAction>,
    var frequency: Int = 1,
    var lastExecuted: Long = System.currentTimeMillis(),
    val contextHash: String
)

data class TimedAction(
    val action: UserAction,
    val timeDelta: Long,
    val screenHash: String
)
