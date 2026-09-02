package com.mattdixon.snake.engine

import kotlin.math.PI
import kotlin.math.sqrt
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private const val FIXED_DT = 1f / 60f

class GameEngineTest {

    // Difficulty's spawn-timer ranges are fixed (not test-overridable), so tests that place
    // their own entities keep well under the shortest spawn window (~4s) to stay deterministic.
    private fun quietConfig(
        arenaWidth: Float = 2000f,
        arenaHeight: Float = 2000f,
    ) = GameConfig(arenaWidth = arenaWidth, arenaHeight = arenaHeight, difficulty = Difficulty.NORMAL)

    @Test
    fun `vehicle moves forward along its heading each tick`() {
        val engine = GameEngine(quietConfig(), random = Random(1))
        val start = engine.currentState().head

        val state = engine.update(FIXED_DT)

        assertTrue(state.head.distanceTo(start) > 0f, "head should have moved")
        assertEquals(GameStatus.Playing, state.status)
    }

    @Test
    fun `vehicle bounces off the top wall instead of leaving the arena`() {
        // Facing up by default. A short arena means it reaches the top edge in about a
        // second, comfortably before the earliest random obstacle/power-up spawn (~4s).
        val config = quietConfig(arenaHeight = 300f)
        val engine = GameEngine(config, random = Random(1))
        var bounced = false

        repeat(180) {
            val state = engine.update(FIXED_DT)
            if (state.events.contains(GameEvent.WallBounced)) bounced = true
            assertTrue(state.head.y >= 0f && state.head.y <= config.arenaHeight, "head must stay inside the arena")
        }

        assertTrue(bounced, "expected the vehicle to bounce off a wall within 3 simulated seconds")
    }

    @Test
    fun `turning continuously for a long time never ends the round on its own`() {
        // There's no self-collision at all anymore - obstacles are the only hazard - so looping
        // back over the vehicle's own earlier path is perfectly safe. Kept comfortably under
        // HARD's 3-second obstacle-spawn floor so a randomly spawned obstacle can't make this
        // flaky, and comfortably over one full turning circle (~2.1s at this radius and speed)
        // so the loop genuinely crosses its own past path at least once.
        val config = GameConfig(arenaWidth = 4000f, arenaHeight = 4000f, difficulty = Difficulty.HARD)
        val engine = GameEngine(config, random = Random(1))
        engine.setTurnInput(TurnInput.LEFT)

        var status: GameStatus = GameStatus.Playing
        repeat(150) { status = engine.update(FIXED_DT).status }

        assertEquals(GameStatus.Playing, status)
    }

    @Test
    fun `while diamond-rotate is active the vehicle bounces off the cut corner instead of reaching it`() {
        // The UI only rotates the *rendering* for DIAMOND_ROTATE, clipped to a same-size window -
        // that cuts the arena's four true corners out of what's visible (it's what makes the
        // effect read as an octagon, not a shrinking diamond). Without a matching cut in the
        // collision shape, the head could sit in a true corner the clip renders as empty black
        // space - this test drives it straight at one and confirms it bounces well short of it.
        val config = GameConfig(arenaWidth = 2000f, arenaHeight = 2000f, difficulty = Difficulty.NORMAL)
        val engine = GameEngine(config, random = Random(1))
        engine.debugActivateEffect(PowerUpType.DIAMOND_ROTATE, durationSeconds = 30f)

        // Default heading is straight up (-PI/2); turn right until it points diagonally at the
        // bottom-right corner (+PI/4) - a 3*PI/4 turn, ~53 frames at NORMAL's turn rate.
        engine.setTurnInput(TurnInput.RIGHT)
        repeat(53) { engine.update(FIXED_DT) }
        engine.setTurnInput(TurnInput.NONE)

        val cx = config.arenaWidth / 2f
        val cy = config.arenaHeight / 2f
        val diagLimit = config.arenaWidth / sqrt(2f) - config.headRadius * sqrt(2f)

        var maxSumSeen = Float.NEGATIVE_INFINITY
        repeat(300) {
            val state = engine.update(FIXED_DT)
            val sum = (state.head.x - cx) + (state.head.y - cy)
            if (sum > maxSumSeen) maxSumSeen = sum
        }

        assertTrue(
            maxSumSeen <= diagLimit + 1f,
            "expected the vehicle to bounce off the cut corner at sum=$diagLimit, but it reached sum=$maxSumSeen",
        )
    }

    @Test
    fun `hitting an obstacle anywhere but its exposed back ends the round`() {
        val engine = GameEngine(quietConfig(), random = Random(1))
        val head = engine.currentState().head
        // Default heading points up (-y); the obstacle sits directly in that path, facing back
        // toward the oncoming vehicle - so its exposed rear points away, and the vehicle runs
        // straight into its front.
        engine.debugPlaceObstacle(
            position = Vec2(head.x, head.y - 200f),
            radius = 20f,
            facingRadians = PI.toFloat() / 2f, // facing down, toward the oncoming vehicle
        )

        var reason: GameOverReason? = null
        repeat(300) {
            val state = engine.update(FIXED_DT)
            (state.status as? GameStatus.GameOver)?.let { reason = it.reason }
        }

        assertEquals(GameOverReason.OBSTACLE_COLLISION, reason)
    }

    @Test
    fun `ramming an obstacle from its exposed back destroys it and awards a score bonus`() {
        val engine = GameEngine(quietConfig(), random = Random(1))
        val head = engine.currentState().head
        // Same setup, but the obstacle now faces the same way the vehicle is already heading
        // (up) - so its exposed back is the underside, exactly where the vehicle approaches from.
        engine.debugPlaceObstacle(
            position = Vec2(head.x, head.y - 200f),
            radius = 20f,
            facingRadians = -PI.toFloat() / 2f, // facing up, same direction as the oncoming vehicle
        )

        var scoreBeforeHit = 0
        var scoreAfterHit: Int? = null
        var finalStatus: GameStatus = GameStatus.Playing
        repeat(300) {
            val prevScore = engine.currentState().score
            val state = engine.update(FIXED_DT)
            finalStatus = state.status
            if (scoreAfterHit == null && state.events.any { it is GameEvent.ObstacleDestroyed }) {
                scoreBeforeHit = prevScore
                scoreAfterHit = state.score
            }
        }

        val after = requireNotNull(scoreAfterHit) { "expected the vehicle to destroy the obstacle by ramming its exposed back" }
        assertTrue(after - scoreBeforeHit >= 30, "destroying an obstacle should add a real score bonus, not just one frame of survival points")
        assertEquals(GameStatus.Playing, finalStatus, "destroying an obstacle from behind shouldn't end the round")
        assertTrue(engine.currentState().obstacles.isEmpty(), "the destroyed obstacle should be gone from the board")
    }

    @Test
    fun `collecting a shield power-up grants a shield charge`() {
        val engine = GameEngine(quietConfig(), random = Random(1))
        val head = engine.currentState().head
        engine.debugPlacePowerUp(position = Vec2(head.x, head.y - 100f), type = PowerUpType.SHIELD)

        var stateAfter = engine.currentState()
        for (frame in 0 until 300) {
            stateAfter = engine.update(FIXED_DT)
            if (stateAfter.events.any { it is GameEvent.PowerUpCollected }) break
        }

        assertEquals(1, stateAfter.shieldCharges, "expected the shield pickup to grant a charge")
    }

    @Test
    fun `a shield charge absorbs a bad hit instead of ending the round`() {
        val engine = GameEngine(quietConfig(), random = Random(1))
        engine.debugGrantShieldCharge()
        val head = engine.currentState().head
        // Same front-on setup as the "ends the round" test above, but this time with a shield
        // charge in reserve.
        engine.debugPlaceObstacle(
            position = Vec2(head.x, head.y - 200f),
            radius = 20f,
            facingRadians = PI.toFloat() / 2f, // facing down, toward the oncoming vehicle
        )

        var consumedShield = false
        var finalStatus: GameStatus = GameStatus.Playing
        repeat(300) {
            val state = engine.update(FIXED_DT)
            if (state.events.any { it is GameEvent.ShieldConsumed }) consumedShield = true
            finalStatus = state.status
        }

        assertTrue(consumedShield, "expected the shield to absorb the front hit")
        assertEquals(GameStatus.Playing, finalStatus, "a shielded hit shouldn't end the round")
        assertEquals(0, engine.currentState().shieldCharges, "the shield charge should be consumed")
    }

    @Test
    fun `destroying enough obstacles earns a bonus shield charge`() {
        val engine = GameEngine(quietConfig(), random = Random(1))
        val head = engine.currentState().head
        // Three obstacles in a row directly ahead, each with its exposed back facing the
        // oncoming vehicle, spaced well apart so they're destroyed one at a time in sequence.
        listOf(150f, 350f, 550f).forEach { distance ->
            engine.debugPlaceObstacle(
                position = Vec2(head.x, head.y - distance),
                radius = 20f,
                facingRadians = -PI.toFloat() / 2f,
            )
        }

        var destroyedCount = 0
        var shieldEarned = false
        repeat(600) {
            val state = engine.update(FIXED_DT)
            destroyedCount += state.events.count { it is GameEvent.ObstacleDestroyed }
            if (state.events.any { it is GameEvent.ShieldEarned }) shieldEarned = true
        }

        assertEquals(3, destroyedCount, "expected all three obstacles to be destroyed")
        assertTrue(shieldEarned, "expected a bonus shield charge after destroying enough obstacles in a row")
        assertEquals(1, engine.currentState().shieldCharges)
    }

    @Test
    fun `shield charges are capped at the maximum`() {
        val engine = GameEngine(quietConfig(), random = Random(1))
        repeat(5) { engine.debugGrantShieldCharge() }

        assertEquals(2, engine.currentState().shieldCharges)
    }

    @Test
    fun `collecting a shared gift increases speed`() {
        val engine = GameEngine(quietConfig(), random = Random(1))
        val head = engine.currentState().head
        engine.debugPlacePowerUp(position = Vec2(head.x, head.y - 100f), type = PowerUpType.SHARED_GIFT)

        var collected = false
        var stateAfter = engine.currentState()
        for (frame in 0 until 300) {
            stateAfter = engine.update(FIXED_DT)
            if (stateAfter.events.any { it is GameEvent.PowerUpCollected }) {
                collected = true
                break
            }
        }

        assertTrue(collected, "expected the vehicle to reach and collect the power-up")
        assertTrue(stateAfter.activeEffects.containsKey(PowerUpType.SHARED_GIFT))
        assertTrue(stateAfter.speed > Difficulty.NORMAL.baseSpeed, "a shared gift's speed boost should raise speed above the base ramp value")
    }

    @Test
    fun `score increases while surviving`() {
        val engine = GameEngine(quietConfig(), random = Random(1))
        repeat(120) { engine.update(FIXED_DT) }
        assertTrue(engine.currentState().score > 0)
    }

    @Test
    fun `a token disappears on its own if not collected in time`() {
        val engine = GameEngine(quietConfig(), random = Random(1))
        val head = engine.currentState().head
        // Well outside the arena bounds, so the vehicle can never actually reach it - this test
        // is only about the timer, not about collection.
        engine.debugPlacePowerUp(position = Vec2(head.x + 50_000f, head.y + 50_000f), type = PowerUpType.TOKEN)

        val justPlaced = engine.update(FIXED_DT)
        assertTrue(justPlaced.powerUps.any { it.type == PowerUpType.TOKEN }, "token should be present right after spawning")

        repeat(600) { engine.update(FIXED_DT) } // 10 more simulated seconds, past TOKEN's 9s lifetime

        assertTrue(engine.currentState().powerUps.none { it.type == PowerUpType.TOKEN }, "token should have expired by now")
    }

    @Test
    fun `placeSpecificPowerUp puts a pool-pulled effect on the board immediately`() {
        val engine = GameEngine(quietConfig(), random = Random(1))

        val placed = engine.placeSpecificPowerUp(PowerUpType.SHARED_GIFT)

        assertTrue(placed, "expected a valid spawn spot in an empty 2000x2000 arena")
        assertEquals(1, engine.currentState().powerUps.count { it.type == PowerUpType.SHARED_GIFT })
    }

    @Test
    fun `pool-exclusive effects never appear through the normal random spawn cycle`() {
        val engine = GameEngine(quietConfig(), random = Random(1))

        val seenTypes = mutableSetOf<PowerUpType>()
        repeat(3600) { // 60 simulated seconds: several full power-up spawn cycles
            seenTypes += engine.update(FIXED_DT).powerUps.map { it.type }
        }

        assertTrue(PowerUpType.SHARED_GIFT !in seenTypes, "SHARED_GIFT should only ever appear via placeSpecificPowerUp")
        assertTrue(PowerUpType.SHARED_PRANK !in seenTypes, "SHARED_PRANK should only ever appear via placeSpecificPowerUp")
    }
}
