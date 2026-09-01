package com.mattdixon.snake.ui

import androidx.compose.runtime.staticCompositionLocalOf
import com.mattdixon.snake.di.AppContainer

val LocalAppContainer = staticCompositionLocalOf<AppContainer> {
    error("AppContainer not provided — wrap the composable tree in CompositionLocalProvider")
}
