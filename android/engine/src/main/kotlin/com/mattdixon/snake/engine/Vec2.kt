package com.mattdixon.snake.engine

import kotlin.math.sqrt

data class Vec2(val x: Float, val y: Float) {
    operator fun plus(other: Vec2) = Vec2(x + other.x, y + other.y)
    operator fun minus(other: Vec2) = Vec2(x - other.x, y - other.y)
    operator fun times(scalar: Float) = Vec2(x * scalar, y * scalar)

    fun length(): Float = sqrt(x * x + y * y)

    fun distanceTo(other: Vec2): Float = (this - other).length()

    companion object {
        val ZERO = Vec2(0f, 0f)

        fun heading(angleRadians: Float): Vec2 =
            Vec2(kotlin.math.cos(angleRadians), kotlin.math.sin(angleRadians))
    }
}
