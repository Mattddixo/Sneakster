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
    /** Eases speed off for a few seconds so a tight spot can be threaded safely. */
    SLOW_DOWN(scoreBonus = 10, effectDurationSeconds = 5f),

    /** Rotates the arena's rendering another 45 degrees clockwise every time one is collected -
     * not a timed effect: it sticks at whatever rotation it's on until the next one nudges it
     * further, tracked via [GameState.diamondRotationStage] rather than the generic
     * [GameState.activeEffects] timer map. Odd stages read as a corner-cut octagon, even stages
     * as a plain square (see [GameEngine]'s corner-cutting collision logic), so every other
     * pickup visually "turns off" the effect even though the rotation keeps accumulating. */
    DIAMOND_ROTATE(scoreBonus = 20, effectDurationSeconds = 0f),

    /** Grants one shield charge (see [GameEngine]'s shield handling): the next obstacle hit that
     * isn't a clean rear shot bounces the vehicle off instead of ending the run, consuming the
     * charge. Not a timed effect - it just increments a persistent counter, same shape as TOKEN. */
    SHIELD(scoreBonus = 15, effectDurationSeconds = 0f),

    /** The currency collectible: a small score bonus, and it credits the player's persistent
     * token balance — the app layer is responsible for that half, by reacting to the
     * [GameEvent.PowerUpCollected] event this produces just like any other pickup. Unlike
     * every other power-up, it expires on its own if not reached in time. Lifetime is a bit
     * longer than the gap between spawns so a faster spawn rate (see [GameConfig.tokenSpawnPeriodSeconds])
     * actually converts into more *collected* tokens rather than more missed ones. */
    TOKEN(scoreBonus = 5, effectDurationSeconds = 0f, lifetimeSeconds = 11f),

    /** A gift from another player's shared-pool contribution: bonus points plus a modest speed
     * boost. Never spawns on its own — only via [GameEngine.placeSpecificPowerUp]. */
    SHARED_GIFT(scoreBonus = 50, effectDurationSeconds = 6f, poolExclusive = true),

    /** Also from the shared pool, but mischievous: bonus points, but it drops extra obstacles.
     * Never spawns on its own. */
    SHARED_PRANK(scoreBonus = 25, effectDurationSeconds = 5f, poolExclusive = true),

    /** From the shared pool: instantly grants the finder a shield charge, same handling as the
     * regular SHIELD pickup. A pure gift - never spawns on its own. */
    SHARED_SHIELD(scoreBonus = 10, effectDurationSeconds = 0f, poolExclusive = true),

    /** From the shared pool, mischievous: dims the board for a few seconds. Purely visual, no
     * collision impact - just makes the finder's board harder to read at a glance for a bit.
     * Never spawns on its own. */
    SHARED_FOG(scoreBonus = 10, effectDurationSeconds = 5f, poolExclusive = true),
}

data class PowerUp(
    val id: Long,
    val position: Vec2,
    val type: PowerUpType,
    val radius: Float,
    val spawnedAt: Float,
)
