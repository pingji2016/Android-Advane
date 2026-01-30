package com.example.myapplication.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Context
import android.content.Intent
import android.graphics.Path
import android.graphics.Rect
import android.media.AudioManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import org.json.JSONObject
import kotlin.math.max
import kotlin.math.min

class ControlService : AccessibilityService() {

    companion object {
        private const val TAG = "ControlService"
        var instance: ControlService? = null
            private set
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        Log.d(TAG, "ControlService connected")
    }

    override fun onUnbind(intent: Intent?): Boolean {
        instance = null
        Log.d(TAG, "ControlService unbound")
        return super.onUnbind(intent)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // We don't need to process events for now, just performing actions
    }

    override fun onInterrupt() {
        Log.w(TAG, "ControlService interrupted")
    }

    fun executeCommand(jsonCmd: String) {
        try {
            val json = JSONObject(jsonCmd)
            val type = json.optString("type")
            
            when (type) {
                "click" -> handleClick(json)
                "scroll" -> handleScroll(json)
                "long_click" -> handleLongClick(json)
                "double_click" -> handleDoubleClick(json)
                "text" -> handleText(json)
                "volume" -> handleVolume(json)
                "back" -> performGlobalAction(GLOBAL_ACTION_BACK)
                "home" -> performGlobalAction(GLOBAL_ACTION_HOME)
                "recent" -> performGlobalAction(GLOBAL_ACTION_RECENTS)
                else -> Log.w(TAG, "Unknown command type: $type")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to execute command: $jsonCmd", e)
        }
    }

    private fun handleText(json: JSONObject) {
        val text = json.optString("text", "")
        if (text.isEmpty()) return

        val root = rootInActiveWindow ?: return
        val focus = root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
        
        if (focus != null && focus.isEditable) {
            val arguments = Bundle()
            arguments.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
            focus.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
            focus.recycle()
            Log.d(TAG, "Text set to: $text")
        } else {
            Log.w(TAG, "No editable focus found")
        }
    }

    private fun handleVolume(json: JSONObject) {
        val action = json.optString("action")
        val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        
        when (action) {
            "up" -> {
                audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI)
                Log.d(TAG, "Volume Up")
            }
            "down" -> {
                audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI)
                Log.d(TAG, "Volume Down")
            }
            "mute" -> {
                audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_TOGGLE_MUTE, AudioManager.FLAG_SHOW_UI)
                Log.d(TAG, "Volume Mute Toggle")
            }
        }
    }

    private fun handleClick(json: JSONObject) {
        val x = json.optDouble("x", 0.0).toFloat()
        val y = json.optDouble("y", 0.0).toFloat()
        
        val screenWidth = resources.displayMetrics.widthPixels
        val screenHeight = resources.displayMetrics.heightPixels
        
        val actualX = (x * screenWidth).coerceIn(0f, screenWidth.toFloat())
        val actualY = (y * screenHeight).coerceIn(0f, screenHeight.toFloat())

        Log.d(TAG, "Click at: $actualX, $actualY")

        val path = Path()
        path.moveTo(actualX, actualY)
        
        val builder = GestureDescription.Builder()
        val gesture = builder
            .addStroke(GestureDescription.StrokeDescription(path, 0, 100))
            .build()
            
        dispatchGesture(gesture, null, null)
    }

    private fun handleScroll(json: JSONObject) {
        val x = json.optDouble("x", 0.5).toFloat()
        val y = json.optDouble("y", 0.5).toFloat()
        val dx = json.optDouble("dx", 0.0).toFloat()
        val dy = json.optDouble("dy", 0.0).toFloat()
        val duration = json.optLong("duration", 300)

        val screenWidth = resources.displayMetrics.widthPixels
        val screenHeight = resources.displayMetrics.heightPixels

        val startX = (x * screenWidth).coerceIn(0f, screenWidth.toFloat())
        val startY = (y * screenHeight).coerceIn(0f, screenHeight.toFloat())
        val endX = ((x + dx) * screenWidth).coerceIn(0f, screenWidth.toFloat())
        val endY = ((y + dy) * screenHeight).coerceIn(0f, screenHeight.toFloat())

        Log.d(TAG, "Scroll from ($startX, $startY) to ($endX, $endY)")

        val path = Path()
        path.moveTo(startX, startY)
        path.lineTo(endX, endY)

        val builder = GestureDescription.Builder()
        val gesture = builder
            .addStroke(GestureDescription.StrokeDescription(path, 0, duration))
            .build()

        dispatchGesture(gesture, null, null)
    }

    private fun handleLongClick(json: JSONObject) {
        val x = json.optDouble("x", 0.5).toFloat()
        val y = json.optDouble("y", 0.5).toFloat()
        val duration = json.optLong("duration", 600)

        val screenWidth = resources.displayMetrics.widthPixels
        val screenHeight = resources.displayMetrics.heightPixels
        val actualX = (x * screenWidth).coerceIn(0f, screenWidth.toFloat())
        val actualY = (y * screenHeight).coerceIn(0f, screenHeight.toFloat())

        Log.d(TAG, "LongClick at: $actualX, $actualY duration=$duration")

        val path = Path().apply { moveTo(actualX, actualY) }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, duration))
            .build()

        dispatchGesture(gesture, null, null)
    }

    private fun handleDoubleClick(json: JSONObject) {
        val x = json.optDouble("x", 0.5).toFloat()
        val y = json.optDouble("y", 0.5).toFloat()
        val interval = json.optLong("interval", 120)

        val screenWidth = resources.displayMetrics.widthPixels
        val screenHeight = resources.displayMetrics.heightPixels
        val actualX = (x * screenWidth).coerceIn(0f, screenWidth.toFloat())
        val actualY = (y * screenHeight).coerceIn(0f, screenHeight.toFloat())

        fun tapGesture(): GestureDescription {
            val path = Path().apply { moveTo(actualX, actualY) }
            return GestureDescription.Builder()
                .addStroke(GestureDescription.StrokeDescription(path, 0, 100))
                .build()
        }

        val handler = Handler(Looper.getMainLooper())
        dispatchGesture(tapGesture(), object : GestureResultCallback() {
            override fun onCompleted(gestureDescription: GestureDescription?) {
                handler.postDelayed({
                    dispatchGesture(tapGesture(), null, null)
                }, interval)
            }
        }, null)
    }
}
