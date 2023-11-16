package com.map.utils

import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import com.map.main.MainActivity

class LocationHandler(private val context: Context) {

    private var locationManager: LocationManager? = null
    private var locationListener: LocationListener? = null


    // Callback to receive location updates
    private val locationCallback = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            // Handle location updates here
            // You can access latitude and longitude using location.latitude and location.longitude
            MainActivity.onLocChangedInterface.onLocationChanged(location)
        }

        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {
            // Handle status changes if needed
        }

        override fun onProviderEnabled(provider: String) {
            // Called when the provider is enabled by the user
        }

        override fun onProviderDisabled(provider: String) {
            // Called when the provider is disabled by the user
        }
    }

    // Start listening for location updates
    fun startLocationUpdates() {
        locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager?
        locationListener = locationCallback

        // Check for permission before requesting location updates
        if (PermissionUtils.checkLocationPermission(context)) {
            locationManager?.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                MIN_TIME_BETWEEN_UPDATES,
                MIN_DISTANCE_CHANGE_FOR_UPDATES,
                locationListener!!
            )
        }
    }

    // Stop listening for location updates
    fun stopLocationUpdates() {
        locationManager?.removeUpdates(locationListener!!)
    }

    companion object {
        private const val MIN_TIME_BETWEEN_UPDATES: Long = 1000 * 60 // 1 minute
        private const val MIN_DISTANCE_CHANGE_FOR_UPDATES: Float = 10f // 10 meters
    }

}

interface OnLocChangedInterface{

    fun onLocationChanged(location: Location)
}
