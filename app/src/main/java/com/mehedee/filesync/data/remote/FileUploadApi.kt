package com.mehedee.filesync.data.remote

import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*

interface FileUploadApi {

    @GET("/")
    suspend fun checkServerStatus(): Response<ServerStatusResponse>

    @GET("/api/status")
    suspend fun getDetailedStatus(): Response<ServerStatusResponse>

    @Multipart
    @POST("/api/upload")
    suspend fun uploadFile(
        @Part file: MultipartBody.Part,
        @Part("filename") filename: RequestBody,
        @Part("fileHash") fileHash: RequestBody,
        @Part("fileSize") fileSize: RequestBody
    ): Response<UploadResponse>

    @GET("/api/files")
    suspend fun listFiles(): Response<FileListResponse>
}

data class ServerStatusResponse(
    val status: String,
    val message: String? = null,
    val server: String? = null,
    val version: String? = null,
    val timestamp: String? = null,
    val upload_folder: String? = null
)

data class UploadResponse(
    val success: Boolean,
    val message: String,
    val filename: String? = null,
    val hash: String? = null,
    val hash_verified: Boolean? = null,
    val timestamp: String? = null,
    val error: String? = null
)

data class FileListResponse(
    val success: Boolean,
    val count: Int,
    val files: List<ServerFile>,
    val error: String? = null
)

data class ServerFile(
    val filename: String,
    val size: Long,
    val modified: String
)