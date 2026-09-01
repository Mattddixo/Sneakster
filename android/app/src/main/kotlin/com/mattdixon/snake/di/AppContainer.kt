package com.mattdixon.snake.di

import android.content.Context
import com.mattdixon.snake.BuildConfig
import com.mattdixon.snake.data.GameSettings
import com.mattdixon.snake.data.LeaderboardService
import com.mattdixon.snake.data.PoolService
import com.mattdixon.snake.data.SettingsRepository
import com.mattdixon.snake.data.createSneaksterHttpClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

/**
 * Manual dependency container: this app is small enough that Hilt/Koin would be pure overhead.
 * Everything here is a cheap singleton constructed once at process start.
 */
class AppContainer(context: Context, applicationScope: CoroutineScope) {
    val settingsRepository = SettingsRepository(context.applicationContext)

    /** A synchronous snapshot of settings, for callers (like the HTTP client) that can't suspend. */
    val settingsState: StateFlow<GameSettings> = settingsRepository.settings.stateIn(
        scope = applicationScope,
        started = SharingStarted.Eagerly,
        initialValue = GameSettings(),
    )

    private val httpClient = createSneaksterHttpClient()
    private val serverBaseUrl = { settingsState.value.serverBaseUrl.ifBlank { BuildConfig.DEFAULT_SERVER_BASE_URL } }

    val leaderboardService = LeaderboardService(httpClient, serverBaseUrl)
    val poolService = PoolService(httpClient, serverBaseUrl)
}
