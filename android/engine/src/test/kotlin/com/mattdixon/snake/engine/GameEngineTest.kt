package com.mattdixon.snake.engine

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
    fun `snake moves forward along its heading each tick`() {
        val engine = GameEngine(quietConfig(), random = Random(1))
        val start = engine.currentState().head

        val state = engine.update(FIXED_DT)

        assertTrue(state.head.distanceTo(start) > 0f, "head should have moved")
        assertEquals(GameStatus.Playing, state.status)
    }

    @Test
    fun `snake bounces off the top wall instead of leaving the arena`() {
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

        assertTrue(bounced, "expected the snake to bounce off a wall within 3 simulated seconds")
    }

    @Test
    fun `turning left for a full loop causes self collision when the body is long enough`() {
        val config = GameConfig(
            arenaWidth = 4000f,
            arenaHeight = 4000f,
            difficulty = Difficulty.HARD, // turnRate 3 rad/s, speed 150 -> turning circle radius 50
            minBodyLength = 500f, // longer than the ~314-unit circumference, guaranteeing overlap
        )
        val engine = GameEngine(config, random = Random(1))
        engine.setTurnInput(TurnInput.LEFT)

        var gameOverReason: GameOverReason? = null
        repeat(600) { // 10 simulated seconds, several full loops
            val state = engine.update(FIXED_DT)
            val ended = state.events.filterIsInstance<GameEvent.RoundEnded>().firstOrNull()
            if (ended != null) gameOverReason = ended.reason
        }

        assertEquals(GameOverReason.SELF_COLLISION, gameOverReason)
    }

    @Test
    fun `bouncing dead-on with no attempt to turn still kills you on your own tail`() {
        // Default heading is straight up with no turn input held: a short, wide arena makes the
        // first bounce an exact 180-degree reversal, so the head retraces its own tail with zero
        // separation. That's a fair death — you drove straight into a wall and straight back into
        // yourself without ever touching the controls.
        val config = GameConfig(
            arenaWidth = 2000f,
            arenaHeight = 300f,
            difficulty = Difficulty.NORMAL,
            minBodyLength = 250f,
        )
        val engine = GameEngine(config, random = Random(1))

        var reason: GameOverReason? = null
        for (frame in 0 until 200) {
            val state = engine.update(FIXED_DT)
            (state.status as? GameStatus.GameOver)?.let { reason = it.reason }
            if (reason != null) break
        }

        assertEquals(GameOverReason.SELF_COLLISION, reason)
    }

    @Test
    fun `bouncing off a wall at a slight angle avoids the tail with no turn input held`() {
        // A brief nudge before release puts the approach a few degrees off dead-on (about 7-8
        // degrees at NORMAL's turn rate), then the player lets go entirely: no turn input is
        // held during the approach, the bounce, or the retrace. Purely on geometry - the wider
        // neckClearance gives real separation room to build before any collision check even
        // starts - this angled bounce should clear its own tail, unlike the dead-on case above.
        val config = GameConfig(
            arenaWidth = 2000f,
            arenaHeight = 300f,
            difficulty = Difficulty.NORMAL,
            minBodyLength = 250f,
        )
        val engine = GameEngine(config, random = Random(1))
        engine.setTurnInput(TurnInput.LEFT)
        repeat(3) { engine.update(FIXED_DT) }
        engine.setTurnInput(TurnInput.NONE)

        var framesSinceBounce = -1
        for (frame in 0 until 200) {
            val state = engine.update(FIXED_DT)
            if (framesSinceBounce < 0) {
                if (state.events.contains(GameEvent.WallBounced)) framesSinceBounce = 0
            } else {
                framesSinceBounce++
                if (framesSinceBounce > 40) break
                assertEquals(GameStatus.Playing, state.status, "a slight-angle bounce with no input held should clear its own tail")
            }
        }

        assertTrue(framesSinceBounce >= 0, "expected a wall bounce within the simulated window")
    }

    @Test
    fun `running into an obstacle ends the round`() {
        val engine = GameEngine(quietConfig(), random = Random(1))
        val head = engine.currentState().head
        // Default heading points up (-y); place an obstacle directly in that path.
        engine.debugPlaceObstacle(position = Vec2(head.x, head.y - 200f), radius = 20f)

        var reason: GameOverReason? = null
        repeat(300) {
            val state = engine.update(FIXED_DT)
            (state.status as? GameStatus.GameOver)?.let { reason = it.reason }
        }

        assertEquals(GameOverReason.OBSTACLE_COLLISION, reason)
    }

    @Test
    fun `collecting a speed-up power-up increases speed and body length`() {
        val engine = GameEngine(quietConfig(), random = Random(1))
        val head = engine.currentState().head
        engine.debugPlacePowerUp(position = Vec2(head.x, head.y - 100f), type = PowerUpType.SPEED_UP)

        var collected = false
        var stateAfter = engine.currentState()
        for (frame in 0 until 300) {
            stateAfter = engine.update(FIXED_DT)
            if (stateAfter.events.any { it is GameEvent.PowerUpCollected }) {
                collected = true
                break
            }
        }

        assertTrue(collected, "expected the snake to reach and collect the power-up")
        assertTrue(stateAfter.activeEffects.containsKey(PowerUpType.SPEED_UP))
        assertTrue(stateAfter.speed > Difficulty.NORMAL.baseSpeed, "speed-up should raise speed above the base ramp value")
    }

    @Test
    fun `slow motion effect scales down elapsed simulation time`() {
        val engine = GameEngine(quietConfig(), random = Random(1))
        val head = engine.currentState().head
        engine.debugPlacePowerUp(position = Vec2(head.x, head.y - 50f), type = PowerUpType.SLOW_MOTION)

        var collectedAtElapsed: Float? = null
        var state = engine.currentState()
        repeat(200) {
            state = engine.update(FIXED_DT)
            if (collectedAtElapsed == null && state.events.any { it is GameEvent.PowerUpCollected }) {
                collectedAtElapsed = state.elapsedSeconds
            }
        }
        val collectedAt = requireNotNull(collectedAtElapsed) { "power-up was never collected" }

        val elapsedBeforeSlowMotionTick = state.elapsedSeconds
        state = engine.update(FIXED_DT)
        val gained = state.elapsedSeconds - elapsedBeforeSlowMotionTick

        assertTrue(collectedAt > 0f)
        assertTrue(gained < FIXED_DT, "while slow motion is active, sim time should advance slower than real time")
    }

    @Test
    fun `score increases while surviving`() {
        val engine = GameEngine(quietConfig(), random = Random(1))
        repeat(120) { engine.update(FIXED_DT) }
        assertTrue(engine.currentState().score > 0)
    }
}
