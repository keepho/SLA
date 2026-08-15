package com.sla.ui

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.sla.PatternStore
import com.sla.R
import com.sla.ScreenLearningAccessibilityService

class ControlActivity : AppCompatActivity() {
    
    private lateinit var tvPatternCount: TextView
    private lateinit var tvActionCount: TextView
    private lateinit var tvStatus: TextView
    private lateinit var tvLog: TextView
    private lateinit var btnLearn: Button
    private lateinit var btnExecute: Button
    private lateinit var btnStop: Button
    
    private lateinit var store: PatternStore
    private var isLearning = false
    private var isExecuting = false
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_control)
        
        store = PatternStore(this)
        initViews()
        updateStats()
        
        if (!isAccessibilityEnabled()) {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }
    }
    
    private fun initViews() {
        tvPatternCount = findViewById(R.id.tvPatternCount)
        tvActionCount = findViewById(R.id.tvActionCount)
        tvStatus = findViewById(R.id.tvStatus)
        tvLog = findViewById(R.id.tvLog)
        btnLearn = findViewById(R.id.btnLearn)
        btnExecute = findViewById(R.id.btnExecute)
        btnStop = findViewById(R.id.btnStop)
        
        btnLearn.setOnClickListener {
            isLearning = true
            isExecuting = false
            ScreenLearningAccessibilityService.isLearning = true
            tvStatus.text = "학습 중"
            tvStatus.setTextColor(getColor(android.R.color.holo_blue_bright))
            addLog("학습 모드 시작")
            btnStop.visibility = Button.VISIBLE
        }
        
        btnExecute.setOnClickListener {
            isLearning = false
            isExecuting = true
            ScreenLearningAccessibilityService.isLearning = false
            tvStatus.text = "자동 실행"
            tvStatus.setTextColor(getColor(android.R.color.holo_green_light))
            addLog("실행 모드 시작")
            btnStop.visibility = Button.VISIBLE
        }
        
        btnStop.setOnClickListener {
            isLearning = false
            isExecuting = false
            tvStatus.text = "대기"
            tvStatus.setTextColor(getColor(android.R.color.darker_gray))
            addLog("정지됨")
            btnStop.visibility = Button.GONE
        }
    }
    
    private fun updateStats() {
        val patterns = store.getAllPatterns()
        tvPatternCount.text = patterns.size.toString()
    }
    
    private fun addLog(message: String) {
        val time = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.KOREA)
            .format(java.util.Date())
        tvLog.append("[$time] $message\n")
    }
    
    private fun isAccessibilityEnabled(): Boolean {
        val service = packageName + "/" + ScreenLearningAccessibilityService::class.java.canonicalName
        val enabledServices = Settings.Secure.getString(contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
        return enabledServices?.contains(service) == true
    }
}
