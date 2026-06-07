package com.example.data.network

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Url

interface VercelApiService {
    @POST
    suspend fun syncCapture(
        @Url url: String,
        @Body payload: CapturePayload
    ): Response<CaptureResponse>
}
