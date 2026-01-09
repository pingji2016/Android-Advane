package com.example.myapplication.game.objects

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope

class Hole(
    var x: Float = 0f,
    var y: Float = 0f,
    val radius: Float = 40f
) {
    fun draw(scope: DrawScope) {
        scope.drawCircle(
            color = Color.Black,
            center = Offset(x, y),
            radius = radius
        )
    }
    
    fun contains(ballX: Float, ballY: Float): Boolean {
        val dx = ballX - x
        val dy = ballY - y
        return dx * dx + dy * dy < radius * radius
    }
}
