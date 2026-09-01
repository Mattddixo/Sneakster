package com.mattdixon.snake.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.mattdixon.snake.engine.Difficulty
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "sneakster_settings")

/** Persists the player's in-game configuration and a couple of small conveniences (nickname, best score). */
class SettingsRepository(private val context: Context) {

    private object Keys {
        val DIFFICULTY = stringPreferencesKey("difficulty")
        val SENSITIVITY = floatPreferencesKey("control_sensitivity")
        val SOUND_ENABLED = booleanPreferencesKey("sound_enabled")
        val HAPTICS_ENABLED = booleanPreferencesKey("haptics_enabled")
        val NICKNAME = stringPreferencesKey("nickname")
        val SERVER_BASE_URL = stringPreferencesKey("server_base_url")
        val BEST_SCORE = intPreferencesKey("best_score")
    }

    val settings: Flow<GameSettings> = context.dataStore.data.map { prefs ->
        GameSettings(
            difficulty = prefs[Keys.DIFFICULTY]?.let(Difficulty::valueOf) ?: Difficulty.NORMAL,
            controlSensitivity = prefs[Keys.SENSITIVITY] ?: 1.0f,
            soundEnabled = prefs[Keys.SOUND_ENABLED] ?: true,
            hapticsEnabled = prefs[Keys.HAPTICS_ENABLED] ?: true,
            nickname = prefs[Keys.NICKNAME] ?: "",
            serverBaseUrl = prefs[Keys.SERVER_BASE_URL] ?: "",
            bestScore = prefs[Keys.BEST_SCORE] ?: 0,
        )
    }

    suspend fun setDifficulty(difficulty: Difficulty) {
        context.dataStore.edit { it[Keys.DIFFICULTY] = difficulty.name }
    }

    suspend fun setControlSensitivity(sensitivity: Float) {
        context.dataStore.edit {
            it[Keys.SENSITIVITY] = sensitivity.coerceIn(GameSettings.MIN_SENSITIVITY, GameSettings.MAX_SENSITIVITY)
        }
    }

    suspend fun setSoundEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.SOUND_ENABLED] = enabled }
    }

    suspend fun setHapticsEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.HAPTICS_ENABLED] = enabled }
    }

    suspend fun setNickname(nickname: String) {
        context.dataStore.edit { it[Keys.NICKNAME] = nickname }
    }

    suspend fun setServerBaseUrl(url: String) {
        context.dataStore.edit { it[Keys.SERVER_BASE_URL] = url.trim().trimEnd('/') }
    }

    suspend fun recordScoreIfBest(score: Int) {
        context.dataStore.edit {
            val current = it[Keys.BEST_SCORE] ?: 0
            if (score > current) it[Keys.BEST_SCORE] = score
        }
    }
}
