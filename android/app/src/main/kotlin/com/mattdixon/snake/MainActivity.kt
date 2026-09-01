package com.mattdixon.snake

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import com.mattdixon.snake.navigation.SneaksterNavHost
import com.mattdixon.snake.ui.LocalAppContainer
import com.mattdixon.snake.ui.theme.SneaksterTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val container = (application as SneaksterApplication).container

        setContent {
            CompositionLocalProvider(LocalAppContainer provides container) {
                SneaksterTheme {
                    Surface(modifier = Modifier.fillMaxSize()) {
                        SneaksterNavHost()
                    }
                }
            }
        }
    }
}
