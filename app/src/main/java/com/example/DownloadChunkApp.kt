package com.example

import android.app.Application
import android.util.Log

class DownloadChunkApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Log.d("DownloadChunkApp", "DownloadChunkApp initialized")

        // Capturar excepciones globales no controladas para evitar que el usuario sea expulsado
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Log.e("DownloadChunkApp", "Unhandled exception caught on thread ${thread.name}: ${throwable.message}", throwable)
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }
}
