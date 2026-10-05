package com.example.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log

class DownloadActionReceiver : BroadcastReceiver() {

    companion object {
        const val TAG = "DownloadChunk"
        const val ACTION_PAUSE = "com.example.service.ACTION_PAUSE"
        const val ACTION_RESUME = "com.example.service.ACTION_RESUME"
        const val ACTION_CANCEL = "com.example.service.ACTION_CANCEL"
        const val ACTION_PAUSE_ALL = "com.example.service.ACTION_PAUSE_ALL"
        const val ACTION_CANCEL_ALL = "com.example.service.ACTION_CANCEL_ALL"
        const val EXTRA_DOWNLOAD_ID = "extra_download_id"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val downloadId = intent.getStringExtra(EXTRA_DOWNLOAD_ID)

        Log.d(TAG, "DownloadActionReceiver received action: $action, downloadId: $downloadId")

        val serviceIntent = Intent(context, DownloadService::class.java).apply {
            this.action = action
            if (downloadId != null) {
                putExtra(EXTRA_DOWNLOAD_ID, downloadId)
            }
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error starting DownloadService from receiver: ${e.message}", e)
        }
    }
}
