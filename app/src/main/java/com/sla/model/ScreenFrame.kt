package com.sla.model

import android.graphics.Bitmap

data class ScreenFrame(
    val bitmap: Bitmap?,
    val timestamp: Long,
    val width: Int = 1080,
    val height: Int = 2400
) {
    companion object {
        val EMPTY = ScreenFrame(null, 0)
    }
}
