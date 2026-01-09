package com.example.myapplication.game

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.example.myapplication.game.objects.Ball
import com.example.myapplication.game.objects.Bar
import com.example.myapplication.game.objects.HandleType
import com.example.myapplication.game.objects.Hole

class World {
    var width: Float = 1080f
    var height: Float = 1920f
    
    val bar = Bar()
    val ball = Ball()
    val hole = Hole()
    
    private var initialized = false
    var gameState = "PLAYING" // PLAYING, WON

    private var activeHandle: HandleType = HandleType.NONE

    fun init(w: Float, h: Float) {
        if (w <= 0 || h <= 0) return
        width = w
        height = h
        
        // Setup initial positions
        val centerX = width / 2
        
        // Bar at top quarter
        val barY = height * 0.25f
        bar.x1 = centerX - 300f
        bar.x2 = centerX + 300f
        bar.init(barY)
        
        // Ball on bar
        ball.x = centerX
        ball.y = barY - ball.radius - bar.thickness / 2 - 1
        ball.vx = 0f
        ball.vy = 0f
        
        // Hole at bottom
        hole.x = centerX
        hole.y = height - 200f
        
        initialized = true
        gameState = "PLAYING"
    }

    fun step(dtNs: Long) {
        if (!initialized) return
        if (gameState != "PLAYING") return

        val dt = dtNs / 1_000_000_000f // to seconds
        
        // Update Bar physics (smooth movement)
        bar.update(dt)
        
        // Update Ball physics
        ball.update(dt, bar)
        
        // Check win
        if (hole.contains(ball.x, ball.y)) {
            gameState = "WON"
        }
        
        // Check loss (off screen) or just reset if too far down
        if (ball.y > height + 200) {
            // Reset ball to top
            ball.x = width / 2
            ball.y = (bar.y1 + bar.y2) / 2 - 100 // Approximation
            ball.vx = 0f
            ball.vy = 0f
        }
    }

    fun draw(scope: DrawScope) {
        if (!initialized || width != scope.size.width || height != scope.size.height) {
             if (!initialized) {
                 init(scope.size.width, scope.size.height)
             }
        }
        
        hole.draw(scope)
        bar.draw(scope, activeHandle)
        ball.draw(scope)
        
        if (gameState == "WON") {
             scope.drawCircle(Color.Green.copy(alpha=0.3f), radius=200f, center=Offset(width/2, height/2))
        }
    }
    
    fun onTouchStart(x: Float, y: Float) {
        if (!initialized) return
        activeHandle = bar.getHandleAt(x, y)
        if (activeHandle != HandleType.NONE) {
            onTouchMove(x, y)
        }
    }
    
    fun onTouchMove(x: Float, y: Float) {
        if (!initialized) return
        if (activeHandle == HandleType.LEFT) {
             bar.setTarget(leftY = y.coerceIn(0f, height), rightY = null)
        } else if (activeHandle == HandleType.RIGHT) {
             bar.setTarget(leftY = null, rightY = y.coerceIn(0f, height))
        }
    }
    
    fun onTouchEnd() {
        activeHandle = HandleType.NONE
    }
}
