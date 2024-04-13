package com.example.taller2

import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.Color
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.AsyncTask
import android.os.Bundle
import android.os.StrictMode
import android.util.Log
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.taller2.databinding.ActivityMapaBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import org.osmdroid.api.IMapController
import org.osmdroid.bonuspack.routing.OSRMRoadManager
import org.osmdroid.bonuspack.routing.Road
import org.osmdroid.bonuspack.routing.RoadManager
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.TilesOverlay
import java.io.IOException
import kotlin.math.roundToInt

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
    private val RADIUS_OF_EARTH_KM = 6371
    private lateinit var roadManager: RoadManager
    private val routePolylineMap = mutableMapOf<Road, Polyline>()
    private val routeColors = mutableMapOf<Road, Int>()


    private inner class FetchRouteTask(private val start: GeoPoint, private val finish: GeoPoint) : AsyncTask<Void, Void, Road>() {

        override fun doInBackground(vararg params: Void?): Road? {
            val routePoints = ArrayList<GeoPoint>()
            routePoints.add(start)
            routePoints.add(finish)
            return roadManager.getRoad(routePoints)
        }

        override fun onPostExecute(result: Road?) {
            super.onPostExecute(result)
            if (result != null) {
                drawRoad(result)
            } else {
                Toast.makeText(this@MapaActivity, "Error al obtener la ruta", Toast.LENGTH_SHORT).show()
            }
        }
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Configuration.getInstance().userAgentValue = applicationContext.packageName

        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        lightSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_LIGHT)
        locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
        roadManager = OSRMRoadManager(this, "ANDROID")

        handlePermissions()

        binding = ActivityMapaBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.osmMap.setTileSource(TileSourceFactory.MAPNIK)
        binding.osmMap.setMultiTouchControls(true)
        binding.osmMap.overlays.add(createOverlayEvents())

        mGeocoder = Geocoder(baseContext)

        binding.centerButton.setOnClickListener {
            centerCameraOnUser()
        }

        binding.editText.setOnEditorActionListener { v, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                val inputMethodManager = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                inputMethodManager.hideSoftInputFromWindow(binding.editText.windowToken, 0)

                val addressString = binding.editText.text.toString()
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

                                showDistanceToast(newMarker.position.latitude, newMarker.position.longitude, marker!!.position.latitude, marker!!.position.longitude, addressString)
                                markers.add(newMarker)
                                binding.osmMap.overlays.add(newMarker)
                                drawRoute(marker!!.position, newMarker.position)

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
                    Toast.makeText(this, "La dirección está vacía", Toast.LENGTH_SHORT).show()
                }
                true
            } else {
                false
            }
        }

    }

    private fun createOverlayEvents(): MapEventsOverlay {
        val overlayEventos = MapEventsOverlay(object : MapEventsReceiver {
            override fun singleTapConfirmedHelper(p: GeoPoint): Boolean {
                return false
            }
            override fun longPressHelper(loc: GeoPoint): Boolean {
                longPressOnMap(loc)
                return true
            }
        })
        return overlayEventos
    }

    private fun longPressOnMap(loc: GeoPoint) {
        val mapController: IMapController = binding.osmMap.controller
        mapController.setCenter(loc)
        mapController.setZoom(18.0)

        val newMarker = Marker(binding.osmMap)
        newMarker.position = loc
        newMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        val nombreLugar = buscarNombrePorLocalizacion(loc)
        newMarker.title = nombreLugar
        if (nombreLugar != null) {
            showDistanceToast(loc.latitude, loc.longitude, marker!!.position.latitude, marker!!.position.longitude, nombreLugar)
        }
        drawRoute(marker!!.position, newMarker.position)

        markers.add(newMarker)
        binding.osmMap.overlays.add(newMarker)

        binding.osmMap.invalidate()
    }

    private fun buscarNombrePorLocalizacion(loc: GeoPoint): String? {
        try {
            if (Geocoder.isPresent()) {
                val addresses: List<Address>? = mGeocoder?.getFromLocation(loc.latitude, loc.longitude, 1)

                if (addresses != null && addresses.isNotEmpty()) {
                    val address = addresses[0]
                    // Combina todos los campos de dirección en una cadena
                    val addressFragments = with(address) {
                        (0..maxAddressLineIndex).map { getAddressLine(it) }
                    }
                    return addressFragments.joinToString(separator = "\n")
                }
            } else {
                Toast.makeText(this, "Geocoder no disponible", Toast.LENGTH_SHORT).show()
                Log.e("MapaActivity", "Geocoder no disponible")
            }
        } catch (e: IOException) {
            e.printStackTrace()
            Toast.makeText(this, "Error al obtener la dirección", Toast.LENGTH_SHORT).show()
            Log.e("MapaActivity", "Error al obtener la dirección: ${e.message}")
        }
        return null
    }

    private fun showDistanceToast(lat1: Double, long1: Double, lat2: Double, long2: Double, nombreLugar: String) {
        val latDistance = Math.toRadians(lat1 - lat2)
        val lngDistance = Math.toRadians(long1 - long2)
        val a = (Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lngDistance / 2) * Math.sin(lngDistance / 2))
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        val result = RADIUS_OF_EARTH_KM * c
        val distance = (result * 100.0).roundToInt() / 100.0

        Toast.makeText(this, "Estas a $distance km de $nombreLugar", Toast.LENGTH_LONG).show()
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

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // No implementation needed
    }

    private fun drawRoute(start: GeoPoint, finish: GeoPoint) {
        FetchRouteTask(start, finish).execute()
    }

    private fun drawRoad(road: Road) {
        Log.i("OSM_acticity", "Route length: ${road.mLength} klm")
        Log.i("OSM_acticity", "Duration: ${road.mDuration / 60} min")

        // Verifica si ya existe una Polyline para esta ruta
        val existingPolyline = routePolylineMap[road]

        if (existingPolyline == null) {
            // Si no existe, crea una nueva Polyline
            val newPolyline = RoadManager.buildRoadOverlay(road)
            val color = generateUniqueColor()
            newPolyline.outlinePaint.color = color
            newPolyline.outlinePaint.strokeWidth = 10f
            routePolylineMap[road] = newPolyline
            routeColors[road] = color
            binding.osmMap.overlays.add(newPolyline)
        }

        binding.osmMap.invalidate()
    }

    private fun generateUniqueColor(): Int {
        return Color.rgb((Math.random() * 256).toInt(), (Math.random() * 256).toInt(), (Math.random() * 256).toInt())
    }

    private fun centerCameraOnUser() {
        marker?.let {
            val mapController: IMapController = binding.osmMap.controller
            mapController.setCenter(marker!!.position)
            mapController.setZoom(18.0)
        } ?: run {
            Toast.makeText(this, "Ubicación no disponible", Toast.LENGTH_SHORT).show()
        }
    }


}
