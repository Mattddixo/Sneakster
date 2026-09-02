package com.mattdixon.snake.engine

import kotlin.math.PI
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * Pure, Android-free simulation of one match. Call [setTurnInput] whenever the player's
 * held control changes, then drive the match forward with [update] once per rendered
 * frame. Everything here is deterministic given the injected [random], which makes it
 * straightforward to unit test.
 */
class GameEngine(
    private val config: GameConfig,
    private val random: Random = Random.Default,
) {
    private val headSpawnClearance = config.headRadius * 4f
    private val survivalPointsPerSecondPerSpeedUnit = 0.15f

    // DIAMOND_ROTATE only rotates the *rendering*, not the arena's own coordinates - the UI
    // clips the rotated square to a same-size window, which cuts off its four corners (that's
    // what makes it read as an octagon rather than a shrinking diamond). Without this, the head
    // could still legally sit in one of those true corners - a position the clip renders as
    // empty black space, since nothing drawn there survives the clip - so it'd look like driving
    // into a void with no wall. While the effect is active, these two diagonal boundaries cut
    // the same four corners from the *collision* shape, so the head can never reach a spot the
    // clip would hide: this diamondDiagLimit is the exact distance (center to a cut edge) where
    // a square intersects its own 45-degree rotation, inset by headRadius so it's the head's
    // *center* limit, matching how the plain arena bounds above are also center limits.
    private val diamondDiagLimit = config.arenaWidth / sqrt(2f) - config.headRadius * sqrt(2f)

    // Awarded once, the instant an obstacle is destroyed - well above a single frame's worth of
    // survival points, so it reads as a deliberate bonus rather than a blip.
    private val obstacleDestroyScoreBonus = 40f

    // A hit anywhere but the back arc below - the front and sides, the remaining ~260 degrees of
    // it - still ends the run just like before; only a clean approach from within that cone
    // destroys it instead.
    private val obstacleBackArcHalfAngleRadians = OBSTACLE_BACK_ARC_HALF_ANGLE_DEGREES / 180f * PI.toFloat()

    // Shield: a bad hit with a charge available bounces the vehicle off instead of ending the
    // run. Capped so it's a real safety net, not a way to ignore obstacles entirely; earned
    // both from the SHIELD pickup and, passively, by chaining obstacle destroys - the latter
    // ties the run's own safety margin back into the core ram-from-behind mechanic instead of
    // making it a separate bolt-on system.
    private val maxShieldCharges = 2
    private val obstacleDestroysPerBonusShield = 3
    private val invincibilitySecondsAfterShieldHit = 1f
    private var shieldCharges = 0
    private var obstacleDestroysSinceLastShield = 0
    private var invincibleUntil = 0f

    private var head = Vec2(config.arenaWidth / 2f, config.arenaHeight / 2f)
    private var heading = -PI.toFloat() / 2f
    private var speed = config.difficulty.baseSpeed * config.scale

    // A short, fixed-length motion trail behind the vehicle - purely cosmetic now that there's
    // nothing left to collide with back here.
    private val trail = ArrayDeque<Vec2>().apply { addFirst(head) }

    private val obstacles = mutableListOf<Obstacle>()
    private val powerUps = mutableListOf<PowerUp>()
    private val activeEffects = mutableMapOf<PowerUpType, Float>()

    private var scoreAccumulator = 0f
    private var elapsedSeconds = 0f
    private var status: GameStatus = GameStatus.Playing
    private var turnInput = TurnInput.NONE
    private var nextId = 1L

    private var nextPowerUpSpawnAt = randomIn(config.difficulty.powerUpSpawnPeriodSeconds)
    private var nextObstacleSpawnAt = randomIn(config.difficulty.obstacleSpawnPeriodSeconds)
    private var nextTokenSpawnAt = randomIn(config.tokenSpawnPeriodSeconds)

    fun setTurnInput(input: TurnInput) {
        turnInput = input
    }

    fun currentState(): GameState = snapshot(emptyList())

    /** Places [type] on the board right now if there's room, regardless of its normal spawn
     * cycle — used for a shared-pool pull, which needs to appear at a specific moment chosen
     * by the caller rather than whenever the random timer would have fired. Returns false if
     * no valid spot could be found (the caller can just try again shortly after). */
    fun placeSpecificPowerUp(type: PowerUpType): Boolean {
        val position = findSpawnPosition(config.powerUpRadius) ?: return false
        powerUps.add(PowerUp(id = nextId++, position = position, type = type, radius = config.powerUpRadius, spawnedAt = elapsedSeconds))
        return true
    }

    /** Test-only hook: places an obstacle at an exact spot instead of a random one. [facingRadians]
     * defaults to "facing east" - a value deliberately unrelated to the default straight-up
     * heading, so a test that doesn't care about front/back doesn't silently get one for free. */
    internal fun debugPlaceObstacle(position: Vec2, radius: Float = config.obstacleRadius, facingRadians: Float = 0f) {
        obstacles.add(Obstacle(id = nextId++, position = position, radius = radius, facingRadians = facingRadians, spawnedAt = elapsedSeconds))
    }

    /** Test-only hook: places a power-up at an exact spot instead of a random one. */
    internal fun debugPlacePowerUp(position: Vec2, type: PowerUpType) {
        powerUps.add(PowerUp(id = nextId++, position = position, type = type, radius = config.powerUpRadius, spawnedAt = elapsedSeconds))
    }

    /** Test-only hook: grants a shield charge directly instead of via a SHIELD pickup. */
    internal fun debugGrantShieldCharge() {
        shieldCharges = (shieldCharges + 1).coerceAtMost(maxShieldCharges)
    }

    /** Test-only hook: activates a timed effect directly instead of via a pickup. */
    internal fun debugActivateEffect(type: PowerUpType, durationSeconds: Float) {
        activeEffects[type] = elapsedSeconds + durationSeconds
    }

    fun update(dtSeconds: Float): GameState {
        if (status != GameStatus.Playing) return snapshot(emptyList())
        val events = mutableListOf<GameEvent>()

        elapsedSeconds += dtSeconds
        activeEffects.keys.retainAll { activeEffects.getValue(it) > elapsedSeconds }

        applyTurn(dtSeconds)
        speed = currentSpeed()
        moveHead(dtSeconds, events)
        updateTrail()

        checkObstacleCollisions(events)
        expirePowerUps()
        checkPowerUpPickups(events)
        maybeSpawnEntities()
        scoreAccumulator += dtSeconds * speed * survivalPointsPerSecondPerSpeedUnit

        return snapshot(events)
    }

    private fun currentSpeed(): Float {
        var multiplier = 1f
        if (isEffectActive(PowerUpType.SLOW_DOWN)) multiplier *= 0.55f
        if (isEffectActive(PowerUpType.SHARED_GIFT)) multiplier *= 1.4f
        return config.difficulty.baseSpeedAt(elapsedSeconds) * config.scale * multiplier
    }

    private fun isEffectActive(type: PowerUpType): Boolean {
        val expiresAt = activeEffects[type] ?: return false
        return expiresAt > elapsedSeconds
    }

    private fun applyTurn(dt: Float) {
        val direction = when (turnInput) {
            TurnInput.NONE -> 0f
            TurnInput.LEFT -> -1f
            TurnInput.RIGHT -> 1f
        }
        heading += direction * config.difficulty.turnRateRadiansPerSecond * config.controlSensitivity * dt
        heading = normalizeAngle(heading)
    }

    private fun moveHead(dt: Float, events: MutableList<GameEvent>) {
        var next = head + Vec2.heading(heading) * (speed * dt)
        var bounced = false

        val r = config.headRadius
        if (next.x - r < 0f || next.x + r > config.arenaWidth) {
            heading = normalizeAngle(PI.toFloat() - heading)
            next = Vec2(next.x.coerceIn(r, config.arenaWidth - r), next.y)
            bounced = true
        }
        if (next.y - r < 0f || next.y + r > config.arenaHeight) {
            heading = normalizeAngle(-heading)
            next = Vec2(next.x, next.y.coerceIn(r, config.arenaHeight - r))
            bounced = true
        }
        if (isEffectActive(PowerUpType.DIAMOND_ROTATE)) {
            val (cornered, adjusted) = bounceOffDiamondCorners(next)
            next = adjusted
            if (cornered) bounced = true
        }

        head = next
        if (bounced) events.add(GameEvent.WallBounced)
    }

    /** Cuts the arena's four true corners against the two diagonals a same-size square rotated
     * 45 degrees would intersect it at - see [diamondDiagLimit]. Applied as two independent
     * axis-like checks (sum and difference of the centered coordinates), same shape as the
     * plain x/y wall checks above, just rotated 45 degrees. */
    private fun bounceOffDiamondCorners(position: Vec2): Pair<Boolean, Vec2> {
        val cx = config.arenaWidth / 2f
        val cy = config.arenaHeight / 2f
        var (x, y) = position
        var cornered = false

        val sum = (x - cx) + (y - cy)
        if (sum > diamondDiagLimit) {
            val excess = sum - diamondDiagLimit
            x -= excess / 2f
            y -= excess / 2f
            heading = normalizeAngle(-PI.toFloat() / 2f - heading)
            cornered = true
        } else if (sum < -diamondDiagLimit) {
            val excess = -diamondDiagLimit - sum
            x += excess / 2f
            y += excess / 2f
            heading = normalizeAngle(-PI.toFloat() / 2f - heading)
            cornered = true
        }

        val diff = (x - cx) - (y - cy)
        if (diff > diamondDiagLimit) {
            val excess = diff - diamondDiagLimit
            x -= excess / 2f
            y += excess / 2f
            heading = normalizeAngle(PI.toFloat() / 2f - heading)
            cornered = true
        } else if (diff < -diamondDiagLimit) {
            val excess = -diamondDiagLimit - diff
            x += excess / 2f
            y -= excess / 2f
            heading = normalizeAngle(PI.toFloat() / 2f - heading)
            cornered = true
        }

        return cornered to Vec2(x, y)
    }

    private fun updateTrail() {
        trail.addFirst(head)
        var accumulated = 0f
        var keep = 1
        while (keep < trail.size && accumulated < config.trailLength) {
            accumulated += trail[keep - 1].distanceTo(trail[keep])
            keep++
        }
        while (trail.size > keep) trail.removeLast()
    }

    private fun checkObstacleCollisions(events: MutableList<GameEvent>) {
        if (elapsedSeconds < invincibleUntil) return
        val hit = obstacles.firstOrNull { head.distanceTo(it.position) < config.headRadius + it.radius } ?: return
        if (isRearHit(hit)) {
            destroyObstacle(hit, events)
        } else if (shieldCharges > 0) {
            shieldCharges--
            bounceOffObstacle(hit)
            invincibleUntil = elapsedSeconds + invincibilitySecondsAfterShieldHit
            events.add(GameEvent.ShieldConsumed)
        } else {
            endRound(GameOverReason.OBSTACLE_COLLISION, events)
        }
    }

    private fun destroyObstacle(obstacle: Obstacle, events: MutableList<GameEvent>) {
        obstacles.remove(obstacle)
        scoreAccumulator += obstacleDestroyScoreBonus
        events.add(GameEvent.ObstacleDestroyed(obstacle.id))

        obstacleDestroysSinceLastShield++
        if (obstacleDestroysSinceLastShield >= obstacleDestroysPerBonusShield && shieldCharges < maxShieldCharges) {
            obstacleDestroysSinceLastShield = 0
            shieldCharges++
            events.add(GameEvent.ShieldEarned)
        }
    }

    /** Knocks the head back to a safe distance from [obstacle] and turns it to face directly
     * away, the same "recoil" feel as a wall bounce - clamped to the arena so a shield hit near
     * an edge can't push the vehicle out of bounds. */
    private fun bounceOffObstacle(obstacle: Obstacle) {
        val away = (head - obstacle.position).normalized()
        val safeDistance = config.headRadius + obstacle.radius + config.headRadius
        val bounced = obstacle.position + away * safeDistance
        head = Vec2(
            bounced.x.coerceIn(config.headRadius, config.arenaWidth - config.headRadius),
            bounced.y.coerceIn(config.headRadius, config.arenaHeight - config.headRadius),
        )
        heading = normalizeAngle(away.angleRadians())
    }

    /** True if the head struck [obstacle] from within its exposed rear cone - i.e. the head, at
     * the moment of impact, sits roughly opposite the direction the obstacle is facing. */
    private fun isRearHit(obstacle: Obstacle): Boolean {
        val impactAngle = (head - obstacle.position).angleRadians()
        val rearAngle = normalizeAngle(obstacle.facingRadians + PI.toFloat())
        val angleFromRear = kotlin.math.abs(normalizeAngle(impactAngle - rearAngle))
        return angleFromRear < obstacleBackArcHalfAngleRadians
    }

    private fun endRound(reason: GameOverReason, events: MutableList<GameEvent>) {
        status = GameStatus.GameOver(reason)
        events.add(GameEvent.RoundEnded(reason))
    }

    private fun expirePowerUps() {
        powerUps.removeAll { powerUp ->
            val lifetime = powerUp.type.lifetimeSeconds ?: return@removeAll false
            elapsedSeconds - powerUp.spawnedAt > lifetime
        }
    }

    private fun checkPowerUpPickups(events: MutableList<GameEvent>) {
        val collected = powerUps.filter { head.distanceTo(it.position) < config.headRadius + it.radius }
        if (collected.isEmpty()) return

        collected.forEach { powerUp ->
            scoreAccumulator += powerUp.type.scoreBonus
            if (powerUp.type.effectDurationSeconds > 0f) {
                activeEffects[powerUp.type] = elapsedSeconds + powerUp.type.effectDurationSeconds
            }
            if (powerUp.type == PowerUpType.SHARED_PRANK) {
                repeat(if (random.nextBoolean()) 2 else 1) { spawnObstacle() }
            }
            if (powerUp.type == PowerUpType.SHIELD || powerUp.type == PowerUpType.SHARED_SHIELD) {
                shieldCharges = (shieldCharges + 1).coerceAtMost(maxShieldCharges)
            }
            events.add(GameEvent.PowerUpCollected(powerUp.type))
        }
        powerUps.removeAll(collected)
    }

    private fun maybeSpawnEntities() {
        if (elapsedSeconds >= nextPowerUpSpawnAt && regularPowerUpCount() < config.maxConcurrentPowerUps) {
            spawnPowerUp()
            nextPowerUpSpawnAt = elapsedSeconds + randomIn(config.difficulty.powerUpSpawnPeriodSeconds)
        }
        if (elapsedSeconds >= nextObstacleSpawnAt && obstacles.size < config.maxConcurrentObstacles) {
            spawnObstacle()
            nextObstacleSpawnAt = elapsedSeconds + randomIn(config.difficulty.obstacleSpawnPeriodSeconds)
        }
        if (elapsedSeconds >= nextTokenSpawnAt && tokenCount() < config.maxConcurrentTokens) {
            spawnToken()
            nextTokenSpawnAt = elapsedSeconds + randomIn(config.tokenSpawnPeriodSeconds)
        }
    }

    private fun regularPowerUpCount(): Int = powerUps.count { !it.type.poolExclusive && it.type != PowerUpType.TOKEN }

    private fun tokenCount(): Int = powerUps.count { it.type == PowerUpType.TOKEN }

    private fun spawnPowerUp() {
        val position = findSpawnPosition(config.powerUpRadius) ?: return
        val type = PowerUpType.entries.filter { !it.poolExclusive && it != PowerUpType.TOKEN }.random(random)
        powerUps.add(PowerUp(id = nextId++, position = position, type = type, radius = config.powerUpRadius, spawnedAt = elapsedSeconds))
    }

    private fun spawnToken() {
        val position = findSpawnPosition(config.tokenRadius) ?: return
        powerUps.add(PowerUp(id = nextId++, position = position, type = PowerUpType.TOKEN, radius = config.tokenRadius, spawnedAt = elapsedSeconds))
    }

    private fun spawnObstacle() {
        if (obstacles.size >= config.maxConcurrentObstacles) return
        val position = findSpawnPosition(config.obstacleRadius) ?: return
        obstacles.add(
            Obstacle(
                id = nextId++,
                position = position,
                radius = config.obstacleRadius,
                facingRadians = random.nextFloat() * 2f * PI.toFloat(),
                spawnedAt = elapsedSeconds,
            ),
        )
    }

    /** Rejection-samples a spot clear of the vehicle's head and other entities; gives up after a few tries. */
    private fun findSpawnPosition(radius: Float): Vec2? {
        repeat(20) {
            val candidate = Vec2(
                x = radius + random.nextFloat() * (config.arenaWidth - 2 * radius),
                y = radius + random.nextFloat() * (config.arenaHeight - 2 * radius),
            )
            val clearOfHead = candidate.distanceTo(head) > headSpawnClearance
            val clearOfObstacles = obstacles.none { candidate.distanceTo(it.position) < it.radius + radius + 8f }
            val clearOfPowerUps = powerUps.none { candidate.distanceTo(it.position) < it.radius + radius + 8f }
            if (clearOfHead && clearOfObstacles && clearOfPowerUps) return candidate
        }
        return null
    }

    private fun randomIn(range: ClosedFloatingPointRange<Float>): Float =
        range.start + random.nextFloat() * (range.endInclusive - range.start)

    private fun normalizeAngle(angle: Float): Float {
        var result = angle
        val twoPi = (2 * PI).toFloat()
        while (result > PI) result -= twoPi
        while (result < -PI) result += twoPi
        return result
    }

    private fun snapshot(events: List<GameEvent>): GameState = GameState(
        head = head,
        headingRadians = heading,
        speed = speed,
        trail = trail.toList(),
        obstacles = obstacles.toList(),
        powerUps = powerUps.toList(),
        activeEffects = activeEffects.toMap(),
        shieldCharges = shieldCharges,
        isInvincible = elapsedSeconds < invincibleUntil,
        score = scoreAccumulator.toInt(),
        elapsedSeconds = elapsedSeconds,
        status = status,
        events = events,
    )
}
