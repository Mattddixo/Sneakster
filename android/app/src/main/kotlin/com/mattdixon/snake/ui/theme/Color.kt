package com.mattdixon.snake.ui.theme

import androidx.compose.ui.graphics.Color

val ArenaBackground = Color(0xFF0B1512)
val ArenaGrid = Color(0xFF16221D)
val SnakeHead = Color(0xFF7CFFB2)
val SnakeTailFar = Color(0xFF1D6B45)
val ObstacleColor = Color(0xFFFF5C5C)
/** The exposed, destroyable rear arc drawn on every obstacle — deliberately the same inviting
 * green as the vehicle itself, so "safe to ram" reads at a glance against the danger-red body. */
val ObstacleBackSafe = Color(0xFF6CFFA0)

// One hue per board pickup, spread around the color wheel so no two are ever mistaken for each
// other at a glance - these three used to be SLOW_DOWN 0xFF4CC2FF, DIAMOND_ROTATE 0xFF5CE1E6,
// and SHIELD 0xFF4CD6FF, all blue/cyan variants sitting right on top of each other.
val PowerUpSlowDown = Color(0xFF4C8DFF) // blue - the one pickup that keeps a "cold" association
val PowerUpDiamondRotate = Color(0xFFFF4CD6) // magenta
val PowerUpShield = Color(0xFFFFA13D) // orange
val TokenColor = Color(0xFFFFD700) // gold
/** Every shared-pool effect (gift, prank, or anything added to the shop later) renders as this
 * one color on the board — which specific effect a pull turns out to be is meant to be a
 * surprise, so nothing should hint at it before it's collected. */
val PoolEffectColor = Color(0xFFB98CFF)

val AccentPrimary = Color(0xFF39FF88)
val SurfaceDark = Color(0xFF0F1A16)
val SurfaceRaised = Color(0xFF16231D)
val TextPrimary = Color(0xFFEAF7EF)
val TextMuted = Color(0xFF8FA79A)
val DangerColor = Color(0xFFFF5C5C)
