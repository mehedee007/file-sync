package com.mehedee.filesync.data.remote

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.FileOutputStream

class FileUploadService(private val context: Context) {

    private val api = RetrofitClient.api

    suspend fun checkServerConnection(): Result<ServerStatusResponse> {
        return try {
            val response = api.checkServerStatus()
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Server returned error: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun uploadFile(
        uri: Uri,
        fileName: String,
        fileHash: String,
        fileSize: Long
    ): Result<UploadResponse> {
        return try {
            // Convert URI to File
            val file = uriToFile(uri, fileName)

            if (!file.exists()) {
                return Result.failure(Exception("File not found"))
            }

            // Create request body
            val requestFile = file.asRequestBody("multipart/form-data".toMediaTypeOrNull())
            val filePart = MultipartBody.Part.createFormData("file", fileName, requestFile)

            val fileNameBody = fileName.toRequestBody("text/plain".toMediaTypeOrNull())
            val fileHashBody = fileHash.toRequestBody("text/plain".toMediaTypeOrNull())
            val fileSizeBody = fileSize.toString().toRequestBody("text/plain".toMediaTypeOrNull())

            // Upload
            val response = api.uploadFile(filePart, fileNameBody, fileHashBody, fileSizeBody)

            // Clean up temp file
            file.delete()

            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Upload failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun uriToFile(uri: Uri, fileName: String): File {
        val tempFile = File(context.cacheDir, fileName)
        context.contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(tempFile).use { output ->
                input.copyTo(output)
            }
        }
        return tempFile
    }
}