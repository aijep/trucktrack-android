package com.trucktrack.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.trucktrack.R
import com.trucktrack.data.ApiClient
import com.trucktrack.data.StatusUpdateRequest
import com.trucktrack.service.LocationTrackingService
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var tvStatus: TextView
    private lateinit var tvRoute: TextView
    private lateinit var tvMaterial: TextView
    private lateinit var tvDriver: TextView
    private lateinit var btnAction: Button
    private lateinit var btnLoadTrip: Button
    private lateinit var etTripId: EditText
    private lateinit var tvGpsStatus: TextView

    private var currentStatus = ""
    private var currentTripId = -1

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        if (perms.values.all { it }) loadTrip()
        else toast("Location permission is required for tracking")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvStatus   = findViewById(R.id.tv_status)
        tvRoute    = findViewById(R.id.tv_route)
        tvMaterial = findViewById(R.id.tv_material)
        tvDriver   = findViewById(R.id.tv_driver)
        btnAction  = findViewById(R.id.btn_action)
        btnLoadTrip = findViewById(R.id.btn_load_trip)
        etTripId   = findViewById(R.id.et_trip_id)
        tvGpsStatus = findViewById(R.id.tv_gps_status)

        btnLoadTrip.setOnClickListener {
            val id = etTripId.text.toString().toIntOrNull()
            if (id == null) { toast("Enter a valid Trip ID"); return@setOnClickListener }
            currentTripId = id
            checkPermissionsAndLoad()
        }

        btnAction.setOnClickListener { performNextAction() }
    }

    private fun checkPermissionsAndLoad() {
        val perms = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            perms.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        val missing = perms.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missing.isEmpty()) loadTrip()
        else permissionLauncher.launch(missing.toTypedArray())
    }

    private fun loadTrip() {
        tvStatus.text = "Loading trip #$currentTripId..."
        lifecycleScope.launch {
            try {
                val trip = ApiClient.api.getTrip(currentTripId)
                currentStatus = trip.status
                tvRoute.text    = "📍 ${trip.source_name}  →  🏁 ${trip.dest_name}"
                tvMaterial.text = "📦 ${trip.material_desc ?: "—"}  •  ${trip.load_weight_ton ?: "?"}t"
                tvDriver.text   = "👤 ${trip.driver.name}  •  🚛 ${trip.vehicle.plate_number} ${trip.vehicle.model ?: ""}"
                updateUI()
                toast("Trip loaded successfully!")
            } catch (e: Exception) {
                tvStatus.text = "Error loading trip"
                toast("Could not load trip: ${e.message}")
            }
        }
    }

    private fun performNextAction() {
        val nextStatus = when (currentStatus) {
            "pending"    -> "loaded"
            "loaded"     -> "in_transit"
            "in_transit" -> "arrived"
            "arrived"    -> "unloaded"
            "unloaded"   -> "completed"
            else -> return
        }
        btnAction.isEnabled = false
        lifecycleScope.launch {
            try {
                val trip = ApiClient.api.updateStatus(currentTripId, StatusUpdateRequest(nextStatus))
                currentStatus = trip.status
                when (nextStatus) {
                    "in_transit" -> startTracking()
                    "completed"  -> stopTracking()
                }
                updateUI()
                toast(statusMessage(nextStatus))
            } catch (e: Exception) {
                toast("Error: ${e.message}")
            } finally {
                btnAction.isEnabled = currentStatus != "completed"
            }
        }
    }

    private fun startTracking() {
        val intent = Intent(this, LocationTrackingService::class.java).apply {
            putExtra(LocationTrackingService.EXTRA_TRIP_ID, currentTripId)
        }
        ContextCompat.startForegroundService(this, intent)
        tvGpsStatus.text = "🟢 GPS tracking ON — sending location every 10s"
    }

    private fun stopTracking() {
        stopService(Intent(this, LocationTrackingService::class.java))
        tvGpsStatus.text = "⚫ GPS tracking OFF"
    }

    private fun updateUI() {
        tvStatus.text = "Status: ${currentStatus.replace("_", " ").uppercase()}"
        btnAction.text = when (currentStatus) {
            "pending"    -> "✅ Confirm materials loaded"
            "loaded"     -> "🚛 Start trip (begin GPS)"
            "in_transit" -> "📍 Mark arrived at destination"
            "arrived"    -> "📦 Confirm materials unloaded"
            "unloaded"   -> "✔️ Complete trip"
            "completed"  -> "Trip completed ✅"
            else -> "—"
        }
        btnAction.isEnabled = currentStatus != "completed" && currentTripId != -1
    }

    private fun statusMessage(s: String) = when (s) {
        "loaded"     -> "✅ Materials loaded confirmed"
        "in_transit" -> "🚛 Trip started — GPS tracking ON!"
        "arrived"    -> "📍 Arrived at destination"
        "unloaded"   -> "📦 Materials unloaded confirmed"
        "completed"  -> "✅ Trip completed successfully!"
        else -> s
    }

    private fun toast(msg: String) =
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
}
