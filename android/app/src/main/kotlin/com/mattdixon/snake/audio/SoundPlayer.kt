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
            PowerUpType.SPEED_UP -> ToneGenerator.TONE_PROP_ACK
            PowerUpType.SLOW_DOWN, PowerUpType.SLOW_MOTION -> ToneGenerator.TONE_PROP_NACK
            PowerUpType.SPAWN_OBSTACLE -> ToneGenerator.TONE_PROP_PROMPT
        }
        toneGenerator?.startTone(tone, 90)
    }

    fun playGameOver() {
        toneGenerator?.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 300)
    }

    fun release() {
        toneGenerator?.release()
    }
}
