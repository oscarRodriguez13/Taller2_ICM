package com.example.taller2

import android.app.UiModeManager
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.preference.PreferenceManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.taller2.databinding.ActivityMapaBinding
import org.osmdroid.api.IMapController
import org.osmdroid.config.Configuration
import org.osmdroid.library.BuildConfig
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.TilesOverlay

class MapaActivity : AppCompatActivity(), SensorEventListener, LocationListener {

    private lateinit var binding: ActivityMapaBinding
    private lateinit var sensorManager: SensorManager
    private lateinit var locationManager: LocationManager
    private var lightSensor: Sensor? = null
    private var marker: Marker? = null
    var geoPoint: GeoPoint? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Configuration.getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this))
        Configuration.getInstance().userAgentValue = applicationContext.packageName

        binding = ActivityMapaBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.osmMap.setTileSource(TileSourceFactory.MAPNIK)
        binding.osmMap.setMultiTouchControls(true)

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager  // Initialize locationManager before calling handlePermissions()
        binding.osmMap.setMultiTouchControls(true)

        if (lightSensor == null) {
            Toast.makeText(this, "No light sensor found!", Toast.LENGTH_LONG).show()
        }

        handlePermissions()  // Now locationManager is initialized before this method is called.
    }

    private fun handlePermissions() {
        when {
            ContextCompat.checkSelfPermission(
                this, android.Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED -> {
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 0f, this)
            }
            ActivityCompat.shouldShowRequestPermissionRationale(
                this, android.Manifest.permission.ACCESS_FINE_LOCATION) -> {
                requestPermissions(
                    arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION),
                    Datos.MY_PERMISSION_REQUEST_LOCATION)
            }
            else -> {
                requestPermissions(
                    arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION),
                    Datos.MY_PERMISSION_REQUEST_LOCATION
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        binding.osmMap.onResume()
        sensorManager.registerListener(this, lightSensor, SensorManager.SENSOR_DELAY_NORMAL)
    }

    override fun onPause() {
        super.onPause()
        binding.osmMap.onPause()
        sensorManager.unregisterListener(this)
        locationManager.removeUpdates(this)
    }

    override fun onLocationChanged(location: Location) {
        geoPoint = GeoPoint(location.latitude, location.longitude)
        val mapController: IMapController = binding.osmMap.controller
        mapController.setCenter(geoPoint)
        mapController.setZoom(18.0)

        if (marker == null) {
            marker = Marker(binding.osmMap)
            binding.osmMap.overlays.add(marker)
        }
        marker?.position = geoPoint
        marker?.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        marker?.title = "Localizacion"
        binding.osmMap.invalidate()
    }

    override fun onSensorChanged(event: SensorEvent?) {
        event?.let {
            if (event.sensor.type == Sensor.TYPE_LIGHT) {
                val lux = event.values[0]
                if (lux < 40) {
                    binding.osmMap.overlayManager.tilesOverlay.setColorFilter(TilesOverlay.INVERT_COLORS)
                } else {
                    binding.osmMap.overlayManager.tilesOverlay.setColorFilter(null)
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // No implementation needed
    }
}
