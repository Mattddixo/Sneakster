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

    /** Signed angular difference between where [obstacle] would need to face to point directly
     * at the head in [state] and where it's actually facing, normalized to (-PI, PI]. */
    private fun angleErrorToHead(state: GameState, obstacle: Obstacle): Float {
        val bearing = (state.head - obstacle.position).angleRadians()
        var diff = bearing - obstacle.facingRadians
        while (diff > PI) diff -= (2 * PI).toFloat()
        while (diff < -PI) diff += (2 * PI).toFloat()
        return diff
    }

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
        engine.debugSetDiamondRotationStage(1)

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
    fun `newly spawned obstacles never appear inside the wider obstacle spawn clearance`() {
        // HARD spawns obstacles most often (3-6s), giving the best odds of exercising the
        // natural spawn cycle within a reasonably short simulated run.
        val config = GameConfig(arenaWidth = 2000f, arenaHeight = 2000f, difficulty = Difficulty.HARD)
        val engine = GameEngine(config, random = Random(7))
        // Mirrors GameEngine's private obstacleSpawnClearance (headRadius * 12) - not exposed
        // directly, so recomputed here the same way other tests recompute the engine's formulas.
        val obstacleSpawnClearance = config.headRadius * 12f

        var previousIds = engine.currentState().obstacles.map { it.id }.toSet()
        var sawASpawn = false

        repeat(3000) {
            val state = engine.update(FIXED_DT)
            state.obstacles.filter { it.id !in previousIds }.forEach { obstacle ->
                sawASpawn = true
                val distance = state.head.distanceTo(obstacle.position)
                assertTrue(
                    distance >= obstacleSpawnClearance,
                    "expected a newly spawned obstacle to be at least $obstacleSpawnClearance from the head, but it was $distance",
                )
            }
            previousIds = state.obstacles.map { it.id }.toSet()
        }

        assertTrue(sawASpawn, "expected at least one obstacle to spawn during the test window")
    }

    @Test
    fun `an obstacle within magnet range turns to reduce its facing error toward the head`() {
        val engine = GameEngine(quietConfig(), random = Random(1))
        val head = engine.currentState().head
        // 30 units to the side - well inside the 70-unit magnet range at this config's default
        // headRadius - facing directly away from the head for the largest possible starting
        // error. Offset sideways rather than directly ahead so the vehicle's forward motion
        // can't run it over mid-test.
        engine.debugPlaceObstacle(position = Vec2(head.x + 30f, head.y), radius = 15f, facingRadians = 0f)
        val initialError = angleErrorToHead(engine.currentState(), engine.currentState().obstacles.single())

        var state = engine.currentState()
        repeat(20) { state = engine.update(FIXED_DT) }

        val laterError = angleErrorToHead(state, state.obstacles.single())
        assertTrue(
            kotlin.math.abs(laterError) < kotlin.math.abs(initialError),
            "expected the obstacle to turn toward the head, reducing its facing error - was $initialError, now $laterError",
        )
    }

    @Test
    fun `an obstacle keeps spinning briefly after leaving magnet range, then decelerates to a stop`() {
        val config = GameConfig(arenaWidth = 4000f, arenaHeight = 4000f, difficulty = Difficulty.NORMAL)
        val engine = GameEngine(config, random = Random(1))
        // Far from the vehicle's starting position (well beyond magnet range) - this is purely
        // about what happens to existing spin, not about the pull itself.
        engine.debugPlaceObstacle(position = Vec2(100f, 100f), radius = 15f, facingRadians = 0f)
        engine.debugSetObstacleAngularVelocities(2f)

        val justAfter = engine.update(FIXED_DT)
        val velocityRightAway = justAfter.obstacles.single().angularVelocityRadiansPerSecond
        assertTrue(velocityRightAway > 0f, "expected momentum to persist into the very next frame rather than snapping to zero")
        assertTrue(velocityRightAway < 2f, "expected friction to have already started slowing it down")

        repeat(120) { engine.update(FIXED_DT) }
        val later = engine.currentState().obstacles.single().angularVelocityRadiansPerSecond
        assertEquals(0f, later, "expected it to have fully decelerated to a stop by now")
    }

    @Test
    fun `an obstacle far outside magnet range never starts turning on its own`() {
        val config = GameConfig(arenaWidth = 4000f, arenaHeight = 4000f, difficulty = Difficulty.NORMAL)
        val engine = GameEngine(config, random = Random(1))
        engine.debugPlaceObstacle(position = Vec2(100f, 100f), radius = 15f, facingRadians = 1.23f)

        repeat(120) { engine.update(FIXED_DT) }

        val obstacle = engine.currentState().obstacles.single()
        assertEquals(1.23f, obstacle.facingRadians, "an obstacle with nothing nearby shouldn't rotate at all")
        assertEquals(0f, obstacle.angularVelocityRadiansPerSecond)
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
    fun `collecting a diamond-rotate power-up advances the rotation stage and never expires it`() {
        val engine = GameEngine(quietConfig(), random = Random(1))
        val head = engine.currentState().head
        engine.debugPlacePowerUp(position = Vec2(head.x, head.y - 100f), type = PowerUpType.DIAMOND_ROTATE)

        var stateAfter = engine.currentState()
        for (frame in 0 until 300) {
            stateAfter = engine.update(FIXED_DT)
            if (stateAfter.events.any { it is GameEvent.PowerUpCollected }) break
        }
        assertEquals(1, stateAfter.diamondRotationStage, "expected the pickup to advance the stage by one")
        assertTrue(stateAfter.isDiamondCornersActive, "an odd stage should have the corners cut")

        // Unlike a timed effect, this should still be active a long time later with nothing
        // else collected - there's no timer to expire it.
        repeat(600) { stateAfter = engine.update(FIXED_DT) }
        assertEquals(1, stateAfter.diamondRotationStage)
        assertTrue(stateAfter.isDiamondCornersActive, "the effect should stick until another pickup, not expire on its own")
    }

    @Test
    fun `a second diamond-rotate pickup advances the stage again rather than resetting it`() {
        val engine = GameEngine(quietConfig(), random = Random(1))
        engine.debugSetDiamondRotationStage(1)
        val head = engine.currentState().head
        engine.debugPlacePowerUp(position = Vec2(head.x, head.y - 100f), type = PowerUpType.DIAMOND_ROTATE)

        var stateAfter = engine.currentState()
        for (frame in 0 until 300) {
            stateAfter = engine.update(FIXED_DT)
            if (stateAfter.events.any { it is GameEvent.PowerUpCollected }) break
        }

        assertEquals(2, stateAfter.diamondRotationStage, "expected the second pickup to advance to stage 2, not reset to 1")
        assertTrue(!stateAfter.isDiamondCornersActive, "an even stage should be back to a plain square")
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
        // This test runs well past the shortest spawn window (13s) to observe expiry, unlike
        // quietConfig()'s other callers - so it needs its own config with natural token spawns
        // pushed out of range, or a real spawn could land mid-test and the "no TOKEN present"
        // assertion below would fail for the wrong reason.
        val config = GameConfig(
            arenaWidth = 2000f,
            arenaHeight = 2000f,
            difficulty = Difficulty.NORMAL,
            tokenSpawnPeriodSeconds = 1000f..2000f,
        )
        val engine = GameEngine(config, random = Random(1))
        val head = engine.currentState().head
        // Well outside the arena bounds, so the vehicle can never actually reach it - this test
        // is only about the timer, not about collection.
        engine.debugPlacePowerUp(position = Vec2(head.x + 50_000f, head.y + 50_000f), type = PowerUpType.TOKEN)

        val justPlaced = engine.update(FIXED_DT)
        assertTrue(justPlaced.powerUps.any { it.type == PowerUpType.TOKEN }, "token should be present right after spawning")

        repeat(780) { engine.update(FIXED_DT) } // 13 more simulated seconds, past TOKEN's 11s lifetime

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
