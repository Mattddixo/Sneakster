package com.mattdixon.snake.engine

import kotlin.math.PI
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
    private val bodyCollisionRadius = config.headRadius * 1.6f
    private val neckClearance = config.headRadius * 3f
    private val survivalPointsPerSecondPerSpeedUnit = 0.15f

    private var head = Vec2(config.arenaWidth / 2f, config.arenaHeight / 2f)
    private var heading = -PI.toFloat() / 2f
    private var speed = config.difficulty.baseSpeed
    private val path = ArrayDeque<Vec2>().apply { addFirst(head) }
    private var bodyLength = config.minBodyLength

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

    fun setTurnInput(input: TurnInput) {
        turnInput = input
    }

    fun currentState(): GameState = snapshot(emptyList())

    /** Test-only hook: places an obstacle at an exact spot instead of a random one. */
    internal fun debugPlaceObstacle(position: Vec2, radius: Float = config.obstacleRadius, expiresInSeconds: Float = 30f) {
        obstacles.add(Obstacle(id = nextId++, position = position, radius = radius, spawnedAt = elapsedSeconds, expiresAt = elapsedSeconds + expiresInSeconds))
    }

    /** Test-only hook: places a power-up at an exact spot instead of a random one. */
    internal fun debugPlacePowerUp(position: Vec2, type: PowerUpType) {
        powerUps.add(PowerUp(id = nextId++, position = position, type = type, radius = config.powerUpRadius))
    }

    fun update(dtSeconds: Float): GameState {
        if (status != GameStatus.Playing) return snapshot(emptyList())
        val events = mutableListOf<GameEvent>()

        val timeScale = if (isEffectActive(PowerUpType.SLOW_MOTION)) 0.4f else 1f
        val dt = dtSeconds * timeScale
        elapsedSeconds += dt
        activeEffects.keys.retainAll { activeEffects.getValue(it) > elapsedSeconds }

        applyTurn(dt)
        speed = currentSpeed()
        moveHead(dt, events)
        updateBody()

        checkSelfCollision(events)
        checkObstacleCollisions(events)
        expireObstacles()
        checkPowerUpPickups(events)
        maybeSpawnEntities()
        scoreAccumulator += dt * speed * survivalPointsPerSecondPerSpeedUnit

        return snapshot(events)
    }

    private fun currentSpeed(): Float {
        var multiplier = 1f
        if (isEffectActive(PowerUpType.SPEED_UP)) multiplier *= 1.5f
        if (isEffectActive(PowerUpType.SLOW_DOWN)) multiplier *= 0.55f
        return config.difficulty.baseSpeedAt(elapsedSeconds) * multiplier
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

        head = next
        if (bounced) events.add(GameEvent.WallBounced)
    }

    private fun updateBody() {
        path.addFirst(head)
        bodyLength = (config.minBodyLength + (speed - config.difficulty.baseSpeed) * config.bodyLengthPerSpeedUnit)
            .coerceIn(config.minBodyLength, config.maxBodyLength)

        var accumulated = 0f
        var keep = 1
        while (keep < path.size && accumulated < bodyLength) {
            accumulated += path[keep - 1].distanceTo(path[keep])
            keep++
        }
        while (path.size > keep) path.removeLast()
    }

    private fun checkSelfCollision(events: MutableList<GameEvent>) {
        var accumulated = 0f
        for (i in 1 until path.size) {
            accumulated += path[i - 1].distanceTo(path[i])
            if (accumulated < neckClearance) continue
            if (head.distanceTo(path[i]) < bodyCollisionRadius) {
                endRound(GameOverReason.SELF_COLLISION, events)
                return
            }
        }
    }

    private fun checkObstacleCollisions(events: MutableList<GameEvent>) {
        val hit = obstacles.any { head.distanceTo(it.position) < config.headRadius + it.radius }
        if (hit) endRound(GameOverReason.OBSTACLE_COLLISION, events)
    }

    private fun endRound(reason: GameOverReason, events: MutableList<GameEvent>) {
        status = GameStatus.GameOver(reason)
        events.add(GameEvent.RoundEnded(reason))
    }

    private fun expireObstacles() {
        obstacles.removeAll { it.expiresAt <= elapsedSeconds }
    }

    private fun checkPowerUpPickups(events: MutableList<GameEvent>) {
        val collected = powerUps.filter { head.distanceTo(it.position) < config.headRadius + it.radius }
        if (collected.isEmpty()) return

        collected.forEach { powerUp ->
            scoreAccumulator += powerUp.type.scoreBonus
            if (powerUp.type.effectDurationSeconds > 0f) {
                activeEffects[powerUp.type] = elapsedSeconds + powerUp.type.effectDurationSeconds
            }
            if (powerUp.type == PowerUpType.SPAWN_OBSTACLE) {
                repeat(if (random.nextBoolean()) 2 else 1) { spawnObstacle() }
            }
            events.add(GameEvent.PowerUpCollected(powerUp.type))
        }
        powerUps.removeAll(collected)
    }

    private fun maybeSpawnEntities() {
        if (elapsedSeconds >= nextPowerUpSpawnAt && powerUps.size < config.maxConcurrentPowerUps) {
            spawnPowerUp()
            nextPowerUpSpawnAt = elapsedSeconds + randomIn(config.difficulty.powerUpSpawnPeriodSeconds)
        }
        if (elapsedSeconds >= nextObstacleSpawnAt && obstacles.size < config.maxConcurrentObstacles) {
            spawnObstacle()
            nextObstacleSpawnAt = elapsedSeconds + randomIn(config.difficulty.obstacleSpawnPeriodSeconds)
        }
    }

    private fun spawnPowerUp() {
        val position = findSpawnPosition(config.powerUpRadius) ?: return
        powerUps.add(PowerUp(id = nextId++, position = position, type = PowerUpType.entries.random(random), radius = config.powerUpRadius))
    }

    private fun spawnObstacle() {
        if (obstacles.size >= config.maxConcurrentObstacles) return
        val position = findSpawnPosition(config.obstacleRadius) ?: return
        obstacles.add(
            Obstacle(
                id = nextId++,
                position = position,
                radius = config.obstacleRadius,
                spawnedAt = elapsedSeconds,
                expiresAt = elapsedSeconds + randomIn(config.obstacleLifetimeSeconds),
            ),
        )
    }

    /** Rejection-samples a spot clear of the snake's head and other entities; gives up after a few tries. */
    private fun findSpawnPosition(radius: Float): Vec2? {
        repeat(20) {
            val candidate = Vec2(
                x = radius + random.nextFloat() * (config.arenaWidth - 2 * radius),
                y = radius + random.nextFloat() * (config.arenaHeight - 2 * radius),
            )
            val clearOfHead = candidate.distanceTo(head) > neckClearance * 2f
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
        body = path.toList(),
        bodyLength = bodyLength,
        obstacles = obstacles.toList(),
        powerUps = powerUps.toList(),
        activeEffects = activeEffects.toMap(),
        score = scoreAccumulator.toInt(),
        elapsedSeconds = elapsedSeconds,
        status = status,
        events = events,
    )
}
