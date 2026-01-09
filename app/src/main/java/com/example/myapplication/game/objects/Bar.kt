package com.example.myapplication.game.objects

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import kotlin.math.min

enum class HandleType {
    NONE, LEFT, RIGHT
}

class Bar(
    var x1: Float = 0f, var y1: Float = 0f,
    var x2: Float = 0f, var y2: Float = 0f,
    val thickness: Float = 20f
) {
    // Target Y positions for smooth movement
    private var targetY1: Float = y1
    private var targetY2: Float = y2
    
    // Physics properties
    var vy1: Float = 0f
    var vy2: Float = 0f

    // Smoothing speed (higher = faster response)
    private val responseSpeed = 20f 
    
    val handleRadius = 40f

    fun init(startY: Float) {
        y1 = startY
        y2 = startY
        targetY1 = startY
        targetY2 = startY
        vy1 = 0f
        vy2 = 0f
    }

    fun setTarget(leftY: Float?, rightY: Float?) {
        if (leftY != null) targetY1 = leftY
        if (rightY != null) targetY2 = rightY
    }
    
    fun getHandleAt(x: Float, y: Float): HandleType {
        // Increase hit area to 3x radius (approx 120px / ~1.5cm) for easier touch
        val hitRadius = handleRadius * 3f
        val touchRadiusSq = hitRadius * hitRadius
        
        val d1 = (x - x1) * (x - x1) + (y - y1) * (y - y1)
        if (d1 <= touchRadiusSq) return HandleType.LEFT
        
        val d2 = (x - x2) * (x - x2) + (y - y2) * (y - y2)
        if (d2 <= touchRadiusSq) return HandleType.RIGHT
        
        return HandleType.NONE
    }

    fun update(dt: Float) {
        // Simple proportional controller (P-controller) for smooth movement
        // y += (target - y) * speed * dt
        
        val dy1 = targetY1 - y1
        // Calculate velocity for physics interactions (optional, if we want to impart momentum)
        val newY1 = y1 + dy1 * min(1f, responseSpeed * dt)
        vy1 = (newY1 - y1) / dt
        y1 = newY1

        val dy2 = targetY2 - y2
        val newY2 = y2 + dy2 * min(1f, responseSpeed * dt)
        vy2 = (newY2 - y2) / dt
        y2 = newY2
    }

    fun draw(scope: DrawScope, activeHandle: HandleType = HandleType.NONE) {
        scope.drawLine(
            color = Color.Gray,
            start = Offset(x1, y1),
            end = Offset(x2, y2),
            strokeWidth = thickness,
            cap = androidx.compose.ui.graphics.StrokeCap.Round
        )
        
        // Draw handles
        val leftColor = if (activeHandle == HandleType.LEFT) Color.Red else Color.Blue
        val rightColor = if (activeHandle == HandleType.RIGHT) Color.Red else Color.Blue
        
        val leftRadius = if (activeHandle == HandleType.LEFT) handleRadius * 1.2f else handleRadius
        val rightRadius = if (activeHandle == HandleType.RIGHT) handleRadius * 1.2f else handleRadius

        scope.drawCircle(leftColor, leftRadius, Offset(x1, y1))
        scope.drawCircle(rightColor, rightRadius, Offset(x2, y2))
        
        // Inner detail
        scope.drawCircle(Color.White, leftRadius * 0.4f, Offset(x1, y1))
        scope.drawCircle(Color.White, rightRadius * 0.4f, Offset(x2, y2))
    }
}
