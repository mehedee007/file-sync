package com.mehedee.filesync.data.remote

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {

    // Default server URL - user can change this in settings
    private var baseUrl = "http://192.168.100.147:5000/"  // Change to your IP

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val api: FileUploadApi = retrofit.create(FileUploadApi::class.java)

    fun updateBaseUrl(newUrl: String) {
        baseUrl = if (newUrl.endsWith("/")) newUrl else "$newUrl/"
    }

    fun getBaseUrl(): String = baseUrl
}