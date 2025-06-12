package com.iust.polaris.data.remote

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface ApiService {

    /**
     * Sends a batch of network measurements to the server.
     * @param syncRequest The request body containing the list of measurements.
     * @return A Retrofit Response object. We expect an empty body on success (2xx).
     */
    @POST("/api/NetworkMeasurement/SaveMultiple")
    suspend fun syncMeasurements(@Body syncRequest: SyncRequestDto): Response<Unit>

}
