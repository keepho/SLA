package com.sla

import android.graphics.Bitmap
import android.util.Log
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.korean.KoreanTextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class ScreenAnalyzer {
    
    private val textRecognizer = TextRecognition.getClient(
        KoreanTextRecognizerOptions.Builder().build()
    )
    
    suspend fun analyze(bitmap: Bitmap?): ScreenAnalysis {
        if (bitmap == null) return ScreenAnalysis.EMPTY
        
        return suspendCancellableCoroutine { continuation ->
            val image = InputImage.fromBitmap(bitmap, 0)
            
            textRecognizer.process(image)
                .addOnSuccessListener { result ->
                    val elements = result.textBlocks.flatMap { block ->
                        block.lines.map { line ->
                            UIElement(
                                text = line.text,
                                boundingBox = line.boundingBox,
                                type = classifyText(line.text)
                            )
                        }
                    }
                    
                    val analysis = ScreenAnalysis(
                        elements = elements,
                        screenState = classifyScreen(elements),
                        timestamp = System.currentTimeMillis()
                    )
                    continuation.resume(analysis)
                }
                .addOnFailureListener { e ->
                    Log.e("SLA", "분석 오류: ${e.message}")
                    continuation.resume(ScreenAnalysis.EMPTY)
                }
        }
    }
    
    private fun classifyText(text: String): UIElementType {
        return when {
            text.contains(Regex("로그인|확인|다음|전송|검색|시작|닫기", RegexOption.IGNORE_CASE)) -> UIElementType.BUTTON
            text.contains("@") -> UIElementType.EMAIL
            text.length > 30 -> UIElementType.PARAGRAPH
            text.length < 10 -> UIElementType.LABEL
            else -> UIElementType.GENERAL
        }
    }
    
    private fun classifyScreen(elements: List<UIElement>): ScreenState {
        val types = elements.map { it.type }
        return when {
            types.contains(UIElementType.BUTTON) && types.contains(UIElementType.EMAIL) -> ScreenState.LOGIN
            types.contains(UIElementType.BUTTON) && elements.size > 5 -> ScreenState.HOME
            else -> ScreenState.GENERAL
        }
    }
}

data class ScreenAnalysis(
    val elements: List<UIElement>,
    val screenState: ScreenState,
    val timestamp: Long
) {
    companion object {
        val EMPTY = ScreenAnalysis(emptyList(), ScreenState.UNKNOWN, 0)
    }
}

data class UIElement(
    val text: String,
    val boundingBox: android.graphics.Rect?,
    val type: UIElementType
)

enum class UIElementType { BUTTON, EMAIL, LABEL, PARAGRAPH, GENERAL }
enum class ScreenState { HOME, LOGIN, GENERAL, UNKNOWN }
