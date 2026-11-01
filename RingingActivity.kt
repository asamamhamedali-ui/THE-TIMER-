package com.thestudytimer

import android.app.Activity
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

class RingingActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val label = intent.getStringExtra("label") ?: "THE STUDY TIMER"
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(32, 32, 32, 32)
            setBackgroundColor(android.graphics.Color.rgb(8, 15, 30))
        }
        layout.addView(TextView(this).apply {
            text = label
            textSize = 30f
            setTextColor(android.graphics.Color.WHITE)
            gravity = Gravity.CENTER
        })
        layout.addView(Button(this).apply {
            text = "Dismiss"
            setOnClickListener { finish() }
        })
        setContentView(layout)
    }
}
