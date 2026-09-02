package com.mattdixon.snake.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.mattdixon.snake.engine.Difficulty
import java.util.UUID
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
        val TOKEN_BALANCE = intPreferencesKey("token_balance")
        val DEVICE_ID = stringPreferencesKey("device_id")
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
            tokenBalance = prefs[Keys.TOKEN_BALANCE] ?: 0,
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

    suspend fun addTokens(count: Int) {
        context.dataStore.edit {
            it[Keys.TOKEN_BALANCE] = (it[Keys.TOKEN_BALANCE] ?: 0) + count
        }
    }

    /** Deducts [cost] tokens and returns true, or leaves the balance untouched and returns
     * false if it's insufficient. DataStore's edit block is an atomic read-modify-write, so
     * this is safe even if two spends somehow raced. */
    suspend fun trySpendTokens(cost: Int): Boolean {
        var spent = false
        context.dataStore.edit {
            val current = it[Keys.TOKEN_BALANCE] ?: 0
            if (current >= cost) {
                it[Keys.TOKEN_BALANCE] = current - cost
                spent = true
            }
        }
        return spent
    }

    /** A random ID generated once on first use and persisted forever after - not tied to any
     * account or login, just an anonymous per-install identity signal so the backend can rate-limit
     * how fast one install floods the shared effects pool (see PoolService.contribute). Same
     * atomic-edit trick as [trySpendTokens] so two concurrent callers can't each generate and
     * persist a different ID. */
    suspend fun getOrCreateDeviceId(): String {
        var id = ""
        context.dataStore.edit { prefs ->
            val existing = prefs[Keys.DEVICE_ID]
            id = existing ?: UUID.randomUUID().toString().also { prefs[Keys.DEVICE_ID] = it }
        }
        return id
    }
}
