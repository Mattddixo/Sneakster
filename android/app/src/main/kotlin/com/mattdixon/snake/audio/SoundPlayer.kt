package com.mattdixon.snake.audio

import android.media.AudioManager
import android.media.ToneGenerator
import com.mattdixon.snake.engine.PowerUpType

/**
 * Short synthesized beeps via [ToneGenerator] — no bundled audio assets to keep the app tiny
 * and the sound effects trivial to reason about. Call [release] when the game screen leaves
 * composition.
 */
class SoundPlayer {
    private val toneGenerator = runCatching { ToneGenerator(AudioManager.STREAM_MUSIC, 70) }.getOrNull()

    fun playWallBounce() {
        toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, 40)
    }

    fun playPowerUpCollected(type: PowerUpType) {
        val tone = when (type) {
            PowerUpType.SHARED_GIFT, PowerUpType.SHIELD, PowerUpType.SHARED_SHIELD -> ToneGenerator.TONE_PROP_ACK
            PowerUpType.SLOW_DOWN -> ToneGenerator.TONE_PROP_NACK
            PowerUpType.SHARED_PRANK, PowerUpType.SHARED_FOG -> ToneGenerator.TONE_PROP_PROMPT
            PowerUpType.DIAMOND_ROTATE -> ToneGenerator.TONE_PROP_BEEP2
            PowerUpType.TOKEN -> ToneGenerator.TONE_PROP_BEEP
        }
        toneGenerator?.startTone(tone, 90)
    }

    fun playObstacleDestroyed() {
        toneGenerator?.startTone(ToneGenerator.TONE_SUP_CONGESTION, 150)
    }

    /** A shield charge just absorbed a hit that would otherwise have ended the run. */
    fun playShieldConsumed() {
        toneGenerator?.startTone(ToneGenerator.TONE_SUP_ERROR, 120)
    }

    /** A bonus shield charge was just earned by chaining obstacle destroys. */
    fun playShieldEarned() {
        toneGenerator?.startTone(ToneGenerator.TONE_CDMA_CONFIRM, 120)
    }

    fun playGameOver() {
        toneGenerator?.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 300)
    }

    fun release() {
        toneGenerator?.release()
    }
}
