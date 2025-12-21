package com.mehedee.filesync.data.remote

import android.content.Context
import android.net.Uri
import android.util.Log
import com.mehedee.filesync.data.local.entity.FileSyncEntity
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class ChunkedUploadService(private val context: Context) {

    private val TAG = "ChunkedUploadService"
    private val CHUNK_SIZE = 1024 * 1024 // 1MB chunks

    private var isPaused = false

    suspend fun uploadFileWithProgress(
        file: FileSyncEntity,
        onProgress: (uploadedBytes: Long, progress: Int) -> Unit
    ): Result<UploadResponse> {
        return try {
            isPaused = false

            val uri = Uri.parse(file.filePath)
            val tempFile = uriToFile(uri, file.fileName)

            if (!tempFile.exists()) {
                return Result.failure(Exception("File not found"))
            }

            val startByte = file.uploadedBytes
            val totalBytes = file.fileSize

            Log.d(TAG, "Starting upload: ${file.fileName}, Size: $totalBytes, Resume from: $startByte")

            // Simulate chunked upload with progress
            var uploadedBytes = startByte

            while (uploadedBytes < totalBytes && !isPaused) {
                val chunkSize = minOf(CHUNK_SIZE.toLong(), totalBytes - uploadedBytes)

                // Simulate network delay
                kotlinx.coroutines.delay(500)

                uploadedBytes += chunkSize
                val progress = ((uploadedBytes.toFloat() / totalBytes) * 100).toInt()

                onProgress(uploadedBytes, progress)

                Log.d(TAG, "Progress: $progress% ($uploadedBytes / $totalBytes)")
            }

            if (isPaused) {
                Log.d(TAG, "Upload paused at $uploadedBytes bytes")
                tempFile.delete()
                return Result.failure(Exception("Upload paused"))
            }

            // Final upload to server
            val result = uploadCompleteFile(tempFile, file)

            tempFile.delete()

            result

        } catch (e: Exception) {
            Log.e(TAG, "Upload error: ${e.message}")
            Result.failure(e)
        }
    }

    private suspend fun uploadCompleteFile(
        file: File,
        fileEntity: FileSyncEntity
    ): Result<UploadResponse> {
        return try {
            val requestFile = file.asRequestBody("multipart/form-data".toMediaTypeOrNull())
            val filePart = MultipartBody.Part.createFormData("file", fileEntity.fileName, requestFile)

            val fileNameBody = fileEntity.fileName.toRequestBody("text/plain".toMediaTypeOrNull())
            val fileHashBody = fileEntity.fileHash.toRequestBody("text/plain".toMediaTypeOrNull())
            val fileSizeBody = fileEntity.fileSize.toString().toRequestBody("text/plain".toMediaTypeOrNull())

            val api = RetrofitClient.getApi(context)
            val response = api.uploadFile(filePart, fileNameBody, fileHashBody, fileSizeBody)

            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Upload failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun pauseUpload() {
        isPaused = true
        Log.d(TAG, "Upload pause requested")
    }

    private fun uriToFile(uri: Uri, fileName: String): File {
        val tempFile = File(context.cacheDir, fileName)
        try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(tempFile).use { output ->
                    input.copyTo(output)
                }
            }
        } catch (e: IOException) {
            Log.e(TAG, "Error creating temp file: ${e.message}")
        }
        return tempFile
    }
}