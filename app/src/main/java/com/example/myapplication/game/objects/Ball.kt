package com.example.myapplication.game.objects

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import kotlin.math.*

class Ball(
    var x: Float = 0f,
    var y: Float = 0f,
    val radius: Float = 25f
) {
    var vx: Float = 0f
    var vy: Float = 0f
    private val gravity = 1000f // pixels/s^2

    fun update(dt: Float, bar: Bar) {
        // Physics logic: Check collision with bar
        val dx = bar.x2 - bar.x1
        val dy = bar.y2 - bar.y1
        val lengthSq = dx * dx + dy * dy
        
        // Calculate t: projection of ball center onto line segment
        var t = ((x - bar.x1) * dx + (y - bar.y1) * dy) / lengthSq
        
        // If t is within [0, 1], the ball is "over" the bar segment
        val isOverBar = t in 0.0f..1.0f
        
        if (isOverBar) {
            val closestX = bar.x1 + t * dx
            val closestY = bar.y1 + t * dy
            
            val distToLineX = x - closestX
            val distToLineY = y - closestY
            val dist = sqrt(distToLineX * distToLineX + distToLineY * distToLineY)
            
            // Normal vector calculation (pointing "up" / away from bar top)
            // If bar goes left to right, (dy, -dx) points "up" (negative Y) if dy=0.
            val invLen = 1f / sqrt(lengthSq)
            val nx = dy * invLen
            val ny = -dx * invLen
            
            // Tangent vector
            val tx = dx * invLen
            val ty = dy * invLen
            
            // Bar vertical velocity at collision point (Lerp)
            val barVy = bar.vy1 + t * (bar.vy2 - bar.vy1)
            
            // Project bar velocity onto normal
            // Bar velocity vector is (0, barVy)
            val vBarN = barVy * ny
            
            // Check if touching (with some tolerance)
            if (dist <= radius + bar.thickness / 2 + 15f) {
                // Ball normal velocity
                val vBallN = vx * nx + vy * ny
                
                // If ball is moving into bar (vBallN < vBarN) or is close enough to stick
                if (vBallN < vBarN || dist <= radius + bar.thickness / 2 + 5f) {
                    
                    // 1. Position correction: Snap to surface
                    // Only snap if we are "penetrating" or very close
                     x = closestX + nx * (radius + bar.thickness / 2)
                     y = closestY + ny * (radius + bar.thickness / 2)
                    
                    // 2. Velocity update
                    // Match normal velocity to bar (stick/slide)
                    // New velocity = Tangential Component + Bar's Normal Component
                    
                    val vBallT = vx * tx + vy * ty
                    
                    // Add gravity along tangent
                    // Gravity vector (0, g) projected onto T
                    val gDotT = gravity * ty
                    val newVBallT = vBallT + gDotT * dt
                    
                    // Reconstruct velocity
                    vx = tx * newVBallT + nx * vBarN
                    vy = ty * newVBallT + ny * vBarN
                    
                    // Friction
                    vx *= 0.99f
                    vy *= 0.99f
                } else {
                     // Ball is separating from bar (e.g. bar moving down faster than ball)
                     applyGravity(dt)
                }
            } else {
                // Free fall
                 applyGravity(dt)
            }
        } else {
             // Free fall
             applyGravity(dt)
        }
        
        // Apply velocity to position
        x += vx * dt
        y += vy * dt
    }
    
    private fun applyGravity(dt: Float) {
        vy += gravity * dt
    }
    
    fun draw(scope: DrawScope) {
        scope.drawCircle(
            color = Color.Red,
            center = Offset(x, y),
            radius = radius
        )
    }
}
