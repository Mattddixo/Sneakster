package com.mattdixon.snake.engine

import kotlin.math.atan2
import kotlin.math.sqrt

data class Vec2(val x: Float, val y: Float) {
    operator fun plus(other: Vec2) = Vec2(x + other.x, y + other.y)
    operator fun minus(other: Vec2) = Vec2(x - other.x, y - other.y)
    operator fun times(scalar: Float) = Vec2(x * scalar, y * scalar)

    fun length(): Float = sqrt(x * x + y * y)

    fun distanceTo(other: Vec2): Float = (this - other).length()

    /** This vector's direction, in radians, using the same convention as [heading]. */
    fun angleRadians(): Float = atan2(y, x)

    /** This vector scaled to length 1, or [ZERO] itself if it has no length to normalize. */
    fun normalized(): Vec2 {
        val len = length()
        return if (len > 0f) this * (1f / len) else this
    }

    companion object {
        val ZERO = Vec2(0f, 0f)

        fun heading(angleRadians: Float): Vec2 =
            Vec2(kotlin.math.cos(angleRadians), kotlin.math.sin(angleRadians))
    }
}
