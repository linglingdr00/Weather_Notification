package com.linglingdr00.weather

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import java.util.Locale
import java.util.concurrent.TimeUnit


class CurrentLocationListener(private val context: Context, private val callback: (Location?, List<Address>?) -> Unit) {

    private val TAG = "CurrentLocationListener"

    private var currentLocation: Location? = null
    private var currentAddress: List<Address>? = null

    // location
    private var locationManager: LocationManager? = context.getSystemService(AppCompatActivity.LOCATION_SERVICE) as LocationManager
    private val networkProvider = LocationManager.NETWORK_PROVIDER
    private val gpsProvider = LocationManager.GPS_PROVIDER

    // gms location
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback: LocationCallback


    private val timeout = 10
    private val handler: Handler = Handler(Looper.getMainLooper())
    private var counts = 0

    private val requestWithTimeout: Runnable = object: Runnable {
        override fun run() {
            counts++
            Log.d(TAG, "counts: $counts")
            // 如果時間超過 timeout 就停止更新位置
            if (counts > timeout) {
                Log.d(TAG, "timeout")
                stopToLocationUpdates()
                callback.invoke(currentLocation, currentAddress)
            } else {
                // 還沒超過 timeout 就繼續計算時間
                handler.postDelayed(this, 1000)
            }
        }
    }

    fun initGetLocation() {
        // 開始更新位置
        startToLocationUpdates()
        counts = 0
        handler.postDelayed(requestWithTimeout, 1000)
    }

    // 將經緯度轉成地址
    private fun tranToAddress(location: Location) {

        val geocoder = Geocoder(context, Locale.getDefault())
        val latitude = location.latitude
        val longitude = location.longitude

        // 將得到的 lastLocation 存在 currentLocation
        currentLocation = location
        // 將得到的 address 存在 currentAddress
        currentAddress = geocoder.getFromLocation(latitude, longitude, 1)
        Log.d(TAG, "address: ${currentAddress?.get(0)?.getAddressLine(0)}")

        if (currentAddress != null) {
            // 如果已得到位置和地址，就把資料傳回去
            callback.invoke(currentLocation, currentAddress)
        }

    }

    fun createLocationRequest() {
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context)

        locationRequest = LocationRequest.create().apply {
            interval = TimeUnit.SECONDS.toMillis(5)
            fastestInterval = TimeUnit.SECONDS.toMillis(5)
            maxWaitTime = TimeUnit.MINUTES.toMillis(1000)
            priority = LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY
        }

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                super.onLocationResult(locationResult)

                val location = locationResult.lastLocation
                Log.d(TAG, "gms location: $location")
                //停止
                stopToLocationUpdates()
                tranToAddress(location)

            }
        }
    }

    // 2. gms last location 定位
    fun getGMSLastLocation() {
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        fusedLocationProviderClient.lastLocation
            .addOnSuccessListener { location : Location? ->
                if (location != null) {
                    Log.d(TAG, "gms lastLocation: $location")
                    tranToAddress(location)
                } else {
                    // 用 gms location 定位
                    initGetLocation()
                }
            }
    }

    // 用 gms location 定位
    private fun startToLocationUpdates() {
        Log.d(TAG, "startToLocationUpdates()")

        try {
            fusedLocationProviderClient.requestLocationUpdates(
                locationRequest, locationCallback, Looper.getMainLooper())
        } catch (unlikely: SecurityException) {
            Log.e(TAG, "Lost location permissions. Couldn't remove updates. $unlikely")
        }
    }

    fun stopToLocationUpdates() {
        Log.d(TAG, "stopToLocationUpdates()")

        try {
            val removeTask = fusedLocationProviderClient.removeLocationUpdates(locationCallback)
            removeTask.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d(TAG, "Location Callback removed.")
                } else {
                    Log.d(TAG, "Failed to remove Location Callback.")
                }
            }
        } catch (unlikely: SecurityException) {
            Log.e(TAG, "Lost location permissions. Couldn't remove updates. $unlikely")
        }
    }


    /*private var locationListener: LocationListener = object: LocationListener {

        override fun onLocationChanged(location: Location) {
            Log.d(TAG, "location: $location")
            stopGetLocation()
            tranToAddress(location)
        }

        override fun onProviderDisabled(provider: String) {
        }

        override fun onProviderEnabled(provider: String) {
        }
    }*/

    // 1. 一般 last location 定位
    fun startGetLocation() {
        // 使用 requestLocationUpdates 開始更新經緯度
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        // 用 gpsProvider 定位
        val gpsLocation = locationManager?.getLastKnownLocation(gpsProvider)
        if (gpsLocation != null) {
            Log.d(TAG, "location gps lastLocation: $gpsLocation")
            tranToAddress(gpsLocation)
        } else {
            // 用 networkProvider 定位
            val networkLocation = locationManager?.getLastKnownLocation(networkProvider)
            if (networkLocation != null) {
                Log.d(TAG, "location network lastLocation: $networkLocation")
                tranToAddress(networkLocation)
            } else {
                // 用 gms last location 定位
                getGMSLastLocation()
            }
        }

    }

    /*private fun stopGetLocation() {
        // 移除 locationListener
        locationManager?.removeUpdates(locationListener)
    }*/

}