package com.trucktrack.service

import android.app.*
import android.content.Intent
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.*
import com.trucktrack.data.ApiClient
import com.trucktrack.data.LocationPingRequest
import kotlinx.coroutines.*

class LocationTrackingService : Service() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    companion object {
        const val CHANNEL_ID      = "truck_tracking_channel"
        const val NOTIFICATION_ID = 1001
        const val EXTRA_TRIP_ID   = "trip_id"
        var tripId: Int = -1
    }

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        tripId = intent?.getIntExtra(EXTRA_TRIP_ID, -1) ?: -1
        startForeground(NOTIFICATION_ID, buildNotification("Tracking active…"))
        startLocationUpdates()
        return START_STICKY  // restart if killed
    }

    private fun startLocationUpdates() {
        val request = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY, 10_000L  // every 10 seconds
        ).apply {
            setMinUpdateIntervalMillis(5_000L)
            setMinUpdateDistanceMeters(20f)           // only if moved 20m+
        }.build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { loc ->
                    sendPing(loc.latitude, loc.longitude,
                             loc.speed * 3.6f,         // m/s → km/h
                             loc.bearing, loc.accuracy)
                }
            }
        }

        try {
            fusedLocationClient.requestLocationUpdates(
                request, locationCallback, Looper.getMainLooper()
            )
        } catch (e: SecurityException) {
            stopSelf()
        }
    }

    private fun sendPing(
        lat: Double, lng: Double,
        speedKmh: Float, bearing: Float, accuracy: Float
    ) {
        if (tripId == -1) return
        serviceScope.launch {
            try {
                ApiClient.api.postLocationPing(
                    LocationPingRequest(
                        trip_id   = tripId,
                        lat       = lat,
                        lng       = lng,
                        speed_kmh = speedKmh.toDouble(),
                        bearing   = bearing.toDouble(),
                        accuracy  = accuracy.toDouble()
                    )
                )
            } catch (e: Exception) {
                // silent retry on next ping
            }
        }
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID, "Truck Tracking",
            NotificationManager.IMPORTANCE_LOW
        ).apply { description = "Live GPS tracking for active trip" }
        getSystemService(NotificationManager::class.java)
            .createNotificationChannel(channel)
    }

    private fun buildNotification(text: String): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this, 0,
            packageManager.getLaunchIntentForPackage(packageName),
            PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("TruckTrack — Trip #$tripId")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        fusedLocationClient.removeLocationUpdates(locationCallback)
        serviceScope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
