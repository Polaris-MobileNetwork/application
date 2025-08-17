package com.iust.polaris.data.remote

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface ApiService {

    /**
     * Sends a batch of network measurements to the server.
     * @param syncRequest The request body containing the list of measurements.
     * @return A Retrofit Response object. We expect an empty body on success (2xx).
     */
    @POST("/api/NetworkMeasurement/SaveMultiple")
    suspend fun syncMeasurements(@Body syncRequest: SyncRequestDto): Response<Unit>

    @POST("/api/Test/except")
    suspend fun getTests(@Body request: TestSyncRequestDto): Response<TestSyncResponseDto>

    /**
     * Sends a batch of test results to the server.
     */
    @POST("/api/TestResult/SaveMultiple") // Endpoint from your specification
    suspend fun syncTestResults(@Body request: TestResultSyncRequestDto): Response<Unit>

    @GET("/api/Test/deleted")
    suspend fun getDeletedTestIds(): Response<DeletedTestsResponseDto>

}
