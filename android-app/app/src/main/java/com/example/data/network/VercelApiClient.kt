package com.example.data.network

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object VercelApiClient {
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    val service: VercelApiService by lazy {
        Retrofit.Builder()
            // Generic placeholder URL, overridden dynamically via @Url parameter
            .baseUrl("https://api.vercel.app/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(VercelApiService::class.java)
    }

    fun sanitizeUrl(url: String): String {
        val trimmed = url.trim()
        if (trimmed.isEmpty()) {
            return "https://factbook-orcin.vercel.app/api/capture"
        }
        
        try {
            val withProtocol = if (!trimmed.startsWith("http://") && !trimmed.startsWith("https://")) {
                "https://$trimmed"
            } else {
                trimmed
            }
            
            val uri = java.net.URI(withProtocol)
            val host = uri.host
            if (!host.isNullOrEmpty()) {
                val scheme = uri.scheme ?: "https"
                
                // If it already contains '/api/capture', let's clean the prefix and discard any trailing characters
                if (trimmed.contains("/api/capture")) {
                    val base = withProtocol.substringBefore("/api/capture") + "/api/capture"
                    return base
                }
                
                // Otherwise, automatically direct the request to the root-relative /api/capture route
                return "$scheme://$host/api/capture"
            }
        } catch (e: Exception) {
            // Ignore exception and fall back to manual string transformations
        }

        val cleanBase = if (trimmed.endsWith("/")) trimmed.dropLast(1) else trimmed
        return if (cleanBase.endsWith("/api/capture")) {
            cleanBase
        } else {
            "$cleanBase/api/capture"
        }
    }
}
