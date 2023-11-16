package com.map.main

import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.FragmentActivity
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.MapsInitializer
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.map.R
import com.map.utils.LocationHandler
import com.map.utils.OnLocChangedInterface
import com.map.utils.PathJSONParser
import com.map.utils.PermissionUtils
import com.map.databinding.ActivityMainBinding
import com.map.databinding.LayoutDetailsBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.lang.Math.atan2
import java.lang.Math.cos
import java.lang.Math.sin
import java.lang.Math.sqrt
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import kotlin.math.roundToInt


class MainActivity : FragmentActivity(), OnMapReadyCallback, OnLocChangedInterface {


    private var mMap: GoogleMap? = null
    private lateinit var mapView: MapView
    private lateinit var binding: ActivityMainBinding
    private val LOCATION_PERMISSION_REQUEST_CODE = 1001
    private lateinit var locationHandler: LocationHandler
    private lateinit var bindBottom: LayoutDetailsBinding

    companion object {
        lateinit var onLocChangedInterface: OnLocChangedInterface
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MapsInitializer.initialize(
            this,
            MapsInitializer.Renderer.LATEST
        ) {
            //println(it.name)
            Log.d("TAG", "onMapsSdkInitialized: ")
        }
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        onLocChangedInterface = this

        /* init map */
        mapView = binding.mapView
        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync(this)


        /* enable location and permissions */
        locationHandler = LocationHandler(this)
        // Check and request location permissions if needed
        if (PermissionUtils.checkLocationPermission(this)) {
            startLocationUpdates()
        } else {
            requestLocationPermission()
        }


        val bottom = BottomSheetDialog(this,  R.style.ThemeOverlay_App_BottomSheetDialog)
        bottom.behavior.state = BottomSheetBehavior.STATE_COLLAPSED
        bottom.behavior.peekHeight = 300
        bottom.setCancelable(false)
        bindBottom = LayoutDetailsBinding.inflate(layoutInflater)

        bottom.setContentView(bindBottom.root)
        bottom.show()


    }

    private fun startLocationUpdates() {
        locationHandler.startLocationUpdates()
    }

    private fun stopLocationUpdates() {
        locationHandler.stopLocationUpdates()
    }

    private fun requestLocationPermission() {
        PermissionUtils.requestLocationPermission(this, LOCATION_PERMISSION_REQUEST_CODE)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            LOCATION_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    startLocationUpdates()
                } else {
                    // Handle the case where the user denies the permission
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopLocationUpdates()
    }

    override fun onMapReady(p0: GoogleMap) {
        mMap = p0
    }

    override fun onLocationChanged(location: Location) {
        mMap!!.clear()

        // Specify the locations you want directions between
        val origin = LatLng(location.latitude, location.longitude)
        val destination = LatLng(25.25550940537631, 55.32958662395205)

        // Add markers for origin and destination
        mMap!!.addMarker(MarkerOptions().position(origin).title("Start").icon(BitmapDescriptorFactory.fromBitmap(generateSmallIcon())))
        mMap!!.addMarker(MarkerOptions().position(destination).title("End"))

        // Move camera to show both markers
        mMap!!.moveCamera(CameraUpdateFactory.newLatLngZoom(origin, 12f))

        // Fetch and draw directions
        getDirections(origin, destination)

        val geocoder = Geocoder(this ,Locale.getDefault())
        var addressText = ""

        try {
            val addresses: List<Address>? = geocoder.getFromLocation(location.latitude, location.longitude, 1)

            if (!addresses.isNullOrEmpty()) {
                val address: Address = addresses[0]
                addressText = address.getAddressLine(0) + address.adminArea + address.locality + address.countryName

            // You can get more details like city, state, country, etc. from other methods of the Address class
            }
        } catch (e: IOException) {
            Log.e("Geocoder", "Error getting location details: ${e.localizedMessage}")
        }
        bindBottom.dest.text = addressText
       calculateDistance(location.latitude, location.longitude, 25.25550940537631, 55.32958662395205)

    }

    private fun calculateDistance(
        lat1: Double,
        lon1: Double,
        lat2: Double,
        lon2: Double
    ) {
        val R = 6371 // Earth radius in kilometers

        val latDistance = Math.toRadians(lat2 - lat1)
        val lonDistance = Math.toRadians(lon2 - lon1)

        val a = kotlin.math.sin(latDistance / 2) * kotlin.math.sin(latDistance / 2) +
                kotlin.math.cos(Math.toRadians(lat1)) * kotlin.math.cos(Math.toRadians(lat2)) *
                kotlin.math.sin(lonDistance / 2) * kotlin.math.sin(lonDistance / 2)

        val c = 2 * kotlin.math.atan2(kotlin.math.sqrt(a), kotlin.math.sqrt(1 - a))

        val result = R * c // Distance in kilometers

        bindBottom.km.text = result.roundToInt().toString()
    }

    private fun generateSmallIcon(): Bitmap {
        val height = 200
        val width = 200
        val bitmap = BitmapFactory.decodeResource(resources, R.drawable.car)
        return Bitmap.createScaledBitmap(bitmap, width, height, false)
    }

    private fun getDirections(origin: LatLng, destination: LatLng) {
        val url = "https://maps.googleapis.com/maps/api/directions/json" +
                "?origin=${origin.latitude},${origin.longitude}" +
                "&destination=${destination.latitude},${destination.longitude}" +
                "&key=AIzaSyAKueYWFVF6M472H_4nPwZEyxhkfNOmj8o"

        GlobalScope.launch(Dispatchers.IO) {
            try {
                val connection = URL(url).openConnection() as HttpURLConnection
                val inputStream = connection.inputStream
                val bufferedReader = BufferedReader(InputStreamReader(inputStream))

                val response = StringBuilder()
                var line: String?

                while (bufferedReader.readLine().also { line = it } != null) {
                    response.append(line)
                }

                bufferedReader.close()

                withContext(Dispatchers.Main) {
                    drawPolyline(response.toString())
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun drawPolyline(jsonData: String) {
        val jObject = JSONObject(jsonData)
        val routes = PathJSONParser().parse(jObject)

        for (i in routes.indices) {
            val points = routes[i]

            val lineOptions = PolylineOptions()
            for (point in points) {
                lineOptions.add(point)
            }

            lineOptions.width(10f)
            lineOptions.color(resources.getColor(R.color.colorPrimary))

            mMap!!.addPolyline(lineOptions)
        }
    }

    override fun onStart() {
        super.onStart()
        mapView.onStart()
    }

    override fun onStop() {
        super.onStop()
        mapView.onStop()
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView.onLowMemory()
    }




}