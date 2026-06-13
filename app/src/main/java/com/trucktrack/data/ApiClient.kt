package com.trucktrack.data

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*

// ── Your live backend URL ─────────────────────────────────────────────────────
private const val BASE_URL = "https://trucktrack-backend.onrender.com"

// ── Request/Response models ───────────────────────────────────────────────────
data class LocationPingRequest(
    val trip_id: Int,
    val lat: Double,
    val lng: Double,
    val speed_kmh: Double,
    val bearing: Double,
    val accuracy: Double
)

data class StatusUpdateRequest(val status: String)

data class TripResponse(
    val id: Int,
    val status: String,
    val source_name: String,
    val dest_name: String,
    val material_desc: String?,
    val load_weight_ton: Double?,
    val driver: DriverResponse,
    val vehicle: VehicleResponse
)

data class DriverResponse(val id: Int, val name: String, val phone: String)
data class VehicleResponse(val id: Int, val plate_number: String, val model: String?)
data class PingResponse(val id: Int, val lat: Double, val lng: Double, val timestamp: String)

// ── Retrofit API interface ────────────────────────────────────────────────────
interface TruckTrackApi {
    @GET("api/trips/{trip_id}")
    suspend fun getTrip(@Path("trip_id") tripId: Int): TripResponse

    @PATCH("api/trips/{trip_id}/status")
    suspend fun updateStatus(
        @Path("trip_id") tripId: Int,
        @Body body: StatusUpdateRequest
    ): TripResponse

    @POST("api/tracking/ping")
    suspend fun postLocationPing(@Body body: LocationPingRequest): PingResponse
}

// ── Singleton client ──────────────────────────────────────────────────────────
object ApiClient {
    val api: TruckTrackApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(TruckTrackApi::class.java)
    }
}
