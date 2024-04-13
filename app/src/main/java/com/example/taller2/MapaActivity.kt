package com.example.taller2

import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.util.Log
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.taller2.databinding.ActivityMapaBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import org.osmdroid.api.IMapController
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.TilesOverlay
import java.io.IOException

class MapaActivity : AppCompatActivity(), SensorEventListener, LocationListener {

    private lateinit var binding: ActivityMapaBinding
    private lateinit var mFusedLocationProviderClient: FusedLocationProviderClient
    private var sensorManager: SensorManager? = null
    private lateinit var locationManager: LocationManager
    private var lightSensor: Sensor? = null
    private var marker: Marker? = null
    private var mGeocoder: Geocoder? = null
    private var geoPoint: GeoPoint? = null
    private val markers = mutableListOf<Marker>()

    @SuppressLint("UseCompatLoadingForDrawables")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Configuration.getInstance().userAgentValue = applicationContext.packageName

        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        lightSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_LIGHT)
        locationManager = getSystemService(LOCATION_SERVICE) as LocationManager

        handlePermissions()

        binding = ActivityMapaBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.osmMap.setTileSource(TileSourceFactory.MAPNIK)
        binding.osmMap.setMultiTouchControls(true)

        mGeocoder = Geocoder(baseContext)

        binding.editText.setOnEditorActionListener { v, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_SEND) {
            }
            val addressString = binding.editText.text.toString()
            //val addressString = "Universidad de la Sabana"
            if (addressString.isNotEmpty()) {
                try {
                    if (Geocoder.isPresent()) {
                        Log.d("MapaActivity", "Dirección a buscar: $addressString")
                        val addresses: List<Address>? = mGeocoder!!.getFromLocationName(
                            addressString, 2, Datos.lowerLeftLatitude, Datos.lowerLeftLongitude,
                            Datos.upperRightLatitude, Datos.upperRightLongitude
                        )

                        if (addresses != null && addresses.isNotEmpty()) {
                            val addressResult = addresses[0]
                            val position = GeoPoint(addressResult.latitude, addressResult.longitude)
                            val mapController: IMapController = binding.osmMap.controller
                            mapController.setCenter(position)
                            mapController.setZoom(18.0)

                            val newMarker = Marker(binding.osmMap)
                            newMarker.position = position
                            newMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                            newMarker.title = addressString

                            markers.add(newMarker)
                            binding.osmMap.overlays.add(newMarker)

                            binding.osmMap.invalidate()
                        } else {
                            Toast.makeText(this, "Dirección no encontrada", Toast.LENGTH_SHORT).show()
                            Log.e("MapaActivity", "No se encontraron direcciones para: $addressString")
                        }

                    } else {
                        Toast.makeText(this, "Geocoder no disponible", Toast.LENGTH_SHORT).show()
                        Log.e("MapaActivity", "Geocoder no disponible")
                    }

                } catch (e: IOException) {
                    e.printStackTrace()
                    Toast.makeText(this, "Error al buscar la dirección", Toast.LENGTH_SHORT).show()
                    Log.e("MapaActivity", "Error al buscar la dirección: ${e.message}")
                }
            } else {
                Toast.makeText(this, "La dirección esta vacía", Toast.LENGTH_SHORT).show()
            }
            true
        }
    }

    private fun handlePermissions() {
        when {
            ContextCompat.checkSelfPermission(
                this, android.Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED -> {
                mFusedLocationProviderClient.lastLocation.addOnSuccessListener { location ->
                    onLocationChanged(location)
                }
            }
            ActivityCompat.shouldShowRequestPermissionRationale(
                this, android.Manifest.permission.ACCESS_FINE_LOCATION) -> {
                requestPermissions(
                    arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION),
                    Datos.MY_PERMISSION_REQUEST_LOCATION
                )
            }
            else -> {
                requestPermissions(
                    arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION),
                    Datos.MY_PERMISSION_REQUEST_LOCATION
                )
            }
        }
    }

    @SuppressLint("MissingPermission")
    override fun onResume() {
        sensorManager?.registerListener(this, lightSensor, SensorManager.SENSOR_DELAY_NORMAL)
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 2000, 10f, this)
        val latitude = 4.62
        val longitude = -74.07
        val startPoint = GeoPoint(latitude, longitude)
        super.onResume()
        binding.osmMap.onResume()
        val mapController: IMapController = binding.osmMap.controller
        mapController.setZoom(18.0)
        mapController.setCenter(startPoint)
    }

    override fun onPause() {
        super.onPause()
        binding.osmMap.onPause()
        sensorManager?.unregisterListener(this)
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
        marker?.title = "Tú"
        binding.osmMap.invalidate()
    }

    override fun onSensorChanged(event: SensorEvent?) {
        event?.let {
            if (event.sensor.type == Sensor.TYPE_LIGHT) {
                val lux = event.values[0]
                if (lux < 15000) {
                    binding.osmMap.overlayManager.tilesOverlay.setColorFilter(TilesOverlay.INVERT_COLORS)
                } else {
                    binding.osmMap.overlayManager.tilesOverlay.setColorFilter(null)
                }
            }
        }
    }

    /*private fun updateLocationUI(location: android.location.Location?) {
        location?.let { loc ->
            geoPoint = GeoPoint(loc.latitude, loc.longitude)

            // Acceder al controlador del mapa para manipular la posición y el zoom
            val mapController: IMapController = binding.osmMap.controller
            mapController.setCenter(geoPoint)
            mapController.setZoom(18.0)  // Puedes ajustar el nivel de zoom según tus necesidades

            // Comprobar si el marcador ya está inicializado o no
            if (marker == null) {
                marker = Marker(binding.osmMap)
                binding.osmMap.overlays.add(marker)
            }

            // Actualizar la posición del marcador
            marker?.position = geoPoint
            marker?.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            marker?.title = "Tu ubicación actual"

            // Redibujar el mapa para mostrar los cambios
            binding.osmMap.invalidate()
        } ?: run {
            // Manejo de situación cuando la ubicación es nula
            Toast.makeText(this, "Ubicación no disponible", Toast.LENGTH_SHORT).show()
        }
    }*/

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // No implementation needed
    }
}
