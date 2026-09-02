package com.mattdixon.snake

import android.app.Application
import android.util.Log
import com.mattdixon.snake.di.AppContainer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob

private const val CRASH_LOG_TAG = "SneaksterCrash"

class SneaksterApplication : Application() {
    private val applicationScope = CoroutineScope(SupervisorJob())

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        installCrashLogger()
        container = AppContainer(this, applicationScope)
    }

    /** No crash-reporting service (no accounts, no telemetry to send anywhere) - this just
     * makes sure a fatal crash is clearly visible in logcat before handing off to the platform's
     * default handler, instead of the failure only showing up as "app has stopped" with nothing
     * to grep for. Never swallows the exception: the app should still die and show the system
     * crash UI, not silently limp along in a broken state. */
    private fun installCrashLogger() {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Log.e(CRASH_LOG_TAG, "Uncaught exception on thread ${thread.name}", throwable)
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }
}
