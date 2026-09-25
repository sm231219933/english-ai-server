package com.TeacherTinkl.myapplication.voice

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

sealed class DownloadState {
    object Idle : DownloadState()
    data class Progress(val progressPercent: Int, val downloadedBytes: Long, val totalBytes: Long) : DownloadState()
    object Completed : DownloadState()
    data class Error(val message: String) : DownloadState()
}

class WhisperModelDownloader(private val context: Context) {

    companion object {
        const val MODEL_NAME = "ggml-tiny.en.bin"
        const val MODEL_URL = "https://huggingface.co/Sandy22723/whisper-tiny-model/resolve/main/ggml-tiny.en.bin"
    }

    fun getModelFile(): File {
        return File(context.filesDir, MODEL_NAME)
    }

    fun isModelDownloaded(): Boolean {
        val file = getModelFile()
        return file.exists() && file.length() > 10_000_000L // ~39MB
    }

    fun downloadModel(): Flow<DownloadState> = flow {
        val targetFile = getModelFile()
        if (isModelDownloaded()) {
            emit(DownloadState.Completed)
            return@flow
        }

        try {
            emit(DownloadState.Progress(0, 0L, 39_000_000L))
            val url = URL(MODEL_URL)
            val connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 15000
            connection.readTimeout = 30000
            connection.connect()

            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                emit(DownloadState.Error("Server returned code ${connection.responseCode}"))
                return@flow
            }

            val fileLength = if (connection.contentLengthLong > 0) connection.contentLengthLong else 39_000_000L
            val input = connection.inputStream
            val output = FileOutputStream(targetFile)

            val buffer = ByteArray(8192)
            var downloaded = 0L
            var count: Int

            while (input.read(buffer).also { count = it } != -1) {
                downloaded += count
                output.write(buffer, 0, count)
                val progress = ((downloaded * 100) / fileLength).toInt().coerceIn(0, 100)
                emit(DownloadState.Progress(progress, downloaded, fileLength))
            }

            output.flush()
            output.close()
            input.close()

            if (isModelDownloaded()) {
                emit(DownloadState.Completed)
            } else {
                emit(DownloadState.Error("Downloaded file is incomplete"))
            }

        } catch (e: Exception) {
            Log.e("WhisperDownloader", "Download failed", e)
            emit(DownloadState.Error(e.localizedMessage ?: "Network error during download"))
        }
    }.flowOn(Dispatchers.IO)
}
