package com.mattdixon.snake.engine

enum class PowerUpType(
    val scoreBonus: Int,
    val effectDurationSeconds: Float,
    /** If set, a spawned instance disappears on its own after this many seconds if never
     * collected. Null (the default) matches every original power-up's behavior: sit there
     * until picked up. */
    val lifetimeSeconds: Float? = null,
    /** Never chosen by the normal random spawn cycle — only ever placed deliberately, e.g. by
     * [GameEngine.placeSpecificPowerUp] when a shared-pool pull needs to appear on the board. */
    val poolExclusive: Boolean = false,
) {
    /** Boosts speed for a short burst — more score per second, more risk. */
    SPEED_UP(scoreBonus = 15, effectDurationSeconds = 5f),

    /** Eases speed off for a few seconds so a tight spot can be threaded safely. */
    SLOW_DOWN(scoreBonus = 10, effectDurationSeconds = 5f),

    /** Risk/reward: drops fresh obstacles elsewhere on the field in exchange for bonus points -
     * more hazards to dodge, but also more exposed backs to ram for a destroy bonus. */
    SPAWN_OBSTACLE(scoreBonus = 30, effectDurationSeconds = 0f),

    /** Purely visual: the UI spins the arena from square to diamond and back. Doesn't touch
     * physics or collision at all — the engine just tracks that it's active, like any other
     * timed effect, so the UI layer can read it from [GameState.activeEffects]. */
    DIAMOND_ROTATE(scoreBonus = 20, effectDurationSeconds = 6f),

    /** The currency collectible: a small score bonus, and it credits the player's persistent
     * token balance — the app layer is responsible for that half, by reacting to the
     * [GameEvent.PowerUpCollected] event this produces just like any other pickup. Unlike
     * every other power-up, it expires on its own if not reached in time. */
    TOKEN(scoreBonus = 5, effectDurationSeconds = 0f, lifetimeSeconds = 9f),

    /** A gift from another player's shared-pool contribution: bonus points plus a modest speed
     * boost. Never spawns on its own — only via [GameEngine.placeSpecificPowerUp]. */
    SHARED_GIFT(scoreBonus = 50, effectDurationSeconds = 6f, poolExclusive = true),

    /** Also from the shared pool, but mischievous: bonus points, but it drops extra obstacles
     * just like SPAWN_OBSTACLE does. Never spawns on its own. */
    SHARED_PRANK(scoreBonus = 25, effectDurationSeconds = 5f, poolExclusive = true),
}

data class PowerUp(
    val id: Long,
    val position: Vec2,
    val type: PowerUpType,
    val radius: Float,
    val spawnedAt: Float,
)
